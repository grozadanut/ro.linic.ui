package ro.linic.ui.legacy.components;

public interface AsyncLoadResult<E>
{
	void success(E result);
	void error(String details);
}
