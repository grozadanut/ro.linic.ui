package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.legacy.session.Messages;

public class RegisterZPage extends FieldEditorPreferencePage {

	public RegisterZPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(PreferenceKey.REGISTER_Z_DIALOG_KEY, Messages.RegisterZPage_Message, 
				new String[][]{{Messages.RegisterZPage_Standard, PreferenceKey.REGISTER_Z_DIALOG_STANDARD_VALUE},
					{Messages.RegisterZPage_Cafe, PreferenceKey.REGISTER_Z_DIALOG_CAFE_VALUE}},
				getFieldEditorParent()));
	}
}