package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.selection.command.MoveSelectionCommand;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectCellCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class SelectableTable<T>
{
	public static final String GREEN_LABEL = "green_label";
	
	private static final int NAME_ID = 0;
	private static final Column nameColumn = new Column(NAME_ID, EMPTY_STRING, EMPTY_STRING, 0);
	
	private static ImmutableList<Column> ALL_COLUMNS = ImmutableList.<Column>builder()
			.add(nameColumn)
			.build();
	
	private EventList<T> sourceData;
	private NatTable table;
	private IDisplayConverter displayConverter;
	
//	private TextMatcherEditor<T> quickSearchFilter;
	private SelectionLayer selectionLayer;
	private RowSelectionProvider<T> selectionProvider;
	private ViewportLayer viewportLayer;
	private IMouseAction doubleClickAction;
	private Predicate<T> addGreenLabel;
	
	public SelectableTable()
	{
		
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnAccessor<T> columnAccessor = new ColumnAccessor(ALL_COLUMNS.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<T, T> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);
        final FilterList<T> filteredData = new FilterList<>(rowObjectsGlazedList);
        final SortedList<T> sortedData = new SortedList<>(filteredData, null);

		// create the body layer stack
		final IRowDataProvider<T> bodyDataProvider = new ListDataProvider<>(sortedData, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider)
		{
			@Override public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition)
			{
				super.accumulateConfigLabels(configLabels, columnPosition, rowPosition);
				final T rowObj = bodyDataProvider.getRowObject(rowPosition);
				if (addGreenLabel != null && addGreenLabel.test(rowObj))
					configLabels.addLabelOnTop(GREEN_LABEL);
			}
		});
		bodyDataLayer.setColumnWidthPercentageByPosition(0, 100);
		final GlazedListsEventLayer<T> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedData);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		viewportLayer.setRegionName(GridRegion.BODY);
		
		selectionProvider = new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
		
		table = new NatTable(parent, viewportLayer, false);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<T>()
				{
					@Override public Serializable getRowId(final T rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		// define a TextMatcherEditor and set it to the FilterList
//		quickSearchFilter = new TextMatcherEditor<GameProduct>(new TextFilterator<GameProduct>()
//		{
//			@Override public void getFilterStrings(final List<String> baseList, final GameProduct element)
//			{
//				baseList.add(element.getBarcode());
//				baseList.add(element.getName());
//			}
//		});
//		quickSearchFilter.setMode(TextMatcherEditor.CONTAINS);
//		
//		filteredData.setMatcherEditor(quickSearchFilter);
	}
	
	public SelectableTable<T> loadData(final ImmutableList<T> data)
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
	
	public SelectableTable<T> remove(final T product)
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
	
	public void addSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.addSelectionChangedListener(listener);
	}
	
	public void moveSelection(final MoveDirectionEnum direction)
	{
		final List<T> sel = selection();
		selectionProvider.setSelection(new StructuredSelection(sel));
		
		if (sel.isEmpty())
			viewportLayer.doCommand(new SelectCellCommand(viewportLayer, 0, 0, false, false));
		else
			viewportLayer.doCommand(new MoveSelectionCommand(direction, false, false));
	}
	
//	public SelectableTable<T> filter(final String searchText)
//	{
//	     quickSearchFilter.setFilterText(searchText.split(SPACE));
//	     return this;
//	}
	
	public EventList<T> getSourceData()
	{
		return sourceData;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public void setDisplayConverter(final IDisplayConverter displayConverter)
	{
		this.displayConverter = displayConverter;
	}
	
	public IDisplayConverter getDisplayConverter()
	{
		return displayConverter;
	}
	
	public void setAddGreenLabel(final Predicate<T> addGreenLabel)
	{
		this.addGreenLabel = addGreenLabel;
	}
	
	public List<T> selection()
	{
		return ((RowSelectionModel<T>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public void select(final T model)
	{
		if (model == null)
			selectionProvider.setSelection(StructuredSelection.EMPTY);
		else
			selectionProvider.setSelection(new StructuredSelection(model));
	}
	
	public void unselectAll()
	{
		selectionLayer.clear();
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(nameColumn));
			
			final Style greenStyle = new Style();
			greenStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_GREEN);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenStyle, DisplayMode.NORMAL,
					GREEN_LABEL);
			
			// DISPLAY CONVERTERS
			if (displayConverter != null)
				configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, displayConverter, DisplayMode.NORMAL, 
						ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(nameColumn));
		}
		
		@Override public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
			if (doubleClickAction != null)
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(0), doubleClickAction);
		}
	}
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<T>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
		}
		
		@Override
		public Object getDataValue(final T rowObject, final int columnIndex)
		{
			final Column column = ALL_COLUMNS.get(columnIndex);
			
			switch (column.getIndex())
			{
			case NAME_ID:
				return rowObject;

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
}
