package ro.linic.ui.legacy.dialogs.components;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.replaceIfEmpty;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.fieldassist.IContentProposal;

import ro.colibri.wrappers.ThreeEntityWrapper;

public class UsedAddressContentProposal implements IContentProposal
{
	public static final String ENTITIES_SEPARATOR = "\036"; // ASCII 036 RS - Record Separator
	public static final int ADDRESS_INDEX = 0;
	public static final int PHONE_INDEX = 1;
	public static final int INDICATII_INDEX = 2;
	
	private String content = EMPTY_STRING;
	private String label = EMPTY_STRING;
	private String description = EMPTY_STRING;
	private int cursorPosition = 0;
	
	/**
	 * Create a content proposal whose content, label, description, and cursor
	 * position are as specified in the parameters.
	 *
	 * @exception IllegalArgumentException
	 *                if the index is not between 0 and the number of characters
	 *                in the content.
	 */
	public UsedAddressContentProposal(final ThreeEntityWrapper<String> proposal)
	{
		final String content = MessageFormat.format("{1}{0}{2}{0}{3}", ENTITIES_SEPARATOR,
				replaceIfEmpty(proposal.getEntity1(), SPACE),
				replaceIfEmpty(proposal.getEntity2(), SPACE),
				replaceIfEmpty(proposal.getEntity3(), SPACE));
		final String label = content.replaceAll(ENTITIES_SEPARATOR, "  |  ");
		final int cursorPosition = content.length();
		
		Assert.isLegal(cursorPosition >= 0 && cursorPosition <= content.length());
		this.content = content;
		this.label = label;
		this.description = null;
		this.cursorPosition = cursorPosition;
	}
	
	/**
	 * @param entityIndex one of ADDRESS_INDEX, PHONE_INDEX, INDICATII_INDEX
	 * @return
	 */
	public String extractTextFromContent(final int entityIndex)
	{
		final String extracted = safeString(content, c -> c.split(UsedAddressContentProposal.ENTITIES_SEPARATOR)[entityIndex]);
		return extracted.equals(SPACE) ? EMPTY_STRING : extracted;
	}

	@Override
	public String getContent()
	{
		return content;
	}

	@Override
	public int getCursorPosition()
	{
		return cursorPosition;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getLabel()
	{
		return label;
	}
}