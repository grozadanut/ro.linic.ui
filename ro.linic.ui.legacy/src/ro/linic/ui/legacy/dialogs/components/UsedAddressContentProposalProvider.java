package ro.linic.ui.legacy.dialogs.components;

import java.util.ArrayList;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.StringUtils.SmartNameMatcher;
import ro.colibri.wrappers.ThreeEntityWrapper;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class UsedAddressContentProposalProvider implements IContentProposalProvider
{
	private Job loadJob;
	/**
	 * The proposals provided.
	 * 1 - Address
	 * 2 - Phone
	 * 3 - Indicatii
	 */
	private ImmutableList<ThreeEntityWrapper<String>> proposals;
	private Partner selectedPartner;

	/*
	 * The proposals mapped to IContentProposal. Cached for speed in the case
	 * where filtering is not used.
	 */
	private IContentProposal[] contentProposals;

	/*
	 * Boolean that tracks whether filtering is used.
	 */
	private boolean filterProposals = true;
	
	private Logger log;

	public UsedAddressContentProposalProvider(final Logger log)
	{
		super();
		this.log = log;
	}

	/**
	 * Return an array of Objects representing the valid content proposals for a
	 * field.
	 *
	 * @param contents
	 *            the current contents of the field (only consulted if filtering
	 *            is set to <code>true</code>)
	 * @param position
	 *            the current cursor position within the field (ignored)
	 * @return the array of Objects that represent valid proposals for the field
	 *         given its current content.
	 */
	@Override
	public IContentProposal[] getProposals(final String contents, final int position)
	{
		if (filterProposals)
		{
			final ArrayList<UsedAddressContentProposal> list = new ArrayList<>();
			final SmartNameMatcher contentsMatcher = SmartNameMatcher.create(contents);
			for (final ThreeEntityWrapper<String> proposal : proposals())
			{
				if (contentsMatcher.isMatch(proposal.getEntity1()) ||
						contentsMatcher.isMatch(proposal.getEntity2()) ||
						contentsMatcher.isMatch(proposal.getEntity3()))
					list.add(new UsedAddressContentProposal(proposal));
			}
			return list.toArray(new IContentProposal[list.size()]);
		}
		if (contentProposals == null)
		{
			contentProposals = new IContentProposal[proposals().size()];
			for (int i = 0; i < proposals().size(); i++)
				contentProposals[i] = new UsedAddressContentProposal(proposals().get(i));
		}
		return contentProposals;
	}
	
	private ImmutableList<ThreeEntityWrapper<String>> proposals()
	{
		if (proposals == null)
		{
			loadJob = BusinessDelegate.loadUsedAddresses(new AsyncLoadData<ThreeEntityWrapper<String>>()
			{
				@Override public void success(final ImmutableList<ThreeEntityWrapper<String>> data)
				{
					proposals = data;
				}

				@Override public void error(final String details)
				{
					log.error(details);
				}
			}, selectedPartner);
			
			// Temporary list so we don't get a NPE,
			// the actual proposals will be filled when the data finishes loading
			proposals = ImmutableList.of();
		}
		
		return proposals;
	}

	/**
	 * Set the boolean that controls whether proposals are filtered according to
	 * the current field content.
	 *
	 * @param filterProposals
	 *            <code>true</code> if the proposals should be filtered to
	 *            show only those that match the current contents of the field,
	 *            and <code>false</code> if the proposals should remain the
	 *            same, ignoring the field content.
	 * @since 3.3
	 */
	public void setFiltering(final boolean filterProposals)
	{
		this.filterProposals = filterProposals;
		// Clear any cached proposals.
		this.contentProposals = null;
	}
	
	public void setSelectedPartner(final Partner selectedPartner)
	{
		cancelLoadJob();
		this.selectedPartner = selectedPartner;
		this.proposals = null;
		this.contentProposals = null;
	}

	private void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
	}
}