package ro.linic.ui.p2.internal.ui;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * ProvUIImages provides convenience methods for accessing shared images
 * provided by the <i>org.eclipse.equinox.internal.provisional.p2.ui</i> plug-in.
 * <p>
 * This class provides <code>ImageDescriptor</code>s for each named image in
 * {@link ProvUIImages}. All <code>Image</code> objects created from the
 * provided descriptors are managed the caller and must be disposed
 * appropriately.
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients
 *
 * @since 3.4
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ProvUIImages {

	// bundle-relative icon path
	public final static String ICON_PATH = "$nl$/icons/"; //$NON-NLS-1$
	//objects
	public final static String IMG_ARTIFACT_REPOSITORY = "obj/artifact_repo_obj.png"; //$NON-NLS-1$
	public final static String IMG_METADATA_REPOSITORY = "obj/metadata_repo_obj.png"; //$NON-NLS-1$
	public final static String IMG_IU = "obj/iu_obj.png"; //$NON-NLS-1$
	public final static String IMG_DISABLED_IU = "obj/iu_disabled_obj.png"; //$NON-NLS-1$
	public final static String IMG_ADDED = "obj/iu_add.png"; //$NON-NLS-1$
	public final static String IMG_REMOVED = "obj/iu_remove.png"; //$NON-NLS-1$
	public final static String IMG_CHANGED = "obj/iu_update_obj.png"; //$NON-NLS-1$
	public final static String IMG_NOTADDED = "obj/iu_notadd.png"; //$NON-NLS-1$

	public final static String IMG_UPDATED_IU = "obj/iu_update_obj.png"; //$NON-NLS-1$
	public final static String IMG_UPGRADED_IU = "obj/iu_upgraded.png"; //$NON-NLS-1$
	public final static String IMG_DOWNGRADED_IU = "obj/iu_downgraded.png"; //$NON-NLS-1$
	public final static String IMG_ADDED_OVERLAY = "ovr/added_overlay.png"; //$NON-NLS-1$
	public final static String IMG_REMOVED_OVERLAY = "ovr/removed_overlay.png"; //$NON-NLS-1$
	public final static String IMG_PATCH_IU = "obj/iu_patch_obj.png"; //$NON-NLS-1$
	public final static String IMG_DISABLED_PATCH_IU = "obj/iu_disabled_patch_obj.png"; //$NON-NLS-1$
	public final static String IMG_PROFILE = "obj/profile_obj.png"; //$NON-NLS-1$
	public final static String IMG_CATEGORY = "obj/category_obj.png"; //$NON-NLS-1$
	public final static String IMG_INFO = "obj/iu_info.png"; //$NON-NLS-1$
	public final static String IMG_COPY = "obj/copy_edit.png"; //$NON-NLS-1$

	// wizard graphics
	public final static String WIZARD_BANNER_INSTALL = "wizban/install_wiz.png"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_UNINSTALL = "wizban/uninstall_wiz.png"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_UPDATE = "wizban/update_wiz.png"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_REVERT = "wizban/revert_wiz.png"; //$NON-NLS-1$

	/**
	 * Returns the image descriptor for the given image ID. Returns
	 * <code>null</code> if there is no such image.
	 *
	 * @param id
	 *            the identifier for the image to retrieve
	 * @return the image descriptor associated with the given ID
	 */
	public static ImageDescriptor getImageDescriptor(final IEclipseContext ctx, final String id) {
		final ImageRegistry imageRegistry = (ImageRegistry) ctx.get(ProvUIAddon.IMG_REGISTRY);
		return imageRegistry.getDescriptor(id);
	}

	/**
	 * Returns the image for the given image ID. Returns <code>null</code> if
	 * there is no such image.
	 *
	 * @param id
	 *            the identifier for the image to retrieve
	 * @return the image associated with the given ID. This image is managed in
	 *         an image registry and should not be freed by the client.
	 */
	public static Image getImage(final IEclipseContext ctx, final String id) {
		final ImageRegistry imageRegistry = (ImageRegistry) ctx.get(ProvUIAddon.IMG_REGISTRY);
		return imageRegistry.get(id);
	}
}
