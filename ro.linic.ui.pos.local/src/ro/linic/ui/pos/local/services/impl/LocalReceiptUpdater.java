package ro.linic.ui.pos.local.services.impl;

import static ro.linic.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.linic.util.commons.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ReceiptUpdater;
import ro.linic.ui.pos.base.services.SQLiteHelper;

@Component(property = org.osgi.framework.Constants.SERVICE_RANKING + "=0")
public class LocalReceiptUpdater implements ReceiptUpdater {
	@Reference private LocalDatabase localDatabase;
	@Reference private SQLiteHelper sqliteHelper;
	
	@Override
	public IStatus create(final Receipt model) {
		if (model == null)
			return ValidationStatus.OK_STATUS;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+Receipt.class.getSimpleName())
		.append("(")
		.append(sqliteHelper.receiptColumns())
		.append(")").append(" VALUES("+sqliteHelper.receiptColumnsPlaceholder()+")");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	model.setId(nextId(dbName));
        	sqliteHelper.insertReceiptInStatement(model, pstmt);
            pstmt.executeUpdate();
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	private long nextId(final String dbName) throws SQLException {
        final String sql = "SELECT MAX("+Receipt.ID_FIELD+") FROM "+Receipt.class.getSimpleName();
        long nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        }
        
        return nextId;
    }

	@Override
	public IStatus update(final long id, final Receipt model) {
		if (model == null || model.getId() == null)
			return ValidationStatus.OK_STATUS;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Receipt.class.getSimpleName()+" SET ")
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(Receipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Receipt.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setBoolean(1, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
        	pstmt.setBigDecimal(2, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
            // WHERE
            pstmt.setLong(3, model.getId());
            pstmt.executeUpdate();
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
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final String sql = "DELETE FROM "+Receipt.class.getSimpleName()+" WHERE "+Receipt.ID_FIELD+" == ?";

		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sql)) {
        	for (final Long id : ids) {
        		pstmt.setLong(1, id);
        		pstmt.executeUpdate();
			}
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
	
	@Override
	public IStatus closeReceipt(final Long id) {
		if (id == null)
			return ValidationStatus.OK_STATUS;
		
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Receipt.class.getSimpleName()+" SET ")
		.append(Receipt.CLOSED_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(Receipt.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setBoolean(1, true);
            // WHERE
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
            return ValidationStatus.OK_STATUS;
        } catch (final SQLException e) {
        	throw new RuntimeException(e);
        } finally {
			dbLock.writeLock().unlock();
		}
	}
}
