package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.matchers.TextMatcherEditor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.LobImage;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.AdaugaProductDialog;
import ro.linic.ui.legacy.dialogs.CasaDeptDialog;
import ro.linic.ui.legacy.dialogs.CategoriiUIDialog;
import ro.linic.ui.legacy.dialogs.PrintBarcodeDialog;
import ro.linic.ui.legacy.dialogs.PrintOfertaDiscountDialog;
import ro.linic.ui.legacy.dialogs.RaionDialog;
import ro.linic.ui.legacy.dialogs.ReducereDialog;
import ro.linic.ui.legacy.dialogs.RetetarDialog;
import ro.linic.ui.legacy.expressions.BetaTester;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.legacy.tables.AllProductsNatTable.SourceLoc;
import ro.linic.ui.security.services.AuthenticationSession;

public class CatalogProdusePart
{
	public static final String PART_ID = "linic_gest_client.part.produse"; //$NON-NLS-1$
	
	private static final String PRODUCTS_TABLE_STATE_PREFIX = "produse.all_products_nt"; //$NON-NLS-1$
	
	private Button adauga;
	private Button salvare;
	private Button refresh;
	private Button retetar;
	private Button categoriiUI;
	private Button departamente;
	private Button raioane;
	private Button reduceri;
	private Button etichete;
	private Button sterge;
	private Button printareOferta;
	private Button printareCatalog;
	private Button editInMoqui;
	
	private Combo category;
	private Combo uiCategory;
	private Combo searchMode;
	private Text searchFilter;
	private AllProductsNatTable table;
	
	@Inject private AuthenticationSession auth;
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	public static void openPart(final EPartService partService)
	{
		partService.showPart(CatalogProdusePart.PART_ID, PartState.ACTIVATE);
	}
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		final GridLayout parentLayout = new GridLayout(2, false);
		parentLayout.horizontalSpacing = 0;
		parentLayout.verticalSpacing = 0;
		parent.setLayout(parentLayout);
		createFirstRow(parent);
		createSecondRow(parent);
		addListeners();
		loadData(false);
	}

	private void createFirstRow(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		
		adauga = new Button(container, SWT.PUSH);
		adauga.setText(Messages.Add);
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(adauga);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adauga);
		
		salvare = new Button(container, SWT.PUSH);
		salvare.setText(Messages.Save);
		salvare.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		salvare.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(salvare);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(salvare);
		
		refresh = new Button(container, SWT.PUSH);
		refresh.setText(Messages.Refresh);
		refresh.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		refresh.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(refresh);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(refresh);
		
		categoriiUI = new Button(container, SWT.PUSH);
		categoriiUI.setText(Messages.CatalogProdusePart_VisualCat);
		categoriiUI.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		categoriiUI.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(categoriiUI);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(categoriiUI);
		
		departamente = new Button(container, SWT.PUSH);
		departamente.setText(Messages.CatalogProdusePart_ECRDept);
		departamente.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		departamente.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(departamente);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(departamente);
		
		raioane = new Button(container, SWT.PUSH);
		raioane.setText(Messages.CatalogProdusePart_Sections);
		raioane.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		raioane.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raioane);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(raioane);
		
		reduceri = new Button(container, SWT.PUSH);
		reduceri.setText(Messages.CatalogProdusePart_Allowances);
		reduceri.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		reduceri.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(reduceri);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(reduceri);
		
		retetar = new Button(container, SWT.PUSH);
		retetar.setText(Messages.CatalogProdusePart_Recipes);
		retetar.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		retetar.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(retetar);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(retetar);
		
		etichete = new Button(container, SWT.PUSH);
		etichete.setText(Messages.PrintLabels);
		etichete.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		etichete.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(etichete);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(etichete);
		
		sterge = new Button(container, SWT.PUSH);
		sterge.setText(Messages.Delete);
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(sterge);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sterge);
		
		printareOferta = new Button(container, SWT.PUSH);
		printareOferta.setText(Messages.CatalogProdusePart_Offer);
		printareOferta.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareOferta.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(printareOferta);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BOTTOM).grab(true, true).applyTo(printareOferta);
		
		printareCatalog = new Button(container, SWT.PUSH);
		printareCatalog.setText(Messages.CatalogProdusePart_PrintCatalogue);
		printareCatalog.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		printareCatalog.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(printareCatalog);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BOTTOM).grab(true, false).applyTo(printareCatalog);
		
		editInMoqui = new Button(container, SWT.PUSH);
		editInMoqui.setText(Messages.CatalogProdusePart_EditInMoqui);
		editInMoqui.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN));
		editInMoqui.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(editInMoqui);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BOTTOM).grab(true, false).exclude(!new BetaTester().evaluate(auth)).applyTo(editInMoqui);
	}
	
	private void createSecondRow(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		category = new Combo(container, SWT.DROP_DOWN);
		category.setItems(Product.ALL_CATEGORIES.toArray(new String[] {}));
		UIUtils.setFont(category);
		GridDataFactory.swtDefaults().applyTo(category);
		
		uiCategory = new Combo(container, SWT.DROP_DOWN);
		UIUtils.setFont(uiCategory);
		GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).applyTo(uiCategory);
		
		searchMode = new Combo(container, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);
		
		searchFilter = new Text(container, SWT.BORDER);
		searchFilter.setMessage(Messages.BarcodeName);
		UIUtils.setFont(searchFilter);
		GridDataFactory.swtDefaults().hint(500, SWT.DEFAULT).applyTo(searchFilter);
		
		table = new AllProductsNatTable(SourceLoc.CATALOG_PRODUSE, bundle, log);
		table.afterChange(op -> part.setDirty(true));
		table.postConstruct(container);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(4, 1).applyTo(table.getTable());
		loadState(PRODUCTS_TABLE_STATE_PREFIX, table.getTable(), part);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(PRODUCTS_TABLE_STATE_PREFIX, table.getTable(), part);
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			final ImmutableList<InvocationResult> results = table.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<Product>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.map(p -> 
					{
						final byte[] img = Icons.imageFromCache(p.getImageUUID());
						if (img != null)
							BusinessDelegate.mergeImageLobs(ImmutableList.of(LobImage.from(null, img)));
						return p;
					})
					.map(BusinessDelegate::mergeProduct)
					.collect(toImmutableList());

			showResult(InvocationResult.flatMap(results));
			
			results.stream()
			.filter(InvocationResult::statusOk)
			.map(result -> (Product) result.extra(InvocationResult.PRODUCT_KEY))
			.map(mergedProduct -> table.replace(mergedProduct, mergedProduct))
			.findAny()
			.ifPresent(t -> t.getTable().refresh());
			
			part.setDirty(false);
		}
	}
	
	private void addListeners()
	{
		searchMode.addModifyListener(this::filterModeChange);
		searchFilter.addModifyListener(e -> table.filter(searchFilter.getText()));
		category.addModifyListener(e -> table.filterCategory(category.getText()));
		uiCategory.addModifyListener(e -> table.filterUiCategory(uiCategory.getText()));
		
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				new AdaugaProductDialog(adauga.getShell(), table::add).open();
			}
		});
		
		salvare.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		refresh.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				loadData(true);
			}
		});
		
		categoriiUI.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (new CategoriiUIDialog(categoriiUI.getShell(), sync, bundle, log).open() == Window.OK)
					loadData(true);
			}
		});
		
		departamente.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (new CasaDeptDialog(departamente.getShell()).open() == Window.OK)
					loadData(true);
			}
		});
		
		raioane.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (new RaionDialog(raioane.getShell()).open() == Window.OK)
					loadData(true);
			}
		});
		
		reduceri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (new ReducereDialog(reduceri.getShell()).open() == Window.OK)
					loadData(true);
			}
		});
		
		retetar.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				final Optional<Product> selectedProd = table.selection().stream().findFirst();
				if (selectedProd.isPresent())
					new RetetarDialog(retetar.getShell(), sync, bundle, log, selectedProd.get(), p -> table.replace(p, p)).open();
			}
		});

		etichete.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ImmutableList<BarcodePrintable> printables = BarcodePrintable.fromProducts(table.selection());
				
				if (!printables.isEmpty())
					new PrintBarcodeDialog(etichete.getShell(), printables, log, bundle).open();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (!MessageDialog.openQuestion(sterge.getShell(), Messages.CatalogProdusePart_DeleteProducts, Messages.CatalogProdusePart_DeleteProductsMessage))
					return;
				
				askSave();
				table.selection().forEach(pToDelete ->
				{
					final InvocationResult result = BusinessDelegate.deleteProduct(pToDelete.getId());
					showResult(result);
					if (result.statusOk())
						table.remove(pToDelete);
				});
			}
		});
		
		printareOferta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final List<Product> selProducts = table.selection();
				final Stream<Product> productsStream = 
						selProducts.isEmpty() ? table.getFilteredSortedData().stream() : selProducts.stream().sorted(Comparator.comparing(Product::getName));
				final ImmutableList<Product> products = productsStream
						.filter(Product::isActiv)
						.collect(toImmutableList());

				if (!products.isEmpty())
					new PrintOfertaDiscountDialog(Display.getCurrent().getActiveShell(), bundle, log, products).open();
			}
		});
		
		printareCatalog.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (!table.getFilteredData().isEmpty())
					try
					{
						JasperReportManager.instance(bundle, log).printCatalogProduse(bundle, ClientSession.instance().getLoggedUser().getSelectedGestiune(),
								ImmutableList.copyOf(table.getFilteredSortedData()));
					}
					catch (IOException | JRException ex)
					{
						log.error(ex);
						showException(ex);
					}
			}
		});
		
		editInMoqui.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final String baseUrl = ro.linic.ui.base.services.util.UIUtils.moquiBaseUrl();
				table.selection().forEach(p -> 
				ro.linic.ui.base.services.util.UIUtils.openUrl(baseUrl+"/qapps/PopcAdmin/Catalog/Product/EditPrices?productId="+p.getId()));
			}
		});
	}
	
	private void loadData(final boolean showConfirmation)
	{
		BusinessDelegate.allProducts_InclInactive(new AsyncLoadData<Product>()
		{
			@Override
			public void success(final ImmutableList<Product> data)
			{
				final ImmutableList<ProductUiCategory> uiCategories = BusinessDelegate.uiCategories_Sync(bundle, log);
				uiCategory.setItems(uiCategories.stream()
						.map(ProductUiCategory::displayName)
						.toArray(String[]::new));
				
				table.loadData(data, uiCategories, BusinessDelegate.casaDepts_Sync(), BusinessDelegate.raioane_Sync(),
						BusinessDelegate.reduceri_Sync());
				part.setDirty(false);

				if (showConfirmation)
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.Success,
							Messages.CatalogProdusePart_SuccessMessage);
			}

			@Override
			public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.CatalogProdusePart_ErrorLoading, details);
			}
		}, sync, bundle, log);
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.Save, Messages.SaveMessage))
			onSave();
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		table.filterMode(filterMode);
		table.filter(searchFilter.getText());
	}
}
