package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.util.List;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
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
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune.Recenzie;
import ro.colibri.entities.user.User;
import ro.colibri.wrappers.ClasamentEntry;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;
import ro.linic.ui.legacy.tables.components.RecenziiImagePainter;

public class ClasamentNatTable
{
	private static final int profitPointsId = 3;
	private static final int extraPointsId = 4;
	private static final int totalPointsId = 5;
	private static final int estimatedSalaryBonusId = 6;
	private static final int recenziiId = 7;
	
	private static final Column gestiuneColumn = new Column(0, ClasamentEntry.USER_FIELD +"."+ User.SELECTED_GESTIUNE_FIELD +"."+ Gestiune.NAME_FIELD, "Locatie", 100);
	private static final Column usernameColumn = new Column(1, ClasamentEntry.USER_FIELD, "Utilizator", 200);
	private static final Column daysWorkedColumn = new Column(2, ClasamentEntry.DAYS_WORKED_FIELD, "Zile lucrate", 90);
	private static final Column profitPointsColumn = new Column(profitPointsId, EMPTY_STRING, "Puncte profit", 120);
	private static final Column extraPointsColumn = new Column(extraPointsId, EMPTY_STRING, "Puncte sarcini", 120);
	private static final Column totalPointsColumn = new Column(totalPointsId, EMPTY_STRING, "Total puncte", 120);
	private static final Column estimatedSalaryBonusColumn = new Column(estimatedSalaryBonusId, EMPTY_STRING, "Bonus salar(RON)", 150);
	private static final Column recenziiColumn = new Column(recenziiId, EMPTY_STRING, "Recenzii", 150);
	
	private static ImmutableList<Column> columns = ImmutableList.of(gestiuneColumn, usernameColumn, daysWorkedColumn,
			profitPointsColumn, extraPointsColumn, totalPointsColumn, recenziiColumn, estimatedSalaryBonusColumn);
	
	private EventList<ClasamentEntry> sourceData;
	private FilterList<ClasamentEntry> filteredData;
	private SortedList<ClasamentEntry> filteredSortedData;
	
	private NatTable table;
	private TextMatcherEditor<ClasamentEntry> quickSearchFilter;
	
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private IRowDataProvider<ClasamentEntry> bodyDataProvider;
	private DataLayer bodyDataLayer;
	private GridLayer gridLayer;
	private Bundle bundle;
	private Logger log;
	
	public ClasamentNatTable(final Bundle bundle, final Logger log)
	{
		this.bundle = bundle;
		this.log = log;
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
		table.configure();
		
		// define a TextMatcherEditor and set it to the FilterList
		quickSearchFilter = new TextMatcherEditor<ClasamentEntry>(new TextFilterator<ClasamentEntry>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final ClasamentEntry element)
			{
				baseList.add(safeString(element.getUser(), User::displayName));
			}
		});
		quickSearchFilter.setMode(TextMatcherEditor.CONTAINS);
        filteredData.setMatcherEditor(quickSearchFilter);
	}
	
	public ClasamentNatTable loadData(final ImmutableList<ClasamentEntry> data)
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
	
	public ClasamentNatTable add(final ClasamentEntry newEntry)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.add(newEntry);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public ClasamentNatTable remove(final ClasamentEntry entry)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(entry);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<ClasamentEntry> getSourceData()
	{
		return sourceData;
	}
	
	public FilterList<ClasamentEntry> getFilteredData()
	{
		return filteredData;
	}
	
	public SortedList<ClasamentEntry> getFilteredSortedData()
	{
		return filteredSortedData;
	}
	
	public ClasamentNatTable filter(final String searchText)
	{
        quickSearchFilter.setFilterText(searchText.split(SPACE));
        return this;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	private class CommonGridLayerStack extends AbstractLayerTransform
	{
		public CommonGridLayerStack(final ConfigRegistry configRegistry)
		{
			final IColumnPropertyAccessor<ClasamentEntry> columnAccessor = new ColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
			
			sourceData = GlazedLists.eventListOf();
	        final TransformedList<ClasamentEntry, ClasamentEntry> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

	        filteredData = new FilterList<>(rowObjectsGlazedList);
	        filteredSortedData = new SortedList<>(filteredData, null);

			// create the body layer stack
			bodyDataProvider = new ListDataProvider<>(filteredSortedData, columnAccessor);
			bodyDataLayer = new DataLayer(bodyDataProvider);
			bodyDataLayer.setDefaultRowHeight(32);
			bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
			for (int i = 0; i < columns.size(); i++)
				bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
			// add a DataChangeLayer that tracks data changes but directly updates
			// the underlying data model
			dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
			final GlazedListsEventLayer<ClasamentEntry> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, filteredSortedData);
			selectionLayer = new SelectionLayer(glazedListsEventLayer);
			viewportLayer = new ViewportLayer(selectionLayer);
			final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
			final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer, selectionLayer);
			
			// create the column header layer stack
			final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
			final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
			final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, compositeFreezeLayer, selectionLayer);
			columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
			
			// add the SortHeaderLayer to the column header layer stack
	        // as we use GlazedLists, we use the GlazedListsSortModel which
	        // delegates the sorting to the SortedList
			final SortHeaderLayer<ClasamentEntry> sortHeaderLayer = new SortHeaderLayer<ClasamentEntry>(columnHeaderLayer,
					new GlazedListsSortModel<ClasamentEntry>(filteredSortedData, columnAccessor, configRegistry, columnHeaderDataLayer));

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
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<ClasamentEntry>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final ClasamentEntry rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case profitPointsId:
				return rowObject.profitPoints();
				
			case extraPointsId:
				return rowObject.getExtraPoints();
			
			case totalPointsId:
				return rowObject.totalPoints();
				
			case estimatedSalaryBonusId:
				return rowObject.estimatedSalaryBonus();
				
			case recenziiId:
				return rowObject.getRecenzii();

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(gestiuneColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(recenziiColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(daysWorkedColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(profitPointsColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(extraPointsColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(totalPointsColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(estimatedSalaryBonusColumn));
			
			// Display converters
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(usernameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(estimatedSalaryBonusColumn));
			
			// cell painters
			final RecenziiImagePainter recenziiImgPainter = new RecenziiImagePainter(0, true);
			recenziiImgPainter.add(Recenzie.SMILEY, Icons.createImageResource(bundle, Icons.SMILEY_32x32_PATH, log).orElse(null));
			recenziiImgPainter.add(Recenzie.NEUTRU, Icons.createImageResource(bundle, Icons.OK_SMILEY_32x32_PATH, log).orElse(null));
			recenziiImgPainter.add(Recenzie.SAD, Icons.createImageResource(bundle, Icons.SAD_32x32_PATH, log).orElse(null));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					recenziiImgPainter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(recenziiColumn));
		}
	}
	
	private static class RowIdAccessor implements IRowIdAccessor<ClasamentEntry>
	{
		@Override public Serializable getRowId(final ClasamentEntry rowObject)
		{
			return rowObject.hashCode();
		}
	}
}
