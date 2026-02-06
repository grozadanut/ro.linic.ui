package ro.linic.ui.legacy.service;

import java.util.function.Consumer;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import ro.linic.ui.base.services.ui.Notifier;

public class InAppNotificationService implements NotificationService {
	private Bundle bundle;
	private Logger log;

	public InAppNotificationService(final Bundle bundle, final Logger log) {
		super();
		this.bundle = bundle;
		this.log = log;
	}

	@Override
	public void show(final String title, final String notification, final Consumer<Object> callback) {
		try {
			Notifier.notify(title, notification, 30000, callback);
		} catch (final Exception e) {
			log.error(e);
		}
	}
}