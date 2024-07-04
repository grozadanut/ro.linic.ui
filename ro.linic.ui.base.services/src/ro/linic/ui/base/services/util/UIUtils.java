package ro.linic.ui.base.services.util;

import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.util.commons.PresentationUtils.safeString;
import static ro.linic.util.commons.StringUtils.isEmpty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.ui.PercentagePopup;

public class UIUtils {
	private static final ILog log = logger(UIUtils.class);
	
	public static String OS = System.getProperty("os.name").toLowerCase();

	public static final String FONT_SIZE_KEY = "font_size";
	public static final String FONT_SIZE_DEFAULT = "14";

	public static final String EXTRA_BANNER_FONT = "extra_banner_font";
	public static final String XX_BANNER_FONT = "xx_banner_font";
	public static final String XXX_BANNER_FONT = "xxx_banner_font";

	public static void setDialogFont(final Control control) {
		control.setFont(JFaceResources.getDialogFont());
	}

	public static <T extends Control> T setFont(final T control) {
		control.setFont(JFaceResources.getDefaultFont());
		return control;
	}

	public static <T extends Control> T setBoldFont(final T control) {
		control.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		return control;
	}

	public static <T extends Control> T setBannerFont(final T control) {
		control.setFont(JFaceResources.getBannerFont());
		return control;
	}

	public static <T extends Control> T setBoldBannerFont(final T control) {
		control.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.BANNER_FONT));
		return control;
	}

	public static <T extends Control> T setCustomFont(final T control, final String key) {
		control.setFont(JFaceResources.getFontRegistry().get(key));
		return control;
	}

	public static <T extends Control> T setBoldCustomFont(final T control, final String key) {
		control.setFont(JFaceResources.getFontRegistry().getBold(key));
		return control;
	}
	
	public static void showResult(final IStatus status)
	{
		if (!status.isOK())
			MessageDialog.openError(Display.getCurrent().getActiveShell(), status.getPlugin(), status.getMessage());
	}

	public static void showException(final Exception ex) {
		showException("Error", ex, null, null);
	}

	public static void showException(final Exception ex, final UISynchronize sync) {
		showException("Error", ex, null, sync);
	}

	public static void showException(final Throwable ex, final String message) {
		showException("Error", ex, message, null);
	}

	public static void showException(final String title, final Throwable ex, final String message) {
		showException(title, ex, message, null);
	}

	public static void showException(final String title, final Throwable ex, final String message,
			final UISynchronize sync) {
		if (sync != null)
			sync.asyncExec(() -> MessageDialog.openError(Display.getCurrent().getActiveShell(), title,
					MessageFormat.format("{1}{0}{0}{2}", isEmpty(message) ? EMPTY_STRING : NEWLINE, safeString(message),
							safeString(ex, Throwable::getMessage))));
		else
			MessageDialog.openError(Display.getCurrent().getActiveShell(), title,
					MessageFormat.format("{1}{0}{0}{2}", isEmpty(message) ? EMPTY_STRING : NEWLINE, safeString(message),
							safeString(ex, Throwable::getMessage)));
	}

	public static GridLayout layoutNoSpaces(final GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static void insertDate(final DateTime widget, final LocalDate localDate) {
		if (localDate == null || widget.isDisposed())
			return;

		widget.setDate(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());
	}

	public static void insertDate(final DateTime widget, final LocalDateTime localDateTime) {
		if (localDateTime == null || widget.isDisposed())
			return;

		widget.setDate(localDateTime.getYear(), localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth());
		widget.setTime(localDateTime.getHour(), localDateTime.getMinute(), 0);
	}

	public static void insertTime(final DateTime widget, final LocalTime localTime) {
		if (localTime == null || widget.isDisposed())
			return;

		widget.setTime(localTime.getHour(), localTime.getMinute(), 0);
	}

	public static LocalDate extractLocalDate(final DateTime widget) {
		return LocalDate.of(widget.getYear(), widget.getMonth() + 1, widget.getDay());
	}

	public static LocalDateTime extractLocalDateTime(final DateTime dateWidget, final DateTime timeWidget) {
		return LocalDateTime.of(dateWidget.getYear(), dateWidget.getMonth() + 1, dateWidget.getDay(),
				timeWidget.getHours(), timeWidget.getMinutes(), 0);
	}

	public static LocalTime extractLocalTime(final DateTime timeWidget) {
		return LocalTime.of(timeWidget.getHours(), timeWidget.getMinutes(), 0);
	}

	public static Point localToDisplayLocation(final Control widget) {
		return widget.toDisplay(0, 0);
	}

	public static Point localToDisplayLocation(final Control widget, final int x, final int y) {
		return widget.toDisplay(x, y);
	}

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	public static void setChildrenEnabled(final Composite composite, final boolean enabled) {
		for (final Control control : composite.getChildren())
			if (control instanceof Composite)
				setChildrenEnabled((Composite) control, enabled);
			else if (!(control instanceof Label))
				control.setEnabled(enabled);
	}

	/**
	 * Adds a popup that opens on key press that can calculate the margin percentage
	 * from a base value. <br>
	 * <br>
	 * example: with a base value of 100 and a margin of 15%, the result will
	 * be:<br>
	 * result = baseValue*(margin+1) = 100*(0.15+1) = 115
	 * 
	 * @param control        the control to which to apply the percentage popup
	 * @param baseSupplier   supplier to get the value that is the base of the
	 *                       percentage calculation
	 * @param resultConsumer consume the result of the percentage calculation,
	 *                       rounded to 2 decimal places
	 */
	public static void applyMarginPopup(final Control control, final Supplier<BigDecimal> baseSupplier,
			final Consumer<BigDecimal> resultConsumer) {
		control.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.character == 'a' || e.character == 'A' || e.character == '%') {
					new PercentagePopup(control.getShell(), localToDisplayLocation(control, 0, control.getSize().y),
							baseSupplier, resultConsumer, BigDecimal.ONE).open();
					e.doit = false;
				}
			}
		});
	}

	/**
	 * Adds a popup that opens on key press that can calculate the percentage from a
	 * base value. <br>
	 * <br>
	 * example: with a base value of 100 and a percentage of 15%, the result will
	 * be:<br>
	 * result = baseValue*percentage = 100*0.15 = 15
	 * 
	 * @param control        the control to which to apply the percentage popup
	 * @param baseSupplier   supplier to get the value that is the base of the
	 *                       percentage calculation
	 * @param resultConsumer consume the result of the percentage calculation,
	 *                       rounded to 2 decimal places
	 */
	public static void applyPercentagePopup(final Control control, final Supplier<BigDecimal> baseSupplier,
			final Consumer<BigDecimal> resultConsumer) {
		control.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.character == 'a' || e.character == 'A' || e.character == '%') {
					new PercentagePopup(control.getShell(), localToDisplayLocation(control, 0, control.getSize().y),
							baseSupplier, resultConsumer, BigDecimal.ZERO).open();
					e.doit = false;
				}
			}
		});
	}
	
	public static void saveState(final String prefix, final NatTable table, final MPart part)
	{
		final Properties prop = new Properties();
		table.saveState(prefix, prop);
		for (final Entry<Object, Object> entry : prop.entrySet())
			part.getPersistedState().put((String)entry.getKey(), (String)entry.getValue());
	}
	
	public static void loadState(final String prefix, final NatTable table, final MPart part)
	{
		final Properties prop = new Properties();
		prop.putAll(part.getPersistedState());
		if (prop.keySet().stream().filter(p -> p.toString().startsWith(prefix)).findAny().isPresent())
			table.loadState(prefix, prop);
	}

	public static String removeFileExtension(final String path) {
		if (isEmpty(path))
			return "";

		if (path.contains("\\")) { // direct substring method give wrong result for "a.b.c.d\e.f.g\supersu"
			String filenameWithoutExtension;
			final String filename = path.substring(path.lastIndexOf("\\"));
			final String foldrpath = path.substring(0, path.lastIndexOf('\\'));

			if (filename.contains("."))
				filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
			else
				filenameWithoutExtension = filename;
			return foldrpath + filenameWithoutExtension;
		} else
			return path.substring(0, path.lastIndexOf('.'));
	}

	/**
	 * @return The number of bytes, possibly zero,that were actually transferred
	 */
	public static Optional<Long> copyFileFromTo(final InputStream from, final String outputFileUri) {
		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFileUri)) {
			final ReadableByteChannel readableByteChannel = Channels.newChannel(from);
			final FileChannel fileChannel = fileOutputStream.getChannel();
			return Optional.of(fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE));
		} catch (final IOException e) {
			e.printStackTrace();
			showException(e);
			return Optional.empty();
		}
	}
	
	public static void askSave(final Shell activeShell, final EPartService partService) {
		final MPart activePart = partService.getActivePart();
		if (activePart != null && activePart.isDirty() && MessageDialog.openQuestion(activeShell, Messages.Save, Messages.SaveConfirm))
			partService.savePart(activePart, false);
	}
	
	public static Optional<URL> find(final Bundle bundle, final String url) {
		try {
			return Optional.ofNullable(FileLocator.toFileURL(FileLocator.find(bundle, new Path(url))));
		} catch (final IOException e) {
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
	}
	
	public static ILog logger(final Class<?> clazz) {
		try {
			return ILog.of(clazz);
		} catch (final Exception e) {
			// logger not present in unit tests
			return new ILog() {
				@Override
				public void removeLogListener(final ILogListener listener) {
				}
				@Override
				public void log(final IStatus status) {
					System.out.println(status);
				}
				@Override
				public Bundle getBundle() {
					return null;
				}
				@Override
				public void addLogListener(final ILogListener listener) {
				}
			};
		}
	}
}
