package ro.linic.ui.legacy.preferences;

public interface PreferenceKey {
	public static final String NODE_PATH = "ro.linic.ui.legacy";
	/**
	 * String value. Specifies which dialog to use when registering Z report, the standard one, 
	 * or the one for cafe with sold cafes and other fields.
	 */
	public static final String REGISTER_Z_DIALOG_KEY = "register_z_dialog";
	/**
	 * value for REGISTER_Z_DIALOG_KEY specifying to use the default dialog
	 */
	public static final String REGISTER_Z_DIALOG_STANDARD_VALUE = "standard";
	/**
	 * value for REGISTER_Z_DIALOG_KEY specifying to use the cafe dialog
	 */
	public static final String REGISTER_Z_DIALOG_CAFE_VALUE = "cafe";
	
	public static final String TVA_PERCENT_KEY = "tva_percent";
}
