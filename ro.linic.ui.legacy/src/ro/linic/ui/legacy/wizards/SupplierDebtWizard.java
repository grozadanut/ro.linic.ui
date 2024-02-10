package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class SupplierDebtWizard extends Wizard
{
	private SupplierDebtSelectPage one;
	private SupplierDebtConfirmPage two;
	
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	
	private ImmutableList<AccountingDocument> paidDoc = ImmutableList.of();
	private String errors;
	
	static Stream<AccountingDocument> notFullyCoveredCumparari(final List<Object> toBePaid)
	{
		//		if (toBePaid.stream().filter(RulajPartener.class::isInstance).findAny().isPresent())
		//		{
		//			/**
		//			 * THIS HAPPENS ON THE SERVER SIDE
		//			 * Partner selected; 
		//			 * 1. we create accDoc payment for not fully covered docs from this partner
		//			 * 2. if we have some money left, we create plata with the rest for this partner
		//			 */
		//			return toBePaid.stream()
		//           		.filter(RulajPartener.class::isInstance)
		//           		.map(RulajPartener.class::cast)
		//           		.flatMap(RulajPartener::getAccDocsStream);
		//		}
		//		else
		//		{
		/**
		 * THIS HAPPENS ON THE SERVER SIDE
		 * Docs selected; we create accDoc payments for selected docs
		 */
		return toBePaid.stream()
				.filter(AccountingDocument.class::isInstance)
				.map(AccountingDocument.class::cast);
		//		}
	}

	public SupplierDebtWizard(final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		super();
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
	}

	@Override
	public String getWindowTitle()
	{
		return "Plateste";
	}

	@Override
	public void addPages()
	{
		one = new SupplierDebtSelectPage(sync, bundle, log);
		two = new SupplierDebtConfirmPage();
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
		final ImmutableSet<Long> notFullyCoveredCumparari = notFullyCoveredCumparari(toBePaid)
        		.map(AccountingDocument::getId)
        		.collect(toImmutableSet());
		
		final Long partnerId = toBePaid.stream()
				.filter(RulajPartener.class::isInstance)
        		.map(RulajPartener.class::cast)
				.map(RulajPartener::getId)
				.findFirst()
				.orElse(null);
		
		final InvocationResult result = BusinessDelegate.platesteDocs(notFullyCoveredCumparari, partnerId, one.platit(), one.regCasa(),
				one.contBancar(), one.doc(), one.nrDoc(), one.dataDoc(), persist);
		
		if (result.statusCanceled())
		{
			errors = result.toTextDescriptionWithCode();
			paidDoc = ImmutableList.of();
			return false;
		}
		else
		{
			errors = result.toTextDescription();
			paidDoc = ImmutableList.of(result.extra(InvocationResult.CHITANTA_KEY));
			return true;
		}
	}
	
	private void updateResultText()
	{
		final StringBuilder sb = new StringBuilder();
		final ImmutableList<AccountingDocument> chitante = AccountingDocument.extractChitante(paidDoc);
		final ImmutableList<AccountingDocument> nonOfficialPlati = AccountingDocument.extractNotChitante(paidDoc);
		
		final BigDecimal totalChitante = chitante.stream()
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal totalNonOfficialPlati = nonOfficialPlati.stream()
				.map(AccountingDocument::getTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		final BigDecimal achitat = one.platit();
		final BigDecimal totalDocumente = totalChitante.add(totalNonOfficialPlati);
		
		// atentionari
		if (achitat.compareTo(totalDocumente) != 0)
			sb.append(MessageFormat.format("ATENTIE! Totalul introdus este {0}, iar totalul documentelor create este {1}", displayBigDecimal(achitat), displayBigDecimal(totalDocumente)))
			.append(NEWLINE).append(NEWLINE);
		
		// documente create si totaluri
		if (!chitante.isEmpty())
			sb.append(NEWLINE).append("Chitante:").append(NEWLINE)
			.append(chitante.stream().map(AccountingDocument::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE)
			.append("Total ").append(displayBigDecimal(totalChitante)).append(" RON").append(NEWLINE);
		
		if (!nonOfficialPlati.isEmpty())
			sb.append(NEWLINE).append("Plati:").append(NEWLINE)
			.append(nonOfficialPlati.stream().map(AccountingDocument::displayName).collect(Collectors.joining(NEWLINE)))
			.append(NEWLINE)
			.append("Total ").append(displayBigDecimal(totalNonOfficialPlati)).append(" RON").append(NEWLINE);
		
		// ERORI de la server
		if (!isEmpty(errors) && globalIsMatch(errors, "OK", TextFilterMethod.NOT_EQUALS))
			sb.append(NEWLINE).append("Erori:").append(NEWLINE).append(errors);
		
		two.resultDescription(sb.toString());
	}
}
