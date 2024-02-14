package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.Dialog;

/**
 * Element class representing an explanation for no children appearing beneath
 * an element.
 *
 * @since 3.5
 */
public class EmptyElementExplanation extends ProvElement {

	String explanation;
	int severity;
	String description;

	/**
	 * Create an empty element explanation
	 *
	 * @param parent      the parent of this element
	 * @param severity    the severity of the explanation {@link IStatus#INFO},
	 * @param explanation
	 */
	public EmptyElementExplanation(final IEclipseContext ctx, final Object parent, final int severity, final String explanation,
			final String description) {
		super(ctx, parent);
		this.explanation = explanation;
		this.severity = severity;
		this.description = description;
	}

	@Override
	protected String getImageId(final Object obj) {
		if (severity == IStatus.ERROR)
			return Dialog.DLG_IMG_MESSAGE_ERROR;
		if (severity == IStatus.WARNING)
			return Dialog.DLG_IMG_MESSAGE_WARNING;
		return Dialog.DLG_IMG_MESSAGE_INFO;
	}

	@Override
	public String getLabel(final Object o) {
		return explanation;
	}

	@Override
	public Object[] getChildren(final Object o) {
		return new Object[0];
	}

	public String getDescription() {
		return description;
	}
}
