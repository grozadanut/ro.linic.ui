package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.legacy.session.Messages;

public class PrintReceptiePage extends FieldEditorPreferencePage {

	public PrintReceptiePage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.RECEPTIE_GROUPBY_VAT_KEY, Messages.PrintReceptiePage_GroupByVAT, getFieldEditorParent()));
	}
}