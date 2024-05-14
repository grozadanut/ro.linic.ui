 
package ro.linic.ui.base.handlers;

import java.util.Locale;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.nls.ILocaleChangeService;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolItem;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class LanguageHandler {
	private MHandledToolItem languageToolItem;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService) {
		languageToolItem = modelService.findElements(application, "ro.linic.ui.base.handledtoolitem.language", //$NON-NLS-1$
				MHandledToolItem.class).iterator().next();
		localeChange(Locale.getDefault());
	}
	
	@Execute
	public void execute() {
		final ToolItem toolItem = (ToolItem) languageToolItem.getWidget();
		
		// Creates fake selection event.
		final Event newEvent = new Event();
		newEvent.button = 1;
		newEvent.widget = toolItem;
		newEvent.detail = SWT.ARROW;
		newEvent.x = toolItem.getBounds().x;
		newEvent.y = toolItem.getBounds().y + toolItem.getBounds().height;

		// Dispatches the event.
		toolItem.notifyListeners(SWT.Selection, newEvent);
	}
	
	@Inject
	void localeChange(@Optional @UIEventTopic(ILocaleChangeService.LOCALE_CHANGE) final Locale locale) {
		if (languageToolItem == null)
			return;
		
		if (locale == null) {
			languageToolItem.setIconURI("platform:/plugin/ro.linic.ui.base/icons/usa_flag_32x32.png");
			return;
		}
		
		switch (locale.getLanguage()) {
		case "ro" -> languageToolItem.setIconURI("platform:/plugin/ro.linic.ui.base/icons/ro_flag_32x32.png");
		case "hu" -> languageToolItem.setIconURI("platform:/plugin/ro.linic.ui.base/icons/hu_flag_32x32.png");
		default -> languageToolItem.setIconURI("platform:/plugin/ro.linic.ui.base/icons/usa_flag_32x32.png");
		}
	}
}