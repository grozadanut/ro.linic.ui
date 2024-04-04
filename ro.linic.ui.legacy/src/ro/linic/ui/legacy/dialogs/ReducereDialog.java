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

import ro.colibri.entities.comercial.Reducere;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.ReducereNatTable;

public class ReducereDialog extends Dialog
{
	private Button adauga;
	private Button sterge;
	
	private ReducereNatTable table;
	
	public ReducereDialog(final Shell parent)
	{
		super(parent);
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.ReducereDialog_Title);
		
		adauga = new Button(contents, SWT.PUSH);
		adauga.setText(Messages.Add);
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adauga);
		UIUtils.setBoldBannerFont(adauga);
		
		sterge = new Button(contents, SWT.PUSH);
		sterge.setText(Messages.Delete);
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sterge);
		UIUtils.setBoldBannerFont(sterge);
		
		table = new ReducereNatTable();
		table.postConstruct(contents);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).minSize(800, 400).span(2, 1).applyTo(table.getTable());
		
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
				adaugaReducere();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				table.selection().forEach(table::remove);
			}
		});
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, Messages.Save, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed()
	{
		final ImmutableList<Reducere> newReduceri = table.getSourceData().stream()
				.collect(toImmutableList());
		final InvocationResult result = BusinessDelegate.updateReduceri(newReduceri);
		showResult(result);
		if (result.statusOk())
			super.okPressed();
	}
	
	private void loadData()
	{
		table.loadData(BusinessDelegate.reduceri_Sync());
	}
	
	private void adaugaReducere()
	{
		final Reducere newReducere = new Reducere();
		table.add(newReducere);
	}
}