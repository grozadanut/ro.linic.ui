package ro.linic.ui.pos.ui;

import static ro.flexbiz.util.commons.PresentationUtils.EMPTY_STRING;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jakarta.inject.Inject;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.Messages;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.model.ReceiptUIComponent;
import ro.linic.ui.pos.base.services.PosResources;
import ro.linic.ui.pos.base.services.ProductDataHolder;

public class DefaultReceiptUIComponent implements ReceiptUIComponent {
	private static final int CLOSE_BUTTON_WIDTH = 110;
	
	private static final String PRODUCTS_TABLE_STATE_PREFIX = "receipt.products_nt"; //$NON-NLS-1$
	private static final String RECEIPT_TABLE_STATE_PREFIX = "receipt.receipt_nt"; //$NON-NLS-1$
	private static final String VERTICAL_SASH_STATE_PREFIX = "receipt.vertical_sash"; //$NON-NLS-1$
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "receipt.horizontal_sash"; //$NON-NLS-1$
	
	private static final Column p_skuColumn = new Column(0, Product.SKU_FIELD, Messages.SKU, 70);
	private static final Column p_nameColumn = new Column(1, Product.NAME_FIELD, Messages.Name, 350);
	private static final Column p_uomColumn = new Column(2, Product.UOM_FIELD, Messages.UOM, 70);
	private static final Column p_priceColumn = new Column(3, Product.PRICE_FIELD, Messages.Price, 90);
	private static final List<Column> allProductsColumns = List.of(p_skuColumn, p_nameColumn, p_uomColumn, p_priceColumn);
	
	private static final Column rl_nameColumn = new Column(0, ReceiptLine.NAME_FIELD, Messages.Name, 350);
	private static final Column rl_priceColumn = new Column(1, ReceiptLine.PRICE_FIELD, Messages.Price, 90);
	private static final Column rl_uomColumn = new Column(2, ReceiptLine.UOM_FIELD, Messages.UOM, 70);
	private static final Column rl_quantityColumn = new Column(3, ReceiptLine.QUANTITY_FIELD, Messages.Quantity, 90);
	private static final Column rl_totalColumn = new Column(4, EMPTY_STRING, Messages.Total, 70);
	private static final List<Column> allReceiptLineColumns = List.of(rl_nameColumn, rl_priceColumn, rl_uomColumn, rl_quantityColumn,
			rl_totalColumn);
	
	// services
	private ProductDataHolder productDataHolder;
	
	// model
	private Receipt receipt;
	
	// UI
	private SashForm verticalSash;
	private SashForm horizontalSash;
	
	private Text searchText;
	private Text quantityText;
	
	private Button minusOne;
	private Button plusOne;
	private Button plusFive;
	private Button enter;
	private Button del, dot, zero, one, two, three, four, five, six, seven, eight, nine;
	
	private Button closeCashButton;
	private Button closeCardButton;
	private Button closeOtherButton;
	
	private Button ecrActiveButton;
	private Button priceOfferButton;
	
	private FullFeaturedNatTable<Product> productsTable;
	private FullFeaturedNatTable<ReceiptLine> receiptTable;
	
	private Label totalNoTaxLabel;
	private Label taxLabel;
	private Label totalWithTaxLabel;
	
	private Button loadReceiptButton;
	private Button refreshButton;
	private Button ecrManagerButton;
	private Button cancelReceiptButton;
	private Button deleteLineButton;
	
	private MPart part;

	@Inject
	public DefaultReceiptUIComponent(final ProductDataHolder productDataHolder, final MPart part) {
		this.productDataHolder = productDataHolder;
		this.part = part;
	}

	@Override
	public void postConstruct(final Composite parent) {
		parent.setLayout(new GridLayout());
//		createTopBar(parent);

		verticalSash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(verticalSash);
		createTopArea(verticalSash);
		
		receiptTable = TableBuilder.with(ReceiptLine.class, allReceiptLineColumns)
				.build(verticalSash);
		receiptTable.natTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(receiptTable.natTable());

		createTotalsBar(parent);
		createBottomBar(parent);
		
		loadState();
//		addListeners();
	}
	
	private void createTopArea(final Composite parent) {
	    horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
	    GridDataFactory.fillDefaults().applyTo(horizontalSash);
		
		final Composite leftContainer = new Composite(horizontalSash, SWT.NONE);
		leftContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().applyTo(leftContainer);
		
		searchText = new Text(leftContainer, SWT.BORDER);
		searchText.setMessage(Messages.Search);
		searchText.setTextLimit(255);
		searchText.setFocus();
		UIUtils.setBannerFont(searchText);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(searchText);

		quantityText = new Text(leftContainer, SWT.BORDER);
		quantityText.setMessage(Messages.Quantity);
		quantityText.setText("1"); //$NON-NLS-1$
		quantityText.setTextLimit(18);
		UIUtils.setBannerFont(quantityText);
		GridDataFactory.fillDefaults().applyTo(quantityText);
		
		createNumericKeypad(leftContainer);
		
		closeCashButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
		closeCashButton.setText(Messages.CloseCash);
		closeCashButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		closeCashButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(closeCashButton);
		GridDataFactory.fillDefaults()
			.grab(false, true)
			.hint(CLOSE_BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(closeCashButton);
		
		closeCardButton = new Button(leftContainer, SWT.PUSH);
		closeCardButton.setText(PosResources.getString(PaymentType.CARD.toString()));
		closeCardButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		closeCardButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(closeCardButton);
		GridDataFactory.fillDefaults()
			.grab(false, true)
			.align(SWT.RIGHT, SWT.FILL)
			.hint(CLOSE_BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(closeCardButton);
		
		closeOtherButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
		closeOtherButton.setText(Messages.CloseOther);
		closeOtherButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		closeOtherButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(closeOtherButton);
		GridDataFactory.fillDefaults()
			.grab(false, true)
			.align(SWT.RIGHT, SWT.FILL)
			.hint(CLOSE_BUTTON_WIDTH, SWT.DEFAULT)
			.applyTo(closeOtherButton);
		
		ecrActiveButton = new Button(leftContainer, SWT.CHECK);
		ecrActiveButton.setText(Messages.ECRActive);
		ecrActiveButton.setSelection(true);
		UIUtils.setFont(ecrActiveButton);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(ecrActiveButton);

		priceOfferButton = new Button(leftContainer, SWT.PUSH);
		priceOfferButton.setText(Messages.Offer);
		UIUtils.setFont(priceOfferButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).applyTo(priceOfferButton);
		
		productsTable = TableBuilder.with(Product.class, allProductsColumns, productDataHolder.getData())
				.build(horizontalSash);
		productsTable.natTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(productsTable.natTable());
	}
	
	private void createNumericKeypad(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().span(3, 1).applyTo(container);
		
		final int defaultWith = 60;
		final int defaultHeight = 60;
		
		minusOne = new Button(container, SWT.PUSH);
		minusOne.setText("-1"); //$NON-NLS-1$
		UIUtils.setBannerFont(minusOne);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(minusOne);
		
		plusOne = new Button(container, SWT.PUSH);
		plusOne.setText("+1"); //$NON-NLS-1$
		UIUtils.setBannerFont(plusOne);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(plusOne);
		
		plusFive = new Button(container, SWT.PUSH);
		plusFive.setText("+5"); //$NON-NLS-1$
		UIUtils.setBannerFont(plusFive);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(plusFive);
		
		enter = new Button(container, SWT.PUSH);
		enter.setText("ENTER");
		UIUtils.setBannerFont(enter);
		GridDataFactory.swtDefaults().span(1, 5).hint(100, 160).applyTo(enter);
		
		seven = new Button(container, SWT.PUSH);
		seven.setText("7"); //$NON-NLS-1$
		UIUtils.setBannerFont(seven);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(seven);
		
		eight = new Button(container, SWT.PUSH);
		eight.setText("8"); //$NON-NLS-1$
		UIUtils.setBannerFont(eight);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(eight);
		
		nine = new Button(container, SWT.PUSH);
		nine.setText("9"); //$NON-NLS-1$
		UIUtils.setBannerFont(nine);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(nine);
		
		four = new Button(container, SWT.PUSH);
		four.setText("4"); //$NON-NLS-1$
		UIUtils.setBannerFont(four);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(four);
		
		five = new Button(container, SWT.PUSH);
		five.setText("5"); //$NON-NLS-1$
		UIUtils.setBannerFont(five);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(five);
		
		six = new Button(container, SWT.PUSH);
		six.setText("6"); //$NON-NLS-1$
		UIUtils.setBannerFont(six);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(six);
		
		one = new Button(container, SWT.PUSH);
		one.setText("1"); //$NON-NLS-1$
		UIUtils.setBannerFont(one);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(one);
		
		two = new Button(container, SWT.PUSH);
		two.setText("2"); //$NON-NLS-1$
		UIUtils.setBannerFont(two);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(two);
		
		three = new Button(container, SWT.PUSH);
		three.setText("3"); //$NON-NLS-1$
		UIUtils.setBannerFont(three);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(three);
		
		del = new Button(container, SWT.PUSH);
		del.setText("DEL"); //$NON-NLS-1$
		UIUtils.setBannerFont(del);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(del);
		
		zero = new Button(container, SWT.PUSH);
		zero.setText("0"); //$NON-NLS-1$
		UIUtils.setBannerFont(zero);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(zero);
		
		dot = new Button(container, SWT.PUSH);
		dot.setText("."); //$NON-NLS-1$
		UIUtils.setBannerFont(dot);
		GridDataFactory.swtDefaults().hint(defaultWith, defaultHeight).applyTo(dot);
	}

	private void createTotalsBar(final Composite parent) {
		final Composite totalsContainer = new Composite(parent, SWT.NONE);
		totalsContainer.setLayout(new GridLayout(6, false));
		totalsContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Label totalNoTaxHint = new Label(totalsContainer, SWT.NONE);
		totalNoTaxHint.setText(Messages.TotalNoTax);

		totalNoTaxLabel = new Label(totalsContainer, SWT.BORDER);
		totalNoTaxLabel.setText("0"); //$NON-NLS-1$
		totalNoTaxLabel.setAlignment(SWT.RIGHT);
		totalNoTaxLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalNoTaxLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData faraTvaGD = new GridData();
		faraTvaGD.widthHint = 200;
		totalNoTaxLabel.setLayoutData(faraTvaGD);
		UIUtils.setBoldBannerFont(totalNoTaxLabel);

		final Label totalTaxHint = new Label(totalsContainer, SWT.NONE);
		totalTaxHint.setText(Messages.Tax);

		taxLabel = new Label(totalsContainer, SWT.BORDER);
		taxLabel.setText("0"); //$NON-NLS-1$
		taxLabel.setAlignment(SWT.RIGHT);
		taxLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		taxLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData tvaGD = new GridData();
		tvaGD.widthHint = 200;
		taxLabel.setLayoutData(tvaGD);
		UIUtils.setBoldBannerFont(taxLabel);

		final Label totalWithTaxHint = new Label(totalsContainer, SWT.NONE);
		totalWithTaxHint.setText(Messages.TotalWithTax);

		totalWithTaxLabel = new Label(totalsContainer, SWT.BORDER);
		totalWithTaxLabel.setText("0"); //$NON-NLS-1$
		totalWithTaxLabel.setAlignment(SWT.CENTER);
		totalWithTaxLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		totalWithTaxLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		totalWithTaxLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		UIUtils.setBoldCustomFont(totalWithTaxLabel, UIUtils.XXX_BANNER_FONT);
	}
	
	private void createBottomBar(final Composite parent) {
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(5, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		loadReceiptButton = new Button(footerContainer, SWT.PUSH);
		loadReceiptButton.setText(Messages.LoadReceipt);
		UIUtils.setBannerFont(loadReceiptButton);

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText(Messages.Refresh);
		UIUtils.setBannerFont(refreshButton);

		ecrManagerButton = new Button(footerContainer, SWT.PUSH);
		ecrManagerButton.setText(Messages.ECRManager);
		UIUtils.setBannerFont(ecrManagerButton);
		
		cancelReceiptButton = new Button(footerContainer, SWT.PUSH);
		cancelReceiptButton.setText(Messages.CancelReceipt);
		cancelReceiptButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		cancelReceiptButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData cancelGD = new GridData();
		cancelGD.horizontalAlignment = SWT.RIGHT;
		cancelReceiptButton.setLayoutData(cancelGD);
		UIUtils.setBoldBannerFont(cancelReceiptButton);

		deleteLineButton = new Button(footerContainer, SWT.PUSH);
		deleteLineButton.setText(Messages.DeleteLine);
		deleteLineButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		deleteLineButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData stergeGD = new GridData();
		stergeGD.horizontalAlignment = SWT.RIGHT;
		deleteLineButton.setLayoutData(stergeGD);
		UIUtils.setBoldBannerFont(deleteLineButton);
	}
	
	@Override
	public void persistState() {
		UIUtils.saveState(PRODUCTS_TABLE_STATE_PREFIX, productsTable.natTable(), part);
		UIUtils.saveState(RECEIPT_TABLE_STATE_PREFIX, receiptTable.natTable(), part);
		
		final int[] verticalWeights = verticalSash.getWeights();
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".0", String.valueOf(verticalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(VERTICAL_SASH_STATE_PREFIX+".1", String.valueOf(verticalWeights[1])); //$NON-NLS-1$
		final int[] horizontalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".0", String.valueOf(horizontalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".1", String.valueOf(horizontalWeights[1])); //$NON-NLS-1$
	}
	
	private void loadState() {
		UIUtils.loadState(RECEIPT_TABLE_STATE_PREFIX, receiptTable.natTable(), part);
		UIUtils.loadState(PRODUCTS_TABLE_STATE_PREFIX, productsTable.natTable(), part);
		
		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".0", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(VERTICAL_SASH_STATE_PREFIX+".1", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalSash.setWeights(verticalWeights);
		final int[] horizontalWeights = new int[2];
		horizontalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".0", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".1", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalSash.setWeights(horizontalWeights);
	}
	
	@Override
	public void setFocus() {
		searchText.setFocus();
	}

	@Override
	public boolean canCloseReceipt() {
		return receipt != null;
	}

	@Override
	public void closeReceipt(final PaymentType paymentType) {
		// TODO Auto-generated method stub

	}
	
//	private void addListeners() {
//		search.addModifyListener(e -> allProductsTable.filter(search.getText()));
//		search.addKeyListener(new KeyAdapter()
//		{
//			@Override public void keyPressed(final KeyEvent e)
//			{
//				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR))
//					if (selectedProduct().isPresent())
//						addNewOperationToBon(selectedProduct());
//					else
//						cantitateText.setFocus();
//				
//				if (e.keyCode == SWT.ARROW_DOWN)
//				{
//					e.doit = false;
//					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
//				}
//				if (e.keyCode == SWT.ARROW_UP)
//				{
//					e.doit = false;
//					allProductsTable.moveSelection(MoveDirectionEnum.UP);
//				}
//			}
//		});
//		
//		quantityText.addKeyListener(new KeyAdapter()
//		{
//			@Override public void keyPressed(final KeyEvent e)
//			{
//				if ((e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) && isNumeric(cantitateText.getText()))
//					addNewOperationToBon(selectedProduct());
//
//				if (e.keyCode == SWT.ARROW_DOWN)
//				{
//					e.doit = false;
//					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
//				}
//				if (e.keyCode == SWT.ARROW_UP)
//				{
//					e.doit = false;
//					allProductsTable.moveSelection(MoveDirectionEnum.UP);
//				}
//			}
//		});
//		
//		minusOne.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				quantityText.setText(parse(quantityText.getText()).subtract(BigDecimal.ONE).toString());
//				search.setFocus();
//			}
//		});
//		
//		plusOne.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				cantitateText.setText(parse(cantitateText.getText()).add(BigDecimal.ONE).toString());
//				search.setFocus();
//			}
//		});
//		
//		plusFive.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				cantitateText.setText(parse(cantitateText.getText()).add(new BigDecimal("5")).toString()); //$NON-NLS-1$
//				search.setFocus();
//			}
//		});
//		
//		deleteCant.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				cantitateText.setText("1"); //$NON-NLS-1$
//				search.setFocus();
//			}
//		});
//		
//		enter.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				if (isNumeric(cantitateText.getText()))
//					addNewOperationToBon(selectedProduct());
//				changeClient();
//			}
//		});
//		
//		uiCategoriesTable.addSelectionListener(new ISelectionChangedListener()
//		{
//			@Override public void selectionChanged(final SelectionChangedEvent event)
//			{
//				uiCategoriesTable.selection().stream()
//				.findFirst()
//				.ifPresent(uiCat -> allProductsTable.loadData(uiCat.getProducts().stream()
//						.sorted(Comparator.comparing(Product::getName))
//						.collect(toImmutableList())));
//			}
//		});
//		
//		cardOrPhone.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetDefaultSelected(final SelectionEvent e)
//			{
//				if (bonCasa == null)
//				{
//					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_MissingDoc,
//							Messages.VanzareBarPart_MissingDocMessage);
//					return;
//				}
//				
//				if (ClientSession.instance().isOfflineMode())
//				{
//					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_NotAcc,
//							Messages.VanzareBarPart_NotAccMessage);
//					return;
//				}
//				
//				changeClient();
//			}
//		});
//
//		inchideCasaButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				closeBon(TipInchidere.PRIN_CASA);
//			}
//		});
//		
//		inchideFacturaBCButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				closeBon(TipInchidere.FACTURA_BC);
//			}
//		});
//		
//		inchideCardButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				closeBon(TipInchidere.PRIN_CARD);
//			}
//		});
//
//		cantitateText.addFocusListener(new FocusAdapter()
//		{
//			@Override public void focusGained(final FocusEvent e)
//			{
//				cantitateText.selectAll();
//			}
//		});
//		
//		search.addFocusListener(new FocusAdapter()
//		{
//			@Override public void focusGained(final FocusEvent e)
//			{
//				search.selectAll();
//			}
//		});
//		
//		managerCasaButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				new ManagerCasaDialog(Display.getCurrent().getActiveShell(), log, ctx).open();
//			}
//		});
//		
//		incarcaBonuriButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				if (ClientSession.instance().isOfflineMode())
//				{
//					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_Offline, Messages.VanzareBarPart_OfflineMessage);
//					return;
//				}
//				
//				final AccountingDocument currentBon = BusinessDelegate.reloadDoc(bonCasa);
//				new VanzariIncarcaDocDialog(Display.getCurrent().getActiveShell(), VanzareBarPart.this, ctx).open();
//				updateBonCasa(currentBon, true);
//			}
//		});
//
//		stergeRandButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				deleteOperations(bonDeschisTable.selection().stream()
//						.collect(toImmutableList()));
//			}
//		});
//
//		cancelBonButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				if (bonCasa != null && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_CancelDoc, Messages.VanzareBarPart_CancelDocMessage))
//					deleteOperations(ImmutableList.copyOf(bonCasa.getOperatiuni()));
//			}
//		});
//
//		refreshButton.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				if (ClientSession.instance().isOfflineMode())
//				{
//					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.VanzareBarPart_Offline, Messages.VanzareBarPart_OfflineRefreshMessage);
//					return;
//				}
//				
//				refreshPartners();
//				reloadProduct(null, true);
//				updateBonCasa(BusinessDelegate.reloadDoc(bonCasa), true);
//			}
//		});
//	}
}
