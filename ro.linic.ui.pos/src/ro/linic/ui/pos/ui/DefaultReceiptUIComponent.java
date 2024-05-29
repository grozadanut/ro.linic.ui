package ro.linic.ui.pos.ui;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import jakarta.inject.Inject;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.pos.Messages;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptUIComponent;
import ro.linic.ui.pos.base.services.ProductDataHolder;

public class DefaultReceiptUIComponent implements ReceiptUIComponent {
	private static final Column skuColumn = new Column(0, Product.SKU_FIELD, Messages.SKU, 70);
	private static final Column nameColumn = new Column(1, Product.NAME_FIELD, Messages.Name, 300);
	private static final Column uomColumn = new Column(2, Product.UOM_FIELD, Messages.UOM, 50);
	private static final Column priceColumn = new Column(3, Product.PRICE_FIELD, Messages.Price, 70);
	
	private static final List<Column> allProductsColumns = List.of(skuColumn, nameColumn, uomColumn, priceColumn);
	
	// services
	private ProductDataHolder productDataHolder;
	
	// model
	private Receipt receipt;
	
	// UI
	private SashForm verticalSash;
	private SashForm horizontalSash;
	
	private FullFeaturedNatTable<Product> allProductsTable;

	@Inject
	public DefaultReceiptUIComponent(final ProductDataHolder productDataHolder) {
		this.productDataHolder = productDataHolder;
	}

	@Override
	public void postConstruct(final Composite parent) {
		parent.setLayout(new GridLayout());
//		createTopBar(parent);

		verticalSash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(verticalSash);
		createTopArea(verticalSash);
//		
//		bonDeschisTable = new TreeOperationsNatTable(TreeOperationsNatTable.SourceLoc.VANZARI);
//		bonDeschisTable.postConstruct(verticalSash);
//		bonDeschisTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(bonDeschisTable.getTable());
//
//		createTotalsBar(parent);
//		createBottomBar(parent);
//		
//		loadVisualState();
//		addListeners();
//		loadData();
	}
	
	private void createTopArea(final Composite parent) {
	    horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
	    GridDataFactory.fillDefaults().applyTo(horizontalSash);
		
		final Composite leftContainer = new Composite(horizontalSash, SWT.NONE);
		leftContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().applyTo(leftContainer);
		
//		searchMode = new Combo(leftContainer, SWT.DROP_DOWN);
//		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
//		searchMode.select(0);
//		UIUtils.setFont(searchMode);
//		GridDataFactory.fillDefaults().applyTo(searchMode);
//
//		denumireText = new Text(leftContainer, SWT.BORDER);
//		denumireText.setMessage(Messages.VanzarePart_Name);
//		denumireText.setTextLimit(255);
//		denumireText.setFocus();
//		UIUtils.setBannerFont(denumireText);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(denumireText);
//
//		cantitateText = new Text(leftContainer, SWT.BORDER);
//		cantitateText.setMessage(Messages.VanzarePart_Quant);
//		cantitateText.setText("1"); //$NON-NLS-1$
//		cantitateText.setTextLimit(10);
//		UIUtils.setBannerFont(cantitateText);
//		GridDataFactory.fillDefaults().applyTo(cantitateText);
//		
//		inchideCasaButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
//		inchideCasaButton.setText(Messages.VanzarePart_CloseCash);
//		inchideCasaButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
//		inchideCasaButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
//		UIUtils.setFont(inchideCasaButton);
//		GridDataFactory.fillDefaults()
//			.grab(false, true)
//			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
//			.applyTo(inchideCasaButton);
//		
//		inchideFacturaBCButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
//		inchideFacturaBCButton.setText(Messages.VanzarePart_CloseInvoice);
//		inchideFacturaBCButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
//		inchideFacturaBCButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
//		UIUtils.setFont(inchideFacturaBCButton);
//		GridDataFactory.fillDefaults()
//			.grab(true, true)
//			.align(SWT.RIGHT, SWT.FILL)
//			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
//			.applyTo(inchideFacturaBCButton);
//		
//		inchideCardButton = new Button(leftContainer, SWT.PUSH | SWT.WRAP);
//		inchideCardButton.setText(Messages.VanzarePart_CloseCard);
//		inchideCardButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//		inchideCardButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
//		UIUtils.setFont(inchideCardButton);
//		GridDataFactory.fillDefaults()
//			.grab(false, true)
//			.align(SWT.RIGHT, SWT.FILL)
//			.hint(InchideBonWizard.BUTTON_WIDTH, SWT.DEFAULT)
//			.applyTo(inchideCardButton);
//		
//		casaActivaButton = new Button(leftContainer, SWT.CHECK);
//		casaActivaButton.setText(Messages.VanzarePart_ECRActive);
//		casaActivaButton.setSelection(true);
//		casaActivaButton.setEnabled(ClientSession.instance().hasPermission(Permissions.CLOSE_WITHOUT_CASA));
//		setFont(casaActivaButton);
//		GridDataFactory.swtDefaults().span(2, 1).applyTo(casaActivaButton);
//
//		ofertaPretButton = new Button(leftContainer, SWT.PUSH);
//		ofertaPretButton.setText(Messages.VanzarePart_Offer);
//		setFont(ofertaPretButton);
//		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).applyTo(ofertaPretButton);
		
		allProductsTable = TableBuilder.with(Product.class, allProductsColumns, productDataHolder.getData())
				.build(horizontalSash);
		allProductsTable.natTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(allProductsTable.natTable());
	}
	
//	private void createTotalsBar(final Composite parent)
//	{
//		final Composite totalsContainer = new Composite(parent, SWT.NONE);
//		totalsContainer.setLayout(new GridLayout(6, false));
//		totalsContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//		final Label totalFaraTVAHint = new Label(totalsContainer, SWT.NONE);
//		totalFaraTVAHint.setText(Messages.VanzarePart_TotalNoVAT);
//
//		totalFaraTVALabel = new Label(totalsContainer, SWT.BORDER);
//		totalFaraTVALabel.setText("0"); //$NON-NLS-1$
//		totalFaraTVALabel.setAlignment(SWT.RIGHT);
//		totalFaraTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
//		totalFaraTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		final GridData faraTvaGD = new GridData();
//		faraTvaGD.widthHint = 200;
//		totalFaraTVALabel.setLayoutData(faraTvaGD);
//		UIUtils.setBoldBannerFont(totalFaraTVALabel);
//
//		final Label totalTVAHint = new Label(totalsContainer, SWT.NONE);
//		totalTVAHint.setText(Messages.VanzarePart_VAT);
//
//		tvaLabel = new Label(totalsContainer, SWT.BORDER);
//		tvaLabel.setText("0"); //$NON-NLS-1$
//		tvaLabel.setAlignment(SWT.RIGHT);
//		tvaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
//		tvaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		final GridData tvaGD = new GridData();
//		tvaGD.widthHint = 200;
//		tvaLabel.setLayoutData(tvaGD);
//		UIUtils.setBoldBannerFont(tvaLabel);
//
//		final Label totalCuTVAHint = new Label(totalsContainer, SWT.NONE);
//		totalCuTVAHint.setText(Messages.VanzarePart_TotalWVAT);
//
//		totalCuTVALabel = new Label(totalsContainer, SWT.BORDER);
//		totalCuTVALabel.setText("0"); //$NON-NLS-1$
//		totalCuTVALabel.setAlignment(SWT.CENTER);
//		totalCuTVALabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
//		totalCuTVALabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		totalCuTVALabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		UIUtils.setBoldCustomFont(totalCuTVALabel, XXX_BANNER_FONT);
//	}
//	
//	private void createBottomBar(final Composite parent)
//	{
//		final Composite footerContainer = new Composite(parent, SWT.NONE);
//		footerContainer.setLayout(new GridLayout(7, false));
//		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//		incarcaBonuriButton = new Button(footerContainer, SWT.PUSH);
//		incarcaBonuriButton.setText(Messages.VanzarePart_LoadDoc);
//		UIUtils.setBannerFont(incarcaBonuriButton);
//
//		refreshButton = new Button(footerContainer, SWT.PUSH);
//		refreshButton.setText(Messages.Refresh);
//		UIUtils.setBannerFont(refreshButton);
//
//		managerCasaButton = new Button(footerContainer, SWT.PUSH);
//		managerCasaButton.setText(Messages.VanzarePart_ECRManager);
//		UIUtils.setBannerFont(managerCasaButton);
//		
//		printPlanuriButton = new Button(footerContainer, SWT.PUSH);
//		printPlanuriButton.setText(Messages.VanzarePart_PrintRoutes);
//		UIUtils.setBannerFont(printPlanuriButton);
//		
//		transferaButton = new Button(footerContainer, SWT.PUSH);
//		transferaButton.setText(Messages.Transfer);
//		transferaButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
//		transferaButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		final GridData transferaGD = new GridData(GridData.FILL_HORIZONTAL);
//		transferaGD.horizontalAlignment = SWT.RIGHT;
//		transferaButton.setLayoutData(transferaGD);
//		UIUtils.setBoldBannerFont(transferaButton);
//
//		cancelBonButton = new Button(footerContainer, SWT.PUSH);
//		cancelBonButton.setText(Messages.VanzarePart_CancelDoc);
//		cancelBonButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//		cancelBonButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		final GridData cancelGD = new GridData();
//		cancelGD.horizontalAlignment = SWT.RIGHT;
//		cancelBonButton.setLayoutData(cancelGD);
//		UIUtils.setBoldBannerFont(cancelBonButton);
//
//		stergeRandButton = new Button(footerContainer, SWT.PUSH);
//		stergeRandButton.setText(Messages.VanzarePart_DeleteRow);
//		stergeRandButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//		stergeRandButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		final GridData stergeGD = new GridData();
//		stergeGD.horizontalAlignment = SWT.RIGHT;
//		stergeRandButton.setLayoutData(stergeGD);
//		UIUtils.setBoldBannerFont(stergeRandButton);
//	}

	@Override
	public boolean canCloseReceipt() {
		return receipt != null;
	}

	@Override
	public void closeReceipt(final PaymentType paymentType) {
		// TODO Auto-generated method stub

	}
}
