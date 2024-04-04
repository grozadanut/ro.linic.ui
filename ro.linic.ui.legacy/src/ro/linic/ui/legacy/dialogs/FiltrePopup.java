package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Text;
import org.jboss.weld.exceptions.UnsupportedOperationException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.user.User;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.widgets.NatComboWidget;

public class FiltrePopup extends PopupDialog
{
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 250;
	
	private static final String FILTRE_STATE_PREFIX = "filtre.popup"; //$NON-NLS-1$
	
	private TipOp tipOp;

	private Combo category;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private Button maximRec;
	private Button ziCurentaRec;
	private Button lunaCurentaRec;
	private Button anCurentRec;
	private DateTime from;
	private DateTime to;
	private DateTime fromRec;
	private DateTime toRec;
	private NatComboWidget doc;
	private Text nrDoc;
	private Combo partner;
	private Text barcode;
	private Text denumire;
	private Combo gestiuneOp;
	private Combo gestiuneDoc;
	private Combo user;
	private Text maxRows;
	
	private Button executa;
	private Button inchide;
	
	private ImmutableList<Partner> allPartners;
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<User> allUsers;
	
	private Point initialLocation;
	private Point initialSize;
	private UISynchronize sync;
	private Logger log;
	private Consumer<ImmutableList<Operatiune>> resultConsumer;
	
	private Job loadJob;
	
	public FiltrePopup(final Shell shell, final Point initialLocation, final Point initialSize, final UISynchronize sync, 
			final ImmutableList<Partner> allPartners, final TipOp tipOp, final Consumer<ImmutableList<Operatiune>> resultConsumer, final Logger log)
	{
		super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
		this.initialLocation = initialLocation;
		this.initialSize = initialSize;
		this.sync = sync;
		this.allPartners = allPartners;
		this.tipOp = tipOp;
		this.allGestiuni = BusinessDelegate.allGestiuni();
		this.allUsers = BusinessDelegate.dbUsers();
		this.resultConsumer = resultConsumer;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));
		createWidgetArea(container);
		
		executa = new Button(container, SWT.PUSH | SWT.WRAP);
		executa.setText(Messages.FiltrePopup_Execute);
		executa.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		executa.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(executa);
		final GridData executaGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		executaGD.horizontalAlignment = SWT.RIGHT;
		executa.setLayoutData(executaGD);
		
		maxRows = new Text(container, SWT.BORDER);
		maxRows.setMessage(Messages.FiltrePopup_MaxRows);
		maxRows.setText("500"); //$NON-NLS-1$
		UIUtils.setFont(maxRows);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).hint(BUTTON_WIDTH-15, SWT.DEFAULT).applyTo(maxRows);
		
		inchide = new Button(container, SWT.PUSH | SWT.WRAP);
		inchide.setText(Messages.Close);
		inchide.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		inchide.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(inchide);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(inchide);
		
		addListeners();
		loadState(FILTRE_STATE_PREFIX, ClientSession.instance().getProperties());
		return container;
	}
	
	private void createWidgetArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(10, false));
		GridDataFactory.swtDefaults().span(1, 2).applyTo(container);
		
		final Label categoryLabel = new Label(container, SWT.NONE);
		categoryLabel.setText(Messages.FiltrePopup_Category);
		UIUtils.setFont(categoryLabel);
		
		category = new Combo(container, SWT.DROP_DOWN);
		category.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		category.setItems(Product.ALL_CATEGORIES.toArray(new String[] {}));
		UIUtils.setFont(category);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(category);
		
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
		
		new Label(container, SWT.NONE); // layout purpose
		
		maximRec = new Button(container, SWT.PUSH);
		maximRec.setText(Messages.Max);
		maximRec.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maximRec);
		GridDataFactory.swtDefaults().applyTo(maximRec);
		
		ziCurentaRec = new Button(container, SWT.PUSH);
		ziCurentaRec.setText(Messages.CurrentDay);
		ziCurentaRec.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurentaRec);
		GridDataFactory.swtDefaults().applyTo(ziCurentaRec);
		
		lunaCurentaRec = new Button(container, SWT.PUSH);
		lunaCurentaRec.setText(Messages.CurrentMonth);
		lunaCurentaRec.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurentaRec);
		GridDataFactory.swtDefaults().applyTo(lunaCurentaRec);
		
		anCurentRec = new Button(container, SWT.PUSH);
		anCurentRec.setText(Messages.CurrentYear);
		anCurentRec.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurentRec);
		GridDataFactory.swtDefaults().applyTo(anCurentRec);
		
		final Label dataDocLabel = new Label(container, SWT.NONE);
		dataDocLabel.setText(Messages.FiltrePopup_DocDate);
		UIUtils.setFont(dataDocLabel);

		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		
		final Label dataRecLabel = new Label(container, SWT.NONE);
		dataRecLabel.setText(Messages.FiltrePopup_ReceptionDate);
		UIUtils.setFont(dataRecLabel);
		
		fromRec = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(fromRec);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(fromRec);
		
		toRec = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(toRec);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(toRec);

		final Label docLabel = new Label(container, SWT.NONE);
		docLabel.setText(Messages.FiltrePopup_DocType);
		UIUtils.setFont(docLabel);
		
		doc = new NatComboWidget(container, SWT.MULTI | SWT.CHECK);
		UIUtils.setFont(doc);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(doc);
		setDocTypes();
		
		final Label nrDocLabel = new Label(container, SWT.NONE);
		nrDocLabel.setText(Messages.FiltrePopup_DocNumber);
		UIUtils.setFont(nrDocLabel);
		
		nrDoc = new Text(container, SWT.BORDER);
		UIUtils.setFont(nrDoc);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(nrDoc);
		
		final Label partnerLabel = new Label(container, SWT.NONE);
		partnerLabel.setText(Messages.FiltrePopup_Partner);
		UIUtils.setFont(partnerLabel);
		
		partner = new Combo(container, SWT.DROP_DOWN);
		partner.setItems(allPartners.stream().map(Partner::getName).toArray(String[]::new));
		UIUtils.setFont(partner);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(partner);
		
		final Label barcodeLabel = new Label(container, SWT.NONE);
		barcodeLabel.setText(Messages.FiltrePopup_Barcode);
		UIUtils.setFont(barcodeLabel);
		
		barcode = new Text(container, SWT.BORDER);
		UIUtils.setFont(barcode);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(barcode);
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText(Messages.FiltrePopup_Name);
		UIUtils.setFont(nameLabel);
		
		denumire = new Text(container, SWT.BORDER);
		UIUtils.setFont(denumire);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(denumire);
		
		final Label gestiuneOpLabel = new Label(container, SWT.NONE);
		gestiuneOpLabel.setText(Messages.FiltrePopup_Inventory);
		UIUtils.setFont(gestiuneOpLabel);
		
		gestiuneOp = new Combo(container, SWT.DROP_DOWN);
		gestiuneOp.setItems(allGestiuni.stream().map(Gestiune::getName).toArray(String[]::new));
		UIUtils.setFont(gestiuneOp);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(gestiuneOp);
		
		final Label gestiuneDocLabel = new Label(container, SWT.NONE);
		gestiuneDocLabel.setText(Messages.FiltrePopup_DocInventory);
		UIUtils.setFont(gestiuneDocLabel);
		
		gestiuneDoc = new Combo(container, SWT.DROP_DOWN);
		gestiuneDoc.setItems(allGestiuni.stream().map(Gestiune::getName).toArray(String[]::new));
		UIUtils.setFont(gestiuneDoc);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(gestiuneDoc);
		
		final Label userLabel = new Label(container, SWT.NONE);
		userLabel.setText(Messages.FiltrePopup_User);
		UIUtils.setFont(userLabel);

		user = new Combo(container, SWT.DROP_DOWN);
		user.setItems(allUsers.stream().map(User::displayName).toArray(String[]::new));
		UIUtils.setFont(user);
		GridDataFactory.fillDefaults().grab(true, false).span(9, 1).applyTo(user);
	}
	
	private void addListeners()
	{
		executa.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				cancelLoadJob();
				executa.setEnabled(false);
				
				final Partner selPartner = partner().orElse(null);
				final Gestiune selGestiuneOp = gestiuneOp().orElse(null);
				final Gestiune selGestiuneDoc = gestiuneDoc().orElse(null);
				final User selUser = user().orElse(null);
				
				loadJob = BusinessDelegate.filteredOperations(new AsyncLoadData<Operatiune>()
				{
					@Override public void success(final ImmutableList<Operatiune> data)
					{
						resultConsumer.accept(data);
						close();
					}

					@Override public void error(final String details)
					{
						MessageDialog.openError(getShell(), Messages.FiltrePopup_FilterError, details);
						close();
					}
				}, sync, tipOp, category.getText(), extractLocalDate(from), extractLocalDate(to), selectedDocTypes(), nrDoc.getText(),
				selPartner, barcode.getText(), denumire.getText(), selGestiuneOp, selGestiuneDoc, selUser, parseToInt(maxRows.getText()),
				extractLocalDate(fromRec), extractLocalDate(toRec), log);
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
		
		maximRec.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(fromRec, POSTGRES_MIN);
				insertDate(toRec, POSTGRES_MAX);
			}
		});
		
		ziCurentaRec.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(fromRec, LocalDate.now());
				insertDate(toRec, LocalDate.now());
			}
		});
		
		lunaCurentaRec.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(fromRec, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(toRec, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
			}
		});
		
		anCurentRec.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(fromRec, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(toRec, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
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
		saveState(FILTRE_STATE_PREFIX, ClientSession.instance().getProperties());
		return super.close();
	}
	
	public void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
	}
	
	public void saveState(final String prefix, final Properties properties)
	{
		if (getShell() == null || getShell().isDisposed())
			return;
		
		properties.put(prefix+".category", category.getText()); //$NON-NLS-1$
		properties.put(prefix+".from", extractLocalDate(from)); //$NON-NLS-1$
		properties.put(prefix+".to", extractLocalDate(to)); //$NON-NLS-1$
		properties.put(prefix+".fromRec", extractLocalDate(fromRec)); //$NON-NLS-1$
		properties.put(prefix+".toRec", extractLocalDate(toRec)); //$NON-NLS-1$
		properties.put(prefix+".doc", doc.getSelection()); //$NON-NLS-1$
		properties.put(prefix+".nrDoc", nrDoc.getText()); //$NON-NLS-1$
		properties.put(prefix+".partner", partner.getSelectionIndex()); //$NON-NLS-1$
		properties.put(prefix+".barcode", barcode.getText()); //$NON-NLS-1$
		properties.put(prefix+".denumire", denumire.getText()); //$NON-NLS-1$
		properties.put(prefix+".gestiuneOp", gestiuneOp.getSelectionIndex()); //$NON-NLS-1$
		properties.put(prefix+".gestiuneDoc", gestiuneDoc.getSelectionIndex()); //$NON-NLS-1$
		properties.put(prefix+".user", user.getSelectionIndex()); //$NON-NLS-1$
		properties.put(prefix+".maxRows", maxRows.getText()); //$NON-NLS-1$
	}
	
	public void loadState(final String prefix, final Properties properties)
	{
		category.setText(properties.getProperty(prefix+".category", EMPTY_STRING)); //$NON-NLS-1$
		insertDate(from, (LocalDate)properties.getOrDefault(prefix+".from", LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()))); //$NON-NLS-1$
		insertDate(to, (LocalDate)properties.getOrDefault(prefix+".to", LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()))); //$NON-NLS-1$
		insertDate(fromRec, (LocalDate)properties.getOrDefault(prefix+".fromRec", POSTGRES_MIN.toLocalDate())); //$NON-NLS-1$
		insertDate(toRec, (LocalDate)properties.getOrDefault(prefix+".toRec", POSTGRES_MAX.toLocalDate())); //$NON-NLS-1$
		final String[] docSavedItems = (String[]) properties.get(prefix+".doc"); //$NON-NLS-1$
		if (docSavedItems != null && doc.getItems().containsAll(ImmutableList.copyOf(docSavedItems)))
			doc.setSelection(docSavedItems);
		nrDoc.setText(properties.getProperty(prefix+".nrDoc", EMPTY_STRING)); //$NON-NLS-1$
		partner.select((int)properties.getOrDefault(prefix+".partner", -1)); //$NON-NLS-1$
		barcode.setText(properties.getProperty(prefix+".barcode", EMPTY_STRING)); //$NON-NLS-1$
		denumire.setText(properties.getProperty(prefix+".denumire", EMPTY_STRING)); //$NON-NLS-1$
		gestiuneOp.select((int)properties.getOrDefault(prefix+".gestiuneOp", -1)); //$NON-NLS-1$
		gestiuneDoc.select((int)properties.getOrDefault(prefix+".gestiuneDoc", -1)); //$NON-NLS-1$
		user.select((int)properties.getOrDefault(prefix+".user", -1)); //$NON-NLS-1$
		maxRows.setText(properties.getProperty(prefix+".maxRows", "500")); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (!properties.containsKey(prefix+".gestiuneOp")) //$NON-NLS-1$
			gestiuneOp.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		if (!properties.containsKey(prefix+".partner")) //$NON-NLS-1$
			for (int i = 0; i < allPartners.size(); i++)
			{
				if (Partner.STANDARD_PARTNER_NAME.equalsIgnoreCase(allPartners.get(i).getName()))
				{
					partner.select(i);
					break;
				}
			}
	}
	
	@Override
	protected Control getFocusControl()
	{
		return denumire;
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
		if (this.initialSize != null)
			return initialSize;

		return super.getDefaultSize();
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
		list.add(category);
		list.add(maxim);
		list.add(ziCurenta);
		list.add(lunaCurenta);
		list.add(anCurent);
		list.add(maximRec);
		list.add(ziCurentaRec);
		list.add(lunaCurentaRec);
		list.add(anCurentRec);
		list.add(from);
		list.add(to);
		list.add(fromRec);
		list.add(toRec);
		list.add(doc);
		list.add(doc.getTextControl());
		list.add(doc.getIconCanvas());
		list.add(nrDoc);
		list.add(partner);
		list.add(barcode);
		list.add(denumire);
		list.add(gestiuneOp);
		list.add(gestiuneDoc);
		list.add(user);
		list.add(executa);
		list.add(inchide);
		list.add(maxRows);
		return list;
	}
	
	@Override
	protected List<Control> getForegroundColorExclusions()
	{
		final List<Control> list = super.getForegroundColorExclusions();
		list.add(executa);
		list.add(inchide);
		return list;
	}
	
	private void setDocTypes()
	{
		switch (tipOp)
		{
		case INTRARE:
			doc.setItems(AccountingDocument.ALL_INTRARI_OPERATION_DOC_TYPES.toArray(new String[] {}));
			break;
			
		case IESIRE:
			doc.setItems(AccountingDocument.ALL_IESIRI_OPERATION_DOC_TYPES.toArray(new String[] {}));
			break;

		default:
			throw new UnsupportedOperationException(Messages.FiltrePopup_OpTypeUnsupported+tipOp);
		}
	}
	
	private Optional<Partner> partner()
	{
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allPartners.get(index));
	}
	
	private Optional<Gestiune> gestiuneOp()
	{
		final int index = gestiuneOp.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allGestiuni.get(index));
	}
	
	private Optional<Gestiune> gestiuneDoc()
	{
		final int index = gestiuneDoc.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allGestiuni.get(index));
	}
	
	private Optional<User> user()
	{
		final int index = user.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allUsers.get(index));
	}
	
	private ImmutableSet<String> selectedDocTypes()
	{
		return ImmutableSet.copyOf(doc.getSelection());
	}
}
