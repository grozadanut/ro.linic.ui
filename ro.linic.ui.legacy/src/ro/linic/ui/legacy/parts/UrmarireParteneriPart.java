package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.layoutNoSpaces;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.localToDisplayLocation;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.BancaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CasaLoad;
import ro.colibri.entities.comercial.AccountingDocument.ContaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CoveredDocsLoad;
import ro.colibri.entities.comercial.AccountingDocument.DocumentTypesLoad;
import ro.colibri.entities.comercial.AccountingDocument.RPZLoad;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Document;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.InvocationResult.Problem;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.anaf.AnafReporter;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.dialogs.AdaugaDocDialog;
import ro.linic.ui.legacy.dialogs.ConexiuniDialog;
import ro.linic.ui.legacy.dialogs.SelectEntityDialog;
import ro.linic.ui.legacy.parts.components.UrmarireParteneriExtraFilters;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.RestCaller;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.DocumentNatTable;
import ro.linic.ui.legacy.tables.DocumentNatTable.SourceLoc;
import ro.linic.ui.legacy.widgets.ExportButton;
import ro.linic.ui.legacy.wizards.EFacturaDetailsWizardDialog;
import ro.linic.ui.legacy.wizards.EFacturaFileWizard;

public class UrmarireParteneriPart implements IMouseAction
{
	public static final String PART_ID = "linic_gest_client.part.urmarire_parteneri"; //$NON-NLS-1$
	
	private static final String DOCUMENTS_TABLE_STATE_PREFIX = "urmarire_parteneri.documents_nt"; //$NON-NLS-1$
	private static final String INDIFERENT = Messages.UrmarireParteneriPart_Indifferent;
	
	private static final String RAP_FISA_PARTENERI = Messages.UrmarireParteneriPart_PartnersTabRep;
	private static final String RAP_REG_INCASARI_PLATI = Messages.UrmarireParteneriPart_ReceiptsPaymentsRegRep;
	private static final String RAP_REG_CASA = Messages.UrmarireParteneriPart_ECRRep;
	private static final String RAP_JURNAL_GENERAL = Messages.UrmarireParteneriPart_GeneralRep;
	private static final String RAP_RPZ = Messages.UrmarireParteneriPart_DailyInventoryRep;
	private static final String RAP_PRINT_FILTRU = Messages.UrmarireParteneriPart_PrintFilter;
	private static final String RAP_REG_BANCA = Messages.UrmarireParteneriPart_BankReg;
	private static final String RAP_REG_ANAF = Messages.UrmarireParteneriPart_AnafReg;
	private static final String RAP_INV_PROSOFT = Messages.UrmarireParteneriPart_RapInvProsoft;
	private static final ImmutableList<String> ALL_RAPOARTE = ImmutableList.of(RAP_FISA_PARTENERI, RAP_REG_INCASARI_PLATI, RAP_REG_CASA,
			RAP_JURNAL_GENERAL, RAP_RPZ, RAP_PRINT_FILTRU, RAP_REG_BANCA, RAP_REG_ANAF, RAP_INV_PROSOFT);
	
	private Button goleste;
	private List gestiuni;
	private List operatii;
	private Combo parteneri;
	private List rapoarte;
	private Button printareRaport;
	
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button doarRpz;
	private Button faraRpz;
	private Button indiferentRpz;
	private Button doarCasa;
	private Button faraCasa;
	private Button indiferentCasa;
	private Button doarDiscounturi;
	private Button faraDiscounturi;
	private Button indiferentDiscounturi;
	private Button doarCovered;
	private Button faraCovered;
	private Button indiferentCovered;
	private Button extraFilters;
	private UrmarireParteneriExtraFilters extraFilterPopup;
	private Button executaFiltrarea;
	
	private Button adauga;
	private Button salveaza;
	private Button conexiuni;
	private Button sterge;
	private Button gdpr;
	private ExportButton printareDocs;
	private Button printareDocsComasat;
	
	private DocumentNatTable table;
	
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<Partner> allPartners;
	
	private AdaugaDocDialog adaugaDocDialog;
	private Job loadJob;
	private Job raportJob;
	
	@Inject private MPart part;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject private Logger log;
	
	public static void openPart(final EPartService partService)
	{
		partService.showPart(PART_ID, PartState.ACTIVATE);
	}
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		this.allGestiuni = BusinessDelegate.allGestiuni().stream()
				.sorted(Comparator.comparing(Gestiune::getName))
				.collect(toImmutableList());
		
		parent.setLayout(layoutNoSpaces(new GridLayout()));
		parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		
		createTop(parent);
		table = new DocumentNatTable(SourceLoc.URMARIRE_PARTENERI, bundle, log);
		table.afterChange(op -> part.setDirty(true));
		table.doubleClickAction(this);
		table.postConstruct(parent);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getTable());
		loadState(DOCUMENTS_TABLE_STATE_PREFIX, table.getTable(), part);
		createBottom(parent);
		
		reloadPartners();
		addListeners();
	}
	
	@PreDestroy
	public void preDestroy()
	{
		cancelLoadJob();
		cancelRaportJob();
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(DOCUMENTS_TABLE_STATE_PREFIX, table.getTable(), part);
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
	
	private void createTop(final Composite parent)
	{
		final Color bgColor = ClientSession.instance().getLoggedUser().isHideUnofficialDocs() ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layoutNoSpaces(new GridLayout(6, false)));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
//		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 280).applyTo(container);
		container.setBackground(bgColor);
		ClientSession.instance().addHideStateControl(container);
		
		goleste = new Button(container, SWT.PUSH | SWT.WRAP);
		goleste.setText(Messages.UrmarireParteneriPart_Empty);
		goleste.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		goleste.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(goleste);
		GridDataFactory.fillDefaults().hint(25, SWT.DEFAULT).span(1, 3).applyTo(goleste);
		
		final Label gestiuniLabel = new Label(container, SWT.NONE);
		gestiuniLabel.setText(Messages.UrmarireParteneriPart_Inventories);
		gestiuniLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(gestiuniLabel);
		
		final Label parteneriLabel = new Label(container, SWT.NONE);
		parteneriLabel.setText(Messages.UrmarireParteneriPart_Partners);
		parteneriLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(parteneriLabel);
		
		final Label operatiiLabel = new Label(container, SWT.NONE);
		operatiiLabel.setText(Messages.UrmarireParteneriPart_Ops);
		operatiiLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(operatiiLabel);
		
		final Composite filtersContainer = createFiltersArea(container);
		
		rapoarte = new List(container, SWT.SINGLE | SWT.BORDER);
		rapoarte.setItems(ALL_RAPOARTE.toArray(new String[] {}));
		rapoarte.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
		UIUtils.setFont(rapoarte);
		GridDataFactory.fillDefaults().grab(false, true).span(1, 2).applyTo(rapoarte);
		
		gestiuni = new List(container, SWT.SINGLE | SWT.BORDER);
		gestiuni.setItems(createGestiuniArray());
		selectGest(ClientSession.instance().getLoggedUser().getSelectedGestiune());
		gestiuni.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		UIUtils.setFont(gestiuni);
		GridDataFactory.fillDefaults().grab(false, true).span(1, 2).applyTo(gestiuni);
		
		parteneri = new Combo(container, SWT.SIMPLE);
		parteneri.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		UIUtils.setFont(parteneri);
		GridDataFactory.fillDefaults().grab(false, true).span(1, 2).hint(300, SWT.DEFAULT).applyTo(parteneri);
		
		operatii = new List(container, SWT.SINGLE | SWT.BORDER);
		operatii.setItems(createOperatiiArray());
		operatii.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		UIUtils.setFont(operatii);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).span(1, 2).applyTo(operatii);
		
		executaFiltrarea = new Button(container, SWT.PUSH);
		executaFiltrarea.setText(Messages.Execute);
		executaFiltrarea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		executaFiltrarea.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(executaFiltrarea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(executaFiltrarea);
		
		printareRaport = new Button(container, SWT.PUSH);
		printareRaport.setText(Messages.UrmarireParteneriPart_PrintRep);
		printareRaport.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		printareRaport.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printareRaport);
		GridDataFactory.fillDefaults().applyTo(printareRaport);
		
		final int topHeight = filtersContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT).y +
				printareRaport.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, topHeight).applyTo(container);
	}
	
	private Composite createFiltersArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).span(1, 2).applyTo(container);
		
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
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		
		final Composite rpzGroup = new Composite(container, SWT.NONE);
		rpzGroup.setLayout(new GridLayout());
		rpzGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(rpzGroup);
		
		doarRpz = new Button(rpzGroup, SWT.RADIO);
		doarRpz.setText(Messages.UrmarireParteneriPart_RPZOnly);
		doarRpz.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarRpz);
		
		faraRpz = new Button(rpzGroup, SWT.RADIO);
		faraRpz.setText(Messages.UrmarireParteneriPart_NoRPZ);
		faraRpz.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(faraRpz);

		indiferentRpz = new Button(rpzGroup, SWT.RADIO);
		indiferentRpz.setText(Messages.UrmarireParteneriPart_Indifferent);
		indiferentRpz.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(indiferentRpz);

		final Composite casaGroup = new Composite(container, SWT.NONE);
		casaGroup.setLayout(new GridLayout());
		casaGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(casaGroup);
		
		doarCasa = new Button(casaGroup, SWT.RADIO);
		doarCasa.setText(Messages.UrmarireParteneriPart_ECROnly);
		doarCasa.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarCasa);
		
		faraCasa = new Button(casaGroup, SWT.RADIO);
		faraCasa.setText(Messages.UrmarireParteneriPart_NoECR);
		faraCasa.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(faraCasa);
		
		indiferentCasa = new Button(casaGroup, SWT.RADIO);
		indiferentCasa.setText(Messages.UrmarireParteneriPart_Indifferent);
		indiferentCasa.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().applyTo(indiferentCasa);
		
		final Composite discounturiGroup = new Composite(container, SWT.NONE);
		discounturiGroup.setLayout(new GridLayout());
		discounturiGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(discounturiGroup);
		
		doarDiscounturi = new Button(discounturiGroup, SWT.RADIO);
		doarDiscounturi.setText(Messages.UrmarireParteneriPart_DiscOnly);
		doarDiscounturi.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarDiscounturi);
		
		faraDiscounturi = new Button(discounturiGroup, SWT.RADIO);
		faraDiscounturi.setText(Messages.UrmarireParteneriPart_NoDisc);
		faraDiscounturi.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(faraDiscounturi);
		
		indiferentDiscounturi = new Button(discounturiGroup, SWT.RADIO);
		indiferentDiscounturi.setText(Messages.UrmarireParteneriPart_Indifferent);
		indiferentDiscounturi.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(indiferentDiscounturi);
		
		final Composite coveredGroup = new Composite(container, SWT.NONE);
		coveredGroup.setLayout(new GridLayout());
		coveredGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(coveredGroup);
		
		doarCovered = new Button(coveredGroup, SWT.RADIO);
		doarCovered.setText(Messages.UrmarireParteneriPart_OnlyCovered);
		doarCovered.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarCovered);
		
		faraCovered = new Button(coveredGroup, SWT.RADIO);
		faraCovered.setText(Messages.UrmarireParteneriPart_NoCovered);
		faraCovered.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(faraCovered);
		
		indiferentCovered = new Button(coveredGroup, SWT.RADIO);
		indiferentCovered.setText(Messages.UrmarireParteneriPart_Indifferent);
		indiferentCovered.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(indiferentCovered);
		
		extraFilters = new Button(container, SWT.PUSH);
		extraFilters.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		extraFilters.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().span(4, 1).applyTo(extraFilters);
		UIUtils.setFont(extraFilters);
		extraFilterPopup = new UrmarireParteneriExtraFilters(extraFilters.getShell(), extraFilters);
		return container;
	}

	private void createBottom(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layoutNoSpaces(new GridLayout(7, false)));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 50).applyTo(container);
		
		adauga = new Button(container, SWT.PUSH);
		adauga.setText(Messages.Add);
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(adauga);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(adauga);
		
		salveaza = new Button(container, SWT.PUSH);
		salveaza.setText(Messages.Save);
		salveaza.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		salveaza.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(salveaza);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(salveaza);
		
		conexiuni = new Button(container, SWT.PUSH);
		conexiuni.setText(Messages.UrmarireParteneriPart_Connections);
		conexiuni.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		conexiuni.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(conexiuni);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(conexiuni);
		
		sterge = new Button(container, SWT.PUSH);
		sterge.setText(Messages.Delete);
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(sterge);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(sterge);
		
		gdpr = new Button(container, SWT.PUSH);
		gdpr.setText(Messages.GDPR);
		gdpr.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		gdpr.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(gdpr);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.RIGHT, SWT.FILL).applyTo(gdpr);
		
		printareDocsComasat = new Button(container, SWT.PUSH);
		printareDocsComasat.setText(Messages.UrmarireParteneriPart_MergePrint);
		printareDocsComasat.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareDocsComasat.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printareDocsComasat);
		GridDataFactory.swtDefaults().grab(false, true).align(SWT.RIGHT, SWT.FILL).applyTo(printareDocsComasat);
		
		printareDocs = new ExportButton(container, SWT.RIGHT, ImmutableList.of(Messages.Print, Messages.Email, "XML(UBL 2.1)", Messages.EInvoice), "down_0_inv"); //$NON-NLS-3$ //$NON-NLS-5$
		printareDocs.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareDocs.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printareDocs);
		GridDataFactory.swtDefaults().grab(false, true).align(SWT.RIGHT, SWT.FILL).applyTo(printareDocs);
	}
	
	private void addListeners()
	{
		goleste.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				clearAllFields();
			}
		});
		
		parteneri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetDefaultSelected(final SelectionEvent e)
			{
				if (adaugaDocDialog != null && adaugaDocDialog.getShell() != null)
					adaugaDocDialog.changedSelectedPartner(selectedPartner());
				
				if (selectedPartner().isPresent())
					executaFiltrarea();
			}
		});
		
		operatii.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (adaugaDocDialog != null && adaugaDocDialog.getShell() != null)
					adaugaDocDialog.changedTipDoc(selectedTipDoc());
				
				if (selectedPartner().isPresent())
					executaFiltrarea();
			}
		});
		
		executaFiltrarea.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				executaFiltrarea();
			}
		});
		
		printareRaport.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printareRaport();
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
		
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedPartner().isPresent() && selectedTipDoc().isPresent())
				{
					final Point adaugaLoc = localToDisplayLocation(adauga);
					adaugaDocDialog = new AdaugaDocDialog(adauga.getShell(), sync, new Point(adaugaLoc.x, adaugaLoc.y-220), 
							selectedPartner().get(), selectedTipDoc().get(), table::add, log);
					adaugaDocDialog.open();
				}
			}
		});
		
		salveaza.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		conexiuni.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<AccountingDocument> selAccDoc = table.selectedAccDocs_Stream().findFirst();
				if (selAccDoc.isPresent())
				{
					TipDoc tipDoc;
					if (TipDoc.CUMPARARE.equals(selAccDoc.get().getTipDoc()))
						tipDoc = TipDoc.PLATA;
					else if (TipDoc.VANZARE.equals(selAccDoc.get().getTipDoc()))
						tipDoc = TipDoc.INCASARE;
					else if (TipDoc.PLATA.equals(selAccDoc.get().getTipDoc()))
						tipDoc = TipDoc.CUMPARARE;
					else if (TipDoc.INCASARE.equals(selAccDoc.get().getTipDoc()))
						tipDoc = TipDoc.VANZARE;
					else
						throw new UnsupportedOperationException(NLS.bind(Messages.DocTypeNotImpl, selAccDoc.get().getTipDoc()));
					
					final LocalDate migrationDate = LocalDate.parse(BusinessDelegate.persistedProp(PersistedProp.MIGRATION_DATE_KEY)
							.getValueOr(PersistedProp.MIGRATION_DATE_DEFAULT));
					conexiuni.setEnabled(false);
					BusinessDelegate.filteredDocuments(new AsyncLoadData<Document>()
					{
						@Override public void success(final ImmutableList<Document> data)
						{
							conexiuni.setEnabled(true);
							final ImmutableList<AccountingDocument> partnerAccDocs = data.stream()
									.map(AccountingDocument.class::cast)
									.collect(toImmutableList());
							new ConexiuniDialog(conexiuni.getShell(), selAccDoc.get(), partnerAccDocs, bundle, log).open();
						}
						
						@Override public void error(final String details)
						{
							conexiuni.setEnabled(true);
							MessageDialog.openError(conexiuni.getShell(), Messages.Error, details);
						}
					}, sync, null, selAccDoc.get().getPartner().getId(), tipDoc, migrationDate, POSTGRES_MAX.toLocalDate(), RPZLoad.INDIFERENT,
					CasaLoad.INDIFERENT, BancaLoad.INDIFERENT, null, DocumentTypesLoad.FARA_DISCOUNTURI, CoveredDocsLoad.FARA_COVERED,
					null, null, ContaLoad.INDIFERENT, null, null, log);
				}
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (!MessageDialog.openQuestion(sterge.getShell(), Messages.UrmarireParteneriPart_DeleteDocs, Messages.UrmarireParteneriPart_DeleteDocsMessage))
					return;
				
				showResult(InvocationResult.flatMap(table.selection().stream()
						.filter(Document.class::isInstance)
						.map(doc -> 
						{
							InvocationResult result = InvocationResult.canceled(Problem.code("UPPart435") //$NON-NLS-1$
									.description(NLS.bind(Messages.UrmarireParteneriPart_DocClassError, doc.getClass().getSimpleName())));
							if (doc instanceof AccountingDocument)
								result = BusinessDelegate.deleteAccDoc(((AccountingDocument) doc).getId());
							else if (doc instanceof DocumentWithDiscount)
								result = BusinessDelegate.deleteDiscountDoc(((DocumentWithDiscount) doc).getId());

							if (result.statusOk())
								table.remove(doc);

							return result;
						})
						.collect(toImmutableList())));
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
		
		printareDocsComasat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log).printDocs(bundle, JasperReportManager.comasarePtPrint(table.selectedAccDocs(), true), false);
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});
		
		gdpr.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printGDPR(bundle, selectedPartner().map(Partner::getName).orElse("....................................")); //$NON-NLS-1$
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});
	}
	
	private void executaFiltrarea()
	{
		cancelLoadJob();
		executaFiltrarea.setEnabled(false);
		askSave();

		loadJob = BusinessDelegate.filteredDocuments(new AsyncLoadData<Document>()
		{
			@Override public void success(final ImmutableList<Document> data)
			{
				part.setDirty(false);
				executaFiltrarea.setEnabled(true);
				table.loadData(data);
			}

			@Override public void error(final String details)
			{
				executaFiltrarea.setEnabled(true);
				MessageDialog.openError(executaFiltrarea.getShell(), Messages.ErrorFiltering, details);
			}
		}, sync, selectedGestiune().map(Gestiune::getId).orElse(null), selectedPartner().map(Partner::getId).orElse(null),
		selectedTipDoc().orElse(null), extractLocalDate(from), extractLocalDate(to), selectedRpzLoad(), selectedCasaLoad(),
		extraFilterPopup.getBancaLoad(), extraFilterPopup.getContBancarId(), selectedDiscounturiLoad(), selectedCoveredLoad(),
		extraFilterPopup.getShouldTransport(), extraFilterPopup.getUserId(), extraFilterPopup.getContaLoad(),
		extraFilterPopup.getTransportFrom(), extraFilterPopup.getTransportTo(), log);
	}
	
	private void printareRaport()
	{
		if (!selectedRaportType().isPresent())
			return;
		
		cancelRaportJob();
		final String raportType = selectedRaportType().get();
		
		if (raportType.equalsIgnoreCase(RAP_FISA_PARTENERI))
			printareFisaParteneri();
		else if (raportType.equalsIgnoreCase(RAP_REG_INCASARI_PLATI))
			printareRegIncasariPlati();
		else if (raportType.equalsIgnoreCase(RAP_REG_CASA))
			printareRegCasa();
		else if (raportType.equalsIgnoreCase(RAP_JURNAL_GENERAL))
			printareJurnalGeneral();
		else if (raportType.equalsIgnoreCase(RAP_RPZ))
			printareRapRPZ();
		else if (raportType.equalsIgnoreCase(RAP_PRINT_FILTRU))
			printareFiltru();
		else if (raportType.equalsIgnoreCase(RAP_REG_BANCA))
			printareRegBanca();
		else if (raportType.equalsIgnoreCase(RAP_REG_ANAF))
			printareRegAnaf();
		else if (raportType.equalsIgnoreCase(RAP_INV_PROSOFT))
			exportInvProsoft();
	}
	
	private void printareFisaParteneri()
	{
//		final int result = MessageDialog.open(MessageDialog.QUESTION, Display.getCurrent().getActiveShell(), RAP_FISA_PARTENERI, "Selectati tipul de raport",
//				SWT.NONE, "Centralizat", "Detaliat");
//		
//		if (result == 0) // Centralizat
//		{
			printareRaport.setEnabled(false);
			final LocalDate fromDate = extractLocalDate(from);
			final LocalDate toDate = extractLocalDate(to);
			raportJob = BusinessDelegate.rulajeParteneri(new AsyncLoadData<RulajPartener>()
			{
				@Override public void success(final ImmutableList<RulajPartener> data)
				{
					printareRaport.setEnabled(true);
					try
					{
						JasperReportManager.instance(bundle, log).printFisaParteneri_Centralizat(bundle, fromDate, toDate, data);
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				}

				@Override public void error(final String details)
				{
					printareRaport.setEnabled(true);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
				}
			}, sync, selectedGestiune().map(Gestiune::getId).orElse(null), selectedPartner().map(Partner::getId).orElse(null), fromDate, toDate, log);
//		}
//		else if (result == 1) // Detaliat
//		{
//			
//		}
	}
	
	private void printareRegIncasariPlati()
	{
		final int rapType = MessageDialog.open(MessageDialog.QUESTION, Display.getCurrent().getActiveShell(), RAP_REG_INCASARI_PLATI, Messages.UrmarireParteneriPart_SelectType,
				SWT.NONE, Messages.UrmarireParteneriPart_AllPeriod, Messages.UrmarireParteneriPart_Daily);
		
		if (rapType == 0 || rapType == 1) // 0 - Inlantuit; 1 - Pe zile
		{
			printareRaport.setEnabled(false);
			final Optional<Gestiune> selGest = selectedGestiune();
			raportJob = BusinessDelegate.regIncasariPlati(new AsyncLoadResult<InvocationResult>()
			{
				@Override public void success(final InvocationResult result)
				{
					printareRaport.setEnabled(true);
					final Map<LocalDate, java.util.List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
					final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
					try
					{
						if (rapType == 0) // 0 - Inlantuit
							JasperReportManager.instance(bundle, log).printRegIncasariPlati_Inlantuit(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
						else if (rapType == 1) // 1 - Pe zile
							JasperReportManager.instance(bundle, log).printRegIncasariPlati_Zile(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				}

				@Override public void error(final String details)
				{
					printareRaport.setEnabled(true);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
				}
			}, sync, selGest.map(Gestiune::getId).orElse(null), extractLocalDate(from), extractLocalDate(to), log);
		}
	}
	
	private void printareRegCasa()
	{
		final int rapType = MessageDialog.open(MessageDialog.QUESTION, Display.getCurrent().getActiveShell(), RAP_REG_CASA, Messages.UrmarireParteneriPart_SelectType,
				SWT.NONE, Messages.UrmarireParteneriPart_AllPeriod, Messages.UrmarireParteneriPart_Daily);
		
		if (rapType == 0 || rapType == 1) // 0 - Inlantuit; 1 - Pe zile
		{
			printareRaport.setEnabled(false);
			final Optional<Gestiune> selGest = selectedGestiune();
			raportJob = BusinessDelegate.regCasa(new AsyncLoadResult<InvocationResult>()
			{
				@Override public void success(final InvocationResult result)
				{
					printareRaport.setEnabled(true);
					final Map<LocalDate, java.util.List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
					final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
					try
					{
						if (rapType == 0) // 0 - Inlantuit
							JasperReportManager.instance(bundle, log).printRegCasa_Inlantuit(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
						else if (rapType == 1) // 1 - Pe zile
							JasperReportManager.instance(bundle, log).printRegCasa_Zile(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				}

				@Override public void error(final String details)
				{
					printareRaport.setEnabled(true);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
				}
			}, sync, selGest.map(Gestiune::getId).orElse(null), extractLocalDate(from), extractLocalDate(to), log);
		}
	}
	
	private void printareJurnalGeneral()
	{
		printareRaport.setEnabled(false);
		final Optional<Gestiune> selGest = selectedGestiune();
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		raportJob = BusinessDelegate.filteredDocuments(new AsyncLoadData<Document>()
		{
			@Override public void success(final ImmutableList<Document> accDocs)
			{
				printareRaport.setEnabled(true);
				try
				{
					JasperReportManager.instance(bundle, log).printJurnalGeneral(bundle, fromDate, toDate, 
							accDocs.stream().map(AccountingDocument.class::cast).collect(toImmutableList()));
				}
				catch (IOException | JRException e)
				{
					log.error(e);
					showException(e);
				}
			}

			@Override public void error(final String details)
			{
				printareRaport.setEnabled(true);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
			}
		}, sync, selGest.map(Gestiune::getId).orElse(null), selectedPartner().map(Partner::getId).orElse(null), null, fromDate, toDate,
		selectedRpzLoad(), selectedCasaLoad(), BancaLoad.INDIFERENT, null, DocumentTypesLoad.FARA_DISCOUNTURI, CoveredDocsLoad.INDIFERENT,
		null, null, ContaLoad.INDIFERENT, null, null, log);
	}
	
	private void printareRapRPZ()
	{
		final int rapType = MessageDialog.open(MessageDialog.QUESTION, Display.getCurrent().getActiveShell(), RAP_RPZ, Messages.UrmarireParteneriPart_SelectType,
				SWT.NONE, Messages.UrmarireParteneriPart_AllPeriod, Messages.UrmarireParteneriPart_Daily);
		
		if (rapType == 0 || rapType == 1) // 0 - Inlantuit; 1 - Pe zile
		{
			printareRaport.setEnabled(false);
			final Optional<Gestiune> selGest = selectedGestiune();
			raportJob = BusinessDelegate.regRPZ(new AsyncLoadResult<InvocationResult>()
			{
				@Override public void success(final InvocationResult result)
				{
					printareRaport.setEnabled(true);
					final Map<LocalDate, java.util.List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
					final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
					try
					{
						if (rapType == 0) // 0 - Inlantuit
							JasperReportManager.instance(bundle, log).printRegRPZ_Inlantuit(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
						else if (rapType == 1) // 1 - Pe zile
							JasperReportManager.instance(bundle, log).printRegRPZ_Zile(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									accDocsByDate, solduriInitiale);
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				}

				@Override public void error(final String details)
				{
					printareRaport.setEnabled(true);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
				}
			}, sync, selGest.map(Gestiune::getId).orElse(null), extractLocalDate(from), extractLocalDate(to), log);
		}
	}
	
	private void printareFiltru()
	{
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		try
		{
			JasperReportManager.instance(bundle, log).printFiltruUrmarire(bundle, fromDate, toDate,
					table.getSourceList().stream()
					.filter(AccountingDocument.class::isInstance)
					.map(AccountingDocument.class::cast)
					.collect(toImmutableList()));
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void printareRegBanca()
	{
		final SelectEntityDialog<ContBancar> regBancaDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
				RAP_REG_BANCA, Messages.UrmarireParteneriPart_SelectType, Messages.UrmarireParteneriPart_BankAcc, BusinessDelegate.allConturiBancare(), Messages.UrmarireParteneriPart_AllPeriod, Messages.UrmarireParteneriPart_Daily);
		final int rapType = regBancaDialog.open();
		final Integer contBancarId = regBancaDialog.selectedEntity().map(ContBancar::getId).orElse(null);
		final ContBancar contBancar = regBancaDialog.selectedEntity().orElse(null);
		
		if (rapType == 0 || rapType == 1) // 0 - Inlantuit; 1 - Pe zile
		{
			printareRaport.setEnabled(false);
			final Optional<Gestiune> selGest = selectedGestiune();
			raportJob = BusinessDelegate.regBanca(new AsyncLoadResult<InvocationResult>()
			{
				@Override public void success(final InvocationResult result)
				{
					printareRaport.setEnabled(true);
					final Map<LocalDate, java.util.List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
					final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
					try
					{
						if (rapType == 0) // 0 - Inlantuit
							JasperReportManager.instance(bundle, log).printRegBanca_Inlantuit(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									contBancar, accDocsByDate, solduriInitiale);
						else if (rapType == 1) // 1 - Pe zile
							JasperReportManager.instance(bundle, log).printRegBanca_Zile(bundle, selGest.map(Gestiune::getName).orElse(EMPTY_STRING),
									contBancar, accDocsByDate, solduriInitiale);
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				}

				@Override public void error(final String details)
				{
					printareRaport.setEnabled(true);
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_ErrorLoadingRep, details);
				}
			}, sync, selGest.map(Gestiune::getId).orElse(null), contBancarId, extractLocalDate(from), extractLocalDate(to), log);
		}
	}
	
	private void printareRegAnaf()
	{
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);
		try
		{
			JasperReportManager.instance(bundle, log).printRegAnaf(bundle, fromDate, toDate,
					AnafReporter.findAnafMessagesBetween(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX)));
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void exportInvProsoft()
	{
		final LocalDate fromDate = extractLocalDate(from);
		final LocalDate toDate = extractLocalDate(to);

		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		final String period = fromDate.toString() + "_" + toDate.toString();
		dialog.setFileName("prosoft_"+period+".zip");
		final String outputFileUri = dialog.open();

		if (isEmpty(outputFileUri))
			return;

		final String invoiceBaseUrl = BusinessDelegate.persistedProp(PersistedProp.BILLING_BASE_URL_KEY)
				.getValueOr(PersistedProp.BILLING_BASE_URL_DEFAULT);
		final String downloadUrl = invoiceBaseUrl + "/invoice/prosoft";

		final java.util.List<NameValuePair> params = java.util.List.of(new BasicNameValuePair("from", fromDate.toString()),
				new BasicNameValuePair("to", toDate.toString()));
		final Optional<Long> downloadResponse = RestCaller.post_WithSSL_DownloadFile(downloadUrl, "", outputFileUri, params);

		if (downloadResponse.isPresent())
		{
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
				Desktop.getDesktop().browseFileDirectory(new File(outputFileUri));
			else
				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
						NLS.bind(ro.linic.ui.legacy.session.Messages.FileSaved, outputFileUri));
		}
	}
	
	private void printSelectedDocs()
	{
		try
		{
			JasperReportManager.instance(bundle, log).printDocs(bundle, table.selectedAccDocs(), true);
		}
		catch (IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
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
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_MissingDelegate, Messages.UrmarireParteneriPart_MissingDelegateMessage);
					return;
				}

				final boolean hasMailConfigured = Boolean.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
						.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
				if (!hasMailConfigured)
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error, Messages.UrmarireParteneriPart_MailSMTPError);
					return;
				}

				JasperReportManager.instance(bundle, log).printFactura_ClientDuplicate(bundle, docSelectat, null);
			}
			else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_WrongDoc, Messages.UrmarireParteneriPart_WrongDocMessage);
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
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_MissingDelegate,
							NLS.bind(Messages.UrmarireParteneriPart_MissingDelegateMessageNls, docSelectat.getDoc(), docSelectat.getNrDoc()));
					return;
				}

				final FileDialog chooser = new FileDialog(printareDocs.getShell(), SWT.SAVE);
				chooser.setFileName(Messages.Invoice_+docSelectat.getNrDoc()+".xml"); //$NON-NLS-2$
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
		if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_Report, 
				MessageFormat.format(Messages.UrmarireParteneriPart_ReportMessage,
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
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.UrmarireParteneriPart_MissingDelegate,
							NLS.bind(Messages.UrmarireParteneriPart_MissingDelegateMessageNls, docSelectat.getDoc(), docSelectat.getNrDoc()));
					return;
				}
				
				AnafReporter.reportInvoice(docSelectat.getCompany().getId(), docSelectat.getId());
			}
		});
	}

	public void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
	}
	
	public void cancelRaportJob()
	{
		if (raportJob != null)
			raportJob.cancel();
	}
	
	private String[] createGestiuniArray()
	{
		final String[] gestiuni = new String[allGestiuni.size()+1];
		gestiuni[0] = INDIFERENT;
		for (int i = 0; i < allGestiuni.size(); i++)
			gestiuni[i+1] = allGestiuni.get(i).getName();
		return gestiuni;
	}
	
	private String[] createOperatiiArray()
	{
		final String[] gestiuni = new String[5];
		gestiuni[0] = INDIFERENT;
		for (int i = 0; i < 4; i++)
			gestiuni[i+1] = TipDoc.values()[i].toString();
		return gestiuni;
	}
	
	public void reloadPartners()
	{
		parteneri.deselectAll();
		allPartners = BusinessDelegate.allPartners();
		parteneri.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
	}
	
	private void selectGest(final Gestiune gest)
	{
		if (gest != null)
			gestiuni.select(allGestiuni.indexOf(gest)+1);
		else
			gestiuni.deselectAll();
	}
	
	private Optional<Partner> selectedPartner()
	{
		final int index = parteneri.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allPartners.get(index));
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiuni.getSelectionIndex();
		if (index == -1 || index == 0)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index-1));
	}
	
	private Optional<String> selectedRaportType()
	{
		final int index = rapoarte.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(rapoarte.getItem(index));
	}
	
	private Optional<TipDoc> selectedTipDoc()
	{
		final int index = operatii.getSelectionIndex();
		if (index == -1 || index == 0)
			return Optional.empty();
		
		return Optional.of(TipDoc.values()[index-1]);
	}
	
	private RPZLoad selectedRpzLoad()
	{
		if (doarRpz.getSelection())
			return RPZLoad.DOAR_RPZ;
		if (faraRpz.getSelection())
			return RPZLoad.FARA_RPZ;
		return RPZLoad.INDIFERENT;
	}
	
	private CasaLoad selectedCasaLoad()
	{
		if (doarCasa.getSelection())
			return CasaLoad.DOAR_CASA;
		if (faraCasa.getSelection())
			return CasaLoad.FARA_CASA;
		return CasaLoad.INDIFERENT;
	}
	
	private DocumentTypesLoad selectedDiscounturiLoad()
	{
		if (doarDiscounturi.getSelection())
			return DocumentTypesLoad.DOAR_DISCOUNTURI;
		if (faraDiscounturi.getSelection())
			return DocumentTypesLoad.FARA_DISCOUNTURI;
		return DocumentTypesLoad.INDIFERENT;
	}
	
	private CoveredDocsLoad selectedCoveredLoad()
	{
		if (doarCovered.getSelection())
			return CoveredDocsLoad.DOAR_COVERED;
		if (faraCovered.getSelection())
			return CoveredDocsLoad.FARA_COVERED;
		return CoveredDocsLoad.INDIFERENT;
	}
	
	private void clearAllFields()
	{
		parteneri.deselectAll();
		gestiuni.deselectAll();
		operatii.deselectAll();
		insertDate(from, POSTGRES_MIN);
		insertDate(to, POSTGRES_MAX);
		doarRpz.setSelection(false);
		faraRpz.setSelection(false);
		indiferentRpz.setSelection(true);
		doarCasa.setSelection(false);
		faraCasa.setSelection(false);
		indiferentCasa.setSelection(true);
		doarDiscounturi.setSelection(false);
		faraDiscounturi.setSelection(false);
		indiferentDiscounturi.setSelection(true);
		doarCovered.setSelection(false);
		faraCovered.setSelection(false);
		indiferentCovered.setSelection(true);
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.Save, Messages.UrmarireParteneriPart_SaveMessage))
			onSave();
	}
}
