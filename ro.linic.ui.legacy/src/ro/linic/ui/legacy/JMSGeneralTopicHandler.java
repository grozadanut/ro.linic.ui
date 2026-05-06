package ro.linic.ui.legacy;

import static ro.colibri.util.NumberUtils.parseToLong;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.ServerConstants.REFRESH_ALL_PERSISTED_PROPS;
import static ro.linic.ui.legacy.session.UIUtils.getEPartService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.osgi.framework.Bundle;

import io.nats.client.MessageHandler;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.legacy.parts.VerifyOperationsPart;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.NotificationManager;

public class JMSGeneralTopicHandler {
	public static final String JMSMSGTYPE_LOG_REQUEST = "JMSMSGTYPE_LOG_REQUEST";

	public JMSGeneralTopicHandler(final Logger log, final Bundle bundle, final UISynchronize sync, final ro.linic.ui.base.services.MessagingService nats) {
		nats.subscribe(ClientSession.instance().getCompany().getId()+"", JMSMessageType.WAIT_ORDER_RECEIVED.toString(), new WaitOrderReceivedHandler(log, sync));
		nats.subscribe(ClientSession.instance().getCompany().getId()+"", REFRESH_ALL_PERSISTED_PROPS, new ResetPropsHandler(log));
		nats.subscribe(ClientSession.instance().getCompany().getId()+"", ClientSession.instance().getLoggedUser().getId()+"",
				JMSMSGTYPE_LOG_REQUEST, new LogRequestHandler(log, nats));
	}

	private static class ResetPropsHandler implements MessageHandler {
		private Logger log;

		public ResetPropsHandler(final Logger log) {
			this.log = log;
		}

		@Override
		public void onMessage(final io.nats.client.Message msg) throws InterruptedException {
			try {
				ClientSession.instance().resetPersistedPropsCache();
			} catch (final Exception e) {
				log.error(e);
			}
		}
	}

	private static class WaitOrderReceivedHandler implements MessageHandler {
		private Logger log;
		private UISynchronize sync;

		public WaitOrderReceivedHandler(final Logger log, final UISynchronize sync) {
			this.log = log;
			this.sync = sync;
		}

		@Override
		public void onMessage(final io.nats.client.Message msg) throws InterruptedException {
			try {
				final InvocationResult result = ro.linic.ui.legacy.session.UIUtils.deserialize(msg.getData());
				final AccountingDocument accDoc = result.extra(InvocationResult.ACCT_DOC_KEY);
				final Long opId = parseToLong(result.extraString(InvocationResult.OPERATIONS_KEY));
				final String opName = accDoc.getOperatiuni_Stream().filter(o -> o.getId().equals(opId)).findFirst()
						.map(Operatiune::getName).orElse(EMPTY_STRING);

				sync.asyncExec(() -> NotificationManager.showNotification(
						"Marfa primita " + safeString(accDoc, AccountingDocument::getPartner, Partner::getName),
						opName,
						ev -> VerifyOperationsPart.loadDoc(getEPartService(), accDoc)));
			} catch (final Exception e) {
				log.error(e);
			}
		}
	}
	
	private static class LogRequestHandler implements MessageHandler {
		private Logger log;
		private ro.linic.ui.base.services.MessagingService nats;

		public LogRequestHandler(final Logger log, final ro.linic.ui.base.services.MessagingService nats) {
			this.log = log;
			this.nats = nats;
		}

		@Override
		public void onMessage(final io.nats.client.Message msg) throws InterruptedException {
			try {
				final Path installPath = Paths.get(Platform.getInstallLocation().getURL().toURI());
				final Path logPath = Paths.get(installPath.toString(), "workspace-"+ClientSession.instance().getLoggedUser().getId(),
						".metadata", ".log");
				final String logData = Files.readString(logPath);
				final String bundleNicenames = UIUtils.bundleNicenames();

				nats.sendReply(msg, MessageFormat.format("{0}{1}{1}{1}{2}", bundleNicenames, PresentationUtils.NEWLINE, logData));
			} catch (final Exception e) {
				log.error(e);
			}
		}
	}
}
