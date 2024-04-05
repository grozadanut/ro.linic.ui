package ro.linic.ui.legacy.parts;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setFont;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.user.AuditEvent;
import ro.colibri.entities.user.AuditEvent.AuditEventType;
import ro.colibri.entities.user.User;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AuditEventsNatTable;

public class AuditEventsPart
{
	public static final String PART_ID = "linic_gest_client.part.audit_events"; //$NON-NLS-1$
	
	private static final String TABLE_STATE_PREFIX = "audit_events.events_nt"; //$NON-NLS-1$
	
	private Combo type;
	private Combo user;
	private Combo gestiune;
	private Text descriere;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button execute;
	private AuditEventsNatTable auditEventsTable;
	
	private ImmutableList<AuditEventType> allTypes;
	private ImmutableList<User> allUsers;
	private ImmutableList<Gestiune> allGestiuni;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		this.allTypes = ImmutableList.copyOf(AuditEventType.values());
		this.allGestiuni = BusinessDelegate.allGestiuni();
		this.allUsers = BusinessDelegate.dbUsers();
		
		parent.setLayout(new GridLayout());
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(9, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		setFont(new Label(container, SWT.NONE)).setText(Messages.AuditEventsPart_Type);//layout
		setFont(new Label(container, SWT.NONE)).setText(Messages.AuditEventsPart_User);//layout
		new Label(container, SWT.NONE);//layout
		new Label(container, SWT.NONE);//layout
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText(Messages.Max);
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText(Messages.Today);
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText(Messages.ThisMonth);
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText(Messages.ThisYear);
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		new Label(container, SWT.NONE);//layout
		
		type = new Combo(container, SWT.DROP_DOWN);
		type.setItems(allTypes.stream().map(AuditEventType::displayName).toArray(String[]::new));
		UIUtils.setFont(type);
		GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).applyTo(type);
		
		user = new Combo(container, SWT.DROP_DOWN);
		user.setItems(allUsers.stream().map(User::displayName).toArray(String[]::new));
		UIUtils.setFont(user);
		GridDataFactory.swtDefaults().hint(120, SWT.DEFAULT).applyTo(user);
		
		gestiune = new Combo(container, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getImportName).toArray(String[]::new));
		UIUtils.setFont(gestiune);
		
		descriere = new Text(container, SWT.BORDER);
		descriere.setMessage(Messages.AuditEventsPart_Description);
		UIUtils.setFont(descriere);
		GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).applyTo(descriere);
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		
		execute = new Button(container, SWT.PUSH | SWT.WRAP);
		execute.setText(Messages.Execute);
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);
		
		auditEventsTable = new AuditEventsNatTable();
		auditEventsTable.postConstruct(parent);
		auditEventsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(8, 1).applyTo(auditEventsTable.getTable());
		loadState(TABLE_STATE_PREFIX, auditEventsTable.getTable(), part);
		
		addListeners();
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(TABLE_STATE_PREFIX, auditEventsTable.getTable(), part);
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
		BusinessDelegate.auditEvents(new AsyncLoadData<AuditEvent>()
		{
			@Override public void success(final ImmutableList<AuditEvent> data)
			{
				auditEventsTable.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AuditEventsPart_ErrorLoading, details);
			}
		}, sync, selectedType().orElse(null), selectedUser().map(User::getId).orElse(null), selectedGestiune().map(Gestiune::getId).orElse(null),
		extractLocalDate(from), extractLocalDate(to), descriere.getText(), log);
	}
	
	private Optional<AuditEventType> selectedType()
	{
		return allTypes.stream()
				.filter(t -> t.displayName().equalsIgnoreCase(type.getText()))
				.findFirst();
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		return allGestiuni.stream()
				.filter(gest -> gest.getImportName().equalsIgnoreCase(gestiune.getText()))
				.findFirst();
	}
	
	private Optional<User> selectedUser()
	{
		return allUsers.stream()
				.filter(u -> u.displayName().equalsIgnoreCase(user.getText()))
				.findFirst();
	}
}
