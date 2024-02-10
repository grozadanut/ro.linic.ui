package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.DATE_FORMATTER;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.StringUtils.globalIsMatch;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBooleanDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultLongDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.CheckBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.config.DefaultFreezeGridBindings;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
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
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class OperationsNatTable implements IMouseAction
{
	private static final int tvaPercentID = 3;
	private static final int transferatID = 25;
	
	private static final Column rpzCol = new Column(0, Operatiune.RPZ_FIELD, "RPZ", 40);
	private static final Column tipOpCol = new Column(1, Operatiune.TIP_OP_FIELD, "Intr/Ies", 70);
	private static final Column categoryCol = new Column(2, Operatiune.CATEGORIE_FIELD, "Categorie", 90);
	private static final Column tvaPercentCol = new Column(tvaPercentID, "TVA", "%TVA", 50);
	private static final Column barcodeCol = new Column(4, Operatiune.BARCODE_FIELD, "COD", 70);
	private static final Column nameCol = new Column(5, Operatiune.NAME_FIELD, "DENUMIRE", 250);
	private static final Column uomCol = new Column(6, Operatiune.UM_FIELD, "UM", 50);
	private static final Column gestCol = new Column(7, Operatiune.GESTIUNE_FIELD + "." + Gestiune.IMPORT_NAME_FIELD, "L", 50);
	private static final Column cantitateCol = new Column(8, Operatiune.CANTITATE_FIELD, "Cant", 70);
	private static final Column puafTvaCol = new Column(9, Operatiune.PUA_fTVA_FIELD, "PUA-fTVA", 70);
	private static final Column vafTvaCol = new Column(10, Operatiune.VA_fTVA_FIELD, "VA-fTVA", 70);
	private static final Column vaTvaCol = new Column(11, Operatiune.VA_TVA_FIELD, "VA-TVA", 70);
	private static final Column pvCol = new Column(12, Operatiune.PRET_UNITAR_FIELD, "PV+TVA", 70);
	private static final Column vvfTvaCol = new Column(13, Operatiune.VV_fTVA_FIELD, "VV-fTVA", 70);
	private static final Column vvTvaCol = new Column(14, Operatiune.VV_TVA_FIELD, "VV-TVA", 70);
	private static final Column accDocGestCol = new Column(15, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.GESTIUNE_FIELD + "." + Gestiune.IMPORT_NAME_FIELD, "DocL", 50);
	private static final Column accDocDocCol = new Column(16, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.DOC_FIELD, "Doc", 100);
	private static final Column accDocNrCol = new Column(17, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.NR_DOC_FIELD, "Nr", 100);
	private static final Column accDocDataCol = new Column(18, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.DATA_DOC_FIELD, "Data", 100);
	private static final Column accDocPartnerCol = new Column(19, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.PARTNER_FIELD, "Partener", 100);
	private static final Column operatorCol = new Column(20, Operatiune.OPERATOR_FIELD, "Operator", 90);
	private static final Column dataOpCol = new Column(21, Operatiune.DATA_OP_FIELD, "Dataop", 150);
	private static final Column shouldVerifyCol = new Column(22, Operatiune.SHOULD_VERIFY_FIELD, "De verificat", 70);
	private static final Column verifiedCol = new Column(23, Operatiune.VERIFICAT_FIELD, "Verificat", 150);
	private static final Column idCol = new Column(24, Operatiune.ID_FIELD, "idx", 60);
	private static final Column transferatCol = new Column(transferatID, EMPTY_STRING, "Transferat", 70);
	private static final Column accDocDataRecCol = new Column(26, Operatiune.ACC_DOC_FIELD + "." + AccountingDocument.DATA_RECEPTIE_FIELD, "DataRec", 100);
	
	private EventList<Operatiune> sourceData;
	private NatTable table;
	
	private SelectionLayer selectionLayer;
	private RowSelectionProvider<Operatiune> selectionProvider;
	private ViewportLayer viewportLayer;
	private DataChangeLayer dataChangeLayer;
	private IRowDataProvider<Operatiune> bodyDataProvider;
	private ImmutableList<Column> columns;
	
	private Consumer<Operatiune> afterChange;
	private IMouseAction doubleClickAction;
	private Consumer<Operatiune> transferRowClick;

	public OperationsNatTable()
	{
		final Builder<Column> builder = ImmutableList.<Column>builder();
		builder
		.add(rpzCol)
		.add(tipOpCol)
		.add(categoryCol)
		.add(tvaPercentCol)
		.add(barcodeCol)
		.add(nameCol)
		.add(uomCol)
		.add(gestCol)
		.add(cantitateCol)
		.add(transferatCol)
		.add(puafTvaCol)
		.add(vafTvaCol)
		.add(vaTvaCol)
		.add(pvCol)
		.add(vvfTvaCol)
		.add(vvTvaCol)
		.add(accDocGestCol)
		.add(accDocDocCol)
		.add(accDocNrCol)
		.add(accDocDataCol)
		.add(accDocDataRecCol)
		.add(accDocPartnerCol)
		.add(operatorCol)
		.add(dataOpCol)
		.add(shouldVerifyCol)
		.add(verifiedCol)
		.add(idCol);
		columns = builder.build();
	}
	
	public void postConstruct(final Composite parent)
	{
		final IColumnPropertyAccessor<Operatiune> columnAccessor = new ColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<Operatiune, Operatiune> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);

        final FilterList<Operatiune> filteredData = new FilterList<>(rowObjectsGlazedList);
        final SortedList<Operatiune> sortedList = new SortedList<>(filteredData, null);

		// create the body layer stack
		bodyDataProvider = new ListDataProvider<>(sortedList, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		for (int i = 0; i < columns.size(); i++)
			bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
		// add a DataChangeLayer that tracks data changes but directly updates
		// the underlying data model
		dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor()), false);
		final GlazedListsEventLayer<Operatiune> glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer, sortedList);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		selectionProvider = new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
		final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
		final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer, selectionLayer);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, compositeFreezeLayer, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		// add default column labels to the label stack
        // need to be done on the column header data layer, otherwise the label
        // stack does not contain the necessary labels at the time the
        // comparator is searched
//        columnHeaderDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		
		// add the SortHeaderLayer to the column header layer stack
        // as we use GlazedLists, we use the GlazedListsSortModel which
        // delegates the sorting to the SortedList
		final SortHeaderLayer<Operatiune> sortHeaderLayer = new SortHeaderLayer<Operatiune>(columnHeaderLayer,
				new GlazedListsSortModel<Operatiune>(sortedList, columnAccessor, configRegistry, columnHeaderDataLayer));

		final FilterRowHeaderComposite<Operatiune> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
				new DefaultGlazedListsFilterStrategy<>(filteredData, columnAccessor, configRegistry),
				sortHeaderLayer, columnHeaderDataLayer.getDataProvider(), configRegistry);
		
		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		rowHeaderDataLayer.setDefaultColumnWidth(60);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, compositeFreezeLayer, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, filterRowHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(compositeFreezeLayer, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new DefaultFreezeGridBindings());
		table.addConfiguration(new FreezeMenuConfiguration(table));
		table.addConfiguration(new FilterRowConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Operatiune>()
				{
					@Override public Serializable getRowId(final Operatiune rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multiple selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public OperationsNatTable loadData(final ImmutableList<Operatiune> data)
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
	
	public OperationsNatTable replace(final Operatiune oldOp, final Operatiune newOp)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.set(sourceData.indexOf(oldOp), newOp);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public void afterChange(final Consumer<Operatiune> afterChange)
	{
		this.afterChange = afterChange;
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}
	
	public void transferRowClick(final Consumer<Operatiune> transferRowClick)
	{
		this.transferRowClick = transferRowClick;
	}
	
	public EventList<Operatiune> getSourceData()
	{
		return sourceData;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public SelectionLayer getSelectionLayer()
	{
		return selectionLayer;
	}
	
	public RowSelectionProvider<Operatiune> getSelectionProvider()
	{
		return selectionProvider;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	public List<Operatiune> selection()
	{
		return ((RowSelectionModel<Operatiune>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	@Override
	public void run(final NatTable natTable, final MouseEvent event)
	{
		final NatEventData eventData = NatEventData.createInstanceFromEvent(event);
		final int rowIndex = natTable.getRowIndexByPosition(eventData.getRowPosition());

		final Operatiune rowObject = bodyDataProvider.getRowObject(rowIndex);
		if (transferRowClick != null)
			transferRowClick.accept(rowObject);
	}
	
	private class ColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<Operatiune>
	{
		public ColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final Operatiune rowObj, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case tvaPercentID:
				final BigDecimal tvaPercent = AccountingDocument.extractTvaPercentage(rowObj.getValoareAchizitieFaraTVA(), rowObj.getValoareAchizitieTVA());
				return tvaPercent.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_EVEN);
			case transferatID:
				/**
				 * Transfer: 
				 * (child1)(owner2)ciment l1 -50 
				 * (child2)ciment l2 50
				 * 
				 * Bon casa: 
				 * (owner1) ciment l2 150 splits into: 
				 * 			- (child2) ciment l1 50(changed gestiune to child1.gest l1) 
				 * 			- (owner1) ciment l2 100(cant-child2.cant)
				 * 
				 */
				if (TipOp.IESIRE.equals(rowObj.getTipOp()) &&
						rowObj.getChildOp() != null && rowObj.getChildOp().getChildOp() != null &&
						globalIsMatch(rowObj.getChildOp().getAccDoc().getDoc(), AccountingDocument.TRANSFER_DOC_NAME, TextFilterMethod.EQUALS))
					return rowObj.getChildOp().getChildOp().getCantitate();
				return BigDecimal.ZERO;
				
			default:
				return super.getDataValue(rowObj, columnIndex);
			}
		}
		
		@Override
		public void setDataValue(final Operatiune rowObj, final int columnIndex, final Object newValue)
		{
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
			final Style greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			// Register Cell style
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(rpzCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(tipOpCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(categoryCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(tvaPercentCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocGestCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocDocCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocNrCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocDataCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocDataRecCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocPartnerCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(operatorCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(dataOpCol));
			
			final Style yellowBgStyle = new Style();
			yellowBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(barcodeCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uomCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(idCol));
			
			final Style blueBgStyle = new Style();
			blueBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			blueBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, blueBgStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(gestCol));
			
			// Register Cell Painters
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					new CheckBoxPainter(), DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(rpzCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					new CheckBoxPainter(), DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(shouldVerifyCol));
			
			// Register Display converters
			final IDisplayConverter timeToDateConverter = new LocalDateTimeDisplayConverter(DATE_FORMATTER);
			final PresentableDisplayConverter presentableDisplayConverter = new PresentableDisplayConverter();
			final IDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			final DefaultBigDecimalDisplayConverter puafTvaConverter = new DefaultBigDecimalDisplayConverter();
			puafTvaConverter.setMaximumFractionDigits(6);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBooleanDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(rpzCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(cantitateCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(transferatCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, puafTvaConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(puafTvaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vafTvaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vaTvaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(pvCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vvfTvaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vvTvaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, timeToDateConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocDataCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, timeToDateConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocDataRecCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, presentableDisplayConverter, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(accDocPartnerCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, presentableDisplayConverter, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(operatorCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(), DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(dataOpCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultBooleanDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(shouldVerifyCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, presentableDisplayConverter, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(verifiedCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultLongDisplayConverter(), DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(idCol));
			
			// CELL EDITOR CONFIG
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(rpzCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(puafTvaCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vafTvaCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vaTvaCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(pvCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vvfTvaCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(vvTvaCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(shouldVerifyCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameCol));
			
			// CELL EDITOR
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(rpzCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(shouldVerifyCol));
			
			// add a special style to highlight the modified cells
            final Style style = new Style();
            style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);
            configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                    style, DisplayMode.NORMAL, DataChangeLayer.DIRTY);
		}
		
		@Override
		public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
			if (doubleClickAction != null)
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.rowHeaderLeftClick(0), doubleClickAction);
			
			final CellLabelMouseEventMatcher transferatMouseEventMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(transferatCol));
			if (transferRowClick != null)
				uiBindingRegistry.registerDoubleClickBinding(transferatMouseEventMatcher, OperationsNatTable.this);
		}
	}
	
	private static class RowIdAccessor implements IRowIdAccessor<Operatiune>
	{
		@Override public Serializable getRowId(final Operatiune rowObject)
		{
			return rowObject.getId();
		}
	}
}
