package ro.linic.ui.legacy;

import static ro.colibri.util.NumberUtils.parseToLong;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.ServerConstants.GENERAL_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.JMS_MESSAGE_TYPE_KEY;
import static ro.colibri.util.ServerConstants.REFRESH_ALL_PERSISTED_PROPS;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.truncate;
import static ro.linic.ui.legacy.session.UIUtils.getEPartService;

import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.osgi.framework.Bundle;

import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.components.CommandHandler;
import ro.linic.ui.legacy.components.JMSMessageHandler;
import ro.linic.ui.legacy.parts.VerifyOperationsPart;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;
import ro.linic.ui.legacy.session.NotificationManager;

public class JMSGeneralTopicHandler {

	public JMSGeneralTopicHandler(final Logger log, final Bundle bundle, final UISynchronize sync) {
		final JMSMessageHandler messageHandler = new JMSMessageHandler();
		messageHandler.registerHandler(new ResetPropsHandler(log));
		messageHandler.registerHandler(new WaitOrderReceivedHandler(log, sync));
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
}
