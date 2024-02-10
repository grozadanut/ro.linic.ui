package ro.linic.ui.legacy.widgets;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.linic.ui.legacy.session.UIUtils.localToDisplayLocation;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public abstract class ExtraFilterPopup extends PopupDialog
{
	private Button openButton;
	private Point initialLocation;
	private Point initialSize;
	
	public ExtraFilterPopup(final Shell shell, final Button openButton)
	{
		super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
		this.openButton = openButton;
		init();
	}
	
	private void init()
	{
		openButton.setText("Extra");
		
		openButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				setInitialLocation(localToDisplayLocation(openButton, 0, openButton.getSize().y));
				open();
			}
		});
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout());
		createWidgetArea(container);
		addListeners();
		loadState();
		return container;
	}
	
	protected abstract void createWidgetArea(final Composite parent);
	protected abstract void addListeners();
	protected abstract void loadState();
	
	final protected void markDirty(final boolean dirty)
	{
		openButton.setText(MessageFormat.format("Extra{0}", dirty ? "*" : EMPTY_STRING));
	}
	
	public void setInitialLocation(final Point initialLocation)
	{
		this.initialLocation = initialLocation;
	}
	
	public void setInitialSize(final Point initialSize)
	{
		this.initialSize = initialSize;
	}
	
	@Override
	final protected Point getDefaultLocation(final Point initialSize)
	{
		if (this.initialLocation != null)
			return initialLocation;
		
		return super.getDefaultLocation(initialSize);
	}
	
	@Override
	final protected Point getDefaultSize()
	{
		if (this.initialSize != null)
			return initialSize;

		return super.getDefaultSize();
	}
	
	@Override
	public boolean close()
	{
		saveState();
		return super.close();
	}

	protected abstract void saveState();
}