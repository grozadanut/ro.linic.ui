package ro.linic.ui.pos.base.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

public class DudeECRDriverPage extends FieldEditorPreferencePage {

	public DudeECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceKey.DUDE_ECR_FOLDER, "Folder comenzi", getFieldEditorParent()));
		final StringFieldEditor ipFieldEditor = new StringFieldEditor(PreferenceKey.DUDE_ECR_IP, "IP", getFieldEditorParent());
		ipFieldEditor.setEmptyStringAllowed(false);
		addField(ipFieldEditor);
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_PORT, "Port", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_OPERATOR, "Operator", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_PASSWORD, "Parola", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_NR_AMEF, "Nr AMEF", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_TAX_CODE, "Cota TVA", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.DUDE_ECR_DEPT, "Departament", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceKey.DUDE_REPORT_Z_AND_D, "Scoate si raportul D la inchiderea zilei", getFieldEditorParent()));
	}
}