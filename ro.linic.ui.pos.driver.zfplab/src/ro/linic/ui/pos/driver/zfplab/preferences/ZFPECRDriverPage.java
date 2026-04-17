package ro.linic.ui.pos.driver.zfplab.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import ro.linic.ui.pos.driver.zfplab.Messages;

public class ZFPECRDriverPage extends FieldEditorPreferencePage {

	public ZFPECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceKey.SERVER_ADDRESS, Messages.ZFPECRDriverPage_ServerAddress, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.ECR_IP, Messages.ZFPECRDriverPage_Ip, getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceKey.ECR_PORT, Messages.ZFPECRDriverPage_Port, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.ECR_PASSWORD, Messages.ZFPECRDriverPage_Password, getFieldEditorParent()));
	}
}