package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.DATE_FORMATTER;
import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.NumberUtils.subtract;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeData;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
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
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.AbstractOverrider;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.cell.AutomaticRowHeightTextPainter;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.TextDecorationEnum;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;
import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.Verificat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.LocalDateUtils;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.ExpandCollapseMenuConfiguration;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;
import ro.linic.ui.legacy.tables.components.TreeGlazedListsFilterStrategy;

public class TreeOperationsNatTable implements IMouseAction
{
	public static final String TREE_ROW_LABEL_PREFIX = "TREE_"; //$NON-NLS-1$
	public static final String OP_LABEL_PREFIX = "OP_"; //$NON-NLS-1$
	
	public static final Comparator<AccountingDocument> ACC_DOC_COMPARATOR =
			Comparator.comparing(AccountingDocument::getTransportType, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(AccountingDocument::getTransportDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(AccountingDocument::getDataDoc);
	public static final Comparator<Operatiune> OP_COMPARATOR =
			Comparator.<Operatiune, String>comparing(op -> op.getGestiune().getImportName(), Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(Operatiune::getId);
	private static final int emptyID = -1;
	private static final int categorieID = 1;
	private static final int barcodeID = 2;
	private static final int verificaID = 10;
	private static final int accDocID = 11;
	private static final int neverificatID = 14;
	
	private static final Column emptyColumn = new Column(emptyID, EMPTY_STRING, EMPTY_STRING, 0);
	
	private static final Column gestColumn = new Column(0, Operatiune.GESTIUNE_FIELD + "." + Gestiune.IMPORT_NAME_FIELD, "L", 50);
	private static final Column categorieColumn = new Column(categorieID, Operatiune.CATEGORIE_FIELD, "Categorie", 90);
	private static final Column barcodeColumn = new Column(barcodeID, Operatiune.BARCODE_FIELD, "COD", 70);
	private static final Column nameColumn = new Column(3, Operatiune.NAME_FIELD, "DENUMIRE", 400);
	private static final Column uomColumn = new Column(4, Operatiune.UM_FIELD, "UM", 50);
	private static final Column pretColumn = new Column(5, Operatiune.PRET_UNITAR_FIELD, "PU", 70);
	private static final Column cantColumn = new Column(6, Operatiune.CANTITATE_FIELD, "CANT", 70);
	private static final Column vvfTvaColumn = new Column(7, Operatiune.VV_fTVA_FIELD, "ValFaraTVA", 80);
	private static final Column vvTvaColumn = new Column(8, Operatiune.VV_TVA_FIELD, "ValTVA", 70);
	
	private static final Column tipOpColumn = new Column(9, Operatiune.TIP_OP_FIELD, "Intr/Ies", 70);
	private static final Column verificaColumn = new Column(verificaID, EMPTY_STRING, "Verifica", 70);
	
	private static final Column accDocColumn = new Column(accDocID, EMPTY_STRING, EMPTY_STRING, 600);
	private static final Column verificatColumn = new Column(12, Operatiune.VERIFICAT_FIELD, "Ridicat", 70);
	private static final Column totalColumn = new Column(13, "total", "Total", 80);
	private static final Column neverificatColumn = new Column(neverificatID, EMPTY_STRING, "RAMAS", 70);
	
	private static ImmutableList<Column> VANZARI_COLS = ImmutableList.<Column>builder()
			.add(gestColumn)
			.add(categorieColumn)
			.add(barcodeColumn)
			.add(nameColumn)
			.add(uomColumn)
			.add(pretColumn)
			.add(cantColumn)
			.add(vvfTvaColumn)
			.add(vvTvaColumn)
			.add(totalColumn)
			.build();
	
	private static ImmutableList<Column> VERIFY_COLS = ImmutableList.<Column>builder()
			.add(tipOpColumn)
			.add(categorieColumn)
			.add(barcodeColumn)
			.add(nameColumn.withSize(accDocColumn.getSize()))
			.add(uomColumn)
			.add(pretColumn)
			.add(neverificatColumn)
			.add(gestColumn)
			.add(verificaColumn)
			.build();
	
	private static ImmutableList<Column> ACC_DOC_COLS = ImmutableList.<Column>builder()
			.add(emptyColumn)
			.add(categorieColumn)
			.add(barcodeColumn)
			.add(accDocColumn)
			.add(emptyColumn)
			.add(emptyColumn)
			.add(emptyColumn)
			.add(emptyColumn)
			.add(verificaColumn)
			.build();
	
	public enum SourceLoc
	{
		VANZARI, VERIFY_OPERATIONS;
	}

	private TransformedList<Object, Object> sourceList;
	private FilterList<Object> filteredData;
	
	private ImmutableList<Column> columns;
	private NatTable table;
	private IRowDataProvider<Object> bodyDataProvider;
	private SelectionLayer selectionLayer;
	private SourceLoc source;
	private TreeColumnAccessor columnAccessor;
	
	private Consumer<Object> verifyCons;
	private IMouseAction doubleClickAction;

	public TreeOperationsNatTable(final SourceLoc source)
	{
		this.source = source;
		switch (source)
		{
		case VANZARI:
			columns = VANZARI_COLS;
			break;
			
		case VERIFY_OPERATIONS:
			columns = VERIFY_COLS;
			break;

		default:
			throw new IllegalArgumentException("Case not implemented: "+source);
		}
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final OperatiuneTwoLevelTreeFormat treeFormat = new OperatiuneTwoLevelTreeFormat();
		final BodyLayerStack bodyLayerStack = new BodyLayerStack(new ArrayList<>(), treeFormat);
		
		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(columnHeaderDataProvider), bodyLayerStack, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());

		final FilterRowHeaderComposite<Object> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
				new TreeGlazedListsFilterStrategy<>(filteredData, columnAccessor, configRegistry, treeFormat),
				columnHeaderLayer, columnHeaderDataProvider, configRegistry);
		
		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayerStack, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, filterRowHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(bodyLayerStack, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new ExpandCollapseMenuConfiguration(table));
		table.addConfiguration(new FilterRowConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<Object>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Object>()
				{
					@Override public Serializable getRowId(final Object rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
//		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	@Override
    public void run(final NatTable natTable, final MouseEvent event)
	{
		// Verifica clicked
		final NatEventData eventData = NatEventData.createInstanceFromEvent(event);
        final int rowIndex = natTable.getRowIndexByPosition(eventData.getRowPosition());

        final Object rowObject = bodyDataProvider.getRowObject(rowIndex);
        if (verifyCons != null)
        	verifyCons.accept(rowObject);
    }

	public TreeOperationsNatTable loadData(final ImmutableList<Operatiune> data)
	{
		try
		{
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.clear();
			this.sourceList.addAll(data);
			table.refresh();
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public TransformedList<Object, Object> getSourceList()
	{
		return sourceList;
	}
	
	public void setVerifyConsumer(final Consumer<Object> verifyCons)
	{
		this.verifyCons = verifyCons;
	}

	public NatTable getTable()
	{
		return table;
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}
	
	public Optional<AccountingDocument> selectedAccDoc()
	{
		return selection().stream()
				.map(sel -> 
				{
					if (sel instanceof AccountingDocument)
						return (AccountingDocument) sel;
					else if (sel instanceof Operatiune)
						return ((Operatiune) sel).getAccDoc();
					return null;
				})
				.filter(Objects::nonNull)
				.findFirst();
	}
	
	public List<Object> selection()
	{
		return ((RowSelectionModel<Object>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	/**
     * Always encapsulate the body layer stack in an AbstractLayerTransform to
     * ensure that the index transformations are performed in later commands.
     *
     * @param 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class BodyLayerStack extends AbstractLayerTransform
    {
        public BodyLayerStack(final List values, final TreeList.Format treeFormat)
        {
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
            final EventList eventList = GlazedLists.eventList(values);
            sourceList = GlazedLists.threadSafeList(eventList);
            filteredData = new FilterList<>(sourceList);

            // wrap the SortedList with the TreeList
            final TreeList treeList = new TreeList(filteredData, treeFormat, TreeList.nodesStartExpanded());

            columnAccessor = new TreeColumnAccessor(
            		new OpColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList())),
            		new AccDocColumnAccessor(ACC_DOC_COLS.stream().map(Column::getProperty).collect(toImmutableList())));
            bodyDataProvider = new ListDataProvider(treeList, columnAccessor);
            final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
            bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
            for (int i = 0; i < columns.size(); i++)
    			bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
    		
            // only apply labels in case it is a real row object and not a tree
            // structure object
            bodyDataLayer.setConfigLabelAccumulator(new AbstractOverrider()
            {
                @Override public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition)
                {
                    final Object rowObject = bodyDataProvider.getRowObject(rowPosition);
                    if (rowObject instanceof Operatiune)
                    	configLabels.addLabel(OP_LABEL_PREFIX + columnPosition);
                    else
                    	configLabels.addLabel(TREE_ROW_LABEL_PREFIX + columnPosition);
                }

                @Override public Collection getProvidedLabels()
                {
                    // make the custom labels available for the CSS engine
                    final Collection result = super.getProvidedLabels();
                    result.add(TREE_ROW_LABEL_PREFIX);
                    result.add(OP_LABEL_PREFIX);
                    return result;
                }
            });

            // layer for event handling of GlazedLists and PropertyChanges
            final GlazedListsEventLayer glazedListsEventLayer = new GlazedListsEventLayer(bodyDataLayer, treeList);

            final GlazedListTreeData treeData = new GlazedListTreeData(treeList);
            final ITreeRowModel treeRowModel = new GlazedListTreeRowModel<>(treeData);

            selectionLayer = new SelectionLayer(glazedListsEventLayer);

            final TreeLayer treeLayer = new TreeLayer(selectionLayer, treeRowModel);
            final ViewportLayer viewportLayer = new ViewportLayer(treeLayer);
            
            setUnderlyingLayer(viewportLayer);
        }
    }
	
	private static class AccDocColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<AccountingDocument>
	{
		public AccDocColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final AccountingDocument rowObject, final int columnIndex)
		{
			switch (ACC_DOC_COLS.get(columnIndex).getIndex())
			{
			case emptyID:
				return EMPTY_STRING;
			case categorieID:
				return safeString(rowObject.getMasina(), Masina::getMarca);
			case barcodeID:
				return safeString(rowObject.getMasina(), Masina::getNr);
			case accDocID:
				if (TipDoc.VANZARE.equals(rowObject.getTipDoc()))
					return MessageFormat.format("{1}{0}{2} - {3}{0}{4} - {5}", NEWLINE,
							rowObject.transportDateReadable(),
							safeString(rowObject.getPartner(), Partner::getName),
							safeString(rowObject.phone().orElse(rowObject.getPartner().getPhone())),
							rowObject.address().orElseGet(() -> rowObject.getPartner().deliveryAddress().orElse(safeString(rowObject.getPartner().getAddress(), Address::displayName))),
							safeString(rowObject.getIndicatii()));

				// INTRARI, transferuri
				return MessageFormat.format("{0} - {1} {2} din {3}", safeString(rowObject.getPartner(), Partner::getName),
						rowObject.getDoc(), rowObject.getNrDoc(),
						safeString(rowObject.getTransportDateTime(), LocalDateUtils::displayLocalDateTime, displayLocalDate(rowObject.getDataDoc_toLocalDate())));
			case verificaID:
				return "Tot bonul";
				
			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
	
	private class OpColumnAccessor extends ExtendedReflectiveColumnPropertyAccessor<Operatiune>
	{
		public OpColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final Operatiune rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case verificaID:
				return "Verifica";
			case neverificatID:
				return subtract(rowObject.getCantitate(),
						Optional.ofNullable(rowObject.getVerificat())
						.map(Verificat::getVerificatCantitate)
						.orElse(null));

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
			final Style yellowBgStyle = new Style();
			yellowBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			yellowBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			Style greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(tipOpColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(categorieColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(barcodeColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(uomColumn));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, TREE_ROW_LABEL_PREFIX + columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, new AutomaticRowHeightTextPainter(), DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(nameColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			final Style blueBgStyle = new Style();
			blueBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			blueBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			blueBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, blueBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(pretColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(cantColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenBgStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(neverificatColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(vvfTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(vvTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, OP_LABEL_PREFIX + columns.indexOf(totalColumn));
		
			final Style linkStyle = new Style();
	        linkStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
	        linkStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);
	        final Style linkSelectStyle = new Style();
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);

	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkStyle, DisplayMode.NORMAL,
	        		OP_LABEL_PREFIX + columns.indexOf(verificaColumn));
	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkSelectStyle, DisplayMode.SELECT,
	        		OP_LABEL_PREFIX + columns.indexOf(verificaColumn));
	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkStyle, DisplayMode.NORMAL,
	        		TREE_ROW_LABEL_PREFIX + columns.indexOf(verificaColumn));
	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkSelectStyle, DisplayMode.SELECT,
	        		TREE_ROW_LABEL_PREFIX + columns.indexOf(verificaColumn));
			
			// Register Display converters
			final IDisplayConverter timeToDateConverter = new LocalDateTimeDisplayConverter(DATE_FORMATTER);
			final IDisplayConverter bigDecimalConv = new DefaultBigDecimalDisplayConverter();
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(pretColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(cantColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(vvfTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(vvTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(totalColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(verificatColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + columns.indexOf(neverificatColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, timeToDateConverter, DisplayMode.NORMAL, 
					TREE_ROW_LABEL_PREFIX + columns.indexOf(barcodeColumn)); //accDoc date
		}
		
		@Override
        public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
            // Match a mouse event on the body, when the left button is clicked
            // and the custom cell label is present
            final CellLabelMouseEventMatcher opMouseEventMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON,
            		OP_LABEL_PREFIX + columns.indexOf(verificaColumn));
            final CellLabelMouseEventMatcher opMouseHoverMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, 0,
            		OP_LABEL_PREFIX + columns.indexOf(verificaColumn));
            
            final CellLabelMouseEventMatcher docMouseEventMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON,
            		TREE_ROW_LABEL_PREFIX + columns.indexOf(verificaColumn));
            final CellLabelMouseEventMatcher docMouseHoverMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, 0,
            		TREE_ROW_LABEL_PREFIX + columns.indexOf(verificaColumn));

            // Inform the button painter of the click.
            uiBindingRegistry.registerFirstSingleClickBinding(opMouseEventMatcher, TreeOperationsNatTable.this);
            // show hand cursor, which is usually used for links
            uiBindingRegistry.registerFirstMouseMoveBinding(opMouseHoverMatcher, (natTable, event) -> {
                natTable.setCursor(natTable.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            });
            
            // Inform the button painter of the click.
            uiBindingRegistry.registerFirstSingleClickBinding(docMouseEventMatcher, TreeOperationsNatTable.this);
            // show hand cursor, which is usually used for links
            uiBindingRegistry.registerFirstMouseMoveBinding(docMouseHoverMatcher, (natTable, event) -> {
            	natTable.setCursor(natTable.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            });

            if (doubleClickAction != null)
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.rowHeaderLeftClick(0), doubleClickAction);
        }
	}
	
    private class TreeColumnAccessor implements IColumnAccessor<Object>
    {
        private IColumnAccessor<Operatiune> cpa;
        private IColumnAccessor<AccountingDocument> cpaDoc;

        public TreeColumnAccessor(final IColumnAccessor<Operatiune> cpa, final IColumnAccessor<AccountingDocument> cpaDoc)
        {
            this.cpa = cpa;
            this.cpaDoc = cpaDoc;
        }

        @Override public Object getDataValue(final Object rowObject, final int columnIndex)
        {
            if (rowObject instanceof Operatiune)
                return cpa.getDataValue((Operatiune) rowObject, columnIndex);
            else if (rowObject instanceof AccountingDocument)
                return cpaDoc.getDataValue((AccountingDocument) rowObject, columnIndex);
            else if (columnIndex == 0)
                return rowObject;
            
            return null;
        }

        @Override public void setDataValue(final Object rowObject, final int columnIndex, final Object newValue)
        {
            if (rowObject instanceof Operatiune)
                cpa.setDataValue((Operatiune) rowObject, columnIndex, newValue);
        }

        @Override public int getColumnCount()
        {
            return cpa.getColumnCount();
        }
    }
    
    private class OperatiuneTwoLevelTreeFormat implements TreeList.Format<Object>
    {
        @Override public void getPath(final List<Object> path, final Object element)
        {
            if (element instanceof Operatiune)
            {
                final Operatiune ele = (Operatiune) element;
                if (SourceLoc.VANZARI.equals(source))
                	path.add(ele.getGestiune().getImportName());
                else if (SourceLoc.VERIFY_OPERATIONS.equals(source))
                	path.add(ele.getAccDoc());
            }
            path.add(element);
        }

        @Override public boolean allowsChildren(final Object element)
        {
            return true;
        }

        @Override public Comparator<Object> getComparator(final int depth)
        {
            return new Comparator<Object>()
            {
                @SuppressWarnings({ "rawtypes", "unchecked" })
				@Override public int compare(final Object o1, final Object o2)
                {
                	if (o1 instanceof Operatiune && o2 instanceof Operatiune)
                		return OP_COMPARATOR.compare((Operatiune) o1, (Operatiune) o2);
                	else if (o1 instanceof AccountingDocument && o2 instanceof AccountingDocument)
                		return ACC_DOC_COMPARATOR.compare((AccountingDocument) o1, (AccountingDocument) o2);
                	
                    return ((Comparable) o1).compareTo(o2);
                }
            };
        }
    }
}
