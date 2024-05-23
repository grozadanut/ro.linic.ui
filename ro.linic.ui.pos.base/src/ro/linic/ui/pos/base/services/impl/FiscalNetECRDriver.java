package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.NumberUtils.smallerThan;
import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.util.commons.PresentationUtils.safeString;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ECRDriver;
import ro.linic.util.commons.StringUtils;

@Component
public class FiscalNetECRDriver implements ECRDriver {
	private static final Logger log = Logger.getLogger(FiscalNetECRDriver.class.getName());
	private static final String ECR_REPORT_DATE_PATTERN = "dd/MM/yyyy HH:mm:ss"; //$NON-NLS-1$

	private static String formatPrice(final BigDecimal price) {
		// cu 2 zecimale fara delimitator (ex. 1000 – pentru 10 RON)
		return price.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_EVEN).toString(); //$NON-NLS-1$
	}
	
	private static String formatQuantity(final BigDecimal quantity) {
		// CANTITATE – cu 3 zecimale fara delimitator (ex. 1000 – pentru 1)
		return quantity.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_EVEN).toString(); //$NON-NLS-1$
	}
	
	@Override
	public boolean isECRSupported(final String ecrModel) {
		return ECR_MODEL_PARTNER.equalsIgnoreCase(ecrModel);
	}

	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId) {
		if (receipt == null || receipt.lines().isEmpty())
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(receipt.total(), BigDecimal.ZERO))
			return CompletableFuture.completedFuture(Result.ok());
		
		
		final StringBuilder ecrCommands = new StringBuilder();
		taxId.filter(StringUtils::notEmpty).ifPresent(tid -> ecrCommands.append("CF^"+tid).append(NEWLINE)); //$NON-NLS-1$
		ecrCommands.append(saleLines(receipt));
		
		/* close receipt
		 * P^TipPlata(1,2,3,4,5,…)^VALOARE 
		 */
		ecrCommands.append(MessageFormat.format("P^{0}^{1}", mapPaymentType(paymentType), //$NON-NLS-1$
				formatPrice(receipt.total())));
		
		return CompletableFuture.supplyAsync(new ReadResult(sendToEcr(ecrCommands)));
	}
	
	private String mapPaymentType(final PaymentType paymentType) {
		// Tipuri de plata: 1=Numerar, 2=Card, 3=Credit, 4=Tichet masa, 5=Tichet valoric, 6=Voucher, 7=Plata moderna, 
		// 8=Alte modalitati, 9=Alte modalitati
		switch (paymentType) {
		case CASH: return "1"; //$NON-NLS-1$
		case CARD: return "2"; //$NON-NLS-1$
		case CREDIT: return "3"; //$NON-NLS-1$
		case MEAL_TICKET: return "4"; //$NON-NLS-1$
		case VALUE_TICKET: return "5"; //$NON-NLS-1$
		case VOUCHER: return "6"; //$NON-NLS-1$
		case MODERN_PAYMENT: return "7"; //$NON-NLS-1$
		case OTHER: return "8"; //$NON-NLS-1$
		default:
			throw new IllegalArgumentException(NLS.bind(Messages.FiscalNetECRDriver_PaymentTypeError, paymentType));
		}
	}

	private String saleLines(final Receipt receipt)
	{
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		final StringBuilder ecrCommands = new StringBuilder();
		
		// sales
		ecrCommands.append(receipt.lines().stream()
				.map(line -> toEcrSale(line, prefs))
				.collect(Collectors.joining(NEWLINE, EMPTY_STRING, NEWLINE)));

		/* Subtotal
		 * Comanda Discount valoric
		 * DV^VALOARE
		 * Comanda Majorare valorica
		 * MV^VALOARE
		 */
		ecrCommands.append("ST^").append(NEWLINE); //$NON-NLS-1$
		if (receipt.allowanceCharge() != null)
			ecrCommands.append(MessageFormat.format("{0}^{1}", //$NON-NLS-1$
					safeString(receipt.allowanceCharge(), AllowanceCharge::chargeIndicator, i -> i ? "MV" : "DV"), //$NON-NLS-1$ //$NON-NLS-2$
					safeString(receipt.allowanceCharge(), AllowanceCharge::amountAbs, FiscalNetECRDriver::formatPrice)))
			.append(NEWLINE);

		return ecrCommands.toString();
	}
	
	private String toEcrSale(final ReceiptLine line, final IEclipsePreferences prefs)
	{
		/* Comanda Vanzare:
		 * S^DENUMIRE ARTICOL^PRET^CANTITATE^UM^GRTVA^GRDEP
		 * PRET – cu 2 zecimale fara delimitator (ex. 1000 – pentru 10 RON)
		 * CANTITATE – cu 3 zecimale fara delimitator (ex. 1000 – pentru 1)
		 * GRTVA – grupa de TVA (1,2,3,4,5)
		 * GRDEP – grupa de articole, daca modelul de casa suporta (1,2,3,4,5,…) daca nu se completeaza cu 1
		 */
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append(MessageFormat.format("S^{0}^{1}^{2}^{3}^{4}^{5}",  //$NON-NLS-1$
				line.name(),
				formatPrice(line.price()),
				formatQuantity(line.quantity()),
				line.uom(),
				safeString(line.taxCode(), prefs.get(PreferenceKey.FISCAL_NET_TAX_CODE, PreferenceKey.FISCAL_NET_TAX_CODE_DEF)),
				safeString(line.departmentCode(), prefs.get(PreferenceKey.FISCAL_NET_DEPT, PreferenceKey.FISCAL_NET_DEPT_DEF))));
		
		/* Comanda Discount valoric
		 * DV^VALOARE
		 * Comanda Majorare valorica
		 * MV^VALOARE
		 */
		if (line.allowanceCharge() != null)
			ecrCommands.append(NEWLINE).append(MessageFormat.format("{0}^{1}", //$NON-NLS-1$
					safeString(line.allowanceCharge(), AllowanceCharge::chargeIndicator, i -> i ? "MV" : "DV"), //$NON-NLS-1$ //$NON-NLS-2$
					safeString(line.allowanceCharge(), AllowanceCharge::amountAbs, FiscalNetECRDriver::formatPrice)));
		return ecrCommands.toString();
	}

	@Override
	public void reportZ() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("Z^"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}

	@Override
	public void reportX() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("X^"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}

	@Override
	public void reportD() {
		Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.FiscalNetECRDriver_Error,
				Messages.FiscalNetECRDriver_OperationNotPermitted));
	}

	@Override
	public CompletableFuture<Result> reportMF(final LocalDateTime reportStart, final LocalDateTime reportEnd,
			final String chosenDirectory) {
		final StringBuilder ecrCommands = new StringBuilder();

		ecrCommands.append(MessageFormat.format("RF^{0}^{1}^{2}",  //$NON-NLS-1$
				reportStart.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)),
				reportEnd.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)),
				chosenDirectory));
		return CompletableFuture.supplyAsync(new ReadResult(sendToEcr(ecrCommands)));
	}

	@Override
	public void cancelReceipt() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("VB^"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}
	
	private Optional<Path> sendToEcr(final StringBuilder ecrCommands)
	{
		try
		{
			final Bundle bundle = FrameworkUtil.getBundle(getClass());
			final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
			
			final String commandFolderPath = prefs.get(PreferenceKey.FISCAL_NET_COMMAND_FOLDER, PreferenceKey.FISCAL_NET_COMMAND_FOLDER_DEF);
			int i = 1;
			String filename = System.currentTimeMillis()+"_"+i+".txt"; //$NON-NLS-1$ //$NON-NLS-2$
			
			while (Files.exists(Paths.get(commandFolderPath, filename)))
				filename = System.currentTimeMillis()+"_"+ ++i+".txt"; //$NON-NLS-1$ //$NON-NLS-2$
			
			Files.write(Paths.get(commandFolderPath, filename), ecrCommands.toString().getBytes());
			final String responseFolderPath = prefs.get(PreferenceKey.FISCAL_NET_RESPONSE_FOLDER, PreferenceKey.FISCAL_NET_RESPONSE_FOLDER_DEF);
			return Optional.of(Paths.get(responseFolderPath, filename));
		}
		catch (final IOException e)
		{
			log.log(Level.SEVERE, "Error writting ecr commands", e); //$NON-NLS-1$
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.FiscalNetECRDriver_Error,
					NLS.bind(Messages.FiscalNetECRDriver_ErrorWrittingFileNLS, e.getMessage())));
			return Optional.empty();
		}
	}
	
	private static class ReadResult implements Supplier<Result> {
		private Optional<Path> resultPath;

		public ReadResult(final Optional<Path> resultPath) {
			this.resultPath = resultPath;
		}

		@Override
		public Result get() {
			if (resultPath.isEmpty())
				return Result.error(Messages.FiscalNetECRDriver_ErrorWrittingFile);
			
			try {
				while (Files.notExists(resultPath.get()))
					Thread.sleep(200);

				Thread.sleep(200);
			} catch (final InterruptedException e) {
			}
			
			/*
			 * Raspunsul contine 2 sau mai multe linii linii:
			 * Linia 1 - “BONOK=1” sau “BONOK=0” (1 = bon emis corect, 0 = bon incorect)
			 * Linia 2 - “NRBON=?” contine numarul bonului, in cazul in care acesta a fost emis corect sau “ERRCODE=?” – codul de eroare, in caz de eroare si
			 * Linia 3 - “ERRINFO=?” – info eroare
			 */
			try (Stream<String> resultLines = Files.lines(resultPath.get()))
			{
				final String[] lines = resultLines.toArray(String[]::new);
				
				if (lines[0].equalsIgnoreCase("BONOK=0")) //$NON-NLS-1$
					return Result.error(MessageFormat.format("{0}; {1}", lines[1], lines.length > 2 ? lines[2] : EMPTY_STRING)); //$NON-NLS-1$
				else
				{
					Files.deleteIfExists(resultPath.get());
					return Result.ok();
				}
			}
			catch (final IOException e)
			{
				return Result.error(e.getMessage());
			}
		}
	}
}
