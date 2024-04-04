package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.calculateAdaosPercent;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.displayPercentage;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.setChildrenEnabled;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import ro.colibri.embeddable.ProductRecipeMappingId;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.ProductRecipeMapping;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.legacy.tables.AllProductsNatTable.SourceLoc;

public class RetetarDialog extends Dialog
{
	private Product produsFinit;
	private Product produsSelectat;
	
	private Combo category;
	private Label categoryLabel;
	private Combo searchMode;
	private Text barcodeName;
	private Text cantitate;
	
	private Label valAchizitie;
	private Label adaosPercent;
	private Label adaosLei;
	
	private Text prodFinDenumire;
	private Text prodFinUM;
	private Text prodFinPV;
	
	private Button adauga;
	private Button sterge;
	
	private AllProductsNatTable allProductsTable;
	private AllProductsNatTable matPrimaTable;
	
	private Consumer<Product> productConsumer;
	private BigDecimal tvaPercentDb;
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;

	public RetetarDialog(final Shell parent, final UISynchronize sync, final Bundle bundle, final Logger log,
			final Product produsFinit, final Consumer<Product> productConsumer)
	{
		super(parent);
		this.produsFinit = produsFinit;
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
		this.productConsumer = productConsumer;
		this.tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.RetetarDialog_Title);
		
		createLeftContainer(contents);
		createRightContainer(contents);
		contents.setTabList(new Control[]{contents.getChildren()[0], contents.getChildren()[1]});
		
		allProductsTable = new AllProductsNatTable(SourceLoc.MANAGER_ADAUGA, bundle, log);
		allProductsTable.postConstruct(contents);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).minSize(400, 200).applyTo(allProductsTable.getTable());
		
		addListeners();
		loadData();
		updateProdusFinit();
		
		this.category.setText(Product.MATERIE_PRIMA_CATEGORY);
		updateCategory(Product.MATERIE_PRIMA_CATEGORY);
		setChildrenEnabled(prodFinDenumire.getParent(), false);
		
		return contents;
	}
	
	private void createLeftContainer(final Composite parent)
	{
		final Composite leftContainer = new Composite(parent, SWT.NONE);
		leftContainer.setLayout(new GridLayout(3, false));
		
		category = new Combo(leftContainer, SWT.DROP_DOWN);
		category.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		category.setItems(Product.ALL_CATEGORIES.toArray(new String[] {}));
		UIUtils.setFont(category);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).span(4, 1).minSize(150, SWT.DEFAULT).applyTo(category);
		
		final Label searchModeLabel = new Label(leftContainer, SWT.NONE);
		searchModeLabel.setText(Messages.RetetarDialog_Mode);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(searchModeLabel);
		
		final Label barcodeNameLabel = new Label(leftContainer, SWT.NONE);
		barcodeNameLabel.setText(Messages.RetetarDialog_BarcodeName);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(barcodeNameLabel);
		
		final Label cantLabel = new Label(leftContainer, SWT.NONE);
		cantLabel.setText(Messages.RetetarDialog_Quantity);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(cantLabel);
		
		searchMode = new Combo(leftContainer, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);
		
		barcodeName = new Text(leftContainer, SWT.BORDER);
		barcodeName.setTextLimit(255);
		barcodeName.setFocus();
		UIUtils.setFont(barcodeName);
		GridDataFactory.fillDefaults().grab(true, false).minSize(150, SWT.DEFAULT).applyTo(barcodeName);
		
		cantitate = new Text(leftContainer, SWT.BORDER);
		cantitate.setText("0"); //$NON-NLS-1$
		cantitate.setTextLimit(11);
		UIUtils.setFont(cantitate);
		GridDataFactory.swtDefaults().hint(50, SWT.DEFAULT).applyTo(cantitate);
		
		final Composite newProductContainer = new Composite(leftContainer, SWT.NONE);
		newProductContainer.setLayout(new GridLayout(3, false));
		newProductContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(newProductContainer);
		
		final Label newNameLabel = new Label(newProductContainer, SWT.NONE);
		newNameLabel.setText(Messages.RetetarDialog_ManufacturedProd);
		newNameLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		newNameLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(newNameLabel);
		
		final Label uomLabel = new Label(newProductContainer, SWT.NONE);
		uomLabel.setText(Messages.RetetarDialog_UOM);
		uomLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		uomLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(uomLabel);
		
		final Label pvLabel = new Label(newProductContainer, SWT.NONE);
		pvLabel.setText(Messages.RetetarDialog_Price);
		pvLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		pvLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(pvLabel);
		
		prodFinDenumire = new Text(newProductContainer, SWT.BORDER);
		prodFinDenumire.setTextLimit(255);
		UIUtils.setFont(prodFinDenumire);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(prodFinDenumire);
		
		prodFinUM = new Text(newProductContainer, SWT.BORDER);
		UIUtils.setFont(prodFinUM);
		GridDataFactory.swtDefaults().applyTo(prodFinUM);
		
		prodFinPV = new Text(newProductContainer, SWT.BORDER);
		UIUtils.setFont(prodFinPV);
		GridDataFactory.swtDefaults().applyTo(prodFinPV);
		
		leftContainer.setTabList(new Control[]{barcodeName, cantitate});
	}
	
	private void createRightContainer(final Composite parent)
	{
		final Composite rightContainer = new Composite(parent, SWT.NONE);
		rightContainer.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().applyTo(rightContainer);
		
		categoryLabel = new Label(rightContainer, SWT.NONE);
		categoryLabel.setAlignment(SWT.CENTER);
		categoryLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldBannerFont(categoryLabel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(categoryLabel);
		
		matPrimaTable = new AllProductsNatTable(SourceLoc.RETETAR_MAT_PRIMA, bundle, log);
		matPrimaTable.afterChange(p -> updateAdaos());
		matPrimaTable.postConstruct(rightContainer);
		matPrimaTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).minSize(550, 150).applyTo(matPrimaTable.getTable());
		
		valAchizitie = new Label(rightContainer, SWT.NONE);
		valAchizitie.setAlignment(SWT.RIGHT);
		valAchizitie.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valAchizitie);
		
		adaosPercent = new Label(rightContainer, SWT.NONE);
		adaosPercent.setAlignment(SWT.RIGHT);
		adaosPercent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adaosPercent);
		
		adaosLei = new Label(rightContainer, SWT.NONE);
		adaosLei.setAlignment(SWT.RIGHT);
		adaosLei.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adaosLei);
		
		adauga = new Button(rightContainer, SWT.PUSH);
		adauga.setText(Messages.RetetarDialog_Add);
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adauga);
		UIUtils.setBoldBannerFont(adauga);
		
		sterge = new Button(rightContainer, SWT.PUSH);
		sterge.setText(Messages.Delete);
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sterge);
		UIUtils.setBoldBannerFont(sterge);
		
		rightContainer.setTabList(new Control[]{adauga});
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	private void addListeners()
	{
		category.addModifyListener(e -> updateCategory(category.getText()));
		
		searchMode.addModifyListener(this::filterModeChange);
		barcodeName.addModifyListener(e -> allProductsTable.filter(barcodeName.getText()));
		barcodeName.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
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
				
				Product product = null;
				
				if (e.keyCode == SWT.F2)
				{
					final List<Product> selection = allProductsTable.selection();
					if (!selection.isEmpty())
						product = selection.get(0);
					else if (!isEmpty(barcodeName.getText()))
						product = allProductsTable.getSourceData().stream()
								.filter(p -> globalIsMatch(p.getBarcode(), barcodeName.getText(), TextFilterMethod.EQUALS))
								.findFirst()
								.orElse(null);
					
					if (product != null)
						updateProdusSelectat(product);
					e.doit = false;
					allProductsTable.clearSelection();
				}
				if (e.keyCode == SWT.F4)
					adaugaMatPrima();
				
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					final Optional<Product> matchedProd = allProductsTable.getSourceData().stream()
							.filter(p -> globalIsMatch(p.getBarcode(), barcodeName.getText(), TextFilterMethod.EQUALS))
							.findFirst();
					if (!isEmpty(barcodeName.getText()) && matchedProd.isPresent())
						product = matchedProd.get();
					else 
						product = allProductsTable.selection().stream().findFirst().orElse(null);
					
					if (product != null)
						updateProdusSelectat(product);
					e.doit = false;
					allProductsTable.clearSelection();
				}
			}
		});
		
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				adaugaMatPrima();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				matPrimaTable.selection().forEach(matPrimaTable::remove);
				updateAdaos();
			}
		});
		
		final KeyAdapter keyListener = new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT, e);
					e.doit = false;
				}
				if (e.keyCode == SWT.F4)
					adaugaMatPrima();
			}
		};

		cantitate.addKeyListener(keyListener);
		
		final FocusAdapter selectAllListener = new FocusAdapter()
		{
			@Override public void focusGained(final FocusEvent e)
			{
				((Text) e.widget).selectAll();
			}
		};
		
		barcodeName.addFocusListener(selectAllListener);
		cantitate.addFocusListener(selectAllListener);
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, Messages.Save, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed()
	{
		final ImmutableList<ProductRecipeMapping> ingredients = matPrimaTable.getSourceData().stream()
				.flatMap(p -> p.getIngredients().stream())
				.collect(toImmutableList());
		final InvocationResult result = BusinessDelegate.updateReteta(produsFinit.getId(), ingredients);
		showResult(result);
		if (result.statusOk())
		{
			productConsumer.accept(result.extra(InvocationResult.PRODUCT_KEY));
			super.okPressed();
		}
	}
	
	private void loadData()
	{
		BusinessDelegate.allProducts(new AsyncLoadData<Product>()
		{
			@Override public void success(final ImmutableList<Product> data)
			{
				allProductsTable.loadData(data, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.RetetarDialog_LoadError, details);
			}
		}, sync, bundle, log);	
	}
	
	private void updateCategory(final String category)
	{
		clearAllFields();
		categoryLabel.setText(category);
		allProductsTable.filterCategory(category);
	}
	
	private void clearAllFields()
	{
		barcodeName.setText(EMPTY_STRING);
		updateAdaos();
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		allProductsTable.filterMode(filterMode);
		allProductsTable.filter(barcodeName.getText());
	}
	
	private void updateProdusFinit()
	{
		if (produsFinit != null)
		{
			produsFinit = BusinessDelegate.productById(produsFinit.getId(), bundle, log);
			prodFinDenumire.setText(safeString(produsFinit, Product::getName));
			prodFinUM.setText(safeString(produsFinit, Product::getUom));
			prodFinPV.setText(safeString(produsFinit, Product::getPricePerUom, PresentationUtils::displayBigDecimal));
			matPrimaTable.loadData(produsFinit.getIngredients().stream()
					.filter(prm -> prm.getProductFinit().equals(produsFinit))
					.map(prm -> 
					{
						final Product matPrima = prm.getProductMatPrima();
						final ProductRecipeMapping conexiuneLaProdFin = new ProductRecipeMapping(new ProductRecipeMappingId(produsFinit.getId(), matPrima.getId()),
								produsFinit, matPrima, prm.getQuantity());
						final HashSet<ProductRecipeMapping> conexiuniLaProdFin = new HashSet<>();
						conexiuniLaProdFin.add(conexiuneLaProdFin);
						matPrima.setIngredients(conexiuniLaProdFin);
						return matPrima;
					})
					.collect(toImmutableList()),
					ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
			updateAdaos();
		}
		else
		{
			matPrimaTable.loadData(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
			updateAdaos();
		}
	}
	
	private void updateProdusSelectat(final Product product)
	{
		this.produsSelectat = product;
		
		if (product != null)
		{
			barcodeName.setText(safeString(product, Product::getBarcode));
			
			cantitate.setFocus();
			cantitate.selectAll();
		}
	}
	
	private void updateAdaos()
	{
		final BigDecimal puaFaraTVA = matPrimaTable.getSourceData().stream()
				.filter(p -> p.getLastBuyingPriceNoTva() != null)
				.map(p -> p.getLastBuyingPriceNoTva().multiply(p.matPrimaQuantity()))
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal getLastBuyingPriceWithTva = matPrimaTable.getSourceData().stream()
				.filter(p -> p.getLastBuyingPriceNoTva() != null)
				.map(p -> 
				{
					final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
					final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);

					final BigDecimal puaFTVA = p.getLastBuyingPriceNoTva().multiply(p.matPrimaQuantity());
					return puaFTVA.multiply(tvaExtractDivisor);
				})
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		final BigDecimal tvaPercent = produsFinit.deptTvaPercentage().orElse(tvaPercentDb);
		final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
		
		final BigDecimal pvCuTVA = produsFinit.getPricePerUom();
		final BigDecimal pvFaraTVA = pvCuTVA.divide(tvaExtractDivisor, 2, RoundingMode.HALF_EVEN);
		
		valAchizitie.setText(MessageFormat.format(Messages.RetetarDialog_PValue, displayBigDecimal(puaFaraTVA)));
		valAchizitie.setToolTipText(Messages.RetetarDialog_PValTooltip);
		adaosPercent.setText(MessageFormat.format(Messages.RetetarDialog_MarginPerc, displayPercentage(calculateAdaosPercent(getLastBuyingPriceWithTva, pvCuTVA))));
		adaosPercent.setToolTipText(Messages.RetetarDialog_MarginPercTooltip);
		adaosLei.setText(MessageFormat.format(Messages.RetetarDialog_Margin, displayBigDecimal(pvFaraTVA.subtract(puaFaraTVA), 2, RoundingMode.HALF_EVEN)));
		adaosLei.setToolTipText(Messages.RetetarDialog_MarginTooltip);
	}
	
	private boolean isValid()
	{
		return produsSelectat != null;
	}
	
	private void adaugaMatPrima()
	{
		if (isValid())
		{
			final ProductRecipeMapping conexiuneLaProdFin = new ProductRecipeMapping(new ProductRecipeMappingId(produsFinit.getId(), produsSelectat.getId()),
					produsFinit, produsSelectat, parse(cantitate.getText()));
			final HashSet<ProductRecipeMapping> conexiuniLaProdFin = new HashSet<>();
			conexiuniLaProdFin.add(conexiuneLaProdFin);
			produsSelectat.setIngredients(conexiuniLaProdFin);
			
			matPrimaTable.add(produsSelectat);
			updateProdusSelectat(null);
			clearAllFields();
			barcodeName.setFocus();
		}
	}
}