package ro.linic.ui.base.services.impl;

import static ro.flexbiz.util.commons.PresentationUtils.safeString;
import static ro.flexbiz.util.commons.StringUtils.isEmpty;
import static ro.flexbiz.util.commons.StringUtils.notEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import ro.linic.ui.base.services.MessagingService;
import ro.linic.ui.base.services.preferences.PreferenceKey;
import ro.linic.ui.base.services.util.UIUtils;

@Component(scope = ServiceScope.SINGLETON)
public class MessagingServiceImpl implements MessagingService {
	private static ILog log = ILog.of(MessagingServiceImpl.class);
	
	private Connection nc;

	@Activate
	private void activate() throws IOException, InterruptedException {
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode("ro.linic.ui.base");
		final String natsUrl = safeString(prefs.get(PreferenceKey.NATS_URL, System.getProperty(PreferenceKey.NATS_URL)),
						System.getenv(PreferenceKey.NATS_URL));
		
		if (isEmpty(natsUrl)) {
			log.warn("'natsUrl' is empty, NOT activating NATS messaging service!");
			return;
		}
		
		final Options options = new Options.Builder()
				.server(natsUrl)
				.noEcho() // don't send message back to me
				.build();
		nc = Nats.connect(options);
	}
	
	@Deactivate
	private void deactivate() throws InterruptedException {
		if (nc != null)
			nc.close();
	}
	
	@Override
	public void sendMessage(final String tenandId, final String subject, final String body) {
		if (nc == null)
			return;
		
		nc.publish(tenandId+"."+subject, body == null ? null : body.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public void sendMessage(final String tenandId, final String subject, final Serializable body) {
		if (nc == null)
			return;
		
		nc.publish(tenandId+"."+subject, UIUtils.serialize(body));
	}
	
	@Override
	public void sendMessage(final String tenandId, final String userId, final String subject, final Serializable body) {
		if (nc == null)
			return;
		
		nc.publish(tenandId+"."+userId+"."+subject, UIUtils.serialize(body));
	}
	
	@Override
	public Optional<Message> requestReply(final String tenandId, final String subject, final Serializable body, final Duration timeout) {
		return requestReply(tenandId, null, subject, body, timeout);
	}
	
	@Override
	public void sendReply(final Message replyTo, final Serializable body) {
		if (nc == null)
			return;
		
		nc.publish(replyTo.getReplyTo(), UIUtils.serialize(body));
	}
	
	@Override
	public Optional<Message> requestReply(final String tenandId, final String userId, final String subject, final Serializable body, final Duration timeout) {
		if (nc == null)
			return Optional.empty();
		
		try {
			final StringBuilder sb = new StringBuilder(tenandId);
			if (notEmpty(userId))
				sb.append("."+userId);
			sb.append("."+subject);
			return Optional.ofNullable(nc.request(sb.toString(), UIUtils.serialize(body), timeout));
		} catch (final InterruptedException e) {
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
	}
	
	@Override
	public void subscribe(final String tenandId, final String subject, final MessageHandler handler) {
		if (nc == null)
			return;
		
		nc.createDispatcher(handler).subscribe(tenandId+"."+subject);
	}
	
	@Override
	public void subscribe(final String tenandId, final String userId, final String subject, final MessageHandler handler) {
		if (nc == null)
			return;
		
		nc.createDispatcher(handler).subscribe(tenandId+"."+userId+"."+subject);
	}
}
