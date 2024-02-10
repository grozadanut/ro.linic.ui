package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes;

/**
 * The configuration to enable the edit mode for the grid and additional edit
 * configurations like converters and validators.
 */
public class FilterRowConfiguration extends AbstractRegistryConfiguration
{
	@Override
	public void configureRegistry(final IConfigRegistry configRegistry)
	{
		configRegistry.registerConfigAttribute(FilterRowConfigAttributes.TEXT_DELIMITER, "&");
	}
}