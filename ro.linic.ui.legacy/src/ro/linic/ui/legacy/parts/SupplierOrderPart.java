package ro.linic.ui.legacy.parts;

import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.legacy.tables.AllProductsNatTable.SourceLoc;

public class SupplierOrderPart
{
	public static final String PART_ID = "linic_gest_client.part.comenzi_furnizori"; //$NON-NLS-1$
	
	private static final String TABLE_STATE_PREFIX = "supplier_order.products_nt"; //$NON-NLS-1$
	
	private Combo searchMode;
	private Text searchFilter;
	private Text furnizorFilter;
	private AllProductsNatTable allProductsTable;
	private Button eliminateButton;
	private Button refreshButton;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		parent.setLayout(new GridLayout());
		createTopBar(parent);
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		searchMode = new Combo(container, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);
		
		searchFilter = new Text(container, SWT.BORDER);
		searchFilter.setMessage(Messages.BarcodeName);
		UIUtils.setFont(searchFilter);
		GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT).applyTo(searchFilter);
		
		furnizorFilter = new Text(container, SWT.BORDER);
		furnizorFilter.setMessage(Messages.SupplierOrderPart_Supplier);
		UIUtils.setFont(furnizorFilter);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(furnizorFilter);
		
		allProductsTable = new AllProductsNatTable(SourceLoc.COMENZI_FURNIZORI, bundle, log);
		allProductsTable.postConstruct(parent);
		allProductsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(allProductsTable.getTable());
		loadState(TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
		
		createBottomBar(parent);
		
		addListeners();
		loadData(false);
	}
	
	private void createBottomBar(final Composite parent)
	{
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(3, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		eliminateButton = new Button(footerContainer, SWT.PUSH);
		eliminateButton.setText(Messages.SupplierOrderPart_Eliminate);
		eliminateButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		eliminateButton.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(eliminateButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(eliminateButton);
		
		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText(Messages.Refresh);
		UIUtils.setBannerFont(refreshButton);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(TABLE_STATE_PREFIX, allProductsTable.getTable(), part);
	}
	
	private void addListeners()
	{
		searchMode.addModifyListener(this::filterModeChange);
		searchFilter.addModifyListener(e -> allProductsTable.filter(searchFilter.getText()));
		furnizorFilter.addModifyListener(e -> allProductsTable.filterFurnizori(furnizorFilter.getText()));
		
		eliminateButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (!allProductsTable.selection().isEmpty() && !MessageDialog.openQuestion(allProductsTable.getTable().getShell(),
						Messages.SupplierOrderPart_EliminateProd, Messages.SupplierOrderPart_EliminateProdMessage))
					return;
				
				allProductsTable.selection().forEach(p ->
				{
					final InvocationResult result = BusinessDelegate.eliminateProductFromOrdering(p.getId());
					showResult(result);
					if (result.statusOk())
						allProductsTable.remove(p);
				});
			}
		});
		
		refreshButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData(true);
			}
		});
	}
	
	private void loadData(final boolean showConfirmation)
	{
		BusinessDelegate.allProductsForOrdering(new AsyncLoadData<Product>()
		{
			@Override public void success(final ImmutableList<Product> data)
			{
				allProductsTable.loadData(data, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of());

				if (showConfirmation)
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.Success,
							Messages.SupplierOrderPart_SuccessMessage);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SupplierOrderPart_ErrorLoading,
						details);
			}
		}, sync, bundle, log);
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		allProductsTable.filterMode(filterMode);
		allProductsTable.filter(searchFilter.getText());
	}
}
