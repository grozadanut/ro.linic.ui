package ro.linic.ui.legacy.tables;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
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
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.util.LocalDateUtils;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.tables.components.ExpandCollapseMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class CustomerDebtNatTable
{
//	private static final String SCADENTA_YELLOW_LABEL = "scadenta_yellow_label";
//	private static final String SCADENTA_RED_LABEL = "scadenta_red_label";
	
	private static ImmutableList<String> columns = ImmutableList.<String>builder()
			.add("Partener")
			.add("Gestiune")
			.add("Nr")
			.add("Data")
			.add("Total")
			.build();
	
	private static final Comparator<AccountingDocument> COMPARATOR = Comparator.nullsLast(Comparator.comparing(AccountingDocument::getDataDoc).reversed());

	private EventList<Object> sourceList;
	
	private NatTable table;
	private SelectionLayer selectionLayer;
	private TextMatcherEditor<Object> matcherEditor;
	private FilterList<Object> filteredData;
	private TreeList<Object> treeList;
	private RowSelectionProvider<Object> selectionProvider;

	public CustomerDebtNatTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
        final BodyLayerStack bodyLayerStack = new BodyLayerStack(new ArrayList<>(), new TwoLevelTreeFormat());

		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.toArray(new String[] {}));
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(new DataLayer(columnHeaderDataProvider), bodyLayerStack, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());

		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyLayerStack.bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayerStack, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, columnHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(bodyLayerStack, columnHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new ExpandCollapseMenuConfiguration(table));
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<Object>(selectionLayer, bodyLayerStack.bodyDataProvider, new IRowIdAccessor<Object>()
				{
					@Override public Serializable getRowId(final Object rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multiple selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		// define a TextMatcherEditor and set it to the FilterList
		matcherEditor = new TextMatcherEditor<>(new TextFilterator<Object>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Object element)
			{
				// add all values that should be included in filtering
				// Note:
				// if special converters are involved in rendering,
				// consider using them for adding the String values
				if (element instanceof AccountingDocument)
					baseList.add(safeString(((AccountingDocument) element).getRulajPartener(), RulajPartener::getName));
				else if (element instanceof RulajPartener)
					baseList.add(safeString(((RulajPartener) element).getName()));
			}
		});
		matcherEditor.setMode(TextMatcherEditor.CONTAINS);
		matcherEditor.setStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
		filteredData.setMatcherEditor(matcherEditor);
	}

	public CustomerDebtNatTable loadData(final ImmutableList<AccountingDocument> accDocs, final ImmutableList<RulajPartener> unpaidPartners)
	{
		try
		{
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.clear();
			this.sourceList.addAll(accDocs);
			this.sourceList.addAll(unpaidPartners);
			table.refresh();
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public EventList<Object> getSourceList()
	{
		return sourceList;
	}
	
	public TreeList<Object> getTreeList()
	{
		return treeList;
	}
	
	public CustomerDebtNatTable filter(final String searchText)
	{
        matcherEditor.setFilterText(searchText.split(SPACE));
        return this;
	}

	public NatTable getTable()
	{
		return table;
	}
	
	public RowSelectionProvider<Object> getSelectionProvider()
	{
		return selectionProvider;
	}
	
	public List<Object> selection()
	{
		return ((RowSelectionModel<Object>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	private class ColumnAccessor implements IColumnAccessor<AccountingDocument>
	{
		private final List<String> propertyNames;
		
		public ColumnAccessor(final List<String> propertyNames)
		{
	        this.propertyNames = propertyNames;
	    }
		
		@Override
		public Object getDataValue(final AccountingDocument rowObject, final int columnIndex)
		{
			switch (columnIndex)
			{
			case 0: //Partener
				return safeString(rowObject.getDoc());

			case 1: //Gestiunea
				return safeString(rowObject.getGestiune(), Gestiune::getImportName);
				
			case 2: //Nr
				return safeString(rowObject.getNrDoc());

			case 3: //Data
				return safeString(rowObject.getDataDoc(), LocalDateTime::toLocalDate, LocalDateUtils::displayLocalDate);

			case 4: //Total
				return displayBigDecimal(rowObject.totalUnlinked());

			default:
				return EMPTY_STRING;
			}
		}

		@Override
		public void setDataValue(final AccountingDocument rowObject, final int columnIndex, final Object newValue)
		{
		}

		@Override
		public int getColumnCount()
		{
			return propertyNames.size();
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
//			final Style yellowStyle = new Style();
//			yellowStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);
//			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowStyle, DisplayMode.NORMAL,
//					SCADENTA_YELLOW_LABEL);
//			
//			final Style redStyle = new Style();
//			redStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);
//			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, redStyle, DisplayMode.NORMAL,
//					SCADENTA_RED_LABEL);
			
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 0);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 1);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 2);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 3);
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 4);
		}
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
        private final IRowDataProvider<Object> bodyDataProvider;

        public BodyLayerStack(final List values, final TreeList.Format treeFormat)
        {
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
            sourceList = GlazedLists.eventList(values);
            final TransformedList<Object, Object> threadSafeList = GlazedLists.threadSafeList(sourceList);

            // wrap the SortedList with the TreeList
            filteredData = new FilterList<>(threadSafeList);
            treeList = new TreeList(filteredData, treeFormat, TreeList.NODES_START_EXPANDED);

            bodyDataProvider = new ListDataProvider(treeList, new TreeColumnAccessor(new ColumnAccessor(columns)));
            final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
            bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
    		bodyDataLayer.setDefaultColumnWidthByPosition(0, 170); //Partener
    		bodyDataLayer.setDefaultColumnWidthByPosition(1, 50); //Gestiunea
    		bodyDataLayer.setDefaultColumnWidthByPosition(2, 70); //Nr
    		bodyDataLayer.setDefaultColumnWidthByPosition(3, 100); //Data
    		bodyDataLayer.setDefaultColumnWidthByPosition(4, 100); //Total
    		
//            bodyDataLayer.setConfigLabelAccumulator(new AbstractOverrider()
//            {
//                @Override public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition)
//                {
//                    final Object rowObject = bodyDataProvider.getRowObject(rowPosition);
//                    if (rowObject instanceof AccountingDocument)
//                    {
//                    	final AccountingDocument accDoc = (AccountingDocument) rowObject;
//						if (equal(accDoc.getScadenta(), LocalDate.now()))
//                    		configLabels.addLabelOnTop(SCADENTA_YELLOW_LABEL);
//						else if (isBeforeExclusive(accDoc.getScadenta(), LocalDate.now()))
//							configLabels.addLabelOnTop(SCADENTA_RED_LABEL);
//                    }
//                    else if (rowObject instanceof RulajPartener)
//                    {
//                    	final RulajPartener rulaj = (RulajPartener) rowObject;
//                    	if (rulaj.getAccDocsStream()
//                    			.anyMatch(accDoc -> isBeforeExclusive(accDoc.getScadenta(), LocalDate.now())))
//                    		configLabels.addLabelOnTop(SCADENTA_RED_LABEL);
//                    	else if (rulaj.getAccDocsStream()
//                    			.anyMatch(accDoc -> equal(accDoc.getScadenta(), LocalDate.now())))
//                    		configLabels.addLabelOnTop(SCADENTA_YELLOW_LABEL);
//                    }
//                }
//            });
    		
            // layer for event handling of GlazedLists and PropertyChanges
            final GlazedListsEventLayer glazedListsEventLayer = new GlazedListsEventLayer(bodyDataLayer, treeList);

            final GlazedListTreeData treeData = new GlazedListTreeData(treeList);
            final ITreeRowModel treeRowModel = new GlazedListTreeRowModel<>(treeData);

            selectionLayer = new SelectionLayer(glazedListsEventLayer);

            final TreeLayer treeLayer = new TreeLayer(selectionLayer, treeRowModel);
            final ViewportLayer viewportLayer = new ViewportLayer(treeLayer);
            
            selectionProvider =  new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
            setUnderlyingLayer(viewportLayer);
        }
    }
    
    private class TreeColumnAccessor implements IColumnAccessor<Object>
    {
        private IColumnAccessor<AccountingDocument> cpa;

        public TreeColumnAccessor(final IColumnAccessor<AccountingDocument> cpa)
        {
            this.cpa = cpa;
        }

        @Override public Object getDataValue(final Object rowObject, final int columnIndex)
        {
            if (rowObject instanceof AccountingDocument)
                return cpa.getDataValue((AccountingDocument) rowObject, columnIndex);
            else if (rowObject instanceof RulajPartener)
            	if (columnIndex == 0)
            		return ((RulajPartener) rowObject).getName();
            	else if (columnIndex == 4)
            		return displayBigDecimal(((RulajPartener) rowObject).getDeIncasat());
            
            return null;
        }

        @Override public void setDataValue(final Object rowObject, final int columnIndex, final Object newValue)
        {
        }

        @Override public int getColumnCount()
        {
            return cpa.getColumnCount();
        }
    }
    
    private class TwoLevelTreeFormat implements TreeList.Format<Object>
    {
        @Override public void getPath(final List<Object> path, final Object element)
        {
            if (element instanceof AccountingDocument)
            {
                final AccountingDocument ele = (AccountingDocument) element;
                path.add(ele.getRulajPartener());
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
                	if (o1 instanceof AccountingDocument && o2 instanceof AccountingDocument)
                		return COMPARATOR.compare((AccountingDocument) o1, (AccountingDocument) o2);
                	
                    return ((Comparable) o1).compareTo(o2);
                }
            };
        }
    }
}
