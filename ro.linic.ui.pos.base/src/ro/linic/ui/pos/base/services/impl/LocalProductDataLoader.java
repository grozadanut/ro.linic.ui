package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataLoader;

@Component
public class LocalProductDataLoader implements ProductDataLoader {
	private final static Logger log = Logger.getLogger(LocalProductDataLoader.class.getName());
	
	@Reference private LocalDatabase localDatabase;
	
	@Override
	public List<Product> findAll() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		final ObjectMapper objectMapper = new ObjectMapper();
		
		final List<Product> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(Product.ID_FIELD).append(LIST_SEPARATOR)
		.append(Product.CATEGORY_FIELD).append(LIST_SEPARATOR)
		.append(Product.SKU_FIELD).append(LIST_SEPARATOR)
		.append(Product.BARCODES_FIELD).append(LIST_SEPARATOR)
		.append(Product.NAME_FIELD).append(LIST_SEPARATOR)
		.append(Product.UOM_FIELD).append(LIST_SEPARATOR)
		.append(Product.IS_STOCKABLE_FIELD).append(LIST_SEPARATOR)
		.append(Product.PRICE_FIELD).append(LIST_SEPARATOR)
		.append(Product.STOCK_FIELD).append(NEWLINE)
		.append("FROM "+Product.class.getSimpleName());
		
		dbLock.readLock().lock();
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement();
				ResultSet rs = stmt.executeQuery(querySb.toString())) {
			while (rs.next()) {
				final Product p = new Product();
				p.setId(rs.getLong(Product.ID_FIELD));
				p.setCategory(rs.getString(Product.CATEGORY_FIELD));
				p.setSku(rs.getString(Product.SKU_FIELD));
				p.setBarcodes(objectMapper.readValue(rs.getString(Product.BARCODES_FIELD), new TypeReference<Set<String>>(){}));
				p.setName(rs.getString(Product.NAME_FIELD));
				p.setUom(rs.getString(Product.UOM_FIELD));
				p.setStockable(rs.getBoolean(Product.IS_STOCKABLE_FIELD));
				p.setPrice(rs.getBigDecimal(Product.PRICE_FIELD));
				p.setStock(rs.getBigDecimal(Product.STOCK_FIELD));
				result.add(p);
			}
		} catch (final SQLException | JsonProcessingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}

	@Override
	public List<Product> findByIdIn(final List<Long> ids) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		final ObjectMapper objectMapper = new ObjectMapper();
		
		final List<Product> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(Product.ID_FIELD).append(LIST_SEPARATOR)
		.append(Product.CATEGORY_FIELD).append(LIST_SEPARATOR)
		.append(Product.SKU_FIELD).append(LIST_SEPARATOR)
		.append(Product.BARCODES_FIELD).append(LIST_SEPARATOR)
		.append(Product.NAME_FIELD).append(LIST_SEPARATOR)
		.append(Product.UOM_FIELD).append(LIST_SEPARATOR)
		.append(Product.IS_STOCKABLE_FIELD).append(LIST_SEPARATOR)
		.append(Product.PRICE_FIELD).append(LIST_SEPARATOR)
		.append(Product.STOCK_FIELD).append(NEWLINE)
		.append("FROM "+Product.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(Product.ID_FIELD).append(" IN (?)");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setObject(1, ids);
			final ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				final Product p = new Product();
				p.setId(rs.getLong(Product.ID_FIELD));
				p.setCategory(rs.getString(Product.CATEGORY_FIELD));
				p.setSku(rs.getString(Product.SKU_FIELD));
				p.setBarcodes(objectMapper.readValue(rs.getString(Product.BARCODES_FIELD), new TypeReference<Set<String>>(){}));
				p.setName(rs.getString(Product.NAME_FIELD));
				p.setUom(rs.getString(Product.UOM_FIELD));
				p.setStockable(rs.getBoolean(Product.IS_STOCKABLE_FIELD));
				p.setPrice(rs.getBigDecimal(Product.PRICE_FIELD));
				p.setStock(rs.getBigDecimal(Product.STOCK_FIELD));
				result.add(p);
			}
		} catch (final SQLException | JsonProcessingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}
}
