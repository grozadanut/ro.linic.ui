package ro.linic.ui.legacy;

import static ro.colibri.util.NumberUtils.parseToLong;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.ServerConstants.GENERAL_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.JMS_MESSAGE_TYPE_KEY;
import static ro.colibri.util.ServerConstants.L1_NAME;
import static ro.colibri.util.ServerConstants.L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.REFRESH_ALL_PERSISTED_PROPS;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.truncate;
import static ro.linic.ui.legacy.session.UIUtils.FONT_SIZE_DEFAULT;
import static ro.linic.ui.legacy.session.UIUtils.FONT_SIZE_KEY;
import static ro.linic.ui.legacy.session.UIUtils.getEPartService;
import static ro.linic.ui.legacy.session.UIUtils.isWindows;

import java.awt.SystemTray;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

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
import jakarta.jms.TextMessage;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.legacy.components.CommandHandler;
import ro.linic.ui.legacy.components.JMSMessageHandler;
import ro.linic.ui.legacy.dialogs.ReleaseNotesDialog;
import ro.linic.ui.legacy.parts.VerifyOperationsPart;
import ro.linic.ui.legacy.service.PeripheralService;
import ro.linic.ui.legacy.service.WindowsNotificationService;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.LegacyReceiptLine;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;
import ro.linic.ui.legacy.session.NotificationManager;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.pos.base.addons.InitAddon;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
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
			update(workbenchContext, prefs, sync);
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
		registerGeneralTopic(log, bundle, sync);
		
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
		if (PeripheralService.instance(log).isPrinterConnected(barcodePrinter))
			MessagingService.instance().registerMsgListener(remoteJndi, new PrintTopicListener(log, bundle));
	}
	
	private void registerGeneralTopic(final Logger log, final Bundle bundle, final UISynchronize sync)
	{
		final JMSMessageHandler messageHandler = new JMSMessageHandler();
		messageHandler.registerHandler(new ResetPropsHandler(log));
		messageHandler.registerHandler(new WaitOrderReceivedHandler(log, sync));
		MessagingService.instance().registerMsgListener(GENERAL_TOPIC_REMOTE_JNDI, messageHandler);
	}
	
	private void initSQLite(final IEclipseContext workbenchContext) {
		final LocalDatabase localDatabase = workbenchContext.get(LocalDatabase.class);
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(InitAddon.class).getSymbolicName());
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
		.append(Receipt.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(Receipt.CREATION_TIME_FIELD+" text").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
	
	public static String createReceiptLineTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(ReceiptLine.PRODUCT_ID_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.RECEIPT_ID_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.NAME_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.UOM_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.QUANTITY_FIELD+" numeric(16,3),").append(NEWLINE)
		.append(ReceiptLine.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(ReceiptLine.TAX_TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.TAX_CODE_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.DEPARTMENT_CODE_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.CREATION_TIME_FIELD+" text,").append(NEWLINE)
		.append(LegacyReceiptLine.WAREHOUSE_ID_FIELD+" integer,").append(NEWLINE)
		.append(LegacyReceiptLine.USER_ID_FIELD+" integer,").append(NEWLINE)
		.append(LegacyReceiptLine.ECR_ACTIVE_FIELD+" integer").append(NEWLINE)
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
	
	private static class ResetPropsHandler implements CommandHandler<Message>
	{
		private Logger log;
		
		public ResetPropsHandler(final Logger log)
		{
			this.log = log;
		}

		@Override
		public boolean handle(final Message msg)
		{
			try
			{
				if (Objects.equals(JMSMessageType.REFRESH.toString(), msg.getStringProperty(JMS_MESSAGE_TYPE_KEY)) &&
						msg instanceof TextMessage)
				{
					final TextMessage textMessage = (TextMessage) msg;
					if (globalIsMatch(textMessage.getText(), REFRESH_ALL_PERSISTED_PROPS, TextFilterMethod.EQUALS))
					{
						ClientSession.instance().resetPersistedPropsCache();
						return true;
					}
				}
				return false;
			}
			catch (final Exception e)
			{
				log.error(e);
				return false;
			}
		}
	}
	
	private static class WaitOrderReceivedHandler implements CommandHandler<Message>
	{
		private Logger log;
		private UISynchronize sync;

		public WaitOrderReceivedHandler(final Logger log, final UISynchronize sync)
		{
			this.log = log;
			this.sync = sync;
		}

		@Override
		public boolean handle(final Message msg)
		{
			try
			{
				if (Objects.equals(JMSMessageType.WAIT_ORDER_RECEIVED.toString(), msg.getStringProperty(JMS_MESSAGE_TYPE_KEY)) &&
						msg instanceof ObjectMessage)
				{
					final ObjectMessage objMessage = (ObjectMessage) msg;
					final AccountingDocument accDoc = (AccountingDocument) objMessage.getObject();
					final Long opId = parseToLong(objMessage.getStringProperty(InvocationResult.OPERATIONS_KEY));
					final String opName = accDoc.getOperatiuni_Stream()
							.filter(o -> o.getId().equals(opId))
							.findFirst()
							.map(Operatiune::getName)
							.orElse(EMPTY_STRING);

					NotificationManager.showNotification("Marfa primita "+
							safeString(accDoc, AccountingDocument::getPartner, Partner::getName),
							MessageFormat.format("{0}. Click pentru a deschide!", truncate(opName, 24)),
							ev -> sync.asyncExec(() -> VerifyOperationsPart.loadDoc(getEPartService(), accDoc)));
					return true;
				}
				return false;
			}
			catch (final Exception e)
			{
				log.error(e);
				return false;
			}
		}
	}
}
