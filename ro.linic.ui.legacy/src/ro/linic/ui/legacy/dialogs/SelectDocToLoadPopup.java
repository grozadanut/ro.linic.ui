package ro.linic.ui.legacy.dialogs;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.LoadBonuri;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.tables.IncarcaDocNatTable;

public class SelectDocToLoadPopup extends PopupDialog implements IMouseAction
{
	private Point initialLocation;
	
	private IncarcaDocNatTable natTable;
	
	private TipDoc tipDoc;
	private LocalDate from;
	private LocalDate to;
	private LoadBonuri incarcaBonuri;
	
	private UISynchronize sync;
	private Logger log;
	private Consumer<AccountingDocument> updateAccDoc;
	
	private Job loadJob;
	
	public SelectDocToLoadPopup(final Shell shell, final Point initialLocation, final UISynchronize sync, final TipDoc tipDoc, 
			final LocalDate from, final LocalDate to, final LoadBonuri incarcaBonuri, final Consumer<AccountingDocument> updateAccDoc, final Logger log)
	{
		super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
		this.initialLocation = initialLocation;
		this.tipDoc = tipDoc;
		this.from = from;
		this.to = to;
		this.incarcaBonuri = incarcaBonuri;
		this.sync = sync;
		this.updateAccDoc = updateAccDoc;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		natTable = new IncarcaDocNatTable();
		natTable.doubleClickAction(this);
		natTable.postConstruct(container);
		natTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(natTable.getTable());
		loadData();
		return container;
	}
	
	@Override
	public void run(final NatTable table, final MouseEvent event)
	{
		// double click
		updateAccDoc.accept(BusinessDelegate.reloadDoc(natTable.selection()));
		close();
	}
	
	@Override
	public boolean close()
	{
		if (loadJob != null)
			loadJob.cancel();
			
		return super.close();
	}
	
	private void loadData()
	{
		loadJob = BusinessDelegate.filteredDocs(new AsyncLoadData<AccountingDocument>()
		{
			@Override public void success(final ImmutableList<AccountingDocument> data)
			{
				natTable.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(getShell(), Messages.SelectDocToLoadPopup_Error, details);
			}
		}, sync, tipDoc, from, to, incarcaBonuri, log);
	}
	
	@Override
	protected Point getDefaultLocation(final Point initialSize)
	{
		if (this.initialLocation != null)
			return initialLocation;
		
		return super.getDefaultLocation(initialSize);
	}
	
	@Override
	protected Point getDefaultSize()
	{
		return new Point(850, 600);
	}
	
	@Override
	protected Color getBackground()
	{
		return Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
	}
	
	@Override
	protected List<Control> getBackgroundColorExclusions()
	{
		final List<Control> list = super.getBackgroundColorExclusions();
		list.add(natTable.getTable());
		return list;
	}
}
