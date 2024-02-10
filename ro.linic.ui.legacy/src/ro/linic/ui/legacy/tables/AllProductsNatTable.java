package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toHashSet;
import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBooleanDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultIntegerDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.CheckBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.config.DefaultFreezeGridBindings;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.IOverlayPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.CheckBoxPainter;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.selection.command.ClearAllSelectionsCommand;
import org.eclipse.nebula.widgets.nattable.selection.command.MoveSelectionCommand;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectCellCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.summaryrow.DefaultSummaryRowConfiguration;
import org.eclipse.nebula.widgets.nattable.summaryrow.FixedSummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.embeddable.ProductReducereMappingId;
import ro.colibri.entities.comercial.CasaDepartment;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.entities.comercial.Raion;
import ro.colibri.entities.comercial.Reducere;
import ro.colibri.entities.comercial.mappings.ProductReducereMapping;
import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;
import ro.linic.ui.legacy.tables.components.UuidImageCellEditor;
import ro.linic.ui.legacy.tables.components.UuidImagePainter;

public class AllProductsNatTable
{
	public static final String CONTAINS_MODE = "Contine";
	public static final String STARTS_WITH_MODE = "Incepe cu";
	public static final ImmutableList<String> ALL_SEARCH_MODES = ImmutableList.of(CONTAINS_MODE, STARTS_WITH_MODE);
	
	private static final int RETETA_QUANTITY_ID = 21;
	private static final int REDUCERI_ID = 26;
	private static final int STOC_ID_BASE = 1000;
	private static final int AVERAGE_DAY_SALE_ID_BASE = 2000;
	private static final int FUTURE_STOC_ID_BASE = 3000;
	private static final int MAX_SALE_ID_BASE = 4000;
	private static final int RECOMMENDED_ORDER_ID_BASE = 5000;
	
	private static final Column barcodeColumn = new Column(0, Product.BARCODE_FIELD, "Cod", 70);
	private static final Column nameColumn = new Column(1, Product.NAME_FIELD, "Denumire", 300);
	private static final Column uomColumn = new Column(2, Product.UOM_FIELD, "UM", 50);
	private static final Column ULPColumn = new Column(3,Product.LAST_BUYING_PRICE_FIELD, "ULPfTVA", 70);
	private static final Column priceColumn = new Column(4, Product.PRICE_FIELD, "PU", 70);
	
	private static final Column activColumn = new Column(7, Product.ACTIV_FIELD, "Activ", 70);
	private static final Column categoryColumn = new Column(8, Product.CATEGORY_FIELD, "Categorie", 100);
	private static final Column stocMinimColumn = new Column(9, Product.MIN_STOC_FIELD, "Stoc min", 70);
	private static final Column idxColumn = new Column(10, Product.ID_FIELD, "Idx", 70);
	
	private static final Column furnizoriColumn = new Column(11, Product.FURNIZORI_FIELD, "Furnizori", 150);
	private static final Column hideWhenOrderingColumn = new Column(20, Product.HIDE_WHEN_ORDERING_FIELD, "NeComandabil", 70);
	private static final Column retetaQuantityColumn = new Column(RETETA_QUANTITY_ID, EMPTY_STRING, "Cant", 70);
	private static final Column uiCategoryColumn = new Column(22, Product.UI_CATEGORY_FIELD, "Cat Vizuala", 120);
	private static final Column imageColumn = new Column(23, Product.IMAGE_UUID_FIELD, "Poza", 120);
	private static final Column casaDeptColumn = new Column(24, Product.CASA_DEPT_FIELD, "Departament Casa", 120);
	private static final Column raionColumn = new Column(25, Product.RAION_FIELD, "Raion", 120);
	private static final Column reduceriColumn = new Column(REDUCERI_ID, EMPTY_STRING, "Reduceri", 120);
	
	private static final ImmutableMap<Column, Gestiune> stocColumns;
	private static final ImmutableMap<Column, Gestiune> averageDaySaleColumns;
	private static final ImmutableMap<Column, Gestiune> futureStocColumns;
	private static final ImmutableMap<Column, Gestiune> maxSaleColumns;
	private static final ImmutableMap<Column, Gestiune> recommendedOrderColumns;
	
	static
	{
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> stocBuilder = ImmutableMap.builder();
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> averageDaySaleBuilder = ImmutableMap.builder();
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> futureStocBuilder = ImmutableMap.builder();
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> maxSaleBuilder = ImmutableMap.builder();
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> recommendedOrderBuilder = ImmutableMap.builder();
		
		int i = 0;
		for (final Gestiune gest : BusinessDelegate.allGestiuni())
		{
			stocBuilder.put(new Column(STOC_ID_BASE+i, EMPTY_STRING, "STC "+gest.getImportName(), 70), gest);
			averageDaySaleBuilder.put(new Column(AVERAGE_DAY_SALE_ID_BASE+i, EMPTY_STRING, "Media Vanzari "+gest.getImportName(), 100), gest);
			futureStocBuilder.put(new Column(FUTURE_STOC_ID_BASE+i, EMPTY_STRING, "Stoc "+gest.getImportName()+" dupa "+Product.FUTURE_STOC_DAYS+" zile", 100), gest);
			maxSaleBuilder.put(new Column(MAX_SALE_ID_BASE+i, EMPTY_STRING, "Vanzare maxima "+gest.getImportName(), 100), gest);
			recommendedOrderBuilder.put(new Column(RECOMMENDED_ORDER_ID_BASE+i, EMPTY_STRING, "Comanda recomandata "+gest.getImportName(), 100), gest);
			i++;
		}
		
		stocColumns = stocBuilder.build();
		averageDaySaleColumns = averageDaySaleBuilder.build();
		futureStocColumns = futureStocBuilder.build();
		maxSaleColumns = maxSaleBuilder.build();
		recommendedOrderColumns = recommendedOrderBuilder.build();
	}
	
	public enum SourceLoc
	{
		VANZARI, MANAGER_ADAUGA, CATALOG_PRODUSE, COMENZI_FURNIZORI, RETETAR_MAT_PRIMA;
	}

	private ImmutableList<Column> columns;
	private EventList<Product> sourceData;
	private FilterList<Product> filteredData;
	private SortedList<Product> filteredSortedData;
	private EventList<ProductUiCategory> uiCategories = GlazedLists.eventListOf();
	private EventList<CasaDepartment> depts = GlazedLists.eventListOf();
	private EventList<Raion> raioane = GlazedLists.eventListOf();
	private EventList<Reducere> reduceri = GlazedLists.eventListOf();
	
	private NatTable table;
	private SearchEngineTextMatcherEditor<Product> quickSearchFilter;
	private int quickSearchFilterMode = TextMatcherEditor.CONTAINS;
	private TextMatcherEditor<Product> categoryFilter;
	private TextMatcherEditor<Product> uiCategoryFilter;
	private TextMatcherEditor<Product> furnizoriFilter;
	
	private RowSelectionProvider<Product> selectionProvider;
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private IRowDataProvider<Product> bodyDataProvider;
	private DataLayer bodyDataLayer;
	private GridLayer gridLayer;
	
	private SourceLoc source;
	private Consumer<Product> afterChange;
	
	private Bundle bundle;
	private Logger log;

	public AllProductsNatTable(final SourceLoc source, final Bundle bundle, final Logger log)
	{
		this.bundle = bundle;
		this.log = log;
		
		this.source = source;
		final Builder<Column> builder = ImmutableList.<Column>builder();
		
		switch (source)
		{
		case VANZARI:
			builder.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(priceColumn)
			.addAll(stocColumns.keySet());
			
			break;
		case MANAGER_ADAUGA:
			builder.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(ULPColumn)
			.add(priceColumn)
			.addAll(stocColumns.keySet());
			break;
		case CATALOG_PRODUSE:
			builder.add(activColumn)
			.add(hideWhenOrderingColumn)
			.add(categoryColumn)
			.add(casaDeptColumn)
			.add(raionColumn)
			.add(reduceriColumn)
			.add(uiCategoryColumn)
			.add(imageColumn)
			.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(ULPColumn)
			.add(priceColumn)
			.addAll(stocColumns.keySet())
			.add(stocMinimColumn)
			.add(idxColumn);
			break;
		case COMENZI_FURNIZORI:
			builder.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(ULPColumn)
			.add(priceColumn)
			.add(furnizoriColumn)
			.add(stocMinimColumn);
			for (int i = 0; i < stocColumns.size(); i++)
			{
				builder.add(stocColumns.keySet().asList().get(i))
				.add(averageDaySaleColumns.keySet().asList().get(i))
				.add(futureStocColumns.keySet().asList().get(i))
				.add(maxSaleColumns.keySet().asList().get(i))
				.add(recommendedOrderColumns.keySet().asList().get(i));
			}
			break;
		case RETETAR_MAT_PRIMA:
			builder.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(retetaQuantityColumn)
			.add(ULPColumn)
			.add(priceColumn);
			break;

		default:
			throw new IllegalArgumentException("Source location "+source+" not implemented!");
		}
		
		columns = builder.build();
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final CommonGridLayerStack gridLayer = new CommonGridLayerStack(configRegistry);
		
		final AbstractLayer underlyingLayer;
		
		if (source.equals(SourceLoc.CATALOG_PRODUSE))
		{
			// create a standalone summary row
	        // for a grid this is the FixedGridSummaryRowLayer
	        final FixedSummaryRowLayer summaryRowLayer = new FixedSummaryRowLayer(bodyDataLayer, gridLayer, configRegistry, false);
	        summaryRowLayer.addConfiguration(new SummaryRowGridConfiguration(bodyDataProvider));
	        summaryRowLayer.setSummaryRowLabel("Total");

	        // create a composition that has the grid on top and the summary row on
	        // the bottom
	        final CompositeLayer composite = new CompositeLayer(1, 2);
	        composite.setChildLayer("GRID", gridLayer, 0, 0);
	        composite.setChildLayer("SUMMARY", summaryRowLayer, 0, 1);
			underlyingLayer = composite;
		}
		else
			underlyingLayer = gridLayer;
		
		table = new NatTable(parent, underlyingLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new DefaultFreezeGridBindings());
		table.addConfiguration(new FreezeMenuConfiguration(table));
		table.addConfiguration(new FilterRowConfiguration());
		new CustomContentTooltip(table);

		// Custom selection configuration
		final boolean multipleSelection = SourceLoc.CATALOG_PRODUSE.equals(source) || SourceLoc.COMENZI_FURNIZORI.equals(source);
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Product>()
				{
					@Override public Serializable getRowId(final Product rowObject)
					{
						return rowObject.hashCode();
					}
				}, multipleSelection)); //true = multiple; false = single selection
		
		// configure a painter that renders a line on top of the summary row
        // this is necessary because the CompositeLayerPainter does not render
        // lines on the top of a region
		if (source.equals(SourceLoc.CATALOG_PRODUSE))
		{
			table.addOverlayPainter(new IOverlayPainter()
			{
				@Override public void paintOverlay(final GC gc, final ILayer layer)
				{
					// render a line on top of the summary row
					final Color beforeColor = gc.getForeground();
					gc.setForeground(GUIHelper.COLOR_GRAY);
					final int gridBorderY = gridLayer.getHeight() - 1;
					gc.drawLine(0, gridBorderY, layer.getWidth() - 1, gridBorderY);
					gc.setForeground(beforeColor);
				}
			});
		}
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		// define a TextMatcherEditor and set it to the FilterList
		final EventList<MatcherEditor<Product>> stringMatcherEditors = new BasicEventList<MatcherEditor<Product>>();
				
		quickSearchFilter = new SearchEngineTextMatcherEditor<Product>(new TextFilterator<Product>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Product element)
			{
				baseList.add(element.getBarcode());
				baseList.add(element.getName());
			}
		});
		quickSearchFilter.setMode(quickSearchFilterMode);
		
		categoryFilter = new TextMatcherEditor<Product>(new TextFilterator<Product>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Product element)
			{
				baseList.add(element.getCategorie());
			}
		});
		categoryFilter.setMode(TextMatcherEditor.EXACT);
		
		uiCategoryFilter = new TextMatcherEditor<Product>(new TextFilterator<Product>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Product element)
			{
				baseList.add(safeString(element.getUiCategory(), ProductUiCategory::displayName));
			}
		});
		uiCategoryFilter.setMode(TextMatcherEditor.EXACT);
		
		furnizoriFilter = new TextMatcherEditor<Product>(new TextFilterator<Product>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Product element)
			{
				baseList.add(element.getFurnizori());
			}
		});
		furnizoriFilter.setMode(TextMatcherEditor.CONTAINS);
		
		stringMatcherEditors.add(quickSearchFilter);
		stringMatcherEditors.add(categoryFilter);
		stringMatcherEditors.add(uiCategoryFilter);
		stringMatcherEditors.add(furnizoriFilter);
		
		final CompositeMatcherEditor<Product> matcherEditor = new CompositeMatcherEditor<>(stringMatcherEditors);
        filteredData.setMatcherEditor(matcherEditor);
	}
	
	public AllProductsNatTable loadData(final ImmutableList<Product> data, final ImmutableList<ProductUiCategory> uiCategories,
			final ImmutableList<CasaDepartment> depts, final ImmutableList<Raion> raioane, final ImmutableList<Reducere> reduceri)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			this.uiCategories.clear();
			this.uiCategories.addAll(uiCategories);
			this.depts.clear();
			this.depts.addAll(depts);
			this.raioane.clear();
			this.raioane.addAll(raioane);
			this.reduceri.clear();
			this.reduceri.addAll(reduceri);
			this.sourceData.clear();
			this.sourceData.addAll(data);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public AllProductsNatTable replace(final Product oldProduct, final Product newProduct)
	{
		final boolean restoreSelection = selection().stream()
				.anyMatch(p -> p.equals(oldProduct));
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.set(sourceData.indexOf(oldProduct), newProduct);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		if (restoreSelection)
			selectionProvider.setSelection(new StructuredSelection(new Product[] {newProduct}));
		return this;
	}
	
	public AllProductsNatTable add(final Product newProduct)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.add(newProduct);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public AllProductsNatTable remove(final Product product)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(product);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<Product> getSourceData()
	{
		return sourceData;
	}
	
	public FilterList<Product> getFilteredData()
	{
		return filteredData;
	}
	
	public SortedList<Product> getFilteredSortedData()
	{
		return filteredSortedData;
	}
	
	public AllProductsNatTable filterMode(final int mode)
	{
		quickSearchFilterMode = mode;
		quickSearchFilter.setMode(quickSearchFilterMode);
		quickSearchFilter.setFilterText(new String[] {});
        return this;
	}
	
	public AllProductsNatTable filter(final String searchText)
	{
        if (quickSearchFilterMode == TextMatcherEditor.CONTAINS)
        	quickSearchFilter.refilter(searchText);
        else
        	quickSearchFilter.setFilterText(new String[] {searchText});
        return this;
	}
	
	public AllProductsNatTable filterCategory(final String category)
	{
        categoryFilter.setFilterText(new String[] {category});
        return this;
	}
	
	public AllProductsNatTable filterUiCategory(final String uiCategory)
	{
        uiCategoryFilter.setFilterText(new String[] {uiCategory});
        return this;
	}
	
	public AllProductsNatTable filterFurnizori(final String furnizori)
	{
        furnizoriFilter.setFilterText(furnizori.split(SPACE));
        return this;
	}
	
	public void addSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.addSelectionChangedListener(listener);
	}
	
	public void removeSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.removeSelectionChangedListener(listener);
	}
	
	public void moveSelection(final MoveDirectionEnum direction)
	{
		final List<Product> sel = selection();
		selectionProvider.setSelection(new StructuredSelection(sel));
		
		if (sel.isEmpty())
			viewportLayer.doCommand(new SelectCellCommand(viewportLayer, 0, 0, false, false));
		else
			viewportLayer.doCommand(new MoveSelectionCommand(direction, false, false));
	}
	
	public void clearSelection()
	{
		selectionLayer.doCommand(new ClearAllSelectionsCommand());
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<Product> selection()
	{
		return ((RowSelectionModel<Product>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	public AllProductsNatTable afterChange(final Consumer<Product> afterChange)
	{
		this.afterChange = afterChange;
		return this;
	}
	
	private class CommonGridLayerStack extends AbstractLayerTransform
	{
		public CommonGridLayerStack(final ConfigRegistry configRegistry)
		{
			final IColumnPropertyAccessor<Product> columnAccessor = new ColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
			
			sourceData = GlazedLists.eventListOf();
	        final TransformedList<Product, Product> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

	        final FilterList<Product> filteredHeaderData = new FilterList<>(rowObjectsGlazedList);
	        filteredData = new FilterList<>(filteredHeaderData);
	        filteredSortedData = new SortedList<>(filteredData, null);

			// create the body layer stack
			bodyDataProvider = new ListDataProvider<>(filteredSortedData, columnAccessor);
			bodyDataLayer = new DataLayer(bodyDataProvider);
			bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
			for (int i = 0; i < columns.size(); i++)
				bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
			// add a DataChangeLayer that tracks data changes but directly updates
			// the underlying data model
			dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
			final GlazedListsEventLayer<Product> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, filteredSortedData);
			selectionLayer = new SelectionLayer(glazedListsEventLayer);
			viewportLayer = new ViewportLayer(selectionLayer);
			final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
			final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer, selectionLayer);
			
			selectionProvider = new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
			
			// create the column header layer stack
			final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
			final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
			final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, compositeFreezeLayer, selectionLayer);
			columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
			
			// add the SortHeaderLayer to the column header layer stack
	        // as we use GlazedLists, we use the GlazedListsSortModel which
	        // delegates the sorting to the SortedList
			final SortHeaderLayer<Product> sortHeaderLayer = new SortHeaderLayer<Product>(columnHeaderLayer,
					new GlazedListsSortModel<Product>(filteredSortedData, columnAccessor, configRegistry, columnHeaderDataLayer));

			final FilterRowHeaderComposite<Product> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
					new DefaultGlazedListsFilterStrategy<>(filteredHeaderData, columnAccessor, configRegistry),
					sortHeaderLayer, columnHeaderDataLayer.getDataProvider(), configRegistry);
			
			// create the row header layer stack
			final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
			final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
			rowHeaderDataLayer.setDefaultColumnWidth(60);
			final RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, compositeFreezeLayer, selectionLayer);

			// create the corner layer stack
			final ILayer cornerLayer = new CornerLayer(
					new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
					rowHeaderLayer, filterRowHeaderLayer);

			// create the grid layer composed with the prior created layer stacks
			gridLayer = new GridLayer(compositeFreezeLayer, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);
			setUnderlyingLayer(gridLayer);
		}
	}
	
	private class SummaryRowGridConfiguration extends DefaultSummaryRowConfiguration
	{
		private final IDataProvider dataProvider;

		public SummaryRowGridConfiguration(final IDataProvider dataProvider)
		{
			this.dataProvider = dataProvider;
			this.summaryRowFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}

		@Override public void addSummaryProviderConfig(final IConfigRegistry configRegistry)
		{
			// Labels are applied to the summary row and cells by default to
			// make configuration easier.
			// See the Javadoc for the SummaryRowLayer
			
			configRegistry.registerConfigAttribute(
                    SummaryRowConfigAttributes.SUMMARY_PROVIDER,
                    new StocInLeiTotalSummary(this.dataProvider, columns.indexOf(ULPColumn), columns.indexOf(priceColumn)), DisplayMode.NORMAL,
                    SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(nameColumn));

			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(
                    SummaryRowConfigAttributes.SUMMARY_PROVIDER,
                    new StocInLeiSummary(this.dataProvider, columns.indexOf(ULPColumn), columns.indexOf(priceColumn), columns.indexOf(col)), DisplayMode.NORMAL,
                    SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(col)));
		}
	}
	
	private class StocInLeiSummary implements ISummaryProvider
	{
		final private IDataProvider dataProvider;
		final private int ulpftvaColIndex;
		final private int pricePerUomColIndex;
		final private int stcColIndex;
		
		public StocInLeiSummary(final IDataProvider dataProvider, final int ulpftvaColIndex, final int pricePerUomColIndex,
				final int stcColIndex)
		{
			this.dataProvider = dataProvider;
			this.ulpftvaColIndex = ulpftvaColIndex;
			this.pricePerUomColIndex = pricePerUomColIndex;
			this.stcColIndex = stcColIndex;
		}
		
		@Override public Object summarize(final int columnIndex)
		{
			BigDecimal totalLaPretAchizitie = BigDecimal.ZERO;
			BigDecimal totalLaPretVanzare = BigDecimal.ZERO;
			final int rowCount = dataProvider.getRowCount();

			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
			{
				final BigDecimal stoc = (BigDecimal) dataProvider.getDataValue(stcColIndex, rowIndex);
				final BigDecimal pretAchizitieFaraTva = (BigDecimal) dataProvider.getDataValue(ulpftvaColIndex, rowIndex);
				final BigDecimal pretVanzareCuTva = (BigDecimal) dataProvider.getDataValue(pricePerUomColIndex, rowIndex);
				if (pretAchizitieFaraTva != null)
					totalLaPretAchizitie = totalLaPretAchizitie.add(pretAchizitieFaraTva.multiply(stoc));
				if (pretVanzareCuTva != null)
					totalLaPretVanzare = totalLaPretVanzare.add(pretVanzareCuTva.multiply(stoc));
			}
			return MessageFormat.format("La PUAfTVA={0} {2} La PV={1} {2}",
					displayBigDecimal(totalLaPretAchizitie, 2, RoundingMode.HALF_EVEN),
					displayBigDecimal(totalLaPretVanzare, 2, RoundingMode.HALF_EVEN),
					"RON");
		}
	}
	
	private class StocInLeiTotalSummary implements ISummaryProvider
	{
		final private IDataProvider dataProvider;
		final private int ulpftvaColIndex;
		final private int pricePerUomColIndex;
		
		public StocInLeiTotalSummary(final IDataProvider dataProvider, final int ulpftvaColIndex, final int pricePerUomColIndex)
		{
			this.dataProvider = dataProvider;
			this.ulpftvaColIndex = ulpftvaColIndex;
			this.pricePerUomColIndex = pricePerUomColIndex;
		}
		
		@Override public Object summarize(final int columnIndex)
		{
			BigDecimal totalLaPretAchizitie = BigDecimal.ZERO;
			BigDecimal totalLaPretVanzare = BigDecimal.ZERO;
			final int rowCount = dataProvider.getRowCount();

			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
			{
				final int ri = rowIndex;
				final BigDecimal stcTotal = stocColumns.keySet().stream()
						.map(col -> (BigDecimal) dataProvider.getDataValue(columns.indexOf(col), ri))
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
						
				final BigDecimal puafTVA = (BigDecimal) dataProvider.getDataValue(ulpftvaColIndex, rowIndex);
				final BigDecimal pv = (BigDecimal) dataProvider.getDataValue(pricePerUomColIndex, rowIndex);
				if (puafTVA != null)
					totalLaPretAchizitie = totalLaPretAchizitie.add(puafTVA.multiply(stcTotal));
				if (pv != null)
					totalLaPretVanzare = totalLaPretVanzare.add(pv.multiply(stcTotal));
			}
			return MessageFormat.format("La PUAfTVA={0} {2} La PV={1} {2}",
					displayBigDecimal(totalLaPretAchizitie, 2, RoundingMode.HALF_EVEN),
					displayBigDecimal(totalLaPretVanzare, 2, RoundingMode.HALF_EVEN),
					"RON");
		}
	}
	
	private class ColumnAccessor extends ReflectiveColumnPropertyAccessor<Product>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final Product rowObject, final int columnIndex)
		{
			final Column column = columns.get(columnIndex);
			
			if (column.getIndex() >= STOC_ID_BASE && column.getIndex() < AVERAGE_DAY_SALE_ID_BASE)
				return rowObject.stoc(stocColumns.get(column));
			if (column.getIndex() >= AVERAGE_DAY_SALE_ID_BASE && column.getIndex() < FUTURE_STOC_ID_BASE)
				return rowObject.averageDaySale(averageDaySaleColumns.get(column));
			if (column.getIndex() >= FUTURE_STOC_ID_BASE && column.getIndex() < MAX_SALE_ID_BASE)
				return rowObject.futureStoc(futureStocColumns.get(column));
			if (column.getIndex() >= MAX_SALE_ID_BASE && column.getIndex() < RECOMMENDED_ORDER_ID_BASE)
				return rowObject.maxSale(maxSaleColumns.get(column));
			if (column.getIndex() >= RECOMMENDED_ORDER_ID_BASE && column.getIndex() < RECOMMENDED_ORDER_ID_BASE+1000)
				return rowObject.recommendedOrder(recommendedOrderColumns.get(column));
			
			switch (column.getIndex())
			{
			case RETETA_QUANTITY_ID:
				return rowObject.matPrimaQuantity();
				
			case REDUCERI_ID:
				return rowObject.getReduceri().stream()
						.map(ProductReducereMapping::getReducere)
						.collect(toHashSet());

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
		
		@Override
		public void setDataValue(final Product rowObj, final int columnIndex, final Object newValue)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case RETETA_QUANTITY_ID:
				rowObj.matPrimaQuantity((BigDecimal) newValue);
				break;
			case REDUCERI_ID:
				if (newValue instanceof Collection<?>)
					rowObj.setReduceri(((Collection<?>) newValue).stream()
							.filter(Reducere.class::isInstance)
							.map(Reducere.class::cast)
							.map(red ->
							{
								final ProductReducereMapping mapping = new ProductReducereMapping();
								mapping.setReducere(red);
								mapping.setId(new ProductReducereMappingId(rowObj.getId(), red.getId()));
								return mapping;
							})
							.collect(toHashSet()));
				else
					rowObj.getReduceri().clear();
				break;
			default:
				super.setDataValue(rowObj, columnIndex, newValue);

			}
			if (afterChange != null)
				afterChange.accept(rowObj);
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
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
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(categoryColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uiCategoryColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(idxColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(casaDeptColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(raionColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(reduceriColumn));
			
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
			averageDaySaleColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			futureStocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			maxSaleColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			recommendedOrderColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					magentaBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(stocMinimColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(retetaQuantityColumn));
			
			// Register Cell Painters
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					new CheckBoxPainter(), DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(activColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					new CheckBoxPainter(), DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(hideWhenOrderingColumn));

			final UuidImagePainter imagePainter = new UuidImagePainter(bundle, log);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, imagePainter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(imageColumn));

			// Display converters
			final DefaultBigDecimalDisplayConverter defaultBigDecimalConv = new DefaultBigDecimalDisplayConverter();
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);
			final DefaultBigDecimalDisplayConverter quantityConverter = new DefaultBigDecimalDisplayConverter();
			quantityConverter.setMinimumFractionDigits(4);
			
			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			averageDaySaleColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			futureStocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			maxSaleColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			recommendedOrderColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(ULPColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(stocMinimColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBooleanDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(activColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBooleanDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(hideWhenOrderingColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultIntegerDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(idxColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, quantityConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(retetaQuantityColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uiCategoryColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(casaDeptColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(raionColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, 
					new PresentableDisplayConverter().setCollectionSeparator(LIST_SEPARATOR), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(reduceriColumn));
			
			// CELL EDITOR CONFIG
			if (source.equals(SourceLoc.VANZARI) && ClientSession.instance().hasPermission(Permissions.MODIFY_PRODUCT_PRICE_IN_SALE))
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			
			if (source.equals(SourceLoc.VANZARI) || source.equals(SourceLoc.MANAGER_ADAUGA))
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			
			if (source.equals(SourceLoc.RETETAR_MAT_PRIMA))
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(retetaQuantityColumn));
			
			if (source.equals(SourceLoc.CATALOG_PRODUSE))
			{
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(activColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(hideWhenOrderingColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uomColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(categoryColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uiCategoryColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(stocMinimColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(imageColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(casaDeptColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(raionColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(reduceriColumn));
				
				// CELL EDITOR
				final ComboBoxCellEditor allCatCellEditor = new ComboBoxCellEditor(Product.ALL_CATEGORIES.asList());
				allCatCellEditor.setFreeEdit(true);
				final ComboBoxCellEditor uiCatCellEditor = new ComboBoxCellEditor(uiCategories);
				uiCatCellEditor.setFreeEdit(true);
				final ComboBoxCellEditor deptCellEditor = new ComboBoxCellEditor(depts);
				deptCellEditor.setFreeEdit(true);
				final ComboBoxCellEditor raionCellEditor = new ComboBoxCellEditor(raioane);
				raionCellEditor.setFreeEdit(true);
				final ComboBoxCellEditor reduceriCellEditor = new ComboBoxCellEditor(reduceri);
				reduceriCellEditor.setMultiselect(true);
				reduceriCellEditor.setUseCheckbox(true);
				
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						new CheckBoxCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(activColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						new CheckBoxCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(hideWhenOrderingColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						allCatCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(categoryColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						uiCatCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uiCategoryColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						new UuidImageCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(imageColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						deptCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(casaDeptColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						raionCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(raionColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
						reduceriCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(reduceriColumn));
			}
			
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
	
	private static class RowIdAccessor implements IRowIdAccessor<Product>
	{
		@Override public Serializable getRowId(final Product rowObject)
		{
			return rowObject.getId();
		}
	}
	
	private class CustomContentTooltip extends NatTableContentTooltip
	{
		public CustomContentTooltip(final NatTable natTable, final String... tooltipRegions)
		{
			super(natTable, tooltipRegions);
		}
		
		@Override protected String getText(final Event event)
		{
			final int col = this.natTable.getColumnPositionByX(event.x);
			final int row = this.natTable.getRowPositionByY(event.y);
			
			if (filteredSortedData.isEmpty())
				return super.getText(event);
			
			if (source.equals(SourceLoc.VANZARI) && ClientSession.instance().hasPermission(Permissions.SUPERADMIN_ROLE))
			{
		        if (col == columns.indexOf(priceColumn)+1 && row > 0) //+1 because of the row header column 
		        {
		        	final Product product = filteredSortedData.get(this.natTable.getRowIndexByPosition(row));
		        	
		        	if (greaterThan(product.getLastBuyingPriceNoTva(), BigDecimal.ZERO))
		        		return "ULPfTVA = " + displayBigDecimal(product.getLastBuyingPriceNoTva());
		        }
			}
			if (col == columns.indexOf(reduceriColumn)+1 && row > 0) //+1 because of the row header column 
	        {
	        	final Product product = filteredSortedData.get(this.natTable.getRowIndexByPosition(row));
	        	return product.getReduceri().stream()
						.map(ProductReducereMapping::getReducere)
						.map(Reducere::displayName)
						.collect(Collectors.joining(NEWLINE));
	        }
			
			return super.getText(event);
		}
	}
}
