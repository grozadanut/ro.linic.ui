package ro.linic.ui.pos.base.addons;

import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;
import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataHolder;
import ro.linic.ui.pos.base.services.ProductDataLoader;

public class InitAddon {
	@PostConstruct
	public void postConstruct(final LocalDatabase localDatabase, final ILog log, final ProductDataHolder productHolder,
			final ProductDataLoader productLoader) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement()) {
			stmt.execute(createProductsTableSql());
			stmt.execute(createReceiptTableSql());
			stmt.execute(createReceiptLineTableSql());
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		}
		
		productHolder.setData(productLoader.findAll());
	}

	public static String createProductsTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Product.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Product.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Product.TYPE_FIELD+" text,").append(NEWLINE)
		.append(Product.TAX_CODE_FIELD+" text,").append(NEWLINE)
		.append(Product.DEPARTMENT_CODE_FIELD+" text,").append(NEWLINE)
		.append(Product.SKU_FIELD+" text UNIQUE,").append(NEWLINE)
		.append(Product.BARCODES_FIELD+" text UNIQUE,").append(NEWLINE)
		.append(Product.NAME_FIELD+" text,").append(NEWLINE)
		.append(Product.UOM_FIELD+" text,").append(NEWLINE)
		.append(Product.IS_STOCKABLE_FIELD+" integer,").append(NEWLINE)
		.append(Product.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(Product.STOCK_FIELD+" numeric(16,4),").append(NEWLINE)
		.append(Product.IMAGE_ID_FIELD+" text,").append(NEWLINE)
		.append(Product.TAX_PERCENTAGE_FIELD+" numeric(6,4)").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
	
	public static String createReceiptTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Receipt.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Receipt.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(Receipt.CLOSED_FIELD+" integer,").append(NEWLINE)
		.append(Receipt.CREATION_TIME_FIELD+" text").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
	
	public static String createReceiptLineTableSql() throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+ReceiptLine.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(ReceiptLine.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(ReceiptLine.PRODUCT_ID_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.RECEIPT_ID_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.NAME_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.UOM_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.QUANTITY_FIELD+" numeric(16,3),").append(NEWLINE)
		.append(ReceiptLine.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(ReceiptLine.TAX_TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.TOTAL_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" integer,").append(NEWLINE)
		.append(ReceiptLine.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" numeric(16,2),").append(NEWLINE)
		.append(ReceiptLine.TAX_CODE_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.DEPARTMENT_CODE_FIELD+" text,").append(NEWLINE)
		.append(ReceiptLine.CREATION_TIME_FIELD+" text").append(NEWLINE)
		.append(");");
		return productsSb.toString();
	}
}
