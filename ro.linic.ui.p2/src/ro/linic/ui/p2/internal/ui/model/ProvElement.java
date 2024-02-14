package ro.linic.ui.p2.internal.ui.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.org.eclipse.ui.IDeferredWorkbenchAdapter;
import ro.linic.ui.p2.org.eclipse.ui.IWorkbenchAdapter;
import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Generic element that represents a provisioning element in a viewer.
 *
 * @since 3.4
 *
 */
public abstract class ProvElement implements IWorkbenchAdapter, IAdaptable {

	private Object parent;
	protected IEclipseContext ctx;

	public ProvElement(final IEclipseContext ctx, final Object parent) {
		this.parent = parent;
		this.ctx = ctx;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return (T) this;
		if ((adapter == IDeferredWorkbenchAdapter.class) && this instanceof IDeferredWorkbenchAdapter)
			return (T) this;
		return null;
	}

	/**
	 * Return a string id of the image that should be used to show the specified
	 * object. Returning null indicates that no image should be used.
	 *
	 * @param obj the object whose image id is requested
	 * @return the string id of the image in the provisioning image registry, or
	 *         <code>null</code> if no image should be shown.
	 */
	protected String getImageId(final Object obj) {
		return null;
	}

	protected String getImageOverlayId(final Object obj) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(final Object object) {
		final String id = getImageId(object);
		if (id == null) {
			return null;
		}
		ImageDescriptor desc = ProvUIImages.getImageDescriptor(ctx, id);
		if (desc == null)
			desc = JFaceResources.getImageRegistry().getDescriptor(id);
		return desc;
	}

	/**
	 * Return the image that should be used to show the specfied object. The image
	 * is managed by an image registry and should not be freed.
	 *
	 * @param object the object whose image id is requested
	 * @return the string id of the image in the provisioning image registry
	 *
	 */
	public Image getImage(final Object object) {
		final String id = getImageId(object);
		if (id == null) {
			return null;
		}
		Image img = ProvUIImages.getImage(ctx, id);
		if (img == null)
			img = JFaceResources.getImageRegistry().get(id);
		final String overlayId = getImageOverlayId(object);
		if (overlayId == null)
			return img;
		final ImageDescriptor overlay = ProvUIImages.getImageDescriptor(ctx, overlayId);
		final String decoratedImageId = id.concat(overlayId);
		if (ProvUIImages.getImage(ctx, decoratedImageId) == null) {
			final DecorationOverlayIcon decoratedImage = new DecorationOverlayIcon(img, overlay, IDecoration.BOTTOM_RIGHT);
			((ImageRegistry) ctx.get(ProvUIAddon.IMG_REGISTRY)).put(decoratedImageId, decoratedImage);
		}
		final Image decoratedImg = ProvUIImages.getImage(ctx, decoratedImageId);
		if (decoratedImg == null)
			return img;
		return decoratedImg;
	}

	protected void handleException(final Exception e, String message) {
		if (message == null) {
			message = e.getMessage();
		}
		final IStatus status = new Status(IStatus.ERROR, ProvUIAddon.PLUGIN_ID, 0, message, e);
		ProvUI.reportStatus(ctx, status, StatusManager.LOG);
	}

	public boolean hasChildren(final Object o) {
		if (this instanceof IDeferredWorkbenchAdapter)
			return ((IDeferredWorkbenchAdapter) this).isContainer();
		final Object[] children = getChildren(o);
		if (children == null) {
			return false;
		}
		return children.length > 0;
	}

	@Override
	public Object getParent(final Object o) {
		return parent;
	}
}
