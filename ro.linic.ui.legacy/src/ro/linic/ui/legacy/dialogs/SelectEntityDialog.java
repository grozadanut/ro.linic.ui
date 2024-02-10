package ro.linic.ui.legacy.dialogs;

import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.colibri.base.IPresentable;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.wizards.InchideBonWizard;

public class SelectEntityDialog<T extends IPresentable> extends Dialog
{
	private String titlu;
	private String descriere;
	
	private String entityLabel;
	private Combo entityCombo;
	private ImmutableList<T> allEntities;
	private String[] buttonLabels;
	
	private Optional<T> selectedEntity = Optional.empty();
	
	public SelectEntityDialog(final Shell parent, final String titlu, final String descriere,
			final String entityLabel, final ImmutableList<T> allEntities, final String... buttonLabels)
	{
		super(parent);
		this.titlu = titlu;
		this.descriere = descriere;
		this.entityLabel = entityLabel;
		this.allEntities = allEntities;
		this.buttonLabels = buttonLabels;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(titlu);
		
		final Label descriptionLabel = new Label(contents, SWT.NONE);
		descriptionLabel.setText(descriere);
		UIUtils.setFont(descriptionLabel);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(descriptionLabel);
		
		final Label entityLabel = new Label(contents, SWT.NONE);
		entityLabel.setText(this.entityLabel);
		UIUtils.setFont(entityLabel);
		
		entityCombo = new Combo(contents, SWT.DROP_DOWN);
		entityCombo.setItems(allEntities.stream().map(IPresentable::displayName).toArray(String[]::new));
		UIUtils.setFont(entityCombo);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(entityCombo);
		
		entityCombo.addModifyListener(e -> selectedEntity = entity());
		return contents;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
        for (int i = 0; i < buttonLabels.length; i++)
			createButton(parent, i, buttonLabels[i], i == 0);
    }
	
	public Optional<T> selectedEntity()
	{
		return selectedEntity;
	}
	
	private Optional<T> entity()
	{
		final int index = entityCombo.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allEntities.get(index));
	}
}
