package ro.linic.ui.legacy.tables.components;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.nebula.widgets.nattable.data.convert.ConversionFailedException;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;

import ro.colibri.entities.comercial.Gestiune;

public class GestiuneImportNameDisplayConverter extends DisplayConverter
{
	private static final Logger log = Logger.getLogger(GestiuneImportNameDisplayConverter.class.getSimpleName());
	
	public GestiuneImportNameDisplayConverter()
	{
		super();
	}
	
	@Override
    public Object canonicalToDisplayValue(final Object canonicalValue)
	{
        try
        {
            if (ObjectUtils.isNotNull(canonicalValue) && canonicalValue instanceof Gestiune)
            	return ((Gestiune) canonicalValue).getImportName();
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
