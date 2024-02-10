package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.addToImmutableMap;
import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ISelectionChangedListener;
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
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
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
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.ClearAllSelectionsCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.PontajZilnic;
import ro.colibri.wrappers.PontajLine;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class PontajTable
{
	private static final int DAY_ID_BASE = 1000;
	
	private static final Column nameColumn = new Column(0, PontajLine.USER_FIELD, "Salariat", 200);
	
	private static final ImmutableMap<Column, Integer> dayColumns;
	
	static
	{
		final com.google.common.collect.ImmutableMap.Builder<Column, Integer> dayBuilder = ImmutableMap.builder();
		
		int i = 0;
		for (int day = 1 ; day <= 31 ; day++)
		{
			dayBuilder.put(new Column(DAY_ID_BASE+i, EMPTY_STRING, String.valueOf(day), 30), day);
			i++;
		}
		
		dayColumns = dayBuilder.build();
	}
	
	private ImmutableList<Column> columns;
	private EventList<PontajLine> sourceData;
	private FilterList<PontajLine> filteredData;
	private SortedList<PontajLine> filteredSortedData;
	
	private NatTable table;
	private RowSelectionProvider<PontajLine> selectionProvider;
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private IRowDataProvider<PontajLine> bodyDataProvider;
	private DataLayer bodyDataLayer;
	private GridLayer gridLayer;
	
	private Consumer<PontajLine> afterChange;
	
	public PontajTable()
	{
		final Builder<Column> builder = ImmutableList.<Column>builder();
		
			builder
			.add(nameColumn)
			.addAll(dayColumns.keySet());
			
		columns = builder.build();
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final CommonGridLayerStack gridLayer = new CommonGridLayerStack(configRegistry);
		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new DefaultFreezeGridBindings());
		table.addConfiguration(new FreezeMenuConfiguration(table));
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<PontajLine>()
				{
					@Override public Serializable getRowId(final PontajLine rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //true = multiple; false = single selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public PontajTable loadData(final ImmutableList<PontajLine> data)
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
	
	public PontajTable add(final PontajLine newLine)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.add(newLine);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public PontajTable remove(final PontajLine line)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(line);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<PontajLine> getSourceData()
	{
		return sourceData;
	}
	
	public FilterList<PontajLine> getFilteredData()
	{
		return filteredData;
	}
	
	public SortedList<PontajLine> getFilteredSortedData()
	{
		return filteredSortedData;
	}
	
	
	public void addSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.addSelectionChangedListener(listener);
	}
	
	public void removeSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.removeSelectionChangedListener(listener);
	}
	
	public void clearSelection()
	{
		selectionLayer.doCommand(new ClearAllSelectionsCommand());
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<PontajLine> selection()
	{
		return ((RowSelectionModel<PontajLine>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	public SelectionLayer getSelectionLayer()
	{
		return selectionLayer;
	}
	
	public PontajTable afterChange(final Consumer<PontajLine> afterChange)
	{
		this.afterChange = afterChange;
		return this;
	}
	
	private class CommonGridLayerStack extends AbstractLayerTransform
	{
		public CommonGridLayerStack(final ConfigRegistry configRegistry)
		{
			final IColumnPropertyAccessor<PontajLine> columnAccessor = new ColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
			
			sourceData = GlazedLists.eventListOf();
	        final TransformedList<PontajLine, PontajLine> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

	        filteredData = new FilterList<>(rowObjectsGlazedList);
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
			final ColumnHideShowLayer columnHideShowLayer = new ColumnHideShowLayer(dataChangeLayer);
			final GlazedListsEventLayer<PontajLine> glazedListsEventLayer = new GlazedListsEventLayer<>(columnHideShowLayer, filteredSortedData);
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
			final SortHeaderLayer<PontajLine> sortHeaderLayer = new SortHeaderLayer<PontajLine>(columnHeaderLayer,
					new GlazedListsSortModel<PontajLine>(filteredSortedData, columnAccessor, configRegistry, columnHeaderDataLayer));

			// create the row header layer stack
			final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
			final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
			rowHeaderDataLayer.setDefaultColumnWidth(60);
			final RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, compositeFreezeLayer, selectionLayer);

			// create the corner layer stack
			final ILayer cornerLayer = new CornerLayer(
					new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
					rowHeaderLayer, sortHeaderLayer);

			// create the grid layer composed with the prior created layer stacks
			gridLayer = new GridLayer(compositeFreezeLayer, sortHeaderLayer, rowHeaderLayer, cornerLayer);
			setUnderlyingLayer(gridLayer);
		}
	}
	
	private class ColumnAccessor extends ReflectiveColumnPropertyAccessor<PontajLine>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final PontajLine rowObject, final int columnIndex)
		{
			final Column column = columns.get(columnIndex);
			if (column.getIndex() >= DAY_ID_BASE && column.getIndex() < DAY_ID_BASE+1000)
			{
				final PontajZilnic pontajZi = rowObject.getPontajeZilnice().get(dayColumns.get(column));
				if (pontajZi == null)
					return EMPTY_STRING;
				return pontajZi.displayName();
			}
			
			return super.getDataValue(rowObject, columnIndex);
		}
		
		@Override
		public void setDataValue(final PontajLine rowObj, final int columnIndex, final Object newValue)
		{
			final Column column = columns.get(columnIndex);
			if (column.getIndex() >= DAY_ID_BASE && column.getIndex() < DAY_ID_BASE+1000)
			{
				final int dayOfMonth = dayColumns.get(column);
				PontajZilnic pontajZi = rowObj.getPontajeZilnice().get(dayOfMonth);
				if (pontajZi == null)
				{
					pontajZi = new PontajZilnic();
					rowObj.setPontajeZilnice(addToImmutableMap(rowObj.getPontajeZilnice(), dayOfMonth, pontajZi));
				}

				pontajZi.setWorkedHours((String) newValue);
			}
			else
				super.setDataValue(rowObj, columnIndex, newValue);
			
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
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			
			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			
			// CELL EDITOR CONFIG
			dayColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			
			// CELL EDITOR
			final ComboBoxCellEditor dayCellEditor = new ComboBoxCellEditor(PontajZilnic.ALL_PONTAJ_TYPES);
			dayCellEditor.setFreeEdit(true);
			
			dayColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					dayCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			
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
	
	private static class RowIdAccessor implements IRowIdAccessor<PontajLine>
	{
		@Override public Serializable getRowId(final PontajLine rowObject)
		{
			return rowObject.hashCode();
		}
	}
}
