package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.flexbiz.util.commons.StringUtils.notEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.layoutNoSpaces;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.cell.CheckBoxPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.BancaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CasaLoad;
import ro.colibri.entities.comercial.AccountingDocument.ContaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CoveredDocsLoad;
import ro.colibri.entities.comercial.AccountingDocument.DocumentTypesLoad;
import ro.colibri.entities.comercial.AccountingDocument.RPZLoad;
import ro.colibri.entities.comercial.Document;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ListUtils;
import ro.colibri.util.LocalDateUtils;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.http.pojo.Result;
import ro.linic.ui.legacy.anaf.AnafMoquiReporter;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.ReceptieEFacturaDialog;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.DocumentNatTable;
import ro.linic.ui.legacy.tables.DocumentNatTable.SourceLoc;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.widgets.ExportButton;
import ro.linic.ui.legacy.wizards.EFacturaDetailsWizardDialog;
import ro.linic.ui.legacy.wizards.EFacturaFileWizard;
import ro.linic.ui.security.services.AuthenticationSession;

public class EFacturaPart implements IMouseAction {
	public static final String PART_ID = "linic_gest_client.part.efactura"; //$NON-NLS-1$

	private static final String TABLE_STATE_PREFIX = "efactura.documents_nt"; //$NON-NLS-1$
	private static final String TABLE_REC_INV_STATE_PREFIX = "efactura.received_invoices_nt"; //$NON-NLS-1$
	private static final String VERTICAL_SASH_STATE_PREFIX = "efactura.vertical_sash"; //$NON-NLS-1$

	private static final Column idColumn = new Column(0, "id", "Id", 70);
	private static final Column supplierTaxIdColumn = new Column(1, "senderId", "Cif emitent", 120);
	private static final Column supplierNameColumn = new Column(2, "senderName", "Nume emitent", 220);
	private static final Column messageTypeColumn = new Column(3, "messageType", "Tip mesaj", 120);
	private static final Column issueDateColumn = new Column(4, "issueDate", "Data", 120);
	private static final Column detailsColumn = new Column(5, "details", "Mesaj", 300);
	private static final Column invoiceNumberColumn = new Column(6, "invoiceNumber", "Numar", 120);
	private static final Column totalColumn = new Column(7, "invoiceTotal", "ValCuTVA", 90);
	private static final Column invoiceIdColumn = new Column(8, "invoiceId", "Id factura", 70);
	private static final Column receivedColumn = new Column(9, "statusId", "Receptionat", 70);

	private static List<Column> REC_INV_COLUMNS = ImmutableList.<Column>builder().add(idColumn).add(supplierTaxIdColumn)
			.add(supplierNameColumn).add(messageTypeColumn).add(issueDateColumn).add(detailsColumn).add(invoiceNumberColumn)
			.add(totalColumn).add(invoiceIdColumn).add(receivedColumn).build();
	private static final String REC_INV_DATA_HOLDER = "EFacturaPart.anafEInvoices"; //$NON-NLS-1$

	private Combo partner;
	private Combo gestiune;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button execute;
	private DocumentNatTable table;
	private FullFeaturedNatTable<GenericValue> recInvTable;
	private SashForm verticalSash;

	private Button readAnafMessages;
	private Button receptie;
	private ExportButton printareDocs;

	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<Partner> allPartners;

	private Job contaJob;

	@Inject private MPart part;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private Logger log;
	@Inject private AuthenticationSession authSession;
	@Inject private DataServices dataServices;
	@Inject private IEclipseContext ctx;

	@PostConstruct
	public void createComposite(final Composite parent, @Named(IServiceConstants.ACTIVE_SHELL) final Shell shell) {
		if (shell != null && ClientSession.PROVIDER_URL_VALUE.contains("localhost"))
			shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		
		this.allGestiuni = BusinessDelegate.allGestiuni();
		this.allPartners = BusinessDelegate.allPartners();

		parent.setLayout(new GridLayout());

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(7, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		final Composite layoutPurpose = new Composite(container, SWT.NONE);
		layoutPurpose.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().span(2, 1).applyTo(layoutPurpose);
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText(Messages.Max);
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);

		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText(Messages.Today);
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);

		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText(Messages.ThisMonth);
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);

		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText(Messages.ThisYear);
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);

		new Label(container, SWT.NONE);// layout

		partner = new Combo(container, SWT.DROP_DOWN);
		partner.setItems(allPartners.stream().map(Partner::getName).toArray(String[]::new));
		UIUtils.setFont(partner);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(partner);

		gestiune = new Combo(container, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getImportName).toArray(String[]::new));
		UIUtils.setFont(gestiune);

		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);

		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);

		execute = new Button(container, SWT.PUSH | SWT.WRAP);
		execute.setText(Messages.Execute);
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);

		verticalSash = new SashForm(container, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).span(7, 1).applyTo(verticalSash);

		table = new DocumentNatTable(SourceLoc.EFACTURA, bundle, log, ctx);
		table.afterChange(doc -> part.setDirty(true));
		table.doubleClickAction(this);
		table.postConstruct(verticalSash);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(table.getTable());

		recInvTable = TableBuilder.with(GenericValue.class, REC_INV_COLUMNS, dataServices.holder(REC_INV_DATA_HOLDER).getData())
				.addConfiguration(new RecInvStyleConfiguration())
				.addClickListener(invoiceIdColumn, EFacturaPart.this::openInvoice)
				.addClickListener(idColumn, EFacturaPart.this::downloadMessage)
				.build(verticalSash);
		GridDataFactory.fillDefaults().applyTo(recInvTable.natTable());

		createBottom(container);

		loadVisualState();
		addListeners();
		loadData();
	}

	private void createBottom(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layoutNoSpaces(new GridLayout(5, false)));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 40).span(7, 1).applyTo(container);

		readAnafMessages = new Button(container, SWT.PUSH);
		readAnafMessages.setText(Messages.EFacturaPart_LoadAnafMessages);
		readAnafMessages.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		readAnafMessages.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(readAnafMessages);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.RIGHT, SWT.FILL).applyTo(readAnafMessages);
		
		receptie = new Button(container, SWT.PUSH);
		receptie.setText(Messages.AccountingPart_Receive);
		receptie.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		receptie.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(receptie);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(receptie);
		
		printareDocs = new ExportButton(container, SWT.PUSH,
				ImmutableList.of(Messages.Print, Messages.Email, "XML(UBL 2.1)", Messages.EInvoice), "down_0_inv"); // $NON-NLS-3$
																													// //$NON-NLS-5$
		printareDocs.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareDocs.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printareDocs);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(printareDocs);
	}

	@PreDestroy
	public void preDestroy() {
		cancelLoadJob();
	}

	@PersistState
	public void persistVisualState() {
		saveState(TABLE_STATE_PREFIX, table.getTable(), part);
		saveState(TABLE_REC_INV_STATE_PREFIX, recInvTable.natTable(), part);

		final int[] verticalWeights = verticalSash.getWeights();
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX + ".0", String.valueOf(verticalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX + ".1", String.valueOf(verticalWeights[1])); //$NON-NLS-1$
	}

	private void loadVisualState() {
		loadState(TABLE_STATE_PREFIX, table.getTable(), part);
		loadState(TABLE_REC_INV_STATE_PREFIX, recInvTable.natTable(), part);

		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer
				.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX + ".0", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalWeights[1] = Integer
				.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX + ".1", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalSash.setWeights(verticalWeights);
	}

	private void addListeners() {
		partner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				askSave();
				loadData();
			}
		});

		execute.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				askSave();
				loadData();
			}
		});
		
		readAnafMessages.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				askSave();
				AnafMoquiReporter.initializeSettingsOnDemand(ctx);
				RestCaller.post("/rest/s1/moqui-anaf-efactura/messages/check")
				.internal(authSession)
				.async(t -> UIUtils.showException(t, sync))
				.thenRun(() -> ctx.get(UISynchronize.class).asyncExec(() -> loadData()));
			}
		});

		receptie.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Optional<GenericValue> selAnafInvoice = recInvTable.selection().stream()
						.filter(gv -> "AnafRecMsgBillReceived".equalsIgnoreCase(gv.getString("messageType")))
						.findFirst();
				if (selAnafInvoice.isPresent())
					new ReceptieEFacturaDialog(receptie.getShell(), log, partService, authSession, selAnafInvoice.get(),
							RestCaller.get("/rest/s1/moqui-linic-legacy/anafInvoiceLines")
							.internal(authSession)
							.addUrlParam("systemMessageId", selAnafInvoice.get().getString("id"))
							.sync(Result.class, t -> UIUtils.showException(t, sync))
							.get().resultList())
					.open();
			}
		});

		printareDocs.addExportCallback(exportCode -> {
			switch (exportCode) {
			case 1: // Email
				sendSelectedDocToEmail();
				break;

			case 2: // XML(UBL)
				sendSelectedDocsToXmlUbl();
				break;

			case 3: // eFactura
				sendSelectedDocsTo_eFactura();
				break;

			case 0: // Printare
			default:
				printSelectedDocs();
				break;
			}
		});

		maxim.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
			}
		});

		ziCurenta.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
			}
		});

		lunaCurenta.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
			}
		});

		anCurent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
			}
		});
	}

	private void loadData() {
		cancelLoadJob();
		execute.setEnabled(false);
		
		contaJob = BusinessDelegate.filteredDocuments(new AsyncLoadData<Document>() {
			@Override
			public void success(final ImmutableList<Document> data) {
				part.setDirty(false);
				execute.setEnabled(true);
				table.loadData(data.stream()
						.filter(AccountingDocument.class::isInstance)
						.map(AccountingDocument.class::cast)
						.filter(accDoc -> AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
						.collect(ListUtils.toImmutableList()));
			}

			@Override
			public void error(final String details) {
				execute.setEnabled(true);
				MessageDialog.openError(execute.getShell(), Messages.ErrorFiltering, details);
			}
		}, sync, selectedGestiune().map(Gestiune::getId).orElse(null), selectedPartner().map(Partner::getId).orElse(null),
				TipDoc.VANZARE, extractLocalDate(from), extractLocalDate(to), RPZLoad.DOAR_RPZ, CasaLoad.INDIFERENT,
				BancaLoad.INDIFERENT, null, DocumentTypesLoad.FARA_DISCOUNTURI, CoveredDocsLoad.INDIFERENT,
				null, null, ContaLoad.INDIFERENT, null, null, log);

		RestCaller.get("/rest/s1/moqui-linic-legacy/anafInvoices/v2").internal(authSession)
				.addUrlParam("start", Timestamp.valueOf(extractLocalDate(from).atStartOfDay()).toString())
				.addUrlParam("end", Timestamp.valueOf(extractLocalDate(to).atTime(23, 59)).toString())
				.async(t -> UIUtils.showException(t, sync))
				.thenApply(rows -> rows.stream().sorted(Comparator.comparing(row -> row.getLong("issueDate"))).toList())
				.thenAccept(dataServices.holder(REC_INV_DATA_HOLDER)::setData);
	}

	private void cancelLoadJob() {
		if (contaJob != null)
			contaJob.cancel();
	}
	
	@Persist
	public void onSave() {
		if (part.isDirty()) {
			final ImmutableList<InvocationResult> results = table.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<Object>) dataChange.getKey()).map(key -> key.rowObject)
					.distinct().filter(AccountingDocument.class::isInstance).map(AccountingDocument.class::cast)
					.map(BusinessDelegate::mergeAccDoc).collect(toImmutableList());

			showResult(InvocationResult.flatMap(results));

			try (Stream<AccountingDocument> stream = results.stream().filter(InvocationResult::statusOk)
					.map(result -> (AccountingDocument) result.extra(InvocationResult.ACCT_DOC_KEY))
					.onClose(() -> table.getTable().refresh())) {
				stream.forEach(mergedAccDoc -> table.replace(mergedAccDoc, mergedAccDoc));
			}

			if (!results.stream().filter(InvocationResult::statusCanceled).findAny().isPresent())
				part.setDirty(false);
		}
	}

	@Override
	public void run(final NatTable table, final MouseEvent event) {
		// double click
		final Optional<AccountingDocument> selectedDoc = this.table.selectedAccDocs_Stream().findFirst();

		if (selectedDoc.isPresent() && !selectedDoc.get().getOperatiuni().isEmpty())
			ManagerPart.loadDocInPart(partService, selectedDoc.get());
	}
	
	private void openInvoice(final GenericValue row, final Object value) {
		if (value != null && NumberUtils.parseToLong(value.toString()) > 0)
			ManagerPart.loadDocInPart(partService, BusinessDelegate.reloadDoc(NumberUtils.parseToLong(value.toString())));
	}
	
	private void downloadMessage(final GenericValue row, final Object value) {
		if (value != null && notEmpty(value.toString()))
			AnafMoquiReporter.downloadResponse(ctx, value.toString());
	}

	private void sendSelectedDocToEmail() {
		final AccountingDocument docSelectat = table.selectedAccDocs_Stream().findFirst().orElse(null);

		if (docSelectat == null)
			return;

		try {
			if (docSelectat.isOfficialVanzariDoc()) {
				if (isEmpty(safeString(docSelectat.getPartner(), Partner::getDelegat, Delegat::getName))) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(),
							Messages.AccountingPart_MissingDelegate, Messages.AccountingPart_MissingDelegateMessage);
					return;
				}

				final boolean hasMailConfigured = Boolean
						.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
								.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
				if (!hasMailConfigured) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error,
							Messages.AccountingPart_SMTPError);
					return;
				}

				JasperReportManager.instance(bundle, log).printFactura_ClientDuplicate(bundle, docSelectat, null);
			} else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AccountingPart_WrongDoc,
						Messages.AccountingPart_OnlyInvoice);
		} catch (final IOException | JRException ex) {
			log.error(ex);
			showException(ex);
		}
	}

	private void sendSelectedDocsToXmlUbl() {
		table.selectedAccDocs_Stream().forEach(docSelectat -> {
			if (docSelectat.isOfficialVanzariDoc()) {
				if (isEmpty(safeString(docSelectat.getPartner(), Partner::getDelegat, Delegat::getName))) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(),
							Messages.AccountingPart_MissingDelegate, NLS.bind(Messages.AccountingPart_EnterDelegate,
									docSelectat.getDoc(), docSelectat.getNrDoc()));
					return;
				}

				final FileDialog chooser = new FileDialog(printareDocs.getShell(), SWT.SAVE);
				chooser.setFileName(Messages.Invoice_ + docSelectat.getNrDoc() + ".xml"); // $NON-NLS-2$
				final String filepath = chooser.open();

				if (isEmpty(filepath))
					return;

				new EFacturaDetailsWizardDialog(Display.getCurrent().getActiveShell(),
						new EFacturaFileWizard(log, docSelectat, filepath)).open();
			}
		});
	}

	private void sendSelectedDocsTo_eFactura() {
		AnafMoquiReporter.initializeSettingsOnDemand(ctx);
		
		final List<AccountingDocument> invoices = table.selectedAccDocs_Stream()
				.filter(accDoc -> TipDoc.VANZARE.equals(accDoc.getTipDoc())
						&& AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
				.collect(Collectors.toList());
		
		if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.AccountingPart_Report,
						MessageFormat.format(Messages.AccountingPart_ReportMessage, invoices.size())))
			return;

		invoices.forEach(invoice -> {
			if (isEmpty(safeString(invoice.getPartner(), Partner::getDelegat, Delegat::getName))) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						Messages.AccountingPart_MissingDelegate, NLS.bind(Messages.AccountingPart_EnterDelegate,
								invoice.getDoc(), invoice.getNrDoc()));
				return;
			}

			AnafMoquiReporter.reportInvoice(ctx, invoice.getCompany().getId(), invoice.getId());
		});
	}

	private void printSelectedDocs() {
		try {
			JasperReportManager.instance(bundle, log).printDocs(bundle, table.selectedAccDocs(), true);

			recInvTable.selection().stream().filter(gv -> "AnafRecMsgBillReceived".equalsIgnoreCase(gv.getString("messageType")))
			.forEach(invoice -> {
				try {
					AnafMoquiReporter.xmlToPdf(invoice.getString("rawXml"), invoice.getString("id"));
				} catch (final IOException e) {
					e.printStackTrace();
					showException(e);
				}
			});
		} catch (IOException | JRException ex) {
			log.error(ex);
			showException(ex);
		}
	}

	private Optional<Gestiune> selectedGestiune() {
		return allGestiuni.stream().filter(gest -> gest.getImportName().equalsIgnoreCase(gestiune.getText()))
				.findFirst();
	}

	private Optional<Partner> selectedPartner() {
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.of(allPartners.get(index));
	}
	
	private void askSave() {
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.Save,
				Messages.SaveMessage))
			onSave();
	}

	private static class RecInvStyleConfiguration extends AbstractRegistryConfiguration {
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry) {
			// Register Cell Painters
			final CheckBoxPainter checkboxPainter = new CheckBoxPainter();
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, checkboxPainter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + REC_INV_COLUMNS.indexOf(receivedColumn));

			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new LocalDateTimeDisplayConverter(LocalDateUtils.DATE_FORMATTER), DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + REC_INV_COLUMNS.indexOf(issueDateColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new ReceivedDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + REC_INV_COLUMNS.indexOf(receivedColumn));

			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			final Style centerAlignStyle = new Style();
			centerAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.CENTER);

			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + REC_INV_COLUMNS.indexOf(totalColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, centerAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + REC_INV_COLUMNS.indexOf(receivedColumn));
		}
	}
	
	private static class ReceivedDisplayConverter extends DisplayConverter {

	    @Override
	    public Object displayToCanonicalValue(final Object displayValue) {
	    	return displayValue;
	    }

	    @Override
	    public Object canonicalToDisplayValue(final Object canonicalValue) {
	    	if (canonicalValue instanceof Boolean)
	    		return canonicalValue;
	    	
	    	if (PresentationUtils.safeString(canonicalValue, Object::toString).equalsIgnoreCase("SmsgConfirmed"))
	    		return Boolean.TRUE;
	        return Boolean.FALSE;
	    }
	}
}
