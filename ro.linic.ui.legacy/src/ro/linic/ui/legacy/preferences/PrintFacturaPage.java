package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.legacy.session.Messages;

public class PrintFacturaPage extends FieldEditorPreferencePage {

	public PrintFacturaPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.FACTURA_PRINT_CONFORMITATE_KEY, Messages.PrintFacturaPage_Conformitate, getFieldEditorParent()));
	}
}