package ro.linic.ui.legacy;

import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.ServerConstants.L1_NAME;
import static ro.colibri.util.ServerConstants.L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.linic.ui.legacy.session.UIUtils.FONT_SIZE_DEFAULT;
import static ro.linic.ui.legacy.session.UIUtils.FONT_SIZE_KEY;
import static ro.linic.ui.legacy.session.UIUtils.isWindows;

import java.awt.SystemTray;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.ImmutableList;
import com.opcoach.e4.preferences.ScopedPreferenceStore;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.dialogs.ReleaseNotesDialog;
import ro.linic.ui.legacy.service.PeripheralService;
import ro.linic.ui.legacy.service.WindowsNotificationService;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;
import ro.linic.ui.legacy.session.NotificationManager;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.cloud.model.CloudReceipt;
import ro.linic.ui.pos.cloud.model.CloudReceiptLine;
import ro.linic.ui.security.services.AuthenticationSession;
import ro.linic.ui.workbench.services.LinicWorkbench;

public class LoginAddon {
	private static final ILog log = ILog.of(LoginAddon.class);
	private static final String JUST_UPDATED_PROP = "justUpdated";
	
	private static void flushPrefs(final IEclipsePreferences prefs, final Logger log)
	{
		try
		{
			prefs.flush();
		}
		catch (final BackingStoreException e)
		{
			log.error(e);
		}
	}
	
	@PostConstruct
	public void beforeStartup(final IEclipseContext workbenchContext, final UISynchronize sync,
			@OSGiBundle final Bundle bundle, @Preference final IEclipsePreferences prefs)
	{
		Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH);
		final Logger log = (Logger) workbenchContext.get(Logger.class.getName());
		try
		{
			final String forcedPlugin = RestCaller.get("https://colibriserver.go.ro/repository/force-update.txt")
				.asyncRaw(BodyHandlers.ofString())
				.thenApply(HttpUtils::checkOk)
				.thenApply(HttpResponse::body)
				.get();
			
			final Version minForcedVersion = Version.parseVersion(forcedPlugin.split(PresentationUtils.SPACE)[1]);
			final Version installedVersion = bundle.getVersion();

			if (installedVersion.compareTo(minForcedVersion) < 0) {
				log.info("current version: {0}, min version: {1}; forcing update!", installedVersion, minForcedVersion);
				update(workbenchContext, prefs, sync);
			}
		}
		catch (final Exception e)
		{
			log.error(e);
		}
		
		logVersion(log);
		initDefaultFonts();
		// force authentication early
		workbenchContext.get(AuthenticationSession.class).authentication();
		// after successful login
		initSQLite(workbenchContext);
//		SQLiteJDBC.instance(bundle, log).init();
//		SQLiteJDBC.instance(bundle, log).saveLocalToServer();
		workbenchContext.get(LinicWorkbench.class).switchWorkspace("workspace-"+ClientSession.instance().getLoggedUser().getId());

		if (Boolean.valueOf(System.getProperty(NotificationManager.SHOW_NOTIFICATIONS_PROP_KEY,
				NotificationManager.SHOW_NOTIFICATIONS_PROP_DEFAULT)) &&
				isWindows() && SystemTray.isSupported())
			NotificationManager.initialize(new WindowsNotificationService(bundle, log));

		registerBarcodePrinter(log, bundle);
		new JMSGeneralTopicHandler(log, bundle, sync);
		
		workbenchContext.set("ro.linic.ui.legacy.prefStore",
				new ScopedPreferenceStore(ConfigurationScope.INSTANCE, bundle.getSymbolicName()));
	}
	
	private void logVersion(final Logger log)
	{
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final Version v = bundle.getVersion();
		log.info(String.format("%s %d.%d.%d", bundle.getSymbolicName(),
				v.getMajor(), v.getMinor(), v.getMicro()));
	}

	private void update(final IEclipseContext workbenchContext, final IEclipsePreferences prefs, final UISynchronize sync)
	{
		final Logger log = (Logger) workbenchContext.get(Logger.class.getName());
		final IProvisioningAgent agent = (IProvisioningAgent) workbenchContext.get(IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			LogHelper.log(new Status(IStatus.ERROR, "linic_gest_client",
					"No provisioning agent found.  This application is not set up for updates."));
		
		// if we're restarting after updating, don't check again.
		if (prefs.getBoolean(JUST_UPDATED_PROP, false))
		{
			System.setProperty(E4Workbench.CLEAR_PERSISTED_STATE, "true"); //$NON-NLS-1$
			prefs.putBoolean(JUST_UPDATED_PROP, false);
			flushPrefs(prefs, log);
			return;
		}

		// check for updates before starting up.
		// If an update is performed, restart. Otherwise log
		// the status.
		final IRunnableWithProgress runnable = new IRunnableWithProgress()
		{
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				final IStatus updateStatus = P2Util.checkForUpdates(agent, monitor);
				if (updateStatus.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
				{
					// continue
				}
				else if (updateStatus.getSeverity() != IStatus.ERROR)
				{
					sync.syncExec(new Runnable()
					{
						@Override public void run()
						{
							prefs.putBoolean(JUST_UPDATED_PROP, true);
							flushPrefs(prefs, log);
							new ReleaseNotesDialog(null, log).open();
							System.exit(0);
						}
					});
				} else
					LogHelper.log(updateStatus);
			}
		};
		try
		{
			new ProgressMonitorDialog(null).run(true, true, runnable);
		}
		catch (final InvocationTargetException e)
		{
			log.error(e);
		}
		catch (final InterruptedException e)
		{
			log.error(e);
		}
	}

	private void initDefaultFonts()
	{
		final int defaultFontSize = Integer.parseInt(System.getProperty(FONT_SIZE_KEY, FONT_SIZE_DEFAULT)); //12
		
		final FontData[] bannerFD = JFaceResources.getBannerFont().getFontData();
		bannerFD[0].setHeight(defaultFontSize+2);//14
		JFaceResources.getFontRegistry().put(JFaceResources.BANNER_FONT, bannerFD);

		final FontData[] dialogFD = JFaceResources.getDialogFont().getFontData();
		dialogFD[0].setHeight(defaultFontSize-2);//10
		JFaceResources.getFontRegistry().put(JFaceResources.DIALOG_FONT, dialogFD);
		
		final FontData[] defaultFD = JFaceResources.getDefaultFont().getFontData();
		defaultFD[0].setHeight(defaultFontSize);//12
		JFaceResources.getFontRegistry().put(JFaceResources.DEFAULT_FONT, defaultFD);
		
		final FontData[] extraBannerFD = JFaceResources.getBannerFont().getFontData();
		extraBannerFD[0].setHeight(defaultFontSize+4);//16
		JFaceResources.getFontRegistry().put(UIUtils.EXTRA_BANNER_FONT, extraBannerFD);
		
		final FontData[] xxBannerFD = JFaceResources.getBannerFont().getFontData();
		xxBannerFD[0].setHeight(defaultFontSize+6);//18
		JFaceResources.getFontRegistry().put(UIUtils.XX_BANNER_FONT, xxBannerFD);
		
		final FontData[] xxxBannerFD = JFaceResources.getBannerFont().getFontData();
		xxxBannerFD[0].setHeight(defaultFontSize+8);//20
		JFaceResources.getFontRegistry().put(UIUtils.XXX_BANNER_FONT, xxxBannerFD);
	}
	
	private void registerBarcodePrinter(final Logger log, final Bundle bundle)
	{
		final String remoteJndi = ClientSession.instance().getLoggedUser().getSelectedGestiune().isMatch(L1_NAME) ? 
				L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI : L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
		final String barcodePrinter = System.getProperty(PeripheralService.BARCODE_PRINTER_KEY, PeripheralService.BARCODE_PRINTER_DEFAULT);
		if (PeripheralService.instance().isPrinterConnected(barcodePrinter))
			MessagingService.instance().registerMsgListener(remoteJndi, new PrintTopicListener(log, bundle));
	}
	
	private void initSQLite(final IEclipseContext workbenchContext) {
		final LocalDatabase localDatabase = workbenchContext.get(LocalDatabase.class);
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement()) {
			stmt.execute(createProductsTableSql());
			stmt.execute(createReceiptTableSql());
			stmt.execute(createReceiptLineTableSql());
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static String createProductsTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Product.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Product.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Product.TYPE_FIELD+" text,").append(NEWLINE)
		.append(Product.TAX_CODE_FIELD+" text,").append(NEWLINE)
		.append(Product.DEPARTMENT_CODE_FIELD+" text,").append(NEWLINE)
		.append(Product.SKU_FIELD+" text UNIQUE,").append(NEWLINE)
		.append(Product.BARCODES_FIELD+" text UNIQUE,").append(NEWLINE)
		.append(Product.NAME_FIELD+" text,").append(NEWLINE)
		.append(Product.UOM_FIELD+" text,").append(NEWLINE)
		.append(Product.IS_STOCKABLE_FIELD+" integer,").append(NEWLINE)
		.append(Product.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(Product.STOCK_FIELD+" numeric(16,4),").append(NEWLINE)
		.append(Product.IMAGE_ID_FIELD+" text,").append(NEWLINE)
		.append(Product.TAX_PERCENTAGE_FIELD+" numeric(6,4)").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
	
	public static String createReceiptTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Receipt.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(CloudReceipt.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(CloudReceipt.CLOSED_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceipt.SYNCED_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceipt.CREATION_TIME_FIELD+" text,").append(NEWLINE)
		.append(CloudReceipt.NUMBER_FIELD+" integer").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
	
	public static String createReceiptLineTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(CloudReceiptLine.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(CloudReceiptLine.PRODUCT_ID_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceiptLine.RECEIPT_ID_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceiptLine.SKU_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.NAME_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.UOM_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.QUANTITY_FIELD+" numeric(16,3),").append(NEWLINE)
		.append(CloudReceiptLine.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(CloudReceiptLine.TAX_TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(CloudReceiptLine.TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(CloudReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(CloudReceiptLine.TAX_CODE_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.DEPARTMENT_CODE_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.CREATION_TIME_FIELD+" text,").append(NEWLINE)
		.append(CloudReceiptLine.SYNCED_FIELD+" integer,").append(NEWLINE)
		.append(LegacyReceiptLine.WAREHOUSE_ID_FIELD+" integer,").append(NEWLINE)
		.append(LegacyReceiptLine.USER_ID_FIELD+" integer").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}

	@PreDestroy
	public void applicationShutdown(final IEclipseContext workbenchContext)
	{
		MessagingService.instance().closeSession();
	}
	
	private static class PrintTopicListener implements MessageListener
	{
		private Logger log;
		private Bundle bundle;
		
		public PrintTopicListener(final Logger log, final Bundle bundle)
		{
			this.log = log;
			this.bundle = bundle;
		}

		@Override public void onMessage(final Message msg)
		{
			try
			{
				if (msg instanceof ObjectMessage)
				{
					final ObjectMessage objMessage = (ObjectMessage) msg;
					final ImmutableList<BarcodePrintable> printables = (ImmutableList<BarcodePrintable>) objMessage.getObject();
					final String barcodePrinter = System.getProperty(PeripheralService.BARCODE_PRINTER_KEY, PeripheralService.BARCODE_PRINTER_DEFAULT);
					PeripheralService.printPrintables(printables, barcodePrinter, log, bundle, false, java.util.Optional.empty());
				}
			}
			catch (final Exception e)
			{
				log.error(e);
			}
		}
	}
}
