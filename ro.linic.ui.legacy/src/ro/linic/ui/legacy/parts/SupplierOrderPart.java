package ro.linic.ui.legacy.parts;

import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;

import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.command.DiscardDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.datachange.command.SaveDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Product;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.GenericDataHolder;
import ro.linic.ui.base.services.di.DiscardChanges;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.security.services.AuthenticationSession;

public class SupplierOrderPart
{
	public static final String PART_ID = "linic_gest_client.part.comenzi_furnizori"; //$NON-NLS-1$
	
	private static final String TABLE_STATE_PREFIX = "supplier_order.products_nt"; //$NON-NLS-1$
	
	private static final int STOC_ID_BASE = 1000;
	private static final int RECOMMENDED_ORDER_ID_BASE = 2000;
	
	private static final Column barcodeColumn = new Column(0, Product.BARCODE_FIELD, "Cod", 70);
	private static final Column nameColumn = new Column(1, Product.NAME_FIELD, "Denumire", 300);
	private static final Column uomColumn = new Column(2, Product.UOM_FIELD, "UM", 50);
	private static final Column ULPColumn = new Column(3,Product.LAST_BUYING_PRICE_FIELD, "ULPfTVA", 70);
	private static final Column priceColumn = new Column(4, Product.PRICE_FIELD, "PU", 90);
	private static final Column furnizoriColumn = new Column(5, Product.FURNIZORI_FIELD, "Furnizori", 220);
	
	private static final ImmutableMap<Column, Gestiune> stocColumns;
	private static final ImmutableMap<Column, Gestiune> recommendedOrderColumns;
	
	static {
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> stocBuilder = ImmutableMap.builder();
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> recommendedOrderBuilder = ImmutableMap.builder();
		
		int i = 0;
		for (final Gestiune gest : BusinessDelegate.allGestiuni()) {
			stocBuilder.put(new Column(STOC_ID_BASE+i, "STC_"+gest.getImportName(), "STC "+gest.getImportName(), 70), gest);
			recommendedOrderBuilder.put(new Column(RECOMMENDED_ORDER_ID_BASE+i, "QTO_"+gest.getImportName(), "Comanda recomandata "+gest.getImportName(), 100), gest);
			i++;
		}
		
		stocColumns = stocBuilder.build();
		recommendedOrderColumns = recommendedOrderBuilder.build();
	}
	
	private ImmutableList<Column> columns;
	private Combo searchMode;
	private Text searchFilter;
	private Text furnizorFilter;
	private FullFeaturedNatTable<GenericValue> allProductsTable;
	private Button refreshButton;
	
	private int quickSearchFilterMode = TextMatcherEditor.CONTAINS;
	private SearchEngineTextMatcherEditor<GenericValue> quickSearchFilter;
	private TextMatcherEditor<GenericValue> furnizoriFilter;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	@Inject private AuthenticationSession authSession;
	@Inject private DataServices dataServices;
	
	private ImmutableList<Column> buildColumns() {
		final Builder<Column> builder = ImmutableList.<Column>builder();
		builder.add(barcodeColumn)
		.add(nameColumn)
		.add(uomColumn)
		.add(ULPColumn)
		.add(priceColumn)
		.add(furnizoriColumn);
		for (int i = 0; i < stocColumns.size(); i++) {
			builder.add(stocColumns.keySet().asList().get(i))
			.add(recommendedOrderColumns.keySet().asList().get(i));
		}
		return builder.build();
	}
	
	@PostConstruct
	public void createComposite(final Composite parent, final ESelectionService selectionService)
	{
		columns = buildColumns();
		parent.setLayout(new GridLayout());
		createTopBar(parent);
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		searchMode = new Combo(container, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);
		
		searchFilter = new Text(container, SWT.BORDER);
		searchFilter.setMessage(Messages.BarcodeName);
		UIUtils.setFont(searchFilter);
		GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT).applyTo(searchFilter);
		
		furnizorFilter = new Text(container, SWT.BORDER);
		furnizorFilter.setMessage(Messages.SupplierOrderPart_Supplier);
		UIUtils.setFont(furnizorFilter);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(furnizorFilter);
		
		allProductsTable = TableBuilder.with(GenericValue.class, columns, dataServices.holder("ProductsToOrder").getData())
				.addConfiguration(new ProductStyleConfiguration())
				.connectDirtyProperty(part)
				.provideSelection(selectionService)
				.saveToDbHandler(data -> true) // TODO implement this
				.build(parent);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(allProductsTable.natTable());
		loadState(TABLE_STATE_PREFIX, allProductsTable.natTable(), part);
		createTableFilters();
		
		createBottomBar(parent);
		
		addListeners();
		loadData(false);
	}
	
	private void createTableFilters() {
		final EventList<MatcherEditor<GenericValue>> stringMatcherEditors = new BasicEventList<MatcherEditor<GenericValue>>();
		
		quickSearchFilter = new SearchEngineTextMatcherEditor<GenericValue>(new TextFilterator<GenericValue>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final GenericValue element)
			{
				baseList.add(element.getString(Product.BARCODE_FIELD));
				baseList.add(element.getString(Product.NAME_FIELD));
			}
		});
		quickSearchFilter.setMode(quickSearchFilterMode);
		
		furnizoriFilter = new TextMatcherEditor<GenericValue>(new TextFilterator<GenericValue>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final GenericValue element)
			{
				baseList.add(element.getString(Product.FURNIZORI_FIELD));
			}
		});
		furnizoriFilter.setMode(TextMatcherEditor.CONTAINS);
		
		stringMatcherEditors.add(quickSearchFilter);
		stringMatcherEditors.add(furnizoriFilter);
		
		final CompositeMatcherEditor<GenericValue> matcherEditor = new CompositeMatcherEditor<>(stringMatcherEditors);
        allProductsTable.filterList().setMatcherEditor(matcherEditor);
	}

	private void createBottomBar(final Composite parent)
	{
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(3, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText(Messages.Refresh);
		UIUtils.setBannerFont(refreshButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(refreshButton);
	}
	
	@PersistState
	public void persistVisualState() {
		saveState(TABLE_STATE_PREFIX, allProductsTable.natTable(), part);
	}
	
	@Persist
	public void onSave(final MPart part) {
		allProductsTable.natTable().doCommand(new SaveDataChangesCommand());
	}
	
	@DiscardChanges
	public void onDiscard(final MPart part) {
		allProductsTable.natTable().doCommand(new DiscardDataChangesCommand());
	}
	
	private void addListeners()
	{
		searchMode.addModifyListener(this::filterModeChange);
		searchFilter.addModifyListener(e -> filter(searchFilter.getText()));
		furnizorFilter.addModifyListener(e -> filterFurnizori(furnizorFilter.getText()));
		
		refreshButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData(true);
			}
		});
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		filterMode(filterMode);
		filter(searchFilter.getText());
	}
	
	private void filterMode(final int mode)
	{
		quickSearchFilterMode = mode;
		quickSearchFilter.setMode(quickSearchFilterMode);
		quickSearchFilter.setFilterText(new String[] {});
	}
	
	private void filter(final String searchText)
	{
        if (quickSearchFilterMode == TextMatcherEditor.CONTAINS)
        	quickSearchFilter.refilter(searchText);
        else
        	quickSearchFilter.setFilterText(new String[] {searchText});
	}
	
	private void filterFurnizori(final String furnizori)
	{
        furnizoriFilter.setFilterText(furnizori.split(SPACE));
	}
	
	private void loadData(final boolean showConfirmation)
	{
		final GenericDataHolder productsHolder = dataServices.holder("ProductsToOrder");
		productsHolder.clear();
		
		RestCaller.get("/rest/s1/moqui-linic-legacy/products/suppliers")
				.internal(authSession.authentication())
				.addUrlParam("organizationPartyId", ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName())
				.async(t -> UIUtils.showException(t, sync))
				.thenAccept(productSuppliers -> productsHolder.addOrUpdate(productSuppliers, "productId", Product.ID_FIELD,
						Map.of("productId", Product.ID_FIELD, "supplierName", Product.FURNIZORI_FIELD)));
		
		BusinessDelegate.allProductsForOrdering(new AsyncLoadData<Product>()
		{
			@Override public void success(final ImmutableList<Product> data)
			{
				productsHolder.addOrUpdate(data.stream()
						.map(p -> {
							final GenericValue gv = GenericValue.of(Product.class.getName(), Map.of(Product.ID_FIELD, p.getId(),
									Product.BARCODE_FIELD, p.getBarcode(), Product.NAME_FIELD, p.getName(),
									Product.UOM_FIELD, p.getUom(), Product.LAST_BUYING_PRICE_FIELD, p.getLastBuyingPriceNoTva(),
									Product.PRICE_FIELD, p.getPricePerUom()));
							for (final Gestiune gest : stocColumns.values()) {
								gv.put("STC_"+gest.getImportName(), p.stoc(gest));
								gv.put("QTO_"+gest.getImportName(), p.recommendedOrder(gest));
							}
							return gv;
						})
						.toList(),
						Product.ID_FIELD);

				// need this fix because of the speed fix in GenericDataHolderImpl:
				// source.silenceListeners(true); // speed fix
				allProductsTable.natTable().refresh();

				if (showConfirmation)
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.Success,
							Messages.SupplierOrderPart_SuccessMessage);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SupplierOrderPart_ErrorLoading,
						details);
			}
		}, sync, bundle, log);
	}

	private class ProductStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			final Style yellowBgStyle = new Style();
			yellowBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			yellowBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(barcodeColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uomColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			final Style blueBgStyle = new Style();
			blueBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			blueBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			blueBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			final Style magentaBgStyle = new Style();
			magentaBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			magentaBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
			magentaBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			final Style greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(ULPColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, blueBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					greenBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			recommendedOrderColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					magentaBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			
			// Display converters
			final DefaultBigDecimalDisplayConverter defaultBigDecimalConv = new DefaultBigDecimalDisplayConverter();
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);
			final DefaultBigDecimalDisplayConverter quantityConverter = new DefaultBigDecimalDisplayConverter();
			quantityConverter.setMinimumFractionDigits(4);
			
			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			recommendedOrderColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(ULPColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			
			// add a special style to highlight the modified cells
			final Style style = new Style();
			style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);
			configRegistry.registerConfigAttribute(
					CellConfigAttributes.CELL_STYLE,
					style,
					DisplayMode.NORMAL,
					DataChangeLayer.DIRTY);
		}
	}
}
