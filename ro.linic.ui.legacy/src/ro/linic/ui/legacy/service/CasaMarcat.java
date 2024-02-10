package ro.linic.ui.legacy.service;

import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.isInDst;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.smallerThan;
import static ro.colibri.util.NumberUtils.truncate;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.util.Benchmarking;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;

public class CasaMarcat
{
	/**
	 * from the ecr documentation
	 */
	public static final int ECR_MAX_ITEM_NAME_LENGTH = 72;
	public static final String ECR_REPORT_DATE_PATTERN = "dd-MM-yy HH:mm:ss"; //DD-MM-YY hh:mm:ss DST
	
	public static final String ECR_FOLDER_KEY = "ecr_folder";
	public static final String ECR_FOLDER_DEFAULT = "C:/datecs/";
	
	public static final String ECR_OPERATOR_KEY = "ecr_operator";
	public static final String ECR_OPERATOR_DEFAULT = "1";
	
	public static final String ECR_PASSWORD_KEY = "ecr_password";
	public static final String ECR_PASSWORD_DEFAULT = "0001";
	
	public static final String ECR_NR_AMEF_KEY = "ecr_nr_amef";
	public static final String ECR_NR_AMEF_DEFAULT = "1";
	
	public static final String ECR_COTA_TVA_KEY = "ecr_cota_tva";
	public static final String ECR_COTA_TVA_DEFAULT = "1";
	
	public static final String ECR_DEPT_KEY = "ecr_departament";
	public static final String ECR_DEPT_DEFAULT = "1";
	
	public static final String ECR_IP_KEY = "ecr_ip";
	public static final String ECR_IP_DEFAULT = "127.0.0.1";
	
	public static final String ECR_PORT_KEY = "ecr_port";
	public static final String ECR_PORT_DEFAULT = "3999";
	
	private static final String RESULT_SUFFIX = "_result";
	private static final long RESULT_READ_TIMEOUT_MS = 30000;
	
	private static CasaMarcat instance;
	
	private Logger log;
	
	public static CasaMarcat instance(final Logger log)
	{
		if (instance == null)
			instance = new CasaMarcat(log);
		
		return instance;
	}
	
	private static AccountingDocument comasareBonuri(final Collection<AccountingDocument> docs)
	{
		final AccountingDocument comasat = new AccountingDocument();
		comasat.setPartner(docs.stream()
				.map(AccountingDocument::getPartner)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null));
		comasat.setGestiune(ClientSession.instance().getLoggedUser().getSelectedGestiune());
		comasat.setDoc(docs.stream()
				.map(AccountingDocument::getDoc)
				.distinct()
				.collect(Collectors.joining(LIST_SEPARATOR)));
		comasat.setNrDoc(docs.stream()
				.map(AccountingDocument::getNrDoc)
				.distinct()
				.collect(Collectors.joining(LIST_SEPARATOR)));
		comasat.setOperatiuni(docs.stream()
				.flatMap(AccountingDocument::getOperatiuni_Stream)
				.collect(toImmutableSet()));
		return comasat;
	}
	
	private CasaMarcat(final Logger log)
	{
		this.log = log;
	}
	
	private Path sendToEcr(final StringBuilder ecrCommands)
	{
		try
		{
			final String folderPath = System.getProperty(ECR_FOLDER_KEY, ECR_FOLDER_DEFAULT);
			int i = 0;
			String filename = "print_"+LocalDate.now().toString()+"_"+i+".in";
			
			while (Files.exists(Paths.get(folderPath+filename)))
				filename = "print_"+LocalDate.now().toString()+"_"+ ++i+".in";
			
			// the file structure will be:
			// line 1: ecr_ip
			// line 2: ecr_port
			// other lines: commands
			ecrCommands.insert(0, System.getProperty(ECR_PORT_KEY, ECR_PORT_DEFAULT)+NEWLINE);
			ecrCommands.insert(0, System.getProperty(ECR_IP_KEY, ECR_IP_DEFAULT)+NEWLINE);
			
			Files.write(Paths.get(folderPath+filename), ecrCommands.toString().getBytes());
			return Paths.get(folderPath+filename+RESULT_SUFFIX);
		}
		catch (final IOException e)
		{
			log.error(e);
			return null;
		}
	}
	
	public void incaseaza(final Collection<AccountingDocument> bonuri, final String cui, final boolean casaActive, final boolean prinCard,
			final boolean retriedBon)
	{
		final AccountingDocument bon = comasareBonuri(bonuri);
		
		if (bon.getOperatiuni().isEmpty())
			return;
		
		if (smallerThan(bon.getTotal(), BigDecimal.ZERO))
			return;
		
		if (casaActive)
		{
			Path resultPath;
			if (prinCard)
				resultPath = incaseazaPrinCard(bon, cui);
			else
				resultPath = incaseazaPrinCasa(bon, cui);
			
			Job.create("Scoate Bon Fiscal", new ReadResult(resultPath, bonuri.stream().map(AccountingDocument::getId).collect(toImmutableSet()),
					retriedBon)).schedule();
		}
	}
	
	private Path incaseazaPrinCasa(final AccountingDocument bon, final String cui)
	{
		final StringBuilder ecrCommands = addSaleLines(bon, cui);
		
		// close receipt
		ecrCommands.append("53,0[\\t][\\t]").append(NEWLINE);
		ecrCommands.append("56").append(NEWLINE);
		// update display
		ecrCommands.append("47, MULTUMIM ! [\\t]").append(NEWLINE);
		ecrCommands.append("35, VA MAI ASTEPTAM [\\t]");
		return sendToEcr(ecrCommands);
	}
	
	private Path incaseazaPrinCard(final AccountingDocument bon, final String cui)
	{
		final StringBuilder ecrCommands = addSaleLines(bon, cui);
		
		// close receipt
		ecrCommands.append("53,1[\\t][\\t]").append(NEWLINE);
		ecrCommands.append("56").append(NEWLINE);
		// update display
		ecrCommands.append("47, MULTUMIM ! [\\t]").append(NEWLINE);
		ecrCommands.append("35, VA MAI ASTEPTAM [\\t]");
		return sendToEcr(ecrCommands);
	}
	
	public void raportZ()
	{
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("69,Z[\\t]");
		sendToEcr(ecrCommands);
	}
	
	public void raportX()
	{
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("69,X[\\t]");
		sendToEcr(ecrCommands);
	}
	
	public void raportD()
	{
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("69,D[\\t]");
		sendToEcr(ecrCommands);
	}
	
	public void anulareBonFiscal()
	{
		final StringBuilder ecrCommands = new StringBuilder();
		ecrCommands.append("60");
		sendToEcr(ecrCommands);
	}
	
	/**
	 * @param reportDate
	 * @param chosenDirectory
	 * @return path of result file
	 */
	public Path raportMF(final LocalDate reportDate, final String chosenDirectory)
	{
		// DD-MM-YY hh:mm:ss DST
		final StringBuilder ecrCommands = new StringBuilder();
		final LocalDateTime startDateTime = reportDate.withDayOfMonth(1).atStartOfDay();
		final LocalDateTime endDateTime = reportDate.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
		
		final String dstStart = isInDst(startDateTime, "Europe/Bucharest") ? " DST" : EMPTY_STRING;
		final String dstEnd = isInDst(endDateTime, "Europe/Bucharest") ? " DST" : EMPTY_STRING;
		
		ecrCommands.append(MessageFormat.format("raportmf&{0}&{1}&{2}", 
				startDateTime.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)) + dstStart,
				endDateTime.format(DateTimeFormatter.ofPattern(ECR_REPORT_DATE_PATTERN)) + dstEnd,
				chosenDirectory));
		return sendToEcr(ecrCommands);
	}
	
	private StringBuilder addSaleLines(final AccountingDocument bon, final String cui)
	{
		final StringBuilder ecrCommands = new StringBuilder();
		
		// deschide bon
		ecrCommands.append(MessageFormat.format("48,{0}[\\t]{1}[\\t]{2}[\\t]{3}[\\t]{4}[\\t]", 
				System.getProperty(ECR_OPERATOR_KEY, ECR_OPERATOR_DEFAULT),
				System.getProperty(ECR_PASSWORD_KEY, ECR_PASSWORD_DEFAULT),
				System.getProperty(ECR_NR_AMEF_KEY, ECR_NR_AMEF_DEFAULT),
				isEmpty(cui) ? EMPTY_STRING : "I",
				safeString(cui)))
		.append(NEWLINE);
		
		// sales
		ecrCommands.append(bon.getOperatiuni().stream()
				.filter(op -> greaterThan(op.getCantitate(), BigDecimal.ZERO))
				.map(this::toEcrSale)
				.collect(Collectors.joining(NEWLINE, EMPTY_STRING, NEWLINE)));

		// subtotal(discount applies to this subtotal)
		// discount
		final Optional<String> discount = bon.getOperatiuni().stream()
		.filter(op -> smallerThan(op.getCantitate(), BigDecimal.ZERO))
		.map(op -> op.getCantitate().multiply(op.getPretVanzareUnitarCuTVA()).abs())
		.reduce(BigDecimal::add)
		.map(total -> safeString(truncate(total, 2), BigDecimal::toString));
		
		ecrCommands.append(MessageFormat.format("51,1[\\t]1[\\t]{0}[\\t]{1}[\\t]",
				discount.isPresent() ? "4" : EMPTY_STRING,
				discount.orElse(EMPTY_STRING)))
		.append(NEWLINE);

		return ecrCommands;
	}
	
	private String toEcrSale(final Operatiune op)
	{
		return MessageFormat.format("49,{0}[\\t]{1}[\\t]{2}[\\t]{3}[\\t][\\t][\\t]{4}[\\t]{5}[\\t]", 
				op.getName().substring(0, Math.min(ECR_MAX_ITEM_NAME_LENGTH, op.getName().length())),
				op.cotaTva().orElseGet(() -> System.getProperty(ECR_COTA_TVA_KEY, ECR_COTA_TVA_DEFAULT)),
				safeString(truncate(op.getPretVanzareUnitarCuTVA(), 2), BigDecimal::toString),
				safeString(truncate(op.getCantitate(), 2), BigDecimal::toString),
				op.dept().orElseGet(() -> System.getProperty(ECR_DEPT_KEY, ECR_DEPT_DEFAULT)),
				op.getUom());
	}
	
	private static class ReadResult implements ICoreRunnable
	{
		private Path resultPath;
		private String resultCode = "-1:Not run";
		private ImmutableSet<Long> docIds;
		private boolean retriedBon;
		
		public ReadResult(final Path resultPath, final ImmutableSet<Long> docIds, final boolean retriedBon)
		{
			this.resultPath = resultPath;
			this.docIds = docIds;
			this.retriedBon = retriedBon;
		}
	
		@Override public void run(final IProgressMonitor m) throws CoreException
		{
			final Optional<IProgressMonitor> monitor = Optional.ofNullable(m);
			if (resultPath == null)
			{
				monitor.ifPresent(mo -> mo.done());
				return;
			}
			
			try
			{
				final Benchmarking bm = Benchmarking.start();
				while (Files.notExists(resultPath))
				{
					if (bm.elapsedMillis() > RESULT_READ_TIMEOUT_MS)
					{
						BusinessDelegate.closeBonCasa_Failed(docIds);
						throw new UnsupportedOperationException("Timeout la citirea rezultatului: "+resultPath);
					}
						
					if (monitor.isPresent() && monitor.get().isCanceled())
					{
						BusinessDelegate.closeBonCasa_Failed(docIds);
						throw new UnsupportedOperationException("Interrupted by user: "+resultPath);
					}
					TimeUnit.MILLISECONDS.sleep(200);
				}
	
				TimeUnit.MILLISECONDS.sleep(200);
				try (Stream<String> resultLines = Files.lines(resultPath))
				{
					resultCode = resultLines.collect(Collectors.joining(NEWLINE));
					Display.getDefault().asyncExec(new Runnable()
					{
						@Override public void run()
						{
							if (!resultCode.trim().startsWith("0:"))
							{
								BusinessDelegate.closeBonCasa_Failed(docIds);
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare Casa Marcat", resultCode);
							}
							else
							{
								try
								{
									if (retriedBon)
										BusinessDelegate.closeBonCasa_RetrySuccess(docIds);
									
									Files.deleteIfExists(resultPath);
								}
								catch (final IOException e)
								{
									showException(e);
								}
							}
						}
					});
				}
				monitor.ifPresent(mo -> mo.done());
			}
			catch (final IOException | InterruptedException e)
			{
				BusinessDelegate.closeBonCasa_Failed(docIds);
				throw new UnsupportedOperationException(e);
			}
		}
	}
}
