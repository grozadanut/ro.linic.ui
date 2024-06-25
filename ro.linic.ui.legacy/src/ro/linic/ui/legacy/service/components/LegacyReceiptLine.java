package ro.linic.ui.legacy.service.components;

import java.math.BigDecimal;
import java.util.Objects;

import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.ReceiptLine;

public class LegacyReceiptLine extends ReceiptLine {
	public static final String WAREHOUSE_ID_FIELD = "warehouseId";
	public static final String USER_ID_FIELD = "userId";
	public static final String ECR_ACTIVE_FIELD = "ecrActive";
	
	private Integer warehouseId;
	private Integer userId;
	private Boolean ecrActive;
	
	public LegacyReceiptLine(final Long id, final Long productId, final Long receiptId, final String name, final String uom, final BigDecimal quantity,
			final BigDecimal price, final AllowanceCharge allowanceCharge, final String taxCode, final String departmentCode,
			final BigDecimal taxTotal, final Integer warehouseId, final Integer userId, final Boolean ecrActive) {
		super(id, productId, receiptId, name, uom, quantity, price, allowanceCharge, taxCode, departmentCode, taxTotal);
		this.warehouseId = warehouseId;
		this.userId = userId;
		this.ecrActive = ecrActive;
	}

	public LegacyReceiptLine() {
	}

	public Integer getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(final Integer warehouseId) {
		this.warehouseId = warehouseId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(final Integer userId) {
		this.userId = userId;
	}

	public Boolean getEcrActive() {
		return ecrActive;
	}

	public void setEcrActive(final Boolean ecrActive) {
		this.ecrActive = ecrActive;
	}

	@Override
	public String toString() {
		return "LegacyReceiptLine [warehouseId=" + warehouseId + ", userId=" + userId + ", ecrActive=" + ecrActive
				+ ", toString()=" + super.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(ecrActive, userId, warehouseId);
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
		return Objects.equals(ecrActive, other.ecrActive) && Objects.equals(userId, other.userId)
				&& Objects.equals(warehouseId, other.warehouseId);
	}
}
