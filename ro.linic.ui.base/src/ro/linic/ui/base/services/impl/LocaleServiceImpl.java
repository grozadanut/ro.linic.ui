package ro.linic.ui.base.services.impl;

import java.util.Locale;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.nls.ILocaleChangeService;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.base.dialogs.PromptRestartDialog;
import ro.linic.ui.base.services.LocaleService;
import ro.linic.ui.security.services.AuthenticationSession;
import ro.linic.ui.workbench.services.LinicWorkbench;

public class LocaleServiceImpl implements LocaleService {
	private static final String LOCALE_ARG = "-nl";
	
	@Inject private IEclipseContext ctx;
	
	@Override
	public void changeLocale(final Locale locale) {
		final LinicWorkbench workbench = ctx.get(LinicWorkbench.class);
		final boolean replaced = workbench.replaceProgramArgument(LOCALE_ARG, locale.toString());
		
		if (!replaced)
			return;
		
		if (ctx.get(MApplication.class) != null) {
			ctx.get(ILocaleChangeService.class).changeApplicationLocale(locale);
			promptRestart();
		} else {
			ctx.get(AuthenticationSession.class).storeSession();
			workbench.lazyRestart();
		}
	}

	private void promptRestart() {
		final int retCode = PromptRestartDialog.promptForRestart(ctx.get(Shell.class), true);
		if (retCode == PromptRestartDialog.PROFILE_RESTART) {
			ctx.get(AuthenticationSession.class).storeSession();
			ctx.get(LinicWorkbench.class).lazyRestart();
		}
	}
}
