package ro.linic.ui.base.services;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;

import io.nats.client.Message;
import io.nats.client.MessageHandler;

public interface MessagingService {
	public void sendMessage(String tenandId, String subject, String body);
	public void sendMessage(String tenandId, String subject, Serializable body);
	public void sendMessage(String tenandId, String userId, String subject, Serializable body);
	public Optional<Message> requestReply(String tenandId, String subject, Serializable body, Duration timeout);
	public Optional<Message> requestReply(String tenandId, String userId, String subject, Serializable body, Duration timeout);
	public void sendReply(Message replyTo, Serializable body);
	public void subscribe(String tenandId, String subject, MessageHandler handler);
	public void subscribe(String tenandId, String userId, String subject, MessageHandler handler);
}
