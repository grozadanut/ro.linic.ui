package ro.linic.ui.legacy.jasper.datasource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Optional;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class OfertaCuDiscountDatasource implements JRDataSource
{
	private boolean withImage;
	private ImmutableList<Product> products;
	private BigDecimal discountPercentage;
	private BigDecimal cappedAdaos;
    private int index = -1;
    private BigDecimal tvaPercentDb;
    
    private Bundle bundle;
    private Logger log;
    
    public OfertaCuDiscountDatasource(final boolean withImage, final ImmutableList<Product> products,
    		final BigDecimal discountPercentage, final BigDecimal cappedAdaos, final Bundle bundle, final Logger log)
    {
        super();
        this.withImage = withImage;
        this.products = products;
        this.discountPercentage = discountPercentage;
        this.cappedAdaos = cappedAdaos;
        this.tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
        		.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
        this.bundle = bundle;
        this.log = log;
    }
    
    @Override
	public Object getFieldValue(final JRField field) throws JRException
    {
        final String fieldName = field.getName();
        
        final Product p = products.get(index);
        
        final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
        final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
        
        if ("barcode".equals(fieldName))
            return p.getBarcode();
        else if ("name".equals(fieldName))
            return p.getName();
        else if ("uom".equals(fieldName))
            return p.getUom();
        else if ("cantitate".equals(fieldName))
            return BigDecimal.ONE;
        else if ("pricePerUom".equals(fieldName))
            return p.getPricePerUom();
        else if ("priceAfterDiscount".equals(fieldName))
            return p.getPricePerUomAfterDiscount(discountPercentage, cappedAdaos, tvaExtractDivisor);
        else if ("valoare".equals(fieldName))
            return p.getPricePerUomAfterDiscount(discountPercentage, cappedAdaos, tvaExtractDivisor);
        else if ("id".equals(fieldName))
            return p.getId();
        else if ("image".equals(fieldName))
        	return withImage ? Optional.ofNullable(BusinessDelegate.imageFromUuid(bundle, log, p.getImageUUID(), true))
        			.map(ByteArrayInputStream::new)
        			.orElse(null) : null;
        
        return "";
    }
    
    @Override
	public boolean next() throws JRException
    {
        return ++index < products.size();
    }
}
