package ro.linic.ui.legacy.jasper.datasource;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.ProductGestiuneMapping;
import ro.colibri.util.ServerConstants;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class ProductsDatasource implements JRDataSource
{
	private ImmutableList<Product> products;
    private int index = -1;
	private BigDecimal tvaPercentDb;
	private ImmutableList<Gestiune> allGestiuni;
    
    public ProductsDatasource(final ImmutableList<Product> products)
    {
        super();
        this.products = products;
        this.tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
        		.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
        this.allGestiuni = BusinessDelegate.allGestiuni();
    }
    
    @Override
	public Object getFieldValue(final JRField field) throws JRException
    {
        final String fieldName = field.getName();
        
        final Product p = products.get(index);
        
        switch (fieldName)
        {
	        case "tvaReadable":
	        	final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
	        	return Operatiune.tvaReadable(tvaPercent);

	        case "barcode":
	        	return p.getBarcode();
	        	
	        case "name":
	        	return p.getName();
	        	
	        case "id":
	        	return p.getId();
	        	
	        case "uom":
	        	return p.getUom();
	        	
	        case "pricePerUom":
	        	return p.getPricePerUom();
	        	
	        case "categorie":
	        	return p.getCategorie();

	        case "stocL1":
	        	return p.getStocuri().stream()
	        			.filter(pgm -> pgm.getGestiune().isMatch(ServerConstants.L1_NAME))
	        			.map(ProductGestiuneMapping::getStoc)
	        			.findFirst()
	        			.orElse(null);
	        	
	        case "stocL2":
	        	return p.getStocuri().stream()
	        			.filter(pgm -> pgm.getGestiune().isMatch(ServerConstants.L2_NAME))
	        			.map(ProductGestiuneMapping::getStoc)
	        			.findFirst()
	        			.orElse(null);
	        	
        	default:
        		return EMPTY_STRING;
        }
    }
    
    @Override
	public boolean next() throws JRException
    {
        return ++index < products.size();
    }
}
