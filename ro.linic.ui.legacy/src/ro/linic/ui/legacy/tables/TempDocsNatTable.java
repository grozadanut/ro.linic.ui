package ro.linic.ui.legacy.tables;

import static ro.colibri.util.NumberUtils.add;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.AbstractOverrider;
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
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
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
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.TempDocument;
import ro.colibri.entities.user.User;
import ro.colibri.util.LocalDateUtils;
import ro.linic.ui.legacy.tables.components.ExpandCollapseMenuConfiguration;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;

public class TempDocsNatTable
{
	private static final String GREEN_LABEL = "green_label";
	
	private static ImmutableList<String> columns = ImmutableList.<String>builder()
			.add("Operator")
			.add("Tip")
			.add("Data")
			.add("Total")
			.build();
	
	private static final Comparator<TempDocument> COMPARATOR = Comparator.comparing(TempDocument::getDataDoc, Comparator.nullsFirst(Comparator.naturalOrder()));

	private EventList<Object> sourceList;
	
	private NatTable table;
	private SelectionLayer selectionLayer;
	private TextMatcherEditor<Object> matcherEditor;
	private FilterList<Object> filteredData;
	private RowSelectionProvider<Object> selectionProvider;
	
	private Map<User, BigDecimal> userTotals = new HashMap<>();

	public TempDocsNatTable()
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
				if (element instanceof TempDocument)
					baseList.add(safeString(((TempDocument) element).getOperator(), User::displayName));
				else if (element instanceof User)
					baseList.add(safeString(((User) element).displayName()));
			}
		});
		matcherEditor.setMode(TextMatcherEditor.CONTAINS);
		matcherEditor.setStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
		filteredData.setMatcherEditor(matcherEditor);
	}

	public TempDocsNatTable loadData(final ImmutableList<TempDocument> tempDocs)
	{
		try
		{
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.clear();
			this.sourceList.addAll(tempDocs);
			userTotals.clear();
			tempDocs.stream().forEach(tDoc ->  userTotals.put(tDoc.getOperator(),
					add(userTotals.getOrDefault(tDoc.getOperator(), BigDecimal.ZERO), tDoc.getTotal())));
			table.refresh();
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public TempDocsNatTable filter(final String searchText)
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
	
	private class ColumnAccessor implements IColumnAccessor<TempDocument>
	{
		private final List<String> propertyNames;
		
		public ColumnAccessor(final List<String> propertyNames)
		{
	        this.propertyNames = propertyNames;
	    }
		
		@Override
		public Object getDataValue(final TempDocument rowObject, final int columnIndex)
		{
			switch (columnIndex)
			{
			case 0: //Operator
				return safeString(rowObject.getPartner(), Partner::displayName);

			case 1: //TipDoc
				return safeString(rowObject.getTipDoc(), TipDoc::shortName);
				
			case 2: //Data
				return safeString(rowObject.getDataDoc(), LocalDateUtils::displayLocalDateTime);

			case 3: //Total
				return displayBigDecimal(rowObject.getTotal());

			default:
				return EMPTY_STRING;
			}
		}

		@Override
		public void setDataValue(final TempDocument rowObject, final int columnIndex, final Object newValue)
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
			final Style greenStyle = new Style();
			greenStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_GREEN);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenStyle, DisplayMode.NORMAL,
					GREEN_LABEL);
			
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 0);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 1);
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 2);
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 3);
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
            final TreeList treeList = new TreeList(filteredData, treeFormat, TreeList.NODES_START_EXPANDED);

            bodyDataProvider = new ListDataProvider(treeList, new TreeColumnAccessor(new ColumnAccessor(columns)));
            final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
            bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
    		bodyDataLayer.setDefaultColumnWidthByPosition(0, 120); //Operator
    		bodyDataLayer.setDefaultColumnWidthByPosition(1, 30); //TipDoc
    		bodyDataLayer.setDefaultColumnWidthByPosition(2, 140); //Data
    		bodyDataLayer.setDefaultColumnWidthByPosition(3, 70); //Total
    		
    		// only apply labels in case it is a real row object and not a tree
            // structure object
            bodyDataLayer.setConfigLabelAccumulator(new AbstractOverrider()
            {
                @Override public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition)
                {
                    final Object rowObject = bodyDataProvider.getRowObject(rowPosition);
                    if (rowObject instanceof TempDocument)
                    {
                    	final TempDocument tempDoc = (TempDocument) rowObject;
                    	if (!tempDoc.incasareCreata())
                    		configLabels.addLabelOnTop(GREEN_LABEL);
                    }
                }
            });
    		
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
        private IColumnAccessor<TempDocument> cpa;

        public TreeColumnAccessor(final IColumnAccessor<TempDocument> cpa)
        {
            this.cpa = cpa;
        }

        @Override public Object getDataValue(final Object rowObject, final int columnIndex)
        {
            if (rowObject instanceof TempDocument)
                return cpa.getDataValue((TempDocument) rowObject, columnIndex);
            else if (rowObject instanceof User)
            {
            	if (columnIndex == 0) //Operator
            		return ((User) rowObject).displayName();
            	else if (columnIndex == 3) //Total
            		return displayBigDecimal(userTotals.get(rowObject));
            }
            
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
            if (element instanceof TempDocument)
            {
                final TempDocument ele = (TempDocument) element;
                path.add(ele.getOperator());
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
                	if (o1 instanceof TempDocument && o2 instanceof TempDocument)
                		return COMPARATOR.compare((TempDocument) o1, (TempDocument) o2);
                	
                    return ((Comparable) o1).compareTo(o2);
                }
            };
        }
    }
}