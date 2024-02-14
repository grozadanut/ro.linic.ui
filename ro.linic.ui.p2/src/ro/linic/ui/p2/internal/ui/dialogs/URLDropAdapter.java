package ro.linic.ui.p2.internal.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.URLTransfer;

/**
 * URLDropAdapter can receive URL text from a drop.
 * The URLDropAdapter should only be used with
 * the URLTransfer mechanism unless otherwise stated.
 *
 * @since 3.4
 *
 */
public abstract class URLDropAdapter extends DropTargetAdapter {

	private boolean convertFileToURL = false;

	protected URLDropAdapter(final boolean convertFileToURL) {
		this.convertFileToURL = convertFileToURL;
	}

	@Override
	public void dragEnter(final DropTargetEvent e) {
		if (!dropTargetIsValid(e)) {
			e.detail = DND.DROP_NONE;
			return;
		}
		if (e.detail == DND.DROP_NONE)
			e.detail = DND.DROP_LINK;
	}

	@Override
	public void dragOperationChanged(final DropTargetEvent e) {
		if (e.detail == DND.DROP_NONE)
			e.detail = DND.DROP_LINK;
	}

	@Override
	public void drop(final DropTargetEvent event) {
		if (dropTargetIsValid(event)) {
			final String urlText = getURLText(event);
			if (urlText != null) {
				handleDrop(urlText, event);
				return;
			}
		}
		event.detail = DND.DROP_NONE;
	}

	private String getURLText(final DropTargetEvent event) {
		if (URLTransfer.getInstance().isSupportedType(event.currentDataType))
			return (String) URLTransfer.getInstance().nativeToJava(event.currentDataType);
		if (convertFileToURL && FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			final String[] names = (String[]) FileTransfer.getInstance().nativeToJava(event.currentDataType);
			if (names != null && names.length == 1) {
				URI potentialLocation;
				try {
					potentialLocation = URIUtil.fromString(names[0]);
					return URIUtil.toUnencodedString(RepositoryHelper.localRepoURIHelper(potentialLocation));
				} catch (final URISyntaxException e) {
					return names[0];
				}
			}
		}
		return null;
	}

	/**
	 * Determine whether the drop target is valid.  Subclasses may override.
	 * @param event the drop target event
	 * @return <code>true</code> if drop should proceed, <code>false</code> if it should not.
	 */
	protected boolean dropTargetIsValid(final DropTargetEvent event) {
		if (URLTransfer.getInstance().isSupportedType(event.currentDataType) && dropTargetDataIsValid(event))
			return true;
		if (!convertFileToURL)
			return false;
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			final String[] names = (String[]) FileTransfer.getInstance().nativeToJava(event.currentDataType);
			return names != null && names.length == 1;
		}
		return false;
	}

	/**
	 * Determine whether the drop target data is valid.  On some platforms this cannot be detected,
	 * in which which case we return true.
	 * @param event the drop target event
	 * @return <code>true</code> if data is valid, (or can not be determined), <code>false</code> otherwise.
	 */
	protected boolean dropTargetDataIsValid(final DropTargetEvent event) {
		if (Util.isWindows())
			return URLTransfer.getInstance().nativeToJava(event.currentDataType) != null;
		return true;
	}

	/**
	 * Handle the drop with the given text as the URL.
	 * @param urlText The url text specified by the drop.  It is never <code>null</code>.
	 * @param event the originating drop target event.
	 */
	protected abstract void handleDrop(String urlText, DropTargetEvent event);
}
