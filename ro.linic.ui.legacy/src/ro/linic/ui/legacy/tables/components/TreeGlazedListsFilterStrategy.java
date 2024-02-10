package ro.linic.ui.legacy.tables.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TreeList;

public class TreeGlazedListsFilterStrategy<T> extends DefaultGlazedListsFilterStrategy<T>
{
	private TreeList.Format<T> treeFormat;
	
	public TreeGlazedListsFilterStrategy(final FilterList<T> filterList, final IColumnAccessor<T> columnAccessor,
			final IConfigRegistry configRegistry, final TreeList.Format<T> treeFormat)
	{
		super(filterList, columnAccessor, configRegistry);
		this.treeFormat = treeFormat;
	}

	@Override
	protected TextFilterator<T> getTextFilterator(final Integer columnIndex, final IDisplayConverter converter)
	{
        return new TreeTextFilterator(converter, columnIndex);
    }
	
	public class TreeTextFilterator implements TextFilterator<T>
	{
        private final IDisplayConverter converter;
        private final Integer columnIndex;

        public TreeTextFilterator(final IDisplayConverter converter, final Integer columnIndex)
        {
            this.converter = converter;
            this.columnIndex = columnIndex;
        }

        @Override
        public void getFilterStrings(final List<String> objectAsListOfStrings, final T rowObject)
        {
        	final List<T> path = new ArrayList<>();
        	treeFormat.getPath(path, rowObject);
        	path.forEach(obj ->
        	{
        		final Object cellData = TreeGlazedListsFilterStrategy.this.columnAccessor.getDataValue(obj, this.columnIndex);
        		Object displayValue = this.converter.canonicalToDisplayValue(cellData);
        		displayValue = (displayValue != null) ? displayValue : ""; //$NON-NLS-1$
        		objectAsListOfStrings.add(displayValue.toString());
        	});
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((this.columnIndex == null) ? 0 : this.columnIndex.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked")
			final TreeTextFilterator other = (TreeTextFilterator) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (this.columnIndex == null)
            {
                if (other.columnIndex != null)
                    return false;
            }
            else if (!this.columnIndex.equals(other.columnIndex))
                return false;
            return true;
        }

        private TreeGlazedListsFilterStrategy<T> getOuterType()
        {
            return TreeGlazedListsFilterStrategy.this;
        }
    }
}