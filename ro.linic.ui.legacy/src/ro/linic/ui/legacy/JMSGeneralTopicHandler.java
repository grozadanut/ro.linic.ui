package ro.linic.ui.legacy;

import static ro.colibri.util.NumberUtils.parseToLong;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.ServerConstants.GENERAL_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.JMS_MESSAGE_TYPE_KEY;
import static ro.colibri.util.ServerConstants.JMS_USERS_KEY;
import static ro.colibri.util.ServerConstants.REFRESH_ALL_PERSISTED_PROPS;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.truncate;
import static ro.linic.ui.legacy.session.UIUtils.getEPartService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableMap;

import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.base.dialogs.InfoDialog;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.legacy.components.CommandHandler;
import ro.linic.ui.legacy.components.JMSMessageHandler;
import ro.linic.ui.legacy.parts.VerifyOperationsPart;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;
import ro.linic.ui.legacy.session.NotificationManager;

public class JMSGeneralTopicHandler {
	public static final String REPLY_TO = "REPLY_TO";
	public static final String JMSMSGTYPE_LOG_REQUEST = "JMSMSGTYPE_LOG_REQUEST";
	public static final String JMSMSGTYPE_LOG_RESPONSE = "JMSMSGTYPE_LOG_RESPONSE";

	public JMSGeneralTopicHandler(final Logger log, final Bundle bundle, final UISynchronize sync) {
		final JMSMessageHandler messageHandler = new JMSMessageHandler();
		messageHandler.registerHandler(new ResetPropsHandler(log));
		messageHandler.registerHandler(new WaitOrderReceivedHandler(log, sync));
		messageHandler.registerHandler(new LogRequestHandler(log));
		messageHandler.registerHandler(new LogReponseHandler(log, sync));
		MessagingService.instance().registerMsgListener(GENERAL_TOPIC_REMOTE_JNDI, messageHandler);
	}

	private static class ResetPropsHandler implements CommandHandler<Message> {
		private Logger log;

		public ResetPropsHandler(final Logger log) {
			this.log = log;
		}

		@Override
		public boolean handle(final Message msg) {
			try {
				if (Objects.equals(JMSMessageType.REFRESH.toString(), msg.getStringProperty(JMS_MESSAGE_TYPE_KEY))
						&& msg instanceof TextMessage) {
					final TextMessage textMessage = (TextMessage) msg;
					if (globalIsMatch(textMessage.getText(), REFRESH_ALL_PERSISTED_PROPS, TextFilterMethod.EQUALS)) {
						ClientSession.instance().resetPersistedPropsCache();
						return true;
					}
				}
				return false;
			} catch (final Exception e) {
				log.error(e);
				return false;
			}
		}
	}

	private static class WaitOrderReceivedHandler implements CommandHandler<Message> {
		private Logger log;
		private UISynchronize sync;

		public WaitOrderReceivedHandler(final Logger log, final UISynchronize sync) {
			this.log = log;
			this.sync = sync;
		}

		@Override
		public boolean handle(final Message msg) {
			try {
				if (Objects.equals(JMSMessageType.WAIT_ORDER_RECEIVED.toString(),
						msg.getStringProperty(JMS_MESSAGE_TYPE_KEY)) && msg instanceof ObjectMessage) {
					final ObjectMessage objMessage = (ObjectMessage) msg;
					final AccountingDocument accDoc = (AccountingDocument) objMessage.getObject();
					final Long opId = parseToLong(objMessage.getStringProperty(InvocationResult.OPERATIONS_KEY));
					final String opName = accDoc.getOperatiuni_Stream().filter(o -> o.getId().equals(opId)).findFirst()
							.map(Operatiune::getName).orElse(EMPTY_STRING);

					NotificationManager.showNotification(
							"Marfa primita " + safeString(accDoc, AccountingDocument::getPartner, Partner::getName),
							MessageFormat.format("{0}. Click pentru a deschide!", truncate(opName, 24)),
							ev -> sync.asyncExec(() -> VerifyOperationsPart.loadDoc(getEPartService(), accDoc)));
					return true;
				}
				return false;
			} catch (final Exception e) {
				log.error(e);
				return false;
			}
		}
	}
	
	private static class LogRequestHandler implements CommandHandler<Message> {
		private Logger log;

		public LogRequestHandler(final Logger log) {
			this.log = log;
		}

		@Override
		public boolean handle(final Message msg) {
			try {
				if (Objects.equals(JMSMSGTYPE_LOG_REQUEST, msg.getStringProperty(JMS_MESSAGE_TYPE_KEY))) {
					final String replyTo = msg.getStringProperty(REPLY_TO);
					if (Objects.equals(replyTo, "1")) // 1 is user id of danut
						sendLogs(replyTo);
					return true;
				}
				return false;
			} catch (final Exception e) {
				log.error(e);
				return false;
			}
		}

		private void sendLogs(final String replyTo) throws IOException, URISyntaxException {
			final Path installPath = Paths.get(Platform.getInstallLocation().getURL().toURI());
			final Path logPath = Paths.get(installPath.toString(), "workspace-"+ClientSession.instance().getLoggedUser().getId(),
					".metadata", ".log");
			final String logData = Files.readString(logPath);
			final String bundleNicenames = UIUtils.bundleNicenames();
			
			MessagingService.instance().sendMsg(GENERAL_TOPIC_REMOTE_JNDI, JMSMSGTYPE_LOG_RESPONSE,
					ImmutableMap.of(JMS_USERS_KEY, replyTo), MessageFormat.format("{0}{1}{1}{1}{2}", bundleNicenames, PresentationUtils.NEWLINE, logData));
		}
	}
	
	private static class LogReponseHandler implements CommandHandler<Message> {
		private Logger log;
		private UISynchronize sync;

		public LogReponseHandler(final Logger log, final UISynchronize sync) {
			this.log = log;
			this.sync = sync;
		}

		@Override
		public boolean handle(final Message msg) {
			try {
				if (Objects.equals(JMSMSGTYPE_LOG_RESPONSE, msg.getStringProperty(JMS_MESSAGE_TYPE_KEY))) {
					final ObjectMessage objMessage = (ObjectMessage) msg;
					final String logs = (String) objMessage.getObject();
					sync.asyncExec(() -> InfoDialog.open(Display.getDefault().getActiveShell(), "Logs", logs));
					return true;
				}
				return false;
			} catch (final Exception e) {
				log.error(e);
				return false;
			}
		}
	}
}
