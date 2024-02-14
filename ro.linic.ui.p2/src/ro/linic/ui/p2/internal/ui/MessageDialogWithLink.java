package ro.linic.ui.p2.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class MessageDialogWithLink extends MessageDialog {

	protected String linkMessage;
	protected Link link;
	protected List<SelectionListener> linkListeners = new ArrayList<>();

	public MessageDialogWithLink(final Shell parentShell, final String dialogTitle, final Image dialogTitleImage, final String dialogMessage, final int dialogImageType, final String[] dialogButtonLabels, final int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		this.message = null;
		this.linkMessage = dialogMessage;
	}

	public MessageDialogWithLink(final Shell parentShell, final String dialogTitle, final Image dialogTitleImage, final String dialogMessage, final int dialogImageType, final int defaultIndex, final String... dialogButtonLabels) {
		this(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
	}

	@Override
	protected Control createMessageArea(final Composite composite) {
		super.createMessageArea(composite);
		// create message
		if (linkMessage != null) {
			this.link = new Link(composite, getMessageLabelStyle());
			this.link.setText(this.linkMessage);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT).applyTo(this.link);
			for (final SelectionListener linkListener : this.linkListeners) {
				this.link.addSelectionListener(linkListener);
			}
		}
		return composite;
	}

	public void addSelectionListener(final SelectionListener listener) {
		if (link != null && !link.isDisposed()) {
			link.addSelectionListener(listener);
		}
		this.linkListeners.add(listener);
	}

	public void removeSelectionListener(final SelectionListener listener) {
		if (link != null && !link.isDisposed()) {
			link.removeSelectionListener(listener);
		}
		this.linkListeners.add(listener);
	}

}
