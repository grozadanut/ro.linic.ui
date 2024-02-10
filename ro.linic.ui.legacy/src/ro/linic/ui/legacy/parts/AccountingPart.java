package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.NumberUtils.nullOrZero;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.layoutNoSpaces;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
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
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.security.Permissions;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.RulajPartener;
import ro.colibri.wrappers.TwoEntityWrapperHet;
import ro.linic.ui.legacy.anaf.AnafReporter;
import ro.linic.ui.legacy.anaf.ReceivedInvoice;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.DocumentNatTable;
import ro.linic.ui.legacy.tables.DocumentNatTable.SourceLoc;
import ro.linic.ui.legacy.tables.ReceivedInvoiceNatTable;
import ro.linic.ui.legacy.widgets.ExportButton;
import ro.linic.ui.legacy.wizards.EFacturaDetailsWizardDialog;
import ro.linic.ui.legacy.wizards.EFacturaFileWizard;

public class AccountingPart implements IMouseAction
{
	public static final String PART_ID = "linic_gest_client.part.accounting";
	
	private static final String TABLE_STATE_PREFIX = "accounting.documents_nt";
	private static final String TABLE_REC_INV_STATE_PREFIX = "accounting.received_invoices_nt";
	private static final String VERTICAL_SASH_STATE_PREFIX = "vanzari.vertical_sash";
	
	private static final String RAP_FISA_PARTENERI_CONTA = "Fisa pe Parteneri Contabila";
	private static final String RAP_ALL_DOCS = "Raport cu Toate Documentele";
	private static final String RAP_TVA_DOCS = "Raport Documente TVA";
	
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
	private ReceivedInvoiceNatTable recInvTable;
	private SashForm verticalSash;
	
	private Text tvaDePlata;
	private Button printFisaParteneri;
	private Button printAllDocs;
	private Button printTvaDocs;

	private Button salveaza;
	private Button closeAll;
	private Button openAll;
	private ExportButton printareDocs;
	
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<Partner> allPartners;
	
	@Inject private MPart part;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		this.allGestiuni = BusinessDelegate.allGestiuni();
		this.allPartners = BusinessDelegate.allPartners();
		
		parent.setLayout(new GridLayout());
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(7, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		new Label(container, SWT.NONE);//layout
		new Label(container, SWT.NONE);//layout
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText("Maxim");
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText("ZiCrt");
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText("LunaCrt");
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText("AnCrt");
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		new Label(container, SWT.NONE);//layout
		
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
		execute.setText("Executa filtrarea");
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);
		
		createResultArea(container);
		
	    verticalSash = new SashForm(container, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).span(7, 1).applyTo(verticalSash);
		
		table = new DocumentNatTable(SourceLoc.CONTABILITATE, bundle, log);
		table.afterChange(op -> part.setDirty(true));
		table.doubleClickAction(this);
		table.postConstruct(verticalSash);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(table.getTable());
		
		recInvTable = new ReceivedInvoiceNatTable();
		recInvTable.postConstruct(verticalSash);
		recInvTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(recInvTable.getTable());
		
		createBottom(container);
		
		loadVisualState();
		addListeners();
		loadData();
	}
	
	private void createResultArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(7, 1).applyTo(container);
		
		final Label tvaDePlataLabel = new Label(container, SWT.WRAP);
		tvaDePlataLabel.setText("TVA de plata");
		UIUtils.setBoldBannerFont(tvaDePlataLabel);
		
		tvaDePlata = new Text(container, SWT.READ_ONLY | SWT.SINGLE | SWT.BORDER);
		UIUtils.setBoldBannerFont(tvaDePlata);
		GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).applyTo(tvaDePlata);
		
		final Composite buttonsCont = new Composite(container, SWT.NONE);
		buttonsCont.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(buttonsCont);
		
		printFisaParteneri = new Button(buttonsCont, SWT.PUSH | SWT.WRAP);
		printFisaParteneri.setText(RAP_FISA_PARTENERI_CONTA);
		printFisaParteneri.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printFisaParteneri.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(printFisaParteneri);
		
		printAllDocs = new Button(buttonsCont, SWT.PUSH | SWT.WRAP);
		printAllDocs.setText(RAP_ALL_DOCS);
		printAllDocs.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printAllDocs.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(printAllDocs);
		
		printTvaDocs = new Button(buttonsCont, SWT.PUSH | SWT.WRAP);
		printTvaDocs.setText(RAP_TVA_DOCS);
		printTvaDocs.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printTvaDocs.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(printTvaDocs);
	}

	private void createBottom(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layoutNoSpaces(new GridLayout(4, false)));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 50).span(7, 1).applyTo(container);
		
		salveaza = new Button(container, SWT.PUSH);
		salveaza.setText("Salveaza");
		salveaza.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		salveaza.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(salveaza);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.RIGHT, SWT.FILL).applyTo(salveaza);
		
		closeAll = new Button(container, SWT.PUSH);
		closeAll.setText("Inchide toate documentele");
		closeAll.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		closeAll.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		closeAll.setEnabled(ClientSession.instance().hasStrictPermission(Permissions.SET_EDITABLE_ACC_DOCS));
		UIUtils.setBoldFont(closeAll);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(closeAll);
		
		openAll = new Button(container, SWT.PUSH);
		openAll.setText("Deschide toate documentele");
		openAll.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		openAll.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		openAll.setEnabled(ClientSession.instance().hasStrictPermission(Permissions.SET_EDITABLE_ACC_DOCS));
		UIUtils.setBoldFont(openAll);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(openAll);
		
		printareDocs = new ExportButton(container, SWT.PUSH, ImmutableList.of("Printare", "Email", "XML(UBL 2.1)", "eFactura"), "down_0_inv");
		printareDocs.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareDocs.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printareDocs);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(printareDocs);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(TABLE_STATE_PREFIX, table.getTable(), part);
		saveState(TABLE_REC_INV_STATE_PREFIX, recInvTable.getTable(), part);
		
		final int[] verticalWeights = verticalSash.getWeights();
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".0", String.valueOf(verticalWeights[0]));
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".1", String.valueOf(verticalWeights[1]));
	}
	
	private void loadVisualState()
	{
		loadState(TABLE_STATE_PREFIX, table.getTable(), part);
		loadState(TABLE_REC_INV_STATE_PREFIX, recInvTable.getTable(), part);
		
		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".0", "250"));
		verticalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".1", "150"));
		verticalSash.setWeights(verticalWeights);
	}
	
	private void addListeners()
	{
		partner.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetDefaultSelected(final SelectionEvent e)
			{
				askSave();
				loadData();
			}
		});
		
		execute.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				loadData();
			}
		});
		
		printFisaParteneri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printareFisaParteneri();
			}
		});
		
		printAllDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printAllDocs();
			}
		});
		
		printTvaDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printTvaDocs();
			}
		});
		
		salveaza.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		closeAll.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (!MessageDialog.openQuestion(closeAll.getShell(), "Inchideti tot", "Inchideti toate documentele incarcate?"))
					return;
					
				closeAll.setEnabled(false);
				openAll.setEnabled(false);
				
				final InvocationResult result = BusinessDelegate.setEditableToAllContaDocs(table.getSourceList().stream()
						.map(AccountingDocument.class::cast)
						.map(AccountingDocument::getId)
						.collect(toImmutableSet()), false);
				showResult(result);
				
				loadData();
				closeAll.setEnabled(true);
				openAll.setEnabled(true);
				
				if (result.statusOk())
					MessageDialog.openInformation(closeAll.getShell(), "Success", "Au fost inchise "+result.extraLong(InvocationResult.UPDATE_COUNT_KEY)+" documente");
			}
		});
		
		openAll.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (!MessageDialog.openQuestion(openAll.getShell(), "Deschideti tot", "Deschideti toate documentele incarcate?"))
					return;
					
				closeAll.setEnabled(false);
				openAll.setEnabled(false);
				
				final InvocationResult result = BusinessDelegate.setEditableToAllContaDocs(table.getSourceList().stream()
						.map(AccountingDocument.class::cast)
						.map(AccountingDocument::getId)
						.collect(toImmutableSet()), true);
				showResult(result);
				
				loadData();
				closeAll.setEnabled(true);
				openAll.setEnabled(true);

				if (result.statusOk())
					MessageDialog.openInformation(openAll.getShell(), "Success", "Au fost deschise "+result.extraLong(InvocationResult.UPDATE_COUNT_KEY)+" documente");
			}
		});
		
		printareDocs.addExportCallback(exportCode ->
		{
			switch (exportCode)
			{
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
		
		maxim.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
			}
		});
		
		ziCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
			}
		});
		
		lunaCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
			}
		});
		
		anCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
			}
		});
	}
	
	private void loadData()
	{
		execute.setEnabled(false);

		BusinessDelegate.contaDocs(new AsyncLoadData<AccountingDocument>()
		{
			@Override public void success(final ImmutableList<AccountingDocument> data)
			{
				part.setDirty(false);
				execute.setEnabled(true);
				table.loadData(data);
				
				tvaDePlata.setText(displayBigDecimal(data.stream()
						.filter(AccountingDocument::calculeazaLaTva)
						.map(AccountingDocument::tvaDePlata)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO)));
			}

			@Override public void error(final String details)
			{
				execute.setEnabled(true);
				MessageDialog.openError(execute.getShell(), "Eroare la filtrare", details);
			}
		}, sync, selectedGestiune().map(Gestiune::getId).orElse(null), selectedPartner().map(Partner::getId).orElse(null),
		extractLocalDate(from), extractLocalDate(to), log);
		
		AnafReporter.findReceivedInvoicesBetween(new AsyncLoadData<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>()
		{
			@Override public void success(final ImmutableList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> data)
			{
				recInvTable.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(execute.getShell(), "Eroare la filtrare Anaf", details);
			}
		}, sync, log, extractLocalDate(from), extractLocalDate(to));
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			final ImmutableList<InvocationResult> results = table.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<Object>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.filter(AccountingDocument.class::isInstance)
					.map(AccountingDocument.class::cast)
					.map(BusinessDelegate::mergeAccDoc)
					.collect(toImmutableList());

			showResult(InvocationResult.flatMap(results));
			
			try (Stream<AccountingDocument> stream = results.stream()
					.filter(InvocationResult::statusOk)
					.map(result -> (AccountingDocument) result.extra(InvocationResult.ACCT_DOC_KEY))
					.onClose(() -> table.getTable().refresh()))
			{
				stream.forEach(mergedAccDoc -> table.replace(mergedAccDoc, mergedAccDoc));
			}
			
			if (!results.stream().filter(InvocationResult::statusCanceled).findAny().isPresent())
				part.setDirty(false);
		}
	}
	
	@Override
	public void run(final NatTable table, final MouseEvent event)
	{
		// double click
		final Optional<AccountingDocument> selectedDoc = this.table.selectedAccDocs_Stream()
				.findFirst();
		
		if (selectedDoc.isPresent() && !selectedDoc.get().getOperatiuni().isEmpty())
			ManagerPart.loadDocInPart(partService, selectedDoc.get());
	}
	
	private void printareFisaParteneri()
	{
		printFisaParteneri.setEnabled(false);
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		final Long selPartnerId = selectedPartner().map(Partner::getId).orElse(null);
		BusinessDelegate.rulajeParteneriInConta(new AsyncLoadData<RulajPartener>()
		{
			@Override public void success(final ImmutableList<RulajPartener> data)
			{
				printFisaParteneri.setEnabled(true);
				try
				{
					JasperReportManager.instance(bundle, log).printFisaParteneri_Centralizat(bundle, fromDate, toDate,
							selPartnerId != null ? data : 
								data.stream()
								.filter(rulaj -> !nullOrZero(rulaj.getDeIncasat()) || !nullOrZero(rulaj.getDePlata()))
								.collect(toImmutableList()));
				}
				catch (IOException | JRException e)
				{
					log.error(e);
					showException(e);
				}
			}

			@Override public void error(final String details)
			{
				printFisaParteneri.setEnabled(true);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea raportului", details);
			}
		}, sync, selectedGestiune().map(Gestiune::getId).orElse(null), selPartnerId, fromDate, toDate, log);
	}
	
	private void printAllDocs()
	{
		printAllDocs.setEnabled(false);
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		try
		{
			JasperReportManager.instance(bundle, log).printJurnalGeneral(bundle, fromDate, toDate, 
					table.getSourceList().stream().map(AccountingDocument.class::cast).collect(toImmutableList()));
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
		printAllDocs.setEnabled(true);
	}
	
	private void printTvaDocs()
	{
		printTvaDocs.setEnabled(false);
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		try
		{
			JasperReportManager.instance(bundle, log).printDocTVA(bundle, fromDate, toDate, 
					table.getSourceList().stream()
					.map(AccountingDocument.class::cast)
					.filter(AccountingDocument::calculeazaLaTva)
					.collect(toImmutableList()));
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
		printTvaDocs.setEnabled(true);
	}
	
	private void sendSelectedDocToEmail()
	{
		final AccountingDocument docSelectat = table.selectedAccDocs_Stream()
				.findFirst()
				.orElse(null);
		
		if (docSelectat == null)
			return;
		
		try
		{
			if (docSelectat.isOfficialVanzariDoc())
			{
				if (isEmpty(safeString(docSelectat.getPartner(), Partner::getDelegat, Delegat::getName)))
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Delegat lipsa", "Introduceti un delegat pentru partenerul curent in Catalog Terti!");
					return;
				}

				final boolean hasMailConfigured = Boolean.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
						.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
				if (!hasMailConfigured)
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare", "MAIL_SMTP nu este configurat! Adresati-va administratorului de sistem pentru configurare!");
					return;
				}

				JasperReportManager.instance(bundle, log).printFactura_ClientDuplicate(bundle, docSelectat, null);
			}
			else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Doc gresit", "Momentan doar facturile de iesire se pot trimite prin email!");
		}
		catch (final IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
	
	private void sendSelectedDocsToXmlUbl()
	{
		table.selectedAccDocs_Stream().forEach(docSelectat ->
		{
			if (docSelectat.isOfficialVanzariDoc())
			{
				if (isEmpty(safeString(docSelectat.getPartner(), Partner::getDelegat, Delegat::getName)))
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Delegat lipsa", "Introduceti un delegat pentru "+docSelectat.getDoc()+" nr: "+docSelectat.getNrDoc());
					return;
				}

				final FileDialog chooser = new FileDialog(printareDocs.getShell(), SWT.SAVE);
				chooser.setFileName("Factura_"+docSelectat.getNrDoc()+".xml");
				final String filepath = chooser.open();

				if (isEmpty(filepath))
					return;

				new EFacturaDetailsWizardDialog(Display.getCurrent().getActiveShell(),
						new EFacturaFileWizard(log, docSelectat, filepath)).open();
			}
		});
	}
	
	private void sendSelectedDocsTo_eFactura()
	{
		if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Raporteaza", 
				MessageFormat.format("Doriti sa raportati {0} facturi la ANAF?",
						table.selectedAccDocs_Stream()
						.filter(accDoc -> TipDoc.VANZARE.equals(accDoc.getTipDoc()) && AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
						.count())))
			return;
		
		table.selectedAccDocs_Stream().forEach(docSelectat ->
		{
			if (TipDoc.VANZARE.equals(docSelectat.getTipDoc()) &&
					AccountingDocument.FACTURA_NAME.equalsIgnoreCase(docSelectat.getDoc()))
			{
				if (isEmpty(safeString(docSelectat.getPartner(), Partner::getDelegat, Delegat::getName)))
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Delegat lipsa", "Introduceti un delegat pentru "+docSelectat.getDoc()+" nr: "+docSelectat.getNrDoc());
					return;
				}
				
				AnafReporter.reportInvoice(docSelectat.getCompany().getId(), docSelectat.getId());
			}
		});
	}
	
	private void printSelectedDocs()
	{
		try
		{
			JasperReportManager.instance(bundle, log).printDocs(bundle, table.selectedAccDocs(), true);
			
			recInvTable.selection().forEach(wrap ->
			{
				try
				{
					AnafReporter.xmlToPdf(wrap.getEntity1().getXmlRaw(), wrap.getEntity1().getUploadIndex());
				}
				catch (final IOException e)
				{
					e.printStackTrace();
					showException(e);
				}
			});
		}
		catch (IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		return allGestiuni.stream()
				.filter(gest -> gest.getImportName().equalsIgnoreCase(gestiune.getText()))
				.findFirst();
	}
	
	private Optional<Partner> selectedPartner()
	{
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allPartners.get(index));
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Salveaza", "Salvati modificarile facute?"))
			onSave();
	}
}
