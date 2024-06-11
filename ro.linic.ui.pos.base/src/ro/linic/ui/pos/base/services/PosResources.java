package ro.linic.ui.pos.base.services;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import ro.linic.ui.pos.base.Messages;

public class PosResources {
	/**
	 * The JFace resource bundle; eagerly initialized.
	 */
	private static final ResourceBundle bundle = ResourceBundle
			.getBundle(Messages.class.getPackageName() + ".messages"); //$NON-NLS-1$
	
	/**
	 * Returns the resource object with the given key in POS's resource
	 * bundle. If there isn't any value under the given key, the key is
	 * returned.
	 *
	 * @param key
	 *            the resource name
	 * @return the string
	 */
	public static String getString(final String key) {
		try {
			return bundle.getString(key);
		} catch (final MissingResourceException e) {
			return key;
		}
	}
}
