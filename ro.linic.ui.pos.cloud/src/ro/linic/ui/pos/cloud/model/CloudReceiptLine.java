package ro.linic.ui.pos.cloud.model;

import java.math.BigDecimal;
import java.util.Objects;

import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.ReceiptLine;

public class CloudReceiptLine extends ReceiptLine {
	public static final String SYNCED_FIELD = "synced";
	
	/**
	 * Whether this ReceiptLine was synchronized to a remote server
	 */
	private Boolean synced;
	
	public CloudReceiptLine(final Long id, final Long productId, final Long receiptId, final String sku, final String name, final String uom,
			final BigDecimal quantity, final BigDecimal price, final AllowanceCharge allowanceCharge, final String taxCode,
			final String departmentCode, final BigDecimal taxTotal, final Boolean synced) {
		super(id, productId, receiptId, sku, name, uom, quantity, price, allowanceCharge, taxCode, departmentCode, taxTotal);
		this.synced = synced;
	}

	public CloudReceiptLine() {
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

	@Override
	public String toString() {
		return "LegacyReceiptLine [synced=" + synced 
				+ ", toString()=" + super.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(synced);
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
		final CloudReceiptLine other = (CloudReceiptLine) obj;
		return Objects.equals(synced, other.synced);
	}
}
