package ro.linic.ui.legacy.service;

import java.util.function.Consumer;

public class NullNotificationService implements NotificationService
{
	@Override public void show(final String title, final String notification, final Consumer<Object> callback)
	{
		// do nothing
	}
}