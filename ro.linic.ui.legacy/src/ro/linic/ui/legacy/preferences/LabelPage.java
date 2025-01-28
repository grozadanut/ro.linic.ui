package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.legacy.session.Messages;

public class LabelPage extends FieldEditorPreferencePage {

	public LabelPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceKey.BROTHER_PRINT_FOLDER, Messages.BrotherPrintFolder, getFieldEditorParent()));
	}
}