package ro.linic.ui.legacy.dialogs;

import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.RulajePartenerNatTable;

public class PuncteFidelitateDialog extends Dialog
{
	private Text partnerFilter;
	private RulajePartenerNatTable table;
	
	private UISynchronize sync;
	
	public PuncteFidelitateDialog(final Shell parent, final UISynchronize sync)
	{
		super(parent);
		this.sync = sync;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.PuncteFidelitateDialog_Title);
		
		final Label partnerFilterLabel = new Label(contents, SWT.NONE);
		partnerFilterLabel.setText(Messages.PuncteFidelitateDialog_Filter);
		UIUtils.setFont(partnerFilterLabel);
		
		partnerFilter = new Text(contents, SWT.SINGLE | SWT.BORDER);
		partnerFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(partnerFilter);
		
		table = new RulajePartenerNatTable();
		table.postConstruct(contents);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(table.getTable());
		
		addListeners();
		loadData();
		return contents;
	}
	
	private void addListeners()
	{
		partnerFilter.addModifyListener(e -> table.filter(partnerFilter.getText()));
	}
	
	private void loadData()
	{
		BusinessDelegate.puncteFidelitate(new AsyncLoadData<RulajPartener>()
		{
			@Override public void success(final ImmutableList<RulajPartener> data)
			{
				table.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.PuncteFidelitateDialog_Error, details);
			}
		}, sync);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(900, 500);
	}
}
