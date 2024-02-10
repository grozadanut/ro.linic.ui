package ro.linic.ui.legacy.tables.components;

import static ro.colibri.util.ListUtils.toImmutableList;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.BackgroundPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.CellStyleUtil;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Operatiune.Recenzie;

public class RecenziiImagePainter extends BackgroundPainter
{
	private Map<Recenzie, Image> recenziiImages = new HashMap<>();
	private final boolean paintBg;
	private int spacing;

	/**
	 * Creates a combination painter
	 *
	 * @param spacing number of pixels of spacing between each image. It can be -ve,
	 *                in that case images will overlap
	 * @param paintBg paint the background or not
	 */
	public RecenziiImagePainter(final int spacing, final boolean paintBg)
	{
		this.spacing = spacing;
		this.paintBg = paintBg;
	}

	public RecenziiImagePainter add(final Recenzie recenzie, final Image image)
	{
		recenziiImages.put(recenzie, image);
		return this;
	}

	@Override
	public int getPreferredWidth(final ILayerCell cell, final GC gc, final IConfigRegistry configRegistry)
	{
		int width = 0;
		final IStyle cellStyle = CellStyleUtil.getCellStyle(cell, configRegistry);
        setupGCFromConfig(gc, cellStyle);
		for (final Entry<Image, Long> imageCount : imageCount(cell).entrySet())
		{
			if (width != 0)
				width += spacing;
			width += gc.textExtent(imageCount.getValue()+"x").x;
			width += imageCount.getKey().getBounds().width;
		}

		return width;
	}

	protected ImmutableList<Image> getImages(final ILayerCell cell)
	{
		final Object cellValue = cell.getDataValue();
		
		if (cellValue instanceof ImmutableList<?>)
			return ((ImmutableList<Recenzie>) cellValue).stream()
					.map(recenziiImages::get)
					.collect(toImmutableList());

		return ImmutableList.of();
	}
	
	protected Map<Image, Long> imageCount(final ILayerCell cell)
	{
		return getImages(cell).stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	@Override
	public int getPreferredHeight(final ILayerCell cell, final GC gc, final IConfigRegistry configRegistry)
	{
		int height = 0;
		for (final Image image : recenziiImages.values())
			height = Math.max(height, image.getBounds().height);

		return height;
	}

	@Override
	public void paintCell(final ILayerCell cell, final GC gc, final Rectangle bounds, final IConfigRegistry configRegistry)
	{
		if (this.paintBg)
		{
			super.paintCell(cell, gc, bounds, configRegistry);
		}

		final IStyle cellStyle = CellStyleUtil.getCellStyle(cell, configRegistry);
		setupGCFromConfig(gc, cellStyle);
		final int fontHeight = gc.getFontMetrics().getHeight();
		
		final Map<Image, Long> imageCount = imageCount(cell);
		int boundX = bounds.x + CellStyleUtil.getHorizontalAlignmentPadding(cellStyle, bounds,
				getPreferredWidth(cell, gc, configRegistry));
		final int paddingY = CellStyleUtil.getVerticalAlignmentPadding(cellStyle, bounds,
				getPreferredHeight(cell, gc, configRegistry));

		for (final Entry<Image, Long> entry : imageCount.entrySet())
		{
			final Rectangle imageBounds = entry.getKey().getBounds();
			final String text = entry.getValue()+"x";
			final int centerToImage = (imageBounds.height/2)-(fontHeight/2)-1;
			gc.drawText(text, boundX, bounds.y + paddingY + centerToImage);
			boundX += gc.textExtent(text).x;
			
			gc.drawImage(entry.getKey(), boundX, bounds.y + paddingY);
			boundX += imageBounds.width + spacing;
		}
	}
	
	/**
     * Setup the GC by the values defined in the given cell style.
     *
     * @param gc
     * @param cellStyle
     */
    public void setupGCFromConfig(final GC gc, final IStyle cellStyle)
    {
        final Color fg = cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR);
        final Color bg = cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR);
        final Font font = cellStyle.getAttributeValue(CellStyleAttributes.FONT);

        gc.setAntialias(GUIHelper.DEFAULT_ANTIALIAS);
        gc.setTextAntialias(GUIHelper.DEFAULT_TEXT_ANTIALIAS);
        gc.setFont(font);
        gc.setForeground(fg != null ? fg : GUIHelper.COLOR_LIST_FOREGROUND);
        gc.setBackground(bg != null ? bg : GUIHelper.COLOR_LIST_BACKGROUND);
    }
}
