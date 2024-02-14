package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Text;

/**
 * @since 3.4
 *
 */
public class TextURLDropAdapter extends URLDropAdapter {

	Text text;

	public TextURLDropAdapter(final Text text, final boolean convertFileToURL) {
		super(convertFileToURL);
		this.text = text;
	}

	@Override
	protected void handleDrop(final String urlText, final DropTargetEvent event) {
		text.setText(urlText);
		event.detail = DND.DROP_LINK;
	}

}
