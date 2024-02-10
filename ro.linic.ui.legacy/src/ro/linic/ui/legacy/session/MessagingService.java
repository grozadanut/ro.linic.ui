package ro.linic.ui.legacy.session;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.ServerConstants.COMPANY_ID_KEY;
import static ro.colibri.util.ServerConstants.JMS_GESTIUNI_KEY;
import static ro.colibri.util.ServerConstants.JMS_MESSAGE_TYPE_KEY;
import static ro.colibri.util.ServerConstants.JMS_USERS_KEY;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.e4.core.services.log.Logger;

import com.google.common.collect.ImmutableMap;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import ro.colibri.util.ServerConstants.JMSMessageType;

public class MessagingService
{
	private static MessagingService instance;
	
	private Connection jmsConnection;
	private Session jmsSession;
	private boolean connectionStarted = false;
	private HashMap<String, MessageListener> listenersByTopic = new HashMap<>();
	
	public static MessagingService instance()
	{
		if (instance == null)
			instance = new MessagingService();
		
		return instance;
	}
	
	private MessagingService()
	{
	}
	
	public synchronized Optional<Connection> jmsConnection(final Logger log)
	{
		if (jmsConnection == null)
		{
			try
			{
				if (!ClientSession.instance().isLoggedIn())
					throw new UnsupportedOperationException("jmsConnection() cannot be called before login");
				
				final Properties jndiProps = new Properties();
				jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, ClientSession.INITIAL_CONTEXT_FACTORY_VALUE);
				jndiProps.put(Context.PROVIDER_URL, ClientSession.PROVIDER_URL_VALUE);
				jndiProps.put(Context.SECURITY_PRINCIPAL, ClientSession.instance().getUsername());
				jndiProps.put(Context.SECURITY_CREDENTIALS, ClientSession.instance().getPassword());
				final InitialContext ic = new InitialContext(jndiProps);
				final ConnectionFactory connF = (ConnectionFactory) ic.lookup("/jms/RemoteConnectionFactory");
				if (connF instanceof ActiveMQConnectionFactory)
					((ActiveMQConnectionFactory) connF).setReconnectAttempts(-1);
				jmsConnection = connF.createConnection(ClientSession.instance().getUsername(),
						ClientSession.instance().getPassword());
				ic.close();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				if (log != null)
					log.error(e);
			}
		}
		
		return Optional.ofNullable(jmsConnection);
	}
	
	public synchronized Optional<Session> jmsSession(final Logger log)
	{
		if (jmsSession == null)
		{
			try
			{
				final Optional<Connection> jmsConn = jmsConnection(log);
				if (jmsConn.isPresent())
					jmsSession = jmsConn.get().createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				if (log != null)
					log.error(e);
			}
		}
		
		return Optional.ofNullable(jmsSession);
	}
	
	public synchronized void closeSession(final Logger log)
	{
		try
		{
			if (jmsSession != null)
				jmsSession.close();
			if (jmsConnection != null)
				jmsConnection.close();
			connectionStarted = false;
			jmsSession = null;
			jmsConnection = null;
			
			if (log != null)
				log.debug("JMS Session and Connection closed");
			else
				System.out.println("JMS Session and Connection closed");
		}
		catch (final Exception e)
		{
			if (log != null)
				log.error(e);
			else
				e.printStackTrace();
		}
	}
	
	public synchronized void restoreListeners(final Logger log)
	{
		for (final Entry<String, MessageListener> listenerByTopic : listenersByTopic.entrySet())
			registerMsgListener(listenerByTopic.getKey(), listenerByTopic.getValue(), false, log);
	}
	
	public void registerMsgListener(final String topicJndi, final MessageListener listener, final Logger log)
	{
		registerMsgListener(topicJndi, listener, true, log);
	}
	
	private void registerMsgListener(final String topicJndi, final MessageListener listener, final boolean addListener, final Logger log)
	{
		try
		{
			final String gestId = String.valueOf(ClientSession.instance().getLoggedUser().getSelectedGestiune().getId());
			final String userId = String.valueOf(ClientSession.instance().getLoggedUser().getId());

			final StringBuilder messageSelector = new StringBuilder()
			.append(COMPANY_ID_KEY+" = "+ClientSession.instance().getLoggedUser().getSelectedCompany().getId()).append(" AND ")
			.append("(").append(JMS_GESTIUNI_KEY+" IS NULL OR "+
					JMS_GESTIUNI_KEY+" LIKE '"+gestId+"' OR "+ // single id
					JMS_GESTIUNI_KEY+" LIKE '%"+LIST_SEPARATOR+gestId+LIST_SEPARATOR+"%' OR "+ // middle
					JMS_GESTIUNI_KEY+" LIKE '%"+LIST_SEPARATOR+gestId+"' OR "+ // end
					JMS_GESTIUNI_KEY+" LIKE '"+gestId+LIST_SEPARATOR+"%'").append(")").append(" AND ") // start
			.append("(").append(JMS_USERS_KEY+" IS NULL OR "+
					JMS_USERS_KEY+" LIKE '"+userId+"' OR "+ // single id
					JMS_USERS_KEY+" LIKE '%"+LIST_SEPARATOR+userId+LIST_SEPARATOR+"%' OR "+ // middle
					JMS_USERS_KEY+" LIKE '%"+LIST_SEPARATOR+userId+"' OR "+ // end
					JMS_USERS_KEY+" LIKE '"+userId+LIST_SEPARATOR+"%'").append(")"); // start
			
			final Properties jndiProps = new Properties();
			jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, ClientSession.INITIAL_CONTEXT_FACTORY_VALUE);
			jndiProps.put(Context.PROVIDER_URL, ClientSession.PROVIDER_URL_VALUE);
			jndiProps.put(Context.SECURITY_PRINCIPAL, ClientSession.instance().getUsername());
			jndiProps.put(Context.SECURITY_CREDENTIALS, ClientSession.instance().getPassword());
			final InitialContext ic = new InitialContext(jndiProps);
			final Topic topic = (Topic) ic.lookup(topicJndi);
			ic.close();
			final MessageConsumer consumer = jmsSession(log).get().createConsumer(topic, messageSelector.toString(), true);
			consumer.setMessageListener(listener);

			if (!connectionStarted)
			{
				jmsConnection(log).get().start();
				connectionStarted = true;
			}
			if (addListener)
				listenersByTopic.put(topicJndi, listener);
		}
		catch (final Exception e)
		{
			if (log != null)
				log.error(e);
			else
				e.printStackTrace();
		} 
	}
	
	public void sendMsg(final String topicJndi, final JMSMessageType messageType, final ImmutableMap<String, String> properties,
			final Message message, final Logger log)
	{
		try
		{
			final Properties jndiProps = new Properties();
			jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, ClientSession.INITIAL_CONTEXT_FACTORY_VALUE);
			jndiProps.put(Context.PROVIDER_URL, ClientSession.PROVIDER_URL_VALUE);
			jndiProps.put(Context.SECURITY_PRINCIPAL, ClientSession.instance().getUsername());
			jndiProps.put(Context.SECURITY_CREDENTIALS, ClientSession.instance().getPassword());
			final InitialContext ic = new InitialContext(jndiProps);
			final Topic topic = (Topic) ic.lookup(topicJndi);
			ic.close();
			final MessageProducer publisher = jmsSession(log).get().createProducer(topic);
			message.setIntProperty(COMPANY_ID_KEY, ClientSession.instance().getLoggedUser().getSelectedCompany().getId());
			message.setStringProperty(JMS_MESSAGE_TYPE_KEY, messageType.toString());
			for (final Entry<String, String> prop : properties.entrySet())
				message.setStringProperty(prop.getKey(), prop.getValue());
			publisher.send(message);
			publisher.close();
		}
		catch (final Exception e)
		{
			log.error(e);
			showException(e);
		} 
	}
	
	public void sendMsg(final String topicJndi, final JMSMessageType messageType, final ImmutableMap<String, String> properties,
			final Serializable objectToSend, final Logger log)
	{
		try
		{
			sendMsg(topicJndi, messageType, properties, jmsSession(log).get().createObjectMessage(objectToSend), log);
		}
		catch (final Exception e)
		{
			log.error(e);
			showException(e);
		} 
	}
}
