package ro.linic.ui.base.services;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;

import io.nats.client.Message;
import io.nats.client.MessageHandler;

public interface MessagingService {
	public void sendMessage(String tenantId, String subject, String body);
	public void sendMessage(String tenantId, String subject, Serializable body);
	public void sendMessage(String tenantId, String userId, String subject, Serializable body);
	public Optional<Message> requestReply(String tenantId, String subject, Serializable body, Duration timeout);
	public Optional<Message> requestReply(String tenantId, String userId, String subject, Serializable body, Duration timeout);
	public void sendReply(Message replyTo, Serializable body);
	public void subscribe(String tenantId, String subject, MessageHandler handler);
	public void subscribe(String tenantId, String userId, String subject, MessageHandler handler);
}
