package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.NumberUtils.equal;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.math.BigDecimal;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ListUtils;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.expressions.BetaTester;
import ro.linic.ui.legacy.mapper.AccDocMapper;
import ro.linic.ui.legacy.service.CasaMarcat;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.services.ECRService;
import ro.linic.ui.security.services.AuthenticationSession;

public class CustomerDebtWizard extends Wizard
{
	private CustomerDebtSelectPage one;
	private CustomerDebtConfirmPage two;
	
	private AuthenticationSession authSession;
	private IEclipseContext ctx;
	private Bundle bundle;
	private Logger log;
	
	private List<AccountingDocument> sourceDocs = ImmutableList.of();
	private List<AccountingDocument> paidDocs = ImmutableList.of();
	private List<AccountingDocument> bonuriCasa = ImmutableList.of();
	private List<DocumentWithDiscount> discountDocs = ImmutableList.of();
	private String errors;
	
	static Stream<AccountingDocument> notFullyCoveredVanzari(final List<Object> toBePaid)
	{
		if (toBePaid.stream().filter(RulajPartener.class::isInstance).findAny().isPresent())
		{
			/**
			 * THIS HAPPENS ON THE SERVER SIDE
			 * Partner selected; 
			 * 1. we create accDoc payment for not fully covered docs from this partner
			 * 2. if we have some money left, we create incasare with the rest for this partner
			 */
			return toBePaid.stream()
           		.filter(RulajPartener.class::isInstance)
           		.map(RulajPartener.class::cast)
           		.flatMap(RulajPartener::getAccDocsStream);
		}
		else
		{
			/**
			 * THIS HAPPENS ON THE SERVER SIDE
			 * Docs selected; we create accDoc payments for selected docs
			 */
			return toBePaid.stream()
            		.filter(AccountingDocument.class::isInstance)
            		.map(AccountingDocument.class::cast);
		}
	}
	
	public CustomerDebtWizard(final IEclipseContext ctx, final Bundle bundle, final Logger log, final AuthenticationSession authSession)
	{
		super();
		this.authSession = authSession;
		this.ctx = ctx;
		this.bundle = bundle;
		this.log = log;
	}

	@Override
	public String getWindowTitle()
	{
		return "Incaseaza";
	}

	@Override
	public void addPages()
	{
		one = new CustomerDebtSelectPage(ctx, bundle, log);
		two = new CustomerDebtConfirmPage();
		addPage(one);
		addPage(two);
	}
	

	@Override
	public boolean performFinish()
	{
		// 0. SAVE TO DB
		if (!createPaidDocs(true))
		{
			updateResultText();
			return false;
		}
		
		try
		{
			// 1. Scoate Bon Fiscal(daca e cazul)
			if (one.casaActiva())
				ctx.get(ECRService.class).printReceipt(AccDocMapper.toReceipt(bonuriCasa),
						one.contBancar() != null ? PaymentType.CARD : PaymentType.CASH)
				.thenAcceptAsync(new CasaMarcat.UpdateDocStatus(bonuriCasa.stream()
						.map(AccountingDocument::getId).collect(Collectors.toSet()), false));
		}
		catch (final Exception ex)
		{
			log.error(ex);
			showException(ex, "Eroare in printarea bonului de casa. Scoateti bonul de casa manual!");
			BusinessDelegate.closeBonCasa_Failed(bonuriCasa.stream()
					.map(AccountingDocument::getId).collect(ListUtils.toImmutableSet()));
		}

		try
		{
			// 2. Print Chitante(daca e cazul)
			// 3. Print documente platite(daca e cazul)
			final ArrayList<AccountingDocument> docsToPrint = new ArrayList<>();
			docsToPrint.addAll(AccountingDocument.extractPrintable(paidDocs));

			if (one.transformaInFactura())
				docsToPrint.addAll(JasperReportManager.comasarePtPrint(sourceDocs, true));

			JasperReportManager.instance(bundle, log).printDocs(bundle, ImmutableList.copyOf(docsToPrint), false);
		}
		catch (final Exception ex)
		{
			log.error(ex);
			showException(ex, "Documentele nu au putut fi printate!");
		}
		
		try
		{
			// sync to moqui
			if (new BetaTester().evaluate(ctx.get(AuthenticationSession.class))) {
				for (final AccountingDocument incasare : paidDocs) {
					final Map<String, Object> partnerBody = Map.of("paymentId", incasare.getId(),
							"fromPartyId", incasare.getPartner().getId(),
							"toPartyId", incasare.getGestiune().getImportName(),
							"amount", incasare.getTotal(),
							"affiliatePartnerId", one.affiliate().map(gv -> gv.getString("partyId")).orElse(null));
					
					RestCaller.put("/rest/s1/moqui-linic-legacy/payment")
					.internal(ctx.get(AuthenticationSession.class).authentication())
					.body(BodyProvider.of(HttpUtils.toJSON(partnerBody)))
					.sync(GenericValue.class, t -> UIUtils.showException(t, ctx.get(UISynchronize.class)));
				}
			}
			
			// produse vandute in cealalta gestiune
			final BigDecimal totalOtherGest = sourceDocs.stream()
					.filter(AccountingDocument::isFullyCovered)
					.flatMap(AccountingDocument::getOperatiuni_Stream)
					.filter(op -> !op.getGestiune().equals(ClientSession.instance().getGestiune()))
					.map(op -> NumberUtils.add(op.getValoareVanzareFaraTVA(), op.getValoareVanzareTVA()))
					.reduce(BigDecimal::add)
					.orElse(BigDecimal.ZERO);
			if (greaterThan(totalOtherGest, BigDecimal.ZERO)) {
				final Map<String, Object> body = Map.of("topic", "LegacyGeneral",
						"showAlert", true,
						"title", MessageFormat.format("Vanzari din cealalta gestiune fara transfer: {0} RON{1}{2}", displayBigDecimal(totalOtherGest),
								NEWLINE,
								sourceDocs.stream()
								.filter(AccountingDocument::isFullyCovered)
								.map(AccountingDocument::namestamp)
								.collect(Collectors.joining(NEWLINE))),
						"userIds", Set.of("100000"),
						"persist", true);
				
				RestCaller.post("/rest/s1/moqui-linic-legacy/notification")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(body)))
				.asyncRaw(BodyHandlers.ofString())
				.thenApply(HttpUtils::checkOk)
				.thenAccept(resp -> log.info(resp.body()))
				.exceptionally(t -> {
					log.error(t.getMessage(), t);
					return null;
				});
			}
		}
		catch (final Exception ex)
		{
			log.error(ex);
			showException(ex, "Notificarea nu a putut fi trimisa!");
		}
		
		return true;
	}
	
	public void nextPressed()
	{
		createPaidDocs(false);
		updateResultText();
		two.setPageComplete(true);
	}
	
	public void backPressed()
	{
		two.setPageComplete(false);
	}
	
	/**
	 * @param persist
	 * @return true if success, false if operation was cancelled
	 */
	private boolean createPaidDocs(final boolean persist)
	{
		final List<Object> toBePaid = one.selection();
		final ImmutableSet<Long> notFullyCoveredVanzari = notFullyCoveredVanzari(toBePaid)
        		.map(AccountingDocument::getId)
        		.collect(toImmutableSet());
		
		final Long partnerId = toBePaid.stream()
				.filter(RulajPartener.class::isInstance)
        		.map(RulajPartener.class::cast)
				.map(RulajPartener::getId)
				.findFirst()
				.orElse(null);
		
		final InvocationResult result = BusinessDelegate.incaseaza(notFullyCoveredVanzari, partnerId, one.incasat(),
				one.contBancar(), one.casaActiva(), persist, one.paidDocNr(), one.canTransformInFactura() && one.transformaInFactura(), 
				one.canTransformInBC() && one.transformaInBC(),
				one.canAddDiscount() && one.addDiscount(), one.discChelt(), one.dataDoc(), one.selectedTempDocIds());
		
		if (result.statusCanceled())
		{
			errors = result.toTextDescriptionWithCode();
			sourceDocs = ImmutableList.of();
			paidDocs = ImmutableList.of();
			bonuriCasa = ImmutableList.of();
			discountDocs = ImmutableList.of();
			return false;
		}
		else
		{
			errors = result.toTextDescription();
			sourceDocs = result.extra(InvocationResult.ACCT_DOC_KEY);
			paidDocs = result.extra(InvocationResult.PAID_DOCS_KEY);
			bonuriCasa = result.extra(InvocationResult.BONURI_CASA_KEY);
			discountDocs = result.extra(InvocationResult.DISCOUNT_DOC_KEY);
			return true;
		}
	}

	private void updateResultText()
	{
		final StringBuilder sb = new StringBuilder();
		final ImmutableList<AccountingDocument> chitante = AccountingDocument.extractChitante(paidDocs);
		final ImmutableList<AccountingDocument> nonOfficialIncasari = AccountingDocument.extractNotChitante(paidDocs);
		
		final ImmutableList<DocumentWithDiscount> discIncasari = discountDocs.stream()
				.filter(doc -> TipDoc.INCASARE.equals(doc.getTipDoc()))
				.collect(toImmutableList());
		final ImmutableList<DocumentWithDiscount> discPlati = discountDocs.stream()
				.filter(doc -> TipDoc.PLATA.equals(doc.getTipDoc()))
				.collect(toImmutableList());
		
		final BigDecimal totalChitante = chitante.stream()
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal totalNonOfficialIncasari = nonOfficialIncasariTotal();
		
		final BigDecimal achitat = one.incasat();
		final BigDecimal totalBonFiscal = bonuriCasa.stream()
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal totalIncasari = totalChitante.add(totalBonFiscal);
		
		final BigDecimal totalDiscAcum = discIncasari.stream()
				.map(DocumentWithDiscount::calculatedDiscount)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal totalDiscFolosit = discPlati.stream()
				.map(DocumentWithDiscount::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		// atentionari
		if (achitat.subtract(totalDiscFolosit).compareTo(totalIncasari) != 0)
			sb.append(MessageFormat.format("ATENTIE! Totalul introdus este {0}, iar totalul documentelor create este {1}", displayBigDecimal(achitat.subtract(totalDiscFolosit)), displayBigDecimal(totalIncasari)))
			.append(NEWLINE).append(NEWLINE);
		if (!equal(one.discChelt(), totalDiscFolosit))
			sb.append(MessageFormat.format("ATENTIE! Discountul folosit introdus este {0}, iar discountul folosit efectiv este {1}", displayBigDecimal(one.discChelt()), displayBigDecimal(totalDiscFolosit)))
			.append(NEWLINE).append(NEWLINE);
		
		// total casa sau card
		if (one.casaActiva() && greaterThan(totalBonFiscal, BigDecimal.ZERO))
			sb.append(MessageFormat.format("Prin casa de marcat{0}: {1} RON", one.contBancar() != null ? "(CARD)" : EMPTY_STRING, displayBigDecimal(totalBonFiscal)))
			.append(NEWLINE);
		
		// documentele platite
		if (!sourceDocs.isEmpty())
			sb.append(NEWLINE).append("Documentele platite:").append(NEWLINE)
			.append(sourceDocs.stream().map(AccountingDocument::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE);
		
		// documente create si totaluri
		if (!chitante.isEmpty())
			sb.append(NEWLINE).append("Chitante:").append(NEWLINE)
			.append(chitante.stream().map(AccountingDocument::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE)
			.append("Total ").append(displayBigDecimal(totalChitante)).append(" RON").append(NEWLINE);
		
		if (!nonOfficialIncasari.isEmpty())
			sb.append(NEWLINE).append("Incasari:").append(NEWLINE)
			.append(nonOfficialIncasari.stream().map(AccountingDocument::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE)
			.append("Total ").append(displayBigDecimal(totalNonOfficialIncasari)).append(" RON").append(NEWLINE);
		
		// discounturi acumulate
		if (!discIncasari.isEmpty())
			sb.append(NEWLINE).append("Puncte de fidelitate:").append(NEWLINE)
			.append(discIncasari.stream().map(DocumentWithDiscount::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE)
			.append("Total disc acumulat ").append(displayBigDecimal(totalDiscAcum)).append(" RON").append(NEWLINE);
		
		// discounturi folosite
		if (!discPlati.isEmpty())
			sb.append(NEWLINE)
			.append(discPlati.stream().map(DocumentWithDiscount::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE);
		
		// ERORI de la server
		if (!isEmpty(errors) && globalIsMatch(errors, "OK", TextFilterMethod.NOT_EQUALS))
			sb.append(NEWLINE).append("Erori:").append(NEWLINE).append(errors);
		
		two.resultDescription(sb.toString());
	}
	
	private BigDecimal nonOfficialIncasariTotal()
	{
		return AccountingDocument.extractNotChitante(paidDocs).stream()
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}
}
