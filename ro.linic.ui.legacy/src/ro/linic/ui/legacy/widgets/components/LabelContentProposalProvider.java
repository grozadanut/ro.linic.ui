package ro.linic.ui.legacy.widgets.components;

import java.util.ArrayList;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import ro.colibri.util.StringUtils.SmartNameMatcher;

public class LabelContentProposalProvider implements IContentProposalProvider
{
	/*
	 * The proposals provided.
	 */
	private IContentProposal[] proposals;

	/*
	 * The proposals mapped to IContentProposal. Cached for speed in the case
	 * where filtering is not used.
	 */
	private IContentProposal[] contentProposals;

	/*
	 * Boolean that tracks whether filtering is used.
	 */
	private boolean filterProposals = true;

	/**
	 * Construct a SimpleContentProposalProvider whose content proposals are always
	 * the specified array of Objects.
	 *
	 * @param proposals
	 *            the Strings to be returned whenever proposals are requested.
	 */
	public LabelContentProposalProvider(final IContentProposal... proposals)
	{
		super();
		this.proposals = proposals;
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
	public IContentProposal[] getProposals(final String contents, final int position) {
		if (filterProposals) {
			final ArrayList<ContentProposal> list = new ArrayList<>();
			final SmartNameMatcher contentsMatcher = SmartNameMatcher.create(contents);
			for (final IContentProposal proposal : proposals) {
				if (contentsMatcher.isMatch(proposal.getContent()) ||
						contentsMatcher.isMatch(proposal.getLabel()) ||
						contentsMatcher.isMatch(proposal.getDescription())) {
					list.add(new ContentProposal(proposal.getContent(), proposal.getLabel(), proposal.getDescription()));
				}
			}
			return list.toArray(new IContentProposal[list
					.size()]);
		}
		if (contentProposals == null) {
			contentProposals = new IContentProposal[proposals.length];
			for (int i = 0; i < proposals.length; i++) {
				contentProposals[i] = new ContentProposal(proposals[i].getContent(), proposals[i].getLabel(), proposals[i].getDescription());
			}
		}
		return contentProposals;
	}

	/**
	 * Set the Strings to be used as content proposals.
	 *
	 * @param items
	 *            the Strings to be used as proposals.
	 */
	public void setProposals(final IContentProposal... items) {
		this.proposals = items;
		contentProposals = null;
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
	public void setFiltering(final boolean filterProposals) {
		this.filterProposals = filterProposals;
		// Clear any cached proposals.
		contentProposals = null;
	}
}
