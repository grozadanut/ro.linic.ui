package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.isNumeric;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.XXX_BANNER_FONT;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setFont;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.embeddable.FidelityCard;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TransferType;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.security.Permissions;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.ManagerCasaDialog;
import ro.linic.ui.legacy.dialogs.VanzariIncarcaDocDialog;
import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.service.PeripheralService;
import ro.linic.ui.legacy.service.SQLiteJDBC;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.vanzari_bar.UIBonDeschisNatTable;
import ro.linic.ui.legacy.tables.vanzari_bar.UICategoriesSimpleTable;
import ro.linic.ui.legacy.tables.vanzari_bar.UIProductsSimpleTable;
import ro.linic.ui.legacy.wizards.InchideBonWizard;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;
import ro.linic.ui.legacy.wizards.InchideBonWizardDialog;

public class VanzareBarPart implements VanzareInterface, IMouseAction
{
	public static final String PART_ID = "linic_gest_client.part.vanzari_bar";
	public static final String PART_DESCRIPTOR_ID = "linic_gest_client.partdescriptor.vanzari_bar";
	
	public static final String PRINT_AROMA_KEY = "print_aroma";
	public static final String PRINT_AROMA_DEFAULT = "false";
	
	private static final String PRODUCTS_TABLE_STATE_PREFIX = "vanzari_bar.all_products_nt";
	private static final String BON_DESCHIS_TABLE_STATE_PREFIX = "vanzari_bar.bon_deschis_nt";
	private static final String INITIAL_PART_LOAD_PROP = "initial_part_load";
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "vanzari_bar.horizontal_sash";

	// MODEL
	private AccountingDocument bonCasa;
	private ImmutableList<RulajPartener> partnersWithFidelityCard;

	private BigDecimal tvaPercentDb;
	
	// UI
	private SashForm horizontalSash;
	
	private Text cantitateText;
	private Button minusOne;
	private Button plusOne;
	private Button plusFive;
	private Button deleteCant;
	private Button enter;
	
	private Text search;
	private UICategoriesSimpleTable uiCategoriesTable;
	private UIProductsSimpleTable allProductsTable;
	
	private Button inchideCasaButton;
	private Button inchideFacturaBCButton;
	private Button inchideCardButton;

	private Text cardOrPhone;
	private Label selectedClientLabel;
	private Button casaActivaButton;
	private UIBonDeschisNatTable bonDeschisTable;

	private Label totalFaraTVALabel;
	private Label tvaLabel;
	private Label totalCuTVALabel;
	private Button retetaButton;
	private Button incarcaBonuriButton;
	private Button refreshButton;
	private Button managerCasaButton;
	private Button cancelBonButton;
	private Button stergeRandButton;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;

	public static VanzareBarPart newPartForBon(final EPartService partService, final AccountingDocument bon)
	{
		final MPart createdPart = partService.showPart(partService.createPart(VanzareBarPart.PART_DESCRIPTOR_ID),
				PartState.VISIBLE);

		if (createdPart == null)
			return null;

		final VanzareBarPart vanzarePart = (VanzareBarPart) createdPart.getObject();

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
		refreshPartners();
		tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		
		if (Boolean.valueOf((String) ClientSession.instance().getProperties().getOrDefault(INITIAL_PART_LOAD_PROP,
				Boolean.TRUE.toString())))
		{
			ClientSession.instance().getProperties().setProperty(INITIAL_PART_LOAD_PROP, Boolean.FALSE.toString());
			BusinessDelegate.unfinishedBonuriCasa().forEach(unfinishedBon -> newPartForBon(partService, unfinishedBon));
		}

		parent.setLayout(new GridLayout());
		createTopBar(parent, partService, bundle, log);

		search = new Text(parent, SWT.BORDER);
		search.setMessage("Denumire");
		search.setTextLimit(255);
		search.setFocus();
		UIUtils.setBannerFont(search);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(search);
		
		horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(horizontalSash);
		
		uiCategoriesTable = new UICategoriesSimpleTable();
		uiCategoriesTable.postConstruct(horizontalSash);
		uiCategoriesTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uiCategoriesTable.getTable());
		
		allProductsTable = new UIProductsSimpleTable(bundle, log);
		allProductsTable.doubleClickAction(this);
		allProductsTable.postConstruct(horizontalSash);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(allProductsTable.getTable());
		
		createRightArea(horizontalSash);

		createTotalsBar(parent);
		createBottomBar(parent);
		
		loadVisualState();
		addListeners();
		loadData();
	}
	
	private void createRightArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().applyTo(container);
		
		cantitateText = new Text(container, SWT.BORDER);
		cantitateText.setMessage("Cantitate");
		cantitateText.setText("1");
		cantitateText.setTextLimit(10);
		UIUtils.setBannerFont(cantitateText);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(cantitateText);
		
		minusOne = new Button(container, SWT.PUSH);
		minusOne.setText("-1");
		UIUtils.setBannerFont(minusOne);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(minusOne);
		
		plusOne = new Button(container, SWT.PUSH);
		plusOne.setText("+1");
		UIUtils.setBannerFont(plusOne);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(plusOne);
		
		plusFive = new Button(container, SWT.PUSH);
		plusFive.setText("+5");
		UIUtils.setBannerFont(plusFive);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(plusFive);
		
		deleteCant = new Button(container, SWT.PUSH);
		deleteCant.setText("DEL");
		UIUtils.setBannerFont(deleteCant);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(deleteCant);
		
		enter = new Button(container, SWT.PUSH);
		enter.setText("ENTER");
		UIUtils.setBannerFont(enter);
		GridDataFactory.swtDefaults().span(3, 1).hint(100, 60).applyTo(enter);
		
		final Composite clientContainer = new Composite(container, SWT.NONE);
		clientContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(clientContainer);
		
		cardOrPhone = new Text(clientContainer, SWT.SINGLE | SWT.BORDER);
		cardOrPhone.setMessage("Card/Telefon Client");
		UIUtils.setBoldFont(cardOrPhone);
		GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).applyTo(cardOrPhone);
		
		selectedClientLabel = new Label(clientContainer, SWT.WRAP);
		UIUtils.setFont(selectedClientLabel);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 60).applyTo(selectedClientLabel);
		
		bonDeschisTable = new UIBonDeschisNatTable();
		bonDeschisTable.postConstruct(container);
		bonDeschisTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(4, 1).applyTo(bonDeschisTable.getTable());
		
		casaActivaButton = new Button(container, SWT.CHECK);
		casaActivaButton.setText("casa marcat activa?");
		casaActivaButton.setSelection(true);
		casaActivaButton.setEnabled(ClientSession.instance().hasPermission(Permissions.CLOSE_WITHOUT_CASA));
		setFont(casaActivaButton);
		GridDataFactory.swtDefaults().span(4, 1).applyTo(casaActivaButton);
		
		final Composite buttonsContainer = new Composite(container, SWT.NONE);
		buttonsContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(buttonsContainer);
		
		inchideCasaButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideCasaButton.setText("Inchidere prin casa - F3");
		inchideCasaButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideCasaButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCasaButton);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT/2).applyTo(inchideCasaButton);
		
		inchideFacturaBCButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideFacturaBCButton.setText("Inchidere prin Factura, BonConsum");
		inchideFacturaBCButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideFacturaBCButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideFacturaBCButton);
		GridDataFactory.fillDefaults()
			.grab(true, false)
			.align(SWT.RIGHT, SWT.CENTER)
			.hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT/2)
			.applyTo(inchideFacturaBCButton);
		
		inchideCardButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideCardButton.setText("Inchidere prin CARD/POS");
		inchideCardButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		inchideCardButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCardButton);
		GridDataFactory.swtDefaults()
			.align(SWT.RIGHT, SWT.CENTER)
			.hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT/2)
			.applyTo(inchideCardButton);
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
		footerContainer.setLayout(new GridLayout(6, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		retetaButton = new Button(footerContainer, SWT.PUSH);
		retetaButton.setText("Reteta");
		UIUtils.setBannerFont(retetaButton);
		
		incarcaBonuriButton = new Button(footerContainer, SWT.PUSH);
		incarcaBonuriButton.setText("Incarca Bon");
		UIUtils.setBannerFont(incarcaBonuriButton);

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText("Refresh");
		UIUtils.setBannerFont(refreshButton);

		managerCasaButton = new Button(footerContainer, SWT.PUSH);
		managerCasaButton.setText("Manager CasaMarcat");
		UIUtils.setBannerFont(managerCasaButton);
		
		cancelBonButton = new Button(footerContainer, SWT.PUSH);
		cancelBonButton.setText("CANCEL BON");
		cancelBonButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		cancelBonButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData cancelGD = new GridData(GridData.FILL_HORIZONTAL);
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
		
		final int[] horizontalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".0", String.valueOf(horizontalWeights[0]));
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".1", String.valueOf(horizontalWeights[1]));
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".2", String.valueOf(horizontalWeights[2]));
	}
	
	private void loadVisualState()
	{
		loadState(BON_DESCHIS_TABLE_STATE_PREFIX, bonDeschisTable.getTable(), part);
		loadState(PRODUCTS_TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
		
		final int[] horizontalWeights = new int[3];
		horizontalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".0", "100"));
		horizontalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".1", "150"));
		horizontalWeights[2] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".2", "150"));
		horizontalSash.setWeights(horizontalWeights);
	}
	
	@Focus
	public void setFocus()
	{
		search.setFocus();
	}

	private void addListeners()
	{
		search.addModifyListener(e -> allProductsTable.filter(search.getText()));
		search.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR))
					cantitateText.setFocus();
				
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
			}
		});
		
		cantitateText.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && isNumeric(cantitateText.getText()))
					addNewOperationToBon();

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
			}
		});
		
		minusOne.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				cantitateText.setText(parse(cantitateText.getText()).subtract(BigDecimal.ONE).toString());
			}
		});
		
		plusOne.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				cantitateText.setText(parse(cantitateText.getText()).add(BigDecimal.ONE).toString());
			}
		});
		
		plusFive.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				cantitateText.setText(parse(cantitateText.getText()).add(new BigDecimal("5")).toString());
			}
		});
		
		deleteCant.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				cantitateText.setText("1");
			}
		});
		
		enter.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (isNumeric(cantitateText.getText()))
					addNewOperationToBon();
				changeClient();
			}
		});
		
		uiCategoriesTable.addSelectionListener(new ISelectionChangedListener()
		{
			@Override public void selectionChanged(final SelectionChangedEvent event)
			{
				uiCategoriesTable.selection().stream()
				.findFirst()
				.ifPresent(uiCat -> allProductsTable.loadData(uiCat.getProducts().stream()
						.sorted(Comparator.comparing(Product::getName))
						.collect(toImmutableList())));
			}
		});
		
		cardOrPhone.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetDefaultSelected(final SelectionEvent e)
			{
				if (bonCasa == null)
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Bon lipsa!",
							"Prima data trebuie adaugate produsele, apoi scanat cardul!");
					return;
				}
				
				if (ClientSession.instance().isOfflineMode())
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Not accepted!",
							"Functia nu este activa in modul OFFLINE!");
					return;
				}
				
				changeClient();
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
		
		managerCasaButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				new ManagerCasaDialog(Display.getCurrent().getActiveShell(), log).open();
			}
		});
		
		retetaButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				allProductsTable.selection().stream()
				.findFirst()
				.map(p -> BusinessDelegate.productById(p.getId(), bundle, log))
				.ifPresent(p ->
				{
					final String reteta = p.getIngredients().stream()
							.filter(prm -> prm.getProductFinit().equals(p))
							.map(prm -> MessageFormat.format("{0} {1} {2}",
									prm.getProductMatPrima().getName(),
									displayBigDecimal(prm.getQuantity()),
									prm.getProductMatPrima().getUom()))
							.collect(Collectors.joining(NEWLINE));
					MessageDialog.openInformation(retetaButton.getShell(), "Reteta", reteta);
				});
			}
		});
		
		incarcaBonuriButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (ClientSession.instance().isOfflineMode())
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Offline!", "Butonul Incarca Bon nu functioneaza in modul OFFLINE!");
					return;
				}
				
				final AccountingDocument currentBon = BusinessDelegate.reloadDoc(bonCasa);
				new VanzariIncarcaDocDialog(Display.getCurrent().getActiveShell(), VanzareBarPart.this, sync).open();
				updateBonCasa(currentBon, true);
			}
		});

		stergeRandButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				deleteOperations(bonDeschisTable.selection().stream()
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
				if (ClientSession.instance().isOfflineMode())
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Offline!", "Butonul Refresh nu functioneaza in modul OFFLINE!");
					return;
				}
				
				refreshPartners();
				reloadProduct(null, true);
				updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
			}
		});
	}
	
	@Override
	public void run(final NatTable table, final MouseEvent event)
	{
		// double click
		if (isNumeric(cantitateText.getText()))
			addNewOperationToBon();
	}
	
	private void loadData()
	{
		reloadProduct(null, false);
		updateBonCasa(bonCasa, true);
	}
	
	/**
	 * 
	 * @param bonCasa @nullable
	 */
	@Override
	public void updateBonCasa(final AccountingDocument bonCasa, final boolean updateTotalLabels)
	{
		this.bonCasa = bonCasa;
		if (this.bonCasa != null)
			bonDeschisTable.loadData(this.bonCasa.getOperatiuni().stream()
					.sorted(Comparator.comparing(Operatiune::getId))
					.collect(toImmutableList()));
		else
			bonDeschisTable.loadData(ImmutableList.of());
		
		final Optional<RulajPartener> rulaj = findRulaj(Optional.ofNullable(bonCasa)
				.map(AccountingDocument::getPartner)
				.map(Partner::getId)
				.orElse(null));
		selectedClientLabel.setText(safeString(bonCasa, AccountingDocument::getPartner,
				p -> MessageFormat.format("{1} {2}{0}{3}{0}{4}", NEWLINE,
						safeString(p.getFidelityCard(), FidelityCard::getNumber), p.getName(),
						rulaj.map(rp -> rp.getDeIncasat()+" RON").orElse(EMPTY_STRING),
						rulaj.map(rp -> rp.getDiscDisponibil()+" G").orElse(EMPTY_STRING))));
		
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

	private void addNewOperationToBon()
	{
		final BigDecimal cantitate = parse(cantitateText.getText());
		final Optional<Product> product = allProductsTable.selection().stream().findFirst();
		
		if (!product.isPresent())
			return;

		final Long accDocId = Optional.ofNullable(bonCasa).map(AccountingDocument::getId).orElse(null);
		InvocationResult result;
		if (ClientSession.instance().isOfflineMode())
			result = SQLiteJDBC.instance(bundle, log).addToBonCasa(bonCasa, product.get(), cantitate, null,
					ClientSession.instance().getLoggedUser().getSelectedGestiune().getId(),
					ClientSession.instance().getLoggedUser().getId(), tvaPercentDb);
		else
			result = BusinessDelegate.addToBonCasa(product.get().getBarcode(), cantitate, 
					null, accDocId, true, TransferType.FARA_TRANSFER, null, bundle, log);


		if (!result.statusOk())
		{
			// in case we have any error we just show the error and cancel the
			// operation
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
		} else
		{
			// everything was okay;
			// - reload product and bonCasa
			// - clear denumire and cantitate input
			// - set focus on denumire widget
			allProductsTable.replace(product.get(), result.extra(InvocationResult.PRODUCT_KEY));
			updateBonCasa(result.extra(InvocationResult.ACCT_DOC_KEY), true);
			search.setText(EMPTY_STRING);
			cantitateText.setText("1");
			search.setFocus();
		}
	}

	/**
	 * @param product if null is passed we reload all products
	 */
	private void reloadProduct(final Product product, final boolean showConfirmation)
	{
		if (product == null)
		{
			BusinessDelegate.uiCategories(new AsyncLoadData<ProductUiCategory>()
			{
				@Override public void success(final ImmutableList<ProductUiCategory> data)
				{
					uiCategoriesTable.loadData(data);
					uiCategoriesTable.unselectAll();
					allProductsTable.loadData(ImmutableList.of());
					
					if (showConfirmation)
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Succes",
								"Produsele au fost incarcate cu succes!");
				}

				@Override public void error(final String details)
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea produselor", details);
				}
			}, sync, true, false, bundle, log);
		} else
		{
			final Product updatedProduct = BusinessDelegate.productById(product.getId(), bundle, log);
			if (updatedProduct == null)
				allProductsTable.remove(product);
			else
				allProductsTable.replace(product, updatedProduct);
		}
	}
	
	private void refreshPartners()
	{
		partnersWithFidelityCard = BusinessDelegate.puncteFidelitate_Sync();
	}

	private void deleteOperations(final ImmutableList<Operatiune> operations)
	{
		final ImmutableList<Operatiune> deletableOps = collectDeletableOperations(operations);
		final ImmutableSet<Long> deletableOpIds = deletableOps.stream()
				.map(Operatiune::getId)
				.collect(toImmutableSet());
		final ImmutableSet<String> operationBarcodes = deletableOps.stream().map(Operatiune::getBarcode)
				.collect(toImmutableSet());
		final ImmutableList<Product> productsToReload = allProductsTable.getSourceData().parallelStream()
				.filter(p -> operationBarcodes.contains(p.getBarcode())).collect(toImmutableList());

		InvocationResult result;
		if (ClientSession.instance().isOfflineMode())
			result = SQLiteJDBC.instance(bundle, log).deleteOperations(bonCasa, deletableOpIds);
		else
			result = BusinessDelegate.deleteOperations(deletableOpIds);
		
		if (!result.statusOk())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
			return;
		}

		// 3. update bonCasa(may be deleted) and reload affected products
		if (ClientSession.instance().isOfflineMode())
			updateBonCasa(bonCasa != null && !bonCasa.getOperatiuni().isEmpty() ? bonCasa : null,
					true);
		else
		{
			updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
			if (productsToReload.size() > 1)
				reloadProduct(null, false);
			else
				productsToReload.forEach(p -> reloadProduct(p, false));
		}
	}
	
	private void changeClient()
	{
		if (bonCasa == null)
			return;
		
		if (ClientSession.instance().isOfflineMode())
			return;
		
		final InvocationResult result = BusinessDelegate.changeDocPartner(bonCasa.getId(), selectedPartnerId(), true);
		showResult(result);
		if (result.statusOk())
			updateBonCasa(result.extra(InvocationResult.ACCT_DOC_KEY), true);
		search.setFocus();
	}

	/**
	 * Open {@link #InchideBonWizardDialog}
	 */
	@Override
	public void closeBon(final TipInchidere tipInchidere)
	{
		if (bonCasa != null)
		{
			if (ClientSession.instance().isOfflineMode() &&
					!tipInchidere.equals(TipInchidere.PRIN_CASA) && !tipInchidere.equals(TipInchidere.PRIN_CARD))
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Not accepted!",
						"In modul OFFLINE doar inchiderea PRIN CASA sau PRIN CARD este posibila!");
				return;
			}
			
			final InchideBonWizardDialog wizardDialog = new InchideBonWizardDialog(
					Display.getCurrent().getActiveShell(),
					new InchideBonWizard(bonCasa, casaActivaButton.getSelection(), sync, bundle, log, tipInchidere));
			if (wizardDialog.open() == Window.OK)
			{
				if (!ClientSession.instance().isOfflineMode())
					printEtichetaAroma();
				updateBonCasa(null, false);
				cardOrPhone.setText(EMPTY_STRING);
			}
			search.setFocus();
		}
	}
	
	private void printEtichetaAroma()
	{
		if (Boolean.parseBoolean(System.getProperty(PRINT_AROMA_KEY, PRINT_AROMA_DEFAULT)))
		{
			final String prefix = BusinessDelegate.persistedProp(PersistedProp.PREFIX_AROMA_KEY)
					.getValueOr(PersistedProp.PREFIX_AROMA_DEFAULT);
			
			final ImmutableList<Operatiune> ops = bonCasa.getOperatiuni_Stream()
			.filter(op -> globalIsMatch(op.getUiCategory(), ProductUiCategory.AROME_UI_CAT, TextFilterMethod.EQUALS))
			.collect(toImmutableList());
			
			PeripheralService.printPrintables(BarcodePrintable.fromOperations_toAromaLabel(ops, prefix),
					System.getProperty(PeripheralService.BARCODE_PRINTER_KEY, PeripheralService.BARCODE_PRINTER_DEFAULT),
					log, bundle, true, Optional.empty());
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
	
	public boolean bonTableNotEmpty()
	{
		return !bonDeschisTable.getSourceData().isEmpty();
	}
	
	private Optional<Long> selectedPartnerId()
	{
		final String cardOrPhone = this.cardOrPhone.getText();
		if (isEmpty(cardOrPhone))
			return Optional.empty();
		
		return partnersWithFidelityCard.stream()
				.filter(rp -> cardOrPhone.equalsIgnoreCase(rp.getCardNumber()) ||
						cardOrPhone.equalsIgnoreCase(rp.getPhoneNumber()))
				.map(RulajPartener::getId)
				.findFirst();
	}
	
	private Optional<RulajPartener> findRulaj(final Long partnerId)
	{
		if (partnerId == null)
			return Optional.empty();
		
		return partnersWithFidelityCard.stream()
				.filter(rp -> partnerId.equals(rp.getId()))
				.findFirst();
	}
}
