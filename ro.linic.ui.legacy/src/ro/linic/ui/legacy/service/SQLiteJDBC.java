package ro.linic.ui.legacy.service;

import static ro.colibri.util.ListUtils.toHashSet;
import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.adjustPrice;
import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.StringUtils.globalIsMatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.InvocationResult.Problem;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;

public class SQLiteJDBC
{
	private static final String LOCAL_DB_NAME = "colibri_local.db";
	private static final String PRIN_CASA_FIELD = "prinCasa";
	
	private static SQLiteJDBC instance;
	private Logger log;
	
	private SQLiteJDBC(final Bundle bundle, final Logger log)
	{
		this.log = log;
	}
	
	
	public void init()
	{
		Connection conn = null;
		try
		{
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:"+LOCAL_DB_NAME);
			createTables(conn);
			conn.close();
		}
		catch (final Exception e)
		{
			log.error(e);
		}
	}
	
	private void createTables(final Connection conn)
	{
		final StringBuilder usersSb = new StringBuilder();
		usersSb.append("CREATE TABLE IF NOT EXISTS users").append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(User.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(User.PASSWORD_FIELD+" text,").append(NEWLINE)
		.append(User.NAME_FIELD+" text,").append(NEWLINE)
		.append(User.EMAIL_FIELD+" text,").append(NEWLINE)
		.append(User.SELECTED_GESTIUNE_FIELD+" integer").append(NEWLINE)
		.append(");");
		
		final StringBuilder opSb = new StringBuilder();
		opSb.append("CREATE TABLE IF NOT EXISTS "+Operatiune.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Operatiune.ID_FIELD+" BIGINT PRIMARY KEY,").append(NEWLINE)
		.append(Operatiune.GESTIUNE_FIELD+" integer,").append(NEWLINE)
		.append(Operatiune.BARCODE_FIELD+" text,").append(NEWLINE)
		.append(Operatiune.NAME_FIELD+" text,").append(NEWLINE)
		.append(Operatiune.UM_FIELD+" text,").append(NEWLINE)
		.append(Operatiune.CANTITATE_FIELD+" numeric(12,4),").append(NEWLINE)
		.append(Operatiune.PRET_UNITAR_FIELD+" numeric(10,2),").append(NEWLINE)
		.append(Operatiune.VV_fTVA_FIELD+" numeric(12,2),").append(NEWLINE)
		.append(Operatiune.VV_TVA_FIELD+" numeric(10,2),").append(NEWLINE)
		.append(Operatiune.OPERATOR_FIELD+" integer,").append(NEWLINE)
		.append(Operatiune.DATA_OP_FIELD+" text,").append(NEWLINE)
		.append(Operatiune.DEPARTMENT_FIELD+" text,").append(NEWLINE)
		.append(Operatiune.COTA_TVA_FIELD+" text,").append(NEWLINE)
		.append(AccountingDocument.NR_DOC_FIELD+" integer,").append(NEWLINE)
		.append(AccountingDocument.DATA_DOC_FIELD+" text,").append(NEWLINE)
		.append(PRIN_CASA_FIELD+" boolean").append(NEWLINE)
		.append(");");
		
		final StringBuilder productsSb = new StringBuilder();
		productsSb.append("CREATE TABLE IF NOT EXISTS "+Product.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(Product.ID_FIELD+" integer PRIMARY KEY,").append(NEWLINE)
		.append(Product.BARCODE_FIELD+" text,").append(NEWLINE)
		.append(Product.NAME_FIELD+" text,").append(NEWLINE)
		.append(Product.UOM_FIELD+" text,").append(NEWLINE)
		.append(Product.UI_CATEGORY_FIELD+" text,").append(NEWLINE)
		.append(Product.PRICE_FIELD+" numeric(8,2)").append(NEWLINE)
		.append(");");
		
		final StringBuilder persPropSb = new StringBuilder();
		persPropSb.append("CREATE TABLE IF NOT EXISTS "+PersistedProp.class.getSimpleName()).append(NEWLINE)
		.append("(").append(NEWLINE)
		.append(PersistedProp.KEY_FIELD+" text PRIMARY KEY,").append(NEWLINE)
		.append(PersistedProp.VALUE_FIELD+" text").append(NEWLINE)
		.append(");");

		try (Statement stmt = conn.createStatement())
		{
//			stmt.execute(usersSb.toString());
			stmt.execute(opSb.toString());
//			stmt.execute(productsSb.toString());
			stmt.execute(persPropSb.toString());
		}
		catch (final SQLException e)
		{
			log.error(e);
		}
	}
	
	public InvocationResult addToBonCasa(AccountingDocument bonCasa, final Product product, BigDecimal cantitate, BigDecimal overridePret,
			final int gestId, final int userId, final BigDecimal tvaPercentDb)
	{
		Connection conn = null;
		try
		{
			if (bonCasa == null)
				bonCasa = new AccountingDocument().setNrDoc(AccountingDocument.BON_CASA_NR_NEINCHIS);
			bonCasa.setTipDoc(TipDoc.VANZARE);
			
			if (globalIsMatch(product.getCategorie(), Product.DISCOUNT_CATEGORY, TextFilterMethod.EQUALS))
				cantitate = cantitate.abs().negate();
			
			// calculeaza discountul procentual, doar daca nu avem deja discount procentual pe bon
			// discountul procentual se cumuleaza cu discountul valoric
			if (Product.isDiscountProcentual(product))
			{
				if (!bonCasa.getOperatiuni_Stream().filter(Product::isDiscountProcentual).findAny().isPresent())
				{
					cantitate = new BigDecimal("-1");
					overridePret = bonCasa.getTotal().multiply(product.getPricePerUom()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN).abs();
				}
				else
					return InvocationResult.canceled(Problem.code("AtBL_DPaa168").description("Pe bon exista deja un discount procentual!"));
			}
			
			final BigDecimal tvaPercent = product.deptTvaPercentage().orElse(tvaPercentDb);
			final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
			
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:"+LOCAL_DB_NAME);
			
			final Operatiune op = new Operatiune();
			op.setId(nextOpId(conn));
			op.setGestiune(new Gestiune().setId(gestId));
			op.setBarcode(product.getBarcode());
			op.setName(product.getName());
			op.setUom(product.getUom());
			op.setCategorie(product.getCategorie());
			Operatiune.fillAmounts(op, cantitate, overridePret, product, tvaPercent, tvaExtractDivisor);
			op.setPretVanzareUnitarCuTVA(adjustPrice(op.getPretVanzareUnitarCuTVA(), op.getCantitate()));
			Operatiune.updateAmounts(op, tvaPercent, tvaExtractDivisor);
			op.setOperator(new User().setId(userId));
			op.setDataOp(LocalDateTime.now());
			if (product.getDepartment() != null)
			{
				op.setDepartment(product.getDepartment().getName());
				op.setCotaTva(product.getDepartment().getCotaTva());
			}
			op.setAccDoc(bonCasa);
			bonCasa.getOperatiuni().add(op);
			// fields not needed for persist, but needed in VanzareBarPart
			op.setTipOp(TipOp.IESIRE);
			persistOp(conn, op);
			conn.close();
			return InvocationResult.ok(ImmutableMap.of(InvocationResult.ACCT_DOC_KEY, bonCasa,
					InvocationResult.PRODUCT_KEY, product));
		}
		catch (final Exception e)
		{
			log.error(e);
			return InvocationResult.canceled(Problem.code("addToBonCasa_local").description(e.getMessage()));
		}
	}
	
	private void persistOp(final Connection conn, final Operatiune op)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO "+Operatiune.class.getSimpleName())
		.append("(")
		.append(Operatiune.ID_FIELD).append(",")
		.append(Operatiune.GESTIUNE_FIELD).append(",")
		.append(Operatiune.BARCODE_FIELD).append(",")
		.append(Operatiune.NAME_FIELD).append(",")
		.append(Operatiune.UM_FIELD).append(",")
		.append(Operatiune.CANTITATE_FIELD).append(",")
		.append(Operatiune.PRET_UNITAR_FIELD).append(",")
		.append(Operatiune.VV_fTVA_FIELD).append(",")
		.append(Operatiune.VV_TVA_FIELD).append(",")
		.append(Operatiune.OPERATOR_FIELD).append(",")
		.append(Operatiune.DATA_OP_FIELD).append(",")
		.append(Operatiune.DEPARTMENT_FIELD).append(",")
		.append(Operatiune.COTA_TVA_FIELD).append(",")
		.append(AccountingDocument.NR_DOC_FIELD)
		.append(")").append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
        try (PreparedStatement pstmt = conn.prepareStatement(sb.toString()))
        {
        	pstmt.setLong(1, op.getId());
            pstmt.setInt(2, op.getGestiune().getId());
            pstmt.setString(3, op.getBarcode());
            pstmt.setString(4, op.getName());
            pstmt.setString(5, op.getUom());
            pstmt.setBigDecimal(6, op.getCantitate());
            pstmt.setBigDecimal(7, op.getPretVanzareUnitarCuTVA());
            pstmt.setBigDecimal(8, op.getValoareVanzareFaraTVA());
            pstmt.setBigDecimal(9, op.getValoareVanzareTVA());
            pstmt.setInt(10, op.getOperator().getId());
            pstmt.setString(11, op.getDataOp().toString());
            pstmt.setString(12, op.getDepartment());
            pstmt.setString(13, op.getCotaTva());
            pstmt.setInt(14, parseToInt(op.getAccDoc().getNrDoc()));
            pstmt.executeUpdate();
        }
        catch (final SQLException e)
        {
            log.error(e);
        }
	}
	
	private long nextOpId(final Connection conn)
	{
        final String sql = "SELECT MAX("+Operatiune.ID_FIELD+") FROM "+Operatiune.class.getSimpleName();
        long nextId = 1;
        
        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql))
        {
            if (rs.next())
            	nextId = rs.getLong(1)+1;
            
        }
        catch (final SQLException e)
        {
            log.error(e);
        }
        
        return nextId;
    }
	
	public InvocationResult deleteOperations(final AccountingDocument bonCasa, final ImmutableSet<Long> operationIds)
	{
		try
		{
			Class.forName("org.sqlite.JDBC");
			final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+LOCAL_DB_NAME);
			operationIds.forEach(opId -> deleteOp(conn, opId));
			conn.close();
			
			if (bonCasa != null)
				bonCasa.setOperatiuni(bonCasa.getOperatiuni_Stream()
						.filter(op -> !operationIds.contains(op.getId()))
						.collect(toHashSet()));
			
			return InvocationResult.ok();
		}
		catch (final Exception e)
		{
			log.error(e);
			return InvocationResult.canceled(Problem.code("deleteOperations_local").description(e.getMessage()));
		}
    }
	
	private void deleteOp(final Connection conn, final long opId)
	{
        final String sql = "DELETE FROM "+Operatiune.class.getSimpleName()+" WHERE "+Operatiune.ID_FIELD+" = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setLong(1, opId);
            pstmt.executeUpdate();
        }
        catch (final SQLException e)
        {
            log.error(e);
        }
    }
	
	public InvocationResult closeBonCasa(final AccountingDocument bonCasa, final boolean casaActive)
	{
		try
		{
			Class.forName("org.sqlite.JDBC");
			final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+LOCAL_DB_NAME);
			final LocalDateTime dataDoc = LocalDateTime.now();
			final int nrDoc = nextNrDoc(conn);
			bonCasa.setDataDoc(dataDoc);
			bonCasa.setNrDoc(String.valueOf(nrDoc));
			bonCasa.setDoc(casaActive ? AccountingDocument.BON_CASA_NAME : AccountingDocument.PROCES_VERBAL_NAME);
			
			bonCasa.getOperatiuni_Stream()
			.forEach(op -> updateOp(conn, nrDoc, dataDoc, casaActive, op.getId()));
			conn.close();
			return InvocationResult.ok(ImmutableMap.of(InvocationResult.ACCT_DOC_KEY, bonCasa));
		}
		catch (final Exception e)
		{
			log.error(e);
			return InvocationResult.canceled(Problem.code("closeBonCasa_local").description(e.getMessage()));
		}
	}
	
	private void updateOp(final Connection conn, final int nrDoc, final LocalDateTime dataDoc, final boolean prinCasa, final long opId)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "+Operatiune.class.getSimpleName()+" SET ")
		.append(AccountingDocument.NR_DOC_FIELD+" = ?,")
		.append(AccountingDocument.DATA_DOC_FIELD+" = ?,")
		.append(PRIN_CASA_FIELD+" = ? ")
		.append("WHERE id = ?");
		
        try (PreparedStatement pstmt = conn.prepareStatement(sb.toString()))
        {
            pstmt.setInt(1, nrDoc);
            pstmt.setString(2, dataDoc.toString());
            pstmt.setBoolean(3, prinCasa);
            pstmt.setLong(4, opId);
            pstmt.executeUpdate();
        }
        catch (final SQLException e)
        {
            log.error(e);
        }
    }
	
	private int nextNrDoc(final Connection conn)
	{
        final String sql = "SELECT MAX("+AccountingDocument.NR_DOC_FIELD+") FROM "+Operatiune.class.getSimpleName();
        int nextId = 1;
        
        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql))
        {
            if (rs.next())
            {
            	final int maxId = rs.getInt(1);
            	nextId = maxId == -1 ? 1 : maxId+1;
            }
            
        }
        catch (final SQLException e)
        {
            log.error(e);
        }
        
        return nextId;
    }
	
	public InvocationResult saveLocalToServer()
	{
		try
		{
			Class.forName("org.sqlite.JDBC");
			final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+LOCAL_DB_NAME);
			final Integer selectedGestId = ClientSession.instance().getLoggedUser().getSelectedGestiune().getId();
			
			final Builder<InvocationResult> results = ImmutableList.<InvocationResult>builder();
			final ImmutableList<Operatiune> filteredOps = localSavedOps(conn).stream()
					.filter(op -> op.getGestiune().getId() == selectedGestId)
					.collect(toImmutableList());
			Iterables.partition(filteredOps, 100).forEach(batchOps ->
			{
				final InvocationResult result = BusinessDelegate.saveLocalOps(ImmutableList.copyOf(batchOps));
				if (result.statusOk())
					batchOps.forEach(op -> deleteOp(conn, op.getId()));
				results.add(result);
			});
			
			conn.close();
			return InvocationResult.flatMap(results.build());
		}
		catch (final Exception e)
		{
			log.error(e);
			return InvocationResult.canceled(Problem.code("saveLocalToServer").description(e.getMessage()));
		}
	}
	
	private ImmutableList<Operatiune> localSavedOps(final Connection conn)
	{
		final Builder<Operatiune> b = ImmutableList.<Operatiune>builder();
		final StringBuilder opSb = new StringBuilder();
		opSb.append("SELECT ").append(NEWLINE)
		.append(Operatiune.ID_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.GESTIUNE_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.BARCODE_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.NAME_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.UM_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.CANTITATE_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.PRET_UNITAR_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.VV_fTVA_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.VV_TVA_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.OPERATOR_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.DATA_OP_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.DEPARTMENT_FIELD).append(LIST_SEPARATOR)
		.append(Operatiune.COTA_TVA_FIELD).append(LIST_SEPARATOR)
		.append(AccountingDocument.NR_DOC_FIELD).append(LIST_SEPARATOR)
		.append(AccountingDocument.DATA_DOC_FIELD).append(LIST_SEPARATOR)
		.append(PRIN_CASA_FIELD).append(NEWLINE)
		.append("FROM "+Operatiune.class.getSimpleName());
		
		try (Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(opSb.toString()))
		{
			// loop through the result set
			while (rs.next())
			{
				final Operatiune op = new Operatiune();
				op.setId(rs.getLong(Operatiune.ID_FIELD));
				op.setGestiune(new Gestiune().setId(rs.getInt(Operatiune.GESTIUNE_FIELD)));
				op.setBarcode(rs.getString(Operatiune.BARCODE_FIELD));
				op.setName(rs.getString(Operatiune.NAME_FIELD));
				op.setUom(rs.getString(Operatiune.UM_FIELD));
				op.setCantitate(rs.getBigDecimal(Operatiune.CANTITATE_FIELD));
				op.setPretVanzareUnitarCuTVA(rs.getBigDecimal(Operatiune.PRET_UNITAR_FIELD));
				op.setValoareVanzareFaraTVA(rs.getBigDecimal(Operatiune.VV_fTVA_FIELD));
				op.setValoareVanzareTVA(rs.getBigDecimal(Operatiune.VV_TVA_FIELD));
				op.setOperator(new User().setId(rs.getInt(Operatiune.OPERATOR_FIELD)));
				op.setDataOp(LocalDateTime.parse(rs.getString(Operatiune.DATA_OP_FIELD)));
				op.setDepartment(rs.getString(Operatiune.DEPARTMENT_FIELD));
				op.setCotaTva(rs.getString(Operatiune.COTA_TVA_FIELD));
				final AccountingDocument doc = new AccountingDocument();
				doc.setNrDoc(String.valueOf(rs.getInt(AccountingDocument.NR_DOC_FIELD)));
				final boolean isUnclosedBon = globalIsMatch(doc.getNrDoc(), AccountingDocument.BON_CASA_NR_NEINCHIS, TextFilterMethod.EQUALS);
				doc.setDataDoc(isUnclosedBon ? LocalDateTime.now() : LocalDateTime.parse(rs.getString(AccountingDocument.DATA_DOC_FIELD)));
				final boolean prinCasa = isUnclosedBon ? true : rs.getBoolean(PRIN_CASA_FIELD);
				doc.setDoc(prinCasa ? AccountingDocument.BON_CASA_NAME : AccountingDocument.PROCES_VERBAL_NAME);
				op.setAccDoc(doc);
				b.add(op);
			}
		}
		catch (final SQLException e)
		{
			log.error(e);
		}
		
		return b.build();
	}
}