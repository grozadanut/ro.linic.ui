package ro.linic.ui.legacy.service;

import static ro.colibri.util.StringUtils.isEmpty;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.Supplier;

import javax.print.DocPrintJob;
import javax.print.PrintService;

import org.eclipse.core.runtime.ILog;
import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;

import jssc.SerialPortException;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.legacy.service.components.BarcodePrintable;

final class WindowsPeripheralService extends PeripheralService
{
	private static final ILog log = UIUtils.logger(WindowsPeripheralService.class);
	
	private static final double WIDTH_INCH = 1.6d;
	private static final double HEIGHT_INCH = 1d;
	private static final double MARGIN_INCH = 0.1d;
	private static final String FONT = "Tahoma";
	private static final double DPI = 72d;
	
	@Override
	public synchronized void printWeightedLabel(final BarcodePrintable printable, final BigDecimal totalUnits, final BigDecimal totalPrice) throws IOException
	{
        final PrinterJob pj = PrinterJob.getPrinterJob();
        if (pj.printDialog())
        {
            final PageFormat pf = pj.defaultPage();
            final Paper paper = pf.getPaper();
            final double width = WIDTH_INCH * DPI;
            final double height = HEIGHT_INCH * DPI;
            final double margin = MARGIN_INCH * DPI;
            paper.setSize(width, height);
            paper.setImageableArea(
            		margin*2,
            		margin,
                    width - (margin * 2),
                    height - (margin * 2));
            pf.setOrientation(PageFormat.PORTRAIT);
            pf.setPaper(paper);
            pj.validatePage(pf);

            final Book pBook = new Book();
            pBook.append(new WeightedPricePage(printable, totalUnits, totalPrice), pf);
            pj.setPageable(pBook);

            try
            {
                pj.print();
            }
            catch (final PrinterException ex)
            {
            	log.error(ex.getMessage(), ex);
            }
        }
    }
	
	@Override
	public synchronized void printPriceLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		defaultPricePrinter(printable.getCantitate(), () -> new PricePage(printable), printerName);
    }
	
	@Override
	public synchronized void printBigPriceLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		defaultPricePrinter(printable.getCantitate(), () -> new BigPricePage(printable), printerName);
	}
	
	@Override
	public synchronized void printCustomLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		defaultPricePrinter(printable.getCantitate(), () -> new CustomLabelPage(printable), printerName);
	}
	
	@Override
	public synchronized void printKeys(final String keysToPrint) throws IOException, SerialPortException, InterruptedException
	{
        final PrinterJob pj = PrinterJob.getPrinterJob();
        if (pj.printDialog()) {
            final PageFormat pf = pj.defaultPage();
            final Paper paper = pf.getPaper();
            final double width = WIDTH_INCH * DPI;
            final double height = HEIGHT_INCH * DPI;
            final double margin = MARGIN_INCH * DPI;
            paper.setSize(width, height);
            paper.setImageableArea(
            		margin*2,
            		margin,
                    width - (margin * 2),
                    height - (margin * 2));
            pf.setOrientation(PageFormat.PORTRAIT);
            pf.setPaper(paper);
            pj.validatePage(pf);

            final Book pBook = new Book();
            final String[] keyRange = keysToPrint.split(KEYS_RANGE_SPLIT);
            
            if (keyRange.length == 2)
            {
            	final int min = Math.min(Integer.parseInt(keyRange[0]), Integer.parseInt(keyRange[1]));
            	final int max = Math.max(Integer.parseInt(keyRange[0]), Integer.parseInt(keyRange[1]));
            	
            	for (int i = min; i <= max; i++)
            		pBook.append(new KeyPage(i), pf);
            }
            else if (keyRange.length == 1)
            	pBook.append(new KeyPage(Integer.parseInt(keyRange[0])), pf);
            else
            	throw new IllegalArgumentException("Key format not accepted: "+keysToPrint);
            
            pj.setPageable(pBook);

            try
            {
                pj.print();
            }
            catch (final PrinterException ex)
            {
            	log.error(ex.getMessage(), ex);
            }
        }
    }
	
	@Override
	public boolean isPrinterConnected(final String printerName)
	{
		// weird bug, first time returns empty, subsequent times work
		PrinterJob.lookupPrintServices();
		final PrintService[] service = PrinterJob.lookupPrintServices(); // list of printers
		for (int i = 0; i < service.length; i++)
		    if (service[i].getName().equalsIgnoreCase(printerName))
		    	return true;
		
		return false;
	}
	
	private synchronized void defaultPricePrinter(final Integer noPriceLabels, final Supplier<Printable> pageSupplier, final String printerName)
	{
		if (noPriceLabels == null || noPriceLabels <= 0)
			return;
		
		final PrintService[] service = PrinterJob.lookupPrintServices(); // list of printers
		DocPrintJob docPrintJob = null;

		log.info("No. Printers: "+service.length);
		for (int i = 0; i < service.length; i++)
		{
			log.info(service[i].getName());
		    if (service[i].getName().equalsIgnoreCase(printerName))
		    {
		        docPrintJob = service[i].createPrintJob();
		        break;
		    }
		}
		
		final PrinterJob pj = PrinterJob.getPrinterJob();
		try
		{
			if (docPrintJob == null)
				throw new PrinterException("Printer not found");
			
			pj.setPrintService(docPrintJob.getPrintService());
		}
		catch (final PrinterException e)
		{
			log.error(e.getMessage(), e);
			if (!pj.printDialog())
				return;
		}
		final PageFormat pf = pj.defaultPage();
		final Paper paper = pf.getPaper();
		final double width = WIDTH_INCH * DPI;
		final double height = HEIGHT_INCH * DPI;
		final double margin = MARGIN_INCH * DPI;
		paper.setSize(width, height);
		paper.setImageableArea(
				margin,
				margin,
				width - (margin * 2),
				height - (margin * 2));
		pf.setOrientation(PageFormat.PORTRAIT);
		pf.setPaper(paper);
		pj.validatePage(pf);

		final Book pBook = new Book();
		for (int i = 0; i < noPriceLabels; i++)
			pBook.append(pageSupplier.get(), pf);

		pj.setPageable(pBook);

		try
		{
			pj.print();
		}
		catch (final PrinterException ex)
		{
			log.error(ex.getMessage(), ex);
		}
	}
	
	
	public static class PricePage implements Printable
	{
		private BarcodePrintable printable;
		
        public PricePage(final BarcodePrintable printable)
        {
			this.printable = printable;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
		{
//            if (pageIndex >= 1)
//                return Printable.NO_SUCH_PAGE;

            final Graphics2D g2d = (Graphics2D) graphics;
            // Be careful of clips...
            g2d.translate((int) pageFormat.getImageableX()+1, (int) pageFormat.getImageableY());

            final double width = pageFormat.getImageableWidth();
            final double height = pageFormat.getImageableHeight();

            // denumire
            writeNameToGc(g2d, printable, width);

            // pret/UM
            final String price = printable.getPricePerUom().setScale(2).toString();
            g2d.setFont(new Font(FONT, Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            final double priceX = width/2 - fm.stringWidth(price)/2 - (printable.getPricePerUom().intValue() > 999 ? 10 : 0);
            final double priceY = height/2;// + fm.getAscent();
            g2d.drawString(price, (int)priceX-5, (int)priceY);
            
            final String uom = "lei/"+printable.getUom();
            g2d.setFont(new Font(FONT, Font.PLAIN, 8));
            fm = g2d.getFontMetrics();
            final double uomX = width - fm.stringWidth(uom);
            g2d.drawString(uom, (int)uomX, (int)priceY);
            
            // cod de bare
            writeBarcodeToGc(g2d, printable, (int)priceY+5);

            return Printable.PAGE_EXISTS;
        }
    }
	
	public static class BigPricePage implements Printable
	{
		private BarcodePrintable printable;
		
        public BigPricePage(final BarcodePrintable printable)
        {
			this.printable = printable;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
		{
            final Graphics2D g2d = (Graphics2D) graphics;
            // Be careful of clips...
            g2d.translate((int) pageFormat.getImageableX()+1, (int) pageFormat.getImageableY());

            final double width = pageFormat.getImageableWidth();
            final double height = pageFormat.getImageableHeight();

            // denumire
            writeNameToGc(g2d, printable, width);

            // pret/UM
            final String price = printable.getPricePerUom().setScale(2).toString();
            g2d.setFont(new Font(FONT, Font.PLAIN, 26));
            FontMetrics fm = g2d.getFontMetrics();
            final double priceX = 10 - (printable.getPricePerUom().intValue() > 999 ? 6 : 0);
            final double priceY = height-10;// + fm.getAscent();
            g2d.drawString(price, (int)priceX-5, (int)priceY);
            
            final String uom = printable.getUom();
            g2d.setFont(new Font(FONT, Font.PLAIN, 5));
            fm = g2d.getFontMetrics();
            final double uomX = width - fm.stringWidth(uom);
            g2d.drawString(uom, (int)uomX, (int)priceY);
            
            return Printable.PAGE_EXISTS;
        }
    }
	
	public static class WeightedPricePage implements Printable
	{
		private BarcodePrintable printable;
		private BigDecimal totalUnits;
		private BigDecimal totalPrice;
		
        public WeightedPricePage(final BarcodePrintable printable, final BigDecimal totalUnits, final BigDecimal totalPrice)
        {
			this.printable = printable;
			this.totalUnits = totalUnits;
			this.totalPrice = totalPrice;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
		{
            if (pageIndex >= 1)
                return Printable.NO_SUCH_PAGE;

            final Graphics2D g2d = (Graphics2D) graphics;
            // Be careful of clips...
            g2d.translate((int) pageFormat.getImageableX()+1, (int) pageFormat.getImageableY());

            final double width = pageFormat.getImageableWidth();
            final double height = pageFormat.getImageableHeight();

            // denumire
            writeNameToGc(g2d, printable, width);

            // numar bucati sau greutate
            final String totalUnitsText = totalUnits.toString();
            g2d.setFont(new Font(FONT, Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            final double priceX = width/2 - fm.stringWidth(totalUnitsText)/2;
            final double priceY = height/2;// + fm.getAscent();
            g2d.drawString(totalUnitsText, (int)priceX-5, (int)priceY);
            
            g2d.setFont(new Font(FONT, Font.PLAIN, 8));
            fm = g2d.getFontMetrics();
            final double uomX = width - fm.stringWidth(printable.getUom());
            g2d.drawString(printable.getUom(), (int)uomX, (int)priceY);
            
            // cod de bare
            writeBarcodeToGc(g2d, printable, (int)priceY+5);

            return Printable.PAGE_EXISTS;
        }
    }
	
	public static class KeyPage implements Printable
	{
		private int key;
		
        public KeyPage(final int key)
        {
			this.key = key;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
		{
            final Graphics2D g2d = (Graphics2D) graphics;
            // Be careful of clips...
            g2d.translate((int) pageFormat.getImageableX()+1, (int) pageFormat.getImageableY());

            final double width = pageFormat.getImageableWidth();
            final double height = pageFormat.getImageableHeight();

            // Tasta
            g2d.setFont(new Font(FONT, Font.PLAIN, 8));
            FontMetrics fm = g2d.getFontMetrics();
            final double keyX = width/2 - fm.stringWidth("Tasta")/2;
            g2d.drawString("Tasta", (int)keyX, fm.getAscent());

            // nr tastei
            g2d.setFont(new Font(FONT, Font.BOLD, 20));
            fm = g2d.getFontMetrics();
            final String keyString = String.valueOf(key);
            final double keyNumberX = width/2 - fm.stringWidth(keyString)/2;
            final double keyNumberY = height/3;
            g2d.drawString(keyString, (int)keyNumberX, (int)keyNumberY+fm.getAscent());
            
            return Printable.PAGE_EXISTS;
        }
    }
	
	public static class CustomLabelPage implements Printable
	{
		private BarcodePrintable printable;

		public CustomLabelPage(final BarcodePrintable printable)
		{
			this.printable = printable;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
		{
			final Graphics2D g2d = (Graphics2D) graphics;
			// Be careful of clips...
			g2d.translate((int) pageFormat.getImageableX()+1, (int) pageFormat.getImageableY());

			final double width = pageFormat.getImageableWidth();
			final double height = pageFormat.getImageableHeight();

			// denumire
			// in this case the name is the prefix
			writeNameToGc(g2d, printable, width);

			// barcode would then be the big text
			g2d.setFont(new Font(FONT, Font.BOLD, 16));
			FontMetrics fm = g2d.getFontMetrics();
			if (fm.stringWidth(printable.getBarcode()) > width)
				g2d.setFont(new Font(FONT, Font.BOLD, 14));
			fm = g2d.getFontMetrics();
			if (fm.stringWidth(printable.getBarcode()) > width)
				g2d.setFont(new Font(FONT, Font.BOLD, 12));
			
			final double textY = height-10;// + fm.getAscent();
			g2d.drawString(printable.getBarcode(), 0, (int)textY);

			return Printable.PAGE_EXISTS;
		}
	}
	
	private static void writeNameToGc(final Graphics2D g2d, final BarcodePrintable printable, final double imageableWidth)
	{
		if (isEmpty(printable.getName()))
			return;
		
		g2d.setFont(new Font(FONT, Font.PLAIN, 8));
        FontMetrics fm = g2d.getFontMetrics();
        if (fm.stringWidth(printable.getName()) > imageableWidth)
        {
        	g2d.setFont(new Font(FONT, Font.PLAIN, 6));
        	fm = g2d.getFontMetrics();
        	if (fm.stringWidth(printable.getName()) > imageableWidth)
        	{
        		final String firstRow = printable.getName().substring(0, Math.min(31, printable.getName().length()));
        		final String secondRow = printable.getName().substring(Math.min(31, printable.getName().length())).trim();
        		g2d.drawString(firstRow, 0, fm.getAscent());
        		g2d.drawString(secondRow, 0, fm.getAscent()*2+1);
        		return;
        	}
        }
        
        g2d.drawString(printable.getName(), 0, fm.getAscent());
	}
	
	public static void writeBarcodeToGc(final Graphics2D g2d, final BarcodePrintable printable, final int barcodeY)
	{
		if (isEmpty(printable.getBarcode()))
			return;
		
		AbstractBarcodeBean barcodeBean;
        
        if (isEAN13(printable.getBarcode()))
        	barcodeBean = new EAN13Bean();
        else
        	barcodeBean = new Code128Bean();

        barcodeBean.setModuleWidth(UnitConv.in2mm(2d / DPI)); //makes the narrow bar 
        barcodeBean.doQuietZone(false);
        barcodeBean.setFontSize(UnitConv.pt2mm(16));
        barcodeBean.setFontName(FONT);
        
    	final Rectangle2D rectangle = new Rectangle(10, barcodeY, BARCODE_WIDTH, BARCODE_HEIGHT);
    	
//    	BarcodeDimension dimension = barcodeBean.calcDimensions(product.getBarcode());
//        Rectangle2D barcodeDim = dimension.getBoundingRect();
        g2d.translate(rectangle.getX(), rectangle.getY());
//        g2d.scale(rectangle.getWidth() / barcodeDim.getWidth(), rectangle.getHeight() / barcodeDim.getHeight());
        final Java2DCanvasProvider canvasProvider = new Java2DCanvasProvider(g2d, 0);
        barcodeBean.generateBarcode(canvasProvider, printable.getBarcode());
	}
}
