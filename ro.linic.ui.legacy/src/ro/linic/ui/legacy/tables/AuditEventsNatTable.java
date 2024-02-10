package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;

import java.io.Serializable;
import java.util.List;

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
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.user.AuditEvent;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class AuditEventsNatTable
{
	private static final Column typeColumn = new Column(0, AuditEvent.TYPE_FIELD, "Tip", 150);
	private static final Column creationTimeColumn = new Column(1, AuditEvent.CREATION_TIME_FIELD, "Data", 150);
	private static final Column userColumn = new Column(2, AuditEvent.USER_FIELD, "Operator", 120);
	private static final Column gestiuneColumn = new Column(3, AuditEvent.GESTIUNE_FIELD + "." + Gestiune.IMPORT_NAME_FIELD, "Gest", 50);
	private static final Column descriptionColumn = new Column(4, AuditEvent.DESCRIPTION_FIELD, "Descriere", 900);
	private static final Column idxColumn = new Column(5, AuditEvent.ID_FIELD, "idx", 90);
	
	private static ImmutableList<Column> ALL_COLUMNS = ImmutableList.<Column>builder()
			.add(typeColumn)
			.add(creationTimeColumn)
			.add(userColumn)
			.add(gestiuneColumn)
			.add(descriptionColumn)
			.add(idxColumn)
			.build();
	
	private EventList<AuditEvent> sourceData;
	private NatTable table;
	
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	
	public AuditEventsNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<AuditEvent> columnAccessor = new ExtendedReflectiveColumnPropertyAccessor<>(ALL_COLUMNS.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<AuditEvent, AuditEvent> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);
        final SortedList<AuditEvent> sortedData = new SortedList<>(rowObjectsGlazedList, null);

		// create the body layer stack
		final IRowDataProvider<AuditEvent> bodyDataProvider = new ListDataProvider<>(sortedData, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < ALL_COLUMNS.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLUMNS.get(i).getSize());
		final GlazedListsEventLayer<AuditEvent> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedData);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
		final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer, selectionLayer);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLUMNS.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, compositeFreezeLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();

		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<AuditEvent> sortHeaderLayer = new SortHeaderLayer<AuditEvent>(columnHeaderLayer,
				new GlazedListsSortModel<AuditEvent>(sortedData, columnAccessor, configRegistry, columnHeaderDataLayer));

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
		final GridLayer gridLayer = new GridLayer(compositeFreezeLayer, sortHeaderLayer, rowHeaderLayer, cornerLayer);

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
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<AuditEvent>()
				{
					@Override public Serializable getRowId(final AuditEvent rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public AuditEventsNatTable loadData(final ImmutableList<AuditEvent> data)
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
	
	public EventList<AuditEvent> getSourceData()
	{
		return sourceData;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<AuditEvent> selection()
	{
		return ((RowSelectionModel<AuditEvent>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override public void configureRegistry(final IConfigRegistry configRegistry)
		{
			// Display converters
			final PresentableDisplayConverter presentableConv = new PresentableDisplayConverter();
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, presentableConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(typeColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, presentableConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(userColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(creationTimeColumn));
		}
	}
}
