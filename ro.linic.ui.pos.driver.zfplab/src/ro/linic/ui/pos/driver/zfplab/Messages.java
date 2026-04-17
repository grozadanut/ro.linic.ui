package ro.linic.ui.pos.driver.zfplab;

import org.eclipse.osgi.util.NLS;

public class Messages {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
	
	public static String ErrorECRDriver_SetIp;
	public static String ZFPECRDriverPage_ServerAddress;
	public static String ZFPECRDriverPage_Ip;
	public static String ZFPECRDriverPage_Port;
	public static String ZFPECRDriverPage_Password;
}
