package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.UICategoriesNatTable;

public class CategoriiUIDialog extends Dialog
{
	private Button adauga;
	private Button sterge;
	
	private UICategoriesNatTable uiCategoriesTable;
	
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;

	public CategoriiUIDialog(final Shell parent, final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		super(parent);
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText("Categorii Vizuale");
		
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
		
		uiCategoriesTable = new UICategoriesNatTable(bundle, log);
		uiCategoriesTable.postConstruct(contents);
		uiCategoriesTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).minSize(650, 300).span(2, 1).applyTo(uiCategoriesTable.getTable());
		
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
				adaugaCategorie();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				uiCategoriesTable.selection().forEach(uiCategoriesTable::remove);
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
		final ImmutableList<ProductUiCategory> newCategories = uiCategoriesTable.getSourceData().stream()
				.collect(toImmutableList());
		final InvocationResult result = BusinessDelegate.updateCategories(newCategories);
		showResult(result);
		if (result.statusOk())
			super.okPressed();
	}
	
	private void loadData()
	{
		BusinessDelegate.uiCategories(new AsyncLoadData<ProductUiCategory>()
		{
			@Override public void success(final ImmutableList<ProductUiCategory> data)
			{
				uiCategoriesTable.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea categoriilor vizuale", details);
			}
		}, sync, false, true, bundle, log);
	}
	
	private void adaugaCategorie()
	{
		final ProductUiCategory newUiCat = new ProductUiCategory();
		uiCategoriesTable.add(newUiCat);
	}
}