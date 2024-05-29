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
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.preferences.PreferenceKey;

public class DBInitAddon {
	@PostConstruct
	public void postConstruct(final LocalDatabase localDatabase, final ILog log) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement()) {
			createProductsTable(stmt);
		} catch (final SQLException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void createProductsTable(final Statement stmt) throws SQLException {
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Product.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Product.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Product.CATEGORY_FIELD+" text,").append(NEWLINE)
		.append(Product.SKU_FIELD+" text,").append(NEWLINE)
		.append(Product.BARCODES_FIELD+" text,").append(NEWLINE)
		.append(Product.NAME_FIELD+" text,").append(NEWLINE)
		.append(Product.UOM_FIELD+" text,").append(NEWLINE)
		.append(Product.IS_STOCKABLE_FIELD+" integer,").append(NEWLINE)
		.append(Product.PRICE_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(Product.STOCK_FIELD+" numeric(16,4)").append(NEWLINE)
		.append(");");
		stmt.execute(productsSb.toString());
	}
}
