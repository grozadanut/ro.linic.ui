package ro.linic.ui.legacy.service.components;

import java.math.BigDecimal;
import java.util.Objects;

import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.cloud.model.CloudReceiptLine;

public class LegacyReceiptLine extends CloudReceiptLine {
	public static final String WAREHOUSE_ID_FIELD = "warehouseId";
	public static final String USER_ID_FIELD = "userId";
	
	private Integer warehouseId;
	private Integer userId;
	
	public LegacyReceiptLine(final Long id, final Long productId, final Long receiptId, final String sku, final String name, final String uom,
			final BigDecimal quantity, final BigDecimal price, final AllowanceCharge allowanceCharge, final String taxCode,
			final String departmentCode, final BigDecimal taxTotal, final Boolean synced, final Integer warehouseId, final Integer userId) {
		super(id, productId, receiptId, sku, name, uom, quantity, price, allowanceCharge, taxCode, departmentCode, taxTotal, synced);
		this.warehouseId = warehouseId;
		this.userId = userId;
	}

	public LegacyReceiptLine() {
	}

	public Integer getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(final Integer warehouseId) {
		firePropertyChange("warehouseId", this.warehouseId, this.warehouseId = warehouseId);
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(final Integer userId) {
		firePropertyChange("userId", this.userId, this.userId = userId);
	}

	@Override
	public String toString() {
		return "LegacyReceiptLine [warehouseId=" + warehouseId + ", userId=" + userId 
				+ ", toString()=" + super.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(userId, warehouseId);
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
		final LegacyReceiptLine other = (LegacyReceiptLine) obj;
		return Objects.equals(userId, other.userId) && Objects.equals(warehouseId, other.warehouseId);
	}
}
