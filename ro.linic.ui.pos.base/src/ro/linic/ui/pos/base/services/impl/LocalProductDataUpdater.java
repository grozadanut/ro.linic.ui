package ro.linic.ui.pos.base.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;
import static ro.linic.util.commons.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.odell.glazedlists.EventList;
import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ProductDataHolder;
import ro.linic.ui.pos.base.services.ProductDataUpdater;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component
public class LocalProductDataUpdater implements ProductDataUpdater {
	private final static ILog log = ILog.of(LocalProductDataUpdater.class);
	
	@Reference private LocalDatabase localDatabase;
	@Reference private ProductDataHolder dataHolder;
	@Reference private SQLiteHelper sqliteHelper;
	
	@Override
	public IStatus create(final Product p) {
		if (p == null)
			return ValidationStatus.OK_STATUS;
		final IStatus validationStatus = validate(p);
		if (!validationStatus.isOK())
			return validationStatus;
		final IStatus codeStatus = validateSkuAndBarcode(p);
		if (!codeStatus.isOK())
			return codeStatus;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+Product.class.getSimpleName())
		.append("(")
		.append(sqliteHelper.productColumns())
		.append(")").append(" VALUES("+sqliteHelper.productColumnsPlaceholder()+")");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	p.setId(nextId(dbName));
        	sqliteHelper.insertProductInStatement(p, pstmt);
            if (pstmt.executeUpdate() == 1)
            	dataHolder.add(p);
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private long nextId(final String dbName) throws SQLException {
        final String sql = "SELECT MAX("+Product.ID_FIELD+") FROM "+Product.class.getSimpleName();
        long nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        }
        
        return nextId;
    }

	@Override
	public IStatus update(final Product p) {
		if (p == null || p.getId() == null)
			return ValidationStatus.OK_STATUS;
		
		final IStatus validationStatus = validate(p);
		if (!validationStatus.isOK())
			return validationStatus;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		final ObjectMapper objectMapper = new ObjectMapper();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Product.class.getSimpleName()+" SET ")
		.append(Product.TYPE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.TAX_CODE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.DEPARTMENT_CODE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.SKU_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.BARCODES_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.NAME_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.UOM_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.IS_STOCKABLE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.PRICE_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.IMAGE_ID_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Product.TAX_PERCENTAGE_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Product.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setString(1, p.getType());
        	pstmt.setString(2, p.getTaxCode());
        	pstmt.setString(3, p.getDepartmentCode());
        	pstmt.setString(4, p.getSku());
        	pstmt.setString(5, p.getBarcodes().isEmpty() ? null : objectMapper.writeValueAsString(p.getBarcodes()));
            pstmt.setString(6, p.getName());
            pstmt.setString(7, p.getUom());
            pstmt.setBoolean(8, p.isStockable());
            pstmt.setBigDecimal(9, p.getPrice());
            pstmt.setString(10, p.getImageId());
            pstmt.setBigDecimal(11, p.getTaxPercentage());
            // WHERE
            pstmt.setLong(12, p.getId());
            pstmt.executeUpdate();
            
            dataHolder.getData().stream()
            .filter(pp -> pp.getId() == p.getId())
            .findFirst()
            .ifPresent(old -> dataHolder.replace(old, p));
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException | JsonProcessingException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private IStatus validate(final Product p) {
		if (isEmpty(p.getType()))
			return ValidationStatus.error(Messages.ProductTypeMandatory);
		if (isEmpty(p.getName()))
			return ValidationStatus.error(Messages.NameMandatory);
		if (isEmpty(p.getUom()))
			return ValidationStatus.error(Messages.UOMMandatory);
		if (p.getPrice() == null)
			return ValidationStatus.error(Messages.PriceMandatory);
		return ValidationStatus.OK_STATUS;
	}
	
	private IStatus validateSkuAndBarcode(final Product product) {
		final EventList<Product> products = dataHolder.getData();
		products.getReadWriteLock().readLock().lock();
		try {
			final Optional<String> codeFound = products.stream()
			.flatMap(p -> Stream.concat(p.getBarcodes().stream(), Stream.of(p.getSku())))
			.filter(Objects::nonNull)
			.filter(code -> code.equalsIgnoreCase(product.getSku()) || product.getBarcodes().contains(code))
			.findAny();
			
			return codeFound.isPresent() ? ValidationStatus.error(NLS.bind(Messages.CodeExists, codeFound.get())) :
				ValidationStatus.OK_STATUS;
		} finally {
			products.getReadWriteLock().readLock().unlock();
		}
	}
	
	@Override
	public IStatus updateStock(final long id, final BigDecimal stock) {
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
            
            dataHolder.getData().stream()
            .filter(p -> p.getId() == id)
            .findFirst()
            .ifPresent(p -> p.setStock(stock));
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}

	@Override
	public IStatus delete(final List<Long> ids) {
		if (ids == null || ids.isEmpty())
			return ValidationStatus.OK_STATUS;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final String sql = "DELETE FROM "+Product.class.getSimpleName()+" WHERE "+Product.ID_FIELD+" == ?";

		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sql)) {
        	for (final Long id : ids) {
        		pstmt.setLong(1, id);
        		if (pstmt.executeUpdate() == 1)
        			dataHolder.getData().stream()
        			.filter(p -> p.getId() == id)
        			.findFirst()
        			.ifPresent(dataHolder.getData()::remove);
			}
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	@Override
	public IStatus synchronize(final List<Product> products) {
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final String deleteSql = "DELETE FROM "+Product.class.getSimpleName(); // delete all
		
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+Product.class.getSimpleName())
		.append("(")
		.append(sqliteHelper.productColumns())
		.append(")").append(" VALUES("+sqliteHelper.productColumnsPlaceholder()+")");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString());
        		Statement stmt  = localDatabase.getConnection(dbName).createStatement();) {
        	stmt.execute(deleteSql);
        	dataHolder.setData(List.of());
        	
        	for (final Product product : products) {
        		sqliteHelper.insertProductInStatement(product, pstmt);
        		if (pstmt.executeUpdate() == 1)
        			dataHolder.add(product);
			}
        	
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
}
