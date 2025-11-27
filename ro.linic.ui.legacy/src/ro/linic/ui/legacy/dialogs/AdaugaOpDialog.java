package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.calculateAdaosPercent;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.smallerThanOrEqual;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.displayPercentage;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.applyAdaosPopup;
import static ro.linic.ui.legacy.session.UIUtils.applyPercentagePopup;
import static ro.linic.ui.legacy.session.UIUtils.setChildrenEnabled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.CasaDepartment;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.NumberAwareStringComparator;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.components.AnafInvoiceLineTray;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.legacy.tables.AllProductsNatTable.SourceLoc;
import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.services.AuthenticationSession;

public class AdaugaOpDialog extends TrayDialog {
	private Point initialLocation;
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	private AuthenticationSession authSession;
	private BiFunction<Operatiune, Optional<Product>, InvocationResult> adaugaOpConsumer;
	private Supplier<AccountingDocument> accDocSupplier;
	private Supplier<Long> partnerIdSupplier;

	private Combo category;
	private Label categoryLabel;
	private Combo searchMode;
	private Button autoCode;
	private Text barcodeName;
	private Text cantitate;
	private Combo gestiune;

	private Text puaFaraTVA;
	private Text valAchFaraTVA;
	private Text valAchTVA;
	private Text pvCuTVA;
	private Text valVanzFaraTVA;
	private Text valVanzTVA;
	private Label adaosPercent;
	private Label adaosLei;

	private Text newDenumire;
	private Text newUM;

	private Text discountPercent;
	private Text discountValue;
	private Button discountOk;

	private Button salvare;
	private Button inchide;

	private AllProductsNatTable allProductsTable;

	private Product produsSelectat;
	private List<GenericValue> anafInvoiceLines;
	private GenericValue selAnafInvoiceLine;
	private ImmutableList<Gestiune> allGestiuni;
	private BigDecimal tvaPercent;
	private BigDecimal tvaExtractDivisor;

	public AdaugaOpDialog(final Shell parentShell, final Point initialLocation, final UISynchronize sync,
			final Bundle bundle, final Logger log,
			final BiFunction<Operatiune, Optional<Product>, InvocationResult> adaugaOpConsumer,
			final Supplier<AccountingDocument> accDocSupplier, final AuthenticationSession authSession,
			final Supplier<Long> partnerIdSupplier, final List<GenericValue> anafInvoiceLines) {
		super(parentShell);
		this.initialLocation = initialLocation;
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
		this.adaugaOpConsumer = adaugaOpConsumer;
		this.accDocSupplier = accDocSupplier;
		this.allGestiuni = BusinessDelegate.allGestiuni();
		this.tvaPercent = Optional.ofNullable(produsSelectat).map(Product::getDepartment)
				.map(CasaDepartment::getTvaPercentage).orElseGet(() -> new BigDecimal(BusinessDelegate
						.persistedProp(PersistedProp.TVA_PERCENT_KEY).getValueOr(PersistedProp.TVA_PERCENT_DEFAULT)));
		this.tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
		this.authSession = authSession;
		this.partnerIdSupplier = partnerIdSupplier;
		this.anafInvoiceLines = anafInvoiceLines;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.AdaugaOpDialog_Add);

		createLeftContainer(contents);
		createRightContainer(contents);
		contents.setTabList(new Control[] { contents.getChildren()[0], contents.getChildren()[1] });

		allProductsTable = new AllProductsNatTable(SourceLoc.MANAGER_ADAUGA, bundle, log);
		allProductsTable.postConstruct(contents);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).minSize(850, 200)
				.applyTo(allProductsTable.getTable());

		addListeners();
		loadData();

		this.category.setText(Product.MARFA_CATEGORY);
		updateCategory(Product.MARFA_CATEGORY);
		setChildrenEnabled(newDenumire.getParent(), false);

		return contents;
	}

	private void createLeftContainer(final Composite parent) {
		final Composite leftContainer = new Composite(parent, SWT.NONE);
		leftContainer.setLayout(new GridLayout(5, false));

		category = new Combo(leftContainer, SWT.DROP_DOWN);
		category.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		category.setItems(Product.ALL_CATEGORIES.toArray(new String[] {}));
		UIUtils.setFont(category);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).span(5, 1)
				.minSize(300, SWT.DEFAULT).applyTo(category);

		final Label searchModeLabel = new Label(leftContainer, SWT.NONE);
		searchModeLabel.setText(Messages.AdaugaOpDialog_Mode);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(searchModeLabel);

		final Label barcodeNameLabel = new Label(leftContainer, SWT.NONE);
		barcodeNameLabel.setText(Messages.AdaugaOpDialog_BarcodeName);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(barcodeNameLabel);

		autoCode = new Button(leftContainer, SWT.PUSH);
		autoCode.setText("A"); //$NON-NLS-1$
		autoCode.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(autoCode);

		final Label cantLabel = new Label(leftContainer, SWT.NONE);
		cantLabel.setText(Messages.AdaugaOpDialog_Quantity);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(cantLabel);

		final Label gestLabel = new Label(leftContainer, SWT.NONE);
		gestLabel.setText("L1/L2"); //$NON-NLS-1$
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(gestLabel);

		searchMode = new Combo(leftContainer, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);

		barcodeName = new Text(leftContainer, SWT.BORDER);
		barcodeName.setTextLimit(255);
		barcodeName.setFocus();
		UIUtils.setFont(barcodeName);
		GridDataFactory.fillDefaults().grab(true, false).minSize(300, SWT.DEFAULT).span(2, 1).applyTo(barcodeName);

		cantitate = new Text(leftContainer, SWT.BORDER);
		cantitate.setText("0"); //$NON-NLS-1$
		cantitate.setTextLimit(11);
		UIUtils.setFont(cantitate);
		GridDataFactory.swtDefaults().hint(50, SWT.DEFAULT).applyTo(cantitate);

		gestiune = new Combo(leftContainer, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getImportName).toArray(String[]::new));
		gestiune.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		UIUtils.setFont(gestiune);
		GridDataFactory.swtDefaults().applyTo(gestiune);

		final Composite newProductContainer = new Composite(leftContainer, SWT.NONE);
		newProductContainer.setLayout(new GridLayout(2, false));
		newProductContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridDataFactory.fillDefaults().grab(true, false).span(5, 1).applyTo(newProductContainer);

		final Label newNameLabel = new Label(newProductContainer, SWT.NONE);
		newNameLabel.setText(Messages.AdaugaOpDialog_Name);
		newNameLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		newNameLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(newNameLabel);

		final Label uomLabel = new Label(newProductContainer, SWT.NONE);
		uomLabel.setText(Messages.UOM);
		uomLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		uomLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(uomLabel);

		newDenumire = new Text(newProductContainer, SWT.BORDER);
		newDenumire.setTextLimit(255);
		UIUtils.setFont(newDenumire);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(newDenumire);

		newUM = new Text(newProductContainer, SWT.BORDER);
		UIUtils.setFont(newUM);
		GridDataFactory.swtDefaults().applyTo(newUM);

		final Composite discountContainer = new Composite(leftContainer, SWT.NONE);
		discountContainer.setLayout(new GridLayout(3, false));
		discountContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		GridDataFactory.fillDefaults().grab(true, false).span(5, 1).applyTo(discountContainer);

		discountPercent = new Text(discountContainer, SWT.BORDER);
		discountPercent.setMessage("%"); //$NON-NLS-1$
		UIUtils.setFont(discountPercent);
		GridDataFactory.fillDefaults().hint(70, SWT.DEFAULT).applyTo(discountPercent);

		discountValue = new Text(discountContainer, SWT.BORDER);
		discountValue.setMessage(Messages.AdaugaOpDialog_Value);
		UIUtils.setFont(discountValue);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(discountValue);

		discountOk = new Button(discountContainer, SWT.PUSH);
		discountOk.setText("Discount");
		discountOk.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
		discountOk.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(discountOk);

		leftContainer.setTabList(new Control[] { barcodeName, cantitate, gestiune });
	}

	private void createRightContainer(final Composite parent) {
		final Composite rightContainer = new Composite(parent, SWT.NONE);
		rightContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().applyTo(rightContainer);

		categoryLabel = new Label(rightContainer, SWT.NONE);
		categoryLabel.setAlignment(SWT.CENTER);
		categoryLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldBannerFont(categoryLabel);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(categoryLabel);

		final Label puaFaraTVALabel = new Label(rightContainer, SWT.NONE);
		puaFaraTVALabel.setText(Messages.AdaugaOpDialog_PUAfTVA);
		GridDataFactory.swtDefaults().applyTo(puaFaraTVALabel);

		final Label valAchFaraTVALabel = new Label(rightContainer, SWT.NONE);
		valAchFaraTVALabel.setText(Messages.AdaugaOpDialog_VAfTVA);
		GridDataFactory.swtDefaults().applyTo(valAchFaraTVALabel);

		final Label valAchTVALabel = new Label(rightContainer, SWT.NONE);
		valAchTVALabel.setText(Messages.AdaugaOpDialog_VATVA);
		GridDataFactory.swtDefaults().applyTo(valAchTVALabel);

		puaFaraTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		puaFaraTVA.setToolTipText(Messages.AdaugaOpDialog_PressCToCopy);
		UIUtils.setFont(puaFaraTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(puaFaraTVA);

		valAchFaraTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(valAchFaraTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valAchFaraTVA);

		valAchTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(valAchTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valAchTVA);

		final Label pvCuTVALabel = new Label(rightContainer, SWT.NONE);
		pvCuTVALabel.setText(Messages.AdaugaOpDialog_PV);
		GridDataFactory.swtDefaults().applyTo(pvCuTVALabel);

		final Label valVanzFaraTVALabel = new Label(rightContainer, SWT.NONE);
		valVanzFaraTVALabel.setText(Messages.AdaugaOpDialog_VVfTVA);
		GridDataFactory.swtDefaults().applyTo(valVanzFaraTVALabel);

		final Label valVanzTVALabel = new Label(rightContainer, SWT.NONE);
		valVanzTVALabel.setText(Messages.AdaugaOpDialog_VVTVA);
		GridDataFactory.swtDefaults().applyTo(valVanzTVALabel);

		pvCuTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		pvCuTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		UIUtils.setFont(pvCuTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(pvCuTVA);

		valVanzFaraTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		valVanzFaraTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		UIUtils.setFont(valVanzFaraTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valVanzFaraTVA);

		valVanzTVA = new Text(rightContainer, SWT.SINGLE | SWT.BORDER);
		valVanzTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		UIUtils.setFont(valVanzTVA);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valVanzTVA);

		adaosPercent = new Label(rightContainer, SWT.NONE);
		adaosPercent.setAlignment(SWT.RIGHT);
		adaosPercent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(adaosPercent);

		adaosLei = new Label(rightContainer, SWT.NONE);
		adaosLei.setAlignment(SWT.RIGHT);
		adaosLei.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(adaosLei);

		salvare = new Button(rightContainer, SWT.PUSH);
		salvare.setText(Messages.AdaugaOpDialog_Save);
		salvare.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		salvare.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(salvare);
		UIUtils.setBoldBannerFont(salvare);

		inchide = new Button(rightContainer, SWT.PUSH);
		inchide.setText(Messages.AdaugaOpDialog_Close);
		inchide.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		inchide.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(inchide);
		UIUtils.setBoldBannerFont(inchide);

		rightContainer.setTabList(
				new Control[] { puaFaraTVA, valAchFaraTVA, valAchTVA, pvCuTVA, valVanzFaraTVA, valVanzTVA });
	}

	@Override
	protected Control createButtonBar(final Composite parent) {
		buttonBar = super.createButtonBar(parent);
		final GridData gd = new GridData();
		gd.exclude = true;
		buttonBar.setLayoutData(gd);
		buttonBar.setVisible(false);
		buttonBar.setEnabled(false);
		return buttonBar;
	}

	@Override
	protected Point getInitialLocation(final Point initialSize) {
		if (this.initialLocation != null)
			return initialLocation;

		return super.getInitialLocation(initialSize);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void addListeners() {
		category.addModifyListener(e -> updateCategory(category.getText()));

		searchMode.addModifyListener(this::filterModeChange);
		barcodeName.addModifyListener(e -> allProductsTable.filter(barcodeName.getText()));
		barcodeName.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.DOWN);
				}
				if (e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					allProductsTable.moveSelection(MoveDirectionEnum.UP);
				}

				Product product = null;

				if (e.keyCode == SWT.F2) {
					final List<Product> selection = allProductsTable.selection();
					if (!selection.isEmpty())
						product = selection.get(0);
					else if (!isEmpty(barcodeName.getText()))
						product = allProductsTable.getSourceData().stream().filter(
								p -> globalIsMatch(p.getBarcode(), barcodeName.getText(), TextFilterMethod.EQUALS))
								.findFirst().orElse(null);

					if (product == null)
						openCreateProductDialog();
					else
						updateProduct(product);
					e.doit = false;
					allProductsTable.clearSelection();
				}
				if (e.keyCode == SWT.F4)
					adaugaOp();

				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					final Optional<Product> matchedProd = allProductsTable.getSourceData().stream()
							.filter(p -> globalIsMatch(p.getBarcode(), barcodeName.getText(), TextFilterMethod.EQUALS))
							.findFirst();
					if (!isEmpty(barcodeName.getText()) && matchedProd.isPresent())
						product = matchedProd.get();
					else
						product = allProductsTable.selection().stream().findFirst().orElse(null);

					if (product == null)
						openCreateProductDialog();
					else
						updateProduct(product);
					e.doit = false;
					allProductsTable.clearSelection();
				}
			}
		});

		autoCode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				barcodeName.setText(
						String.valueOf(BusinessDelegate.firstFreeNumber(Product.class, Product.BARCODE_FIELD)));
			}
		});

		salvare.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				adaugaOp();
			}
		});

		cantitate.addModifyListener(e -> updateAmounts());
		puaFaraTVA.addModifyListener(e -> {
			updateAmounts();

			if (selAnafInvoiceLine != null)
				puaFaraTVA.setBackground(
						!NumberUtils.equal(parse(puaFaraTVA.getText()), selAnafInvoiceLine.getBigDecimal("price"))
								? Display.getCurrent().getSystemColor(SWT.COLOR_RED)
								: null);
		});
		pvCuTVA.addModifyListener(e -> updateAmounts());
		valAchFaraTVA.addModifyListener(e -> updateAdaos());
		valVanzFaraTVA.addModifyListener(e -> updateAdaos());

		inchide.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				close();
			}
		});

		final KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT, e);
					e.doit = false;
				}
				if (e.keyCode == SWT.F4)
					adaugaOp();
			}
		};

		cantitate.addKeyListener(keyListener);
		gestiune.addKeyListener(keyListener);
		puaFaraTVA.addKeyListener(keyListener);
		puaFaraTVA.addKeyListener(KeyListener.keyPressedAdapter(this::copyPriceFromAnafLine));
		valAchFaraTVA.addKeyListener(keyListener);
		valAchTVA.addKeyListener(keyListener);
		pvCuTVA.addKeyListener(keyListener);
		valVanzFaraTVA.addKeyListener(keyListener);
		valVanzTVA.addKeyListener(keyListener);
		applyAdaosPopup(pvCuTVA, () -> parse(puaFaraTVA.getText()).multiply(tvaExtractDivisor),
				result -> pvCuTVA.setText(result.toString()));
		applyPercentagePopup(valAchTVA, () -> parse(valAchFaraTVA.getText()),
				result -> valAchTVA.setText(result.toString()));
		applyPercentagePopup(valVanzTVA, () -> parse(valVanzFaraTVA.getText()),
				result -> valVanzTVA.setText(result.toString()));

		final FocusAdapter selectAllListener = new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {
				((Text) e.widget).selectAll();
			}
		};

		barcodeName.addFocusListener(selectAllListener);
		cantitate.addFocusListener(selectAllListener);
		puaFaraTVA.addFocusListener(selectAllListener);
		valAchFaraTVA.addFocusListener(selectAllListener);
		valAchTVA.addFocusListener(selectAllListener);
		pvCuTVA.addFocusListener(selectAllListener);
		valVanzFaraTVA.addFocusListener(selectAllListener);
		valVanzTVA.addFocusListener(selectAllListener);

		discountPercent.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					discountValue.forceFocus();
					discountValue.selectAll();
					e.doit = false;
				}
			}
		});
		discountPercent.addModifyListener(e -> discountValue.setText(calculateDiscount()));
		discountOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				adaugaDiscount();
			}
		});
	}

	private void loadData() {
		BusinessDelegate.allProducts(new AsyncLoadData<Product>() {
			@Override
			public void success(final ImmutableList<Product> data) {
				allProductsTable.loadData(data, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(),
						ImmutableList.of());

				nextAnafInvoiceLine();
			}

			@Override
			public void error(final String details) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						Messages.AdaugaOpDialog_ErrorLoadingProducts, details);
			}
		}, sync, bundle, log);
	}
	
	private void copyPriceFromAnafLine(final KeyEvent e) {
		if ((e.character == 'c' || e.character == 'C') &&
				selAnafInvoiceLine != null) {
			puaFaraTVA.setText(PresentationUtils.safeString(selAnafInvoiceLine.getString("price")));
			e.doit = false;
		}
	}

	private void updateProduct(final Product product) {
		this.produsSelectat = product;

		this.tvaPercent = Optional.ofNullable(produsSelectat).map(Product::getDepartment)
				.map(CasaDepartment::getTvaPercentage).orElseGet(() -> new BigDecimal(BusinessDelegate
						.persistedProp(PersistedProp.TVA_PERCENT_KEY).getValueOr(PersistedProp.TVA_PERCENT_DEFAULT)));
		this.tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);

		if (product != null) {
			barcodeName.setText(safeString(product, Product::getBarcode));
			newDenumire.setText(safeString(product, Product::getName));
			newUM.setText(safeString(product, Product::getUom));
			puaFaraTVA.setText(safeString(product, Product::getLastBuyingPriceNoTva, BigDecimal::toString));
			pvCuTVA.setText(safeString(product, Product::getPricePerUom, BigDecimal::toString));
			updateAmounts();

			cantitate.setFocus();
			cantitate.selectAll();
		}
	}

	private void openCreateProductDialog() {
		final AdaugaProductDialog adaugaProdDial = new AdaugaProductDialog(getShell(), this::productCreated);
		adaugaProdDial.setBlockOnOpen(false);
		adaugaProdDial.open();
		adaugaProdDial.setCategory(category.getText());
		if (produsSelectat != null)
			adaugaProdDial.setRaion(produsSelectat.getRaion());
		adaugaProdDial.setBarcode(barcodeName.getText());
		adaugaProdDial.setName(newDenumire.getText());
		adaugaProdDial.setUom(newUM.getText());
		adaugaProdDial.setUlpFTVA(parse(puaFaraTVA.getText()));
		adaugaProdDial.setPret(parse(pvCuTVA.getText()));
	}

	private void productCreated(final Product persistedProd) {
		allProductsTable.add(persistedProd);
		updateProduct(persistedProd);
	}

	private void clearAllFields() {
		barcodeName.setText(EMPTY_STRING);
		newDenumire.setText(EMPTY_STRING);
		newUM.setText(EMPTY_STRING);
		puaFaraTVA.setText("0"); //$NON-NLS-1$
		pvCuTVA.setText("0"); //$NON-NLS-1$
		valAchFaraTVA.setText("0"); //$NON-NLS-1$
		valAchTVA.setText("0"); //$NON-NLS-1$
		valVanzFaraTVA.setText("0"); //$NON-NLS-1$
		valVanzTVA.setText("0"); //$NON-NLS-1$
		adaosPercent.setText(Messages.MarginPerc);
		adaosLei.setText(Messages.Margin);
	}

	private void updateAmounts() {
		final BigDecimal valAchFaraTva = parse(puaFaraTVA.getText()).multiply(parse(cantitate.getText())).setScale(2,
				RoundingMode.HALF_EVEN);
		final BigDecimal valVanzFaraTva = parse(pvCuTVA.getText()).multiply(parse(cantitate.getText()))
				.divide(tvaExtractDivisor, 2, RoundingMode.HALF_EVEN);
		final BigDecimal valVanzTva = parse(pvCuTVA.getText()).multiply(parse(cantitate.getText()))
				.subtract(valVanzFaraTva).setScale(2, RoundingMode.HALF_EVEN);

		valAchFaraTVA.setText(valAchFaraTva.toString());
		valAchTVA.setText(valAchFaraTva.multiply(tvaPercent).setScale(2, RoundingMode.HALF_EVEN).toString());
		valVanzFaraTVA.setText(valVanzFaraTva.toString());
		valVanzTVA.setText(valVanzTva.toString());
		updateAdaos();
	}

	private void updateAdaos() {
		final BigDecimal lastBuyingPriceWithTva = parse(puaFaraTVA.getText()).multiply(tvaExtractDivisor);

		adaosPercent.setText(MessageFormat.format(Messages.MarginPercNLS,
				displayPercentage(calculateAdaosPercent(lastBuyingPriceWithTva, parse(pvCuTVA.getText())))));
		adaosPercent.setToolTipText(Messages.MarginPercFormula);
		adaosLei.setText(MessageFormat.format(Messages.MarginNLS,
				displayBigDecimal(parse(valVanzFaraTVA.getText()).subtract(parse(valAchFaraTVA.getText())))));
		adaosLei.setToolTipText(Messages.MarginFormula);
	}

	private void updateCategory(final String category) {
		clearAllFields();
		categoryLabel.setText(category);
		allProductsTable.filterCategory(category);
	}

	private boolean isValid() {
		return produsSelectat != null && selectedGest() != null;
	}

	private void adaugaOp() {
		if (isValid()) {
			final Operatiune op = new Operatiune();
			op.setBarcode(produsSelectat.getBarcode());
			op.setCantitate(parse(cantitate.getText()));
			op.setCategorie(produsSelectat.getCategorie());
			op.setGestiune(selectedGest());
			op.setName(produsSelectat.getName());
			op.setPretUnitarAchizitieFaraTVA(parse(puaFaraTVA.getText()));
			op.setValoareAchizitieFaraTVA(parse(valAchFaraTVA.getText()));
			op.setValoareAchizitieTVA(parse(valAchTVA.getText()));
			op.setPretVanzareUnitarCuTVA(parse(pvCuTVA.getText()));
			op.setValoareVanzareFaraTVA(parse(valVanzFaraTVA.getText()));
			op.setValoareVanzareTVA(parse(valVanzTVA.getText()));
			op.setUom(produsSelectat.getUom());
			final InvocationResult result = adaugaOpConsumer.apply(op, Optional.of(produsSelectat));
			if (result.statusOk() && selAnafInvoiceLine != null)
				receiveAnafInvoiceLine(result);

			allProductsTable.replace(produsSelectat, BusinessDelegate.productById(produsSelectat.getId(), bundle, log));
			updateProduct(null);
			clearAllFields();
			barcodeName.setFocus();

			nextAnafInvoiceLine();
		}
	}
	
	private void adaugaDiscount() {
		if (greaterThan(parse(discountValue.getText()), BigDecimal.ZERO) && accDocSupplier.get() != null) {
			final Operatiune op = new Operatiune();
			op.setBarcode(Product.DISCOUNT_BARCODE);
			op.setName(Product.DISCOUNT_NAME);
			op.setUom(Product.DISCOUNT_UOM);
			op.setCantitate(BigDecimal.ONE.negate());
			op.setCategorie(Product.DISCOUNT_CATEGORY);
			op.setGestiune(selectedGest());

			if (TipDoc.CUMPARARE.equals(accDocSupplier.get().getTipDoc())) {
				op.setPretUnitarAchizitieFaraTVA(parse(discountValue.getText()));
				op.setPretVanzareUnitarCuTVA(BigDecimal.ZERO);
			} else if (TipDoc.VANZARE.equals(accDocSupplier.get().getTipDoc()))
				op.setPretVanzareUnitarCuTVA(parse(discountValue.getText()));

			Operatiune.updateAmounts(op, tvaPercent, tvaExtractDivisor);
			adaugaOpConsumer.apply(op, Optional.empty());
			close();
		}
	}

	private Gestiune selectedGest() {
		return allGestiuni.stream().filter(gest -> gest.getImportName().equalsIgnoreCase(gestiune.getText()))
				.findFirst().orElse(null);
	}

	private String calculateDiscount() {
		if (accDocSupplier.get() == null)
			return BigDecimal.ZERO.toString();

		final BigDecimal valFTva = accDocSupplier.get().getTotal().subtract(accDocSupplier.get().getTotalTva());

		if (smallerThanOrEqual(valFTva, BigDecimal.ZERO))
			return BigDecimal.ZERO.toString();

		return parse(discountPercent.getText()).divide(new BigDecimal(100)).multiply(valFTva)
				.setScale(2, RoundingMode.HALF_EVEN).toString();
	}

	private void filterModeChange(final ModifyEvent e) {
		int filterMode = TextMatcherEditor.CONTAINS;

		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;

		allProductsTable.filterMode(filterMode);
		allProductsTable.filter(barcodeName.getText());
	}
	
	private void nextAnafInvoiceLine() {
		selectAnafInvoiceLine(anafInvoiceLines.stream().sorted(NumberAwareStringComparator.comparing(gv -> gv.getString("id")))
				.filter(gv -> gv.getString("statusId").equalsIgnoreCase("SmsgConsumed"))
				.findFirst()
				.orElse(null));
	}

	private void selectAnafInvoiceLine(final GenericValue selAnafInvoiceLine) {
		this.selAnafInvoiceLine = selAnafInvoiceLine;

		if (getTray() != null)
			closeTray();

		if (selAnafInvoiceLine != null) {
			openTray(new AnafInvoiceLineTray(selAnafInvoiceLine, e -> selectAnafInvoiceLine(null), e -> markReceived()));
			cantitate.setText(selAnafInvoiceLine.getString("quantity"));
			findProduct().ifPresent(productId -> allProductsTable.getSourceData().stream()
					.filter(p -> p.getId().equals(productId)).findFirst().ifPresent(this::updateProduct));
		}
	}

	private Optional<Integer> findProduct() {
		try {
			return RestCaller.get("/rest/s1/moqui-linic-legacy/suppliers/product")
					.internal(authSession.authentication())
					.addUrlParam("vendorPartyId", safeString(partnerIdSupplier.get(), id -> id.toString()))
					.addUrlParam("otherPartyItemId", selAnafInvoiceLine.getString("itemId"))
					.addUrlParam("otherPartyItemName", selAnafInvoiceLine.getString("name"))
					.async(t -> UIUtils.showException(t, sync)).thenApply(rows -> rows.stream().findFirst()).get()
					.map(gv -> gv.getInt("productId"));
		} catch (final Exception e) {
			log.error(e);
			return Optional.empty();
		}
	}

	private void receiveAnafInvoiceLine(final InvocationResult result) {
		try {
			final AccountingDocument accDoc = result.extra(InvocationResult.ACCT_DOC_KEY);

			// run this with a delay from ManagerPart.updateProductSuppliers because of Moqui entity auto 'store' concurrency issues
			Thread.sleep(1000);
			final HttpResponse<String> response = RestCaller
					.post("/rest/s1/moqui-linic-legacy/anafInvoiceLines/receive").internal(authSession.authentication())
					.addUrlParam("systemMessageId", selAnafInvoiceLine.getString("id"))
					.addUrlParam("invoiceId", accDoc.getId().toString())
					.addUrlParam("supplierId", accDoc.getPartner().getId().toString())
					.addUrlParam("productId", produsSelectat.getId().toString())
					.addUrlParam("facilityPartyId", selectedGest().getImportName())
					.addUrlParam("supplierProductId", selAnafInvoiceLine.getString("itemId"))
					.addUrlParam("supplierProductName", selAnafInvoiceLine.getString("name"))
					.asyncRaw(BodyHandlers.ofString()).get();

			if (response.statusCode() != 200) {
				ro.linic.ui.base.services.util.UIUtils.showResult(ValidationStatus.error(response.body()));
				return;
			}

			selAnafInvoiceLine.put("statusId", "SmsgConfirmed");
		} catch (AuthenticationException | InterruptedException | ExecutionException e) {
			log.error(e);
			UIUtils.showException(e);
			return;
		}
	}
	
	private void markReceived() {
		try {
			final HttpResponse<String> response = RestCaller
					.post("/rest/s1/moqui-linic-legacy/anafInvoiceLines/receive").internal(authSession.authentication())
					.addUrlParam("systemMessageId", selAnafInvoiceLine.getString("id"))
					.addUrlParam("invoiceId", safeString(accDocSupplier.get(), AccountingDocument::getId, Object::toString))
					.asyncRaw(BodyHandlers.ofString()).get();

			if (response.statusCode() != 200) {
				ro.linic.ui.base.services.util.UIUtils.showResult(ValidationStatus.error(response.body()));
				return;
			}

			selAnafInvoiceLine.put("statusId", "SmsgConfirmed");
			nextAnafInvoiceLine();
		} catch (AuthenticationException | InterruptedException | ExecutionException e) {
			log.error(e);
			UIUtils.showException(e);
			return;
		}
	}
}
