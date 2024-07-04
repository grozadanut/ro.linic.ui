package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.LocalDateUtils.isInDst;
import static ro.linic.util.commons.NumberUtils.smallerThan;
import static ro.linic.util.commons.NumberUtils.truncate;
import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.util.commons.PresentationUtils.safeString;
import static ro.linic.util.commons.StringUtils.truncate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ECRDriver;
import ro.linic.util.commons.StringUtils;

@Component
public class DudeECRDriver implements ECRDriver {
	private static final ILog log = UIUtils.logger(DudeECRDriver.class);
	private static final String RESULT_SUFFIX = "_result"; //$NON-NLS-1$
	private static final String ECR_REPORT_DATE_PATTERN = "dd-MM-yy HH:mm:ss"; //DD-MM-YY hh:mm:ss DST //$NON-NLS-1$
	private static final int ECR_MAX_ITEM_NAME_LENGTH = 72;
	private static final int ECR_MAX_ITEM_UOM_LENGTH = 6;
	
	private Function<StringBuilder, Optional<Path>> commandSender;
	private IEclipsePreferences prefs;

	@Override
	public boolean isECRSupported(final String ecrModel) {
		return ECR_MODEL_DATECS.equalsIgnoreCase(ecrModel);
	}
	
	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId) {
		if (receipt == null || receipt.getLines().isEmpty())
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(receipt.total(), BigDecimal.ZERO))
			return CompletableFuture.completedFuture(Result.ok());
		
		Optional<Path> resultPath;
		switch(paymentType) {
		case CASH:
			resultPath = printReceiptCash(receipt, taxId);
			break;
		case CARD:
			resultPath = printReceiptCard(receipt, taxId);
			break;
		default:
			return CompletableFuture.failedFuture(new IllegalArgumentException(NLS.bind(Messages.DudeECRDriver_IllegalPaymentType, paymentType)));
		}

		return CompletableFuture.supplyAsync(new ReadResult(resultPath));
	}
	
	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final Map<PaymentType, BigDecimal> payments,
			final Optional<String> taxId) {
		if (receipt == null || receipt.getLines().isEmpty())
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(receipt.total(), BigDecimal.ZERO))
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(payments.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO), receipt.total()))
			return CompletableFuture.failedFuture(new IllegalArgumentException(Messages.ECRDriver_PaymentSmallerThanTotalErr));
		
		final StringBuilder ecrCommands = addSaleLines(receipt, taxId);
		/* payments
		 * {PaidMode}<SEP>{Amount}<SEP>
		 */
		payments.entrySet().stream()
		.sorted(Comparator.comparing(Entry::getKey))
		.forEach(paymentEntry -> {
			ecrCommands.append(MessageFormat.format("53,{0}[\\t]{1}[\\t]", mapPaymentType(paymentEntry.getKey()), //$NON-NLS-1$
					safeString(truncate(paymentEntry.getValue(), 2), BigDecimal::toString)))
			.append(NEWLINE);
		});
		
		// close receipt
		ecrCommands.append("56").append(NEWLINE); //$NON-NLS-1$
		// update display
		ecrCommands.append("47, MULTUMIM ! [\\t]").append(NEWLINE);
		ecrCommands.append("35, VA MAI ASTEPTAM [\\t]");
		return CompletableFuture.supplyAsync(new ReadResult(sendToEcr(ecrCommands)));
	}
	
	private String mapPaymentType(final PaymentType paymentType) {
		/* PaidMode - Type of payment;
			o '0' – NUMERAR (CASH)
			o '1' – CARD
			o '2' – CREDIT
			o '3' – TICHETE MASA (MEAL VOUCHERS)
			o '4' – TICHETE VALORICE (VALUE TICKETS)
			o '5' – VOUCHER
			o '6' – PLATA MODERNA (MODERN PAYMENT)
			o '7' – CARD + AVANS IN NUMERAR (CASH IN ADVANCE)
			o '8' – ALTE METODE (OTHER METHODS)
			o '9' - Foreign currency
		 */
		switch (paymentType) {
		case CASH: return "0"; //$NON-NLS-1$
		case CARD: return "1"; //$NON-NLS-1$
		case CREDIT: return "2"; //$NON-NLS-1$
		case MEAL_TICKET: return "3"; //$NON-NLS-1$
		case VALUE_TICKET: return "4"; //$NON-NLS-1$
		case VOUCHER: return "5"; //$NON-NLS-1$
		case MODERN_PAYMENT: return "6"; //$NON-NLS-1$
		case OTHER: return "8"; //$NON-NLS-1$
		default:
			throw new IllegalArgumentException(NLS.bind(Messages.ECRDriver_PaymentTypeError, paymentType));
		}
	}
	
	private Optional<Path> printReceiptCash(final Receipt receipt, final Optional<String> taxId)
	{
		final StringBuilder ecrCommands = addSaleLines(receipt, taxId);
		
		// close receipt
		ecrCommands.append("53,0[\\t][\\t]").append(NEWLINE); //$NON-NLS-1$
		ecrCommands.append("56").append(NEWLINE); //$NON-NLS-1$
		// update display
		ecrCommands.append("47, MULTUMIM ! [\\t]").append(NEWLINE);
		ecrCommands.append("35, VA MAI ASTEPTAM [\\t]");
		return sendToEcr(ecrCommands);
	}
	
	private Optional<Path> printReceiptCard(final Receipt receipt, final Optional<String> taxId)
	{
		final StringBuilder ecrCommands = addSaleLines(receipt, taxId);
		
		// close receipt
		ecrCommands.append("53,1[\\t][\\t]").append(NEWLINE); //$NON-NLS-1$
		ecrCommands.append("56").append(NEWLINE); //$NON-NLS-1$
		// update display
		ecrCommands.append("47, MULTUMIM ! [\\t]").append(NEWLINE);
		ecrCommands.append("35, VA MAI ASTEPTAM [\\t]");
		return sendToEcr(ecrCommands);
	}
	
	@Override
	public void cancelReceipt() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("60"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}
	
	@Override
	public void reportZ() {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		
		final StringBuilder ecrCommands = new StringBuilder();
		if (prefs.getBoolean(PreferenceKey.DUDE_REPORT_Z_AND_D, PreferenceKey.DUDE_REPORT_Z_AND_D_DEF))
			ecrCommands.append("69,D[\\t]").append(NEWLINE); //$NON-NLS-1$
		ecrCommands.append("69,Z[\\t]"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}
	
	@Override
	public void reportX() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("69,X[\\t]"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}
	
	@Override
	public void reportD() {
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("69,D[\\t]"); //$NON-NLS-1$
		sendToEcr(ecrCommands);
	}
	
	@Override
	public CompletableFuture<Result> reportMF(final LocalDateTime reportStart, final LocalDateTime reportEnd, final String chosenDirectory) {
		// DD-MM-YY hh:mm:ss DST
		final StringBuilder ecrCommands = new StringBuilder();

		final String dstStart = isInDst(reportStart, "Europe/Bucharest") ? " DST" : EMPTY_STRING; //$NON-NLS-2$
		final String dstEnd = isInDst(reportEnd, "Europe/Bucharest") ? " DST" : EMPTY_STRING; //$NON-NLS-2$

		ecrCommands.append(MessageFormat.format("raportmf&{0}&{1}&{2}",  //$NON-NLS-1$
				reportStart.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)) + dstStart,
				reportEnd.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)) + dstEnd,
				chosenDirectory));
		return CompletableFuture.supplyAsync(new ReadResult(sendToEcr(ecrCommands)));
	}
	
	private Optional<Path> sendToEcr(final StringBuilder ecrCommands)
	{
		try
		{
			if (commandSender != null)
				return commandSender.apply(ecrCommands);
			
			final Bundle bundle = FrameworkUtil.getBundle(getClass());
			final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
			
			final String folderPath = prefs.get(PreferenceKey.DUDE_ECR_FOLDER, PreferenceKey.DUDE_ECR_FOLDER_DEF);
			int i = 0;
			String filename = "print_"+LocalDate.now().toString()+"_"+i+".in"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			while (Files.exists(Paths.get(folderPath, filename)))
				filename = "print_"+LocalDate.now().toString()+"_"+ ++i+".in"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			final String ecrIp = prefs.get(PreferenceKey.DUDE_ECR_IP, null);
			
			if (ecrIp == null) {
				Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.DudeECRDriver_ConfigError, Messages.DudeECRDriver_SetIp));
				return Optional.empty();
			}
			
			// the file structure will be:
			// line 1: ecr_ip
			// line 2: ecr_port
			// other lines: commands
			ecrCommands.insert(0, prefs.get(PreferenceKey.DUDE_ECR_PORT, PreferenceKey.DUDE_ECR_PORT_DEF)+NEWLINE);
			ecrCommands.insert(0, ecrIp+NEWLINE);
			
			Files.write(Paths.get(folderPath, filename), ecrCommands.toString().getBytes());
			return Optional.of(Paths.get(folderPath, filename+RESULT_SUFFIX));
		}
		catch (final IOException e)
		{
			log.error("Error writting ecr commands", e);
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.DudeECRDriver_Error,
					NLS.bind(Messages.DudeECRDriver_ErrorWrittingFileNLS, e.getMessage())));
			return Optional.empty();
		}
	}
	
	private StringBuilder addSaleLines(final Receipt receipt, final Optional<String> taxId)
	{
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = prefsOr(() -> ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName()));
		final StringBuilder ecrCommands = new StringBuilder();
		
		// Open fiscal receipt
		// {OpCode}<SEP>{OpPwd}<SEP>{TillNmb}<SEP>{Invoice}<SEP>{ClientTAXN}<SEP>{AirPortID}<SEP>
		ecrCommands.append(MessageFormat.format("48,{0}[\\t]{1}[\\t]{2}[\\t]{3}[\\t]{4}[\\t]",  //$NON-NLS-1$
				prefs.get(PreferenceKey.DUDE_ECR_OPERATOR, PreferenceKey.DUDE_ECR_OPERATOR_DEF),
				prefs.get(PreferenceKey.DUDE_ECR_PASSWORD, PreferenceKey.DUDE_ECR_PASSWORD_DEF),
				prefs.get(PreferenceKey.DUDE_ECR_NR_AMEF, PreferenceKey.DUDE_ECR_NR_AMEF_DEF),
				taxId.filter(StringUtils::notEmpty).map(id -> "I").orElse(EMPTY_STRING), //$NON-NLS-1$
				taxId.filter(StringUtils::notEmpty).orElse(EMPTY_STRING)))
		.append(NEWLINE);
		
		// sales
		ecrCommands.append(receipt.getLines().stream()
				.map(line -> toEcrSale(line, prefs))
				.collect(Collectors.joining(NEWLINE, EMPTY_STRING, NEWLINE)));

		// Subtotal
		// {Print}<SEP>{Display}<SEP>{DiscountType}<SEP>{DiscountValue}<SEP>
		ecrCommands.append(MessageFormat.format("51,1[\\t]1[\\t]{0}[\\t]{1}[\\t]", //$NON-NLS-1$
				Optional.ofNullable(receipt.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).map(i -> i?"3":"4").orElse(EMPTY_STRING), //$NON-NLS-1$ //$NON-NLS-2$
				safeString(receipt.getAllowanceCharge(), AllowanceCharge::amountAbs, amt -> truncate(amt, 2), BigDecimal::toString)))
		.append(NEWLINE);

		return ecrCommands;
	}
	
	private String toEcrSale(final ReceiptLine line, final IEclipsePreferences prefs)
	{
		// Registration of sale
		// {PluName}<SEP>{TaxCd}<SEP>{Price}<SEP>{Quantity}<SEP>{DiscountType}<SEP
		// >{DiscountValue}<SEP>{Department}<SEP>{Unit}<SEP>
		return MessageFormat.format("49,{0}[\\t]{1}[\\t]{2}[\\t]{3}[\\t]{4}[\\t]{5}[\\t]{6}[\\t]{7}[\\t]",  //$NON-NLS-1$
				truncate(line.getName(), ECR_MAX_ITEM_NAME_LENGTH),
				safeString(line.getTaxCode(), prefs.get(PreferenceKey.DUDE_ECR_TAX_CODE, PreferenceKey.DUDE_ECR_TAX_CODE_DEF)),
				safeString(truncate(line.getPrice(), 2), BigDecimal::toString),
				safeString(truncate(line.getQuantity(), 2), BigDecimal::toString),
				Optional.ofNullable(line.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).map(i -> i?"3":"4").orElse(EMPTY_STRING), //$NON-NLS-1$ //$NON-NLS-2$
				safeString(line.getAllowanceCharge(), AllowanceCharge::amountAbs, amt -> truncate(amt, 2), BigDecimal::toString),
				safeString(line.getDepartmentCode(), prefs.get(PreferenceKey.DUDE_ECR_DEPT, PreferenceKey.DUDE_ECR_DEPT_DEF)),
				truncate(line.getUom(), ECR_MAX_ITEM_UOM_LENGTH));
	}
	
	// ------ Testing purposes 
	public void setCommandSender(final Function<StringBuilder, Optional<Path>> commandSender) {
		this.commandSender = commandSender;
	}
	
	public void setPrefs(final IEclipsePreferences prefs) {
		this.prefs = prefs;
	}
	
	private IEclipsePreferences prefsOr(final Supplier<IEclipsePreferences> supplier) {
		return prefs != null ? prefs : supplier.get();
	}
	// ------- Testing purposes END
	
	private static class ReadResult implements Supplier<Result> {
		private Optional<Path> resultPath;

		public ReadResult(final Optional<Path> resultPath) {
			this.resultPath = resultPath;
		}

		@Override
		public Result get() {
			if (resultPath.isEmpty())
				return Result.error(Messages.DudeECRDriver_ErrorWrittingFile);
			
			try {
				while (Files.notExists(resultPath.get()))
					Thread.sleep(200);

				Thread.sleep(200);
			} catch (final InterruptedException e) {
			}
			
			try (Stream<String> resultLines = Files.lines(resultPath.get()))
			{
				final String resultCode = resultLines.collect(Collectors.joining(LIST_SEPARATOR));
				if (!resultCode.trim().startsWith("0:")) //$NON-NLS-1$
					return Result.error(resultCode);
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
