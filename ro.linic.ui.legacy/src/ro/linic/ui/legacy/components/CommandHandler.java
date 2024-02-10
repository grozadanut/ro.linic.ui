package ro.linic.ui.legacy.components;

@FunctionalInterface
public interface CommandHandler<T>
{
	/**
	 * Handles the given command and returns true if the command has been consumed, 
	 * and should not be passed further, false if the command was not consumed and 
	 * can be passed further to other handlers.
	 * 
	 * @param command the command that should be handled
	 * @return true if the command was consumed, false if it can be passed further
	 */
	public boolean handle(T command);
}