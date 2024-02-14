package ro.linic.ui.p2.internal.ui.dialogs;

import ro.linic.ui.p2.internal.ui.model.ProvElement;
import ro.linic.ui.p2.internal.ui.viewers.IUDetailsLabelProvider;

public class CopyUtils {
	public static final String NEWLINE = System.lineSeparator();
	public static final String DELIMITER = "\t"; //$NON-NLS-1$
	private static final String NESTING_INDENT = "  "; //$NON-NLS-1$

	public static String getIndentedClipboardText(final Object[] elements, final IUDetailsLabelProvider labelProvider) {
		final StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			if (i > 0)
				buffer.append(NEWLINE);
			appendIndention(buffer, elements[i]);
			buffer.append(labelProvider.getClipboardText(elements[i], DELIMITER));
		}
		return buffer.toString();
	}

	private static void appendIndention(final StringBuilder buffer, Object element) {
		Object parent;
		while (element instanceof ProvElement && (parent = ((ProvElement) element).getParent(element)) != null) {
			buffer.append(NESTING_INDENT);
			element = parent;
		}
	}
}