package ro.linic.ui.legacy.service;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ServerConstants.L1_NAME;
import static ro.colibri.util.ServerConstants.L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.linic.ui.legacy.session.UIUtils.isWindows;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;

import jssc.SerialPortException;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.PresentationUtils;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.colibri.wrappers.ThreeEntityWrapper;
import ro.colibri.wrappers.TwoEntityWrapper;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.legacy.parts.Messages;
import ro.linic.ui.legacy.preferences.PreferenceKey;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.BarcodePrintable.LabelType;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;

public abstract class PeripheralService
{
	private static PeripheralService instance;
	
	private static final ILog log = UIUtils.logger(PeripheralService.class);
	private static final String RESULT_SUFFIX = "_result"; //$NON-NLS-1$
	
	public static final String BARCODE_PRINTER_KEY = "barcode_printer";
	public static final String BARCODE_PRINTER_DEFAULT = "HPRT LPQ58";
	
	public static final char EOFB = '\r';
	public static final int BARCODE_WIDTH = 80;
	public static final int BARCODE_HEIGHT = 20;
	public static final String KEYS_RANGE_SPLIT = ":";
	
	public static boolean isEAN13(final String barcode)
	{
		return barcode.length() == 13 && barcode.matches("[0-9]+");
	}
	
	public static void printPrintables(final Collection<BarcodePrintable> printables, final String printerName,
			final Logger log, final Bundle bundle, final boolean sendFurther, final Optional<Gestiune> gestiune)
	{
		// A4 labels we don't send further because the printer is selected from JasperViewer by user
		try
		{
			final ImmutableList<TwoEntityWrapper<BarcodePrintable>> simplePrintables =
					convertToA4SimpleLabel(printables, gestiune);
			final ImmutableList<TwoEntityWrapper<BarcodePrintable>> promoPrintables =
					convertToA4PromoLabel(printables, gestiune);
			final ImmutableList<BarcodePrintable> cantPromoPrintables =
					convertToA4CantPromoLabel(printables, gestiune);
			final ImmutableList<ThreeEntityWrapper<BarcodePrintable>> simpleMiniPrintables =
					convertToA4SimpleMiniLabel(printables, gestiune);
			final ImmutableList<BarcodePrintable> cantPromoMiniPrintables =
					convertToA4CantPromoMiniLabel(printables, gestiune);
			final ImmutableList<BarcodePrintable> brotherPrintables =
					convertToBrotherLabel(printables, gestiune);
			
			if (!simplePrintables.isEmpty() || !promoPrintables.isEmpty() || !cantPromoPrintables.isEmpty() ||
					!simpleMiniPrintables.isEmpty() || !cantPromoMiniPrintables.isEmpty())
				JasperReportManager.instance(bundle, log).printEticheteA4(bundle, simplePrintables,
						promoPrintables, cantPromoPrintables,
						simpleMiniPrintables, cantPromoMiniPrintables);
			
			if (!brotherPrintables.isEmpty())
				printToBrother(brotherPrintables, gestiune);
		}
		catch (IOException | JRException e1)
		{
			log.error(e1);
		}
		
		final ImmutableList<BarcodePrintable> printablesToSend = printables.stream().filter(BarcodePrintable::hasSomethingToPrintNotA4).collect(toImmutableList());
		if (sendFurther && gestiune.isPresent() && !gestiune.get().equals(ClientSession.instance().getLoggedUser().getSelectedGestiune()))
		{
			final String remoteJndi = gestiune.get().isMatch(L1_NAME) ? 
					L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI : L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
			MessagingService.instance().sendMsg(remoteJndi, JMSMessageType.GENERAL, ImmutableMap.of(), printablesToSend);
			return;
		}
		
		if (sendFurther && !instance().isPrinterConnected(printerName))
		{
			final String remoteJndi = ClientSession.instance().getLoggedUser().getSelectedGestiune().isMatch(L1_NAME) ? 
					L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI : L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
			MessagingService.instance().sendMsg(remoteJndi, JMSMessageType.GENERAL, ImmutableMap.of(), printablesToSend);
			return;
		}
		
		printables.stream()
		.filter(printable -> printable.getCantitate() > 0)
		.filter(BarcodePrintable::isNormalLabel)
		.forEach(printable -> {
			try
			{
				instance().printPriceLabel(printable, printerName);
			}
			catch (final IOException e)
			{
				log.error(e);
			}
		});
		
		printables.stream()
		.filter(printable -> printable.getCantitate() > 0)
		.filter(BarcodePrintable::isBigLabel)
		.forEach(printable -> {
			try
			{
				instance().printBigPriceLabel(printable, printerName);
			}
			catch (final IOException e)
			{
				log.error(e);
			}
		});
		
		printables.stream()
		.filter(printable -> printable.getCantitate() > 0)
		.filter(BarcodePrintable::isCustomLabel)
		.forEach(printable -> {
			try
			{
				instance().printCustomLabel(printable, printerName);
			}
			catch (final IOException e)
			{
				log.error(e);
			}
		});
	}
	
	public static ImmutableList<TwoEntityWrapper<BarcodePrintable>> convertToA4SimpleLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> a4Printables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.isNormalA4Label(gestiune))
				.flatMap(bp -> 
				{
					final Stream.Builder<BarcodePrintable> builder = Stream.builder();
					for (int i=0; i < bp.getCantitate(); i++)
						builder.add(bp);
					return builder.build();
				})
				.collect(toImmutableList());
		final Builder<TwoEntityWrapper<BarcodePrintable>> b = ImmutableList.<TwoEntityWrapper<BarcodePrintable>>builder();
		final UnmodifiableIterator<BarcodePrintable> it = a4Printables.iterator();
		while (it.hasNext())
		{
			final BarcodePrintable printable1 = it.next();
			final BarcodePrintable printable2 = it.hasNext() ? it.next() : null;
			b.add(new TwoEntityWrapper<BarcodePrintable>(printable1, printable2));
		}
		return b.build();
	}
	
	public static ImmutableList<TwoEntityWrapper<BarcodePrintable>> convertToA4PromoLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> a4Printables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.isPromoA4Label(gestiune))
				.map(bp -> bp.fillPromoFields(gestiune))
				.flatMap(bp -> 
				{
					final Stream.Builder<BarcodePrintable> builder = Stream.builder();
					for (int i=0; i < bp.getCantitate(); i++)
						builder.add(bp);
					return builder.build();
				})
				.collect(toImmutableList());
		final Builder<TwoEntityWrapper<BarcodePrintable>> b = ImmutableList.<TwoEntityWrapper<BarcodePrintable>>builder();
		final UnmodifiableIterator<BarcodePrintable> it = a4Printables.iterator();
		while (it.hasNext())
		{
			final BarcodePrintable printable1 = it.next();
			final BarcodePrintable printable2 = it.hasNext() ? it.next() : null;
			b.add(new TwoEntityWrapper<BarcodePrintable>(printable1, printable2));
		}
		return b.build();
	}
	
	public static ImmutableList<BarcodePrintable> convertToA4CantPromoLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> a4Printables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.isCantPromoA4Label(gestiune))
				.map(bp -> bp.fillPromoFields(gestiune))
				.flatMap(bp -> 
				{
					final Stream.Builder<BarcodePrintable> builder = Stream.builder();
					for (int i=0; i < bp.getCantitate(); i++)
						builder.add(bp);
					return builder.build();
				})
				.collect(toImmutableList());
		return a4Printables;
	}
	
	public static ImmutableList<ThreeEntityWrapper<BarcodePrintable>> convertToA4SimpleMiniLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> a4Printables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.isNormalA4MiniLabel(gestiune))
				.flatMap(bp -> 
				{
					final Stream.Builder<BarcodePrintable> builder = Stream.builder();
					for (int i=0; i < bp.getCantitate(); i++)
						builder.add(bp);
					return builder.build();
				})
				.collect(toImmutableList());
		final Builder<ThreeEntityWrapper<BarcodePrintable>> b = ImmutableList.<ThreeEntityWrapper<BarcodePrintable>>builder();
		final UnmodifiableIterator<BarcodePrintable> it = a4Printables.iterator();
		while (it.hasNext())
		{
			final BarcodePrintable printable1 = it.next();
			final BarcodePrintable printable2 = it.hasNext() ? it.next() : null;
			final BarcodePrintable printable3 = it.hasNext() ? it.next() : null;
			b.add(new ThreeEntityWrapper<BarcodePrintable>(printable1, printable2, printable3));
		}
		return b.build();
	}
	
	public static ImmutableList<BarcodePrintable> convertToA4CantPromoMiniLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> a4Printables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.isCantPromoA4MiniLabel(gestiune))
				.map(bp -> bp.fillPromoFields(gestiune))
				.flatMap(bp -> 
				{
					final Stream.Builder<BarcodePrintable> builder = Stream.builder();
					for (int i=0; i < bp.getCantitate(); i++)
						builder.add(bp);
					return builder.build();
				})
				.collect(toImmutableList());
		return a4Printables;
	}
	
	public static ImmutableList<BarcodePrintable> convertToBrotherLabel(
			final Collection<BarcodePrintable> printables, final Optional<Gestiune> gestiune)
	{
		final ImmutableList<BarcodePrintable> brotherPrintables = printables.stream()
				.filter(printable -> printable.getCantitate() > 0)
				.filter(bp -> bp.getLabelType().equals(LabelType.BROTHER))
				.map(bp -> bp.fillPromoFields(gestiune))
				.collect(toImmutableList());
		return brotherPrintables;
	}
	
	private static void printToBrother(final ImmutableList<BarcodePrintable> brotherPrintables, final Optional<Gestiune> gestiune) {
		final ImmutableList<BarcodePrintable> simpleLabels = brotherPrintables.stream()
				.filter(bp -> !bp.isPromoLabel(gestiune) && !bp.isSingleCantPromoLabel(gestiune) && !bp.isDoubleCantPromoLabel(gestiune))
				.collect(toImmutableList());
		
		final ImmutableList<BarcodePrintable> singlePromoLabels = brotherPrintables.stream()
				.filter(bp -> bp.isSingleCantPromoLabel(gestiune))
				.collect(toImmutableList());
		
		final ImmutableList<BarcodePrintable> doublePromoLabels = brotherPrintables.stream()
				.filter(bp -> bp.isDoubleCantPromoLabel(gestiune))
				.collect(toImmutableList());
		
		sendToBrother(buildBrotherCommands("simple.lbx", simpleLabels));
		sendToBrother(buildBrotherCommands("single.lbx", singlePromoLabels));
		sendToBrother(buildBrotherCommands("double.lbx", doublePromoLabels));
	}
	
	private static StringBuilder buildBrotherCommands(final String template,
			final ImmutableList<BarcodePrintable> labels) {
		if (labels.isEmpty())
			return null;
		
		/**
		 * File format:
		 * - templateName
		 * - label 1
		 * - label 2
		 * - ...
		 * - label x
		 * 
		 * templateName - eg: single.lbx
		 * label - quantity|objBarcode|objName|objUom|objPrice|objPromoQuant1|objPromoPrice1|objPromoPerc1|objPromoQuant2|objPromoPrice2|objPromoPerc2
		 */
		final String sep = "|";
		final StringBuilder commands = new StringBuilder();
		commands.append(template).append(PresentationUtils.NEWLINE);
		
		for (final BarcodePrintable bp : labels) {
			commands.append(bp.getCantitate())
			.append(sep).append(bp.getBarcode())
			.append(sep).append(bp.getName())
			.append(sep).append("RON/"+bp.getUom())
			.append(sep).append(PresentationUtils.displayBigDecimal(bp.getPricePerUom()))
			.append(sep).append(MessageFormat.format("De la {0} {1}", PresentationUtils.displayBigDecimal(bp.getPromoCantitate1()), bp.getUom()))
			.append(sep).append(PresentationUtils.displayBigDecimal(bp.getPromoPrice1()))
			.append(sep).append("-"+PresentationUtils.displayPercentage(calculateDiscountPercent(bp.getPricePerUom(), bp.getPromoPrice1())))
			.append(sep).append(MessageFormat.format("De la {0} {1}", PresentationUtils.displayBigDecimal(bp.getPromoCantitate2()), bp.getUom()))
			.append(sep).append(PresentationUtils.displayBigDecimal(bp.getPromoPrice2()))
			.append(sep).append("-"+PresentationUtils.displayPercentage(calculateDiscountPercent(bp.getPricePerUom(), bp.getPromoPrice2())))
			.append(PresentationUtils.NEWLINE);
		}
		
		return commands;
	}
	
	/**
	 * Disc Percentage = ((price - reducedPrice) / price)
	 */
	private static BigDecimal calculateDiscountPercent(final BigDecimal price, final BigDecimal reducedPrice)
	{
		if (price == null || NumberUtils.equal(price, BigDecimal.ZERO))
			return BigDecimal.ZERO;
		
		if (reducedPrice == null || NumberUtils.equal(reducedPrice, BigDecimal.ZERO))
			return BigDecimal.ZERO;
		
		return price.subtract(reducedPrice)
			.divide(price, 2, RoundingMode.HALF_EVEN);
	}

	private static Optional<Path> sendToBrother(final StringBuilder commands)
	{
		try
		{
			if (commands == null || commands.isEmpty())
				return Optional.empty();
			
			final Bundle bundle = FrameworkUtil.getBundle(PeripheralService.class);
			final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
			
			final String folderPath = prefs.get(PreferenceKey.BROTHER_PRINT_FOLDER, PreferenceKey.BROTHER_PRINT_FOLDER_DEF);
			int i = 0;
			String filename = "print_"+LocalDate.now().toString()+"_"+i+".brother"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			while (Files.exists(Paths.get(folderPath, filename)))
				filename = "print_"+LocalDate.now().toString()+"_"+ ++i+".brother"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			Files.write(Paths.get(folderPath, filename), commands.toString().getBytes());
			return Optional.of(Paths.get(folderPath, filename+RESULT_SUFFIX));
		}
		catch (final IOException e)
		{
			log.error("Error writting brother commands", e);
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.Error, e.getMessage()));
			return Optional.empty();
		}
	}
	
	public static PeripheralService instance()
	{
		if (instance == null)
		{
			if (isWindows())
				instance = new WindowsPeripheralService();
			else
				instance = new LinuxPeripheralService();
		}
		
		return instance;
	}
	
	public abstract void printWeightedLabel(BarcodePrintable printable, BigDecimal totalUnits, BigDecimal totalPrice) throws IOException;
	public abstract void printPriceLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printBigPriceLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printCustomLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printKeys(String keysToPrint) throws IOException, InterruptedException, SerialPortException;
	public abstract boolean isPrinterConnected(String printerName);
}
