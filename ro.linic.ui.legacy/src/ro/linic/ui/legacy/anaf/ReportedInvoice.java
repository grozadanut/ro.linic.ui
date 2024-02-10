package ro.linic.ui.legacy.anaf;

import java.util.Objects;

public class ReportedInvoice
{
	public enum ReportState
	{
		UPLOAD_ERROR, WAITING_VALIDATION, REJECTED_INVALID, SENT;
	}
	
	private Long invoiceId;
	private ReportState state;
	private String uploadIndex;
	private String downloadId;
	private String errorMessage;
	
	public Long getInvoiceId()
	{
		return invoiceId;
	}

	public void setInvoiceId(final Long invoiceId)
	{
		this.invoiceId = invoiceId;
	}

	public ReportState getState()
	{
		return state;
	}

	public void setState(final ReportState state)
	{
		this.state = state;
	}

	public String getUploadIndex()
	{
		return uploadIndex;
	}

	public void setUploadIndex(final String uploadIndex)
	{
		this.uploadIndex = uploadIndex;
	}

	public String getDownloadId()
	{
		return downloadId;
	}

	public void setDownloadId(final String downloadId)
	{
		this.downloadId = downloadId;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(final String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(downloadId, errorMessage, invoiceId, state, uploadIndex);
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReportedInvoice other = (ReportedInvoice) obj;
		return Objects.equals(downloadId, other.downloadId) && Objects.equals(errorMessage, other.errorMessage)
				&& Objects.equals(invoiceId, other.invoiceId) && state == other.state
				&& Objects.equals(uploadIndex, other.uploadIndex);
	}

	@Override
	public String toString()
	{
		return "ReportedInvoice [invoiceId=" + invoiceId + ", state=" + state + ", uploadIndex=" + uploadIndex
				+ ", downloadId=" + downloadId + ", errorMessage=" + errorMessage + "]";
	}
}
