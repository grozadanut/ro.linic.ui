package ro.linic.ui.legacy.anaf;

import java.time.LocalDateTime;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class ReceivedMessage {
	private Long id;
	private LocalDateTime creationDate;
	private String taxId;
	private String uploadIndex;
	private String details;
	private AnafReceivedMessageType messageType;
	
	public enum AnafReceivedMessageType {
		@SerializedName("FACTURA PRIMITA")
		FACTURA_PRIMITA,
		@SerializedName("FACTURA TRIMISA")
		FACTURA_TRIMISA,
		@SerializedName("ERORI FACTURA")
		ERORI_FACTURA;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(final LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(final String taxId) {
		this.taxId = taxId;
	}

	public String getUploadIndex() {
		return uploadIndex;
	}

	public void setUploadIndex(final String uploadIndex) {
		this.uploadIndex = uploadIndex;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(final String details) {
		this.details = details;
	}

	public AnafReceivedMessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(final AnafReceivedMessageType messageType) {
		this.messageType = messageType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(creationDate, details, id, messageType, taxId, uploadIndex);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReceivedMessage other = (ReceivedMessage) obj;
		return Objects.equals(creationDate, other.creationDate) && Objects.equals(details, other.details)
				&& Objects.equals(id, other.id) && messageType == other.messageType
				&& Objects.equals(taxId, other.taxId) && Objects.equals(uploadIndex, other.uploadIndex);
	}

	@Override
	public String toString() {
		return "ReceivedMessage [id=" + id + ", creationDate=" + creationDate + ", taxId=" + taxId + ", uploadIndex="
				+ uploadIndex + ", details=" + details + ", messageType=" + messageType + "]";
	}
}
