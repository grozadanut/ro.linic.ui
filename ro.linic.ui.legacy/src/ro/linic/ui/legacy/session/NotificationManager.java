package ro.linic.ui.legacy.session;

import java.util.function.Consumer;

import org.wildfly.common.annotation.Nullable;

import ro.linic.ui.legacy.service.NotificationService;
import ro.linic.ui.legacy.service.NullNotificationService;

public class NotificationManager
{
	private static NotificationService instance = new NullNotificationService();
	
	public static final String SHOW_NOTIFICATIONS_PROP_KEY = "show_notifications";
	public static final String SHOW_NOTIFICATIONS_PROP_DEFAULT = "true";
	
	private NotificationManager()
	{
		// NOT instantiable
	}
	
	public static void initialize(@Nullable final NotificationService service)
	{
		if (service == null)
			instance = new NullNotificationService();
		else
			instance = service;
	}
	
	/**
	 * Shows a notification and the callback is called when the notification is clicked.
	 * 
	 * @param title of the notification, implementation not guaranteed
	 * @param notification text
	 * @param callback called when the notification is clicked; can be null;
	 * on object will be provided by the implementation, usually an event; implementation not guaranteed
	 */
	public static void showNotification(@Nullable final String title, final String notification,
			@Nullable final Consumer<Object> callback)
	{
		instance.show(title, notification, callback);
	}
}