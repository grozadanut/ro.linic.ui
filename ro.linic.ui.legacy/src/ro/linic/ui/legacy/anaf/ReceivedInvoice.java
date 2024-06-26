package ro.linic.ui.legacy.anaf;

import java.time.LocalDate;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import com.helger.ubl21.UBL21Reader;

public class ReceivedInvoice implements Invoice<InvoiceTypeDecorator> {
	private Long id;
	private String uploadIndex;
	private String downloadId;
	private String xmlRaw;
	private LocalDate issueDate;
	private Long invoiceId;
	
	@XmlTransient
	private transient InvoiceTypeDecorator invoiceType;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Override
	public String getUploadIndex() {
		return uploadIndex;
	}

	public void setUploadIndex(final String uploadIndex) {
		this.uploadIndex = uploadIndex;
	}

	@Override
	public String getDownloadId() {
		return downloadId;
	}

	public void setDownloadId(final String downloadId) {
		this.downloadId = downloadId;
	}

	@Override
	public String getXmlRaw() {
		return xmlRaw;
	}

	public void setXmlRaw(final String xmlRaw) {
		this.xmlRaw = xmlRaw;
	}

	@Override
	public LocalDate getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(final LocalDate issueDate) {
		this.issueDate = issueDate;
	}

	@Override
	public Long getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(final Long invoiceId) {
		this.invoiceId = invoiceId;
	}
	
	@Override
	public InvoiceTypeDecorator getUblType() {
		if (invoiceType == null)
		    invoiceType = new InvoiceTypeDecorator(UBL21Reader.invoice().read(getXmlRaw()));
		
		return invoiceType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(downloadId, id, invoiceId, issueDate, uploadIndex, xmlRaw);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReceivedInvoice other = (ReceivedInvoice) obj;
		return Objects.equals(downloadId, other.downloadId) && Objects.equals(id, other.id)
				&& Objects.equals(invoiceId, other.invoiceId) && Objects.equals(issueDate, other.issueDate)
				&& Objects.equals(uploadIndex, other.uploadIndex) && Objects.equals(xmlRaw, other.xmlRaw);
	}

	@Override
	public String toString() {
		return "ReceivedInvoice [id=" + id + ", uploadIndex=" + uploadIndex + ", downloadId=" + downloadId + ", xmlRaw="
				+ xmlRaw + ", issueDate=" + issueDate + ", invoiceId=" + invoiceId + "]";
	}
}
