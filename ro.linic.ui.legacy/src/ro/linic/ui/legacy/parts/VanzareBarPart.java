package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.isNumeric;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.linic.ui.legacy.session.UIUtils.XXX_BANNER_FONT;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TransferType;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.ManagerCasaDialog;
import ro.linic.ui.legacy.dialogs.VanzariIncarcaDocDialog;
import ro.linic.ui.legacy.mapper.OperatiuneMapper;
import ro.linic.ui.legacy.mapper.ProductMapper;
import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.vanzari_bar.UIBonDeschisNatTable;
import ro.linic.ui.legacy.tables.vanzari_bar.UIProductsSimpleTable;
import ro.linic.ui.legacy.wizards.InchideBonWizard;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;
import ro.linic.ui.legacy.wizards.InchideBonWizardDialog;
import ro.linic.ui.pos.base.dialogs.CloseReceiptDialog;
import ro.linic.ui.pos.base.dialogs.CloseReceiptDialog.Builder;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.services.ProductDataHolder;
import ro.linic.ui.pos.base.services.ProductDataUpdater;
import ro.linic.ui.pos.base.services.ReceiptLineUpdater;
import ro.linic.ui.pos.base.services.ReceiptLoader;
import ro.linic.ui.pos.base.services.ReceiptUpdater;
import ro.linic.ui.pos.cloud.model.CloudReceipt;

public class VanzareBarPart implements VanzareInterface, IMouseAction {
	public static final String PART_ID = "linic_gest_client.part.vanzari_bar"; //$NON-NLS-1$
	public static final String PART_DESCRIPTOR_ID = "linic_gest_client.partdescriptor.vanzari_bar"; //$NON-NLS-1$

	public static final String PRINT_AROMA_KEY = "print_aroma"; //$NON-NLS-1$
	public static final String PRINT_AROMA_DEFAULT = "false"; //$NON-NLS-1$

	private static final String PRODUCTS_TABLE_STATE_PREFIX = "vanzari_bar.all_products_nt"; //$NON-NLS-1$
	private static final String BON_DESCHIS_TABLE_STATE_PREFIX = "vanzari_bar.bon_deschis_nt"; //$NON-NLS-1$
	private static final String INITIAL_PART_LOAD_PROP = "vanzari_bar.initial_part_load"; //$NON-NLS-1$
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "vanzari_bar.horizontal_sash"; //$NON-NLS-1$

	// MODEL
	private CloudReceipt bonCasa;
	// loaded from VanzariIncarcaDocDialog
	private AccountingDocument loadedBonCasa;

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
	private UIProductsSimpleTable allProductsTable;

	private Button inchideCasaButton;
	private Button inchideFacturaBCButton;
	private Button inchideCardButton;

//	private Button casaActivaButton;
	private UIBonDeschisNatTable bonDeschisTable;

	private Label totalFaraTVALabel;
	private Label tvaLabel;
	private Label totalCuTVALabel;
//	private Button retetaButton;
	private Button incarcaBonuriButton;
	private Button refreshButton;
	private Button managerCasaButton;
	private Button cancelBonButton;
	private Button stergeRandButton;

	@Inject private ProductDataHolder productDataHolder;
	@Inject private ProductDataUpdater productDataUpdater;
	@Inject private ReceiptLoader receiptLoader;
	@Inject private ReceiptUpdater receiptUpdater;
	@Inject private ReceiptLineUpdater receiptLineUpdater;

	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	@Inject IEclipseContext ctx;

	public static VanzareBarPart newPartForBon(final EPartService partService, final CloudReceipt bon) {
		final MPart createdPart = partService.showPart(partService.createPart(VanzareBarPart.PART_DESCRIPTOR_ID),
				PartState.VISIBLE);

		if (createdPart == null)
			return null;

		final VanzareBarPart vanzarePart = (VanzareBarPart) createdPart.getObject();

		if (vanzarePart == null)
			return null;

		vanzarePart.loadReceipt(bon, true);
		return vanzarePart;
	}

	@PostConstruct
	public void createComposite(final Composite parent) {
		tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));

		if (Boolean.valueOf((String) ClientSession.instance().getProperties().getOrDefault(INITIAL_PART_LOAD_PROP,
				Boolean.TRUE.toString()))) {
			ClientSession.instance().getProperties().setProperty(INITIAL_PART_LOAD_PROP, Boolean.FALSE.toString());
			receiptLoader.findUnclosed().stream().map(CloudReceipt.class::cast)
			.forEach(unclosedReceipt -> newPartForBon(partService, unclosedReceipt));
			reloadProduct(null, false);
		}

		parent.setLayout(new GridLayout());
		createTopBar(parent, partService, bundle, log);

		search = new Text(parent, SWT.BORDER);
		search.setMessage(Messages.VanzareBarPart_Name);
		search.setTextLimit(255);
		search.setFocus();
		UIUtils.setBannerFont(search);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(search);

		horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(horizontalSash);

		allProductsTable = new UIProductsSimpleTable(bundle, log, productDataHolder.getData());
		allProductsTable.doubleClickAction(this);
		allProductsTable.postConstruct(horizontalSash);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(allProductsTable.getTable());

		createRightArea(horizontalSash);

		createTotalsBar(parent);
		createBottomBar(parent);
		parent.setTabList(new Control[] { search, horizontalSash });

		loadVisualState();
		addListeners();
	}

	private void createRightArea(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().applyTo(container);

		cantitateText = new Text(container, SWT.BORDER);
		cantitateText.setMessage(Messages.VanzareBarPart_Quant);
		cantitateText.setText("1"); //$NON-NLS-1$
		cantitateText.setTextLimit(10);
		UIUtils.setBannerFont(cantitateText);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(cantitateText);

		minusOne = new Button(container, SWT.PUSH);
		minusOne.setText("-1"); //$NON-NLS-1$
		UIUtils.setBannerFont(minusOne);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(minusOne);

		plusOne = new Button(container, SWT.PUSH);
		plusOne.setText("+1"); //$NON-NLS-1$
		UIUtils.setBannerFont(plusOne);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(plusOne);

		plusFive = new Button(container, SWT.PUSH);
		plusFive.setText("+5"); //$NON-NLS-1$
		UIUtils.setBannerFont(plusFive);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(plusFive);

		deleteCant = new Button(container, SWT.PUSH);
		deleteCant.setText(Messages.VanzareBarPart_DEL);
		UIUtils.setBannerFont(deleteCant);
		GridDataFactory.swtDefaults().hint(60, 60).applyTo(deleteCant);

		enter = new Button(container, SWT.PUSH);
		enter.setText(Messages.VanzareBarPart_ENTER);
		UIUtils.setBannerFont(enter);
		GridDataFactory.swtDefaults().span(3, 1).hint(100, 60).applyTo(enter);

		bonDeschisTable = new UIBonDeschisNatTable();
		bonDeschisTable.postConstruct(container);
		bonDeschisTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(4, 1).applyTo(bonDeschisTable.getTable());

//		casaActivaButton = new Button(container, SWT.CHECK);
//		casaActivaButton.setText(Messages.VanzareBarPart_ECRActive);
//		casaActivaButton.setSelection(true);
//		casaActivaButton.setEnabled(ClientSession.instance().hasPermission(Permissions.CLOSE_WITHOUT_CASA));
//		setFont(casaActivaButton);
//		GridDataFactory.swtDefaults().span(4, 1).applyTo(casaActivaButton);

		final Composite buttonsContainer = new Composite(container, SWT.NONE);
		buttonsContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(buttonsContainer);

		inchideCasaButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideCasaButton.setText(Messages.VanzareBarPart_CloseCash);
		inchideCasaButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideCasaButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCasaButton);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT / 2)
				.applyTo(inchideCasaButton);

		inchideFacturaBCButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideFacturaBCButton.setText(Messages.VanzareBarPart_CloseInvoice);
		inchideFacturaBCButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideFacturaBCButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideFacturaBCButton);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.RIGHT, SWT.CENTER)
				.hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT / 2)
				.applyTo(inchideFacturaBCButton);

		inchideCardButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		inchideCardButton.setText(Messages.VanzareBarPart_CloseCard);
		inchideCardButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		inchideCardButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCardButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER)
				.hint(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT / 2).applyTo(inchideCardButton);

		container.setTabList(new Control[] { cantitateText });
		parent.setTabList(new Control[] { container });
	}

	private void createTotalsBar(final Composite parent) {
		final Composite totalsContainer = new Composite(parent, SWT.NONE);
		totalsContainer.setLayout(new GridLayout(6, false));
		totalsContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Label totalFaraTVAHint = new Label(totalsContainer, SWT.NONE);
		totalFaraTVAHint.setText(Messages.VanzareBarPart_TotalNoVAT);

		totalFaraTVALabel = new Label(totalsContainer, SWT.BORDER);
		totalFaraTVALabel.setText("0"); //$NON-NLS-1$
		totalFaraTVALabel.setAlignment(SWT.RIGHT);
		totalFaraTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalFaraTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData faraTvaGD = new GridData();
		faraTvaGD.widthHint = 200;
		totalFaraTVALabel.setLayoutData(faraTvaGD);
		UIUtils.setBoldBannerFont(totalFaraTVALabel);

		final Label totalTVAHint = new Label(totalsContainer, SWT.NONE);
		totalTVAHint.setText(Messages.VanzareBarPart_VAT);

		tvaLabel = new Label(totalsContainer, SWT.BORDER);
		tvaLabel.setText("0"); //$NON-NLS-1$
		tvaLabel.setAlignment(SWT.RIGHT);
		tvaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		tvaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData tvaGD = new GridData();
		tvaGD.widthHint = 200;
		tvaLabel.setLayoutData(tvaGD);
		UIUtils.setBoldBannerFont(tvaLabel);

		final Label totalCuTVAHint = new Label(totalsContainer, SWT.NONE);
		totalCuTVAHint.setText(Messages.VanzareBarPart_TotalWVAT);

		totalCuTVALabel = new Label(totalsContainer, SWT.BORDER);
		totalCuTVALabel.setText("0"); //$NON-NLS-1$
		totalCuTVALabel.setAlignment(SWT.CENTER);
		totalCuTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalCuTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		totalCuTVALabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		UIUtils.setBoldCustomFont(totalCuTVALabel, XXX_BANNER_FONT);
	}

	private void createBottomBar(final Composite parent) {
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(5, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

//		retetaButton = new Button(footerContainer, SWT.PUSH);
//		retetaButton.setText(Messages.VanzareBarPart_Recipe);
//		UIUtils.setBannerFont(retetaButton);

		incarcaBonuriButton = new Button(footerContainer, SWT.PUSH);
		incarcaBonuriButton.setText(Messages.VanzareBarPart_LoadDoc);
		UIUtils.setBannerFont(incarcaBonuriButton);

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText(Messages.Refresh);
		UIUtils.setBannerFont(refreshButton);

		managerCasaButton = new Button(footerContainer, SWT.PUSH);
		managerCasaButton.setText(Messages.VanzareBarPart_ECRManager);
		UIUtils.setBannerFont(managerCasaButton);

		cancelBonButton = new Button(footerContainer, SWT.PUSH);
		cancelBonButton.setText(Messages.VanzareBarPart_CancelReceipt);
		cancelBonButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		cancelBonButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData cancelGD = new GridData(GridData.FILL_HORIZONTAL);
		cancelGD.horizontalAlignment = SWT.RIGHT;
		cancelBonButton.setLayoutData(cancelGD);
		UIUtils.setBoldBannerFont(cancelBonButton);

		stergeRandButton = new Button(footerContainer, SWT.PUSH);
		stergeRandButton.setText(Messages.VanzareBarPart_DeleteRow);
		stergeRandButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		stergeRandButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData stergeGD = new GridData();
		stergeGD.horizontalAlignment = SWT.RIGHT;
		stergeRandButton.setLayoutData(stergeGD);
		UIUtils.setBoldBannerFont(stergeRandButton);
	}

	@PersistState
	public void persistVisualState() {
		saveState(PRODUCTS_TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
		saveState(BON_DESCHIS_TABLE_STATE_PREFIX, bonDeschisTable.getTable(), part);

		final int[] horizontalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX + ".0", String.valueOf(horizontalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX + ".1", String.valueOf(horizontalWeights[1])); //$NON-NLS-1$
	}

	private void loadVisualState() {
		loadState(BON_DESCHIS_TABLE_STATE_PREFIX, bonDeschisTable.getTable(), part);
		loadState(PRODUCTS_TABLE_STATE_PREFIX, allProductsTable.getTable(), part);

		final int[] horizontalWeights = new int[2];
		horizontalWeights[0] = Integer
				.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX + ".0", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalWeights[1] = Integer
				.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX + ".1", "100")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalSash.setWeights(horizontalWeights);
	}

	@Focus
	public void setFocus() {
		search.setFocus();
	}

	private void addListeners() {
		search.addModifyListener(e -> allProductsTable.filter(search.getText()));
		search.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR))
					if (selectedProduct().isPresent())
						addNewOperationToBon(selectedProduct());
					else
						cantitateText.setFocus();

				if (e.keyCode == SWT.ARROW_DOWN) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
				}
				if (e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.UP);
				}
			}
		});

		cantitateText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && isNumeric(cantitateText.getText()))
					addNewOperationToBon(selectedProduct());

				if (e.keyCode == SWT.ARROW_DOWN) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
				}
				if (e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.UP);
				}
			}
		});

		minusOne.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				cantitateText.setText(parse(cantitateText.getText()).subtract(BigDecimal.ONE).toString());
				search.setFocus();
			}
		});

		plusOne.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				cantitateText.setText(parse(cantitateText.getText()).add(BigDecimal.ONE).toString());
				search.setFocus();
			}
		});

		plusFive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				cantitateText.setText(parse(cantitateText.getText()).add(new BigDecimal("5")).toString()); //$NON-NLS-1$
				search.setFocus();
			}
		});

		deleteCant.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				cantitateText.setText("1"); //$NON-NLS-1$
				search.setFocus();
			}
		});

		enter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (isNumeric(cantitateText.getText()))
					addNewOperationToBon(selectedProduct());
			}
		});

		inchideCasaButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				closeBon(TipInchidere.PRIN_CASA);
			}
		});

		inchideFacturaBCButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				closeBon(TipInchidere.FACTURA_BC);
			}
		});

		inchideCardButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				closeBon(TipInchidere.PRIN_CARD);
			}
		});

		cantitateText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {
				cantitateText.selectAll();
			}
		});

		search.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {
				search.selectAll();
			}
		});

		managerCasaButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				new ManagerCasaDialog(Display.getCurrent().getActiveShell(), log, ctx).open();
			}
		});

//		retetaButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				allProductsTable.selection().stream()
//				.findFirst()
//				.map(p -> BusinessDelegate.productById(p.getId(), bundle, log))
//				.ifPresent(p ->
//				{
//					final String reteta = p.getIngredients().stream()
//							.filter(prm -> prm.getProductFinit().equals(p))
//							.map(prm -> MessageFormat.format("{0} {1} {2}", //$NON-NLS-1$
//									prm.getProductMatPrima().getName(),
//									displayBigDecimal(prm.getQuantity()),
//									prm.getProductMatPrima().getUom()))
//							.collect(Collectors.joining(NEWLINE));
//					MessageDialog.openInformation(retetaButton.getShell(), Messages.VanzareBarPart_Recipe, reteta);
//				});
//			}
//		});

		incarcaBonuriButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final CloudReceipt originalReceipt = bonCasa;
				new VanzariIncarcaDocDialog(Display.getCurrent().getActiveShell(), VanzareBarPart.this, ctx).open();
				loadReceipt(originalReceipt, true);
			}
		});

		stergeRandButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				deleteOperations(bonDeschisTable.selection().stream().collect(toImmutableList()));
			}
		});

		cancelBonButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (bonCasa != null && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
						Messages.VanzareBarPart_CancelDoc, Messages.VanzareBarPart_CancelDocMessage))
					deleteOperations(ImmutableList.copyOf(bonCasa.getLines()));
			}
		});

		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				reloadProduct(null, true);
			}
		});
	}

	@Override
	public void run(final NatTable table, final MouseEvent event) {
		// double click
		if (isNumeric(cantitateText.getText()))
			addNewOperationToBon(allProductsTable.selection().stream().findFirst());
	}

	/**
	 * Legacy method, used in VanzariIncarcaDocDialog, use {@link loadReceipt()}
	 */
	@Deprecated
	@Override
	public void updateBonCasa(final AccountingDocument bonCasa, final boolean updateTotalLabels) {
		this.loadedBonCasa = bonCasa;
		if (this.loadedBonCasa != null)
			bonDeschisTable.loadData(
					this.loadedBonCasa.getOperatiuni().stream().sorted(Comparator.comparing(Operatiune::getId))
							.map(OperatiuneMapper::toLine).collect(toImmutableList()));
		else
			bonDeschisTable.loadData(ImmutableList.of());

		if (updateTotalLabels) {
			totalFaraTVALabel.setText(Optional.ofNullable(this.loadedBonCasa)
					.map(accDoc -> accDoc.getTotal().subtract(accDoc.getTotalTva()))
					.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
			tvaLabel.setText(Optional.ofNullable(this.loadedBonCasa).map(AccountingDocument::getTotalTva)
					.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
			totalCuTVALabel.setText(Optional.ofNullable(this.loadedBonCasa).map(AccountingDocument::getTotal)
					.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
		}
	}

	public void loadReceipt(final CloudReceipt bonCasa, final boolean updateTotalLabels) {
		this.loadedBonCasa = null;
		this.bonCasa = bonCasa;
		if (this.bonCasa != null)
			bonDeschisTable.loadData(this.bonCasa.getLines().stream().sorted(Comparator.comparing(ReceiptLine::getId))
					.collect(toImmutableList()));
		else
			bonDeschisTable.loadData(ImmutableList.of());

		if (updateTotalLabels) {
			totalFaraTVALabel.setText(
					Optional.ofNullable(this.bonCasa).map(receipt -> receipt.total().subtract(receipt.taxTotal()))
							.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
			tvaLabel.setText(Optional.ofNullable(this.bonCasa).map(Receipt::taxTotal)
					.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
			totalCuTVALabel.setText(Optional.ofNullable(this.bonCasa).map(Receipt::total)
					.map(PresentationUtils::displayBigDecimal).orElse("0")); //$NON-NLS-1$
		}
	}

	private Optional<ro.linic.ui.pos.base.model.Product> selectedProduct() {
		return allProductsTable.getSourceData().stream()
				.filter(p -> search.getText().strip().equalsIgnoreCase(p.getSku())).findFirst()
				.or(() -> allProductsTable.selection().stream().findFirst())
				.or(() -> allProductsTable.getFilteredSortedData().size() == 1
						? allProductsTable.getFilteredSortedData().stream().findFirst()
						: Optional.empty());
	}

	private void addNewOperationToBon(final Optional<ro.linic.ui.pos.base.model.Product> product) {
		if (!product.isPresent())
			return;
		
		final BigDecimal cantitate = parse(cantitateText.getText());
		if (bonCasa == null) {
			bonCasa = new CloudReceipt();
			receiptUpdater.create(bonCasa);
		} else {
			/**
			 * The only workflow for the receipt to be synchronized here is:
			 * 1. user opens CloseFacturaBCWizard(thus receipt is synchronized)
			 * 2. user closes the wizard
			 * 3. user tries to add some more lines to this receipt
			 * 
			 * This case is not allowed, either close the receipt, or delete it
			 */
			if (bonCasa.synced())
				throw new UnsupportedOperationException(Messages.VanzareBarPart_UnsuppOpSyncedReceipt);
		}

		final BigDecimal taxPercent = product.map(ro.linic.ui.pos.base.model.Product::getTaxPercentage)
				.orElse(tvaPercentDb);
		final BigDecimal price = NumberUtils.adjustPrice(product.get().getPrice(), cantitate);
		final BigDecimal taxTotal = price.multiply(cantitate).setScale(2, RoundingMode.HALF_EVEN).multiply(taxPercent)
				.setScale(2, RoundingMode.HALF_EVEN);

		final LegacyReceiptLine newReceiptLine = new LegacyReceiptLine(null, product.get().getId(), bonCasa.getId(),
				product.get().getSku(), product.get().getName(), product.get().getUom(), cantitate, price, null,
				product.get().getTaxCode(), product.get().getDepartmentCode(), taxTotal, false,
				ClientSession.instance().getLoggedUser().getSelectedGestiune().getId(),
				ClientSession.instance().getLoggedUser().getId());
		final IStatus result = receiptLineUpdater.create(newReceiptLine);

		if (!result.isOK()) {
			// in case we have any error we just show the error and cancel the
			// operation
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error, result.getMessage());
		} else {
			// everything was okay;
			// - reload bonCasa
			// - clear denumire and cantitate input
			// - set focus on denumire widget
			bonCasa.getLines().add(newReceiptLine);
			loadReceipt(bonCasa, true);
			search.setText(EMPTY_STRING);
			cantitateText.setText("1"); //$NON-NLS-1$
			search.setFocus();
		}
	}

	/**
	 * @param product if null is passed we reload all products
	 */
	private void reloadProduct(final Product product, final boolean showConfirmation) {
		if (product == null) {
			BusinessDelegate.uiCategories(new AsyncLoadData<ProductUiCategory>() {
				@Override
				public void success(final ImmutableList<ProductUiCategory> data) {
					final ImmutableList<ro.linic.ui.pos.base.model.Product> serverProducts = data.stream()
							.flatMap(uiCat -> uiCat.getProducts().stream())
							.sorted(Comparator.comparing(Product::getName)).map(ProductMapper.INSTANCE::from)
							.collect(toImmutableList());

					productDataUpdater.synchronize(serverProducts);

					if (showConfirmation)
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.Success,
								Messages.VanzareBarPart_SuccessMessage);
				}

				@Override
				public void error(final String details) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_ErrorLoading,
							details);
				}
			}, sync, true, false, bundle, log);
		} else {
			final Product updatedProduct = BusinessDelegate.productById(product.getId(), bundle, log);
			if (updatedProduct == null)
				productDataUpdater.delete(List.of(product.getId().longValue()));
			else
				productDataUpdater.updateStock(updatedProduct.getId(),
						updatedProduct.stoc(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		}
	}

	private void deleteOperations(final ImmutableList<ReceiptLine> lines) {
		final ImmutableSet<Long> lineIds = lines.stream().map(ReceiptLine::getId).collect(toImmutableSet());

		final IStatus result = receiptLineUpdater.delete(lineIds);

		if (!result.isOK()) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error, result.getMessage());
			return;
		}

		// update bonCasa(may be deleted) and reload affected products
		loadReceipt((CloudReceipt) receiptLoader.findById(bonCasa.getId()).orElse(null), true);
	}

	@Override
	public void closeBon(final TipInchidere tipInchidere) {
		if (bonCasa != null) {
			if (tipInchidere.equals(TipInchidere.PRIN_CASA) || tipInchidere.equals(TipInchidere.PRIN_CARD)) {
				final PaymentType paymentType = switch (tipInchidere) {
				case PRIN_CASA -> PaymentType.CASH;
				case PRIN_CARD -> PaymentType.CARD;
				default ->
				throw new IllegalArgumentException("Unexpected value: " + tipInchidere);
				};

				final CloseReceiptDialog closeReceiptDialog = Builder.partial(search.getShell(), bonCasa)
						.initialValues(Map.of(paymentType, bonCasa.total()))
						.build();
				ContextInjectionFactory.inject(closeReceiptDialog, ctx);
				
				if (closeReceiptDialog.open() == Window.OK)
					loadReceipt(null, false);
			} else {
				syncReceiptRemote();
				
				final InchideBonWizardDialog wizardDialog = new InchideBonWizardDialog(
						Display.getCurrent().getActiveShell(),
						new InchideBonWizard(BusinessDelegate.reloadDoc(bonCasa.getId()), true, ctx, bundle, log,
								tipInchidere));

				if (wizardDialog.open() == Window.OK)
					loadReceipt(null, false);
			}

			search.setFocus();
		}
	}

	private void syncReceiptRemote() {
		/*
		 * 1. if (!line.synced)
		 * 		Synchronize remote(addToBonCasa for each line)
		 * 2. Update bonCasa.id, bonCasa.synced and bonCasa.lines.synced
		 */
		
		for (final ReceiptLine l : bonCasa.getLines()) {
			final LegacyReceiptLine line = (LegacyReceiptLine) l;
			
			if (!line.synced()) {
				final Long accDocId = bonCasa.synced() ? bonCasa.getId() : null;
				final InvocationResult result = BusinessDelegate.addToBonCasa(line.getSku(), line.getQuantity(), null, accDocId,
						true, TransferType.FARA_TRANSFER, null, bundle, log);
				
				if (result.statusCanceled())
					throw new RuntimeException(result.toTextDescription());
				
				final long oldId = bonCasa.getId();
				final AccountingDocument accDoc = result.extra(InvocationResult.ACCT_DOC_KEY);
				bonCasa.setSynced(true);
				bonCasa.setId(accDoc.getId());
				
				// can call update(receipt) on each line update, because it is idempotent
				final IStatus receiptUpdateResult = receiptUpdater.update(oldId, bonCasa);
				if (!receiptUpdateResult.isOK())
					throw new RuntimeException(receiptUpdateResult.getMessage());
				
				line.setSynced(true);
				line.setReceiptId(bonCasa.getId());
				
				final IStatus lineUpdateResult = receiptLineUpdater.update(line);
				if (!lineUpdateResult.isOK())
					throw new RuntimeException(lineUpdateResult.getMessage());
			}
		}
		
	}

	@Override
	public Logger log() {
		return log;
	}

	@Override
	public AccountingDocument getBonCasa() {
		return loadedBonCasa;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public List<Product> selection() {
		return List.of();
	}

	@Override
	public boolean canCloseReceipt() {
		return bonCasa != null;
	}

	public boolean bonTableNotEmpty() {
		return !bonDeschisTable.getSourceData().isEmpty();
	}
}
