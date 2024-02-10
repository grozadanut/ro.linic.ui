package ro.linic.ui.legacy.service;

import java.util.function.Consumer;

import org.wildfly.common.annotation.Nullable;

public interface NotificationService
{
	/**
	 * Shows a notification and the callback is called when the notification is clicked.
	 * 
	 * @param title of the notification, implementation not guaranteed
	 * @param notification text
	 * @param callback called when the notification is clicked; can be null;
	 * on object will be provided by the implementation, usually an event; implementation not guaranteed
	 */
	public void show(@Nullable String title, String notification, @Nullable Consumer<Object> callback);
}