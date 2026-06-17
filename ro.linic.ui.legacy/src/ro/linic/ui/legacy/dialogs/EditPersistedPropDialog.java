package ro.linic.ui.legacy.dialogs;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.ListUtils;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class EditPersistedPropDialog extends TitleAreaDialog {
	private Map<String, PersistedProp> props;
	private Map<PersistedProp, Text> widgets;
	private String message;

	public EditPersistedPropDialog(final Shell parent, final Map<String, PersistedProp> props, final String message) {
		super(parent);
		this.props = props;
		this.message = message;
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle("Editeaza");
		setMessage(message);
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		widgets = props.entrySet().stream().collect(Collectors.toMap(Entry::getValue,
				e -> {
					new Label(container, SWT.NONE).setText(e.getKey());
					final Text field = new Text(container, SWT.BORDER);
					UIUtils.setFont(field);
					GridDataFactory.fillDefaults().grab(true, false).applyTo(field);
					return field;
				}));

		return area;
	}

	@Override
	protected void okPressed() {
		BusinessDelegate.mergePersistedProps(widgets.entrySet().stream()
				.map(e -> e.getKey().setValue(e.getValue().getText()))
				.collect(ListUtils.toImmutableSet()));
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 600);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}
}
