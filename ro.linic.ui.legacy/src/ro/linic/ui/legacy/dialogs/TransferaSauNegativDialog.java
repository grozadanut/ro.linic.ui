package ro.linic.ui.legacy.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAttributeAdapter;
import org.eclipse.swt.accessibility.AccessibleAttributeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TransferaSauNegativDialog extends Dialog
{
	public static final int INTRA_NEGATIV_ID = IDialogConstants.OK_ID;
	public static final int TRANSFERA_ID = IDialogConstants.PROCEED_ID;
	public static final int TRANSFERA_TOT_ID = IDialogConstants.FINISH_ID;
	
	private boolean showTransferaButton;
	private boolean showTransferaTotButton;
	private String title;
	private String message;
	
	public TransferaSauNegativDialog(final Shell parent, final boolean showTransferaButton, final boolean showTransferaTotButton, final String title, final String message)
	{
		super(parent);
		this.showTransferaButton = showTransferaButton;
		this.showTransferaTotButton = showTransferaTotButton;
		this.title = title;
		this.message = message;
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		getShell().setText(title);
		final Text messageLabel = new Text(contents, SWT.WRAP | SWT.READ_ONLY);
		messageLabel.setText(message); // two lines//$NON-NLS-1$
		messageLabel.setFont(JFaceResources.getDialogFont());
		// Bug 248410 -  This snippet will only work with Windows screen readers.
		messageLabel.getAccessible().addAccessibleAttributeListener(new AccessibleAttributeAdapter()
		{
			@Override public void getAttributes(final AccessibleAttributeEvent e)
			{
				e.attributes = new String[] { "container-live", //$NON-NLS-1$
						"polite", "live", "polite", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						"container-live-role", "status", }; //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		return contents;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		if (showTransferaTotButton)
			createButton(parent, TRANSFERA_TOT_ID, Messages.TransferaSauNegativDialog_TransferAll, false);
		if (showTransferaButton)
			createButton(parent, TRANSFERA_ID, Messages.TransferaSauNegativDialog_TransferDifference, false);
		
		createButton(parent, INTRA_NEGATIV_ID, Messages.TransferaSauNegativDialog_DontTransfer, true).setFocus();
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(final int buttonId)
	{
		super.buttonPressed(buttonId);
		
		if (TRANSFERA_ID == buttonId)
		{
			setReturnCode(TRANSFERA_ID);
			close();
		}
		if (TRANSFERA_TOT_ID == buttonId)
		{
			setReturnCode(TRANSFERA_TOT_ID);
			close();
		}
	}
}
