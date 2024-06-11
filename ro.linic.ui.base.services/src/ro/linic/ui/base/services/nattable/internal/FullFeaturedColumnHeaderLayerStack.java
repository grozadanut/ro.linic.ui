package ro.linic.ui.base.services.nattable.internal;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.util.IClientAreaProvider;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ro.linic.ui.base.services.nattable.Column;

public class FullFeaturedColumnHeaderLayerStack<T> extends AbstractLayerTransform {
    private final ColumnHeaderLayer columnHeaderLayer;
    private final ColumnGroupHeaderLayer columnGroupHeaderLayer;
    private final SortHeaderLayer<T> sortableColumnHeaderLayer;
    private final IDataProvider columnHeaderDataProvider;
    private final DefaultColumnHeaderDataLayer columnHeaderDataLayer;

    public FullFeaturedColumnHeaderLayerStack(final SortedList<T> sortedList,
            final FilterList<T> filterList, final List<Column> columns, final ILayer bodyLayer,
            final SelectionLayer selectionLayer, final ColumnGroupModel columnGroupModel,
            final IConfigRegistry configRegistry) {
    	final String[] propertyNames = columns.stream().map(Column::property).toArray(String[]::new);
        this.columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::name).toArray(String[]::new));

        this.columnHeaderDataLayer = new DefaultColumnHeaderDataLayer(
                this.columnHeaderDataProvider);

        this.columnHeaderLayer = new ColumnHeaderLayer(this.columnHeaderDataLayer,
                bodyLayer, selectionLayer);

        final ReflectiveColumnPropertyAccessor<T> columnPropertyAccessor = new ReflectiveColumnPropertyAccessor<>(
                propertyNames);
        this.sortableColumnHeaderLayer = new SortHeaderLayer<>(this.columnHeaderLayer,
                new GlazedListsSortModel<>(sortedList, columnPropertyAccessor,
                        configRegistry, this.columnHeaderDataLayer));

        this.columnGroupHeaderLayer = new ColumnGroupHeaderLayer(
                this.sortableColumnHeaderLayer, selectionLayer, columnGroupModel);

        final FilterRowDataLayer<T> filterRowDataLayer = new FilterRowDataLayer<>(
                new DefaultGlazedListsFilterStrategy<>(filterList,
                        columnPropertyAccessor, configRegistry),
                this.columnGroupHeaderLayer, this.columnHeaderDataProvider,
                configRegistry);
        filterRowDataLayer.setDefaultRowHeight(30);
        final FilterRowHeaderComposite<T> composite = new FilterRowHeaderComposite<>(this.columnGroupHeaderLayer, filterRowDataLayer);

        setUnderlyingLayer(composite);
    }

    @Override
    public void setClientAreaProvider(final IClientAreaProvider clientAreaProvider) {
        super.setClientAreaProvider(clientAreaProvider);
    }

    public ColumnGroupHeaderLayer getColumnGroupHeaderLayer() {
        return this.columnGroupHeaderLayer;
    }

    public ColumnHeaderLayer getColumnHeaderLayer() {
        return this.columnHeaderLayer;
    }

    public IDataProvider getColumnHeaderDataProvider() {
        return this.columnHeaderDataProvider;
    }

    public DefaultColumnHeaderDataLayer getColumnHeaderDataLayer() {
        return this.columnHeaderDataLayer;
    }
}
