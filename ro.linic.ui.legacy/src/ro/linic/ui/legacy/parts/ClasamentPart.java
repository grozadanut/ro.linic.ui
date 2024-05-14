package ro.linic.ui.legacy.parts;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.wrappers.ClasamentEntry;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.ClasamentNatTable;

public class ClasamentPart
{
	public static final String PART_ID = "linic_gest_client.part.clasament";
	
	private static final String TABLE_STATE_PREFIX = "clasament.clasament_nt";
	
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button execute;
	private ClasamentNatTable table;
	private Job loadJob;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject private Logger log;
	@Inject @OSGiBundle private Bundle bundle;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		parent.setLayout(new GridLayout());
		createTopBar(parent);
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(5, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText("Maxim");
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText("ZiCrt");
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText("LunaCrt");
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText("AnCrt");
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		
		execute = new Button(container, SWT.PUSH | SWT.WRAP);
		execute.setText("Executa filtrarea");
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);
		
		table = new ClasamentNatTable(bundle, log);
		table.postConstruct(container);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(5, 1).applyTo(table.getTable());
		loadState(TABLE_STATE_PREFIX, table.getTable(), part);
		
		loadData();
		addListeners();
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(TABLE_STATE_PREFIX, table.getTable(), part);
	}
	
	private void addListeners()
	{
		execute.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData();
			}
		});
		
		maxim.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
			}
		});
		
		ziCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
			}
		});
		
		lunaCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
			}
		});
		
		anCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
			}
		});
	}
	
	private void loadData()
	{
		if (loadJob != null)
			loadJob.cancel();
		
		loadJob = BusinessDelegate.clasament(new AsyncLoadData<ClasamentEntry>()
		{
			@Override public void success(final ImmutableList<ClasamentEntry> data)
			{
				table.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcare", details);
			}
		}, sync, log, extractLocalDate(from), extractLocalDate(to));
	}
}
