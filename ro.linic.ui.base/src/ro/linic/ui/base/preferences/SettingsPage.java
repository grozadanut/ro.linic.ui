package ro.linic.ui.base.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;

import ro.linic.ui.base.Messages;
import ro.linic.ui.base.services.preferences.PreferenceKey;

public class SettingsPage extends FieldEditorPreferencePage {

	public SettingsPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceKey.SERVER_BASE_URL, Messages.ServerUrl, getFieldEditorParent()));
	}
}