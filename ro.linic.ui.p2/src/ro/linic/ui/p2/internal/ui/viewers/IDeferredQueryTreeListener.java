package ro.linic.ui.p2.internal.ui.viewers;

import java.util.EventListener;

/**
 * A listening interface used to signal when fetching begins and
 * ends.  Used by clients who wish to coordinate fetching with other
 * capabilities of the viewer.
 *
 * @since 3.4
 *
 */
public interface IDeferredQueryTreeListener extends EventListener {

	public void fetchingDeferredChildren(Object parent, Object placeHolder);

	public void finishedFetchingDeferredChildren(Object parent, Object placeHolder);
}
