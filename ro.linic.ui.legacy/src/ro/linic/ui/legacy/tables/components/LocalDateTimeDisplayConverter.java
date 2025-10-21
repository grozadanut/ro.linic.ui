package ro.linic.ui.legacy.tables.components;

import static ro.colibri.util.LocalDateUtils.DATE_TIME_FORMATTER;
import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.LocalDateUtils.displayLocalDateTime;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.nebula.widgets.nattable.data.convert.ConversionFailedException;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;

public class LocalDateTimeDisplayConverter extends DisplayConverter
{
	private static final Logger log = Logger.getLogger(LocalDateTimeDisplayConverter.class.getSimpleName());
	
	private DateTimeFormatter format;

	public LocalDateTimeDisplayConverter(final DateTimeFormatter format)
	{
		super();
		this.format = format;
	}
	
	public LocalDateTimeDisplayConverter()
	{
		super();
		this.format = DATE_TIME_FORMATTER;
	}
	
	@Override
    public Object canonicalToDisplayValue(final Object canonicalValue)
	{
        try
        {
            if (ObjectUtils.isNotNull(canonicalValue))
            	if (canonicalValue instanceof LocalDateTime)
            		return displayLocalDateTime((LocalDateTime) canonicalValue, format);
            	else if (canonicalValue instanceof LocalDate)
            		return displayLocalDate((LocalDate) canonicalValue, format);
            	else if (canonicalValue instanceof Timestamp)
            		return displayLocalDateTime(((Timestamp) canonicalValue).toLocalDateTime(), format);
            	else if (canonicalValue instanceof Long)
            		return displayLocalDateTime(new Timestamp((Long) canonicalValue).toLocalDateTime(), format);
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
            return this.format.parse(displayValue.toString());
        }
        catch (final Exception e)
        {
            throw new ConversionFailedException(Messages.getString("LocalDateTimeDisplayConverter.failure", //$NON-NLS-1$
                    new Object[] { displayValue, this.format.toString() }), e);
        }
    }
}
