package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.io.Serializable;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.nebula.widgets.nattable.data.convert.PercentageDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
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
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.IOverlayPainter;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
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
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummationSummaryProvider;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.wrappers.ProductProfitability;
import ro.colibri.wrappers.RaionProfitability;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class RaionProfitabilityNatTable
{
	private static final int profitPercentId = 3;

	private static final Column nameColumn = new Column(0, RaionProfitability.NAME_FIELD, "Raion", 200);
	private static final Column totalSalesColumn = new Column(1, RaionProfitability.TOTAL_SALES_FIELD, "Vanzari LEI",
			100);
	private static final Column totalProfitColumn = new Column(2, RaionProfitability.TOTAL_PROFIT_FIELD, "Profit LEI",
			100);
	private static final Column profitPercentColumn = new Column(profitPercentId, EMPTY_STRING, "%Profit", 100);

	private static ImmutableList<Column> columns = ImmutableList.of(nameColumn, totalSalesColumn,
			totalProfitColumn, profitPercentColumn);

	private EventList<RaionProfitability> sourceData;
	private FilterList<RaionProfitability> filteredData;
	private SortedList<RaionProfitability> filteredSortedData;

	private NatTable table;

	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private IRowDataProvider<RaionProfitability> bodyDataProvider;
	private DataLayer bodyDataLayer;
	private GridLayer gridLayer;
	private FixedSummaryRowLayer summaryRowLayer;
	private RowSelectionProvider<RaionProfitability> selectionProvider;

	public RaionProfitabilityNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final CommonGridLayerStack gridLayer = new CommonGridLayerStack(configRegistry);

		// create a standalone summary row
		// for a grid this is the FixedGridSummaryRowLayer
		summaryRowLayer = new FixedSummaryRowLayer(bodyDataLayer, gridLayer, configRegistry, false);
		summaryRowLayer.addConfiguration(new SummaryRowGridConfiguration(bodyDataProvider));
		summaryRowLayer.setSummaryRowLabel("Total");

		// create a composition that has the grid on top and the summary row on
		// the bottom
		final CompositeLayer composite = new CompositeLayer(1, 2);
		composite.setChildLayer("GRID", gridLayer, 0, 0);
		composite.setChildLayer("SUMMARY", summaryRowLayer, 0, 1);

		table = new NatTable(parent, composite, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new DefaultFreezeGridBindings());
		table.addConfiguration(new FreezeMenuConfiguration(table));
		// add filter row configuration
		table.addConfiguration(new FilterRowConfiguration());
		new CustomContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<RaionProfitability>()
				{
					@Override public Serializable getRowId(final RaionProfitability rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());

		// configure a painter that renders a line on top of the summary row
		// this is necessary because the CompositeLayerPainter does not render
		// lines on the top of a region
		table.addOverlayPainter(new IOverlayPainter()
		{
			@Override
			public void paintOverlay(final GC gc, final ILayer layer)
			{
				// render a line on top of the summary row
				final Color beforeColor = gc.getForeground();
				gc.setForeground(GUIHelper.COLOR_GRAY);
				final int gridBorderY = gridLayer.getHeight() - 1;
				gc.drawLine(0, gridBorderY, layer.getWidth() - 1, gridBorderY);
				gc.setForeground(beforeColor);
			}
		});

		table.configure();
	}

	public RaionProfitabilityNatTable loadData(final ImmutableCollection<RaionProfitability> data)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			this.sourceData.clear();
			this.sourceData.addAll(data);
			table.refresh();
		} finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}

	public EventList<RaionProfitability> getSourceData()
	{
		return sourceData;
	}

	public FilterList<RaionProfitability> getFilteredData()
	{
		return filteredData;
	}

	public SortedList<RaionProfitability> getFilteredSortedData()
	{
		return filteredSortedData;
	}

	public NatTable getTable()
	{
		return table;
	}

	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	public RowSelectionProvider<RaionProfitability> getSelectionProvider()
	{
		return selectionProvider;
	}

	private class CommonGridLayerStack extends AbstractLayerTransform
	{
		public CommonGridLayerStack(final ConfigRegistry configRegistry)
		{
			final IColumnPropertyAccessor<RaionProfitability> columnAccessor = new ColumnAccessor(
					columns.stream().map(Column::getProperty).collect(toImmutableList()));

			sourceData = GlazedLists.eventListOf();
			final TransformedList<RaionProfitability, RaionProfitability> rowObjectsGlazedList = GlazedLists
					.threadSafeList(sourceData);

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
			dataChangeLayer = new DataChangeLayer(bodyDataLayer,
					new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
			final GlazedListsEventLayer<RaionProfitability> glazedListsEventLayer = new GlazedListsEventLayer<>(
					dataChangeLayer, filteredSortedData);
			selectionLayer = new SelectionLayer(glazedListsEventLayer);
			viewportLayer = new ViewportLayer(selectionLayer);
			final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
			final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer,
					selectionLayer);

			// create the column header layer stack
			final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(
					columns.stream().map(Column::getName).toArray(String[]::new));
			final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
			final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer,
					compositeFreezeLayer, selectionLayer);
			columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
			
			// add the SortHeaderLayer to the column header layer stack
			// as we use GlazedLists, we use the GlazedListsSortModel which
			// delegates the sorting to the SortedList
			final SortHeaderLayer<RaionProfitability> sortHeaderLayer = new SortHeaderLayer<RaionProfitability>(
					columnHeaderLayer, new GlazedListsSortModel<RaionProfitability>(filteredSortedData,
							columnAccessor, configRegistry, columnHeaderDataLayer));

			// Note: The column header layer is wrapped in a filter row composite.
	        // This plugs in the filter row functionality
	        final FilterRowHeaderComposite<RaionProfitability> filterRowHeaderLayer =
	                new FilterRowHeaderComposite<>(
	                        new DefaultGlazedListsFilterStrategy<>(filteredData, columnAccessor, configRegistry),
	                        sortHeaderLayer, columnHeaderDataLayer.getDataProvider(), configRegistry);

			// create the row header layer stack
			final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
			final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
			rowHeaderDataLayer.setDefaultColumnWidth(60);
			final RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, compositeFreezeLayer,
					selectionLayer);

			// create the corner layer stack
			final ILayer cornerLayer = new CornerLayer(
					new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
					rowHeaderLayer, filterRowHeaderLayer);

			// create the grid layer composed with the prior created layer stacks
			gridLayer = new GridLayer(compositeFreezeLayer, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);
			selectionProvider =  new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
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

		@Override
		public void addSummaryProviderConfig(final IConfigRegistry configRegistry)
		{
			// Labels are applied to the summary row and cells by default to
			// make configuration easier.
			// See the Javadoc for the SummaryRowLayer
			configRegistry.registerConfigAttribute(SummaryRowConfigAttributes.SUMMARY_PROVIDER,
					new SummationSummaryProvider(this.dataProvider), DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(totalSalesColumn));
			configRegistry.registerConfigAttribute(SummaryRowConfigAttributes.SUMMARY_PROVIDER,
					new SummationSummaryProvider(this.dataProvider), DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(totalProfitColumn));
			configRegistry.registerConfigAttribute(SummaryRowConfigAttributes.SUMMARY_PROVIDER, columnIndex ->
			{
				// index+1 because of header column
				final Object profit = summaryRowLayer.getDataValueByPosition(columns.indexOf(totalProfitColumn) + 1, 0);
				final Object sales = summaryRowLayer.getDataValueByPosition(columns.indexOf(totalSalesColumn) + 1, 0);
				if (profit instanceof Number && sales instanceof Number)
				{
					if (((Number) sales).doubleValue() == 0)
						return 0;
					return ((Number) profit).doubleValue() / ((Number) sales).doubleValue();
				}
				return null;

			}, DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(profitPercentColumn));

			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);

			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter,
					DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(totalSalesColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter,
					DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(totalProfitColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new PercentageDisplayConverter(), DisplayMode.NORMAL,
					SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + columns.indexOf(profitPercentColumn));
		}
	}

	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<RaionProfitability>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
		}

		@Override
		public Object getDataValue(final RaionProfitability rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case profitPercentId:
				return rowObject.profitPercent();

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
			final Style yellowBgStyle = new Style();
			yellowBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			yellowBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
					Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));

			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);

			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(totalSalesColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(totalProfitColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(profitPercentColumn));

			// Display converters
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);

			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(totalSalesColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(totalProfitColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new PercentageDisplayConverter(), DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(profitPercentColumn));
		}
	}

	private static class RowIdAccessor implements IRowIdAccessor<RaionProfitability>
	{
		@Override
		public Serializable getRowId(final RaionProfitability rowObject)
		{
			return rowObject.hashCode();
		}
	}

	private class CustomContentTooltip extends NatTableContentTooltip
	{
		public CustomContentTooltip(final NatTable natTable, final String... tooltipRegions)
		{
			super(natTable, tooltipRegions);
		}

		@Override
		protected String getText(final Event event)
		{
			final int col = this.natTable.getColumnPositionByX(event.x);
			final int row = this.natTable.getRowPositionByY(event.y);

			if (col == columns.indexOf(totalSalesColumn) + 1 && row == 0) // +1 because of the row header column
				return ProductProfitability.salesHint();
			else if (col == columns.indexOf(totalProfitColumn) + 1 && row == 0) // +1 because of the row header column
				return ProductProfitability.profitHint();
			else if (col == columns.indexOf(profitPercentColumn) + 1 && row == 0) // +1 because of the row header column
				return "(Profit / Vanzari) x 100";

			return super.getText(event);
		}
	}
	
	/**
	 * The configuration to enable the edit mode for the grid and additional edit
	 * configurations like converters and validators.
	 */
	private static class FilterRowConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			configRegistry.registerConfigAttribute(FilterRowConfigAttributes.TEXT_DELIMITER, "&");
		}
	}
}
