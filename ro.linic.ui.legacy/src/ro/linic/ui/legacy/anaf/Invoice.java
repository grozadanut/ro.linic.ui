package ro.linic.ui.legacy.anaf;

import java.time.LocalDate;

public interface Invoice<T extends InvoiceUblType> {
	Long getId();
	String getUploadIndex();
	String getDownloadId();
	String getXmlRaw();
	LocalDate getIssueDate();
	Long getInvoiceId();
	T getUblType();
}
