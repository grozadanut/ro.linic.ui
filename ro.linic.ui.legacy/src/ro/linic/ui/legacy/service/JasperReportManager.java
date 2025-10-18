package ro.linic.ui.legacy.service;

import static ro.colibri.util.ListUtils.toHashSet;
import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.displayLocalDateTime;
import static ro.colibri.util.NumberUtils.equal;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.BR_SEPARATOR;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.PresentationUtils.toWords;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.view.JasperViewer;
import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.PontajZilnic.PontajDayType;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.colibri.wrappers.PontajLine;
import ro.colibri.wrappers.RulajPartener;
import ro.colibri.wrappers.ThreeEntityWrapper;
import ro.colibri.wrappers.TwoEntityWrapper;
import ro.linic.ui.legacy.anaf.ReceivedMessage;
import ro.linic.ui.legacy.dialogs.SendEmailDialog;
import ro.linic.ui.legacy.jasper.datasource.AccDocDatasource;
import ro.linic.ui.legacy.jasper.datasource.OfertaCuDiscountDatasource;
import ro.linic.ui.legacy.jasper.datasource.OfertaDatasource;
import ro.linic.ui.legacy.jasper.datasource.ProductsDatasource;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.JasperChartSerie;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;

public class JasperReportManager
{
	public static final int PAGE_A4_WIDTH = 595;
	public static final int PAGE_A4_HEIGHT = 842;
	
	private static final String CHITANTA_DESCRIPTION = "Am incasat de la {0}, cod fiscal: {1}, avand adresa {2}, suma de {3} RON adica {4}, reprezentand {5}.";
	
	private static JasperReportManager instance;
	
	private Logger log;
	
	public static JasperReportManager instance(final Bundle bundle, final Logger log) throws IOException
	{
		if (instance == null)
			instance = new JasperReportManager(bundle, log);
		
		return instance;
	}
	
	public static ImmutableList<AccountingDocument> comasarePtPrint(final List<AccountingDocument> docs, final boolean reload)
	{
		return docs.stream()
		.filter(AccountingDocument::isPrintable)
		.collect(Collectors.groupingBy(AccountingDocument::getPartner))
		.entrySet().stream()
		.flatMap(entry ->
		{
			final ImmutableList<AccountingDocument> nonOfficialDocs = AccountingDocument.extractNonOfficialDocs_Stream(entry.getValue())
					.map(doc -> 
					{
						AccountingDocument reloadedDoc = doc;
						if (doc.getId() != null && reload)
						{
							reloadedDoc = BusinessDelegate.reloadDoc(doc);
							if (reloadedDoc == null)
								throw new IllegalArgumentException("Documentul cu id "+doc.getId()+" nu a fost gasit in baza de date!");

							final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(reloadedDoc);
							reloadedDoc.setOperatiuni(new HashSet<Operatiune>(operatiuni));
						}
						return reloadedDoc;
					})
					.collect(toImmutableList());
			
			final AccountingDocument comasat = new AccountingDocument();
			comasat.setPartner(entry.getKey());
			comasat.setGestiune(ClientSession.instance().getLoggedUser().getSelectedGestiune());
			comasat.setDoc(nonOfficialDocs.stream()
					.map(AccountingDocument::getDoc)
					.distinct()
					.collect(Collectors.joining(LIST_SEPARATOR)));
			comasat.setNrDoc(nonOfficialDocs.stream()
					.map(AccountingDocument::getNrDoc)
					.distinct()
					.collect(Collectors.joining(LIST_SEPARATOR)));
			comasat.setOperatiuni(nonOfficialDocs.stream()
					.flatMap(AccountingDocument::getOperatiuni_Stream)
					.collect(toImmutableSet()));
			
			final Stream<AccountingDocument> comasatStream = nonOfficialDocs.isEmpty() ? Stream.of() : Stream.of(comasat);
			return Stream.concat(comasatStream, AccountingDocument.extractOfficialDocs_Stream(entry.getValue()));
		})
		.collect(toImmutableList());
	}
	
	private static String buildDetails(final InvocationResult firmaDetails)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("CIF: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY)).append(NEWLINE);
		sb.append("Reg Com: ").append(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY)).append(NEWLINE);
		sb.append("Cap soc: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CAP_SOCIAL_KEY)).append(NEWLINE);
		sb.append("Adresa: ").append(firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY)).append(NEWLINE);
		sb.append("Banca: ").append(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_KEY)).append(NEWLINE);
		sb.append("Cont: ").append(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY)).append(NEWLINE);
		sb.append("Banca: ").append(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_KEY)).append(NEWLINE);
		sb.append("Cont: ").append(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_ACC_KEY)).append(NEWLINE);
		sb.append("Tel: ").append(firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY)).append(NEWLINE);
		sb.append("Email: ").append(firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY)).append(NEWLINE);
		sb.append("Website: ").append(firmaDetails.extraString(PersistedProp.FIRMA_WEBSITE_KEY));
		return sb.toString();
	}
	
	private static String buildOneRowDetails(final InvocationResult firmaDetails)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(firmaDetails.extraString(PersistedProp.FIRMA_BILLING_ADDRESS_KEY)).append(LIST_SEPARATOR);
		sb.append("CIF: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY)).append(LIST_SEPARATOR);
		sb.append("Reg. Com.: ").append(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY));
		return sb.toString();
	}
	
	private static String buildShortDetails(final InvocationResult firmaDetails)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("CIF: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY)).append(NEWLINE);
		sb.append("Reg Com: ").append(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY)).append(NEWLINE);
		sb.append("Cap soc: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CAP_SOCIAL_KEY)).append(NEWLINE);
		sb.append("Adresa: ").append(firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY)).append(NEWLINE);
		sb.append("Banca: ").append(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_KEY)).append(NEWLINE);
		sb.append("Cont: ").append(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY)).append(NEWLINE);
		sb.append("Tel: ").append(firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY)).append(NEWLINE);
		sb.append("Email: ").append(firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY)).append(NEWLINE);
		return sb.toString();
	}
	
	private static String buildRequiredDetails(final InvocationResult firmaDetails)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("CIF: ").append(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY)).append(NEWLINE);
		sb.append("Reg Com: ").append(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY)).append(NEWLINE);
		sb.append("Adresa: ").append(firmaDetails.extraString(PersistedProp.FIRMA_BILLING_ADDRESS_KEY)).append(NEWLINE);
		return sb.toString();
	}
	
	private static String buildFirmaDetails(final Partner partner)
	{
		if (partner == null)
			return EMPTY_STRING;
		
		final StringBuilder sb = new StringBuilder();
		sb.append("CIF: ").append(safeString(partner.getCodFiscal())).append(NEWLINE);
		sb.append("Reg Com: ").append(safeString(partner.getRegCom())).append(NEWLINE);
		sb.append("Adresa: ").append(safeString(partner.getAddress(), Address::displayName)).append(NEWLINE);
		sb.append("Cont: ").append(safeString(partner.getIban())).append(NEWLINE);
		sb.append("Tel: ").append(safeString(partner.getPhone()));
		return sb.toString();
	}
	
	private static String buildDetails(final AccountingDocument nonOfficialDoc)
	{
		final Optional<Partner> partner = Optional.ofNullable(nonOfficialDoc.getPartner());
		final StringBuilder sb = new StringBuilder();
		sb.append("Adresa: ").append(nonOfficialDoc.address()
				.orElseGet(() -> partner.flatMap(Partner::deliveryAddress)
						.orElse(partner.map(Partner::getAddress).map(Address::displayName).orElse(EMPTY_STRING)))).append(NEWLINE);
		sb.append("Tel: ").append(nonOfficialDoc.phone()
				.orElseGet(() -> partner.map(Partner::getPhone).orElse(EMPTY_STRING))).append(NEWLINE);
		if (!isEmpty(partner.map(Partner::getEmail).orElse(EMPTY_STRING)))
			sb.append("Email: ").append(partner.map(Partner::getEmail).orElse(EMPTY_STRING)).append(NEWLINE);
		sb.append(safeString(nonOfficialDoc.getIndicatii()));
		return sb.toString();
	}
	
	private static String buildDetails(final Delegat delegat, final LocalDateTime data)
	{
		if (delegat == null)
			return EMPTY_STRING;
		
		final String ciCnp = isEmpty(delegat.getSerieCI()) ? safeString(delegat.getCnp()) : delegat.getSerieCI();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("Delegat: ").append(safeString(delegat.getName())).append(NEWLINE);
		sb.append("CI/CNP: ").append(ciCnp).append(" elib ").append(safeString(delegat.getElib())).append(NEWLINE);
		sb.append("Auto: ").append(safeString(delegat.getAuto())).append(NEWLINE);
		sb.append("Data: ").append(displayLocalDateTime(data, "dd/MM/uuuu"));
		return sb.toString();
	}
	
	private JasperReportManager(final Bundle bundle, final Logger log) throws IOException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/logo_256x256.png"));
		final URL fileUrl = FileLocator.toFileURL(url);
		new File(fileUrl.getFile());
		this.log = log;
	}
	
	public void printDocs(final Bundle bundle, final ImmutableList<AccountingDocument> docs, final boolean reload)
			throws IOException, JRException
	{
		if (docs.isEmpty())
			return;
		
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("Documente");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		// 1. Print dispozitie plata/incasare
		final ImmutableList<AccountingDocument> disp = docs.stream()
				.filter(AccountingDocument::isPrintable)
				.filter(AccountingDocument::isDispPlataIncasare)
				.collect(toImmutableList());
		if (!disp.isEmpty())
		{
			final JasperPrint dispJasper = createDispPlataIncasare(bundle, disp);
			dispJasper.getPages().stream().forEach(jasperReport::addPage);
		}
		
		// 2. Print other docs
		docs.stream()
		.filter(AccountingDocument::isPrintable)
		.filter(accDoc -> !accDoc.isDispPlataIncasare())
		.map(doc ->
		{
			try
			{
				if (TipDoc.CUMPARARE.equals(doc.getTipDoc()))
					return createReceptie(bundle, doc);
				else if (doc.isOfficialVanzariDoc())
					return createFactura(bundle, doc);
				else if (AccountingDocument.CHITANTA_NAME.equalsIgnoreCase(doc.getDoc()))
					return createChitanta(bundle, doc);
				else
					return createNonOfficialDoc(bundle, doc, reload);
			}
			catch (final Exception e)
			{
				log.error(e);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare", e.toString());
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}
	
	public void printPlanuri(final Bundle bundle, final ImmutableList<AccountingDocument> docs) throws IOException, JRException
	{
		if (docs.isEmpty())
			return;
		
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("Documente");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		docs.stream()
		.filter(AccountingDocument::isPrintable)
		.map(doc ->
		{
			try
			{
				doc.setOperatiuni(doc.getOperatiuni_Stream()
						.filter(accDoc -> globalIsMatch(accDoc.getCategorie(), Product.DISCOUNT_CATEGORY, TextFilterMethod.NOT_EQUALS))
						.collect(toHashSet()));
				
				return createPlanLivrare(bundle, doc);
			}
			catch (final Exception e)
			{
				log.error(e);
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}


	public void printOfertaPret(final Bundle bundle, final AccountingDocument bonCasa) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/oferta.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		
		final Optional<BigDecimal> totalDiscount = bonCasa.getOperatiuni().stream()
				.filter(op -> Objects.equals(op.getCategorie(), Product.DISCOUNT_CATEGORY))
				.filter(op -> NumberUtils.smallerThan(op.getCantitate(), BigDecimal.ZERO))
				.map(op -> NumberUtils.add(op.getValoareVanzareFaraTVA(), op.getValoareVanzareTVA()).abs())
				.reduce(BigDecimal::add);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(bonCasa));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new OfertaDatasource(bonCasa));
		parameters.put(PersistedProp.FIRMA_NAME_KEY, firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put(PersistedProp.FIRMA_CUI_KEY, firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY));
		parameters.put(PersistedProp.FIRMA_ADDRESS_KEY, firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY));
		parameters.put(PersistedProp.FIRMA_PHONE_KEY, firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY));
		parameters.put(PersistedProp.FIRMA_EMAIL_KEY, firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		parameters.put("totalDiscount", totalDiscount.orElse(null));
		
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource), false);
	}
	
	public void printOfertaCuDiscount(final Bundle bundle, final ImmutableList<Product> products, final BigDecimal discountPercentage,
			final BigDecimal cappedAdaos, final boolean withImage) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/oferta_discount.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(discountPercentage));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new OfertaCuDiscountDatasource(withImage, products, discountPercentage, cappedAdaos, bundle, log));
		parameters.put(PersistedProp.FIRMA_NAME_KEY, firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put(PersistedProp.FIRMA_CUI_KEY, firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY));
		parameters.put(PersistedProp.FIRMA_ADDRESS_KEY, firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY));
		parameters.put(PersistedProp.FIRMA_PHONE_KEY, firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY));
		parameters.put(PersistedProp.FIRMA_EMAIL_KEY, firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource), false);
	}
	
	public void printNonOfficialDoc(final Bundle bundle, final AccountingDocument doc, final boolean reload)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createNonOfficialDoc(bundle, doc, reload), false);
	}
	
	public void printFactura(final Bundle bundle, final AccountingDocument factura, AccountingDocument chitanta)
			throws IOException, JRException
	{
		// sanity checks
		if (chitanta != null && !AccountingDocument.CHITANTA_NAME.equalsIgnoreCase(chitanta.getDoc()))
			chitanta = null;
		
		final JasperPrint jasperReport = createFactura(bundle, factura);
		
		if (chitanta != null)
		{
			final JasperPrint chitantaReport = createChitanta(bundle, chitanta);
			jasperReport.getPages().addAll(chitantaReport.getPages());
		}
		
		JasperViewer.viewReport(jasperReport, false);
	}
	
	/**
	 * @param bundle
	 * @param factura @nullable
	 * @param chitanta @nullable
	 * @throws IOException
	 * @throws JRException
	 */
	public void printFactura_ClientDuplicate(final Bundle bundle, final AccountingDocument factura, AccountingDocument chitanta)
			throws IOException, JRException
	{
		// sanity checks
		if (chitanta != null && !AccountingDocument.CHITANTA_NAME.equalsIgnoreCase(chitanta.getDoc()))
			chitanta = null;

		final AccountingDocument facturaReload = loadFacturaWithoutConsumabile(factura);
		JasperPrint clientReport = null;
		
		if (facturaReload != null)
			clientReport = createFacturaPage(bundle, facturaReload, "1");
		
		if (chitanta != null)
		{
			if (facturaReload == null)
				clientReport = createChitantaPage(bundle, chitanta, "1");
			else
				clientReport.getPages().addAll(createChitantaPage(bundle, chitanta, "1").getPages());
		}
		
		// trimite email
		if (clientReport != null)
		{
			final String emailSignature = BusinessDelegate.persistedProp(PersistedProp.EMAIL_SIGNATURE_KEY)
					.getValueOr(PersistedProp.EMAIL_SIGNATURE_DEFAULT);
			
			final String partnerEmail = facturaReload != null ? safeString(facturaReload.getPartner(), Partner::getEmail) :
				safeString(chitanta, AccountingDocument::getPartner, Partner::getEmail);
			
			final StringBuilder messageSB = new StringBuilder();
			final String partnerName = facturaReload != null ? safeString(facturaReload.getPartner(), Partner::getName) :
				safeString(chitanta, AccountingDocument::getPartner, Partner::getName);

			messageSB.append("Catre ").append(partnerName).append(BR_SEPARATOR).append(BR_SEPARATOR)
			.append("Buna ziua,").append(BR_SEPARATOR).append(BR_SEPARATOR)
			.append("Atasat acestui email regasiti urmatoarele documente:").append(BR_SEPARATOR);

			if (facturaReload != null)
				messageSB.append(MessageFormat.format("{0} nr {1}, valoare totala {2} RON", facturaReload.getDoc(),
						facturaReload.getNrDoc(), displayBigDecimal(facturaReload.getTotal()))).append(BR_SEPARATOR);

			if (chitanta != null)
				messageSB.append(MessageFormat.format("{0} nr {1}, valoare totala {2} RON", chitanta.getDoc(),
						chitanta.getNrDoc(), displayBigDecimal(chitanta.getTotal()))).append(BR_SEPARATOR);

			messageSB.append(BR_SEPARATOR).append(emailSignature);
			
			final StringBuilder attachementsDescriptionSB = new StringBuilder();
			if (facturaReload != null)
				attachementsDescriptionSB.append(MessageFormat.format("{0} nr {1}, valoare totala {2}", facturaReload.getDoc(),
						facturaReload.getNrDoc(), displayBigDecimal(facturaReload.getTotal()))).append(NEWLINE);
			if (chitanta != null)
				attachementsDescriptionSB.append(MessageFormat.format("{0} nr {1}, valoare totala {2}", chitanta.getDoc(),
						chitanta.getNrDoc(), displayBigDecimal(chitanta.getTotal())));

			SendEmailDialog.open(Display.getCurrent().getActiveShell(), log, partnerEmail, "Documente "+partnerName,
					messageSB.toString(), JasperExportManager.exportReportToPdf(clientReport), attachementsDescriptionSB.toString());
		}
	}
	
	public void printReceptie(final Bundle bundle, final AccountingDocument doc) throws IOException, JRException
	{
		JasperViewer.viewReport(createReceptie(bundle, doc), false);
	}
	
	public void printCatalogProduse(final Bundle bundle, final Gestiune gestiune, final ImmutableList<Product> products) throws IOException, JRException
	{
		JasperViewer.viewReport(createCatalogProduse(bundle, gestiune, products), false);
	}
	
	public void printCatalogAnaf(final Bundle bundle, final Gestiune gestiune, final ImmutableList<Product> products, final BigDecimal soldFinal)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createCatalogAnaf(bundle, gestiune, products, soldFinal), false);
	}
	
	public void printFisaParteneri_Centralizat(final Bundle bundle, final LocalDate from, final LocalDate to,
			final ImmutableList<RulajPartener> rulajeParteneri) throws IOException, JRException
	{
		JasperViewer.viewReport(createFisaParteneri_Centralizat(bundle, from, to, rulajeParteneri), false);
	}
	
	public void printRegIncasariPlati_Inlantuit(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final BigDecimal soldInitial = solduriInitiale.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey))
				.findFirst()
				.map(Entry::getValue)
				.orElse(BigDecimal.ZERO);
		final ImmutableList<AccountingDocument> accDocs = accDocsByDate.values().stream()
				.flatMap(List::stream)
				.collect(toImmutableList());
		JasperViewer.viewReport(createRegIncasariPlati(bundle, gestiune, soldInitial, accDocs), false);
	}
	
	public void printRegIncasariPlati_Zile(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("RegIncasariPlati_Zile");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		accDocsByDate.entrySet().stream()
		.sorted(Comparator.comparing(Entry::getKey))
		.map(entry ->
		{
			try
			{
				return createRegIncasariPlati(bundle, gestiune, solduriInitiale.get(entry.getKey()), entry.getValue());
			}
			catch (final Exception e)
			{
				log.error(e);
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}
	
	public void printRegCasa_Inlantuit(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final BigDecimal soldInitial = solduriInitiale.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey))
				.findFirst()
				.map(Entry::getValue)
				.orElse(BigDecimal.ZERO);
		final ImmutableList<AccountingDocument> accDocs = accDocsByDate.values().stream()
				.flatMap(List::stream)
				.collect(toImmutableList());
		JasperViewer.viewReport(createRegCasa(bundle, gestiune, soldInitial, accDocs), false);
	}
	
	public void printRegCasa_Zile(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("RegCasa_Zile");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		accDocsByDate.entrySet().stream()
		.sorted(Comparator.comparing(Entry::getKey))
		.map(entry ->
		{
			try
			{
				return createRegCasa(bundle, gestiune, solduriInitiale.get(entry.getKey()), entry.getValue());
			}
			catch (final Exception e)
			{
				log.error(e);
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}
	
	public void printJurnalGeneral(final Bundle bundle, final LocalDate from, final LocalDate to, final ImmutableList<AccountingDocument> accDocs)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createJurnalGeneral(bundle, from, to, accDocs), false);
	}
	
	public void printDocTVA(final Bundle bundle, final LocalDate from, final LocalDate to, final ImmutableList<AccountingDocument> accDocs)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createDocTVA(bundle, from, to, accDocs), false);
	}
	
	public void printRegRPZ_Inlantuit(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final BigDecimal soldInitial = solduriInitiale.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey))
				.findFirst()
				.map(Entry::getValue)
				.orElse(BigDecimal.ZERO);
		final ImmutableList<AccountingDocument> accDocs = accDocsByDate.values().stream()
				.flatMap(List::stream)
				.collect(toImmutableList());
		JasperViewer.viewReport(createRegRPZ(bundle, gestiune, soldInitial, accDocs), false);
	}
	
	public void printRegRPZ_Zile(final Bundle bundle, final String gestiune, final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("RegRPZ_Zile");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		accDocsByDate.entrySet().stream()
		.sorted(Comparator.comparing(Entry::getKey))
		.map(entry ->
		{
			try
			{
				return createRegRPZ(bundle, gestiune, solduriInitiale.get(entry.getKey()), entry.getValue());
			}
			catch (final Exception e)
			{
				log.error(e);
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}
	
	public void printFiltruUrmarire(final Bundle bundle, final LocalDate from, final LocalDate to, final ImmutableList<AccountingDocument> accDocs)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createFiltruUrmarire(bundle, from, to, accDocs), false);
	}
	
	public void printRegBanca_Inlantuit(final Bundle bundle, final String gestiune, final ContBancar cont,
			final Map<LocalDate, List<AccountingDocument>> accDocsByDate, 
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final BigDecimal soldInitial = solduriInitiale.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey))
				.findFirst()
				.map(Entry::getValue)
				.orElse(BigDecimal.ZERO);
		final ImmutableList<AccountingDocument> accDocs = accDocsByDate.values().stream()
				.flatMap(List::stream)
				.collect(toImmutableList());
		JasperViewer.viewReport(createRegBanca(bundle, gestiune, cont, soldInitial, accDocs), false);
	}
	
	public void printRegBanca_Zile(final Bundle bundle, final String gestiune, final ContBancar cont,
			final Map<LocalDate, List<AccountingDocument>> accDocsByDate,
			final ImmutableMap<LocalDate, BigDecimal> solduriInitiale) throws IOException, JRException
	{
		final JasperPrint jasperReport = new JasperPrint();
		jasperReport.setName("RegBanca_Zile");
		jasperReport.setPageWidth(PAGE_A4_WIDTH);
		jasperReport.setPageHeight(PAGE_A4_HEIGHT);
		jasperReport.setOrientation(OrientationEnum.PORTRAIT);
		
		accDocsByDate.entrySet().stream()
		.sorted(Comparator.comparing(Entry::getKey))
		.map(entry ->
		{
			try
			{
				return createRegBanca(bundle, gestiune, cont, solduriInitiale.get(entry.getKey()), entry.getValue());
			}
			catch (final Exception e)
			{
				log.error(e);
				return null;
			}
		})
		.filter(Objects::nonNull)
		.flatMap(jPrint -> jPrint.getPages().stream())
		.forEach(jasperReport::addPage);

		if (!jasperReport.getPages().isEmpty())
			JasperViewer.viewReport(jasperReport, false);
	}
	
	public void printGDPR(final Bundle bundle, final String nume) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/gdpr.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(nume));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("nume", nume);
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorRowDetails", buildOneRowDetails(firmaDetails));
		parameters.put("furnizorEmail", firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource), false);
	}
	
	public void printBarchart(final Bundle bundle, final String title, final Gestiune gestiune, final ImmutableList<JasperChartSerie> stats)
			throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/barchart.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("title", title);
		parameters.put("gestiune", gestiune);
		parameters.put("chartDatasource", new JRBeanCollectionDataSource(stats));
		parameters.put("lineDatasource", new JRBeanCollectionDataSource(stats));
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters,
				new JRBeanCollectionDataSource(ImmutableList.of(EMPTY_STRING))), false);
	}
	
	public void printEticheteA4(final Bundle bundle,
			final ImmutableList<TwoEntityWrapper<BarcodePrintable>> simplePrintables,
			final ImmutableList<TwoEntityWrapper<BarcodePrintable>> promoPrintables,
			final ImmutableList<BarcodePrintable> cantPromoPrintables,
			final ImmutableList<ThreeEntityWrapper<BarcodePrintable>> simpleMiniPrintables,
			final ImmutableList<BarcodePrintable> cantPromoMiniPrintables) throws IOException, JRException
	{
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/eticheta_simple.jasper"))).getFile());
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/eticheta_promotie.jasper"))).getFile());
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/eticheta_promotie_cantitate.jasper"))).getFile());
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/eticheta_simple_mini.jasper"))).getFile());
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/eticheta_promotie_cantitate_mini.jasper"))).getFile());
		final URL url = FileLocator.find(bundle, new Path("resources/etichete_A4.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(EMPTY_STRING));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("simpleDatasource", new JRBeanCollectionDataSource(simplePrintables));
		parameters.put("promotiiDatasource", new JRBeanCollectionDataSource(promoPrintables));
		parameters.put("promotiiCantitateDatasource", new JRBeanCollectionDataSource(cantPromoPrintables));
		parameters.put("simpleMiniDatasource", new JRBeanCollectionDataSource(simpleMiniPrintables));
		parameters.put("promotiiCantitateMiniDatasource", new JRBeanCollectionDataSource(cantPromoMiniPrintables));
		
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource), false);
	}
	
	public void printPontaj(final Bundle bundle, final ImmutableList<PontajLine> lines, final LocalDate month)
			throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/pontaj.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(month));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("month", month);
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(lines));
		parameters.put("legend", ImmutableList.copyOf(PontajDayType.values()).stream()
				.map(pdt -> MessageFormat.format("{0} - {1}", pdt.displayName(), pdt.getDescription()))
				.collect(Collectors.joining(NEWLINE)));
		parameters.put("intocmit", ClientSession.instance().getLoggedUser().displayName());
		
		final JasperPrint pontajJasperReport = JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
		JasperViewer.viewReport(pontajJasperReport, false);
		
		// trimite email
		if (pontajJasperReport != null)
		{
			final String emailSignature = BusinessDelegate.persistedProp(PersistedProp.EMAIL_SIGNATURE_KEY)
					.getValueOr(PersistedProp.EMAIL_SIGNATURE_DEFAULT);
			final String contabilEmail = BusinessDelegate.persistedProp(PersistedProp.CONTABIL_EMAIL_KEY)
					.getValueOr(PersistedProp.CONTABIL_EMAIL_DEFAULT);
			final String contabilName = BusinessDelegate.persistedProp(PersistedProp.CONTABIL_NAME_KEY)
					.getValueOr(PersistedProp.CONTABIL_NAME_DEFAULT);

			final DateTimeFormatter monthPattern = DateTimeFormatter.ofPattern("uuuu-MM");
			final StringBuilder messageSB = new StringBuilder();
			messageSB.append("Catre ").append(contabilName).append(BR_SEPARATOR).append(BR_SEPARATOR)
			.append("Buna ziua,").append(BR_SEPARATOR).append(BR_SEPARATOR)
			.append("Atasat acestui email regasiti urmatoarele documente:").append(BR_SEPARATOR);
			messageSB.append(MessageFormat.format("Pontaj angajati luna {0}", month.format(monthPattern))).append(BR_SEPARATOR);
			messageSB.append(BR_SEPARATOR).append(emailSignature);

			final StringBuilder attachementsDescriptionSB = new StringBuilder();
			attachementsDescriptionSB.append(MessageFormat.format("Pontaj luna {0}", month.format(monthPattern)));

			SendEmailDialog.open(Display.getCurrent().getActiveShell(), log, contabilEmail, "Pontaj luna "+month.format(monthPattern),
					messageSB.toString(), JasperExportManager.exportReportToPdf(pontajJasperReport), attachementsDescriptionSB.toString());
		}
	}
	
	public void printSalarii(final Bundle bundle, final ImmutableCollection<AccountingDocument> docs) throws IOException, JRException
	{
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/salar_angajat.jasper"))).getFile());
		final URL url = FileLocator.find(bundle, new Path("resources/salarii.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
	
		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(EMPTY_STRING));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("docsDatasource", new JRBeanCollectionDataSource(docs));
		
		JasperViewer.viewReport(JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource), false);
	}
	
	public void printRegAnaf(final Bundle bundle, final LocalDate from, final LocalDate to, final Collection<ReceivedMessage> messages)
			throws IOException, JRException
	{
		JasperViewer.viewReport(createRegAnaf(bundle, from, to, messages), false);
	}
	
	private JasperPrint createFactura(final Bundle bundle, final AccountingDocument factura) throws IOException, JRException
	{
		final AccountingDocument facturaReload = loadFacturaWithoutConsumabile(factura);
		final JasperPrint jasperReport = createFacturaPage(bundle, facturaReload, "1");
		final JasperPrint firstCopy = createFacturaPage(bundle, facturaReload, "2");
		final JasperPrint secondCopy = createFacturaPage(bundle, facturaReload, "3");
		
		jasperReport.getPages().addAll(firstCopy.getPages());
		jasperReport.getPages().addAll(secondCopy.getPages());
		
		return jasperReport;
	}
	
	private AccountingDocument loadFacturaWithoutConsumabile(final AccountingDocument factura)
	{
		final AccountingDocument facturaReload = BusinessDelegate.reloadDoc(factura);
		
		if (facturaReload == null)
			return null;
		
		facturaReload.setOperatiuni(facturaReload.getOperatiuni_Stream()
				.filter(accDoc -> globalIsMatch(accDoc.getCategorie(), Product.ALTE_MATERIALE_CATEGORY, TextFilterMethod.NOT_EQUALS))
				.filter(accDoc -> globalIsMatch(accDoc.getCategorie(), Product.OBIECTE_INVENTAR_CATEGORY, TextFilterMethod.NOT_EQUALS))
				.filter(accDoc -> globalIsMatch(accDoc.getCategorie(), Product.SARCINI_CATEGORY, TextFilterMethod.NOT_EQUALS))
				.filter(accDoc -> globalIsMatch(accDoc.getCategorie(), Product.NOTIFICATION_CATEGORY, TextFilterMethod.NOT_EQUALS))
				.collect(toHashSet()));
		
		return facturaReload;
	}
	
	private JasperPrint createFacturaPage(final Bundle bundle, final AccountingDocument factura, final String duplicate) throws IOException, JRException
	{
		final BigDecimal tvaPercent = parse(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final boolean neplatitorTva = equal(tvaPercent, BigDecimal.ZERO);
		final String filepath = neplatitorTva ? "resources/factura_neplatitor.jasper" : "resources/factura.jasper";
		final URL url = FileLocator.find(bundle, new Path(filepath));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final String seriaFactura = BusinessDelegate.persistedProp(PersistedProp.SERIA_FACTURA_KEY)
				.getValueOr(PersistedProp.SERIA_FACTURA_DEFAULT);
		final String tvaReadable = Operatiune.tvaReadable(tvaPercent);
		final String atentionare = BusinessDelegate.persistedProp("invoice_title").getValue();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(factura));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new AccDocDatasource(factura));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildDetails(firmaDetails));
		parameters.put("furnizorRowDetails", buildOneRowDetails(firmaDetails));
		parameters.put("seriaDoc", seriaFactura);
		parameters.put("clientName", safeString(factura.getPartner(), Partner::getName));
		parameters.put("clientDetails", buildFirmaDetails(factura.getPartner()));
		parameters.put("tvaReadable", tvaReadable);
		parameters.put("gestiune", safeString(factura.getGestiune(), Gestiune::getName));
		parameters.put("gestiuneShort", safeString(factura.getGestiune(), Gestiune::getImportName));
		parameters.put("delegatDetails", buildDetails(factura.getPartner().getDelegat(), factura.getDataDoc()));
		parameters.put("intocmit", MessageFormat.format("TERMEN DE PLATA {1} ZILE; INTOCMIT DE {0}", 
				safeString(factura.getOperator(), User::getName),
				safeString(factura.getPartner(), Partner::getTermenPlata, String::valueOf, AccountingDocument.DEFAULT_TERMEN_PLATA.toString())));
		parameters.put("totalFaraTVA", factura.getVanzareTotal().subtract(factura.getVanzareTotalTva()));
		parameters.put("totalTVA", factura.getVanzareTotalTva());
		parameters.put("total", factura.getVanzareTotal());
		parameters.put("duplicateNr", duplicate);
		parameters.put("atentionare", atentionare);
		
		final Optional<String> signatureUuid = ClientSession.instance().loggedUserSignature();
		if (signatureUuid.isPresent())
			parameters.put("signatureImage", BusinessDelegate.imagePathFromUuid(bundle, log, signatureUuid.get()));
		final Optional<String> stampUuid = ClientSession.instance().loggedUserStamp();
		if (stampUuid.isPresent())
			parameters.put("stampImage", BusinessDelegate.imagePathFromUuid(bundle, log, stampUuid.get()));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createReceptie(final Bundle bundle, final AccountingDocument doc) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/receptie.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final BigDecimal tvaPercent = parse(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final String tvaReadable = Operatiune.tvaReadable(tvaPercent);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(doc));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new AccDocDatasource(doc));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildShortDetails(firmaDetails));
		parameters.put("clientName", safeString(doc.getPartner(), Partner::getName));
		parameters.put("clientDetails", buildFirmaDetails(doc.getPartner()));
		parameters.put("tvaReadable", tvaReadable);
		parameters.put("gestiune", safeString(doc.getGestiune(), Gestiune::getName));
		parameters.put("receptionat", MessageFormat.format("Receptionat de {0}; CNP {1}", 
				safeString(doc.getOperator(), User::getName),
				safeString(doc.getOperator(), User::getCnp)));
		parameters.put("achTotalFaraTVA", doc.getAchizitieTotal().subtract(doc.getAchizitieTotalTva()));
		parameters.put("achTotalTVA", doc.getAchizitieTotalTva());
		parameters.put("achTotal", doc.getAchizitieTotal());
		parameters.put("totalFaraTVA", doc.getVanzareTotal().subtract(doc.getVanzareTotalTva()));
		parameters.put("totalTVA", doc.getVanzareTotalTva());
		parameters.put("total", doc.getVanzareTotal());
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createChitanta(final Bundle bundle, final AccountingDocument chitanta) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/chitanta.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final String seriaFactura = BusinessDelegate.persistedProp(PersistedProp.SERIA_FACTURA_KEY)
				.getValueOr(PersistedProp.SERIA_FACTURA_DEFAULT);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(chitanta));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("firmaName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("firmaDetails", buildShortDetails(firmaDetails));
		parameters.put("seriaDoc", seriaFactura);
		
		parameters.put("description", MessageFormat.format(CHITANTA_DESCRIPTION, 
				safeString(chitanta.getPartner(), Partner::getName),
				safeString(chitanta.getPartner(), Partner::getCodFiscal),
				safeString(chitanta.getPartner(), Partner::getAddress, Address::displayName),
				displayBigDecimal(chitanta.getTotal()),
				toWords(chitanta.getTotal()),
				safeString(chitanta.getName())));
		
		final Optional<String> signatureUuid = ClientSession.instance().loggedUserSignature();
		if (signatureUuid.isPresent())
			parameters.put("signatureImage", BusinessDelegate.imagePathFromUuid(bundle, log, signatureUuid.get()));
		final Optional<String> stampUuid = ClientSession.instance().loggedUserStamp();
		if (stampUuid.isPresent())
			parameters.put("stampImage", BusinessDelegate.imagePathFromUuid(bundle, log, stampUuid.get()));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createChitantaPage(final Bundle bundle, final AccountingDocument chitanta, final String duplicate) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/chitanta_single.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final String seriaFactura = BusinessDelegate.persistedProp(PersistedProp.SERIA_FACTURA_KEY)
				.getValueOr(PersistedProp.SERIA_FACTURA_DEFAULT);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(chitanta));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("firmaName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("firmaDetails", buildShortDetails(firmaDetails));
		parameters.put("seriaDoc", seriaFactura);
		parameters.put("duplicateNr", duplicate);
		
		parameters.put("description", MessageFormat.format(CHITANTA_DESCRIPTION, 
				safeString(chitanta.getPartner(), Partner::getName),
				safeString(chitanta.getPartner(), Partner::getCodFiscal),
				safeString(chitanta.getPartner(), Partner::getAddress, Address::displayName),
				displayBigDecimal(chitanta.getTotal()),
				toWords(chitanta.getTotal()),
				safeString(chitanta.getName())));
		
		final Optional<String> signatureUuid = ClientSession.instance().loggedUserSignature();
		if (signatureUuid.isPresent())
			parameters.put("signatureImage", BusinessDelegate.imagePathFromUuid(bundle, log, signatureUuid.get()));
		final Optional<String> stampUuid = ClientSession.instance().loggedUserStamp();
		if (stampUuid.isPresent())
			parameters.put("stampImage", BusinessDelegate.imagePathFromUuid(bundle, log, stampUuid.get()));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createNonOfficialDoc(final Bundle bundle, final AccountingDocument doc, final boolean reload)
			throws IOException, JRException
	{
		AccountingDocument reloadedDoc = doc;
		if (doc.getId() != null && reload)
		{
			reloadedDoc = BusinessDelegate.reloadDoc(doc);
			if (reloadedDoc == null)
				throw new IllegalArgumentException("Documentul cu id "+doc.getId()+" nu a fost gasit in baza de date!");

			final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(reloadedDoc);
			reloadedDoc.setOperatiuni(new HashSet<Operatiune>(operatiuni));
		}

		final URL url = FileLocator.find(bundle, new Path("resources/nonOfficialDoc.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final BigDecimal tvaPercent = parse(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final String tvaReadable = Operatiune.tvaReadable(tvaPercent);
		
		final Optional<BigDecimal> totalDiscount = reloadedDoc.getOperatiuni().stream()
				.filter(op -> Objects.equals(op.getCategorie(), Product.DISCOUNT_CATEGORY))
				.filter(op -> NumberUtils.smallerThan(op.getCantitate(), BigDecimal.ZERO))
				.map(op -> NumberUtils.add(op.getValoareVanzareFaraTVA(), op.getValoareVanzareTVA()).abs())
				.reduce(BigDecimal::add);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(reloadedDoc));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new AccDocDatasource(reloadedDoc));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildDetails(firmaDetails));
		parameters.put("clientName", safeString(reloadedDoc.getPartner(), Partner::getName));
		parameters.put("clientDetails", buildDetails(reloadedDoc));
		parameters.put("tvaReadable", tvaReadable);
		parameters.put("gestiune", safeString(reloadedDoc.getGestiune(), Gestiune::getName));
		parameters.put("total", reloadedDoc.getVanzareTotal());
		parameters.put("totalDiscount", totalDiscount.orElse(null));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createCatalogProduse(final Bundle bundle, final Gestiune gestiune, final ImmutableList<Product> products) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/catalog_produse.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new ProductsDatasource(products));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", safeString(gestiune, Gestiune::getName));
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createCatalogAnaf(final Bundle bundle, final Gestiune gestiune, final ImmutableList<Product> products, final BigDecimal soldFinal)
			throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/catalog_anaf.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new ProductsDatasource(products));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", safeString(gestiune, Gestiune::getName));
		parameters.put("soldFinal", soldFinal);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createFisaParteneri_Centralizat(final Bundle bundle, final LocalDate from, final LocalDate to, 
			final ImmutableList<RulajPartener> rulajeParteneri) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/fisa_parteneri_centralizat.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(from));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(rulajeParteneri));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("from", from);
		parameters.put("to", to);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createRegIncasariPlati(final Bundle bundle, final String gestiune, final BigDecimal soldInitial,
			final List<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/reg_incasari_plati.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final BigDecimal subtotalPlata = accDocs.stream()
				.filter(accDoc -> TipDoc.PLATA.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal subtotalIncasare = accDocs.stream()
				.filter(accDoc -> TipDoc.INCASARE.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", gestiune);
		parameters.put("subtotalPlata", subtotalPlata);
		parameters.put("subtotalIncasare", subtotalIncasare);
		parameters.put("soldInitial", soldInitial);
		parameters.put("soldFinal", soldInitial.add(subtotalIncasare).subtract(subtotalPlata));
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createRegCasa(final Bundle bundle, final String gestiune, final BigDecimal soldInitial,
			final List<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/reg_casa.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final BigDecimal subtotalPlata = accDocs.stream()
				.filter(accDoc -> TipDoc.PLATA.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal subtotalIncasare = accDocs.stream()
				.filter(accDoc -> TipDoc.INCASARE.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", gestiune);
		parameters.put("subtotalPlata", subtotalPlata);
		parameters.put("subtotalIncasare", subtotalIncasare);
		parameters.put("soldInitial", soldInitial);
		parameters.put("soldFinal", soldInitial.add(subtotalIncasare).subtract(subtotalPlata));
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createJurnalGeneral(final Bundle bundle, final LocalDate from, final LocalDate to,
			final ImmutableList<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/jurnal_general.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(from));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("from", from);
		parameters.put("to", to);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createDocTVA(final Bundle bundle, final LocalDate from, final LocalDate to,
			final ImmutableList<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/doc_tva.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(from));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("from", from);
		parameters.put("to", to);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createRegRPZ(final Bundle bundle, final String gestiune, final BigDecimal soldInitial,
			final List<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/reg_rpz.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		
		final BigDecimal subtotalIntrare = accDocs.stream()
				.filter(AccountingDocument::isIntrareInRpz)
				.map(AccountingDocument::getTotalRpz)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		final BigDecimal subtotalIesire = accDocs.stream()
				.filter(AccountingDocument::isIesireInRpz)
				.map(AccountingDocument::getTotalRpz)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs.stream()
				.sorted(Comparator.comparing(AccountingDocument::isIntrareInRpz).reversed().thenComparing(AccountingDocument::getDataDoc))
				.collect(toImmutableList())));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", gestiune);
		parameters.put("soldInitial", soldInitial);
		parameters.put("soldFinal", soldInitial.add(subtotalIntrare).subtract(subtotalIesire));
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createFiltruUrmarire(final Bundle bundle, final LocalDate from, final LocalDate to,
			final ImmutableList<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/filtru_urmarire.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(from));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("from", from);
		parameters.put("to", to);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createRegBanca(final Bundle bundle, final String gestiune, final ContBancar cont,
			final BigDecimal soldInitial, final List<AccountingDocument> accDocs) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/reg_banca.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final BigDecimal subtotalPlata = accDocs.stream()
				.filter(accDoc -> TipDoc.PLATA.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal subtotalIncasare = accDocs.stream()
				.filter(accDoc -> TipDoc.INCASARE.equals(accDoc.getTipDoc()))
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(gestiune));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(accDocs));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("gestiune", gestiune + SPACE + safeString(cont, ContBancar::displayName));
		parameters.put("subtotalPlata", subtotalPlata);
		parameters.put("subtotalIncasare", subtotalIncasare);
		parameters.put("soldInitial", soldInitial);
		parameters.put("soldFinal", soldInitial.add(subtotalIncasare).subtract(subtotalPlata));
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createPlanLivrare(final Bundle bundle, final AccountingDocument doc) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/plan_livrare.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(doc));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new AccDocDatasource(doc));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildDetails(firmaDetails));
		parameters.put("clientName", safeString(doc.getPartner(), Partner::getName));
		parameters.put("clientDetails", buildDetails(doc));
		parameters.put("gestiune", safeString(doc.getGestiune(), Gestiune::getName));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createDispPlataIncasare(final Bundle bundle, final ImmutableList<AccountingDocument> disp) throws IOException, JRException
	{
		new File(FileLocator.toFileURL(FileLocator.find(bundle, new Path("resources/dispozitie_pi_emb.jasper"))).getFile());
		final URL url = FileLocator.find(bundle, new Path("resources/dispozitie_pi.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(EMPTY_STRING));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("docsDatasource", new JRBeanCollectionDataSource(disp));
		parameters.put("firmaName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("firmaDetails", buildOneRowDetails(firmaDetails));
		
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
	
	private JasperPrint createRegAnaf(final Bundle bundle, final LocalDate from, final LocalDate to,
			final Collection<ReceivedMessage> messages) throws IOException, JRException
	{
		final URL url = FileLocator.find(bundle, new Path("resources/reg_anaf.jasper"));
		final URL fileUrl = FileLocator.toFileURL(url);
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();

		final JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(ImmutableList.of(from));
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("tableDataSource", new JRBeanCollectionDataSource(messages));
		parameters.put("furnizorName", firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		parameters.put("furnizorDetails", buildRequiredDetails(firmaDetails));
		parameters.put("from", from);
		parameters.put("to", to);
		return JasperFillManager.fillReport(fileUrl.getFile(), parameters, beanColDataSource);
	}
}
