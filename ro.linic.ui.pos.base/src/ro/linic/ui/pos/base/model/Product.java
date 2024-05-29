package ro.linic.ui.pos.base.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ro.linic.ui.base.services.model.JavaBean;

public class Product extends JavaBean {
	public static final String ID_FIELD = "id";
	public static final String CATEGORY_FIELD = "category";
	public static final String SKU_FIELD = "sku";
	public static final String BARCODES_FIELD = "barcodes";
	public static final String NAME_FIELD = "name";
	public static final String UOM_FIELD = "uom";
	public static final String IS_STOCKABLE_FIELD = "isStockable";
	public static final String PRICE_FIELD = "price";
	public static final String STOCK_FIELD = "stock";
	
	private Long id;
	private String category;
	private String sku;
	private Set<String> barcodes;
	private String name;
	private String uom;
	private boolean isStockable;
	private BigDecimal price;
	private BigDecimal stock;
	
	public Product() {
		barcodes = new HashSet<String>();
	}
	
	public Product(final Long id, final String category, final String sku, final Set<String> barcodes, final String name, final String uom,
			final boolean isStockable, final BigDecimal price, final BigDecimal stock) {
		super();
		this.id = id;
		this.category = category;
		this.sku = sku;
		this.barcodes = barcodes;
		this.name = name;
		this.uom = uom;
		this.isStockable = isStockable;
		this.price = price;
		this.stock = stock;
	}

	public Long getId() {
		return id;
	}
	
	public void setId(final Long id) {
		firePropertyChange("id", this.id, this.id = id);
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(final String category) {
		firePropertyChange("category", this.category, this.category = category);
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
		return isStockable;
	}
	
	public void setStockable(final boolean isStockable) {
		firePropertyChange("isStockable", this.isStockable, this.isStockable = isStockable);
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

	@Override
	public String toString() {
		return "Product [id=" + id + ", category=" + category + ", sku=" + sku + ", barcodes=" + barcodes + ", name="
				+ name + ", uom=" + uom + ", isStockable=" + isStockable + ", price=" + price + ", stock=" + stock
				+ "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(barcodes, category, id, isStockable, name, price, sku, stock, uom);
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
		return Objects.equals(barcodes, other.barcodes) && Objects.equals(category, other.category)
				&& Objects.equals(id, other.id) && isStockable == other.isStockable && Objects.equals(name, other.name)
				&& Objects.equals(price, other.price) && Objects.equals(sku, other.sku)
				&& Objects.equals(stock, other.stock) && Objects.equals(uom, other.uom);
	}
}
