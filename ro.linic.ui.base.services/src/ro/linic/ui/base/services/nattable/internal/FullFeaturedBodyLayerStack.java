package ro.linic.ui.base.services.nattable.internal;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.blink.BlinkLayer;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupExpandCollapseLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupReorderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.config.ColumnStyleChooserConfiguration;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.util.IClientAreaProvider;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

import ca.odell.glazedlists.EventList;
import ro.linic.ui.base.services.nattable.Column;

public class FullFeaturedBodyLayerStack<T> extends AbstractLayerTransform {
	private static Logger log = Logger.getLogger(FullFeaturedBodyLayerStack.class.getName());
	
	private final IColumnPropertyAccessor<T> columnPropertyAccessor;
    private ColumnReorderLayer columnReorderLayer;
    private ColumnGroupReorderLayer columnGroupReorderLayer;
    private ColumnHideShowLayer columnHideShowLayer;
    private ColumnGroupExpandCollapseLayer columnGroupExpandCollapseLayer;
    private final SelectionLayer selectionLayer;
    private final ViewportLayer viewportLayer;
    private BlinkLayer<T> blinkingLayer;
    private DataLayer bodyDataLayer;
    private FreezeLayer freezeLayer;
    private CompositeFreezeLayer compositeFreezeLayer;
    private ListDataProvider<T> bodyDataProvider;
    private GlazedListsEventLayer<T> glazedListsEventLayer;
    private final DataChangeLayer dataChangeLayer;
    
    public FullFeaturedBodyLayerStack(final Class<T> modelClass, final EventList<T> eventList,
            final IRowIdAccessor<T> rowIdAccessor, final List<Column> columns,
            final IConfigRegistry configRegistry, final ColumnGroupModel columnGroupModel) {
        this(modelClass, eventList, rowIdAccessor, columns, configRegistry,
                columnGroupModel, true);
    }

    public FullFeaturedBodyLayerStack(final Class<T> modelClass, final EventList<T> eventList,
            final IRowIdAccessor<T> rowIdAccessor, final List<Column> columns,
            final IConfigRegistry configRegistry, final ColumnGroupModel columnGroupModel,
            final boolean useDefaultConfiguration) {
    	final List<String> propertyNames = columns.stream().map(Column::property).collect(Collectors.toList());
        columnPropertyAccessor = new ReflectiveColumnPropertyAccessor<>(
                propertyNames);
        this.bodyDataProvider = new ListDataProvider<>(eventList,
                columnPropertyAccessor);
        this.bodyDataLayer = new DataLayer(this.bodyDataProvider);
        this.bodyDataLayer.setDefaultRowHeight(30);
        this.bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
        for (int i = 0; i < columns.size(); i++)
        	this.bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).size());
        // add a DataChangeLayer that temporarily tracks data changes without
        // updating the underlying data model
        // Note: we don't need the default configuration because the style is handled 
        // by our theme LinicThemeConfiguration and the CTRL+S and CTRL+D key bindings 
        // are global bindings in the file menu.
        this.dataChangeLayer = new DataChangeLayer(bodyDataLayer, new IdIndexKeyHandler<>(bodyDataProvider, rowIdAccessor, modelClass),
        		true, false, false);
        this.glazedListsEventLayer = new GlazedListsEventLayer<>(dataChangeLayer,
                eventList);
        this.blinkingLayer = new BlinkLayer<>(this.glazedListsEventLayer,
                this.bodyDataProvider, rowIdAccessor, columnPropertyAccessor,
                configRegistry);
        this.columnReorderLayer = new ColumnReorderLayer(this.blinkingLayer);
        this.columnGroupReorderLayer = new ColumnGroupReorderLayer(
                this.columnReorderLayer, columnGroupModel);
        this.columnHideShowLayer = new ColumnHideShowLayer(this.columnGroupReorderLayer);
        this.columnGroupExpandCollapseLayer = new ColumnGroupExpandCollapseLayer(
                this.columnHideShowLayer, columnGroupModel);
        this.selectionLayer = new SelectionLayer(this.columnGroupExpandCollapseLayer);
        this.viewportLayer = new ViewportLayer(this.selectionLayer);
        this.freezeLayer = new FreezeLayer(this.selectionLayer);
        this.compositeFreezeLayer = new CompositeFreezeLayer(this.freezeLayer,
                this.viewportLayer, this.selectionLayer);

        setUnderlyingLayer(this.compositeFreezeLayer);

        if (useDefaultConfiguration) {
            addConfiguration(new ColumnStyleChooserConfiguration(this,
                    this.selectionLayer));
        }

    }

    @Override
    public void setClientAreaProvider(final IClientAreaProvider clientAreaProvider) {
        super.setClientAreaProvider(clientAreaProvider);
    }

    public ColumnReorderLayer getColumnReorderLayer() {
        return this.columnReorderLayer;
    }

    public ColumnHideShowLayer getColumnHideShowLayer() {
        return this.columnHideShowLayer;
    }

    public SelectionLayer getSelectionLayer() {
        return this.selectionLayer;
    }

    public ViewportLayer getViewportLayer() {
        return this.viewportLayer;
    }

    public BlinkLayer<T> getBlinkingLayer() {
        return this.blinkingLayer;
    }

    public DataLayer getBodyDataLayer() {
        return this.bodyDataLayer;
    }

    public ListDataProvider<T> getBodyDataProvider() {
        return this.bodyDataProvider;
    }

    public ColumnGroupExpandCollapseLayer getColumnGroupExpandCollapseLayer() {
        return this.columnGroupExpandCollapseLayer;
    }

    public PropertyChangeListener getGlazedListEventsLayer() {
        return this.glazedListsEventLayer;
    }
    
    public DataChangeLayer getDataChangeLayer() {
		return dataChangeLayer;
	}
    
    public IColumnPropertyAccessor<T> getColumnPropertyAccessor() {
		return columnPropertyAccessor;
	}
}
