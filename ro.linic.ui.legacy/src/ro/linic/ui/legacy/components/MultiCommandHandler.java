package ro.linic.ui.legacy.components;

import java.util.ArrayList;
import java.util.List;

public class MultiCommandHandler<T> implements CommandHandler<T>
{
	private List<CommandHandler<T>> handlers = new ArrayList<>();

	@Override
	public boolean handle(final T command)
	{
		for (final CommandHandler<T> handler : handlers)
			if (handler.handle(command))
				return true;
		return false;
	}
	
	public void registerHandler(final CommandHandler<T> handler)
	{
		handlers.add(handler);
	}
}