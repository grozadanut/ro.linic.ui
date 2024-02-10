package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;

import java.io.Serializable;
import java.util.List;

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
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultIntegerDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
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
import ca.odell.glazedlists.TransformedList;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.BarcodePrintable.LabelType;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class PrintBarcodeNatTable
{
	private static final Column barcodeColumn = new Column(0, BarcodePrintable.BARCODE_FIELD, "Cod", 100);
	private static final Column nameColumn = new Column(1, BarcodePrintable.NAME_FIELD, "Denumire", 300);
	private static final Column uomColumn = new Column(2, BarcodePrintable.UOM_FIELD, "UM", 50);
	private static final Column priceColumn = new Column(3, BarcodePrintable.PRICE_FIELD, "PU", 70);
	private static final Column cantitateColumn = new Column(4, BarcodePrintable.CANTITATE_FIELD, "Cantitate", 90);
	private static final Column labelTypeColumn = new Column(5, BarcodePrintable.LABEL_TYPE_FIELD, "Tip Eticheta", 120);
	
	private static ImmutableList<Column> ALL_COLUMNS = ImmutableList.<Column>builder()
			.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(priceColumn)
			.add(cantitateColumn)
			.add(labelTypeColumn)
			.build();

	private EventList<BarcodePrintable> sourceData;
	
	private NatTable table;
	
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private SortedList<BarcodePrintable> sortedList;
	
	public PrintBarcodeNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<BarcodePrintable> columnAccessor = new ColumnAccessor(ALL_COLUMNS.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<BarcodePrintable, BarcodePrintable> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

        final FilterList<BarcodePrintable> filteredData = new FilterList<>(rowObjectsGlazedList);
        sortedList = new SortedList<>(filteredData, null);

		// create the body layer stack
		final IRowDataProvider<BarcodePrintable> bodyDataProvider = new ListDataProvider<>(sortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < ALL_COLUMNS.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLUMNS.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
		final GlazedListsEventLayer<BarcodePrintable> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, sortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLUMNS.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<BarcodePrintable> sortHeaderLayer = new SortHeaderLayer<BarcodePrintable>(columnHeaderLayer,
				new GlazedListsSortModel<BarcodePrintable>(sortedList, columnAccessor, configRegistry, columnHeaderDataLayer));

		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
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
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<BarcodePrintable>()
				{
					@Override public Serializable getRowId(final BarcodePrintable rowObject)
					{
						return rowObject.hashCode();
					}
				}, false)); //single selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}

	public PrintBarcodeNatTable loadData(final ImmutableList<BarcodePrintable> data)
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
	
	public EventList<BarcodePrintable> getSourceData()
	{
		return sourceData;
	}
	
	public SortedList<BarcodePrintable> getFilteredSortedData()
	{
		return sortedList;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<BarcodePrintable> selection()
	{
		return ((RowSelectionModel<BarcodePrintable>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public ViewportLayer getViewportLayer()
	{
		return viewportLayer;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	private class ColumnAccessor extends ReflectiveColumnPropertyAccessor<BarcodePrintable>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final BarcodePrintable rowObject, final int columnIndex)
		{
			return super.getDataValue(rowObject, columnIndex);
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 0);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 1);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 2);
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 3);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 4);
			
			// Display converters
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultIntegerDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(cantitateColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(priceColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(labelTypeColumn));
			
			// CELL EDITOR CONFIG
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(cantitateColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(labelTypeColumn));
			
			// CELL EDITOR
			final ComboBoxCellEditor labelTypeCellEditor = new ComboBoxCellEditor(ImmutableList.copyOf(LabelType.values()));
			labelTypeCellEditor.setFreeEdit(true);
			
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					labelTypeCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(labelTypeColumn));
		}
	}
	
	private static class RowIdAccessor implements IRowIdAccessor<BarcodePrintable>
	{
		@Override public Serializable getRowId(final BarcodePrintable rowObject)
		{
			return rowObject.hashCode();
		}
	}
}
