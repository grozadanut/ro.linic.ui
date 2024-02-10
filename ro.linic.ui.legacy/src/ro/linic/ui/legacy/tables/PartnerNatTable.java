package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBooleanDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultLongDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.PercentageDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.CheckBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
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
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
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
import ro.colibri.embeddable.FidelityCard;
import ro.colibri.entities.comercial.GrupaInteres;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.mappings.PartnerGrupaInteresMapping;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.LocalDateUtils;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;

public class PartnerNatTable
{
	private static final Column activColumn = new Column(0, Partner.ACTIV_FIELD, "Activ", 70);
	private static final Column nameColumn = new Column(1, Partner.NAME_FIELD, "Partener", 300);
	private static final Column cuiColumn = new Column(2, Partner.COD_FISCAL_FIELD, "CUI", 100);
	private static final Column phoneColumn = new Column(3, Partner.PHONE_FIELD, "Telefon", 100);
	private static final Column cardNrColumn = new Column(4, Partner.FIDELITY_CARD_FIELD + "." + FidelityCard.NUMBER_FIELD, "Nr Card Fidelitate", 130);
	private static final Column cardPercentColumn = new Column(5, Partner.FIDELITY_CARD_FIELD + "." + FidelityCard.DISC_PERCENTAGE_FIELD, "%Disc", 70);
	private static final Column idxColumn = new Column(6, Partner.ID_FIELD, "Idx", 70);
	private static final Column inactivAnafColumn = new Column(7, Partner.INACTIV_FIELD, "InactivANAF", 70);
	private static final Column tvaIncasareColumn = new Column(8, Partner.TVA_INCASARE_FIELD, "TVA la Incasare", 70);
	private static final Column tvaIncasareFromColumn = new Column(9, Partner.TVA_INCASARE_FROM_FIELD, "TVA la Incasare De La", 90);
	private static final Column tvaIncasareToColumn = new Column(10, Partner.TVA_INCASARE_TO_FIELD, "TVA la Incasare Pana La", 90);
	private static final Column emailColumn = new Column(11, Partner.EMAIL_FIELD, "Email", 100);
	private static final Column grupeInteresColumn = new Column(12, EMPTY_STRING, "Grupe Interes", 130);
	private static final Column splitTvaColumn = new Column(13, Partner.SPLIT_TVA_FIELD, "Split TVA", 70);
	private static final Column dataInceputSplitTvaColumn = new Column(14, Partner.DATA_INCEPUT_SPLIT_TVA_FIELD, "Data inceput SPLIT TVA", 90);
	private static final Column dataAnulareSplitTvaColumn = new Column(15, Partner.DATA_ANULARE_SPLIT_TVA_FIELD, "Data anulare SPLIT TVA", 90);
	
	private static ImmutableList<Column> ALL_COLUMNS = ImmutableList.<Column>builder()
			.add(activColumn)
			.add(nameColumn)
			.add(cuiColumn)
			.add(phoneColumn)
			.add(emailColumn)
			.add(grupeInteresColumn)
			.add(cardNrColumn)
			.add(cardPercentColumn)
			.add(inactivAnafColumn)
			.add(tvaIncasareColumn)
			.add(tvaIncasareFromColumn)
			.add(tvaIncasareToColumn)
			.add(splitTvaColumn)
			.add(dataInceputSplitTvaColumn)
			.add(dataAnulareSplitTvaColumn)
			.add(idxColumn)
			.build();

	private EventList<Partner> sourceData;
	
	private NatTable table;
	private TextMatcherEditor<Partner> quickSearchFilter;
	
	private RowSelectionProvider<Partner> selectionProvider;
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	
	public PartnerNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<Partner> columnAccessor = new ColumnAccessor(ALL_COLUMNS.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<Partner, Partner> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

        final FilterList<Partner> filteredQuickData = new FilterList<>(rowObjectsGlazedList);
        final FilterList<Partner> filteredHeaderData = new FilterList<>(filteredQuickData);
        final SortedList<Partner> filteredSortedList = new SortedList<>(filteredHeaderData, null);

		// create the body layer stack
        final IRowDataProvider<Partner> bodyDataProvider = new ListDataProvider<>(filteredSortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < ALL_COLUMNS.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLUMNS.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
		final GlazedListsEventLayer<Partner> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, filteredSortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		
		selectionProvider = new RowSelectionProvider<Partner>(selectionLayer, bodyDataProvider);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLUMNS.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<Partner> sortHeaderLayer = new SortHeaderLayer<Partner>(columnHeaderLayer,
				new GlazedListsSortModel<Partner>(filteredSortedList, columnAccessor, configRegistry, columnHeaderDataLayer));

		final FilterRowHeaderComposite<Partner> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
				new DefaultGlazedListsFilterStrategy<>(filteredHeaderData, columnAccessor, configRegistry),
				sortHeaderLayer, columnHeaderDataLayer.getDataProvider(), configRegistry);

		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		rowHeaderDataLayer.setDefaultColumnWidth(60);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, viewportLayer, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, filterRowHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(viewportLayer, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new FilterRowConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Partner>()
				{
					@Override public Serializable getRowId(final Partner rowObject)
					{
						return rowObject.hashCode();
					}
				}, false)); //single selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		quickSearchFilter = new TextMatcherEditor<Partner>(new TextFilterator<Partner>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Partner element)
			{
				baseList.add(element.getName());
				baseList.add(element.getPhone());
				baseList.add(safeString(element.getFidelityCard(), FidelityCard::getNumber));
			}
		});
		quickSearchFilter.setMode(TextMatcherEditor.CONTAINS);
		quickSearchFilter.setStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
		filteredQuickData.setMatcherEditor(quickSearchFilter);
	}

	public PartnerNatTable loadData(final ImmutableList<Partner> data)
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
	
	public PartnerNatTable replace(final Partner old, final Partner newP)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			if (sourceData.indexOf(old) != -1)
				sourceData.set(sourceData.indexOf(old), newP);
			else
				sourceData.add(newP);
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public PartnerNatTable add(final Partner newP)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.add(newP);
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public PartnerNatTable remove(final Partner partner)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(partner);
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<Partner> getSourceData()
	{
		return sourceData;
	}
	
	public PartnerNatTable filter(final String searchText)
	{
        quickSearchFilter.setFilterText(searchText.split(SPACE));
        return this;
	}
	
	public void moveSelection(final MoveDirectionEnum direction)
	{
		final PositionCoordinate lastSelectedCell = selectionLayer.getLastSelectedCellPosition();
		
		if (lastSelectedCell == null ||
				getViewportLayer().getRowIndexByPosition(lastSelectedCell.rowPosition) == -1)
			getViewportLayer().doCommand(new SelectCellCommand(getViewportLayer(), 0, 0, false, false));
		else
			getViewportLayer().doCommand(new MoveSelectionCommand(direction, false, false));
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
	
	public List<Partner> selection()
	{
		return ((RowSelectionModel<Partner>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public ViewportLayer getViewportLayer()
	{
		return viewportLayer;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<Partner>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final Partner rowObject, final int columnIndex)
		{
			if (columnIndex == ALL_COLUMNS.indexOf(grupeInteresColumn))
				return rowObject.getGrupeInteres().stream()
						.map(PartnerGrupaInteresMapping::getGrupaInteres)
						.map(GrupaInteres::displayName)
						.collect(Collectors.joining(LIST_SEPARATOR));
			
			return super.getDataValue(rowObject, columnIndex);
		}
		
		@Override
		public void setDataValue(final Partner rowObj, final int columnIndex, final Object newValue)
		{
			if (columnIndex == ALL_COLUMNS.indexOf(activColumn))
			{
				final InvocationResult result = BusinessDelegate.setPartnerActive(rowObj.getId(), (boolean) newValue);
				replace(rowObj, result.extra(InvocationResult.PARTNER_KEY));
			}
			else
				super.setDataValue(rowObj, columnIndex, newValue);
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			// Register Cell Painters
			final CheckBoxPainter checkboxPainter = new CheckBoxPainter();
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(activColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(inactivAnafColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(tvaIncasareColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(splitTvaColumn));
			
			// Display converters
			final DefaultBooleanDisplayConverter booleanConv = new DefaultBooleanDisplayConverter();
			final LocalDateTimeDisplayConverter dateConv = new LocalDateTimeDisplayConverter(LocalDateUtils.DATE_FORMATTER);
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(activColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PercentageDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(cardPercentColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(inactivAnafColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(tvaIncasareColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, dateConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(tvaIncasareFromColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, dateConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(tvaIncasareToColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(splitTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, dateConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(dataInceputSplitTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, dateConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(dataAnulareSplitTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultLongDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(idxColumn));
			
			// CELL EDITOR CONFIG
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(activColumn));
			
			// CELL EDITOR
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(activColumn));
		}
	}
	
	private static class RowIdAccessor implements IRowIdAccessor<Partner>
	{
		@Override public Serializable getRowId(final Partner rowObject)
		{
			return rowObject.getId();
		}
	}
}
