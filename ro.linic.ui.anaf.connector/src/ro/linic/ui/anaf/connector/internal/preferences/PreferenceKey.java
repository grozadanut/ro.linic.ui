package ro.linic.ui.anaf.connector.internal.preferences;

public interface PreferenceKey {
	/**
	 * Value is boolean. Whether we should validate the xml when converting xml to pdf with Anaf api
	 */
	public static final String XML_TO_PDF_VALIDATE = "xml_to_pdf_validate";
	public static final boolean XML_TO_PDF_VALIDATE_DEF = true;
}
