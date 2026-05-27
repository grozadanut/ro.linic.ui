package ro.linic.ui.base.services;

import static ro.flexbiz.util.commons.PresentationUtils.NEWLINE;

import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import ro.linic.ui.base.dialogs.ConfirmDialog;
import ro.linic.ui.base.preferences.PreferenceKey;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.security.services.AuthenticationSession;

public class UtilServices {
	private static final ILog log = ILog.of(UtilServices.class);
	
	public static void sendSms(final Shell shell, final IEclipseContext ctx,
			final String name, final List<String> toPhoneNumbers, final String message) {
		final GenericValue phoneBody = new GenericValue("", "");
		phoneBody.put("text", message);
		phoneBody.put("phoneNumbers", toPhoneNumbers);
		
		if (ConfirmDialog.open(shell, ro.linic.ui.base.Messages.Confirm,
				"Catre "+name+" "+toPhoneNumbers+NEWLINE+NEWLINE+message)) {
			RestCaller.post("/rest/s1/moqui-linic-legacy/sms")
			.internal(ctx.get(AuthenticationSession.class))
			.body(BodyProvider.of(phoneBody))
			.sync(t -> UIUtils.showException(t, ctx.get(UISynchronize.class)));
		}
	}
	
	public static boolean isFreshUI() {
		final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		return node.getBoolean(PreferenceKey.IS_FRESH_UI, true);
	}
	
	public static void setFreshUI(final boolean freshUI) {
		final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		try {
			node.putBoolean(PreferenceKey.IS_FRESH_UI, freshUI);
			node.flush();
		} catch (final BackingStoreException e) {
			log.error(e.getMessage(), e);
		}
	}
}
