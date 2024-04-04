package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.localToDisplayLocation;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.LoadBonuri;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;

public class ManagerIncarcaDocPopup extends PopupDialog
{
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 250;
	
	private static final String INCARCA_STATE_PREFIX = "manager_incarca_doc.popup"; //$NON-NLS-1$
	
	private Point initialLocation;
	private TipDoc tipDoc;
	private UISynchronize sync;
	private Logger log;
	private Consumer<AccountingDocument> updateAccDoc;
	
	private Button incarca;
	private DateTime from;
	private DateTime to;
	private Button anulPrecedent;
	private Button lunaCurenta;
	private Button anulCurent;
	private Button maxPeriod;
	private Button faraBonuri;
	private Button siBonuri;
	private Button doarBonuri;
	private Button inchide;
	
	public ManagerIncarcaDocPopup(final Shell shell, final Point initialLocation, final UISynchronize sync, final TipDoc tipDoc,
			final Consumer<AccountingDocument> updateAccDoc, final Logger log)
	{
		super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
		this.initialLocation = initialLocation;
		this.tipDoc = tipDoc;
		this.sync = sync;
		this.updateAccDoc = updateAccDoc;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(3, false));
		
		incarca = new Button(container, SWT.PUSH | SWT.WRAP);
		incarca.setText(Messages.ManagerIncarcaDocPopup_Load);
		final GridData incarcaGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		incarcaGD.verticalSpan = 9;
		incarca.setLayoutData(incarcaGD);
		incarca.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		incarca.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setBoldBannerFont(incarca);
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		from.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		from.setToolTipText(Messages.ManagerIncarcaDocPopup_From);
		UIUtils.setFont(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		
		inchide = new Button(container, SWT.PUSH | SWT.WRAP);
		inchide.setText(Messages.Close);
		final GridData inchideGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		inchideGD.verticalSpan = 9;
		inchide.setLayoutData(inchideGD);
		inchide.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		inchide.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setBoldBannerFont(inchide);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		to.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		to.setToolTipText(Messages.ManagerIncarcaDocPopup_To);
		UIUtils.setFont(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		
		anulPrecedent = new Button(container, SWT.PUSH | SWT.WRAP);
		anulPrecedent.setText(Messages.ManagerIncarcaDocPopup_LastYear);
		anulPrecedent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		anulPrecedent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		anulPrecedent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(anulPrecedent);
		
		lunaCurenta = new Button(container, SWT.PUSH | SWT.WRAP);
		lunaCurenta.setText(Messages.ManagerIncarcaDocPopup_ThisMonth);
		lunaCurenta.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		lunaCurenta.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(lunaCurenta);
		
		anulCurent = new Button(container, SWT.PUSH | SWT.WRAP);
		anulCurent.setText(Messages.ManagerIncarcaDocPopup_ThisYear);
		anulCurent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		anulCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		anulCurent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(anulCurent);
		
		maxPeriod = new Button(container, SWT.PUSH | SWT.WRAP);
		maxPeriod.setText(Messages.ManagerIncarcaDocPopup_MaxPeriod);
		maxPeriod.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		maxPeriod.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		maxPeriod.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(maxPeriod);
		
		faraBonuri = new Button(container, SWT.RADIO);
		faraBonuri.setText(Messages.ManagerIncarcaDocPopup_NoFiscalReceipts);
		faraBonuri.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		UIUtils.setBoldFont(faraBonuri);
		
		siBonuri = new Button(container, SWT.RADIO);
		siBonuri.setText(Messages.ManagerIncarcaDocPopup_WithFiscalReceipts);
		siBonuri.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		UIUtils.setBoldFont(siBonuri);
		
		doarBonuri = new Button(container, SWT.RADIO);
		doarBonuri.setText(Messages.ManagerIncarcaDocPopup_OnlyFiscalReceipts);
		doarBonuri.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		UIUtils.setBoldFont(doarBonuri);
		
		addListeners();
		loadState(INCARCA_STATE_PREFIX, ClientSession.instance().getProperties());
		return container;
	}
	
	private void addListeners()
	{
		incarca.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				LoadBonuri loadBonuri = LoadBonuri.FARA_BONURI;
				if (faraBonuri.getSelection())
					loadBonuri = LoadBonuri.FARA_BONURI;
				else if (siBonuri.getSelection())
					loadBonuri = LoadBonuri.SI_BONURI;
				else if (doarBonuri.getSelection())
					loadBonuri = LoadBonuri.DOAR_BONURI;
				
				new SelectDocToLoadPopup(getParentShell(), localToDisplayLocation(getShell()), 
						sync, tipDoc, extractLocalDate(from), extractLocalDate(to), loadBonuri, updateAccDoc, log)
					.open();
				close();
			}
		});
		
		anulPrecedent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().minusYears(1).with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().minusYears(1).with(TemporalAdjusters.lastDayOfYear()));
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
		
		anulCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
			}
		});
		
		maxPeriod.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN.toLocalDate());
				insertDate(to, POSTGRES_MAX.toLocalDate());
			}
		});
		
		inchide.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				close();
			}
		});
	}
	
	@Override
	public boolean close()
	{
		saveState(INCARCA_STATE_PREFIX, ClientSession.instance().getProperties());
		return super.close();
	}
	
	public void saveState(final String prefix, final Properties properties)
	{
		if (getShell() == null || getShell().isDisposed())
			return;
		
		properties.put(prefix+".from", extractLocalDate(from)); //$NON-NLS-1$
		properties.put(prefix+".to", extractLocalDate(to)); //$NON-NLS-1$
		properties.put(prefix+".faraBonuri", faraBonuri.getSelection()); //$NON-NLS-1$
		properties.put(prefix+".siBonuri", siBonuri.getSelection()); //$NON-NLS-1$
		properties.put(prefix+".doarBonuri", doarBonuri.getSelection()); //$NON-NLS-1$
	}
	
	public void loadState(final String prefix, final Properties properties)
	{
		insertDate(from, (LocalDate)properties.getOrDefault(prefix+".from", LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()))); //$NON-NLS-1$
		insertDate(to, (LocalDate)properties.getOrDefault(prefix+".to", LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()))); //$NON-NLS-1$
		faraBonuri.setSelection((boolean)properties.getOrDefault(prefix+".faraBonuri", true)); //$NON-NLS-1$
		siBonuri.setSelection((boolean)properties.getOrDefault(prefix+".siBonuri", false)); //$NON-NLS-1$
		doarBonuri.setSelection((boolean)properties.getOrDefault(prefix+".doarBonuri", false)); //$NON-NLS-1$
	}

	@Override
	protected Point getDefaultLocation(final Point initialSize)
	{
		if (this.initialLocation != null)
			return initialLocation;
		
		return super.getDefaultLocation(initialSize);
	}
	
	@Override
	protected Color getBackground()
	{
		return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	}
	
	@Override
	protected List<Control> getBackgroundColorExclusions()
	{
		final List<Control> list = super.getBackgroundColorExclusions();
		list.add(incarca);
		list.add(inchide);
		list.add(anulPrecedent);
		list.add(lunaCurenta);
		list.add(anulCurent);
		list.add(maxPeriod);
		list.add(from);
		list.add(to);
		return list;
	}
	
	@Override
	protected List<Control> getForegroundColorExclusions()
	{
		final List<Control> list = super.getForegroundColorExclusions();
		list.add(anulPrecedent);
		list.add(lunaCurenta);
		list.add(anulCurent);
		list.add(maxPeriod);
		return list;
	}
}
