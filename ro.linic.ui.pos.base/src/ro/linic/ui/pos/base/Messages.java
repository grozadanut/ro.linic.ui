package ro.linic.ui.pos.base;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
	
	public static String ECRDriver_PaymentSmallerThanTotalErr;
	public static String ECRDriver_PaymentTypeError;
	public static String DudeECRDriver_ConfigError;
	public static String DudeECRDriver_Error;
	public static String DudeECRDriver_ErrorWrittingFile;
	public static String DudeECRDriver_ErrorWrittingFileNLS;
	public static String DudeECRDriver_IllegalPaymentType;
	public static String DudeECRDriver_SetIp;
	public static String DudeECRDriverPage_AMEFNumber;
	public static String DudeECRDriverPage_Department;
	public static String DudeECRDriverPage_Folder;
	public static String DudeECRDriverPage_IP;
	public static String DudeECRDriverPage_Operator;
	public static String DudeECRDriverPage_Password;
	public static String DudeECRDriverPage_Port;
	public static String DudeECRDriverPage_VATRate;
	public static String DudeECRDriverPage_ZAndD;
	public static String ECRDriverPage_Driver;
	public static String ECRServiceImpl_DriverNotFound;
	public static String ECRServiceImpl_Timeout;
	public static String FiscalNetECRDriver_Error;
	public static String FiscalNetECRDriver_ErrorWrittingFile;
	public static String FiscalNetECRDriver_ErrorWrittingFileNLS;
	public static String FiscalNetECRDriver_OperationNotPermitted;
	public static String FiscalNetECRDriverPage_CommandFolder;
	public static String FiscalNetECRDriverPage_Department;
	public static String FiscalNetECRDriverPage_ResponseFolder;
	public static String FiscalNetECRDriverPage_VATRate;
	public static String ProductTypeMandatory;
	public static String NameMandatory;
	public static String UOMMandatory;
	public static String PriceMandatory;
	public static String QuantityMandatory;
	public static String CodeExists;
	public static String CloseReceipt;
	public static String CloseReceiptDialog_TaxCode;
	public static String CloseReceiptDialog_ReceiptTotal;
	public static String CloseReceiptDialog_OutstandingTotal;
	public static String CloseReceiptDialog_CloseButton;
	public static String CloseReceiptDialog_OutstandingTotalNotZero;
	public static String CloseReceiptDialog_NegativeNotAllowed;
	public static String Cancel;
}
