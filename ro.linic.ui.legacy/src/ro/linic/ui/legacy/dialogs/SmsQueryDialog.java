package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
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

import ro.colibri.entities.comercial.Gestiune;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;

public class SmsQueryDialog extends TitleAreaDialog
{
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Combo gestiune;
	
	private ImmutableList<Gestiune> allGestiuni;
	
	private LocalDate selectedFrom = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
	private LocalDate selectedTo = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
	private Optional<Gestiune> selectedGestiune = Optional.empty();


	public SmsQueryDialog(final Shell parent)
	{
		super(parent);
		this.allGestiuni = BusinessDelegate.allGestiuni();
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(5, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle(Messages.SmsQueryDialog_GenerateSms);
		
		new Label(container, SWT.NONE); // layout purpose
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText(Messages.Max);
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText(Messages.CurrentDay);
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText(Messages.CurrentMonth);
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText(Messages.CurrentYear);
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		final Label dataDocLabel = new Label(container, SWT.NONE);
		dataDocLabel.setText(Messages.FiltrePopup_DocDate);
		UIUtils.setFont(dataDocLabel);

		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		insertDate(from, selectedFrom);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		insertDate(to, selectedTo);
		
		final Label gestiuneOpLabel = new Label(container, SWT.NONE);
		gestiuneOpLabel.setText(Messages.FiltrePopup_Inventory);
		UIUtils.setFont(gestiuneOpLabel);
		
		gestiune = new Combo(container, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getName).toArray(String[]::new));
		UIUtils.setFont(gestiune);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(gestiune);
		gestiune.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		
		addListeners();
		return container;
	}
	
	private void addListeners() {
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
	
	@Override
	protected void okPressed() {
		selectedFrom = ro.linic.ui.base.services.util.UIUtils.extractLocalDate(from);
		selectedTo = ro.linic.ui.base.services.util.UIUtils.extractLocalDate(to);
		selectedGestiune = gestiune();
		super.okPressed();
	}
	
	private Optional<Gestiune> gestiune()
	{
		final int index = gestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allGestiuni.get(index));
	}
	
	public Optional<Gestiune> getSelectedGestiune() {
		return selectedGestiune;
	}
	
	public LocalDate getSelectedFrom() {
		return selectedFrom;
	}
	
	public LocalDate getSelectedTo() {
		return selectedTo;
	}
}
