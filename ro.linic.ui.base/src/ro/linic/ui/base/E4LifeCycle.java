package ro.linic.ui.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.LogManager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.osgi.framework.FrameworkUtil;

import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.p2.ui.Policy;

public class E4LifeCycle {

	@PostContextCreate
	void postContextCreate(final IEclipseContext ctx) {
		registerLogHandler();
		registerP2Policy(ctx);
		initDefaultFonts();
	}
	
	private void registerLogHandler() {
		try {
			final URL baseUrl = FrameworkUtil.getBundle(getClass()).getEntry("logging.properties");
			final URL localURL = FileLocator.toFileURL(baseUrl);
            LogManager.getLogManager().readConfiguration(new FileInputStream(localURL.getFile()));
        } catch (IOException | SecurityException ex) {
        	ex.printStackTrace();
        }
	}

	private void registerP2Policy(final IEclipseContext ctx) {
		ctx.set(Policy.class, new CloudPolicy(ctx));
	}
	
	private void initDefaultFonts()
	{
		final int defaultFontSize = Integer.parseInt(System.getProperty(UIUtils.FONT_SIZE_KEY, UIUtils.FONT_SIZE_DEFAULT)); //14
		
		final FontData[] bannerFD = JFaceResources.getBannerFont().getFontData();
		bannerFD[0].setHeight(defaultFontSize+2);//16
		JFaceResources.getFontRegistry().put(JFaceResources.BANNER_FONT, bannerFD);

		final FontData[] dialogFD = JFaceResources.getDialogFont().getFontData();
		dialogFD[0].setHeight(defaultFontSize-2);//12
		JFaceResources.getFontRegistry().put(JFaceResources.DIALOG_FONT, dialogFD);
		
		final FontData[] defaultFD = JFaceResources.getDefaultFont().getFontData();
		defaultFD[0].setHeight(defaultFontSize);//14
		JFaceResources.getFontRegistry().put(JFaceResources.DEFAULT_FONT, defaultFD);
		
		final FontData[] extraBannerFD = JFaceResources.getBannerFont().getFontData();
		extraBannerFD[0].setHeight(defaultFontSize+4);//18
		JFaceResources.getFontRegistry().put(UIUtils.EXTRA_BANNER_FONT, extraBannerFD);
		
		final FontData[] xxBannerFD = JFaceResources.getBannerFont().getFontData();
		xxBannerFD[0].setHeight(defaultFontSize+6);//20
		JFaceResources.getFontRegistry().put(UIUtils.XX_BANNER_FONT, xxBannerFD);
		
		final FontData[] xxxBannerFD = JFaceResources.getBannerFont().getFontData();
		xxxBannerFD[0].setHeight(defaultFontSize+8);//22
		JFaceResources.getFontRegistry().put(UIUtils.XXX_BANNER_FONT, xxxBannerFD);
	}
}
