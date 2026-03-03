package ro.linic.ui.base.dialogs;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.base.services.util.UIUtils;

public class SelectEntityDialog<T> extends Dialog {
	private static final int EDITABLE_TEXT_WIDTH = 100;
	
	private String title;
	private String description;

	private String entityLabel;
	private Combo entityCombo;
	private List<T> allEntities;
	private Function<T, String> nameMapper;
	private String[] buttonLabels;

	private Optional<T> selectedEntity = Optional.empty();

	public SelectEntityDialog(final Shell parent, final String title, final String description,
			final String entityLabel, final List<T> allEntities, final Function<T, String> nameMapper, final String... buttonLabels) {
		super(parent);
		this.title = title;
		this.description = description;
		this.entityLabel = entityLabel;
		this.allEntities = allEntities;
		this.nameMapper = nameMapper;
		this.buttonLabels = buttonLabels;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(title);

		final Label descriptionLabel = new Label(contents, SWT.NONE);
		descriptionLabel.setText(description);
		UIUtils.setFont(descriptionLabel);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(descriptionLabel);

		final Label entityLabel = new Label(contents, SWT.NONE);
		entityLabel.setText(this.entityLabel);
		UIUtils.setFont(entityLabel);

		entityCombo = new Combo(contents, SWT.DROP_DOWN);
		entityCombo.setItems(allEntities.stream().map(nameMapper).toArray(String[]::new));
		UIUtils.setFont(entityCombo);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(entityCombo);

		entityCombo.addModifyListener(e -> selectedEntity = entity());
		return contents;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		for (int i = 0; i < buttonLabels.length; i++)
			createButton(parent, i, buttonLabels[i], i == 0);
	}

	public Optional<T> selectedEntity() {
		return selectedEntity;
	}

	private Optional<T> entity() {
		final int index = entityCombo.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.ofNullable(allEntities.get(index));
	}
}
