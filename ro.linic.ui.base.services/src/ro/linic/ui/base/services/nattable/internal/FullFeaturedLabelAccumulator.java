package ro.linic.ui.base.services.nattable.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelProvider;

public class FullFeaturedLabelAccumulator<T> implements IConfigLabelProvider {
    private ListDataProvider<T> dataProvider;
    private Set<String> allLabels;
    private Function<T, List<String>> rowLabels;

    /**
     * Create a {@link ColumnLabelAccumulator} which can be used in conjunction
     * with CSS styling, because the labels that are added to the cells are
     * predictable.
     *
     * @param dataProvider
     *            The {@link IDataProvider} that should be used to calculate
     *            which columns are added by this instance.
     */
    public FullFeaturedLabelAccumulator(final ListDataProvider<T> dataProvider, final Set<String> allLabels,
    		final Function<T, List<String>> rowLabels) {
        this.dataProvider = dataProvider;
        this.allLabels = allLabels;
        this.rowLabels = rowLabels;
    }

    @Override
	public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition) {
        configLabels.addLabel(ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columnPosition);
        final T rowObject = dataProvider.getRowObject(rowPosition);
        if (rowLabels != null)
        	rowLabels.apply(rowObject).forEach(configLabels::addLabelOnTop);
    }

    @Override
	public Collection<String> getProvidedLabels() {
    	final Collection<String> result = new HashSet<>();
    	for (int i = 0; i < this.dataProvider.getColumnCount(); i++) {
    		result.add(ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + i);
    	}
    	if (allLabels != null)
    		result.addAll(allLabels);
    	return result;
    }
}
