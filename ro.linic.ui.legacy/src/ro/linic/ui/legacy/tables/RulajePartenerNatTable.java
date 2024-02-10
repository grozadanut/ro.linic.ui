package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.SPACE;

import java.io.Serializable;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.PercentageDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class RulajePartenerNatTable
{
	private static final Column nameColumn = new Column(0, RulajPartener.NAME_FIELD, "Partener", 200);
	private static final Column cardNrColumn = new Column(1, RulajPartener.CARD_NUMBER_FIELD, "Nr Card", 90);
	private static final Column cardPercentColumn = new Column(2, RulajPartener.CARD_PERCENT_FIELD, "%Disc", 70);
	private static final Column phoneColumn = new Column(3, RulajPartener.PHONE_NUMBER_FIELD, "Telefon", 100);
	private static final Column discAcum = new Column(4, RulajPartener.DISC_ACUM_FIELD, "Disc Acum", 100);
	private static final Column discChelt = new Column(5, RulajPartener.DISC_CHELT_FIELD, "Disc Chelt", 100);
	private static final Column discDisp = new Column(6, RulajPartener.DISC_DISP_FIELD, "Disc Disponibil", 120);
	
	private static ImmutableList<Column> columns = ImmutableList.<Column>builder()
			.add(nameColumn)
			.add(cardNrColumn)
			.add(cardPercentColumn)
			.add(phoneColumn)
			.add(discAcum)
			.add(discChelt)
			.add(discDisp)
			.build();
	
	private EventList<RulajPartener> sourceData;
	
	private NatTable table;
	private SelectionLayer selectionLayer;
	private TextMatcherEditor<RulajPartener> filter;

	public RulajePartenerNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<RulajPartener> columnAccessor = new ReflectiveColumnPropertyAccessor<>(
				columns.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<RulajPartener, RulajPartener> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

        final FilterList<RulajPartener> filteredData = new FilterList<>(rowObjectsGlazedList);
        final SortedList<RulajPartener> sortedList = new SortedList<>(filteredData, null);

		// create the body layer stack
		final IRowDataProvider<RulajPartener> bodyDataProvider = new ListDataProvider<>(sortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < columns.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		final GlazedListsEventLayer<RulajPartener> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<RulajPartener> sortHeaderLayer = new SortHeaderLayer<RulajPartener>(columnHeaderLayer,
				new GlazedListsSortModel<RulajPartener>(sortedList, columnAccessor, configRegistry, columnHeaderDataLayer));

		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		rowHeaderDataLayer.setDefaultColumnWidth(60);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, viewportLayer, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, sortHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(viewportLayer, sortHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<RulajPartener>()
				{
					@Override public Serializable getRowId(final RulajPartener rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multiple selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		filter = new TextMatcherEditor<RulajPartener>(new TextFilterator<RulajPartener>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final RulajPartener element)
			{
				baseList.add(element.getName());
				baseList.add(element.getCardNumber());
				baseList.add(element.getPhoneNumber());
			}
		});
		filter.setMode(TextMatcherEditor.CONTAINS);
		filteredData.setMatcherEditor(filter);
	}
	
	public RulajePartenerNatTable loadData(final ImmutableList<RulajPartener> data)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
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
	
	public RulajePartenerNatTable filter(final String search)
	{
		filter.setFilterText(search.split(SPACE));
        return this;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<RulajPartener> selection()
	{
		return ((RowSelectionModel<RulajPartener>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(cardNrColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(phoneColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discAcum));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discChelt));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discDisp));
		
			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PercentageDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(cardPercentColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBigDecimalDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discAcum));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBigDecimalDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discChelt));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBigDecimalDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(discDisp));
		}
	}
}
