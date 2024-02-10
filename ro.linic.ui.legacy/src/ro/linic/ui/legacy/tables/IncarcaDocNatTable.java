package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.DATE_FORMATTER;

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
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
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
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Partner;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;

public class IncarcaDocNatTable
{
	private static ImmutableList<Column> columns = ImmutableList.<Column>builder()
			.add(new Column(0, AccountingDocument.GESTIUNE_FIELD + "." + Gestiune.IMPORT_NAME_FIELD, "L", 50))
			.add(new Column(1, AccountingDocument.DOC_FIELD, "Tip document", 120))
			.add(new Column(2, AccountingDocument.NR_DOC_FIELD, "Nr document", 100))
			.add(new Column(3, AccountingDocument.DATA_DOC_FIELD, "Data document", 100))
			.add(new Column(4, AccountingDocument.PARTNER_FIELD + "." + Partner.NAME_FIELD, "Partener", 200))
			.add(new Column(5, AccountingDocument.NR_RECEPTIE_FIELD, "Nr receptie", 100))
			.add(new Column(6, AccountingDocument.DATA_RECEPTIE_FIELD, "Data receptie", 100))
			.build();

	private EventList<AccountingDocument> sourceData;
	
	private NatTable table;
	
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	
	private IMouseAction doubleClickAction;

	public IncarcaDocNatTable()
	{
	}
	
	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<AccountingDocument> columnAccessor = new ColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
		final TransformedList<AccountingDocument, AccountingDocument> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);
		final SortedList<AccountingDocument> sortedList = new SortedList<>(rowObjectsGlazedList, null);

		// create the body layer stack
		final IRowDataProvider<AccountingDocument> bodyDataProvider = new ListDataProvider<>(sortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < columns.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		final GlazedListsEventLayer<AccountingDocument> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		// add default column labels to the label stack
        // need to be done on the column header data layer, otherwise the label
        // stack does not contain the necessary labels at the time the
        // comparator is searched
        columnHeaderDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator());
        
        final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<AccountingDocument> sortHeaderLayer = new SortHeaderLayer<AccountingDocument>(columnHeaderLayer,
				new GlazedListsSortModel<AccountingDocument>(sortedList, columnAccessor, configRegistry, columnHeaderDataLayer));
		
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
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<AccountingDocument>()
				{
					@Override public Serializable getRowId(final AccountingDocument rowObject)
					{
						return rowObject.hashCode();
					}
				}, false)); //single selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public IncarcaDocNatTable loadData(final ImmutableList<AccountingDocument> data)
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
	
	public AccountingDocument selection()
	{
		return ((RowSelectionModel<AccountingDocument>) selectionLayer.getSelectionModel()).getSelectedRowObjects().stream()
				.findFirst()
				.orElse(null);
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			final IDisplayConverter timeToDateConverter = new LocalDateTimeDisplayConverter(DATE_FORMATTER);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, timeToDateConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 3); // data doc
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, timeToDateConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 6); // data receptie
		}
		
		@Override
		public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
			if (doubleClickAction != null)
			{
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.rowHeaderLeftClick(0), doubleClickAction);
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(0), doubleClickAction);
			}
		}
	}
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<AccountingDocument>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
	        super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final AccountingDocument rowObject, final int columnIndex)
		{
			switch (columnIndex)
			{
			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
}
