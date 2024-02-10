package ro.linic.ui.legacy.tables.components;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.nebula.widgets.nattable.data.convert.ConversionFailedException;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;

import ro.colibri.base.IPresentableEnum;

public class PresentableDisplayConverter extends DisplayConverter
{
	private static final Logger log = Logger.getLogger(PresentableDisplayConverter.class.getSimpleName());
	
	private String collectionSeparator = LIST_SEPARATOR;
	
	public PresentableDisplayConverter()
	{
		super();
	}
	
	public PresentableDisplayConverter setCollectionSeparator(final String collectionSeparator)
	{
		this.collectionSeparator = collectionSeparator;
		return this;
	}
	
	public String getCollectionSeparator()
	{
		return collectionSeparator;
	}
	
	@Override
    public Object canonicalToDisplayValue(final Object canonicalValue)
	{
        try
        {
            if (ObjectUtils.isNotNull(canonicalValue) && canonicalValue instanceof IPresentableEnum)
            	return ((IPresentableEnum) canonicalValue).displayName();
            else if (ObjectUtils.isNotNull(canonicalValue) && canonicalValue instanceof Collection<?>)
            	return ((Collection<?>) canonicalValue).stream()
            			.map(v -> v instanceof IPresentableEnum ? ((IPresentableEnum) v).displayName() : v.toString())
            			.collect(Collectors.joining(collectionSeparator));
        }
        catch (final Exception e)
        {
            log.log(Level.WARNING, "Display exception", e);
        }
        return canonicalValue;
    }

    @Override
    public Object displayToCanonicalValue(final Object displayValue)
    {
        try
        {
            return displayValue.toString();
        }
        catch (final Exception e)
        {
            throw new ConversionFailedException(Messages.getString("PresentableDisplayConverter.failure", //$NON-NLS-1$
                    new Object[] { displayValue }), e);
        }
    }
}
