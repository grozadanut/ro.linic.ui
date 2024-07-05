package ro.linic.ui.pos.cloud.model;

import java.util.List;
import java.util.Objects;

import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;

public class CloudReceipt extends Receipt{
	public static final String SYNCED_FIELD = "synced";
	public static final String NUMBER_FIELD = "number";
	
	/**
	 * Whether this Receipt was synchronized to a remote server
	 */
	private Boolean synced;
	private Integer number;

	public CloudReceipt(final Long id, final List<ReceiptLine> lines, final AllowanceCharge allowanceCharge, final Boolean synced,
			final Integer number) {
		super(id, lines, allowanceCharge);
		this.synced = synced;
		this.number = number;
	}
	
	public CloudReceipt() {
	}
	
	public Boolean getSynced() {
		return synced;
	}
	
	public boolean synced() {
		return synced == null ? false : synced;
	}

	public void setSynced(final Boolean synced) {
		firePropertyChange("synced", this.synced, this.synced = synced);
	}
	
	public Integer getNumber() {
		return number;
	}
	
	public void setNumber(final Integer number) {
		firePropertyChange("number", this.number, this.number = number);
	}

	@Override
	public String toString() {
		return "CloudReceipt [synced=" + synced + ", number=" + number + ", toString()=" + super.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(number, synced);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final CloudReceipt other = (CloudReceipt) obj;
		return Objects.equals(number, other.number) && Objects.equals(synced, other.synced);
	}
}
