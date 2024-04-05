package ro.linic.ui.legacy.parts.components;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.AccountingDocument.BancaLoad;
import ro.colibri.entities.comercial.AccountingDocument.ContaLoad;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.user.User;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.Messages;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.widgets.ExtraFilterPopup;

public class UrmarireParteneriExtraFilters extends ExtraFilterPopup
{
	// referring to transportDate
	private Label dateLabel;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button doarBanca;
	private Button faraBanca;
	private Button indiferentBanca;
	private Button doarConta;
	private Button faraConta;
	private Button indiferentConta;
	private Button doarTransport;
	private Button faraTransport;
	private Button indiferentTransport;
	private Combo contBancar;
	private Combo user;
	
	private LocalDate transportFrom;
	private LocalDate transportTo;
	private BancaLoad bancaLoad = BancaLoad.INDIFERENT;
	private ContaLoad contaLoad = ContaLoad.INDIFERENT;
	private Boolean shouldTransport;
	private Integer contBancarId;
	private Integer userId;
	
	private ImmutableList<ContBancar> allConturiBancare;
	private ImmutableList<User> allUsers;
	
	public UrmarireParteneriExtraFilters(final Shell shell, final Button openButton)
	{
		super(shell, openButton);
		this.allConturiBancare = BusinessDelegate.allConturiBancare();
		this.allUsers = BusinessDelegate.dbUsers();
	}

	@Override
	protected void createWidgetArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.swtDefaults().span(1, 2).applyTo(container);

		dateLabel = new Label(container, SWT.NONE);
		dateLabel.setText(Messages.UrmarireParteneriExtraFilters_BetweenLabel);
		dateLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(dateLabel);
		GridDataFactory.swtDefaults().span(4, 1).applyTo(dateLabel);
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText(Messages.UrmarireParteneriExtraFilters_Max);
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText(Messages.UrmarireParteneriExtraFilters_Today);
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText(Messages.UrmarireParteneriExtraFilters_ThisMonth);
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText(Messages.UrmarireParteneriExtraFilters_ThisYear);
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		insertDate(from, POSTGRES_MIN);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		insertDate(to, POSTGRES_MAX);
		
		final Composite bancaGroup = new Composite(container, SWT.NONE);
		bancaGroup.setLayout(new GridLayout());
		bancaGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(bancaGroup);
		
		doarBanca = new Button(bancaGroup, SWT.RADIO);
		doarBanca.setText(Messages.UrmarireParteneriExtraFilters_BankOnly);
		doarBanca.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		doarBanca.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarBanca);
		
		faraBanca = new Button(bancaGroup, SWT.RADIO);
		faraBanca.setText(Messages.UrmarireParteneriExtraFilters_NoBank);
		faraBanca.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		faraBanca.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(faraBanca);

		indiferentBanca = new Button(bancaGroup, SWT.RADIO);
		indiferentBanca.setText(Messages.UrmarireParteneriExtraFilters_Indifferent);
		indiferentBanca.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		indiferentBanca.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(indiferentBanca);
		
		contBancar = new Combo(container, SWT.DROP_DOWN);
		contBancar.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		contBancar.setText(Messages.UrmarireParteneriExtraFilters_BankAcct);
		contBancar.setToolTipText(Messages.UrmarireParteneriExtraFilters_UsedOnly);
		UIUtils.setFont(contBancar);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(contBancar);
		
		final Composite contaGroup = new Composite(container, SWT.NONE);
		contaGroup.setLayout(new GridLayout());
		contaGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(contaGroup);
		
		doarConta = new Button(contaGroup, SWT.RADIO);
		doarConta.setText(Messages.UrmarireParteneriExtraFilters_AccountingOnly);
		doarConta.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		doarConta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarConta);
		
		faraConta = new Button(contaGroup, SWT.RADIO);
		faraConta.setText(Messages.UrmarireParteneriExtraFilters_NoAccounting);
		faraConta.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		faraConta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(faraConta);

		indiferentConta = new Button(contaGroup, SWT.RADIO);
		indiferentConta.setText(Messages.UrmarireParteneriExtraFilters_Indifferent);
		indiferentConta.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		indiferentConta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(indiferentConta);
		
		final Composite transportGroup = new Composite(container, SWT.NONE);
		transportGroup.setLayout(new GridLayout());
		transportGroup.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(transportGroup);
		
		doarTransport = new Button(transportGroup, SWT.RADIO);
		doarTransport.setText(Messages.UrmarireParteneriExtraFilters_TransportOnly);
		doarTransport.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		doarTransport.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doarTransport);
		
		faraTransport = new Button(transportGroup, SWT.RADIO);
		faraTransport.setText(Messages.UrmarireParteneriExtraFilters_NoTransport);
		faraTransport.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		faraTransport.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(faraTransport);

		indiferentTransport = new Button(transportGroup, SWT.RADIO);
		indiferentTransport.setText(Messages.UrmarireParteneriExtraFilters_Indifferent);
		indiferentTransport.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		indiferentTransport.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		GridDataFactory.fillDefaults().applyTo(indiferentTransport);
		
		user = new Combo(container, SWT.DROP_DOWN);
		user.setItems(allUsers.stream().map(User::displayName).sorted().toArray(String[]::new));
		user.setText(Messages.UrmarireParteneriExtraFilters_User);
		UIUtils.setFont(user);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(user);
	}
	
	@Override
	protected void addListeners()
	{
		maxim.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
				refreshDirtyState();
			}
		});
		
		ziCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
				refreshDirtyState();
			}
		});
		
		lunaCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
				refreshDirtyState();
			}
		});
		
		anCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
				refreshDirtyState();
			}
		});
		
		final SelectionListener refreshSelListener = new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				refreshDirtyState();
			}
		};
		final ModifyListener refreshModifyListener = new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				refreshDirtyState();
			}
		};
		from.addSelectionListener(refreshSelListener);
		to.addSelectionListener(refreshSelListener);
		doarBanca.addSelectionListener(refreshSelListener);
		faraBanca.addSelectionListener(refreshSelListener);
		indiferentBanca.addSelectionListener(refreshSelListener);
		doarConta.addSelectionListener(refreshSelListener);
		faraConta.addSelectionListener(refreshSelListener);
		indiferentConta.addSelectionListener(refreshSelListener);
		doarTransport.addSelectionListener(refreshSelListener);
		faraTransport.addSelectionListener(refreshSelListener);
		indiferentTransport.addSelectionListener(refreshSelListener);
		contBancar.addModifyListener(refreshModifyListener);
		user.addModifyListener(refreshModifyListener);
	}
	
	private void refreshDirtyState()
	{
		if (transportFrom() != null)
			markDirty(true);
		else if (transportTo() != null)
			markDirty(true);
		else if (!selectedBancaLoad().equals(BancaLoad.INDIFERENT))
			markDirty(true);
		else if (!selectedContaLoad().equals(ContaLoad.INDIFERENT))
			markDirty(true);
		else if (shouldTransport() != null)
			markDirty(true);
		else if (contBancarId() != null)
			markDirty(true);
		else if (userId() != null)
			markDirty(true);
		else
			markDirty(false);
	}
	
	@Override
	protected void saveState()
	{
		if (getShell() == null || getShell().isDisposed())
			return;
		
		transportFrom = transportFrom();
		transportTo = transportTo();
		bancaLoad = selectedBancaLoad();
		contaLoad = selectedContaLoad();
		shouldTransport = shouldTransport();
		contBancarId = contBancarId();
		userId = userId();
	}
	
	@Override
	protected void loadState()
	{
		if (transportFrom != null)
			insertDate(from, transportFrom);
		if (transportTo != null)
			insertDate(to, transportTo);
		
		if (bancaLoad != null)
		{
			if (BancaLoad.DOAR_BANCA.equals(bancaLoad))
				doarBanca.setSelection(true);
			else if (BancaLoad.FARA_BANCA.equals(bancaLoad))
				faraBanca.setSelection(true);
		}
		
		if (contaLoad != null)
		{
			if (ContaLoad.DOAR_CONTA.equals(contaLoad))
				doarConta.setSelection(true);
			else if (ContaLoad.FARA_CONTA.equals(contaLoad))
				faraConta.setSelection(true);
		}
		
		if (shouldTransport != null)
		{
			if (shouldTransport)
				doarTransport.setSelection(true);
			else
				faraTransport.setSelection(true);
		}
		
		if (userId != null)
			user.select(allUsers.indexOf(allUsers.stream()
					.filter(u -> userId.equals(u.getId()))
					.findFirst()
					.orElse(null)));
		
		if (contBancarId != null)
			contBancar.select(allConturiBancare.indexOf(allConturiBancare.stream()
					.filter(c -> contBancarId.equals(c.getId()))
					.findFirst()
					.orElse(null)));
	}
	
	@Override
	protected Color getBackground()
	{
		return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
	}
	
	@Override
	protected List<Control> getBackgroundColorExclusions()
	{
		final List<Control> list = super.getBackgroundColorExclusions();
		list.add(maxim);
		list.add(ziCurenta);
		list.add(lunaCurenta);
		list.add(anCurent);
		list.add(from);
		list.add(to);
		list.add(doarBanca);
		list.add(faraBanca);
		list.add(indiferentBanca);
		list.add(indiferentBanca.getParent());
		list.add(doarConta);
		list.add(faraConta);
		list.add(indiferentConta);
		list.add(indiferentConta.getParent());
		list.add(doarTransport);
		list.add(faraTransport);
		list.add(indiferentTransport);
		list.add(indiferentTransport.getParent());
		list.add(contBancar);
		list.add(user);
		return list;
	}
	
	@Override
	protected List<Control> getForegroundColorExclusions()
	{
		final List<Control> list = super.getForegroundColorExclusions();
		list.add(dateLabel);
		list.add(maxim);
		list.add(ziCurenta);
		list.add(lunaCurenta);
		list.add(anCurent);
		list.add(from);
		list.add(to);
		list.add(doarBanca);
		list.add(faraBanca);
		list.add(indiferentBanca);
		list.add(doarConta);
		list.add(faraConta);
		list.add(indiferentConta);
		list.add(doarTransport);
		list.add(faraTransport);
		list.add(indiferentTransport);
		list.add(contBancar);
		list.add(user);
		return list;
	}
	
	private LocalDate transportFrom()
	{
		return extractLocalDate(from).equals(POSTGRES_MIN.toLocalDate()) ? null : extractLocalDate(from);
	}
	
	private LocalDate transportTo()
	{
		return extractLocalDate(to).equals(POSTGRES_MAX.toLocalDate()) ? null : extractLocalDate(to);
	}
	
	private Integer contBancarId()
	{
		final int index = contBancar.getSelectionIndex();
		if (index == -1)
			return null;
		
		return Optional.ofNullable(allConturiBancare.get(index))
				.map(ContBancar::getId)
				.orElse(null);
	}
	
	private Integer userId()
	{
		final int index = user.getSelectionIndex();
		if (index == -1)
			return null;
		
		return Optional.ofNullable(allUsers.get(index))
				.map(User::getId)
				.orElse(null);
	}
	
	private BancaLoad selectedBancaLoad()
	{
		if (doarBanca.getSelection())
			return BancaLoad.DOAR_BANCA;
		if (faraBanca.getSelection())
			return BancaLoad.FARA_BANCA;
		return BancaLoad.INDIFERENT;
	}
	
	private ContaLoad selectedContaLoad()
	{
		if (doarConta.getSelection())
			return ContaLoad.DOAR_CONTA;
		if (faraConta.getSelection())
			return ContaLoad.FARA_CONTA;
		return ContaLoad.INDIFERENT;
	}
	
	private Boolean shouldTransport()
	{
		if (doarTransport.getSelection())
			return true;
		if (faraTransport.getSelection())
			return false;
		return null;
	}
	
	public LocalDate getTransportFrom()
	{
		return transportFrom;
	}
	
	public LocalDate getTransportTo()
	{
		return transportTo;
	}
	
	public BancaLoad getBancaLoad()
	{
		return bancaLoad;
	}
	
	public ContaLoad getContaLoad()
	{
		return contaLoad;
	}
	
	public Boolean getShouldTransport()
	{
		return shouldTransport;
	}
	
	public Integer getContBancarId()
	{
		return contBancarId;
	}
	
	public Integer getUserId()
	{
		return userId;
	}
}