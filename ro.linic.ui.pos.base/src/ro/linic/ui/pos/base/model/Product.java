package ro.linic.ui.pos.base.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ro.linic.ui.base.services.model.JavaBean;

public class Product extends JavaBean {
	public static final String ID_FIELD = "id";
	public static final String TYPE_FIELD = "type";
	public static final String TAX_CODE_FIELD = "taxCode";
	public static final String DEPARTMENT_CODE_FIELD = "departmentCode";
	public static final String SKU_FIELD = "sku";
	public static final String BARCODES_FIELD = "barcodes";
	public static final String NAME_FIELD = "name";
	public static final String UOM_FIELD = "uom";
	public static final String IS_STOCKABLE_FIELD = "isStockable";
	public static final String PRICE_FIELD = "price";
	public static final String STOCK_FIELD = "stock";
	public static final String IMAGE_ID_FIELD = "imageId";
	public static final String TAX_PERCENTAGE_FIELD = "taxPercentage";
	
	public static final String MARFA_CATEGORY = "MARFA";
	public static final String AMBALAJE_CATEGORY = "AMBALAJE";
	public static final String MATERIE_PRIMA_CATEGORY = "MATERIE PRIMA";
	public static final String MATERIALE_AUXILIARE_CATEGORY = "MATERIALE AUXILIARE";
	public static final String COMBUSTIBILI_CATEGORY = "COMBUSTIBILI";
	public static final String ALTE_MATERIALE_CATEGORY = "ALTE MATERIALE";
	public static final String OBIECTE_INVENTAR_CATEGORY = "OBIECTE DE INVENTAR";
	public static final String MIJLOACE_FIXE_CATEGORY = "MIJLOACE FIXE";
	public static final String PRODUS_FINIT_CATEGORY = "PRODUS FINIT";
	public static final String SERVICII_CATEGORY = "SERVICII";
	public static final String DISCOUNT_CATEGORY = "DISCOUNT";
	
	public static final List<String> RO_CATEGORIES = List.of(MARFA_CATEGORY, AMBALAJE_CATEGORY, MATERIE_PRIMA_CATEGORY,
			MATERIALE_AUXILIARE_CATEGORY, COMBUSTIBILI_CATEGORY, ALTE_MATERIALE_CATEGORY, OBIECTE_INVENTAR_CATEGORY, MIJLOACE_FIXE_CATEGORY,
			PRODUS_FINIT_CATEGORY, SERVICII_CATEGORY, DISCOUNT_CATEGORY);
	
	private Long id;
	// MERCHANDISE, RAW MATERIALS, FINISHED GOODS, SERVICE...
	private String type;
	// tax code as specified in the ecr. default: 1
	private String taxCode;
	// product level tax percentage. eg.: 0.19 for 19%
	private BigDecimal taxPercentage;
	// department code as specified in the ecr. default: 1
	// TODO should also be specified at product type level, as a cascade, if not present here
	private String departmentCode;
	private String sku;
	private Set<String> barcodes;
	private String name;
	private String uom;
	private boolean stockable;
	private BigDecimal price;
	private BigDecimal stock;
	private String imageId;
	
	public Product() {
		barcodes = new HashSet<String>();
	}
	
	public Product(final Long id, final String type, final String taxCode, final String departmentCode, final String sku,
			final Set<String> barcodes, final String name, final String uom, final boolean stockable, final BigDecimal price,
			final BigDecimal stock, final String imageId, final BigDecimal taxPercentage) {
		super();
		this.id = id;
		this.type = type;
		this.taxCode = taxCode;
		this.departmentCode = departmentCode;
		this.sku = sku;
		this.barcodes = barcodes;
		this.name = name;
		this.uom = uom;
		this.stockable = stockable;
		this.price = price;
		this.stock = stock;
		this.imageId = imageId;
		this.taxPercentage = taxPercentage;
	}

	public Long getId() {
		return id;
	}
	
	public void setId(final Long id) {
		firePropertyChange("id", this.id, this.id = id);
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(final String type) {
		firePropertyChange("type", this.type, this.type = type);
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
	
	public String getSku() {
		return sku;
	}
	
	public void setSku(final String sku) {
		firePropertyChange("sku", this.sku, this.sku = sku);
	}
	
	public Set<String> getBarcodes() {
		return barcodes;
	}
	
	public void setBarcodes(final Set<String> barcodes) {
		firePropertyChange("barcodes", this.barcodes, this.barcodes = barcodes);
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
	
	public boolean isStockable() {
		return stockable;
	}
	
	public void setStockable(final boolean stockable) {
		firePropertyChange("stockable", this.stockable, this.stockable = stockable);
	}
	
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(final BigDecimal price) {
		firePropertyChange("price", this.price, this.price = price);
	}
	
	public BigDecimal getStock() {
		return stock;
	}
	
	public void setStock(final BigDecimal stock) {
		firePropertyChange("stock", this.stock, this.stock = stock);
	}
	
	public String getImageId() {
		return imageId;
	}
	
	public void setImageId(final String imageId) {
		firePropertyChange("imageId", this.imageId, this.imageId = imageId);
	}
	
	public BigDecimal getTaxPercentage() {
		return taxPercentage;
	}
	
	public void setTaxPercentage(final BigDecimal taxPercentage) {
		firePropertyChange("taxPercentage", this.taxPercentage, this.taxPercentage = taxPercentage);
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", type=" + type + ", taxCode=" + taxCode + ", taxPercentage=" + taxPercentage
				+ ", departmentCode=" + departmentCode + ", sku=" + sku + ", barcodes=" + barcodes + ", name=" + name
				+ ", uom=" + uom + ", stockable=" + stockable + ", price=" + price + ", stock=" + stock + ", imageId="
				+ imageId + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(barcodes, departmentCode, id, imageId, name, price, sku, stock, stockable, taxCode,
				taxPercentage, type, uom);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Product other = (Product) obj;
		return Objects.equals(barcodes, other.barcodes) && Objects.equals(departmentCode, other.departmentCode)
				&& Objects.equals(id, other.id) && Objects.equals(imageId, other.imageId)
				&& Objects.equals(name, other.name) && Objects.equals(price, other.price)
				&& Objects.equals(sku, other.sku) && Objects.equals(stock, other.stock) && stockable == other.stockable
				&& Objects.equals(taxCode, other.taxCode) && Objects.equals(taxPercentage, other.taxPercentage)
				&& Objects.equals(type, other.type) && Objects.equals(uom, other.uom);
	}
}
