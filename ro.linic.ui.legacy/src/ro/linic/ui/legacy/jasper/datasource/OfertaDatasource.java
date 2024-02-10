package ro.linic.ui.legacy.jasper.datasource;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;

public class OfertaDatasource implements JRDataSource
{
	private AccountingDocument doc;
	private ImmutableList<Operatiune> ops;
    private int index = -1;
    
    public OfertaDatasource(final AccountingDocument doc)
    {
        super();
        this.doc = doc;
        this.ops = ImmutableList.copyOf(doc.getOperatiuni());
    }
    
    @Override
	public Object getFieldValue(final JRField field) throws JRException
    {
        final String fieldName = field.getName();
        
        if ("total".equals(fieldName))
            return doc.getTotal();
        
        final Operatiune op = ops.get(index);
        
        if ("barcode".equals(fieldName))
            return op.getBarcode();
        else if ("name".equals(fieldName))
            return op.getName();
        else if ("uom".equals(fieldName))
            return op.getUom();
        else if ("cantitate".equals(fieldName))
            return op.getCantitate();
        else if ("pretVanzareUnitarCuTVA".equals(fieldName))
            return op.getPretVanzareUnitarCuTVA();
        else if ("valoare".equals(fieldName))
            return op.getValoareVanzareFaraTVA().add(op.getValoareVanzareTVA());
        else if ("id".equals(fieldName))
            return op.getId();
        
        return "";
    }
    
    @Override
	public boolean next() throws JRException
    {
        return ++index < ops.size();
    }
}
