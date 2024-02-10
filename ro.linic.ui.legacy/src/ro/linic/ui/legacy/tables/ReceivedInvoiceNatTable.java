package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.DATE_FORMATTER;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.SPACE;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
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
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import ro.colibri.wrappers.TwoEntityWrapperHet;
import ro.linic.ui.legacy.anaf.ReceivedInvoice;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.ExportMenuConfiguration;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;

public class ReceivedInvoiceNatTable
{
	private static final int supplierTaxIdID = 1;
	private static final int supplierNameID = 2;
	
	private static final Column uploadIndexColumn = new Column(0, "entity1.uploadIndex", "Index incarcare", 130);
	private static final Column supplierTaxIdColumn = new Column(supplierTaxIdID, "", "Cif emitent", 120);
	private static final Column supplierNameColumn = new Column(supplierNameID, "", "Nume emitent", 220);
	private static final Column issueDateColumn = new Column(3, "entity1.issueDate", "Data", 120);
	private static final Column invoiceNumberColumn = new Column(4, "entity2.ID.value", "Numar", 120);
	private static final Column totalColumn = new Column(5, "entity2.legalMonetaryTotal.taxInclusiveAmount.value", "ValCuTVA", 90);
	private static final Column invoiceIdColumn = new Column(6, "entity1.invoiceId", "Id factura", 70);
	private static final Column idColumn = new Column(7, "entity1.id", "Id", 70);
	
	private static ImmutableList<Column> ALL_COLUMNS = ImmutableList.<Column>builder()
			.add(uploadIndexColumn)
			.add(supplierTaxIdColumn)
			.add(supplierNameColumn)
			.add(issueDateColumn)
			.add(invoiceNumberColumn)
			.add(totalColumn)
			.add(invoiceIdColumn)
			.add(idColumn)
			.build();

	private EventList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> sourceData;
	
	private NatTable table;
	private TextMatcherEditor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> quickSearchFilter;
	
	private RowSelectionProvider<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> selectionProvider;
	private SelectionLayer selectionLayer;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	
	public ReceivedInvoiceNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> columnAccessor = new ColumnAccessor(ALL_COLUMNS.stream()
				.map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>, TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

        final FilterList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> filteredQuickData = new FilterList<>(rowObjectsGlazedList);
        final FilterList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> filteredHeaderData = new FilterList<>(filteredQuickData);
        final SortedList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> filteredSortedList = new SortedList<>(filteredHeaderData, null);

		// create the body layer stack
        final IRowDataProvider<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> bodyDataProvider = new ListDataProvider<>(filteredSortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < ALL_COLUMNS.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLUMNS.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
		final GlazedListsEventLayer<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, filteredSortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		
		selectionProvider = new RowSelectionProvider<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>(selectionLayer, bodyDataProvider);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLUMNS.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> sortHeaderLayer = new SortHeaderLayer<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>(columnHeaderLayer,
				new GlazedListsSortModel<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>(filteredSortedList, columnAccessor, configRegistry, columnHeaderDataLayer));

		final FilterRowHeaderComposite<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
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
		table.addConfiguration(new ExportMenuConfiguration(table));
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>()
				{
					@Override public Serializable getRowId(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		quickSearchFilter = new TextMatcherEditor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>(new TextFilterator<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> element)
			{
				Optional.ofNullable(element.getEntity2())
				.map(InvoiceType::getAccountingSupplierParty)
				.map(SupplierPartyType::getParty)
				.map(party -> party.getPartyName().stream().findFirst().orElse(null))
				.map(PartyNameType::getNameValue)
				.ifPresent(baseList::add);
			}
		});
		quickSearchFilter.setMode(TextMatcherEditor.CONTAINS);
		quickSearchFilter.setStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
		filteredQuickData.setMatcherEditor(quickSearchFilter);
	}

	public ReceivedInvoiceNatTable loadData(final ImmutableList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> data)
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
	
	public ReceivedInvoiceNatTable replace(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> old, final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> newP)
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
	
	public ReceivedInvoiceNatTable add(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> newP)
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
	
	public ReceivedInvoiceNatTable remove(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> data)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(data);
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> getSourceData()
	{
		return sourceData;
	}
	
	public ReceivedInvoiceNatTable filter(final String searchText)
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
	
	public List<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>> selection()
	{
		return ((RowSelectionModel<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public ViewportLayer getViewportLayer()
	{
		return viewportLayer;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> rowObject, final int columnIndex)
		{
			switch (ALL_COLUMNS.get(columnIndex).getIndex())
			{
			case supplierTaxIdID:
				return Optional.ofNullable(rowObject.getEntity2())
						.map(InvoiceType::getAccountingSupplierParty)
						.map(SupplierPartyType::getParty)
						.map(party -> party.getPartyTaxScheme().stream().findFirst().orElse(null))
						.map(PartyTaxSchemeType::getCompanyIDValue)
						.orElse(EMPTY_STRING);
			case supplierNameID:
				return Optional.ofNullable(rowObject.getEntity2())
						.map(InvoiceType::getAccountingSupplierParty)
						.map(SupplierPartyType::getParty)
						.map(party -> party.getPartyLegalEntity().stream().findFirst().orElse(null))
						.map(PartyLegalEntityType::getRegistrationNameValue)
						.orElse(EMPTY_STRING);

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
			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(DATE_FORMATTER), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(issueDateColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBigDecimalDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(totalColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + ALL_COLUMNS.indexOf(totalColumn));
		}
	}
	
	private static class RowIdAccessor implements IRowIdAccessor<TwoEntityWrapperHet<ReceivedInvoice, InvoiceType>>
	{
		@Override public Serializable getRowId(final TwoEntityWrapperHet<ReceivedInvoice, InvoiceType> rowObject)
		{
			return rowObject.hashCode();
		}
	}
}
