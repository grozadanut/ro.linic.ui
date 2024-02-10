package ro.linic.ui.legacy.tables.components;

import java.io.IOException;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.Icons;

public class UuidImagePainter extends ImagePainter
{
	public static final int DEFAULT_HEIGHT = 120;
	
	private Bundle bundle;
	private Logger log;
	
	public UuidImagePainter(final Bundle bundle, final Logger log)
	{
		super();
		this.bundle = bundle;
		this.log = log;
	}

	public UuidImagePainter(final boolean paintBg, final Bundle bundle, final Logger log)
	{
		super(paintBg);
		this.bundle = bundle;
		this.log = log;
	}

	public UuidImagePainter(final Image image, final boolean paintBg, final Bundle bundle, final Logger log)
	{
		super(image, paintBg);
		this.bundle = bundle;
		this.log = log;
	}

	public UuidImagePainter(final Image image, final Bundle bundle, final Logger log)
	{
		super(image);
		this.bundle = bundle;
		this.log = log;
	}

	@Override
	protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry)
	{
		final Image superImg = super.getImage(cell, configRegistry);
		
		if (superImg!= null)
			return superImg;
		
		final Object cellValue = cell.getDataValue();
		
		if (cellValue instanceof byte[])
			try
			{
				return Icons.imageFromBytes((byte[]) cellValue, DEFAULT_HEIGHT);
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		else if (cellValue instanceof String)
		{
			final String uuid = (String) cellValue;
			try
			{
				return Icons.imageFromBytes(BusinessDelegate.imageFromUuid(bundle, log, uuid, false), DEFAULT_HEIGHT);
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
}