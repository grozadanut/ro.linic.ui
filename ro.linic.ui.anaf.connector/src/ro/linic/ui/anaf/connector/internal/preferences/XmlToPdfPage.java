package ro.linic.ui.anaf.connector.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

public class XmlToPdfPage extends FieldEditorPreferencePage {

	public XmlToPdfPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.XML_TO_PDF_VALIDATE, "Valideaza fisierul XML inainte de a-l converti in PDF", getFieldEditorParent()));
	}
}