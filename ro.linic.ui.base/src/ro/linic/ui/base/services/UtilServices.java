package ro.linic.ui.base.services;

import static ro.flexbiz.util.commons.PresentationUtils.NEWLINE;

import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.base.dialogs.ConfirmDialog;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.security.services.AuthenticationSession;

public class UtilServices {
	public static void sendSms(final Shell shell, final IEclipseContext ctx,
			final String name, final List<String> toPhoneNumbers, final String message) {
		final GenericValue phoneBody = new GenericValue("", "");
		phoneBody.put("text", message);
		phoneBody.put("phoneNumbers", toPhoneNumbers);
		
		if (ConfirmDialog.open(shell, ro.linic.ui.base.Messages.Confirm,
				"Catre "+name+" "+toPhoneNumbers+NEWLINE+NEWLINE+message)) {
			RestCaller.post("/rest/s1/moqui-linic-legacy/sms")
			.internal(ctx.get(AuthenticationSession.class).authentication())
			.body(BodyProvider.of(phoneBody))
			.sync(t -> UIUtils.showException(t, ctx.get(UISynchronize.class)));
		}
	}
}
