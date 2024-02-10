package ro.linic.ui.legacy.jasper.datasource;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;

public class AccDocDatasource implements JRDataSource
{
	private AccountingDocument doc;
	private ImmutableList<Operatiune> ops;
    private int index = -1;
    
    public AccDocDatasource(final AccountingDocument factura)
    {
        super();
        this.doc = factura;
        this.ops = ImmutableList.copyOf(factura.getOperatiuni());
    }
    
    @Override
	public Object getFieldValue(final JRField field) throws JRException
    {
        final String fieldName = field.getName();
        
        if ("totalAchFaraTva".equals(fieldName))
            return doc.getAchizitieTotal().subtract(doc.getAchizitieTotalTva());
        if ("totalAchTva".equals(fieldName))
            return doc.getAchizitieTotalTva();
        if ("totalFaraTva".equals(fieldName))
            return doc.getVanzareTotal().subtract(doc.getVanzareTotalTva());
        if ("totalTva".equals(fieldName))
            return doc.getVanzareTotalTva();
        
        final Operatiune op = ops.get(index);
        
        switch (fieldName)
        {
	        case "barcode":
	        	return op.getBarcode();
	        	
	        case "name":
	        	return op.getName();
	        	
	        case "cantitate":
	        	return op.getCantitate();
	        	
	        case "id":
	        	return op.getId();
	        	
	        case "uom":
	        	return op.getUom();
	        	
	        case "pretUnitarAchizitieFaraTVA":
	        	return op.getPretUnitarAchizitieFaraTVA();
	        	
	        case "valoareAchizitieFaraTVA":
	        	return op.getValoareAchizitieFaraTVA();
	        	
	        case "valoareAchizitieTVA":
	        	return op.getValoareAchizitieTVA();
	        	
	        case "pretVanzareUnitarCuTVA":
	        	return op.getPretVanzareUnitarCuTVA();
	        	
	        case "valoareVanzareFaraTVA":
	        	return op.getValoareVanzareFaraTVA();
	        	
	        case "valoareVanzareTVA":
	        	return op.getValoareVanzareTVA();
	        	
	        case "gestiune":
	        	return op.getGestiune().getImportName();
	        	
	        case "categorie":
	        	return op.getCategorie();
        	
        	default:
        		return EMPTY_STRING;
        }
    }
    
    @Override
	public boolean next() throws JRException
    {
        return ++index < ops.size();
    }
}
