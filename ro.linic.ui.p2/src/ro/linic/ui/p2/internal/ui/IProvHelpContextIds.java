package ro.linic.ui.p2.internal.ui;

import ro.linic.ui.p2.ui.addons.ProvUIAddon;

/**
 * Help context ids for the P2 UI
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 *
 * @since 3.4
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */

public interface IProvHelpContextIds {
	String PREFIX = ProvUIAddon.PLUGIN_ID + "."; //$NON-NLS-1$

	String REVERT_CONFIGURATION_WIZARD = PREFIX + "revert_configuration_wizard_context"; //$NON-NLS-1$

	String UNINSTALL_WIZARD = PREFIX + "uinstall_wizard_context"; //$NON-NLS-1$

	String UPDATE_WIZARD = PREFIX + "update_wizard_context"; //$NON-NLS-1$

	String ADD_REPOSITORY_DIALOG = PREFIX + "add_repository_dialog_context"; //$NON-NLS-1$

	String INSTALL_WIZARD = PREFIX + "install_wizard_context"; //$NON-NLS-1$

	String REPOSITORY_MANIPULATION_DIALOG = PREFIX + "repository_manipulation_dialog_context"; //$NON-NLS-1$

	String INSTALLED_SOFTWARE = PREFIX + "installed_software_context"; //$NON-NLS-1$

	String AVAILABLE_SOFTWARE = PREFIX + "available_software_context"; //$NON-NLS-1$

	String TRUST_DIALOG = PREFIX + "trust_dialog_context"; //$NON-NLS-1$

	String TRUST_AUTHORITIES_DIALOG = PREFIX + "trust_authorities_dialog_context"; //$NON-NLS-1$
}
