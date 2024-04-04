package ro.linic.ui.pos.base.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

import ro.linic.ui.pos.base.Messages;

public class FiscalNetECRDriverPage extends FieldEditorPreferencePage {

	public FiscalNetECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceKey.FISCAL_NET_COMMAND_FOLDER, Messages.FiscalNetECRDriverPage_CommandFolder, getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceKey.FISCAL_NET_RESPONSE_FOLDER, Messages.FiscalNetECRDriverPage_ResponseFolder, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.FISCAL_NET_TAX_CODE, Messages.FiscalNetECRDriverPage_VATRate, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.FISCAL_NET_DEPT, Messages.FiscalNetECRDriverPage_Department, getFieldEditorParent()));
	}
}