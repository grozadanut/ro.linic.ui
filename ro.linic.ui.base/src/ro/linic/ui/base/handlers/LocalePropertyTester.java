 
package ro.linic.ui.base.handlers;

import static ro.linic.util.commons.StringUtils.isEmpty;

import java.util.Locale;

import org.eclipse.core.expressions.PropertyTester;

public class LocalePropertyTester extends PropertyTester  {
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (!property.equalsIgnoreCase("locale"))
			return false;
		
		final Locale locale = Locale.getDefault();
		if (expectedValue == null || isEmpty(expectedValue.toString()))
			return locale == null || isEmpty(locale.getLanguage()) || locale.getLanguage().equalsIgnoreCase("en");
			
		return expectedValue.toString().equalsIgnoreCase(locale.toString()) || 
				expectedValue.toString().contains(locale.getLanguage());
	}
}
