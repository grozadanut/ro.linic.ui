package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

/** A sample preference page to show how it works */
public class RegisterZPage extends FieldEditorPreferencePage {

	public RegisterZPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.RAPORT_Z_AND_D, "Scoate si raportul D la inchiderea zilei", getFieldEditorParent()));
	}
}