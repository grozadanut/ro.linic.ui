package ro.linic.ui.legacy.anaf;

import java.time.LocalDate;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import com.helger.ubl21.UBL21Reader;

public class ReceivedCreditNote implements Invoice<CreditNoteTypeDecorator> {
	private Long id;
	private String uploadIndex;
	private String downloadId;
	private String xmlRaw;
	private LocalDate issueDate;
	
	@XmlTransient
	private transient CreditNoteTypeDecorator creditNoteType;

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
		return null;
	}
	
	@Override
	public CreditNoteTypeDecorator getUblType() {
		if (creditNoteType == null)
			creditNoteType = new CreditNoteTypeDecorator(UBL21Reader.creditNote().read(getXmlRaw()));
		
		return creditNoteType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(downloadId, id, issueDate, uploadIndex, xmlRaw);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReceivedCreditNote other = (ReceivedCreditNote) obj;
		return Objects.equals(downloadId, other.downloadId) && Objects.equals(id, other.id)
				&& Objects.equals(issueDate, other.issueDate)
				&& Objects.equals(uploadIndex, other.uploadIndex) && Objects.equals(xmlRaw, other.xmlRaw);
	}

	@Override
	public String toString() {
		return "ReceivedInvoice [id=" + id + ", uploadIndex=" + uploadIndex + ", downloadId=" + downloadId + ", xmlRaw="
				+ xmlRaw + ", issueDate=" + issueDate + "]";
	}
}
