package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
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

import ro.colibri.embeddable.FidelityCard;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Document;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.Messages;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.wizards.InchideBonWizard;

public class AdaugaDocDialog extends Window
{
	private static final String ACC_DOC_CHOICE = "Document";
	private static final String DISCOUNT_DOC_CHOICE = "Discount";
	
	private Combo documentClass;
	private Label partnerHint;
	private Label tipDocHint;
	
	private Combo doc;
	private Text nrDoc;
	private Button autoNr;
	private DateTime dataDoc;
	private DateTime scadenta;
	private Text tvaReadable;
	private Button regCasa;
	private Combo contBancar;
	private Combo gestiune;
	private Text total;
	private Text totalTva;
	private Text name;
	private Button rpz;
	
	private Button adauga;
	private Button inchide;
	
	private ImmutableList<Gestiune> allGestiuni;
	private String dbTvaReadable;
	private BigDecimal tvaExtractDivisor;
	private Partner selectedPartner;
	private TipDoc selectedTipDoc;
	private Consumer<Document> adaugaConsumer;

	private UISynchronize sync;
	private Logger log;
	private Point initialLocation;
	private final ShellListener parentShellListener;
//	private long parentActivated = 0;
	private long dialogActivated = 0;
	private ImmutableList<ContBancar> allConturiBancare;

	public AdaugaDocDialog(final Shell parentShell, final UISynchronize sync, final Point initialLocation, final Partner selectedPartner,
			final TipDoc selectedTipDoc, final Consumer<Document> adaugaConsumer, final Logger log)
	{
		super(parentShell);
		this.sync = sync;
		this.log = log;
		this.initialLocation = initialLocation;
		this.selectedPartner = selectedPartner;
		this.selectedTipDoc = selectedTipDoc;
		this.adaugaConsumer = adaugaConsumer; 
		this.allGestiuni = BusinessDelegate.allGestiuni();
		final BigDecimal tvaPercent = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		this.tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
		this.dbTvaReadable = Operatiune.tvaReadable(tvaPercent);
		allConturiBancare = BusinessDelegate.allConturiBancare();
		
		setShellStyle(SWT.DIALOG_TRIM | SWT.ON_TOP | getDefaultOrientation());
		setBlockOnOpen(false);
		
		parentShellListener = new ShellAdapter()
		{
//			@Override public void shellActivated(final ShellEvent e)
//			{
//				parentActivated = System.currentTimeMillis();
//			}
			@Override public void shellDeactivated(final ShellEvent e)
			{
				final long start = System.currentTimeMillis();
				sync.asyncExec(() ->
				{
					try
					{
						Thread.sleep(100);
					}
					catch (final InterruptedException e1)
					{
						log.error(e1);
					}
					
					if ((dialogActivated - start) < 0)
						close();
				});
			}
		};
	}
	
	@Override
	protected Control createContents(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		createWidgetArea(container);
		addListeners();
		return container;
	}
	
	private void createWidgetArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(contents);
		getShell().setText(Messages.AdaugaDocDialog_AddDoc);
		
		final Composite topBarContainer = new Composite(contents, SWT.NONE);
		topBarContainer.setLayout(new GridLayout(2, false));
		topBarContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(topBarContainer);
		
		partnerHint = new Label(topBarContainer, SWT.NONE);
		partnerHint.setText(safeString(selectedPartner, Partner::getName));
		partnerHint.setAlignment(SWT.CENTER);
		partnerHint.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		partnerHint.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(partnerHint);
		GridDataFactory.fillDefaults().hint(600, SWT.DEFAULT).applyTo(partnerHint);
		
		tipDocHint = new Label(topBarContainer, SWT.NONE);
		tipDocHint.setText(safeString(selectedTipDoc, TipDoc::toString));
		tipDocHint.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		tipDocHint.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(tipDocHint);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(tipDocHint);
		
		inchide = new Button(contents, SWT.PUSH);
		inchide.setText("Inchide");
		inchide.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		inchide.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchide);
		GridDataFactory.fillDefaults().grab(false, true).span(1, 3).applyTo(inchide);
		
		createDocComposite(contents);
		
		documentClass = new Combo(contents, SWT.DROP_DOWN);
		documentClass.setItems(new String[] {ACC_DOC_CHOICE, DISCOUNT_DOC_CHOICE});
		documentClass.select(0);
		UIUtils.setFont(documentClass);
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).indent(5, 5).applyTo(documentClass);
		
		adauga = new Button(contents, SWT.PUSH);
		adauga.setText(Messages.AdaugaDocDialog_Add);
		adauga.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		adauga.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(adauga);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adauga);
	}

	private void createDocComposite(final Composite contents)
	{
		final Composite docContainer = new Composite(contents, SWT.NONE);
		docContainer.setLayout(new GridLayout(7, false));
		
		final Label docLabel = new Label(docContainer, SWT.NONE);
		docLabel.setText(Messages.AdaugaDocDialog_DocType);
		docLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(docLabel);
		GridDataFactory.fillDefaults().applyTo(docLabel);
		
		final Label nrDocLabel = new Label(docContainer, SWT.NONE);
		nrDocLabel.setText(Messages.AdaugaDocDialog_DocNumber);
		nrDocLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(nrDocLabel);
		GridDataFactory.fillDefaults().applyTo(nrDocLabel);
		
		autoNr = new Button(docContainer, SWT.PUSH);
		autoNr.setText("A");
		autoNr.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		final Label dataDocLabel = new Label(docContainer, SWT.NONE);
		dataDocLabel.setText(Messages.AdaugaDocDialog_Date);
		dataDocLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(dataDocLabel);
		GridDataFactory.fillDefaults().applyTo(dataDocLabel);
		
		final Label scadentaLabel = new Label(docContainer, SWT.NONE);
		scadentaLabel.setText(Messages.AdaugaDocDialog_Due);
		scadentaLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(scadentaLabel);
		GridDataFactory.fillDefaults().applyTo(scadentaLabel);
		
		final Label tvaLabel = new Label(docContainer, SWT.NONE);
		tvaLabel.setText(Messages.VAT_Percentage);
		tvaLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(tvaLabel);
		GridDataFactory.fillDefaults().applyTo(tvaLabel);
		
		gestiune = new Combo(docContainer, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getImportName).toArray(String[]::new));
		gestiune.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		UIUtils.setFont(gestiune);
		GridDataFactory.fillDefaults().applyTo(gestiune);
		
		doc = new Combo(docContainer, SWT.DROP_DOWN);
		UIUtils.setFont(doc);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(doc);
		updateDocTypes();
		
		nrDoc = new Text(docContainer, SWT.BORDER);
		UIUtils.setFont(nrDoc);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(nrDoc);
		
		dataDoc = new DateTime(docContainer, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(dataDoc);
		GridDataFactory.fillDefaults().applyTo(dataDoc);
		
		scadenta = new DateTime(docContainer, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(scadenta);
		GridDataFactory.fillDefaults().applyTo(scadenta);
		updateScadenta();
		
		tvaReadable = new Text(docContainer, SWT.BORDER);
		tvaReadable.setText(dbTvaReadable);
		UIUtils.setFont(tvaReadable);
		GridDataFactory.fillDefaults().applyTo(tvaReadable);
		
		regCasa = new Button(docContainer, SWT.CHECK);
		regCasa.setText("REG-CASA");
		regCasa.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(regCasa);
		
		final Label totalLabel = new Label(docContainer, SWT.NONE);
		totalLabel.setText("Total");
		totalLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(totalLabel);
		GridDataFactory.fillDefaults().applyTo(totalLabel);
		
		final Label totalTvaLabel = new Label(docContainer, SWT.NONE);
		totalTvaLabel.setText(Messages.VAT);
		totalTvaLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(totalTvaLabel);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(totalTvaLabel);
		
		final Label nameLabel = new Label(docContainer, SWT.NONE);
		nameLabel.setText(Messages.AdaugaDocDialog_NameLabel);
		nameLabel.setAlignment(SWT.CENTER);
		UIUtils.setFont(nameLabel);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(nameLabel);
		
		contBancar = new Combo(docContainer, SWT.DROP_DOWN);
		contBancar.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		UIUtils.setFont(contBancar);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(contBancar);
		
		total = new Text(docContainer, SWT.BORDER);
		UIUtils.setFont(total);
		GridDataFactory.fillDefaults().applyTo(total);
		
		totalTva = new Text(docContainer, SWT.BORDER);
		UIUtils.setFont(totalTva);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(totalTva);
		
		name = new Text(docContainer, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().span(4, 1).applyTo(name);
		
		rpz = new Button(docContainer, SWT.CHECK);
		rpz.setText(Messages.AdaugaDocDialog_RPZ);
		rpz.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(rpz);
		GridDataFactory.fillDefaults().span(7, 1).applyTo(rpz);
		
		docContainer.setTabList(new Control[] {doc, nrDoc, dataDoc, scadenta, regCasa, contBancar, total, totalTva, name, rpz});
		contents.setTabList(new Control[] {docContainer});
	}
	
	@Override
	protected Point getInitialLocation(final Point initialSize)
	{
		if (this.initialLocation != null)
			return initialLocation;
		
		return super.getInitialLocation(initialSize);
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(Display.getCurrent().getClientArea().width, super.getInitialSize().y);
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
				adauga();
			}
		});
		
		inchide.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				close();
			}
		});
		
		autoNr.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				nrDoc.setText(String.valueOf(BusinessDelegate.autoNumber(selectedTipDoc, doc.getText(), selectedGestiune().map(Gestiune::getId).orElse(null))));
			}
		});
		
		documentClass.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				changedDocumentClass(documentClass.getText());
			}
		});
		
		total.addModifyListener(e -> totalTva.setText(AccountingDocument.extractTvaAmount(parse(total.getText()), tvaExtractDivisor()).toString()));
		
		final KeyListener keyListener = new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT, e);
					e.doit = false;
				}
				if (e.keyCode == SWT.F4)
					adauga();
			}
		};
		
		doc.addKeyListener(keyListener);
		nrDoc.addKeyListener(keyListener);
		dataDoc.addKeyListener(keyListener);
		scadenta.addKeyListener(keyListener);
		regCasa.addKeyListener(keyListener);
		contBancar.addKeyListener(keyListener);
		total.addKeyListener(keyListener);
		totalTva.addKeyListener(keyListener);
		name.addKeyListener(keyListener);
		rpz.addKeyListener(keyListener);
		
		dataDoc.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateScadenta();
			}
		});
		
		final FocusAdapter selectAllListener = new FocusAdapter()
		{
			@Override public void focusGained(final FocusEvent e)
			{
				((Text) e.widget).selectAll();
			}
		};
		
		nrDoc.addFocusListener(selectAllListener);
		total.addFocusListener(selectAllListener);
		totalTva.addFocusListener(selectAllListener);
		name.addFocusListener(selectAllListener);
		
		getShell().addShellListener(new ShellAdapter()
		{
//			@Override public void shellDeactivated(final ShellEvent e)
//			{
//				final long start = System.currentTimeMillis();
//				sync.asyncExec(() ->
//				{
//					try
//					{
//						Thread.sleep(100);
//					}
//					catch (final InterruptedException e1)
//					{
//						log.error(e1);
//					}
//					
//					if ((parentActivated - start) < 0)
//						close();
//				});
//			}
			
			@Override public void shellActivated(final ShellEvent e) 
			{
				dialogActivated = System.currentTimeMillis();
			}
		});
		getParentShell().addShellListener(parentShellListener);
	}
	
	@Override
	public boolean close()
	{
		getParentShell().removeShellListener(parentShellListener);
		return super.close();
	}
	
	public void changedSelectedPartner(final Optional<Partner> partner)
	{
		if (!partner.isPresent())
		{
			close();
			return;
		}
		
		this.selectedPartner = partner.get();
		updateScadenta();
		updateTvaText(documentClass.getText());
		partnerHint.setText(safeString(selectedPartner, Partner::getName));
	}
	
	public void changedTipDoc(final Optional<TipDoc> tipDoc)
	{
		if (!tipDoc.isPresent())
		{
			close();
			return;
		}
		
		this.selectedTipDoc = tipDoc.get();
		updateDocTypes();
		tipDocHint.setText(safeString(selectedTipDoc, TipDoc::toString));
	}
	
	private void updateScadenta()
	{
		insertDate(scadenta, Optional.ofNullable(selectedPartner)
				.map(Partner::getTermenPlata)
				.map(termen -> extractLocalDate(dataDoc).plusDays(termen))
				.orElse(extractLocalDate(dataDoc)));
	}
	
	private void adauga()
	{
		if (documentClass.getText().equalsIgnoreCase(ACC_DOC_CHOICE))
		{
			if (!selectedGestiune().isPresent())
				return;
			
			final AccountingDocument accDoc = new AccountingDocument();
			accDoc.setDataDoc(extractLocalDate(dataDoc).atTime(LocalTime.now()));
			accDoc.setDoc(doc.getText());
			accDoc.setName(name.getText());
			accDoc.setNrDoc(nrDoc.getText());
			accDoc.setContBancar(contBancar());
			accDoc.setRegCasa(regCasa.getSelection());
			accDoc.setRpz(rpz.getSelection());
			accDoc.setScadenta(extractLocalDate(scadenta));
			accDoc.setTipDoc(selectedTipDoc);
			accDoc.setTotal(parse(total.getText()));
			accDoc.setTotalTva(parse(totalTva.getText()));
			final InvocationResult result = BusinessDelegate.persistAccDoc(accDoc, selectedPartner.getId(), selectedGestiune().get().getId());
			showResult(result);
			
			if (result.statusOk())
			{
				adaugaConsumer.accept(result.extra(InvocationResult.ACCT_DOC_KEY));
				clearFields();
				doc.setFocus();
			}
		}
		else if (documentClass.getText().equalsIgnoreCase(DISCOUNT_DOC_CHOICE))
		{
			final DocumentWithDiscount discountDoc = new DocumentWithDiscount();
			discountDoc.setDataDoc(extractLocalDate(dataDoc).atTime(LocalTime.now()));
			discountDoc.setName(name.getText());
			discountDoc.setTipDoc(selectedTipDoc);
			discountDoc.setTotal(parse(total.getText()));
			discountDoc.setTotalTva(parse(totalTva.getText()));
			final InvocationResult result = BusinessDelegate.persistDiscountDoc(discountDoc, selectedPartner.getId());
			showResult(result);
			
			if (result.statusOk())
			{
				adaugaConsumer.accept(result.extra(InvocationResult.DISCOUNT_DOC_KEY));
				clearFields();
				total.setFocus();
			}
		}
	}
	
	private void changedDocumentClass(final String type)
	{
		doc.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		nrDoc.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		autoNr.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		scadenta.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		regCasa.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		contBancar.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		gestiune.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		rpz.setEnabled(type.equalsIgnoreCase(ACC_DOC_CHOICE));
		updateTvaText(type);
	}
	
	private void updateTvaText(final String documentClass)
	{
		if (documentClass.equalsIgnoreCase(ACC_DOC_CHOICE))
			tvaReadable.setText(dbTvaReadable);
		else if (documentClass.equalsIgnoreCase(DISCOUNT_DOC_CHOICE))
			tvaReadable.setText(Optional.ofNullable(selectedPartner)
					.map(Partner::getFidelityCard)
					.map(FidelityCard::getDiscountPercentage)
					.map(percent -> percent.multiply(new BigDecimal("100")))
					.map(PresentationUtils::displayBigDecimal)
					.orElse("0"));
	}
	
	private void updateDocTypes()
	{
		switch (selectedTipDoc)
		{
		case CUMPARARE:
			doc.setItems(AccountingDocument.CUMPARARE_DOC_TYPES.toArray(new String[] {}));
			break;

		case VANZARE:
			doc.setItems(AccountingDocument.VANZARE_DOC_TYPES.toArray(new String[] {}));
			break;

		case PLATA:
			doc.setItems(AccountingDocument.PLATA_DOC_TYPES.toArray(new String[] {}));
			break;
			
		case INCASARE:
			doc.setItems(AccountingDocument.INCASARE_DOC_TYPES.toArray(new String[] {}));
			break;

		default:
			throw new UnsupportedOperationException("selectedTipDoc neimplementat: "+selectedTipDoc);
		}
	}
	
	private void clearFields()
	{
		nrDoc.setText(EMPTY_STRING);
		regCasa.setSelection(false);
		contBancar.setText(EMPTY_STRING);
		contBancar.deselectAll();
		contBancar.clearSelection();
		total.setText(EMPTY_STRING);
		totalTva.setText(EMPTY_STRING);
		name.setText(EMPTY_STRING);
		rpz.setSelection(false);
	}
	
	public ContBancar contBancar()
	{
		final int index = contBancar.getSelectionIndex();
		if (index == -1)
			return null;
		
		return allConturiBancare.get(index);
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		return allGestiuni.stream()
				.filter(gest -> gest.getImportName().equalsIgnoreCase(gestiune.getText()))
				.findFirst();
	}
	
	private BigDecimal tvaExtractDivisor()
	{
		if (documentClass.getText().equalsIgnoreCase(ACC_DOC_CHOICE))
		{
			final BigDecimal readableTva = parse(tvaReadable.getText());
			if (greaterThan(readableTva, BigDecimal.ZERO))
				return readableTva.divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN).add(BigDecimal.ONE);
			return tvaExtractDivisor;
		}
		return tvaExtractDivisor;
	}
}
