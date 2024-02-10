package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.colibri.entities.comercial.Gestiune;
import ro.linic.ui.legacy.session.UIUtils;

public class GestiuneDialog extends TitleAreaDialog
{
	private Text name;
	private Text importName;
	
	private Gestiune gestiune;
	
	private Supplier<Boolean> okPressed;

	public GestiuneDialog(final Shell parent, final Gestiune gestiune)
	{
		super(parent);
		this.gestiune = gestiune;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle("Gestiune");
		
		final Label nameLabel = new Label(contents, SWT.NONE);
		nameLabel.setText("Nume");
		UIUtils.setFont(nameLabel);
		
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(name);
		
		final Label importNameLabel = new Label(contents, SWT.NONE);
		importNameLabel.setText("Nume scurt");
		UIUtils.setFont(importNameLabel);
		
		importName = new Text(contents, SWT.SINGLE | SWT.BORDER);
		importName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(importName);
		
		fillFields();
		return contents;
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(name.getText()))
		{
			setErrorMessage("Numele este obligatoriu!");
			return;
		}
		
		if (isEmpty(importName.getText()))
		{
			setErrorMessage("Numele scurt este obligatoriu!");
			return;
		}
		
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		name.setText(safeString(gestiune, Gestiune::getName));
		importName.setText(safeString(gestiune, Gestiune::getImportName));
	}
	
	private void fillGestiune()
	{
		gestiune.setName(name.getText());
		gestiune.setImportName(importName.getText());
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public Gestiune filledGestiune()
	{
		fillGestiune();
		return gestiune;
	}
}
