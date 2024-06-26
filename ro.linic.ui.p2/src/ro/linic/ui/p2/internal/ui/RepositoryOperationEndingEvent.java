package ro.linic.ui.p2.internal.ui;

import java.util.EventObject;

import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;

/**
 * Event used to signal that a repository operation has completed.
 *
 * @since 2.0
 */
public class RepositoryOperationEndingEvent extends EventObject {

	private static final long serialVersionUID = -4513769756968621852L;

	/**
	 * A repository event describing the nature of the operation.
	 */
	private RepositoryEvent event;

	/**
	 * A boolean indicating whether the UI should be updated in response
	 * to this event.
	 */
	private boolean update;

	/**
	 * Construct a new instance of this event.
	 *
	 * @param source the source of the event
	 * @param update a boolean indicating whether the UI should be updated in response
	 * to this event.
	 * @param event a {@link RepositoryEvent} describing the underlying event, or <code>null</code>
	 * if no single event can describe the operation. This event may be used by clients to determine
	 * what should be updated after an operation completes.
	 */
	public RepositoryOperationEndingEvent(final Object source, final boolean update, final RepositoryEvent event) {
		super(source);
		this.update = update;
		this.event = event;
	}

	/**
	 * Return a {@link RepositoryEvent} that reflects the operation that
	 * occurred.  A <code>null</code> return value indicates that there
	 * was not a single underlying repository operation.
	 *
	 * @return the {@link RepositoryEvent} that was involved in the operation.  May be
	 * <code>null</code>.  This event may be used by clients to determine
	 * what should be updated after an operation completes.

	 */
	public RepositoryEvent getEvent() {
		return event;
	}

	/**
	 * Return a boolean that indicates whether the client should update the UI in response
	 * to this event.
	 * event.
	 *
	 * @return <code>true</code> if clients should update to reflect to this event, <code>false</code>
	 * if the client should ignore the entire operation.
	 */
	public boolean update() {
		return update;
	}

}
