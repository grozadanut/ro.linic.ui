package ro.linic.ui.pos.base.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

import ro.linic.ui.pos.base.Messages;

public class DudeECRDriverPage extends FieldEditorPreferencePage {

	public DudeECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceKey.DUDE_ECR_FOLDER, Messages.DudeECRDriverPage_Folder, getFieldEditorParent()));
		final StringFieldEditor ipFieldEditor = new StringFieldEditor(PreferenceKey.DUDE_ECR_IP, Messages.DudeECRDriverPage_IP, getFieldEditorParent());
		ipFieldEditor.setEmptyStringAllowed(false);
		addField(ipFieldEditor);
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_PORT, Messages.DudeECRDriverPage_Port, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_OPERATOR, Messages.DudeECRDriverPage_Operator, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_PASSWORD, Messages.DudeECRDriverPage_Password, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_NR_AMEF, Messages.DudeECRDriverPage_AMEFNumber, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_TAX_CODE, Messages.DudeECRDriverPage_VATRate, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_DEPT, Messages.DudeECRDriverPage_Department, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceKey.DUDE_REPORT_Z_AND_D, Messages.DudeECRDriverPage_ZAndD, getFieldEditorParent()));
	}
}