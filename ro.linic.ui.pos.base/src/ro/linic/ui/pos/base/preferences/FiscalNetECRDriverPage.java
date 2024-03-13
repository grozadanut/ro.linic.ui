package ro.linic.ui.pos.base.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

public class FiscalNetECRDriverPage extends FieldEditorPreferencePage {

	public FiscalNetECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceKey.FISCAL_NET_COMMAND_FOLDER, "Folder comenzi", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceKey.FISCAL_NET_RESPONSE_FOLDER, "Folder raspuns", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.FISCAL_NET_TAX_CODE, "Cota TVA", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.FISCAL_NET_DEPT, "Departament", getFieldEditorParent()));
	}
}