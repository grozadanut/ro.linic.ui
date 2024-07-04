package ro.linic.ui.legacy.session;

import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.PresentationUtils.BR_SEPARATOR;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.Bundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.User;
import ro.colibri.util.GsonUtils;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.dialogs.PercentagePopup;

public class UIUtils
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(UIUtils.class.getSimpleName());
	
	public static String OS = System.getProperty("os.name").toLowerCase();
	
	public static final String FONT_SIZE_KEY = "font_size";
	public static final String FONT_SIZE_DEFAULT = "12";
	
	public static final String EXTRA_BANNER_FONT = "extra_banner_font";
	public static final String XX_BANNER_FONT = "xx_banner_font";
	public static final String XXX_BANNER_FONT = "xxx_banner_font";
	
	private static final int TOP_BAR_HEIGHT = 50;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public static void setDialogFont(final Control control)
	{
		control.setFont(JFaceResources.getDialogFont());
	}
	
	public static <T extends Control> T setFont(final T control)
	{
		control.setFont(JFaceResources.getDefaultFont());
		return control;
	}
	
	public static void setBoldFont(final Control control)
	{
		control.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
	}
	
	public static void setBannerFont(final Control control)
	{
		control.setFont(JFaceResources.getBannerFont());
	}
	
	public static void setBoldBannerFont(final Control control)
	{
		control.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.BANNER_FONT));
	}
	
	public static void setCustomFont(final Control control, final String key)
	{
		control.setFont(JFaceResources.getFontRegistry().get(key));
	}
	
	public static void setBoldCustomFont(final Control control, final String key)
	{
		control.setFont(JFaceResources.getFontRegistry().getBold(key));
	}
	
	public static void showResult(final InvocationResult result)
	{
		if (result.statusCanceled())
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(), result.toTextDescription());
	}
	
	public static void showResultEvenIfOK(final InvocationResult result)
	{
		if (result.statusCanceled())
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(), result.toTextDescription());
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription().replaceAll(BR_SEPARATOR, NEWLINE));
	}
	
	public static void showException(final Exception ex)
	{
		showException("Eroare", ex, null, null);
	}
	
	public static void showException(final Exception ex, final UISynchronize sync)
	{
		showException("Eroare", ex, null, sync);
	}
	
	public static void showException(final Throwable ex, final String message)
	{
		showException("Eroare", ex, message, null);
	}
	
	public static void showException(final String title, final Throwable ex, final String message)
	{
		showException(title, ex, message, null);
	}
	
	public static void showException(final String title, final Throwable ex, final String message, final UISynchronize sync)
	{
		if (sync != null)
			sync.asyncExec(() -> MessageDialog.openError(Display.getCurrent().getActiveShell(), title, 
					MessageFormat.format("{1}{0}{0}{2}", isEmpty(message) ? EMPTY_STRING : NEWLINE,
							safeString(message),
							safeString(ex, Throwable::getMessage))));
		else
			MessageDialog.openError(Display.getCurrent().getActiveShell(), title, 
					MessageFormat.format("{1}{0}{0}{2}", isEmpty(message) ? EMPTY_STRING : NEWLINE,
							safeString(message),
							safeString(ex, Throwable::getMessage)));
	}
	
	public static GridLayout layoutNoSpaces(final GridLayout layout)
	{
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}
	
	public static void insertDate(final DateTime widget, final LocalDate localDate)
	{
		if (localDate == null || widget.isDisposed())
			return;
		
		widget.setDate(localDate.getYear(), localDate.getMonthValue()-1, localDate.getDayOfMonth());
	}
	
	public static void insertDate(final DateTime widget, final LocalDateTime localDateTime)
	{
		if (localDateTime == null || widget.isDisposed())
			return;
		
		widget.setDate(localDateTime.getYear(), localDateTime.getMonthValue()-1, localDateTime.getDayOfMonth());
		widget.setTime(localDateTime.getHour(), localDateTime.getMinute(), 0);
	}
	
	public static void insertTime(final DateTime widget, final LocalTime localTime)
	{
		if (localTime == null || widget.isDisposed())
			return;
		
		widget.setTime(localTime.getHour(), localTime.getMinute(), 0);
	}
	
	public static LocalDate extractLocalDate(final DateTime widget)
	{
		return LocalDate.of(widget.getYear(), widget.getMonth()+1, widget.getDay());
	}
	
	public static LocalDateTime extractLocalDateTime(final DateTime dateWidget, final DateTime timeWidget)
	{
		return LocalDateTime.of(dateWidget.getYear(), dateWidget.getMonth()+1, dateWidget.getDay(),
				timeWidget.getHours(), timeWidget.getMinutes(), 0);
	}
	
	public static LocalTime extractLocalTime(final DateTime timeWidget)
	{
		return LocalTime.of(timeWidget.getHours(), timeWidget.getMinutes(), 0);
	}
	
	public static Point localToDisplayLocation(final Control widget)
	{
		return widget.toDisplay(0, 0);
	}
	
	public static Point localToDisplayLocation(final Control widget, final int x, final int y)
	{
		return widget.toDisplay(x, y);
	}
	
	public static boolean isWindows()
	{
		return (OS.indexOf("win") >= 0);
	}
	
	public static boolean isUnix()
	{
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}
	
	public static void setChildrenEnabled(final Composite composite, final boolean enabled)
	{
		for (final Control control : composite.getChildren())
			if (control instanceof Composite)
				setChildrenEnabled((Composite) control, enabled);
			else if (!(control instanceof Label))
				control.setEnabled(enabled);
	}
	
	public static void applyAdaosPopup(final Control control, final Supplier<BigDecimal> baseSupplier, final Consumer<BigDecimal> resultConsumer)
	{
		control.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.character == 'a' || e.character == 'A')
				{
					new PercentagePopup(control.getShell(), localToDisplayLocation(control, 0, control.getSize().y),
							baseSupplier, resultConsumer, BigDecimal.ONE)
					.open();
					e.doit = false;
				}
			}
		});
	}
	
	public static void applyPercentagePopup(final Control control, final Supplier<BigDecimal> baseSupplier, final Consumer<BigDecimal> resultConsumer)
	{
		control.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.character == 'a' || e.character == 'A')
				{
					new PercentagePopup(control.getShell(), localToDisplayLocation(control, 0, control.getSize().y),
							baseSupplier, resultConsumer, BigDecimal.ZERO)
					.open();
					e.doit = false;
				}
			}
		});
	}
	
	public static void createTopBar(final Composite parent)
	{
		final Color bgColor = ClientSession.instance().getLoggedUser().isHideUnofficialDocs() ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				
		final Composite topBarContainer = new Composite(parent, SWT.NONE);
		final GridLayout topBarLayout = new GridLayout(3, false);
		topBarLayout.horizontalSpacing = 0;
		topBarLayout.verticalSpacing = 0;
		topBarLayout.marginWidth = 0;
		topBarLayout.marginHeight = 0;
		topBarContainer.setLayout(topBarLayout);
		final GridData topBarGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		topBarGD.horizontalSpan = 4;
		topBarContainer.setLayoutData(topBarGD);
		topBarContainer.setBackground(bgColor);
		ClientSession.instance().addHideStateControl(topBarContainer);
		
		final CLabel dataLabel = new CLabel(topBarContainer, SWT.BORDER);
		dataLabel.setText(displayLocalDate(LocalDate.now()));
		dataLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		dataLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		dataLabel.setAlignment(SWT.CENTER);
		final GridData dataGD = new GridData();
		dataGD.heightHint = TOP_BAR_HEIGHT;
		dataGD.widthHint = 200;
		dataGD.verticalAlignment = SWT.CENTER;
		dataLabel.setLayoutData(dataGD);
		UIUtils.setBoldBannerFont(dataLabel);

		final CLabel operatorLabel = new CLabel(topBarContainer, SWT.BORDER);
		operatorLabel.setText(safeString(ClientSession.instance().getLoggedUser(), User::displayName));
		operatorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		operatorLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		operatorLabel.setAlignment(SWT.CENTER);
		final GridData operatorGD = new GridData();
		operatorGD.heightHint = TOP_BAR_HEIGHT;
		operatorGD.widthHint = 300;
		operatorLabel.setLayoutData(operatorGD);
		UIUtils.setBoldBannerFont(operatorLabel);
		
		final String companyGestLabel = MessageFormat.format("{0} - {1}",
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedCompany, Company::displayName),
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedGestiune, Gestiune::getName));
		final CLabel gestiuneLabel = new CLabel(topBarContainer, SWT.NONE);
		gestiuneLabel.setText(companyGestLabel);
		gestiuneLabel.setBackground(bgColor);
		gestiuneLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		final GridData gestiuneGD = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gestiuneGD.heightHint = TOP_BAR_HEIGHT;
		gestiuneLabel.setLayoutData(gestiuneGD);
		UIUtils.setBoldBannerFont(gestiuneLabel);
		ClientSession.instance().addHideStateControl(gestiuneLabel);
	}
	
	public static void createTopBar(final Composite parent, final EPartService partService, final Bundle bundle, final Logger log)
	{
		final Color bgColor = ClientSession.instance().getLoggedUser().isHideUnofficialDocs() ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				
		final Composite topBarContainer = new Composite(parent, SWT.NONE);
		final GridLayout topBarLayout = new GridLayout(4, false);
		topBarLayout.horizontalSpacing = 0;
		topBarLayout.verticalSpacing = 0;
		topBarLayout.marginWidth = 0;
		topBarLayout.marginHeight = 0;
		topBarContainer.setLayout(topBarLayout);
		final GridData topBarGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		topBarGD.horizontalSpan = 4;
		topBarContainer.setLayoutData(topBarGD);
		topBarContainer.setBackground(bgColor);
		ClientSession.instance().addHideStateControl(topBarContainer);
		
		final CLabel dataLabel = new CLabel(topBarContainer, SWT.BORDER);
		dataLabel.setText(displayLocalDate(LocalDate.now()));
		dataLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		dataLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		dataLabel.setAlignment(SWT.CENTER);
		final GridData dataGD = new GridData();
		dataGD.heightHint = TOP_BAR_HEIGHT;
		dataGD.widthHint = 200;
		dataGD.verticalAlignment = SWT.CENTER;
		dataLabel.setLayoutData(dataGD);
		UIUtils.setBoldBannerFont(dataLabel);

		final CLabel operatorLabel = new CLabel(topBarContainer, SWT.BORDER);
		operatorLabel.setText(safeString(ClientSession.instance().getLoggedUser(), User::displayName));
		operatorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		operatorLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		operatorLabel.setAlignment(SWT.CENTER);
		final GridData operatorGD = new GridData();
		operatorGD.heightHint = TOP_BAR_HEIGHT;
		operatorGD.widthHint = 300;
		operatorLabel.setLayoutData(operatorGD);
		UIUtils.setBoldBannerFont(operatorLabel);
		
		final String companyGestLabel = MessageFormat.format("{0} - {1}",
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedCompany, Company::displayName),
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedGestiune, Gestiune::getName));
		final CLabel gestiuneLabel = new CLabel(topBarContainer, SWT.NONE);
		gestiuneLabel.setText(companyGestLabel);
		gestiuneLabel.setBackground(bgColor);
		gestiuneLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		final GridData gestiuneGD = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gestiuneGD.heightHint = TOP_BAR_HEIGHT;
		gestiuneLabel.setLayoutData(gestiuneGD);
		UIUtils.setBoldBannerFont(gestiuneLabel);
		ClientSession.instance().addHideStateControl(gestiuneLabel);
		
		final CLabel syncStateLabel = new CLabel(topBarContainer, SWT.NONE);
		syncStateLabel.setText("OK");
		syncStateLabel.setAlignment(SWT.CENTER);
		syncStateLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		syncStateLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData workingModeGD = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		workingModeGD.widthHint = 200;
		workingModeGD.heightHint = TOP_BAR_HEIGHT;
		syncStateLabel.setLayoutData(workingModeGD);
		UIUtils.setBoldBannerFont(syncStateLabel);
		ClientSession.instance().addSyncStateControl(syncStateLabel);
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
	
	/**
	 * Does not work if no Window is visible
	 */
	public static EPartService getEPartService()
	{
		final MApplication lApplication = E4Workbench.getServiceContext().get(MApplication.class);
		final MTrimmedWindow lWindow = (MTrimmedWindow) lApplication.getChildren().stream().findFirst().orElse(null);
		if(lWindow != null && lWindow.getContext() != null)
			return lWindow.getContext().get(EPartService.class);
		return null;
	}
	
	public static Optional<String> readJsonProperty(final String json, final String property)
	{
		try
		{
			final JsonNode parent = OBJECT_MAPPER.readTree(json);
			return Optional.ofNullable(parent.path(property).asText());
		}
		catch (final Exception e)
		{
			log.log(Level.FINE, "Parsing exception", e);
			return Optional.empty();
		}
	}
	
	public static <E> Collection<E> readJsonList(final InputStream json, final Class<E> clazz)
			throws IOException
	{
		final Gson gson = GsonUtils.get();
		final Builder<E> builder = ImmutableList.<E>builder();
	    try (JsonReader reader = new JsonReader(new InputStreamReader(json));)
	    {
	        reader.beginArray();
	        while (reader.hasNext())
	        {
	        	final E entity = gson.fromJson(reader, clazz);
	        	builder.add(entity);
	        }
	        reader.endArray();
	    }
	    return builder.build();
	}
	
	public static String removeFileExtension(final String path)
	{
		if (isEmpty(path))
			return "";
		
		if (path.contains("\\"))
		{ // direct substring method give wrong result for "a.b.c.d\e.f.g\supersu"
			String filenameWithoutExtension;
			final String filename = path.substring(path.lastIndexOf("\\"));
			final String foldrpath = path.substring(0, path.lastIndexOf('\\'));
			
			if (filename.contains("."))
				filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
			else
				filenameWithoutExtension = filename;
			return foldrpath + filenameWithoutExtension;
		}
		else
			return path.substring(0, path.lastIndexOf('.'));
	}
	
	public static Optional<Long> copyFileFromTo(final InputStream from, final String outputFileUri)
	{
		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFileUri))
		{
			final ReadableByteChannel readableByteChannel = Channels.newChannel(from);
			final FileChannel fileChannel = fileOutputStream.getChannel();
			return Optional.of(fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE));
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			showException(e);
			return Optional.empty();
		}
	}
}
