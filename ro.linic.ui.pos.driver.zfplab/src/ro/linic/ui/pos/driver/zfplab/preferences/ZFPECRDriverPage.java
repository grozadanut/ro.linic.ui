package ro.linic.ui.pos.driver.zfplab.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;

import ro.linic.ui.base.widgets.DoubleFieldEditor;
import ro.linic.ui.pos.driver.zfplab.Messages;

public class ZFPECRDriverPage extends FieldEditorPreferencePage {

	public ZFPECRDriverPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceKey.SERVER_ADDRESS, Messages.ZFPECRDriverPage_ServerAddress, getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(PreferenceKey.ECR_LAN_CONNECT, Messages.ECRDriverPage_LanConnect, getFieldEditorParent()));

		final Group lanGroup = new Group(getFieldEditorParent(), SWT.NULL);
		lanGroup.setText("LAN");
		lanGroup.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().span(2, 1).applyTo(lanGroup);
		addField(new StringFieldEditor(PreferenceKey.ECR_IP, Messages.ZFPECRDriverPage_Ip, lanGroup));
		addField(new IntegerFieldEditor(PreferenceKey.ECR_PORT, Messages.ZFPECRDriverPage_Port, lanGroup));
		addField(new StringFieldEditor(PreferenceKey.ECR_PASSWORD, Messages.ZFPECRDriverPage_Password, lanGroup));
		
		final Group comGroup = new Group(getFieldEditorParent(), SWT.NULL);
		comGroup.setText("COM");
		comGroup.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().span(2, 1).applyTo(comGroup);
		addField(new StringFieldEditor(PreferenceKey.ECR_COM_PORT, Messages.ZFPECRDriverPage_ComPort, comGroup));
		addField(new StringFieldEditor(PreferenceKey.ECR_BAUD_RATE, Messages.ZFPECRDriverPage_BaudRate, comGroup));
		
		addField(new DoubleFieldEditor(PreferenceKey.OPERATOR, Messages.ZFPECRDriverPage_Operator, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.OPERATOR_PASSWORD, Messages.ZFPECRDriverPage_OperatorPassword, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceKey.ECR_DEPT, Messages.ZFPECRDriverPage_Department, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceKey.REPORT_Z_AND_D, Messages.ECRDriverPage_ZAndD, getFieldEditorParent()));
	}
}