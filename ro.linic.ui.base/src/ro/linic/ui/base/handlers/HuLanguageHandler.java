 
package ro.linic.ui.base.handlers;

import java.util.Locale;

import org.eclipse.e4.core.di.annotations.Execute;

import ro.linic.ui.base.services.LocaleService;

public class HuLanguageHandler {
	@Execute
	public void execute(final LocaleService localeService) {
		localeService.changeLocale(new Locale("hu"));
	}
}