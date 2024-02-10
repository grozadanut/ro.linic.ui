package ro.linic.ui.legacy.service;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.session.NotificationManager;

public class WindowsNotificationService implements NotificationService
{
	private static Image logoImg;
	
	private Bundle bundle;
	private Logger log;
	
	private static TrayIcon getTrayIcon() throws AWTException
	{
		final SystemTray tray = SystemTray.getSystemTray();
		final TrayIcon[] createdTrayIcons = tray.getTrayIcons();
		TrayIcon trayIcon;
		
		if (createdTrayIcons.length < 1)
		{
			trayIcon = new TrayIcon(logoImg);
			trayIcon.setImageAutoSize(true);

			final java.awt.PopupMenu popup = new PopupMenu();
			final MenuItem muteItem = new MenuItem("Suspenda notificarile");
			muteItem.addActionListener(e -> muteNotifications(tray, trayIcon));
			popup.add(muteItem);
			
			trayIcon.setPopupMenu(popup);
			tray.add(trayIcon);
		}
		else
			trayIcon = createdTrayIcons[0];
		
		return trayIcon;
	}
	
	private static void muteNotifications(final SystemTray tray, final TrayIcon trayIcon)
	{
		NotificationManager.initialize(null);
		tray.remove(trayIcon);
	}

	public WindowsNotificationService(final Bundle bundle, final Logger log)
	{
		super();
		this.bundle = bundle;
		this.log = log;
		logoImg = Icons.createAWTImage(bundle, Icons.LOGO_256x256_PATH, log)
				.orElseGet(() -> new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
	}

	@Override
	public void show(final String title, final String notification, final Consumer<Object> callback)
	{
		try
		{
			final TrayIcon trayIcon = getTrayIcon();
			for (final ActionListener listener : trayIcon.getActionListeners())
				trayIcon.removeActionListener(listener);
			if (callback != null)
				trayIcon.addActionListener(e -> callback.accept(e));
			trayIcon.setToolTip(title);
			trayIcon.displayMessage(title, notification, MessageType.INFO);
		}
		catch (final Exception e)
		{
			log.error(e);
		}
	}
}