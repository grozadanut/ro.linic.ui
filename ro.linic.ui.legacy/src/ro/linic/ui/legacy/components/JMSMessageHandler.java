package ro.linic.ui.legacy.components;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;

public class JMSMessageHandler extends MultiCommandHandler<Message> implements MessageListener
{
	@Override
	public void onMessage(final Message message)
	{
		handle(message);
	}
}