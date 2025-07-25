package ro.linic.ui.legacy.service.impl;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

import ro.colibri.util.InvocationResult;
import ro.linic.ui.base.services.LocalDatabase;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ReceiptLoader;
import ro.linic.ui.pos.base.services.ReceiptUpdater;
import ro.linic.ui.pos.base.services.SQLiteHelper;
import ro.linic.ui.pos.cloud.model.CloudReceipt;

@Component(property = org.osgi.framework.Constants.SERVICE_RANKING + "=1")
public class LegacyReceiptUpdater implements ReceiptUpdater {
	@Reference private LocalDatabase localDatabase;
	@Reference private SQLiteHelper sqliteHelper;
	@Reference private ReceiptLoader receiptLoader;
	
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
        	((CloudReceipt) model).setNumber(nextNumber(dbName));
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
        final String sql = "SELECT MAX("+CloudReceipt.ID_FIELD+") FROM "+Receipt.class.getSimpleName() +
        		" WHERE "+CloudReceipt.ID_FIELD+" < 10000";
        long nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
        }
        
        return nextId;
    }
	
	private int nextNumber(final String dbName) throws SQLException {
        final String sql = "SELECT MAX("+CloudReceipt.NUMBER_FIELD+") FROM "+Receipt.class.getSimpleName() +
        		" WHERE "+CloudReceipt.CREATION_TIME_FIELD +
        		" LIKE '"+LocalDate.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd"))+"%'";
        int nextId = 1;
        
        try (Statement stmt  = localDatabase.getConnection(dbName).createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next())
            	nextId = rs.getInt(1)+1;
        }
        
        return nextId;
    }

	@Override
	public IStatus update(final long id, final Receipt m) {
		if (m == null || m.getId() == null)
			return ValidationStatus.OK_STATUS;
		
		final CloudReceipt model = (CloudReceipt) m;
		final IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(PreferenceKey.class).getSymbolicName());
		final String dbName = node.get(PreferenceKey.LOCAL_DB_NAME, PreferenceKey.LOCAL_DB_NAME_DEF);
		final ReadWriteLock dbLock = localDatabase.getLock(dbName);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Receipt.class.getSimpleName()+" SET ")
		.append(CloudReceipt.ID_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.CHARGE_INDICATOR_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(CloudReceipt.ALLOWANCE_CHARGE_FIELD+"_"+AllowanceCharge.AMOUNT_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(CloudReceipt.SYNCED_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(CloudReceipt.CREATION_TIME_FIELD+" = ?").append(LIST_SEPARATOR)
		.append(CloudReceipt.NUMBER_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(CloudReceipt.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	pstmt.setLong(1, model.getId());
        	pstmt.setBoolean(2, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::chargeIndicator).orElse(false));
        	pstmt.setBigDecimal(3, Optional.ofNullable(model.getAllowanceCharge()).map(AllowanceCharge::amount).orElse(null));
        	pstmt.setObject(4, model.getSynced(), Types.BOOLEAN);
        	pstmt.setString(5, model.getCreationTime().toString());
        	pstmt.setObject(6, model.getNumber(), Types.INTEGER);
            // WHERE
            pstmt.setLong(7, id);
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
		.append(CloudReceipt.CLOSED_FIELD+" = ?").append(NEWLINE)
		.append("WHERE").append(NEWLINE)
		.append(CloudReceipt.ID_FIELD+" = ?");
		
		dbLock.writeLock().lock();
        try (PreparedStatement pstmt = localDatabase.getConnection(dbName).prepareStatement(sb.toString())) {
        	final IStatus remoteResult = closeReceiptRemote(id);
        	if (!remoteResult.isOK())
        		return remoteResult;
        	
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

	private IStatus closeReceiptRemote(final long id) {
		/**
		 * The only workflow for the receipt to be synchronized and unclosed here is:
		 * 1. user opens CloseFacturaBCWizard(thus receipt is synchronized)
		 * 2. user closes the wizard
		 * 3. user closes the receipt by cash/card
		 * 4. user closes the receipt by CloseFacturaBCWizard as a BonConsum; in this case 
		 * we need to close the receipt locally, because on the server it is already closed 
		 * by the time this method is called(after InchideBonWizardDialog is closed)
		 * 
		 * In this case, the receipt id is equal to the remote AccDoc id
		 */
		final Optional<CloudReceipt> receipt = receiptLoader.findById(id)
				.map(CloudReceipt.class::cast)
				.filter(r -> r.synced() && !r.closed());
		
		if (receipt.isEmpty())
			return ValidationStatus.OK_STATUS;
		
		final InvocationResult result = BusinessDelegate.closeBonCasa(id, true);
		
		if (result.toTextCodes().contains("CB_BCls201"))
			return ValidationStatus.OK_STATUS; // see case 4 above
		
		return result.statusOk() ? ValidationStatus.OK_STATUS : ValidationStatus.error(result.toTextDescription());
	}
}
