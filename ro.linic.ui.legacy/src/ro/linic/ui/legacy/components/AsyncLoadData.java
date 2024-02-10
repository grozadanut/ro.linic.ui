package ro.linic.ui.legacy.components;

import com.google.common.collect.ImmutableList;

public interface AsyncLoadData<E>
{
	default void success(final ImmutableList<E> data)
	{
		// do nothing
	}
	default void success(final ImmutableList<E> data1, final ImmutableList<E> data2)
	{
		// do nothing
	}
	default void success(final ImmutableList<E> data1, final ImmutableList<E> data2, final ImmutableList<E> data3)
	{
		// do nothing
	}
	default void success(final ImmutableList<E> data1, final ImmutableList<E> data2, final ImmutableList<E> data3, final ImmutableList<E> data4)
	{
		// do nothing
	}
	void error(String details);
}