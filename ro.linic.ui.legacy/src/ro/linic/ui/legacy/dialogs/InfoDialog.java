package ro.linic.ui.legacy.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.legacy.session.UIUtils;

public class InfoDialog extends Dialog
{
	private String title;
	private String description;
	
	private Text info;
	
	public static int open(final Shell parent, final String title, final String description)
	{
		return new InfoDialog(parent, title, description).open();
	}
	
	public InfoDialog(final Shell parent, final String title, final String description)
	{
		super(parent);
		this.title = title;
		this.description = description;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout());
		getShell().setText(title);
		
		info = new Text(contents, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		info.setText(description);
		UIUtils.setFont(info);
		GridDataFactory.fillDefaults().grab(true, true).hint(800, 600).applyTo(info);
		
		return contents;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
}
