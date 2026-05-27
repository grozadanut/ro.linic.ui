package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import ro.linic.ui.legacy.preferences.PreferenceKey.SalesPartType;
import ro.linic.ui.legacy.session.Messages;

public class SalesPage extends FieldEditorPreferencePage {

	public SalesPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(PreferenceKey.VANZARE_PART_TYPE_KEY, Messages.SalesPage_PartType, 
				new String[][]{{SalesPartType.CAFE.displayName(), SalesPartType.CAFE.name()},
					{SalesPartType.STANDARD.displayName(), SalesPartType.STANDARD.name()},
					{SalesPartType.BETA.displayName(), SalesPartType.BETA.name()}},
				getFieldEditorParent()));
	}
}