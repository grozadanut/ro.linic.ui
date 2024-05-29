package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.util.commons.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataUpdater;

@Component
public class LocalProductDataUpdater implements ProductDataUpdater {
	private final static Logger log = Logger.getLogger(LocalProductDataUpdater.class.getName());
	
	@Reference private LocalDatabase localDatabase;
	
	@Override
	public void create(final Product p) {
		if (p == null)
			return;
		validate(p);
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		final ObjectMapper objectMapper = new ObjectMapper();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+Product.class.getSimpleName())
		.append("(")
		.append(Product.ID_FIELD).append(",")
		.append(Product.CATEGORY_FIELD).append(",")
		.append(Product.SKU_FIELD).append(",")
		.append(Product.BARCODES_FIELD).append(",")
		.append(Product.NAME_FIELD).append(",")
		.append(Product.UOM_FIELD).append(",")
		.append(Product.IS_STOCKABLE_FIELD).append(",")
		.append(Product.PRICE_FIELD).append(",")
		.append(Product.STOCK_FIELD)
		.append(")").append(" VALUES(?,?,?,?,?,?,?,?,?)");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setLong(1, nextId(dbName));
        	pstmt.setString(2, p.getCategory());
        	pstmt.setString(3, p.getSku());
        	pstmt.setString(4, objectMapper.writeValueAsString(p.getBarcodes()));
            pstmt.setString(5, p.getName());
            pstmt.setString(6, p.getUom());
            pstmt.setBoolean(7, p.isStockable());
            pstmt.setBigDecimal(8, p.getPrice());
            pstmt.setBigDecimal(9, p.getStock());
            pstmt.executeUpdate();
        } catch (final SQLException | JsonProcessingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private long nextId(final String dbName) {
        final String sql = "SELECT MAX("+Product.ID_FIELD+") FROM "+Product.class.getSimpleName();
        long nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        } catch (final SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        
        return nextId;
    }

	@Override
	public void update(final Product p) {
		if (p == null || p.getId() == null)
			return;
		validate(p);
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		final ObjectMapper objectMapper = new ObjectMapper();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Product.class.getSimpleName()+" SET ")
		.append(Product.CATEGORY_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.SKU_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.BARCODES_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.NAME_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.UOM_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.IS_STOCKABLE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.PRICE_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Product.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setString(1, p.getCategory());
        	pstmt.setString(2, p.getSku());
        	pstmt.setString(3, objectMapper.writeValueAsString(p.getBarcodes()));
            pstmt.setString(4, p.getName());
            pstmt.setString(5, p.getUom());
            pstmt.setBoolean(6, p.isStockable());
            pstmt.setBigDecimal(7, p.getPrice());
            // WHERE
            pstmt.setLong(8, p.getId());
            pstmt.executeUpdate();
        } catch (final SQLException | JsonProcessingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private void validate(final Product p) {
		if (isEmpty(p.getName()))
			throw new IllegalArgumentException(Messages.NameMandatory);
		if (p.getPrice() == null)
			throw new IllegalArgumentException(Messages.PriceMandatory);
	}
	
	@Override
	public void updateStock(final long id, final BigDecimal stock) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Product.class.getSimpleName()+" SET ")
		.append(Product.STOCK_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Product.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
            pstmt.setBigDecimal(1, stock);
            // WHERE
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}

	@Override
	public void delete(final Product p) {
		if (p == null || p.getId() == null)
			return;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final String sql = "DELETE FROM "+Product.class.getSimpleName()+" WHERE "+Product.ID_FIELD+" = ?";

		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sql)) {
            pstmt.setLong(1, p.getId());
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
}
