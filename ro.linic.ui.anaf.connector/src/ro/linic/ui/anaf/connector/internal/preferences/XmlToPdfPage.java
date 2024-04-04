package ro.linic.ui.anaf.connector.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.anaf.connector.Messages;

public class XmlToPdfPage extends FieldEditorPreferencePage {

	public XmlToPdfPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.XML_TO_PDF_VALIDATE, Messages.XmlToPdfPage_ValidateDesc, getFieldEditorParent()));
	}
}