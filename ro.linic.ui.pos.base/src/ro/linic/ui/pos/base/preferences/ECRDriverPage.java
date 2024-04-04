package ro.linic.ui.pos.base.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.services.ECRDriver;

public class ECRDriverPage extends FieldEditorPreferencePage {

	public ECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(PreferenceKey.ECR_MODEL, Messages.ECRDriverPage_Driver, 
				new String[][]{{ECRDriver.ECR_MODEL_DATECS, ECRDriver.ECR_MODEL_DATECS},
					{ECRDriver.ECR_MODEL_PARTNER, ECRDriver.ECR_MODEL_PARTNER}},
				getFieldEditorParent()));
	}
}