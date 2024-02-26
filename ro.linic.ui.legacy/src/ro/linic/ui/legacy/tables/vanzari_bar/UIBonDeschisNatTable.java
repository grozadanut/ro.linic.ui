package ro.linic.ui.legacy.tables.vanzari_bar;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

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
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.Operatiune;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class UIBonDeschisNatTable
{
	private static final int totalID = 4;
	
	private static final Column nameColumn = new Column(0, Operatiune.NAME_FIELD, "DENUMIRE", 150);
	private static final Column uomColumn = new Column(1, Operatiune.UM_FIELD, "UM", 50);
	private static final Column pretColumn = new Column(2, Operatiune.PRET_UNITAR_FIELD, "PU", 50);
	private static final Column cantColumn = new Column(3, Operatiune.CANTITATE_FIELD, "Cant", 40);
	private static final Column totalColumn = new Column(totalID, EMPTY_STRING, "Total", 60);
	
	private static ImmutableList<Column> ALL_COLS = ImmutableList.<Column>builder()
			.add(nameColumn)
			.add(uomColumn)
			.add(pretColumn)
			.add(cantColumn)
			.add(totalColumn)
			.build();
	
	private EventList<Operatiune> sourceData;
	
	private NatTable table;
	private IRowDataProvider<Operatiune> bodyDataProvider;
	private SelectionLayer selectionLayer;
	
	public UIBonDeschisNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final BodyLayerStack gridLayer = new BodyLayerStack(configRegistry);
		
		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<Operatiune>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Operatiune>()
				{
					@Override public Serializable getRowId(final Operatiune rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public UIBonDeschisNatTable loadData(final ImmutableList<Operatiune> data)
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
	
	public NatTable getTable()
	{
		return table;
	}
	
	public EventList<Operatiune> getSourceData()
	{
		return sourceData;
	}
	
	public List<Operatiune> selection()
	{
		return ((RowSelectionModel<Operatiune>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	/**
     * Always encapsulate the body layer stack in an AbstractLayerTransform to
     * ensure that the index transformations are performed in later commands.
     *
     * @param 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class BodyLayerStack extends AbstractLayerTransform
    {
        public BodyLayerStack(final ConfigRegistry configRegistry)
        {
			final IColumnPropertyAccessor<Operatiune> columnAccessor = new ColumnAccessor(ALL_COLS.stream().map(Column::getProperty).collect(toImmutableList()));
			
			sourceData = GlazedLists.eventListOf();
	        final TransformedList<Operatiune, Operatiune> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

	        final FilterList filteredData = new FilterList<>(rowObjectsGlazedList);
	        final SortedList filteredSortedData = new SortedList<>(filteredData, null);

			// create the body layer stack
			bodyDataProvider = new ListDataProvider<>(filteredSortedData, columnAccessor);
			final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
			bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
			for (int i = 0; i < ALL_COLS.size(); i++)
				bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLS.get(i).getSize());
			// add a DataChangeLayer that tracks data changes but directly updates
			// the underlying data model
			final GlazedListsEventLayer<Operatiune> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, filteredSortedData);
			selectionLayer = new SelectionLayer(glazedListsEventLayer);
			final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
			
			// create the column header layer stack
			final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLS.stream().map(Column::getName).toArray(String[]::new));
			final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
			final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
			columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
			
			// add the SortHeaderLayer to the column header layer stack
	        // as we use GlazedLists, we use the GlazedListsSortModel which
	        // delegates the sorting to the SortedList
			final SortHeaderLayer<Operatiune> sortHeaderLayer = new SortHeaderLayer<Operatiune>(columnHeaderLayer,
					new GlazedListsSortModel<Operatiune>(filteredSortedData, columnAccessor, configRegistry, columnHeaderDataLayer));
			
			final CompositeLayer compositeLayer = new CompositeLayer(1, 2);
	        compositeLayer.setChildLayer(GridRegion.COLUMN_HEADER, sortHeaderLayer, 0, 0);
	        compositeLayer.setChildLayer(GridRegion.BODY, viewportLayer, 0, 1);
			
			setUnderlyingLayer(compositeLayer);
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
			Style greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(uomColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			final Style blueBgStyle = new Style();
			blueBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			blueBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			blueBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, blueBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(pretColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(cantColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(totalColumn));
		
			// Register Display converters
			final IDisplayConverter bigDecimalConv = new DefaultBigDecimalDisplayConverter();
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(pretColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(cantColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLS.indexOf(totalColumn));
		}
	}
	
	private class ColumnAccessor extends ReflectiveColumnPropertyAccessor<Operatiune>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final Operatiune rowObject, final int columnIndex)
		{
			switch (ALL_COLS.get(columnIndex).getIndex())
			{
			case totalID:
				return rowObject.getTotal();

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
}
