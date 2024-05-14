package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.add;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setBoldBannerFont;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Delegat;
import ro.colibri.embeddable.Verificat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.mappings.AccountingDocumentMapping;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.ConfirmDialog;
import ro.linic.ui.legacy.dialogs.ScheduleDialog;
import ro.linic.ui.legacy.dialogs.SendEmailDialog;
import ro.linic.ui.legacy.dialogs.VerifyDialog;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.TreeOperationsNatTable;
import ro.linic.ui.legacy.tables.TreeOperationsNatTable.SourceLoc;
import ro.linic.ui.legacy.widgets.ExportButton;

public class VerifyOperationsPart
{
	public static final String PART_ID = "linic_gest_client.part.verify_operations"; //$NON-NLS-1$
	
	private static final String INTRARI_TABLE_STATE_PREFIX = "verify_operations.intrari_tree_operations_nt"; //$NON-NLS-1$
	private static final String IESIRI_TABLE_STATE_PREFIX = "verify_operations.iesiri_tree_operations_nt"; //$NON-NLS-1$
	
	private TreeOperationsNatTable intrariTable;
	private TreeOperationsNatTable iesiriTable;
	private ExportButton printare;
	private Button schedule;
	private Button refresh;
	private Job loadJob;
	
	private String emailSignature;
	
	@Inject private MPart part;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject private Logger log;
	
	public static VerifyOperationsPart loadDoc(final EPartService partService, final AccountingDocument doc)
	{
		final MPart createdPart = partService.showPart(VerifyOperationsPart.PART_ID, PartState.ACTIVATE);
	
		if (createdPart == null)
			return null;
	
		final VerifyOperationsPart part = (VerifyOperationsPart) createdPart.getObject();
	
		if (part == null)
			return null;
		
		part.cancelLoadJob();
		part.insertDataInTables(doc.getOperatiuni());
		return part;
	}
	
	public static boolean isAutoTransfer_TransferLine(final Operatiune op)
	{
		return TipOp.IESIRE.equals(op.getTipOp()) &&
				op.getOwnerOp() != null &&
				!Objects.equals(op.getGestiune(), ClientSession.instance().getLoggedUser().getSelectedGestiune());
	}
	
	public static boolean isAutoTransfer_IesireLine(final Operatiune op)
	{
		return TipOp.IESIRE.equals(op.getTipOp()) &&
				op.getChildOp() != null &&
				Objects.equals(op.getChildOp().getAccDoc().getDoc(), AccountingDocument.TRANSFER_DOC_NAME);
	}
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		parent.setLayout(new GridLayout());
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		createTopBar(container);
		
		final Label intrariLabel = new Label(container, SWT.NONE);
		intrariLabel.setText(Messages.VerifyOperationsPart_Incoming);
		setBoldBannerFont(intrariLabel);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).applyTo(intrariLabel);
		
		intrariTable = new TreeOperationsNatTable(SourceLoc.VERIFY_OPERATIONS);
		intrariTable.doubleClickAction((t, e) -> 
		{
			this.intrariTable.selectedAccDoc()
			.ifPresent(selectedDoc -> ManagerPart.loadDocInPart(partService, selectedDoc));
		});
		intrariTable.setVerifyConsumer(rowObj -> verifyPressed(rowObj, intrariTable));
		intrariTable.postConstruct(container);
		intrariTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(intrariTable.getTable());
		loadState(INTRARI_TABLE_STATE_PREFIX, intrariTable.getTable(), part);
		
		final Label iesiriLabel = new Label(container, SWT.NONE);
		iesiriLabel.setText(Messages.VerifyOperationsPart_Outgoing);
		setBoldBannerFont(iesiriLabel);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).applyTo(iesiriLabel);
		
		iesiriTable = new TreeOperationsNatTable(SourceLoc.VERIFY_OPERATIONS);
		iesiriTable.doubleClickAction((t, e) -> 
		{
			this.iesiriTable.selectedAccDoc()
			.ifPresent(selectedDoc -> ManagerPart.loadDocInPart(partService, selectedDoc));
		});
		iesiriTable.setVerifyConsumer(rowObj -> verifyPressed(rowObj, iesiriTable));
		iesiriTable.postConstruct(container);
		iesiriTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(iesiriTable.getTable());
		loadState(IESIRI_TABLE_STATE_PREFIX, iesiriTable.getTable(), part);
		
		createBottom(container);
		
		makeInitialVerifications();
		addListeners();
	}
	
	private void createBottom(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		printare = new ExportButton(container, SWT.RIGHT, ImmutableList.of(Messages.Print, Messages.Email), "down_0_inv"); //$NON-NLS-3$
		printare.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		printare.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printare);
		
		schedule = new Button(container, SWT.PUSH);
		schedule.setText(Messages.Schedule);
		schedule.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		schedule.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(schedule);
		
		refresh = new Button(container, SWT.PUSH);
		refresh.setText(Messages.Refresh);
		UIUtils.setBoldFont(refresh);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(INTRARI_TABLE_STATE_PREFIX, intrariTable.getTable(), part);
		saveState(IESIRI_TABLE_STATE_PREFIX, iesiriTable.getTable(), part);
	}
	
	private void addListeners()
	{
		printare.addExportCallback(exportCode ->
		{
			switch (exportCode)
			{
			case 1: // Email
				sendDocToEmail();
				break;
				
			case 0: // Printare
			default:
				printDoc();
				break;
			}
		});
		
		schedule.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<AccountingDocument> selDoc = iesiriTable.selectedAccDoc();
				if (selDoc.isEmpty())
					return;

				final ScheduleDialog dialog = new ScheduleDialog(schedule.getShell(), sync, log, bundle,
						BusinessDelegate.reloadDoc(selDoc.get()));
				if (dialog.open() == Window.OK)
				{
					final AccountingDocument reloadedDoc = dialog.reloadedDoc();
					iesiriTable.getSourceList().removeIf(op -> op instanceof Operatiune &&
							((Operatiune) op).getAccDoc().equals(selDoc.get()));
					
					if (reloadedDoc != null)
						iesiriTable.getSourceList().addAll(reloadedDoc.getOperatiuni_Stream()
								.filter(op -> TipOp.IESIRE.equals(op.getTipOp()))
								.flatMap(AccountingDocument::mapForVerifying)
								.collect(toImmutableList()));
				}
			}
		});
		
		refresh.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData();
			}
		});
		
//		MessagingService.instance().registerMsgListener(VERIFY_OPS_TOPIC_REMOTE_JNDI, new ServerRefreshListener());
	}
	
	private void makeInitialVerifications()
	{
		emailSignature = BusinessDelegate.persistedProp(PersistedProp.EMAIL_SIGNATURE_KEY)
				.getValueOr(PersistedProp.EMAIL_SIGNATURE_DEFAULT);
		
		final ImmutableList<Operatiune> nextScheduledWork = BusinessDelegate.scheduleNextContractWork();
		notifyClients(nextScheduledWork);
//		final ImmutableList<Operatiune> unverifiedOperations = BusinessDelegate.verifyBirthdays();
//		table.loadData(nextScheduledWork.isEmpty() ? unverifiedOperations : nextScheduledWork);
		loadData();
	}
	
	private void notifyClients(final ImmutableList<Operatiune> nextScheduledWork)
	{
		nextScheduledWork.stream()
		.map(Operatiune::getAccDoc)
		.filter(accDoc -> accDoc.getPartner().isNotifyAppointment())
		.collect(Collectors.groupingBy(AccountingDocument::getPartner))
		.entrySet().stream()
		.forEach(this::openEmailDialog);
	}

	private void openEmailDialog(final Entry<Partner, List<AccountingDocument>> entry)
	{
		final String operationsText = entry.getValue().stream()
				.sorted(Comparator.comparing(AccountingDocument::getTransportDateTime, Comparator.nullsFirst(Comparator.naturalOrder())))
				.map(AccountingDocument::clientExplanation)
				.collect(Collectors.joining(NEWLINE));

		final String partnerName = safeString(entry.getKey(), Partner::getName);

		final StringBuilder messageSB = new StringBuilder();
		messageSB.append("Catre ").append(partnerName).append(NEWLINE).append(NEWLINE)
		.append("Buna ziua,").append(NEWLINE).append(NEWLINE)
		.append("Va rugam sa aveti in vedere programarea serviciilor dupa cum urmeaza:").append(NEWLINE)
		.append(operationsText).append(NEWLINE).append(NEWLINE)
		.append(emailSignature);

		SendEmailDialog.open(Display.getCurrent().getActiveShell(), log, 
				entry.getKey().getEmail(), Messages.VerifyOperationsPart_ScheduleServices, messageSB.toString(), null, EMPTY_STRING);
	}
	
	private void loadData()
	{
		cancelLoadJob();
		loadJob = BusinessDelegate.unverifiedOperations(new AsyncLoadData<Operatiune>()
		{
			@Override public void success(final ImmutableList<Operatiune> data)
			{
				insertDataInTables(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VerifyOperationsPart_ErrorLoading, details);
			}
		}, sync, log);
	}
	
	private void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
	}
	
	
	private void insertDataInTables(final Collection<Operatiune> data)
 	{
 		intrariTable.loadData(data.stream()
 				.filter(op -> TipOp.INTRARE.equals(op.getTipOp()))
				.flatMap(AccountingDocument::mapForVerifying)
 				.collect(toImmutableList()));
 		iesiriTable.loadData(data.stream()
 				.filter(op -> TipOp.IESIRE.equals(op.getTipOp()))
				.flatMap(AccountingDocument::mapForVerifying)
 				.collect(toImmutableList()));
 	}
	
	private void verifyPressed(final Object rowObject, final TreeOperationsNatTable table)
	{
		if (rowObject instanceof Operatiune)
		{
			final Operatiune op = (Operatiune) rowObject;
			final VerifyDialog verifyDialog = new VerifyDialog(Display.getCurrent().getActiveShell(), op);
			
			if (verifyDialog.open() == Window.OK)
				verifyDialog.getVerifiedOps().forEach(this::updateVerifiedOp);
		}
		else if (rowObject instanceof AccountingDocument)
		{
			final AccountingDocument accDoc = (AccountingDocument) rowObject;
			final ImmutableList<Operatiune> unverifiedOps = table.getSourceList().stream()
					.filter(Operatiune.class::isInstance)
					.map(Operatiune.class::cast)
					.filter(op -> op.getAccDoc().equals(accDoc))
					.filter(op -> op.isShouldVerify() && !op.isVerified())
					.collect(toImmutableList());
			
			final ConfirmDialog confirmDialog = new ConfirmDialog(Display.getCurrent().getActiveShell(),
					Messages.VerifyOperationsPart_VerifyAll,
					MessageFormat.format(Messages.VerifyOperationsPart_VerifyAllMess,
							unverifiedOps.size(),
							NEWLINE,
							unverifiedOps.stream()
								.sorted(TreeOperationsNatTable.OP_COMPARATOR)
								.map(Operatiune::verifyNicename)
								.collect(Collectors.joining(NEWLINE))));
			
			if (confirmDialog.open() == Window.OK)
			{
				final InvocationResult result = BusinessDelegate.verifyOperationsInBulk(unverifiedOps.stream()
						.flatMap(op -> isAutoTransfer_TransferLine(op) ? Stream.of(op, op.getOwnerOp()) : Stream.of(op))
						.map(Operatiune::getId)
						.collect(toImmutableSet()));
				showResult(result);
				if (result.statusOk())
				{
					intrariTable.getSourceList().removeAll(unverifiedOps);
					iesiriTable.getSourceList().removeAll(unverifiedOps);
				}
			}
		}
	}
	
	private void updateVerifiedOp(final Operatiune loadedOp)
	{
		final Predicate<? super Object> findFilter = sourceOp -> sourceOp instanceof Operatiune &&
				loadedOp.getId().equals(((Operatiune) sourceOp).getId());
		
		if (loadedOp.isVerified())
		{
			intrariTable.getSourceList().removeIf(findFilter);
			iesiriTable.getSourceList().removeIf(findFilter);
		}
		else // not fully verified
		{
			intrariTable.getSourceList().stream()
					.filter(findFilter)
					.map(Operatiune.class::cast)
					.findFirst()
					.ifPresent(foundIntrare -> foundIntrare.setVerificat(loadedOp.getVerificat()));
			intrariTable.getTable().refresh(false);
			
			iesiriTable.getSourceList().stream()
					.filter(findFilter)
					.map(Operatiune.class::cast)
					.findFirst()
					.ifPresent(foundIesire -> 
					{
						if (isAutoTransfer_TransferLine(foundIesire))
							foundIesire.setVerificat(Verificat.negateQuantity(loadedOp));
						else if (isAutoTransfer_IesireLine(loadedOp))
							foundIesire.setVerificat(new Verificat(null, null,
									add(Verificat::getVerificatCantitate,
											loadedOp.getVerificat(), loadedOp.getChildOp().getVerificat())));
						else
							foundIesire.setVerificat(loadedOp.getVerificat());
					});
			iesiriTable.getTable().refresh(false);
		}
	}
	
	private void printDoc()
	{
		final Optional<AccountingDocument> selIntrare = intrariTable.selectedAccDoc();
		final Optional<AccountingDocument> selIesire = iesiriTable.selectedAccDoc();
		final AccountingDocument docIncarcat = BusinessDelegate.reloadDoc(selIesire.orElse(selIntrare.orElse(null)));
		
		if (docIncarcat == null)
			return;
		
		try
		{
			if (TipDoc.VANZARE.equals(docIncarcat.getTipDoc()) && docIncarcat.isUnofficialDoc())
			{
				final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(docIncarcat);
				docIncarcat.setOperatiuni(new HashSet<Operatiune>(operatiuni));
			}
			
			JasperReportManager.instance(bundle, log).printDocs(bundle, ImmutableList.of(docIncarcat), false);
		}
		catch (final IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
	
	private void sendDocToEmail()
	{
		final Optional<AccountingDocument> selIntrare = intrariTable.selectedAccDoc();
		final Optional<AccountingDocument> selIesire = iesiriTable.selectedAccDoc();
		final AccountingDocument docIncarcat = BusinessDelegate.reloadDoc(selIesire.orElse(selIntrare.orElse(null)), true);
		
		if (docIncarcat == null)
			return;
		
		try
		{
			switch (docIncarcat.getTipDoc())
			{
			case CUMPARARE:
			case INCASARE:
			case PLATA:
			case VOUCHER:
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VerifyOperationsPart_WrongDoc, Messages.VerifyOperationsPart_WrongDocMess);
				break;

			case VANZARE:
				if (docIncarcat.isOfficialVanzariDoc())
				{
					if (isEmpty(safeString(docIncarcat.getPartner(), Partner::getDelegat, Delegat::getName)))
					{
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VerifyOperationsPart_DelegateMissing, Messages.VerifyOperationsPart_DelegateMissingMess);
						return;
					}
					
					final boolean hasMailConfigured = Boolean.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
							.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
					if (!hasMailConfigured)
					{
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error, Messages.VerifyOperationsPart_MailSMTPError);
						return;
					}
					
					JasperReportManager.instance(bundle, log).printFactura_ClientDuplicate(bundle, docIncarcat,
							docIncarcat.getPaidBy().stream().map(AccountingDocumentMapping::getPays).findFirst().orElse(null));
				}
				else
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VerifyOperationsPart_WrongDoc, Messages.VerifyOperationsPart_WrongDocMess);
				break;

			default:
				throw new UnsupportedOperationException(NLS.bind(Messages.DocTypeNotImpl, docIncarcat.getTipDoc()));
			}
		}
		catch (final IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
}
