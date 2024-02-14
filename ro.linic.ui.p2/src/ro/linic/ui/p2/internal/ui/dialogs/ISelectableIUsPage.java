package ro.linic.ui.p2.internal.ui.dialogs;

import org.eclipse.jface.wizard.IWizardPage;

/**
 *
 * ISelectableIUsPage is used to get the selected or checked IUs in a
 * wizard page.
 *
 * @since 3.5
 *
 */
public interface ISelectableIUsPage extends IWizardPage {
	public Object[] getCheckedIUElements();

	public Object[] getSelectedIUElements();

	public void setCheckedElements(Object[] elements);
}
