package ro.linic.ui.base.services;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName()+".messages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	public static String Summary;
	public static String Delete;
	public static String DeleteConfirm;
	public static String Save;
	public static String SaveConfirm;
	public static String ExportExcel;
	public static String Error;
}
