package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.NumberUtils.parseToLong;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataLoader;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component
public class LocalProductDataLoader implements ProductDataLoader {
	private final static ILog log = ILog.of(LocalProductDataLoader.class);
	
	@Reference private LocalDatabase localDatabase;
	@Reference private SQLiteHelper sqliteHelper;
	
	@Override
	public List<Product> findAll() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<Product> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.productColumns())
		.append("FROM "+Product.class.getSimpleName());
		
		dbLock.readLock().lock();
		try (Statement stmt = localDatabase.getConnection(dbName).createStatement();
				ResultSet rs = stmt.executeQuery(querySb.toString())) {
			result = sqliteHelper.readProducts(rs);
		} catch (final SQLException | JsonProcessingException e) {
			log.error(e.getMessage(), e);
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
		
		List<Product> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.productColumns())
		.append("FROM "+Product.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(Product.ID_FIELD).append(" IN (?)");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setObject(1, ids);
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readProducts(rs);
		} catch (final SQLException | JsonProcessingException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result;
	}
	
	@Override
	public String autoSku() {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
        final String sql = "SELECT MAX(CAST("+Product.SKU_FIELD+" AS INTEGER)) FROM "+Product.class.getSimpleName();
        long nextId = 1;
        
        dbLock.readLock().lock();
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        } catch (final SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
			dbLock.readLock().unlock();
		}
        
        return String.valueOf(nextId);
    }

	@Override
	public Optional<Product> findByIdentifier(final String identifier) {
		return findById(parseToLong(identifier))
				.or(() -> findByCode(identifier));
	}
	
	private Optional<Product> findByCode(final String code) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		List<Product> result = new ArrayList<>();
		final StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ").append(NEWLINE)
		.append(sqliteHelper.productColumns())
		.append("FROM "+Product.class.getSimpleName()).append(NEWLINE)
		.append("WHERE ").append(NEWLINE)
		.append(Product.SKU_FIELD).append(" == ? OR ").append(Product.BARCODES_FIELD).append(" LIKE %?%");
		
		dbLock.readLock().lock();
		try (PreparedStatement stmt = localDatabase.getConnection(dbName).prepareStatement(querySb.toString())) {
			stmt.setObject(1, code);
			stmt.setObject(2, "\""+code+"\"");
			final ResultSet rs = stmt.executeQuery();
			result = sqliteHelper.readProducts(rs);
		} catch (final SQLException | JsonProcessingException e) {
			log.error(e.getMessage(), e);
		} finally {
			dbLock.readLock().unlock();
		}
		
		return result.stream().findFirst();
	}
}
