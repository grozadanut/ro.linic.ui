package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

public class RegisterZPage extends FieldEditorPreferencePage {

	public RegisterZPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(PreferenceKey.REGISTER_Z_DIALOG_KEY, "Ce fereastra se foloseste la inregistrarea Z", 
				new String[][]{{"Standard", PreferenceKey.REGISTER_Z_DIALOG_STANDARD_VALUE},
					{"Cafe", PreferenceKey.REGISTER_Z_DIALOG_CAFE_VALUE}},
				getFieldEditorParent()));
	}
}