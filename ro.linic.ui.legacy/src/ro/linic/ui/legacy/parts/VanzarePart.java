package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.isNumeric;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.XXX_BANNER_FONT;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setFont;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ca.odell.glazedlists.matchers.TextMatcherEditor;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TransferType;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.ProductGestiuneMapping;
import ro.colibri.entities.user.User;
import ro.colibri.security.Permissions;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.AdaugaPartnerDialog;
import ro.linic.ui.legacy.dialogs.ManagerCasaDialog;
import ro.linic.ui.legacy.dialogs.ScheduleDialog;
import ro.linic.ui.legacy.dialogs.SelectEntityDialog;
import ro.linic.ui.legacy.dialogs.TransferaSauNegativDialog;
import ro.linic.ui.legacy.dialogs.TraseeDialog;
import ro.linic.ui.legacy.dialogs.VanzariIncarcaDocDialog;
import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.legacy.tables.AllProductsNatTable.SourceLoc;
import ro.linic.ui.legacy.tables.TreeOperationsNatTable;
import ro.linic.ui.legacy.wizards.InchideBonWizard;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;
import ro.linic.ui.legacy.wizards.InchideBonWizardDialog;

public class VanzarePart implements VanzareInterface
{
	public static final String PART_ID = "linic_gest_client.part.vanzari";
	public static final String PART_DESCRIPTOR_ID = "linic_gest_client.partdescriptor.vanzari";
	
	public static final String VANZARE_PART_TYPE_KEY = "vanzare_part_type_key";
	public static final String VANZARE_PART_TYPE_DEFAULT = "1";
	
	private static final String PRODUCTS_TABLE_STATE_PREFIX = "vanzari.all_products_nt";
	private static final String BON_DESCHIS_TABLE_STATE_PREFIX = "vanzari.bon_deschis_nt";
	private static final String INITIAL_PART_LOAD_PROP = "initial_part_load";
	private static final String VERTICAL_SASH_STATE_PREFIX = "vanzari.vertical_sash";
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "vanzari.horizontal_sash";

	// MODEL
	private AccountingDocument bonCasa;
	private ImmutableList<Partner> allPartners;

	// UI
	private SashForm verticalSash;
	private SashForm horizontalSash;
	
	private Combo searchMode;
	private Text denumireText;
	private Text cantitateText;
	private AllProductsNatTable allProductsTable;
	
	private Combo partner;
	private Button createPartner;
	private Button scheduleButton;
	private Button inchideCasaButton;
	private Button inchideFacturaBCButton;
	private Button inchideCardButton;

	private Button casaActivaButton;
	private Button ofertaPretButton;
	private TreeOperationsNatTable bonDeschisTable;

	private Label totalFaraTVALabel;
	private Label tvaLabel;
	private Label totalCuTVALabel;
	
	private Button incarcaBonuriButton;
	private Button refreshButton;
	private Button managerCasaButton;
	private Button transferaButton;
	private Button printPlanuriButton;
	private Button cancelBonButton;
	private Button stergeRandButton;
	
	private boolean partnersAreUpdating = false;

	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;

	public static VanzarePart newPartForBon(final EPartService partService, final AccountingDocument bon)
	{
		final MPart createdPart = partService.showPart(partService.createPart(VanzarePart.PART_DESCRIPTOR_ID),
				PartState.VISIBLE);

		if (createdPart == null)
			return null;

		final VanzarePart vanzarePart = (VanzarePart) createdPart.getObject();

		if (vanzarePart == null)
			return null;

		vanzarePart.updateBonCasa(bon, true);
		return vanzarePart;
	}
	
	/**
	 * Ask the user if he wants to delete the connected operations as well.
	 * Always starts from the top owner operation and asks if we want to delete the children as well.
	 * If not, only delete the owner.
	 */
	private static ImmutableList<Operatiune> collectDeletableOperations(final ImmutableList<Operatiune> operations)
	{
		return operations.stream().flatMap(operation ->
		{
			Operatiune lastOwner = operation;
			while (lastOwner.getOwnerOp() != null)
				lastOwner = lastOwner.getOwnerOp();

			Operatiune lastChild = lastOwner.getChildOp();
			final List<Operatiune> deletableOps = new ArrayList<Operatiune>();

			// 1. verify child and owner operations
			while (lastChild != null)
			{
				deletableOps.add(lastChild);
				lastChild = lastChild.getChildOp();
			}

			// 2. delete operations
			if (!deletableOps.isEmpty() && !MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					"Stergeti conexiuni",
					MessageFormat.format(
							"Operatiunea{3}{0}{3}contine alte {1} operatiuni conectate:{3}{2}{3}Stergeti si aceste operatiuni?",
							lastOwner.displayName(), 
							deletableOps.size(),
							deletableOps.stream().map(Operatiune::displayName).collect(Collectors.joining(NEWLINE)),
							NEWLINE)))
			{
				deletableOps.clear();
			}
			deletableOps.add(lastOwner);
			return deletableOps.stream();
		}).collect(toImmutableList());
	}

	@PostConstruct
	public void createComposite(final Composite parent)
	{
		if (Boolean.valueOf((String) ClientSession.instance().getProperties().getOrDefault(INITIAL_PART_LOAD_PROP,
				Boolean.TRUE.toString())))
		{
			ClientSession.instance().getProperties().setProperty(INITIAL_PART_LOAD_PROP, Boolean.FALSE.toString());
			BusinessDelegate.unfinishedBonuriCasa().forEach(unfinishedBon -> newPartForBon(partService, unfinishedBon));
		}

		parent.setLayout(new GridLayout());
		createTopBar(parent);

		verticalSash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(verticalSash);
		createTopArea(verticalSash);
		
		bonDeschisTable = new TreeOperationsNatTable(TreeOperationsNatTable.SourceLoc.VANZARI);
		bonDeschisTable.postConstruct(verticalSash);
		bonDeschisTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(bonDeschisTable.getTable());

		createTotalsBar(parent);
		createBottomBar(parent);
		
		loadVisualState();
		addListeners();
		loadData();
	}
	
	private void createTopArea(final Composite parent)
	{
	    horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
	    GridDataFactory.fillDefaults().applyTo(horizontalSash);
		
		final Composite leftContainer = new Composite(horizontalSash, SWT.NONE);
		leftContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().applyTo(leftContainer);
		
		searchMode = new Combo(leftContainer, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.fillDefaults().applyTo(searchMode);

		denumireText = new Text(leftContainer, SWT.BORDER);
		denumireText.setMessage("Denumire");
		denumireText.setTextLimit(255);
		denumireText.setFocus();
		UIUtils.setBannerFont(denumireText);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(denumireText);

		cantitateText = new Text(leftContainer, SWT.BORDER);
		cantitateText.setMessage("Cant");
		cantitateText.setText("1");
		cantitateText.setTextLimit(10);
		UIUtils.setBannerFont(cantitateText);
		GridDataFactory.fillDefaults().applyTo(cantitateText);
		
		scheduleButton = new Button(leftContainer, SWT.PUSH);
		scheduleButton.setText("Programeaza");
		scheduleButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		scheduleButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(scheduleButton);
		GridDataFactory.fillDefaults().applyTo(scheduleButton);
		
		partner = new Combo(leftContainer, SWT.DROP_DOWN);
		UIUtils.setFont(partner);
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(partner);
		
		createPartner = new Button(leftContainer, SWT.PUSH);
		createPartner.setText("Adauga Partener");
		UIUtils.setFont(createPartner);
		
		inchideCasaButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
		inchideCasaButton.setText("Inchidere prin casa - F3");
		inchideCasaButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideCasaButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCasaButton);
		GridDataFactory.fillDefaults()
			.grab(false, true)
			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(inchideCasaButton);
		
		inchideFacturaBCButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
		inchideFacturaBCButton.setText("Inchidere prin Factura, BonConsum");
		inchideFacturaBCButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideFacturaBCButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideFacturaBCButton);
		GridDataFactory.fillDefaults()
			.grab(true, true)
			.align(SWT.RIGHT, SWT.FILL)
			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(inchideFacturaBCButton);
		
		inchideCardButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
		inchideCardButton.setText("Inchidere prin CARD/POS");
		inchideCardButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		inchideCardButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCardButton);
		GridDataFactory.fillDefaults()
			.grab(false, true)
			.align(SWT.RIGHT, SWT.FILL)
			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(inchideCardButton);
		
		casaActivaButton = new Button(leftContainer, SWT.CHECK);
		casaActivaButton.setText("casa marcat activa?");
		casaActivaButton.setSelection(true);
		casaActivaButton.setEnabled(ClientSession.instance().hasPermission(Permissions.CLOSE_WITHOUT_CASA));
		setFont(casaActivaButton);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(casaActivaButton);

		ofertaPretButton = new Button(leftContainer, SWT.PUSH);
		ofertaPretButton.setText("Oferta");
		setFont(ofertaPretButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).applyTo(ofertaPretButton);
		
		allProductsTable = new AllProductsNatTable(SourceLoc.VANZARI, bundle ,log);
		allProductsTable.postConstruct(horizontalSash);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(allProductsTable.getTable());
	}
	
	private void createTotalsBar(final Composite parent)
	{
		final Composite totalsContainer = new Composite(parent, SWT.NONE);
		totalsContainer.setLayout(new GridLayout(6, false));
		totalsContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Label totalFaraTVAHint = new Label(totalsContainer, SWT.NONE);
		totalFaraTVAHint.setText("TOTAL fara TVA");

		totalFaraTVALabel = new Label(totalsContainer, SWT.BORDER);
		totalFaraTVALabel.setText("0");
		totalFaraTVALabel.setAlignment(SWT.RIGHT);
		totalFaraTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalFaraTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData faraTvaGD = new GridData();
		faraTvaGD.widthHint = 200;
		totalFaraTVALabel.setLayoutData(faraTvaGD);
		UIUtils.setBoldBannerFont(totalFaraTVALabel);

		final Label totalTVAHint = new Label(totalsContainer, SWT.NONE);
		totalTVAHint.setText("TVA");

		tvaLabel = new Label(totalsContainer, SWT.BORDER);
		tvaLabel.setText("0");
		tvaLabel.setAlignment(SWT.RIGHT);
		tvaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		tvaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData tvaGD = new GridData();
		tvaGD.widthHint = 200;
		tvaLabel.setLayoutData(tvaGD);
		UIUtils.setBoldBannerFont(tvaLabel);

		final Label totalCuTVAHint = new Label(totalsContainer, SWT.NONE);
		totalCuTVAHint.setText("TOTAL cu TVA");

		totalCuTVALabel = new Label(totalsContainer, SWT.BORDER);
		totalCuTVALabel.setText("0");
		totalCuTVALabel.setAlignment(SWT.CENTER);
		totalCuTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalCuTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		totalCuTVALabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		UIUtils.setBoldCustomFont(totalCuTVALabel, XXX_BANNER_FONT);
	}
	
	private void createBottomBar(final Composite parent)
	{
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(7, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		incarcaBonuriButton = new Button(footerContainer, SWT.PUSH);
		incarcaBonuriButton.setText("Incarca Bon");
		UIUtils.setBannerFont(incarcaBonuriButton);

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText("Refresh");
		UIUtils.setBannerFont(refreshButton);

		managerCasaButton = new Button(footerContainer, SWT.PUSH);
		managerCasaButton.setText("Manager CasaMarcat");
		UIUtils.setBannerFont(managerCasaButton);
		
		printPlanuriButton = new Button(footerContainer, SWT.PUSH);
		printPlanuriButton.setText("Printare Trasee");
		UIUtils.setBannerFont(printPlanuriButton);
		
		transferaButton = new Button(footerContainer, SWT.PUSH);
		transferaButton.setText("Transfera");
		transferaButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		transferaButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData transferaGD = new GridData(GridData.FILL_HORIZONTAL);
		transferaGD.horizontalAlignment = SWT.RIGHT;
		transferaButton.setLayoutData(transferaGD);
		UIUtils.setBoldBannerFont(transferaButton);

		cancelBonButton = new Button(footerContainer, SWT.PUSH);
		cancelBonButton.setText("CANCEL BON");
		cancelBonButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		cancelBonButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData cancelGD = new GridData();
		cancelGD.horizontalAlignment = SWT.RIGHT;
		cancelBonButton.setLayoutData(cancelGD);
		UIUtils.setBoldBannerFont(cancelBonButton);

		stergeRandButton = new Button(footerContainer, SWT.PUSH);
		stergeRandButton.setText("sterge rand");
		stergeRandButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		stergeRandButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData stergeGD = new GridData();
		stergeGD.horizontalAlignment = SWT.RIGHT;
		stergeRandButton.setLayoutData(stergeGD);
		UIUtils.setBoldBannerFont(stergeRandButton);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(PRODUCTS_TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
		saveState(BON_DESCHIS_TABLE_STATE_PREFIX, bonDeschisTable.getTable(), part);
		
		final int[] verticalWeights = verticalSash.getWeights();
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".0", String.valueOf(verticalWeights[0]));
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".1", String.valueOf(verticalWeights[1]));
		final int[] horizontalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".0", String.valueOf(horizontalWeights[0]));
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".1", String.valueOf(horizontalWeights[1]));
	}
	
	private void loadVisualState()
	{
		loadState(BON_DESCHIS_TABLE_STATE_PREFIX, bonDeschisTable.getTable(), part);
		loadState(PRODUCTS_TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
		
		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".0", "200"));
		verticalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".1", "200"));
		verticalSash.setWeights(verticalWeights);
		final int[] horizontalWeights = new int[2];
		horizontalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".0", "200"));
		horizontalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".1", "200"));
		horizontalSash.setWeights(horizontalWeights);
	}
	
	@Focus
	public void setFocus()
	{
		denumireText.setFocus();
	}

	private void addListeners()
	{
		searchMode.addModifyListener(this::filterModeChange);
		denumireText.addModifyListener(e -> allProductsTable.filter(denumireText.getText()));
		denumireText.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && isNumeric(cantitateText.getText())
						&& !isEmpty(denumireText.getText()))
					addNewOperationToBon(false);

				if (e.keyCode == SWT.ARROW_DOWN)
				{
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
				}
				if (e.keyCode == SWT.ARROW_UP)
				{
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.UP);
				}
				if (e.keyCode == SWT.F2)
				{
					final List<Product> selection = allProductsTable.selection();
					if (!selection.isEmpty())
						denumireText.setText(selection.get(0).getBarcode());
				}
			}
		});
		
		scheduleButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ScheduleDialog dialog = new ScheduleDialog(scheduleButton.getShell(), sync, log, bundle, bonCasa);
				if (dialog.open() == Window.OK)
					updateBonCasa(dialog.reloadedDoc(), false);
			}
		});

		inchideCasaButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				closeBon(TipInchidere.PRIN_CASA);
			}
		});
		
		inchideFacturaBCButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				closeBon(TipInchidere.FACTURA_BC);
			}
		});
		
		inchideCardButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				closeBon(TipInchidere.PRIN_CARD);
			}
		});

		cantitateText.addFocusListener(new FocusAdapter()
		{
			@Override public void focusGained(final FocusEvent e)
			{
				cantitateText.selectAll();
			}
		});
		cantitateText.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
					denumireText.setFocus();
			}
		});
		
		createPartner.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (new AdaugaPartnerDialog(createPartner.getShell(), bundle, log).open() == Window.OK)
					reloadPartners();
			}
		});
		
		managerCasaButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				new ManagerCasaDialog(Display.getCurrent().getActiveShell(), log).open();
			}
		});
		
		printPlanuriButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				new TraseeDialog(printPlanuriButton.getShell(), sync, log, bundle).open();
			}
		});
		
		incarcaBonuriButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final AccountingDocument currentBon = BusinessDelegate.reloadDoc(bonCasa);
				new VanzariIncarcaDocDialog(Display.getCurrent().getActiveShell(), VanzarePart.this, sync).open();
				updateBonCasa(currentBon, true);
			}
		});
		
		transferaButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ImmutableList<Operatiune> sel = bonDeschisTable.selection().stream()
						.filter(Operatiune.class::isInstance)
						.map(Operatiune.class::cast)
						.collect(toImmutableList());
				if (!sel.isEmpty() && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), 
						"Transferati?", 
						"Sunteti sigur ca doriti sa transferati stoc din alta gestiune pentru operatiunile("+sel.size()+") selectate?"))
					transferOperations(sel);
			}
		});

		stergeRandButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				deleteOperations(bonDeschisTable.selection().stream()
						.filter(Operatiune.class::isInstance)
						.map(Operatiune.class::cast)
						.collect(toImmutableList()));
			}
		});

		cancelBonButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (bonCasa != null && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Anulare bon", "Sunteti sigur ca doriti sa anulati bonul?"))
					deleteOperations(ImmutableList.copyOf(bonCasa.getOperatiuni()));
			}
		});

		refreshButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				reloadProduct(null, true);
				updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
				reloadPartners();
			}
		});
		
		ofertaPretButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					if (bonCasa != null)
						JasperReportManager.instance(bundle, log).printOfertaPret(bundle, bonCasa);
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});
	}
	
	private void loadData()
	{
		reloadProduct(null, false);
		updateBonCasa(bonCasa, true);
		reloadPartners();
	}
	
	private void reloadPartners()
	{
		partnersAreUpdating = true;
		allPartners = BusinessDelegate.allPartners();
		partner.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
		partnersAreUpdating = false;
	}
	
	/**
	 * 
	 * @param bonCasa @nullable
	 */
	@Override
	public void updateBonCasa(final AccountingDocument bonCasa, final boolean updateTotalLabels)
	{
		this.bonCasa = bonCasa;
		final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(bonCasa);
		if (this.bonCasa != null)
			this.bonCasa.setOperatiuni(new HashSet<Operatiune>(operatiuni));
		
		bonDeschisTable.loadData(operatiuni);

		if (updateTotalLabels)
		{
			totalFaraTVALabel.setText(Optional.ofNullable(this.bonCasa).map(accDoc -> accDoc.getTotal().subtract(accDoc.getTotalTva()))
					.map(PresentationUtils::displayBigDecimal).orElse("0"));
			tvaLabel.setText(Optional.ofNullable(this.bonCasa).map(AccountingDocument::getTotalTva)
					.map(PresentationUtils::displayBigDecimal).orElse("0"));
			totalCuTVALabel.setText(Optional.ofNullable(this.bonCasa).map(AccountingDocument::getTotal)
					.map(PresentationUtils::displayBigDecimal).orElse("0"));
		}
	}

	private void addNewOperationToBon(final boolean negativeAllowedInitial)
	{
		final User user = ClientSession.instance().getLoggedUser();
		final BigDecimal cantitate = parse(cantitateText.getText());
		final String barcode = denumireText.getText();
		boolean negativeAllowed = negativeAllowedInitial;
		TransferType transferType = TransferType.FARA_TRANSFER;

		final Optional<Product> product = allProductsTable.getSourceData().stream()
				.filter(p -> globalIsMatch(p.getBarcode(), barcode, TextFilterMethod.EQUALS)).findFirst();

		/////////////////////////////// HANDLE NEGATIVE STOC OR AUTOMATIC TRANSFER //////////////////////////////
		if (product.isPresent() && !negativeAllowed && Product.shouldModifyStoc(product.get()) &&
				!Product.PRODUS_FINIT_CATEGORY.equalsIgnoreCase(product.get().getCategorie()) &&
				product.get().getStocuri().size() == 2)
		{
			final ProductGestiuneMapping stocMapping = product.get().getStocuri().stream()
					.filter(stoc -> stoc.getGestiune().equals(user.getSelectedGestiune())).findFirst().get();
			final ProductGestiuneMapping otherGestStocMapping = product.get().getStocuri().stream()
					.filter(stoc -> !stoc.getGestiune().equals(user.getSelectedGestiune())).findFirst().get();

			// cantitatea scazuta e mai mare decat stocul pe gestiunea curenta
			if (cantitate.compareTo(BigDecimal.ZERO) >= 0 && cantitate.compareTo(stocMapping.getStoc()) > 0)
			{
				// cantitatea scazuta e mai mare decat stocul pe ambele gestiuni
				if (cantitate.compareTo(stocMapping.getStoc().max(BigDecimal.ZERO).add(otherGestStocMapping.getStoc().max(BigDecimal.ZERO))) > 0)
				{
					// intra in negativ obligatoriu
					final TransferaSauNegativDialog dialog = new TransferaSauNegativDialog(
							Display.getCurrent().getActiveShell(), false, false, "Stoc insuficient",
							"Nu aveti stoc suficient. Intrati in negativ?");

					if (dialog.open() != TransferaSauNegativDialog.INTRA_NEGATIV_ID)
						return;
					negativeAllowed = true;
				}
				// cantitatea scazuta e mai mare decat stocul curent, dar mai mica decat stocul
				// pe cele 2 gestiuni
				else
				{
					// transfera sau intra in negativ
					final TransferaSauNegativDialog dialog = new TransferaSauNegativDialog(
							Display.getCurrent().getActiveShell(), true, cantitate.compareTo(otherGestStocMapping.getStoc()) <= 0,
							"Transfer?",
							"Transferati diferenta din cealalta gestiune?");

					final int dialogResult = dialog.open();
					if (dialogResult == TransferaSauNegativDialog.INTRA_NEGATIV_ID)
						negativeAllowed = true;
					else if (dialogResult == TransferaSauNegativDialog.TRANSFERA_ID)
						transferType = TransferType.TRANSFERA_DIFERENTA;
					else if (dialogResult == TransferaSauNegativDialog.TRANSFERA_TOT_ID)
						transferType = TransferType.TRANSFERA_TOT;
					else
						return;
				}
			}
		}
		/////////////////////////////// HANDLE NEGATIVE STOC OR AUTOMATIC TRANSFER END //////////////////////////////

		BigDecimal overridePrice = null;
		String overrideName = null;
		
		if (product.isPresent() && allProductsTable.getDataChangeLayer().getDataChanges().stream()
				.map(dataChange -> ((IdIndexIdentifier<Product>)dataChange.getKey()).rowObject)
				.distinct()
				.filter(p -> p.equals(product.get()))
				.findAny()
				.isPresent())
		{
			overridePrice = product.get().getPricePerUom();
			overrideName = product.get().getName();
		}
		
		final Long accDocId = Optional.ofNullable(bonCasa).map(AccountingDocument::getId).orElse(null);
		final InvocationResult result = BusinessDelegate.addToBonCasa(barcode, cantitate, 
				overridePrice, accDocId, negativeAllowed, transferType, overrideName, bundle, log);

		if (!result.statusOk())
		{
			// most of times here we have a difference between the product in the database
			// and the product on the UI;
			// in this case we need to reload the product and restart the addToBon
			// mechanism.
			// in case we have any other error we just show the error and cancel the
			// operation
			if (globalIsMatch(result.toTextCodes(), InvocationResult.STOC_NEGATIV_ERR, TextFilterMethod.CONTAINS)
					|| globalIsMatch(result.toTextCodes(), InvocationResult.STOC_NEGATIV_GLOBAL_ERR,
							TextFilterMethod.CONTAINS))
			{
				reloadProduct(product.orElse(null), false);

				final TransferaSauNegativDialog dialog = new TransferaSauNegativDialog(
						Display.getCurrent().getActiveShell(), false, false, result.toTextCodes(), result.toTextDescription());

				if (dialog.open() != TransferaSauNegativDialog.INTRA_NEGATIV_ID)
					return;
				addNewOperationToBon(true);
			} else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
						result.toTextDescription());
		} else
		{
			// everything was okay;
			// - reload product and bonCasa
			// - clear denumire and cantitate input
			// - set focus on denumire widget
			if (product.isPresent())
				allProductsTable.replace(product.get(), result.extra(InvocationResult.PRODUCT_KEY));
			else
				allProductsTable.add(result.extra(InvocationResult.PRODUCT_KEY));
			updateBonCasa(result.extra(InvocationResult.ACCT_DOC_KEY), true);
			denumireText.setText(EMPTY_STRING);
			cantitateText.setText("1");
			denumireText.setFocus();
		}
	}

	/**
	 * @param product if null is passed we reload all products
	 */
	private void reloadProduct(final Product product, final boolean showConfirmation)
	{
		if (product == null)
		{
			BusinessDelegate.allProducts(new AsyncLoadData<Product>()
			{
				@Override
				public void success(final ImmutableList<Product> data)
				{
					allProductsTable.loadData(data, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());

					if (showConfirmation)
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Succes",
								"Produsele au fost incarcate cu succes!");
				}

				@Override
				public void error(final String details)
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea produselor",
							details);
				}
			}, sync, bundle, log);
		} else
		{
			final Product updatedProduct = BusinessDelegate.productById(product.getId(), bundle, log);
			if (updatedProduct == null)
				allProductsTable.remove(product);
			else
				allProductsTable.replace(product, updatedProduct);
		}
	}

	private void deleteOperations(final ImmutableList<Operatiune> operations)
	{
		final ImmutableList<Operatiune> deletableOps = collectDeletableOperations(operations);
		final ImmutableSet<String> operationBarcodes = deletableOps.stream().map(Operatiune::getBarcode)
				.collect(toImmutableSet());
		final ImmutableList<Product> productsToReload = allProductsTable.getSourceData().parallelStream()
				.filter(p -> operationBarcodes.contains(p.getBarcode())).collect(toImmutableList());

		final InvocationResult result = BusinessDelegate
				.deleteOperations(deletableOps.stream().map(Operatiune::getId).collect(toImmutableSet()));
		if (!result.statusOk())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
			return;
		}

		// 3. update bonCasa(may be deleted) and reload affected products
		updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
		if (productsToReload.size() > 1)
			reloadProduct(null, false);
		else
			productsToReload.forEach(p -> reloadProduct(p, false));
	}
	
	private void transferOperations(final ImmutableList<Operatiune> operations)
	{
		final ImmutableSet<String> operationBarcodes = operations.stream().map(Operatiune::getBarcode)
				.collect(toImmutableSet());
		final ImmutableList<Product> productsToReload = allProductsTable.getSourceData().parallelStream()
				.filter(p -> operationBarcodes.contains(p.getBarcode())).collect(toImmutableList());

		int gestiuneId;
		final Gestiune userGest = ClientSession.instance().getLoggedUser().getSelectedGestiune();
		final ImmutableList<Gestiune> gestiuni = BusinessDelegate.allGestiuni().stream()
				.filter(g -> !g.equals(userGest))
				.collect(toImmutableList());
		if (gestiuni.size() == 1)
			gestiuneId = gestiuni.get(0).getId();
		else
		{
			final SelectEntityDialog<Gestiune> gestiuneDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
					"Transfer", "Selectati gestiunea din care doriti sa transferati", "Gestiune", gestiuni, "OK", "Cancel");
			final int dialogResult = gestiuneDialog.open();
			
			if (dialogResult != 0)
				return;
			
			gestiuneId = gestiuneDialog.selectedEntity().map(Gestiune::getId).get();
		}
		
		final InvocationResult result = BusinessDelegate
				.transferOperations(operations.stream().map(Operatiune::getId).collect(toImmutableSet()), gestiuneId);
		if (!result.statusOk())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
			return;
		}

		// 3. update bonCasa and reload affected products
		updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
		productsToReload.forEach(p -> reloadProduct(p, false));
	}


	/**
	 * Open {@link #InchideBonWizardDialog}
	 */
	@Override
	public void closeBon(final TipInchidere tipInchidere)
	{
		if (bonCasa != null)
		{
			final InchideBonWizardDialog wizardDialog = new InchideBonWizardDialog(
					Display.getCurrent().getActiveShell(),
					new InchideBonWizard(bonCasa, casaActivaButton.getSelection(), sync, bundle, log, tipInchidere));
			if (wizardDialog.open() == Window.OK)
			{
				updateBonCasa(null, false);
				// delivery clear
				partner.setText(EMPTY_STRING);
				partner.clearSelection();
				partner.deselectAll();
				reloadPartners();
			}
			denumireText.setFocus();
		}
	}
	
	@Override
	public Logger log()
	{
		return log;
	}

	@Override
	public AccountingDocument getBonCasa()
	{
		return bonCasa;
	}
	
	@Override
	public Bundle getBundle()
	{
		return bundle;
	}
	
	@Override
	public List<Product> selection()
	{
		return allProductsTable.selection();
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		allProductsTable.filterMode(filterMode);
		allProductsTable.filter(denumireText.getText());
	}
	
	private Optional<Partner> partner()
	{
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allPartners.get(index));
	}
}
