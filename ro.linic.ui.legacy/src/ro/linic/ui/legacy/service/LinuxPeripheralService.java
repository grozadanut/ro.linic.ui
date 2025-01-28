package ro.linic.ui.legacy.service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

import org.eclipse.core.runtime.ILog;

import jssc.SerialPortException;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.legacy.service.components.BarcodePrintable;

final class LinuxPeripheralService extends PeripheralService
{
	private static final ILog log = UIUtils.logger(LinuxPeripheralService.class);
	
	private static final String PRINTER_PORT = "/dev/usb/lp0";
	private static final String PRINTER_LINE_END = "\r\n";
	
	private static StringBuilder prepareSB()
	{
		return new StringBuilder()
				.append("SIZE 48 mm, 25 mm").append(PRINTER_LINE_END)
				.append("DIRECTION 1,0").append(PRINTER_LINE_END)
				.append("REFERENCE 0,0").append(PRINTER_LINE_END)
				.append("OFFSET 0 mm").append(PRINTER_LINE_END)
				.append("SET PEEL OFF").append(PRINTER_LINE_END)
				.append("SET TEAR ON").append(PRINTER_LINE_END)
				.append("CLS").append(PRINTER_LINE_END);
	}
	
	@Override
	public synchronized void printKeys(final String keysToPrint) throws IOException, SerialPortException, InterruptedException
	{
		final StringBuilder sb = prepareSB();
		final String[] keyRange = keysToPrint.split(KEYS_RANGE_SPLIT);
        
        if (keyRange.length == 2)
        	throw new IllegalArgumentException("Multiple keys are not yet supported!");
        else if (keyRange.length == 1)
        	sb.append("TEXT 20,70,\"3\",0,1,1,\"Tasta\"").append(PRINTER_LINE_END)
        	.append("TEXT 130,50,\"0\",0,4,4,\""+keyRange[0]+"\"").append(PRINTER_LINE_END);
        else
        	throw new IllegalArgumentException("Key format not accepted: "+keysToPrint);
        
        sb.append("PRINT 1,1").append(PRINTER_LINE_END);
		log.info(sb.toString());
		final FileWriter writer = new FileWriter(PRINTER_PORT);
		writer.write(sb.toString());
		writer.close();
    }

	@Override
	public synchronized void printWeightedLabel(final BarcodePrintable product, final BigDecimal totalUnits, final BigDecimal totalPrice) throws IOException
	{
		final StringBuilder sb = prepareSB();
        
		sb.append("TEXT 10,10,\"2\",0,1,1,\""+product.getName()+"\"").append(PRINTER_LINE_END)
		.append("TEXT 100,60,\"0\",0,2,2,\""+totalUnits.toString()+"\"").append(PRINTER_LINE_END)
		.append("TEXT 300,90,\"2\",0,1,1,\""+product.getUom()+"\"").append(PRINTER_LINE_END)
		.append("BARCODE 100,120,\"EAN13\",40,2,0,2,2,\""+product.getBarcode()+"\"").append(PRINTER_LINE_END)
        .append("PRINT 1,1").append(PRINTER_LINE_END);
		
		log.info(sb.toString());
		final FileWriter writer = new FileWriter(PRINTER_PORT);
		writer.write(sb.toString());
		writer.close();
	}

	@Override
	public synchronized void printPriceLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		if (printable.getCantitate() <= 0)
			return;
		
		final StringBuilder sb = prepareSB();
        
		final String price = printable.getPricePerUom().setScale(2).toString();
		final String uom = "LEI/"+printable.getUom();
        
		int priceX = 150;
		
		if (printable.getPricePerUom().compareTo(new BigDecimal(999)) >= 1)
			priceX = 100;
		
		String barcode = "BARCODE 100,120,\"EAN13\",40,2,0,2,2,\""+printable.getBarcode()+"\"";
		
		if (!isEAN13(printable.getBarcode()))
			barcode = "TEXT 100,140,\"2\",0,1,1,\""+printable.getBarcode()+"\"";
		
		if (printable.getName().length() > 31)
		{
			final String firstRow = printable.getName().substring(0, 31);
			final String secondRow = printable.getName().substring(31).trim();
			sb.append("TEXT 10,10,\"2\",0,1,1,\""+firstRow+"\"").append(PRINTER_LINE_END)
			.append("TEXT 10,35,\"2\",0,1,1,\""+secondRow+"\"").append(PRINTER_LINE_END);
		}
		else
			sb.append("TEXT 10,10,\"2\",0,1,1,\""+printable.getName()+"\"").append(PRINTER_LINE_END);
		
		sb.append("TEXT "+priceX+",65,\"0\",0,2,2,\""+price+"\"").append(PRINTER_LINE_END)
		.append("TEXT 300,95,\"1\",0,1,1,\""+uom+"\"").append(PRINTER_LINE_END)
		.append(barcode).append(PRINTER_LINE_END)
        .append("PRINT 1,"+printable.getCantitate()).append(PRINTER_LINE_END);
		
		log.info(sb.toString());
		final FileWriter writer = new FileWriter(PRINTER_PORT);
		writer.write(sb.toString());
		writer.close();
    }
	
	@Override
	public synchronized void printBigPriceLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		if (printable.getCantitate() <= 0)
			return;
		
		final StringBuilder sb = prepareSB();
        
		final String price = printable.getPricePerUom().setScale(2).toString();
		int priceX = 40;
		
		if (printable.getPricePerUom().compareTo(new BigDecimal(999)) >= 1)
			priceX = 20;
        
		if (printable.getName().length() > 31)
		{
			final String firstRow = printable.getName().substring(0, 31);
			final String secondRow = printable.getName().substring(31);
			sb.append("TEXT 10,10,\"2\",0,1,1,\""+firstRow+"\"").append(PRINTER_LINE_END)
			.append("TEXT 10,35,\"2\",0,1,1,\""+secondRow+"\"").append(PRINTER_LINE_END);
		}
		else
			sb.append("TEXT 10,10,\"2\",0,1,1,\""+printable.getName()+"\"").append(PRINTER_LINE_END);
		
		sb.append("TEXT "+priceX+",75,\"0\",0,4,4,\""+price+"\"").append(PRINTER_LINE_END)
		.append("TEXT 350,145,\"1\",0,1,1,\""+printable.getUom()+"\"").append(PRINTER_LINE_END)
		.append("PRINT 1,"+printable.getCantitate()).append(PRINTER_LINE_END);
		
		log.info(sb.toString());
		final FileWriter writer = new FileWriter(PRINTER_PORT);
		writer.write(sb.toString());
		writer.close();
    }
	
	@Override
	public void printCustomLabel(final BarcodePrintable printable, final String printerName) throws IOException
	{
		log.error("printCustomLabel NOT IMPLEMENTED FOR LINUX!");
	}
	
	@Override
	public boolean isPrinterConnected(final String printerName)
	{
		return false;
	}
}
