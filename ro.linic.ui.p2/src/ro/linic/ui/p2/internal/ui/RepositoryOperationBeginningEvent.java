package ro.linic.ui.p2.internal.ui;

import java.util.EventObject;

/**
 * Event used to signal that a repository operation is about
 * to begin.  This event can be used to ignore lower-level repository events
 * until the operation is complete.
 *
 * @since 2.0
 */
public class RepositoryOperationBeginningEvent extends EventObject {

	private static final long serialVersionUID = -7529156836242774280L;

	/**
	 * Construct a new instance of this event.
	 * @param source the source of the event
	 */
	public RepositoryOperationBeginningEvent(final Object source) {
		super(source);
	}

}
