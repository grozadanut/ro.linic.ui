package ro.linic.ui.legacy.service;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ServerConstants.L1_NAME;
import static ro.colibri.util.ServerConstants.L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
import static ro.linic.ui.legacy.session.UIUtils.isWindows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;

import jssc.SerialPortException;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.util.ServerConstants.JMSMessageType;
import ro.colibri.wrappers.ThreeEntityWrapper;
import ro.colibri.wrappers.TwoEntityWrapper;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;

public abstract class PeripheralService
{
	private static PeripheralService instance;
	
	public static final String BARCODE_PRINTER_KEY = "barcode_printer";
	public static final String BARCODE_PRINTER_DEFAULT = "HPRT LPQ58";
	
	public static final char EOFB = '\r';
	public static final int BARCODE_WIDTH = 80;
	public static final int BARCODE_HEIGHT = 20;
	public static final String KEYS_RANGE_SPLIT = ":";
	
	protected Logger log;
	
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
			if (!simplePrintables.isEmpty() || !promoPrintables.isEmpty() || !cantPromoPrintables.isEmpty() ||
					!simpleMiniPrintables.isEmpty() || !cantPromoMiniPrintables.isEmpty())
				JasperReportManager.instance(bundle, log).printEticheteA4(bundle, simplePrintables,
						promoPrintables, cantPromoPrintables,
						simpleMiniPrintables, cantPromoMiniPrintables);
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
			MessagingService.instance().sendMsg(remoteJndi, JMSMessageType.GENERAL, ImmutableMap.of(), printablesToSend, log);
			return;
		}
		
		if (sendFurther && !instance(log).isPrinterConnected(printerName))
		{
			final String remoteJndi = ClientSession.instance().getLoggedUser().getSelectedGestiune().isMatch(L1_NAME) ? 
					L1_PRINT_BARCODE_TOPIC_REMOTE_JNDI : L2_PRINT_BARCODE_TOPIC_REMOTE_JNDI;
			MessagingService.instance().sendMsg(remoteJndi, JMSMessageType.GENERAL, ImmutableMap.of(), printablesToSend, log);
			return;
		}
		
		printables.stream()
		.filter(printable -> printable.getCantitate() > 0)
		.filter(BarcodePrintable::isNormalLabel)
		.forEach(printable -> {
			try
			{
				instance(log).printPriceLabel(printable, printerName);
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
				instance(log).printBigPriceLabel(printable, printerName);
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
				instance(log).printCustomLabel(printable, printerName);
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
	
	public static PeripheralService instance(final Logger log)
	{
		if (instance == null)
		{
			if (isWindows())
				instance = new WindowsPeripheralService(log);
			else
				instance = new LinuxPeripheralService(log);
		}
		
		return instance;
	}
	
	protected PeripheralService(final Logger log)
	{
		this.log = log;
	}
	
	public abstract void printWeightedLabel(BarcodePrintable printable, BigDecimal totalUnits, BigDecimal totalPrice) throws IOException;
	public abstract void printPriceLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printBigPriceLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printCustomLabel(BarcodePrintable printable, final String printerName) throws IOException;
	public abstract void printKeys(String keysToPrint) throws IOException, InterruptedException, SerialPortException;
	public abstract boolean isPrinterConnected(String printerName);
}
