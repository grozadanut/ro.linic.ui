package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.CasaDepartment;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.CasaDeptNatTable;

public class CasaDeptDialog extends Dialog
{
	private Button adauga;
	private Button sterge;
	
	private CasaDeptNatTable deptTable;
	
	public CasaDeptDialog(final Shell parent)
	{
		super(parent);
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText("Departamente Casa");
		
		adauga = new Button(contents, SWT.PUSH);
		adauga.setText("Adauga");
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adauga);
		UIUtils.setBoldBannerFont(adauga);
		
		sterge = new Button(contents, SWT.PUSH);
		sterge.setText("Sterge");
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sterge);
		UIUtils.setBoldBannerFont(sterge);
		
		deptTable = new CasaDeptNatTable();
		deptTable.postConstruct(contents);
		deptTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).minSize(450, 300).span(2, 1).applyTo(deptTable.getTable());
		
		contents.setTabList(new Control[]{adauga});

		addListeners();
		loadData();
		
		return contents;
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	private void addListeners()
	{
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				adaugaDept();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				deptTable.selection().forEach(deptTable::remove);
			}
		});
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, "Save", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed()
	{
		final ImmutableList<CasaDepartment> newDepts = deptTable.getSourceData().stream()
				.collect(toImmutableList());
		final InvocationResult result = BusinessDelegate.updateCasaDepts(newDepts);
		showResult(result);
		if (result.statusOk())
			super.okPressed();
	}
	
	private void loadData()
	{
		deptTable.loadData(BusinessDelegate.casaDepts_Sync());
	}
	
	private void adaugaDept()
	{
		final CasaDepartment newDepartment = new CasaDepartment();
		newDepartment.setName("1");
		deptTable.add(newDepartment);
	}
}