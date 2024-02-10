package ro.linic.ui.legacy.parts;

import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.Delegat;
import ro.colibri.embeddable.FidelityCard;
import ro.colibri.embeddable.PartnerGrupaInteresMappingId;
import ro.colibri.entities.comercial.GrupaInteres;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.mappings.PartnerGrupaInteresMapping;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.GrupeInteresDialog;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.PartnerNatTable;
import ro.linic.ui.legacy.widgets.AddressWidget;

public class CatalogTertiPart
{
	public static final String PART_ID = "linic_gest_client.part.terti";
	
	private static final String PARTNERS_TABLE_STATE_PREFIX = "terti.parteneri_nt";
	private static final int TEXT_WIDTH = 150;
	
	private Button adauga;
	private Button salvare;
	private Button sterge;
	private Button refresh;
	private Button grupeInteres;
	
	private Text name;
	private Text cui;
	private Button preiaDate;
	private Text regCom;
	private AddressWidget adresa;
	private Text adresaLivrare;
	private Text telefon;
	private Text email;
	private Text banca;
	private Text cont;
	private Text delegatName;
	private Text delegatCNP;
	private Text delegatCI;
	private Text delegatElib;
	private Text delegatAuto;
	private Text fidelityNumber;
	private Button autoNrCard;
	private Text fidelityDiscountPercentage;
	private Text termenPlata;
	private List grupa;
	private Button notifyAppointment;
	
	private Text quickSearch;
	private PartnerNatTable table;
	
	private Partner partnerIncarcat;
	private boolean modelUpdatesUI = false;
	
	private ImmutableList<GrupaInteres> allGrupe;
	
	@Inject private MPart part;
	@Inject private EPartService partService;
	@Inject private UISynchronize sync;
	
	public static void openPart(final EPartService partService)
	{
		partService.showPart(CatalogTertiPart.PART_ID, PartState.ACTIVATE);
	}
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		final GridLayout parentLayout = new GridLayout(3, false);
		parentLayout.horizontalSpacing = 0;
		parentLayout.verticalSpacing = 0;
		parent.setLayout(parentLayout);
		createFirstRow(parent);
		createSecondRow(parent);
		createThirdRow(parent);
		addListeners();
		loadData();
	}
	
	private void createFirstRow(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		
		adauga = new Button(container, SWT.PUSH);
		adauga.setText("Adauga");
		adauga.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		adauga.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(adauga);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(adauga);
		
		salvare = new Button(container, SWT.PUSH);
		salvare.setText("Salvare");
		salvare.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		salvare.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(salvare);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(salvare);
		
		sterge = new Button(container, SWT.PUSH);
		sterge.setText("Sterge");
		sterge.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		sterge.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(sterge);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sterge);
		
		refresh = new Button(container, SWT.PUSH);
		refresh.setText("Refresh");
		refresh.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		refresh.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(refresh);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(refresh);
		
		grupeInteres = new Button(container, SWT.PUSH);
		grupeInteres.setText("Grupe Interes");
		grupeInteres.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		grupeInteres.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(grupeInteres);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grupeInteres);
	}

	private void createSecondRow(final Composite parent)
	{
		final ScrolledComposite scrollable = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollable.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		GridDataFactory.fillDefaults().grab(false, true).applyTo(scrollable);
		final Composite container = new Composite(scrollable, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		scrollable.setContent(container);
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText("Denumire TERT");
		nameLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		nameLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(nameLabel);
		GridDataFactory.swtDefaults().span(3, 1).align(SWT.CENTER, SWT.CENTER).applyTo(nameLabel);
		
		name = new Text(container, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(name);
		
		final Label cuiLabel = new Label(container, SWT.NONE);
		cuiLabel.setText("Cod fiscal");
		cuiLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		cuiLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(cuiLabel);
		GridDataFactory.swtDefaults().applyTo(cuiLabel);
		
		cui = new Text(container, SWT.BORDER);
		cui.setMessage("RO");
		UIUtils.setFont(cui);
		GridDataFactory.fillDefaults().grab(true, false).minSize(200, SWT.DEFAULT).applyTo(cui);
		
		preiaDate = new Button(container, SWT.PUSH);
		preiaDate.setText("Preia date");
		preiaDate.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		preiaDate.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(preiaDate);
		GridDataFactory.swtDefaults().applyTo(preiaDate);
		
		final Label regComLabel = new Label(container, SWT.NONE);
		regComLabel.setText("RegComert");
		regComLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		regComLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(regComLabel);
		GridDataFactory.swtDefaults().applyTo(regComLabel);
		
		regCom = new Text(container, SWT.BORDER);
		regCom.setMessage("J05/");
		UIUtils.setFont(regCom);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(regCom);
		
		final Label addresaLabel = new Label(container, SWT.NONE);
		addresaLabel.setText("Adresa facturare");
		addresaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		addresaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(addresaLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLabel);
		
		adresa = new AddressWidget(container, SWT.NONE);
		UIUtils.setFont(adresa);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresa);
		
		final Label addresaLivrareLabel = new Label(container, SWT.NONE);
		addresaLivrareLabel.setText("Adresa livrare");
		addresaLivrareLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		addresaLivrareLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(addresaLivrareLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLivrareLabel);
		
		adresaLivrare = new Text(container, SWT.BORDER);
		UIUtils.setFont(adresaLivrare);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresaLivrare);
		
		final Label phoneLabel = new Label(container, SWT.NONE);
		phoneLabel.setText("Telefon");
		phoneLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		phoneLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(phoneLabel);
		GridDataFactory.swtDefaults().applyTo(phoneLabel);
		
		telefon = new Text(container, SWT.BORDER);
		UIUtils.setFont(telefon);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(telefon);
		
		final Label emailLabel = new Label(container, SWT.NONE);
		emailLabel.setText("Email");
		emailLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		emailLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(emailLabel);
		GridDataFactory.swtDefaults().applyTo(emailLabel);
		
		email = new Text(container, SWT.BORDER);
		UIUtils.setFont(email);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(email);
		
		final Label bancaLabel = new Label(container, SWT.NONE);
		bancaLabel.setText("Banca");
		bancaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		bancaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(bancaLabel);
		GridDataFactory.swtDefaults().applyTo(bancaLabel);
		
		banca = new Text(container, SWT.BORDER);
		UIUtils.setFont(banca);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(banca);
		
		final Label contLabel = new Label(container, SWT.NONE);
		contLabel.setText("Cont");
		contLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		contLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(contLabel);
		GridDataFactory.swtDefaults().applyTo(contLabel);
		
		cont = new Text(container, SWT.BORDER);
		UIUtils.setFont(cont);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(cont);
		
		final Label delegatLabel = new Label(container, SWT.NONE);
		delegatLabel.setText("Delegat");
		delegatLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		delegatLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(delegatLabel);
		GridDataFactory.swtDefaults().applyTo(delegatLabel);
		
		delegatName = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatName);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatName);
		
		final Label cnpLabel = new Label(container, SWT.NONE);
		cnpLabel.setText("CNP");
		cnpLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		cnpLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(cnpLabel);
		GridDataFactory.swtDefaults().applyTo(cnpLabel);
		
		delegatCNP = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatCNP);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatCNP);
		
		final Label seriaLabel = new Label(container, SWT.NONE);
		seriaLabel.setText("CI seria/Nr");
		seriaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		seriaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(seriaLabel);
		GridDataFactory.swtDefaults().applyTo(seriaLabel);
		
		delegatCI = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatCI);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatCI);
		
		final Label elibLabel = new Label(container, SWT.NONE);
		elibLabel.setText("Elib de");
		elibLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		elibLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(elibLabel);
		GridDataFactory.swtDefaults().applyTo(elibLabel);
		
		delegatElib = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatElib);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatElib);
		
		final Label autoLabel = new Label(container, SWT.NONE);
		autoLabel.setText("Auto");
		autoLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		autoLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(autoLabel);
		GridDataFactory.swtDefaults().applyTo(autoLabel);
		
		delegatAuto = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatAuto);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatAuto);
		
		final Label fidelityNrLabel = new Label(container, SWT.NONE);
		fidelityNrLabel.setText("Card fidelitate");
		fidelityNrLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		fidelityNrLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(fidelityNrLabel);
		GridDataFactory.swtDefaults().applyTo(fidelityNrLabel);
		
		fidelityNumber = new Text(container, SWT.BORDER);
		UIUtils.setFont(fidelityNumber);
		GridDataFactory.fillDefaults().applyTo(fidelityNumber);
		
		autoNrCard = new Button(container, SWT.PUSH);
		autoNrCard.setText("A");
		autoNrCard.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.swtDefaults().applyTo(autoNrCard);
		
		final Label fidelityDiscLabel = new Label(container, SWT.NONE);
		fidelityDiscLabel.setText("%Discount");
		fidelityDiscLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		fidelityDiscLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(fidelityDiscLabel);
		GridDataFactory.swtDefaults().applyTo(fidelityDiscLabel);
		
		fidelityDiscountPercentage = new Text(container, SWT.BORDER);
		UIUtils.setFont(fidelityDiscountPercentage);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH/2, SWT.DEFAULT).applyTo(fidelityDiscountPercentage);
		
		final Label termenPlataLabel = new Label(container, SWT.NONE);
		termenPlataLabel.setText("Termen plata");
		termenPlataLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		termenPlataLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(termenPlataLabel);
		GridDataFactory.swtDefaults().applyTo(termenPlataLabel);
		
		termenPlata = new Text(container, SWT.BORDER);
		UIUtils.setFont(termenPlata);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(termenPlata);
		
		final Label grupaLabel = new Label(container, SWT.NONE);
		grupaLabel.setText("Grupe interes");
		grupaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		grupaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(grupaLabel);
		GridDataFactory.swtDefaults().applyTo(grupaLabel);
		
		grupa = new List(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		UIUtils.setFont(grupa);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 100).span(2, 1).applyTo(grupa);

		final Label notifyAppointmentLabel = new Label(container, SWT.NONE);
		notifyAppointmentLabel.setText("Email/SMS");
		notifyAppointmentLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		notifyAppointmentLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(notifyAppointmentLabel);
		GridDataFactory.swtDefaults().applyTo(notifyAppointmentLabel);

		notifyAppointment = new Button(container, SWT.CHECK);
		notifyAppointment.setText("Notifica Programarile");
		notifyAppointment.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		notifyAppointment.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(notifyAppointment);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(notifyAppointment);
		
		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	private void createThirdRow(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		quickSearch = new Text(container, SWT.BORDER);
		quickSearch.setMessage("Nume/Card/Telefon");
		UIUtils.setFont(quickSearch);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(quickSearch);
		
		table = new PartnerNatTable();
		table.postConstruct(container);
		table.addSelectionListener(this::handleSelection);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getTable());
		loadState(PARTNERS_TABLE_STATE_PREFIX, table.getTable(), part);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(PARTNERS_TABLE_STATE_PREFIX, table.getTable(), part);
	}
	
	@Persist
	public void onSave()
	{
		if (partnerIncarcat != null && part.isDirty())
		{
			fillPartnerIncarcat();

			if (isEmpty(partnerIncarcat.getName()))
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare", "Trebuie specificat un nume pentru partener!");
			else
			{
				final InvocationResult result = BusinessDelegate.mergePartner(partnerIncarcat);
				showResult(result);
				if (result.statusOk())
				{
					final Partner newPartner = result.extra(InvocationResult.PARTNER_KEY);
					table.replace(partnerIncarcat, newPartner);
					part.setDirty(false);
					updatePartner(newPartner);
					refreshOtherParts();
				}
			}
		}
	}
	
	private void refreshOtherParts()
	{
		partService.getParts().stream()
		.filter(p -> p.getElementId().equals(UrmarireParteneriPart.PART_ID))
		.map(MPart::getObject)
		.filter(Objects::nonNull)
		.filter(UrmarireParteneriPart.class::isInstance)
		.map(UrmarireParteneriPart.class::cast)
		.forEach(UrmarireParteneriPart::reloadPartners);
		
		partService.getParts().stream()
		.filter(p -> p.getElementId().equals(ManagerPart.PART_ID))
		.map(MPart::getObject)
		.filter(Objects::nonNull)
		.filter(ManagerPart.class::isInstance)
		.map(ManagerPart.class::cast)
		.forEach(ManagerPart::refresh);
	}
	
	private void addListeners()
	{
		quickSearch.addModifyListener(e -> table.filter(quickSearch.getText()));
		
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updatePartner(new Partner());
				part.setDirty(true);
			}
		});
		
		salvare.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		refresh.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData();
				updatePartner(null);
			}
		});
		
		grupeInteres.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (new GrupeInteresDialog(grupeInteres.getShell()).open() == Window.OK)
					loadData();
			}
		});
		
		sterge.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (partnerIncarcat != null && partnerIncarcat.getId() != null)
				{
					if (!MessageDialog.openQuestion(sterge.getShell(), "Stergeti partenerul?", "Sunteti sigur ca doriti sa stergeti partenerul selectat?"))
						return;
					
					final InvocationResult result = BusinessDelegate.deletePartner(partnerIncarcat.getId());
					showResult(result);
					if (result.statusOk())
					{
						part.setDirty(false);
						table.remove(partnerIncarcat);
						updatePartner(null);
						refreshOtherParts();
					}
				}
			}
		});
		
		preiaDate.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (partnerIncarcat != null)
				{
					final boolean dirty = part.isDirty();
					part.setDirty(false);
					fillPartnerIncarcat();
					final InvocationResult result = partnerIncarcat.getId() != null ? BusinessDelegate.verifyPartnerAtAnaf(partnerIncarcat.getId()) :
						BusinessDelegate.verifyPartnerAtAnaf(partnerIncarcat);
					showResult(result);
					if (result.statusOk())
					{
						final Partner updatedPartner = result.extra(InvocationResult.PARTNER_KEY);
						table.replace(partnerIncarcat, updatedPartner);
						updatePartner(updatedPartner);
					}
					part.setDirty(dirty);
				}
			}
		});
		
		autoNrCard.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (partnerIncarcat != null)
				{
					fidelityNumber.setText(String.valueOf(BusinessDelegate.firstFreeNumber(Partner.class, FidelityCard.NUMBER_FIELD)));
					part.setDirty(true);
				}
			}
		});
		
		final ModifyListener modifyListener = e -> 
		{
			if (!modelUpdatesUI)
				part.setDirty(true);
		};
		
		name.addModifyListener(modifyListener);
		cui.addModifyListener(modifyListener);
		regCom.addModifyListener(modifyListener);
		adresa.addModifyListener(modifyListener);
		adresaLivrare.addModifyListener(modifyListener);
		telefon.addModifyListener(modifyListener);
		email.addModifyListener(modifyListener);
		banca.addModifyListener(modifyListener);
		cont.addModifyListener(modifyListener);
		delegatName.addModifyListener(modifyListener);
		delegatCNP.addModifyListener(modifyListener);
		delegatCI.addModifyListener(modifyListener);
		delegatElib.addModifyListener(modifyListener);
		delegatAuto.addModifyListener(modifyListener);
		fidelityNumber.addModifyListener(modifyListener);
		fidelityDiscountPercentage.addModifyListener(modifyListener);
		termenPlata.addModifyListener(modifyListener);
		final SelectionAdapter selectionListener = new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (!modelUpdatesUI)
					part.setDirty(true);
			}
		};
		grupa.addSelectionListener(selectionListener);
		notifyAppointment.addSelectionListener(selectionListener);
	}
	
	private void loadData()
	{
		allGrupe = BusinessDelegate.grupeInteres_Sync();
		grupa.setItems(allGrupe.stream().map(GrupaInteres::displayName).toArray(String[]::new));
		BusinessDelegate.allPartners_InclInactive(new AsyncLoadData<Partner>()
		{
			@Override
			public void success(final ImmutableList<Partner> data)
			{
				table.loadData(data);
			}

			@Override
			public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea partenerilor", details);
			}
		}, sync);
	}
	
	private void handleSelection(final SelectionChangedEvent event)
	{
		if (!event.getStructuredSelection().isEmpty())
			updatePartner((Partner) event.getStructuredSelection().getFirstElement());
	}
	
	private void updatePartner(final Partner partner)
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Salveaza", "Salvati modificarile facute?"))
			onSave();
		part.setDirty(false);
		
		if (partner != null && partner.isInactivNullCheck())
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "ATENTIE", "In urma verificarii la ANAF, partenerul figureaza ca INACTIV!!!");
		
		modelUpdatesUI = true;
		this.partnerIncarcat = partner;
		
		name.setText(safeString(partner, Partner::getName));
		cui.setText(safeString(partner, Partner::getCodFiscal));
		regCom.setText(safeString(partner, Partner::getRegCom));
		adresa.updateModel(Optional.ofNullable(partner).map(Partner::getAddress).orElse(new Address()));
		adresaLivrare.setText(safeString(partner, Partner::getDeliveryAddress));
		telefon.setText(safeString(partner, Partner::getPhone));
		email.setText(safeString(partner, Partner::getEmail));
		banca.setText(safeString(partner, Partner::getBanca));
		cont.setText(safeString(partner, Partner::getIban));
		delegatName.setText(safeString(partner, Partner::getDelegat, Delegat::getName));
		delegatCNP.setText(safeString(partner, Partner::getDelegat, Delegat::getCnp));
		delegatCI.setText(safeString(partner, Partner::getDelegat, Delegat::getSerieCI));
		delegatElib.setText(safeString(partner, Partner::getDelegat, Delegat::getElib));
		delegatAuto.setText(safeString(partner, Partner::getDelegat, Delegat::getAuto));
		fidelityNumber.setText(safeString(partner, Partner::getFidelityCard, FidelityCard::getNumber));
		fidelityDiscountPercentage.setText(safeString(partner, Partner::getFidelityCard, FidelityCard::getDiscountPercentage, PresentationUtils::displayPercentageRaw));
		termenPlata.setText(safeString(partner, Partner::getTermenPlata, String::valueOf));
		grupa.setSelection(partner == null ? new int[] {} :
			partner.getGrupeInteres().stream().map(PartnerGrupaInteresMapping::getGrupaInteres).mapToInt(allGrupe::indexOf).toArray());
		notifyAppointment.setSelection(partner == null ? false : partner.isNotifyAppointment());
		modelUpdatesUI = false;
	}
	
	private void fillPartnerIncarcat()
	{
		if (partnerIncarcat == null)
			return;
		
		partnerIncarcat.setName(name.getText());
		partnerIncarcat.setCodFiscal(isEmpty(cui.getText()) ? null : cui.getText());
		partnerIncarcat.setRegCom(regCom.getText());
		partnerIncarcat.setAddress(adresa.getModel());
		partnerIncarcat.setDeliveryAddress(adresaLivrare.getText());
		partnerIncarcat.setPhone(telefon.getText());
		partnerIncarcat.setEmail(email.getText());
		partnerIncarcat.setBanca(banca.getText());
		partnerIncarcat.setIban(cont.getText());
		final Delegat delegat = partnerIncarcat.getDelegat() == null ? new Delegat() : partnerIncarcat.getDelegat();
		delegat.setName(delegatName.getText());
		delegat.setCnp(delegatCNP.getText());
		delegat.setSerieCI(delegatCI.getText());
		delegat.setElib(delegatElib.getText());
		delegat.setAuto(delegatAuto.getText());
		partnerIncarcat.setDelegat(delegat);
		final FidelityCard fidelityCard = partnerIncarcat.getFidelityCard() == null ? new FidelityCard() : partnerIncarcat.getFidelityCard();
		if (isEmpty(fidelityNumber.getText()))
		{
			fidelityCard.setNumber(null);
			fidelityCard.setDiscountPercentage(null);
		}
		else
		{
			fidelityCard.setNumber(fidelityNumber.getText());
			fidelityCard.setDiscountPercentage(extractPercentage(fidelityDiscountPercentage.getText()));
		}
		partnerIncarcat.setFidelityCard(fidelityCard);
		final Integer termenPlataParsed = parseToInt(termenPlata.getText());
		partnerIncarcat.setTermenPlata(termenPlataParsed > 0 ? termenPlataParsed : null);
		
		partnerIncarcat.getGrupeInteres().clear();
		partnerIncarcat.getGrupeInteres().addAll(selectedGrupeMappings(partnerIncarcat));
		
		partnerIncarcat.setNotifyAppointment(notifyAppointment.getSelection());
	}
	
	private ImmutableSet<Integer> selectedGrupeId()
	{
		final Builder<Integer> builder = ImmutableSet.<Integer>builder();
		for (final int grupaSelIndex : grupa.getSelectionIndices())
			builder.add(allGrupe.get(grupaSelIndex).getId());
		return builder.build();
	}
	
	private ImmutableSet<GrupaInteres> selectedGrupe()
	{
		final Builder<GrupaInteres> builder = ImmutableSet.<GrupaInteres>builder();
		for (final int grupaSelIndex : grupa.getSelectionIndices())
			builder.add(allGrupe.get(grupaSelIndex));
		return builder.build();
	}
	
	private ImmutableSet<PartnerGrupaInteresMapping> selectedGrupeMappings(final Partner partner)
	{
		final Builder<PartnerGrupaInteresMapping> builder = ImmutableSet.<PartnerGrupaInteresMapping>builder();
		for (final GrupaInteres selGrupa : selectedGrupe())
		{
			final PartnerGrupaInteresMapping mapping = new PartnerGrupaInteresMapping();
			mapping.setId(new PartnerGrupaInteresMappingId(partner.getId(), selGrupa.getId()));
			mapping.setGrupaInteres(selGrupa);
			mapping.setPartner(partner);
			builder.add(mapping);
		}
		return builder.build();
	}
}
