package ro.linic.ui.pos;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	private Messages() {
	}
	
	public static String Name;
	public static String Price;
	public static String SKU;
	public static String Barcodes;
	public static String BarcodesLong;
	public static String UOM;
	public static String UOMHint;
	public static String IsStockable;
	public static String TaxCode;
	public static String DepartmentCode;
	public static String Search;
	public static String Quantity;
	public static String CloseCash;
	public static String ECRActive;
	public static String Offer;
	public static String TotalNoTax;
	public static String Tax;
	public static String TotalWithTax;
	public static String LoadReceipt;
	public static String Refresh;
	public static String ECRManager;
	public static String CancelReceipt;
	public static String DeleteLine;
	public static String CloseOther;
	public static String ProductType;
	public static String CreateProductDialog_Title;
	public static String CreateProductDialog_Message;
}
