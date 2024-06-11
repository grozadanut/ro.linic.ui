package ro.linic.ui.base.services.nattable.components;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

import ro.linic.util.commons.ListUtils;

public class StringSetDisplayConverter extends DisplayConverter {
	@Override
    public Object canonicalToDisplayValue(final Object canonicalValue) {
        if (canonicalValue instanceof Set) {
            return ((Set) canonicalValue).stream().collect(Collectors.joining(LIST_SEPARATOR));
        }
        return ""; //$NON-NLS-1$
    }

    @Override
    public Object displayToCanonicalValue(final Object displayValue) {
        String displayString = (String) displayValue;
        displayString = displayString.trim();
        return ListUtils.toStream(new StringTokenizer(displayString, LIST_SEPARATOR).asIterator())
        		.map(String.class::cast)
        		.collect(Collectors.toSet());
    }
}
