package ro.linic.ui.pos.base.model;

import static ro.linic.util.commons.NumberUtils.add;
import static ro.linic.util.commons.NumberUtils.multiply;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import ro.linic.ui.base.services.model.JavaBean;

public class ReceiptLine extends JavaBean {
	public static final String ID_FIELD = "id";
	public static final String PRODUCT_ID_FIELD = "productId";
	public static final String RECEIPT_ID_FIELD = "receiptId";
	public static final String NAME_FIELD = "name";
	public static final String UOM_FIELD = "uom";
	public static final String QUANTITY_FIELD = "quantity";
	public static final String PRICE_FIELD = "price";
	public static final String TAX_TOTAL_FIELD = "taxTotal";
	public static final String TOTAL_FIELD = "total";
	public static final String ALLOWANCE_CHARGE_FIELD = "allowanceCharge";
	public static final String TAX_CODE_FIELD = "taxCode";
	public static final String DEPARTMENT_CODE_FIELD = "departmentCode";
	public static final String CREATION_TIME_FIELD = "creationTime";
	
	private Long id;
	private Long productId;
	private Long receiptId;
	private String name;
	private String uom;
	private BigDecimal quantity;
	private BigDecimal price;
	private BigDecimal taxTotal;
	private BigDecimal total;
	private AllowanceCharge allowanceCharge;
	private String taxCode;
	private String departmentCode;
	private Instant creationTime = Instant.now();
	
	/**
	 * @param id unique id of this line
	 * @param productId reference to the product this line refers to, can be null
	 * @param name A name for a sold item.
	 * @param uom unit of measure
	 * @param quantity The quantity of items (goods or services) that is charged, rounded to 3 decimal places.
	 * @param price The price of an item, exclusive of allowances or charges, rounded to 2 decimal places. Item price can not be negative.
	 * @param allowanceCharge Line level allowanceCharge applied to the price.
	 * @param taxCode tax code as specified in the ecr. eg: 1
	 * @param departmentCode department code as specified in the ecr. eg: 1
	 */
	public ReceiptLine(final Long id, final Long productId, final Long receiptId, final String name, final String uom, final BigDecimal quantity,
			final BigDecimal price, final AllowanceCharge allowanceCharge, final String taxCode, final String departmentCode, final BigDecimal taxTotal) {
		this.id = id;
		this.productId = productId;
		this.receiptId = receiptId;
		this.name = name;
		this.uom = uom;
		this.quantity = quantity;
		this.price = price;
		this.allowanceCharge = allowanceCharge;
		this.taxCode = taxCode;
		this.departmentCode = departmentCode;
		this.taxTotal = taxTotal;
		this.total = add(multiply(quantity, price),
				Optional.ofNullable(allowanceCharge)
				.map(AllowanceCharge::amountWithSign)
				.orElse(null));
	}
	
	public ReceiptLine() {
	}
	
	/**
	 * total including taxes and allowance/charges
	 */
	public BigDecimal getTotal() {
		return total;
	}
	
	public void setTotal(final BigDecimal total) {
		firePropertyChange("total", this.total, this.total = total);
	}
	
	public BigDecimal getTaxTotal() {
		return taxTotal;
	}
	
	public void setTaxTotal(final BigDecimal taxTotal) {
		firePropertyChange("taxTotal", this.taxTotal, this.taxTotal = taxTotal);
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		firePropertyChange("id", this.id, this.id = id);
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(final Long productId) {
		firePropertyChange("productId", this.productId, this.productId = productId);
	}
	
	public Long getReceiptId() {
		return receiptId;
	}
	
	public void setReceiptId(final Long receiptId) {
		firePropertyChange("receiptId", this.receiptId, this.receiptId = receiptId);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		firePropertyChange("name", this.name, this.name = name);
	}

	public String getUom() {
		return uom;
	}

	public void setUom(final String uom) {
		firePropertyChange("uom", this.uom, this.uom = uom);
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(final BigDecimal quantity) {
		firePropertyChange("quantity", this.quantity, this.quantity = quantity);
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(final BigDecimal price) {
		firePropertyChange("price", this.price, this.price = price);
	}

	public AllowanceCharge getAllowanceCharge() {
		return allowanceCharge;
	}

	public void setAllowanceCharge(final AllowanceCharge allowanceCharge) {
		firePropertyChange("allowanceCharge", this.allowanceCharge, this.allowanceCharge = allowanceCharge);
	}

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(final String taxCode) {
		firePropertyChange("taxCode", this.taxCode, this.taxCode = taxCode);
	}

	public String getDepartmentCode() {
		return departmentCode;
	}

	public void setDepartmentCode(final String departmentCode) {
		firePropertyChange("departmentCode", this.departmentCode, this.departmentCode = departmentCode);
	}

	public Instant getCreationTime() {
		return creationTime;
	}
	
	public void setCreationTime(final Instant creationTime) {
		firePropertyChange("creationTime", this.creationTime, this.creationTime = creationTime);
	}

	@Override
	public String toString() {
		return "ReceiptLine [id=" + id + ", productId=" + productId + ", receiptId=" + receiptId + ", name=" + name
				+ ", uom=" + uom + ", quantity=" + quantity + ", price=" + price + ", taxTotal=" + taxTotal + ", total="
				+ total + ", allowanceCharge=" + allowanceCharge + ", taxCode=" + taxCode + ", departmentCode="
				+ departmentCode + ", creationTime=" + creationTime + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(allowanceCharge, creationTime, departmentCode, id, name, price, productId, quantity,
				receiptId, taxCode, taxTotal, total, uom);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReceiptLine other = (ReceiptLine) obj;
		return Objects.equals(allowanceCharge, other.allowanceCharge)
				&& Objects.equals(creationTime, other.creationTime)
				&& Objects.equals(departmentCode, other.departmentCode) && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && Objects.equals(price, other.price)
				&& Objects.equals(productId, other.productId) && Objects.equals(quantity, other.quantity)
				&& Objects.equals(receiptId, other.receiptId) && Objects.equals(taxCode, other.taxCode)
				&& Objects.equals(taxTotal, other.taxTotal) && Objects.equals(total, other.total)
				&& Objects.equals(uom, other.uom);
	}
}
