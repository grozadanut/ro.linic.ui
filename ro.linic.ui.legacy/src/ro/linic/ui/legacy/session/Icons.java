package ro.linic.ui.legacy.session;

import static ro.colibri.util.StringUtils.isEmpty;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

public class Icons
{
	public static final String LOGO_256x256_PATH = "icons/logo_256x256.png";
	public static final String TRASH_16X16_PATH = "icons/trash_16x16.png";
	public static final String SMILEY_32x32_PATH = "icons/smiley_32x32.png";
	public static final String OK_SMILEY_32x32_PATH = "icons/ok_32x32.png";
	public static final String SAD_32x32_PATH = "icons/sad_32x32.png";
	public static final String OK_16x16_PATH = "icons/verify_16x16.png";
	public static final String LOADING_16x16_PATH = "icons/loading_16x16.png";
	public static final String ERROR_16x16_PATH = "icons/error_16x16.png";
	public static final String USA_FLAG_32x32_PATH = "icons/usa_flag_32x32.png";
	public static final String RO_FLAG_32x32_PATH = "icons/ro_flag_32x32.png";
	
	private static final Map<String, byte[]> imageCache = new HashMap<>();
	private static URL localURL;
	
	private static URL localURL(final Bundle bundle) throws IOException
	{
		if (localURL == null)
		{
			final URL baseUrl = bundle.getEntry("/");
			localURL = FileLocator.toFileURL(baseUrl);
		}
		return localURL;
	}
	
	/**
	 * TAKE CARE TO DISPOSE THE CREATED IMAGE!!!
	 * @param bundle 
	 */
	public static Optional<Image> createImage(final Bundle bundle, final String imgPath, final Logger log)
	{
		final URL url = FileLocator.find(bundle, new Path(imgPath));
		URL fileUrl;
		try
		{
			fileUrl = FileLocator.toFileURL(url);
		}
		catch (final IOException e)
		{
			log.error(e);
			return Optional.empty();
		}
		return Optional.of(new Image(Display.getCurrent(), fileUrl.getFile()));
	}
	
	/**
	 * The returned image should not be disposed as it is taken care of by JFaceResources
	 */
	public static Optional<Image> createImageResource(final Bundle bundle, final String imgPath, final ILog log)
	{
		Image img = JFaceResources.getImage(imgPath);
		
		if (img != null)
			return Optional.of(img);
		
		final URL url = FileLocator.find(bundle, new Path(imgPath));
		try
		{
			final URL fileUrl = FileLocator.toFileURL(url);
			img = new Image(Display.getCurrent(), fileUrl.getFile());
			JFaceResources.getImageRegistry().put(imgPath, img);
		}
		catch (final IOException e)
		{
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
		return Optional.ofNullable(img);
	}
	
	/**
	 * The returned image doesn't need to be disposed
	 */
	public static Optional<java.awt.Image> createAWTImage(final Bundle bundle, final String imgPath, final Logger log)
	{
		final URL url = FileLocator.find(bundle, new Path(imgPath));
		try
		{
			final URL fileUrl = FileLocator.toFileURL(url);
			return Optional.ofNullable(ImageIO.read(fileUrl));
		}
		catch (final Exception e)
		{
			log.error(e);
			return Optional.empty();
		}
	}
	
	public static byte[] imgPathToBytes(final String path) throws IOException
	{
		final File file = new File(path);
		final byte[] picInBytes = new byte[(int) file.length()];
		final FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(picInBytes);
		fileInputStream.close();
		return picInBytes;
	}
	
	public static byte[] imgRelativePathToBytes(final Bundle bundle, final String relativePath) throws IOException
	{
		byte[] picInBytes = imageCache.get(relativePath);
		
		if (picInBytes == null)
		{
			final URL fileUrl = new URL(localURL(bundle).toString()+relativePath);
			final File file = new File(fileUrl.getFile());
			picInBytes = new byte[(int) file.length()];
			final FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(picInBytes);
			fileInputStream.close();
			imageCache.put(relativePath, picInBytes);
		}
		return picInBytes;
	}
	
	public static byte[] imageFromCache(final String uuid)
	{
		return imageCache.get(BusinessDelegate.productImageFilename(uuid));
	}
	
	public static void putImageInCache(final String uuid, final byte[] img)
	{
		if (!isEmpty(uuid) && img != null)
			imageCache.put(BusinessDelegate.productImageFilename(uuid), img);
	}
	
	public static boolean imgRelativePathExists(final Bundle bundle, final Logger log, final String relativePath)
	{
		try
		{
			if (imageCache.containsKey(relativePath))
				return true;
			
			final URL fileUrl = new URL(localURL(bundle).toString()+relativePath);
			return Files.exists(Paths.get(fileUrl.toURI()));
		}
		catch (final Exception e)
		{
			log.error(e);
			return false;
		}
	}
	
	public static void saveImageBytesToRelativeFile(final Bundle bundle, final String relativePath,
			final byte[] imgBytes) throws IOException
	{
		final URL fileUrl = new URL(localURL(bundle).toString()+relativePath);
		final File file = new File(fileUrl.getFile());
		final FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write(imgBytes);
		fileOutputStream.close();
	}

	public static Image imageFromBytes(final byte[] bytes) throws IOException
	{
		final Image cachedImg = JFaceResources.getImage(bytes.toString());

		if (cachedImg != null)
			return cachedImg;

		final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		final Image img = new Image(Display.getCurrent(), bis);
		bis.close();
		JFaceResources.getImageRegistry().put(bytes.toString(), img);
		return img;
	}

	public static Image imageFromBytes(final byte[] bytes, final int height) throws IOException
	{
		if (bytes == null)
			return null;
		
		final Image cachedImg = JFaceResources.getImage(bytes.toString());

		if (cachedImg != null)
			return cachedImg;

		final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		final java.awt.Image awtImage = ImageIO.read(bis)
				.getScaledInstance(-1, height, java.awt.Image.SCALE_SMOOTH);
		bis.close();
		final Image img = new Image(Display.getCurrent(), convertToSWT(awtImage));
		JFaceResources.getImageRegistry().put(bytes.toString(), img);
		return img;
	}

	private static ImageData convertToSWT(final java.awt.Image image)
	{
		if (image == null)
			throw new IllegalArgumentException("Null 'image' argument.");

		final int w = image.getWidth(null);
		final int h = image.getHeight(null);
		if (w == -1 || h == -1)
			return null;

		final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final Graphics g = bi.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return convertToSWT(bi);
	}

	private static ImageData convertToSWT(final BufferedImage bufferedImage)
	{
		if (bufferedImage.getColorModel() instanceof DirectColorModel)
		{
			final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			final PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
					colorModel.getBlueMask());
			final ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			final WritableRaster raster = bufferedImage.getRaster();
			final int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++)
			{
				for (int x = 0; x < data.width; x++)
				{
					raster.getPixel(x, y, pixelArray);
					final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel)
		{
			final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			final int size = colorModel.getMapSize();
			final byte[] reds = new byte[size];
			final byte[] greens = new byte[size];
			final byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			final RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++)
			{
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			final PaletteData palette = new PaletteData(rgbs);
			final ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			final WritableRaster raster = bufferedImage.getRaster();
			final int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++)
			{
				for (int x = 0; x < data.width; x++)
				{
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		throw new IllegalArgumentException("bufferedImage.getColorModel() is "+bufferedImage.getColorModel());
	}
}
