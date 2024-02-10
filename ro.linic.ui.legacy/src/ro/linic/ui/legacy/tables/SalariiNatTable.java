package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.StringUtils.globalIsMatch;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeData;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
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
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.AbstractOverrider;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.TextDecorationEnum;
import org.eclipse.nebula.widgets.nattable.summaryrow.DefaultSummaryRowConfiguration;
import org.eclipse.nebula.widgets.nattable.summaryrow.FixedSummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jboss.weld.exceptions.IllegalArgumentException;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;
import ro.colibri.base.IPresentable;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.ExpandCollapseMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class SalariiNatTable
{
	public static final String TREE_ROW_LABEL_PREFIX = "TREE_"; //$NON-NLS-1$
	public static final String OP_LABEL_PREFIX = "OP_"; //$NON-NLS-1$
	
	private static final Comparator<AccountingDocument> docComparator = Comparator.comparing(AccountingDocument::getPartner);
	private static final int emptyID = -1;
	private static final int cantitateID = 1;
	private static final int uomID = 2;
	private static final int puaID = 3;
	private static final int restantID = 5;
	private static final Column emptyCol = new Column(emptyID, EMPTY_STRING, EMPTY_STRING, 0);
	
	private static final Column nameCol = new Column(0, Operatiune.NAME_FIELD, "Partener", 250);
	private static final Column cantitateCol = new Column(cantitateID, Operatiune.CANTITATE_FIELD, "Cant", 60);
	private static final Column uomCol = new Column(uomID, Operatiune.UM_FIELD, "UM", 50);
	private static final Column puaCol = new Column(puaID, Operatiune.PUA_fTVA_FIELD, "Pret", 60);
	private static final Column opTotalCol = new Column(4, Operatiune.VA_fTVA_FIELD, "Total", 100);
	private static final Column restantPlaceholderCol = new Column(emptyID, EMPTY_STRING, "Restant", 100);
	private static final Column dataOpCol = new Column(6, Operatiune.DATA_OP_FIELD, "Luna", 150);
	private static final Column operatorCol = new Column(7, Operatiune.OPERATOR_FIELD, "Operator", 120);
	
	private static final Column docPartnerCol = new Column(0, AccountingDocument.PARTNER_FIELD, "Partener", 250);
	// EMPTY COL
	// EMPTY COL
	// EMPTY COL
	private static final Column docTotalCol = new Column(4, AccountingDocument.TOTAL_FIELD, "Total", 100);
	private static final Column restantCol = new Column(restantID, EMPTY_STRING, "Restant", 100);
	private static final Column nrDocCol = new Column(6, AccountingDocument.NR_DOC_FIELD, "Luna", 150);
	private static final Column docOperatorCol = new Column(7, AccountingDocument.OPERATOR_FIELD, "Operator", 120);
	
	private static ImmutableList<Column> ALL_COLS = ImmutableList.<Column>builder()
			.add(nameCol)
			.add(cantitateCol)
			.add(uomCol)
			.add(puaCol)
			.add(opTotalCol)
			.add(restantPlaceholderCol)
			.add(dataOpCol)
			.add(operatorCol)
			.build();
	
	private static ImmutableList<Column> ACC_DOC_COLS = ImmutableList.<Column>builder()
			.add(docPartnerCol)
			.add(emptyCol)
			.add(emptyCol)
			.add(emptyCol)
			.add(docTotalCol)
			.add(restantCol)
			.add(nrDocCol)
			.add(docOperatorCol)
			.build();
	
	private TransformedList<Object, Object> sourceList;
	
	private NatTable table;
	private DataLayer bodyDataLayer;
	private IRowDataProvider<IPresentable> bodyDataProvider;
	private SelectionLayer selectionLayer;
	private DataChangeLayer dataChangeLayer;
	
	private Consumer<Operatiune> afterChange;
	private IMouseAction doubleClickAction;
	private Logger log;

	public SalariiNatTable(final Logger log)
	{
		this.log = log;
	}

	public void postConstruct(final Composite parent)
	{
		final ConfigRegistry configRegistry = new ConfigRegistry();
        final BodyLayerStack bodyLayerStack = new BodyLayerStack(new ArrayList<>(), new OperatiuneTwoLevelTreeFormat());

		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(ALL_COLS.stream().map(Column::getName).toArray(String[]::new));
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(columnHeaderDataProvider), bodyLayerStack, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());

		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayerStack, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, columnHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(bodyLayerStack, columnHeaderLayer, rowHeaderLayer, cornerLayer);

		// create a standalone summary row
        // for a grid this is the FixedGridSummaryRowLayer
        final FixedSummaryRowLayer summaryRowLayer = new FixedSummaryRowLayer(bodyDataLayer, gridLayer, configRegistry, false);
        summaryRowLayer.addConfiguration(new SummaryRowGridConfiguration());
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
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new ExpandCollapseMenuConfiguration(table));
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<IPresentable>(selectionLayer, bodyDataProvider, new RowIdAccessor(), true)); //multi selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
//		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public SalariiNatTable loadData(final ImmutableList<AccountingDocument> data)
	{
		try
		{
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.clear();
			this.sourceList.addAll(data.stream()
					.flatMap(doc -> doc.getOperatiuni().isEmpty() ? Stream.of(doc) : doc.getOperatiuni_Stream())
					.collect(toImmutableList()));
			table.refresh();
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public SalariiNatTable add(final Operatiune op)
	{
		try
		{
			this.sourceList.getReadWriteLock().writeLock().lock();
			sourceList.add(op);
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
	
	public ImmutableList<AccountingDocument> sourceAccDocs()
	{
		return getSourceList().stream()
				.map(obj -> 
				{
					if (obj instanceof AccountingDocument)
						return (AccountingDocument) obj;
					else if (obj instanceof Operatiune)
						return ((Operatiune) obj).getAccDoc();
					return null;
				})
				.filter(Objects::nonNull)
				.distinct()
				.collect(toImmutableList());
	}

	public void afterChange(final Consumer<Operatiune> afterChange)
	{
		this.afterChange = afterChange;
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
	
	public ImmutableList<AccountingDocument> selectedAccDocs()
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
				.distinct()
				.collect(toImmutableList());
	}
	
	public ImmutableList<Operatiune> selectedOps()
	{
		return selection().stream()
				.map(sel -> 
				{
					if (sel instanceof Operatiune)
						return (Operatiune) sel;
					return null;
				})
				.filter(Objects::nonNull)
				.distinct()
				.collect(toImmutableList());
	}
	
	public List<Object> selection()
	{
		return ((RowSelectionModel<Object>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
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

            // wrap the SortedList with the TreeList
            final TreeList treeList = new TreeList(sourceList, treeFormat, TreeList.nodesStartExpanded());

            bodyDataProvider = new ListDataProvider(treeList, new TreeColumnAccessor(
            		new OpColumnAccessor(ALL_COLS.stream().map(Column::getProperty).collect(toImmutableList())),
            		new AccDocColumnAccessor(ACC_DOC_COLS.stream().map(Column::getProperty).collect(toImmutableList()))));
            bodyDataLayer = new DataLayer(bodyDataProvider);
            bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
            for (int i = 0; i < ALL_COLS.size(); i++)
    			bodyDataLayer.setDefaultColumnWidthByPosition(i, ALL_COLS.get(i).getSize());
    		
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
            final IdIndexKeyHandler<IPresentable> keyHandler = new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor());
    		try
			{
				final Field declaredField = IdIndexKeyHandler.class.getDeclaredField("clazz");
				final boolean accessible = declaredField.isAccessible();
				declaredField.setAccessible(true);
				declaredField.set(keyHandler, Operatiune.class);
				declaredField.setAccessible(accessible);
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
			{
				log.error(e);
			}
            dataChangeLayer = new DataChangeLayer(bodyDataLayer, keyHandler, false);
            final GlazedListsEventLayer glazedListsEventLayer = new GlazedListsEventLayer(dataChangeLayer, treeList);

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
			case restantID:
				return rowObject.totalUnlinked();
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
			switch (ALL_COLS.get(columnIndex).getIndex())
			{
			case cantitateID:
				return globalIsMatch(rowObject.getBarcode(), Product.TICHETE_MASA_BARCODE, TextFilterMethod.EQUALS) ? rowObject.getCantitate() : EMPTY_STRING;
			case uomID:
				return globalIsMatch(rowObject.getBarcode(), Product.TICHETE_MASA_BARCODE, TextFilterMethod.EQUALS) ? rowObject.getUom() : EMPTY_STRING;
			case emptyID:
				return EMPTY_STRING;

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
		
		@Override
		public void setDataValue(final Operatiune rowObj, final int columnIndex, final Object newValue)
		{
			switch (ALL_COLS.get(columnIndex).getIndex())
			{
			case cantitateID:
				if (globalIsMatch(rowObj.getBarcode(), Product.TICHETE_MASA_BARCODE, TextFilterMethod.EQUALS))
				{
					rowObj.setCantitate((BigDecimal) newValue);
					Operatiune.updateAmounts(rowObj, BigDecimal.ZERO, BigDecimal.ONE);
				}
				break;
			case puaID:
				super.setDataValue(rowObj, columnIndex, newValue);
				Operatiune.updateAmounts(rowObj, BigDecimal.ZERO, BigDecimal.ONE);
				break;
			default:
				super.setDataValue(rowObj, columnIndex, newValue);
			}
			
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
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(nameCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(uomCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(operatorCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docPartnerCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL,
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docOperatorCol));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(cantitateCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(puaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(opTotalCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					OP_LABEL_PREFIX + ALL_COLS.indexOf(dataOpCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(nrDocCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docTotalCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(restantCol));
		
			final Style linkStyle = new Style();
	        linkStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
	        linkStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);
	        final Style linkSelectStyle = new Style();
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);

			// Register Display converters
			final IDisplayConverter bigDecimalConv = new DefaultBigDecimalDisplayConverter();
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(), DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + ALL_COLS.indexOf(dataOpCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + ALL_COLS.indexOf(cantitateCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + ALL_COLS.indexOf(puaCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + ALL_COLS.indexOf(opTotalCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					OP_LABEL_PREFIX + ALL_COLS.indexOf(operatorCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docPartnerCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docTotalCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docOperatorCol));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					TREE_ROW_LABEL_PREFIX + ACC_DOC_COLS.indexOf(restantCol));
			
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, OP_LABEL_PREFIX + ALL_COLS.indexOf(nameCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, OP_LABEL_PREFIX + ALL_COLS.indexOf(cantitateCol));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, OP_LABEL_PREFIX + ALL_COLS.indexOf(puaCol));
		}
		
		@Override
        public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
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
            else if (rowObject instanceof AccountingDocument)
            	cpaDoc.setDataValue((AccountingDocument) rowObject, columnIndex, newValue);
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
                		return Long.compare(((Operatiune) o1).getId(), ((Operatiune) o2).getId());
                	else if (o1 instanceof AccountingDocument && o2 instanceof AccountingDocument)
                		return docComparator.compare((AccountingDocument) o1, (AccountingDocument) o2);
                	
                    return ((Comparable) o1).compareTo(o2);
                }
            };
        }
    }
    
    private class SummaryRowGridConfiguration extends DefaultSummaryRowConfiguration
	{
		public SummaryRowGridConfiguration()
		{
			this.summaryRowFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}

		@Override public void addSummaryProviderConfig(final IConfigRegistry configRegistry)
		{
			// Labels are applied to the summary row and cells by default to
			// make configuration easier.
			// See the Javadoc for the SummaryRowLayer
			configRegistry.registerConfigAttribute(
                    SummaryRowConfigAttributes.SUMMARY_PROVIDER,
                    new TreeSelectionSummationSummary(bodyDataProvider, selectionLayer), DisplayMode.NORMAL,
                    SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + ACC_DOC_COLS.indexOf(docTotalCol));
			
			configRegistry.registerConfigAttribute(
                    SummaryRowConfigAttributes.SUMMARY_PROVIDER,
                    new TreeSelectionSummationSummary(bodyDataProvider, selectionLayer), DisplayMode.NORMAL,
                    SummaryRowLayer.DEFAULT_SUMMARY_COLUMN_CONFIG_LABEL_PREFIX + ACC_DOC_COLS.indexOf(restantCol));
		}
	}
    
    private static class TreeSelectionSummationSummary implements ISummaryProvider
	{
    	private final IRowDataProvider dataProvider;
    	private final SelectionLayer selectionLayer;
		
		public TreeSelectionSummationSummary(final IRowDataProvider dataProvider, final SelectionLayer selectionLayer)
		{
			this.dataProvider = dataProvider;
			this.selectionLayer = selectionLayer;
		}
		
		@Override public Object summarize(final int columnIndex)
		{
			final boolean hasRowSelection = !selectionLayer.getSelectionModel().isEmpty();
			final int rowCount = this.dataProvider.getRowCount();
			double summaryValue = 0;

			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
			{
				if (hasRowSelection && !selectionLayer.isRowPositionSelected(rowIndex))
					continue;

				final Object rowObject = dataProvider.getRowObject(rowIndex);
				if (rowObject instanceof AccountingDocument)
				{
					final Object dataValue = this.dataProvider.getDataValue(columnIndex, rowIndex);
					if (dataValue instanceof Number)
						summaryValue += ((Number) dataValue).doubleValue();
				}
			}

			return summaryValue;
		}
	}
    
    private static class RowIdAccessor implements IRowIdAccessor<IPresentable>
	{
		@Override public Serializable getRowId(final IPresentable rowObject)
		{
			return rowObject.hashCode();
		}
	}
}