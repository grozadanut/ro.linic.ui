package ro.linic.ui.base.services.nattable.internal;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;

import ro.flexbiz.util.commons.HeterogeneousDataComparator;

public class CustomGeneralConfiguration extends AbstractRegistryConfiguration {
	@Override
	public void configureRegistry(final IConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(
                FilterRowConfigAttributes.FILTER_COMPARATOR,
                HeterogeneousDataComparator.INSTANCE);
		
		configRegistry.registerConfigAttribute(
                SortConfigAttributes.SORT_COMPARATOR,
                HeterogeneousDataComparator.INSTANCE);
	}
}