package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.EXTRA_BANNER_FONT;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.layoutNoSpaces;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.localToDisplayLocation;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setFont;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.AccountingDocumentMapping;
import ro.colibri.entities.comercial.mappings.PartnerGrupaInteresMapping;
import ro.colibri.entities.comercial.mappings.ProductGestiuneMapping;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.InvocationResult.Problem;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.GenericDataHolder;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.anaf.AnafReporter;
import ro.linic.ui.legacy.dialogs.AdaugaOpDialog;
import ro.linic.ui.legacy.dialogs.FiltrePopup;
import ro.linic.ui.legacy.dialogs.ManagerIncarcaDocPopup;
import ro.linic.ui.legacy.dialogs.PrintBarcodeDialog;
import ro.linic.ui.legacy.dialogs.PrintOfertaDiscountDialog;
import ro.linic.ui.legacy.dialogs.ScheduleDialog;
import ro.linic.ui.legacy.dialogs.SelectEntityDialog;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.OperationsNatTable;
import ro.linic.ui.legacy.widgets.ExportButton;
import ro.linic.ui.legacy.wizards.EFacturaDetailsWizardDialog;
import ro.linic.ui.legacy.wizards.EFacturaFileWizard;
import ro.linic.ui.security.services.AuthenticationSession;

public class ManagerPart implements IMouseAction
{
	public static final String PART_ID = "linic_gest_client.part.manager"; //$NON-NLS-1$
	
	private static final String OPERATIONS_TABLE_STATE_PREFIX = "manager.operations_nt"; //$NON-NLS-1$
	
	private static final int TOP_BAR_HEIGHT = 50;
	
	private static final int TIP_OP_RECEPTIE = 0;
	private static final int TIP_OP_IESIRE = 1;
	
	private static final int LEFT_BAR_BUTTON_WIDTH = 170;
	private static final int LEFT_BAR_BUTTON_HEIGHT = 60;
	
	private AccountingDocument docIncarcat;
	private ImmutableList<Partner> allPartners;
	private ImmutableList<Gestiune> allGestiuni;
	private List<GenericValue> anafInvoiceLines = List.of();
	
	// 0 - receptie, 1 - iesiri
	private int tipOp = 0;
	
	private CLabel tipOpLabel;
	private Button receptii;
	private Button iesiri;
	private Button etichete;
	private Button terti;
	private Button produse;
	private Button urmarireParteneri;
	
	private Combo doc;
	private Text nrDoc;
	private DateTime dataDoc;
	private Text nrReceptie;
	private DateTime dataReceptie;
	private Button autoNr;
	private Combo partner;
	private Combo tva;
	private Button rpz;
	private ExportButton printare;
	private Button oferta;
	
	private Button modificaAntet;
	private Button schedule;
	private Button duplicate;
	private Button filtre;
	private Button comaseazaOps;
	
	private Button docNou;
	private Button adauga;
	private Button salvare;
	private Button incarcaDoc;
	private Button refresh;
	
	private Button transfera;
	private Button stergeTot;
	private Button stergeRand;
	private CLabel docGestiune;
	
	private Text invoiceTitleText;
	private Button saveInvoiceTitle;
	
	private OperationsNatTable operationsTable;
	
	private Label achFTVA;
	private Label achTVA;
	private Label achTotal;
	private Label vanzFTVA;
	private Label vanzTVA;
	private Label vanzTotal;
	
	private FiltrePopup filtrePopup;
	
	@Inject private MPart part;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject private Logger log;
	@Inject private AuthenticationSession authSession;
	@Inject private DataServices dataServices;

	public static ManagerPart loadDocInPart(final EPartService partService, final AccountingDocument doc)
	{
		final MPart createdPart = partService.showPart(ManagerPart.PART_ID, PartState.ACTIVATE);

		if (createdPart == null)
			return null;

		final ManagerPart managerPart = (ManagerPart) createdPart.getObject();

		if (managerPart == null)
			return null;

		final AccountingDocument dbDoc = BusinessDelegate.reloadDoc(doc);
		int tipOp;
		
		if (dbDoc == null)
			throw new IllegalArgumentException(Messages.ManagerPart_DocNotFound);
		
		if (TipDoc.CUMPARARE.equals(dbDoc.getTipDoc()))
			tipOp = TIP_OP_RECEPTIE;
		else if (TipDoc.VANZARE.equals(dbDoc.getTipDoc()))
			tipOp = TIP_OP_IESIRE;
		else
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_DocTypeError, dbDoc.getTipDoc()));
		
		managerPart.updateSelectedTipOp(tipOp);
		managerPart.updateDoc(dbDoc);
		managerPart.anafInvoiceLines = List.of();
		return managerPart;
	}
	
	public static ManagerPart loadAnafInvoiceInPart(final EPartService partService, final GenericValue anafInvoice, final List<GenericValue> anafInvoiceLines,
			final Partner supplier)
	{
		final MPart createdPart = partService.showPart(ManagerPart.PART_ID, PartState.ACTIVATE);

		if (createdPart == null)
			return null;

		final ManagerPart managerPart = (ManagerPart) createdPart.getObject();

		if (managerPart == null)
			return null;

		managerPart.updateSelectedTipOp(TIP_OP_RECEPTIE);
		managerPart.updateDoc(null);
		managerPart.anafInvoiceLines = anafInvoiceLines;
		
		managerPart.doc.setText(AccountingDocument.FACTURA_NAME);
		managerPart.nrDoc.setText(anafInvoice.getString("invoiceNumber"));
		insertDate(managerPart.dataDoc, new Timestamp(anafInvoice.getLong("issueDate")).toLocalDateTime());
		managerPart.nrReceptie.setText(String.valueOf(BusinessDelegate.autoNumber(TipDoc.CUMPARARE, AccountingDocument.FACTURA_NAME, null)));
		insertDate(managerPart.dataReceptie, LocalDateTime.now());
		managerPart.partner.select(managerPart.allPartners.indexOf(supplier));
		managerPart.rpz.setSelection(true);
		managerPart.docGestiune.setText(safeString(ClientSession.instance().getLoggedUser(), User::getSelectedGestiune, Gestiune::getName));
		return managerPart;
	}
	
	/**
	 * Ask the user if he wants to delete the connected operations as well.
	 * Only children operations are deleted.
	 * If not, only delete the selected operation.
	 */
	private static ImmutableList<Operatiune> collectDeletableOperations(final ImmutableList<Operatiune> operations)
	{
		return operations.stream().flatMap(operation ->
		{
			Operatiune lastChild = operation.getChildOp();
			final List<Operatiune> deletableOps = new ArrayList<Operatiune>();

			// 1. add all child operations
			while (lastChild != null)
			{
				deletableOps.add(lastChild);
				lastChild = lastChild.getChildOp();
			}

			// 2. delete operations
			if (!deletableOps.isEmpty() && !MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					Messages.ManagerPart_DeleteConn,
					MessageFormat.format(
							Messages.ManagerPart_DeleteConnMessage,
							operation.displayName(), 
							deletableOps.size(),
							deletableOps.stream().map(Operatiune::displayName).collect(Collectors.joining(NEWLINE)),
							NEWLINE)))
			{
				deletableOps.clear();
			}
			deletableOps.add(operation);
			return deletableOps.stream();
		}).collect(toImmutableList());
	}
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		allGestiuni = BusinessDelegate.allGestiuni();
		
		final GridLayout parentLayout = new GridLayout(2, false);
		parentLayout.horizontalSpacing = 0;
		parentLayout.verticalSpacing = 0;
		parent.setLayout(parentLayout);
		createTopBar(parent);
		createLeftBar(parent);
		createMasterPart(parent);
		
		updateSelectedTipOp(TIP_OP_RECEPTIE);
		reloadPartners();
		addListeners();
	}
	
	@PreDestroy
	public void preDestroy()
	{
		if (filtrePopup != null)
			filtrePopup.cancelLoadJob();
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			BusinessDelegate.mergeOperations(operationsTable.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<Operatiune>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.collect(toImmutableSet()));
			part.setDirty(false);
			updateDoc(docIncarcat);
		}
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(OPERATIONS_TABLE_STATE_PREFIX, operationsTable.getTable(), part);
	}

	private void createTopBar(final Composite parent)
	{
		final Color bgColor = ClientSession.instance().getLoggedUser().isHideUnofficialDocs() ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				
		final Composite topBarContainer = new Composite(parent, SWT.NONE);
		final GridLayout topBarLayout = new GridLayout(4, false);
		topBarLayout.horizontalSpacing = 0;
		topBarLayout.verticalSpacing = 0;
		topBarLayout.marginWidth = 0;
		topBarLayout.marginHeight = 0;
		topBarContainer.setLayout(topBarLayout);
		final GridData topBarGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		topBarGD.horizontalSpan = 2;
		topBarContainer.setLayoutData(topBarGD);
		topBarContainer.setBackground(bgColor);
		ClientSession.instance().addHideStateControl(topBarContainer);
		
		final CLabel dataLabel = new CLabel(topBarContainer, SWT.BORDER);
		dataLabel.setText(displayLocalDate(LocalDate.now()));
		dataLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		dataLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		dataLabel.setAlignment(SWT.CENTER);
		final GridData dataGD = new GridData();
		dataGD.heightHint = TOP_BAR_HEIGHT;
		dataGD.widthHint = 200;
		dataGD.verticalAlignment = SWT.CENTER;
		dataLabel.setLayoutData(dataGD);
		UIUtils.setBoldBannerFont(dataLabel);

		final CLabel operatorLabel = new CLabel(topBarContainer, SWT.BORDER);
		operatorLabel.setText(safeString(ClientSession.instance().getLoggedUser(), User::displayName));
		operatorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		operatorLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		operatorLabel.setAlignment(SWT.CENTER);
		final GridData operatorGD = new GridData();
		operatorGD.heightHint = TOP_BAR_HEIGHT;
		operatorGD.widthHint = 300;
		operatorLabel.setLayoutData(operatorGD);
		UIUtils.setBoldBannerFont(operatorLabel);
		
		tipOpLabel = new CLabel(topBarContainer, SWT.BORDER);
		tipOpLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		tipOpLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		tipOpLabel.setAlignment(SWT.CENTER);
		final GridData tipOpGD = new GridData();
		tipOpGD.heightHint = TOP_BAR_HEIGHT;
		tipOpGD.widthHint = 200;
		tipOpLabel.setLayoutData(tipOpGD);
		UIUtils.setBoldBannerFont(tipOpLabel);
		
		final String companyGestLabel = MessageFormat.format("{0} - {1}", //$NON-NLS-1$
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedCompany, Company::displayName),
				safeString(ClientSession.instance().getLoggedUser(), User::getSelectedGestiune, Gestiune::getName));
		final CLabel gestiuneLabel = new CLabel(topBarContainer, SWT.NONE);
		gestiuneLabel.setText(companyGestLabel);
		gestiuneLabel.setBackground(bgColor);
		gestiuneLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		final GridData gestiuneGD = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gestiuneGD.heightHint = TOP_BAR_HEIGHT;
		gestiuneLabel.setLayoutData(gestiuneGD);
		UIUtils.setBoldBannerFont(gestiuneLabel);
		ClientSession.instance().addHideStateControl(gestiuneLabel);
	}
	
	private void createLeftBar(final Composite parent)
	{
		final Composite leftBarContainer = new Composite(parent, SWT.NONE);
		final GridLayout leftBarLayout = new GridLayout();
		leftBarLayout.horizontalSpacing = 0;
		leftBarLayout.verticalSpacing = 0;
		leftBarLayout.marginWidth = 0;
		leftBarLayout.marginHeight = 0;
		leftBarContainer.setLayout(leftBarLayout);
		leftBarContainer.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		leftBarContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		receptii = new Button(leftBarContainer, SWT.PUSH);
		receptii.setText(Messages.ManagerPart_Reception);
		receptii.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		receptii.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData receptiiGD = new GridData();
		receptiiGD.heightHint = LEFT_BAR_BUTTON_HEIGHT;
		receptiiGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		receptii.setLayoutData(receptiiGD);
		UIUtils.setBoldBannerFont(receptii);
		
		iesiri = new Button(leftBarContainer, SWT.PUSH);
		iesiri.setText(Messages.ManagerPart_OutgoingOps);
		iesiri.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		iesiri.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData iesiriGD = new GridData();
		iesiriGD.heightHint = LEFT_BAR_BUTTON_HEIGHT;
		iesiriGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		iesiri.setLayoutData(iesiriGD);
		UIUtils.setBoldBannerFont(iesiri);
		
		etichete = new Button(leftBarContainer, SWT.PUSH);
		etichete.setText(Messages.PrintLabels);
		etichete.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		etichete.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData eticheteGD = new GridData();
		eticheteGD.heightHint = LEFT_BAR_BUTTON_HEIGHT/2;
		eticheteGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		eticheteGD.grabExcessVerticalSpace = true;
		eticheteGD.verticalAlignment = SWT.TOP;
		etichete.setLayoutData(eticheteGD);
		
		terti = new Button(leftBarContainer, SWT.PUSH);
		terti.setText(Messages.ManagerPart_PartnerCatalog);
		terti.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		terti.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData tertiGD = new GridData();
		tertiGD.heightHint = LEFT_BAR_BUTTON_HEIGHT/2;
		tertiGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		terti.setLayoutData(tertiGD);
		UIUtils.setFont(terti);
		
		produse = new Button(leftBarContainer, SWT.PUSH);
		produse.setText(Messages.ManagerPart_ProductsCatalog);
		produse.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		produse.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData produseGD = new GridData();
		produseGD.heightHint = LEFT_BAR_BUTTON_HEIGHT/2;
		produseGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		produse.setLayoutData(produseGD);
		UIUtils.setFont(produse);
		
		urmarireParteneri = new Button(leftBarContainer, SWT.PUSH);
		urmarireParteneri.setText(Messages.ManagerPart_PartnerCRM);
		urmarireParteneri.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		urmarireParteneri.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData urmarireParteneriGD = new GridData();
		urmarireParteneriGD.heightHint = LEFT_BAR_BUTTON_HEIGHT/2;
		urmarireParteneriGD.widthHint = LEFT_BAR_BUTTON_WIDTH;
		urmarireParteneri.setLayoutData(urmarireParteneriGD);
		UIUtils.setFont(urmarireParteneri);
	}
	
	private void createMasterPart(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		final Composite masterContainer = new Composite(container, SWT.NONE);
		masterContainer.setLayout(layoutNoSpaces(new GridLayout(2, false)));
		masterContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		createMasterWidgets(masterContainer);
		
		final Composite uitContainer = new Composite(container, SWT.NONE);
		uitContainer.setLayout(layoutNoSpaces(new GridLayout(2, false)));
		createUitWidget(uitContainer);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uitContainer);
		
		docGestiune = new CLabel(container, SWT.BORDER);
		docGestiune.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		docGestiune.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		docGestiune.setAlignment(SWT.CENTER);
		final GridData dataGD = new GridData(150, 150);
		dataGD.verticalAlignment = SWT.CENTER;
		dataGD.horizontalAlignment = SWT.RIGHT;
		dataGD.grabExcessHorizontalSpace = true;
		docGestiune.setLayoutData(dataGD);
		UIUtils.setBoldCustomFont(docGestiune, EXTRA_BANNER_FONT);
		
		operationsTable = new OperationsNatTable();
		operationsTable.afterChange(op -> part.setDirty(true));
		operationsTable.doubleClickAction(this);
		operationsTable.transferRowClick(op -> 
		{
			final Operatiune opToOpen = op.getOwnerOp() != null ? op.getOwnerOp() : op.getChildOp();
			if (opToOpen != null)
			{
				int tipOp;
				
				if (TipDoc.CUMPARARE.equals(opToOpen.getAccDoc().getTipDoc()))
					tipOp = TIP_OP_RECEPTIE;
				else if (TipDoc.VANZARE.equals(opToOpen.getAccDoc().getTipDoc()))
					tipOp = TIP_OP_IESIRE;
				else
					throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_DocTypeError, opToOpen.getAccDoc().getTipDoc()));
				
				updateSelectedTipOp(tipOp);
				updateDoc(BusinessDelegate.reloadDoc(opToOpen.getAccDoc()));
				operationsTable.getSelectionProvider().setSelection(new StructuredSelection(
						docIncarcat.getOperatiuni_Stream()
						.filter(o -> o.getId().equals(opToOpen.getId()))
						.collect(toImmutableList())));
				anafInvoiceLines = List.of();
			}
		});
		operationsTable.postConstruct(container);
		operationsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(operationsTable.getTable());
		loadState(OPERATIONS_TABLE_STATE_PREFIX, operationsTable.getTable(), part);
		
		final Composite totalsContainer = new Composite(container, SWT.NONE);
		totalsContainer.setLayout(new GridLayout(6, false));
		totalsContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
		final GridData totalsGD = new GridData(SWT.FILL, SWT.TOP, true, false);
		totalsGD.horizontalSpan = 3;
		totalsContainer.setLayoutData(totalsGD);
		
		createMasterTotals(totalsContainer);
	}

	private void createUitWidget(final Composite uitContainer) {
		final Label invoiceHeaderLabel = new Label(uitContainer, SWT.NONE);
		invoiceHeaderLabel.setText(Messages.ManagerPart_InvoiceHeaderText);
		UIUtils.setFont(invoiceHeaderLabel);
		
		saveInvoiceTitle = new Button(uitContainer, SWT.PUSH | SWT.WRAP);
		saveInvoiceTitle.setText(Messages.Save);
		saveInvoiceTitle.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		saveInvoiceTitle.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(saveInvoiceTitle);
		
		invoiceTitleText = new Text(uitContainer, SWT.MULTI | SWT.BORDER);
		invoiceTitleText.setTextLimit(2000);
		UIUtils.setFont(invoiceTitleText);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(invoiceTitleText);
		invoiceTitleText.setText(BusinessDelegate.persistedProp("invoice_title").getValueOr(EMPTY_STRING));
	}

	private void createMasterWidgets(final Composite parent)
	{
		final Composite textContainer = new Composite(parent, SWT.NONE);
		textContainer.setLayout(new GridLayout(5, false));
		
		final Label docLabel = new Label(textContainer, SWT.NONE);
		docLabel.setText(Messages.ManagerPart_DocType);
		docLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(docLabel);
		
		doc = new Combo(textContainer, SWT.DROP_DOWN);
		final GridData docGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		docGD.horizontalSpan = 3;
		doc.setLayoutData(docGD);
		UIUtils.setFont(doc);
		doc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		
		printare = new ExportButton(textContainer, SWT.RIGHT, ImmutableList.of(Messages.Print, Messages.Email, "XML(UBL 2.1)", Messages.EInvoice), "down_0_inv"); //$NON-NLS-3$ //$NON-NLS-5$
		final GridData printareGD = new GridData(SWT.TOP, SWT.FILL, false, false);
		printareGD.verticalSpan = 4;
		printare.setLayoutData(printareGD);
		printare.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		printare.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(printare);
		
		final Label nrDocLabel = new Label(textContainer, SWT.NONE);
		nrDocLabel.setText(Messages.ManagerPart_NoDocDate);
		nrDocLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(nrDocLabel);
		
		nrDoc = new Text(textContainer, SWT.SINGLE | SWT.BORDER);
		nrDoc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(nrDoc);
		
		dataDoc = new DateTime(textContainer, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		dataDoc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		UIUtils.setFont(dataDoc);
		
		autoNr = new Button(textContainer, SWT.PUSH | SWT.WRAP);
		autoNr.setText(Messages.ManagerPart_AutoNo);
		final GridData autoNrGD = new GridData(SWT.TOP, SWT.FILL, false, false);
		autoNrGD.verticalSpan = 2;
		autoNr.setLayoutData(autoNrGD);
		autoNr.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		autoNr.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		UIUtils.setBoldFont(autoNr);
		
		final Label nrReceptieLabel = new Label(textContainer, SWT.NONE);
		nrReceptieLabel.setText(Messages.ManagerPart_NoRecDate);
		nrReceptieLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(nrReceptieLabel);
		
		nrReceptie = new Text(textContainer, SWT.SINGLE | SWT.BORDER);
		nrReceptie.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(nrReceptie);
		
		dataReceptie = new DateTime(textContainer, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		dataReceptie.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		UIUtils.setFont(dataReceptie);
		
		final Label partnerLabel = new Label(textContainer, SWT.NONE);
		partnerLabel.setText(Messages.ManagerPart_Partner);
		partnerLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(partnerLabel);
		
		partner = new Combo(textContainer, SWT.DROP_DOWN);
		UIUtils.setFont(partner);
		partner.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).hint(380, SWT.DEFAULT).applyTo(partner);
		
		final Label tvaLabel = new Label(textContainer, SWT.NONE);
		tvaLabel.setText(Messages.ManagerPart_VAT);
		tvaLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(tvaLabel);
		
		final BigDecimal tvaPercent = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final String tvaReadable = Operatiune.tvaReadable(tvaPercent);
		tva = new Combo(textContainer, SWT.DROP_DOWN);
		tva.setLayoutData(new GridData());
		UIUtils.setFont(tva);
		tva.setItems(new String[] {tvaReadable});
		tva.select(0);
		tva.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		
		rpz = new Button(textContainer, SWT.CHECK);
		rpz.setText(Messages.ManagerPart_DailyRep);
		rpz.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData rpzGD = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		rpzGD.horizontalSpan = 2;
		rpz.setLayoutData(rpzGD);
		setFont(rpz);
		
		oferta = new Button(textContainer, SWT.PUSH | SWT.WRAP);
		oferta.setText(Messages.ManagerPart_Offer);
		oferta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		oferta.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(oferta);
		GridDataFactory.fillDefaults().applyTo(oferta);
		
		textContainer.setTabList(new Control[] {doc, nrDoc, dataDoc, nrReceptie, dataReceptie, partner});
		
		final Composite rightBarContainer = new Composite(parent, SWT.NONE);
		rightBarContainer.setLayout(new GridLayout());
		rightBarContainer.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		modificaAntet = new Button(rightBarContainer, SWT.PUSH | SWT.WRAP);
		modificaAntet.setText(Messages.ManagerPart_ModifyLines);
		modificaAntet.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		modificaAntet.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		modificaAntet.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(modificaAntet);
		
		comaseazaOps = new Button(rightBarContainer, SWT.PUSH | SWT.WRAP);
		comaseazaOps.setText(Messages.ManagerPart_MergeLines);
		comaseazaOps.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		comaseazaOps.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		comaseazaOps.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(comaseazaOps);
		
		duplicate = new Button(rightBarContainer, SWT.PUSH | SWT.WRAP);
		duplicate.setText(Messages.ManagerPart_Duplicate);
		duplicate.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		duplicate.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		duplicate.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(duplicate);
		
		schedule = new Button(rightBarContainer, SWT.PUSH | SWT.WRAP);
		schedule.setText(Messages.Schedule);
		schedule.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		schedule.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		schedule.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(schedule);
		
		filtre = new Button(rightBarContainer, SWT.PUSH | SWT.WRAP);
		filtre.setText(Messages.ManagerPart_Filters);
		filtre.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		filtre.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		filtre.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(filtre);
		
		final Composite bottomBarContainer = new Composite(parent, SWT.NONE);
		bottomBarContainer.setLayout(new GridLayout(8, false));
		final GridData bottomBarGD = new GridData(SWT.FILL, SWT.TOP, false, false);
		bottomBarGD.horizontalSpan = 2;
		bottomBarContainer.setLayoutData(bottomBarGD);
		
		docNou = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		docNou.setText(Messages.ManagerPart_NewDoc);
		docNou.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		docNou.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(docNou);
		
		adauga = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		adauga.setText(Messages.Add);
		adauga.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		adauga.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(adauga);
		
		salvare = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		salvare.setText(Messages.Save);
		salvare.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		salvare.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(salvare);
		
		incarcaDoc = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		incarcaDoc.setText(Messages.ManagerPart_LoadDoc);
		incarcaDoc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		incarcaDoc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(incarcaDoc);
		
		refresh = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		refresh.setText(Messages.Refresh);
		refresh.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		refresh.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(refresh);
		
		transfera = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		transfera.setText(Messages.Transfer);
		transfera.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		transfera.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		transfera.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(transfera);
		
		stergeTot = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		stergeTot.setText(Messages.ManagerPart_DeleteAll);
		stergeTot.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		stergeTot.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(stergeTot);
		
		final Optional<Image> trashImage = Icons.createImage(bundle, Icons.TRASH_16X16_PATH, log);
		stergeRand = new Button(bottomBarContainer, SWT.PUSH | SWT.WRAP);
		stergeRand.setText(Messages.Delete);
		stergeRand.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		stergeRand.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		stergeRand.setImage(trashImage.orElse(null));
		UIUtils.setBoldFont(stergeRand);
		stergeRand.addDisposeListener(e -> trashImage.ifPresent(Image::dispose));
	}
	
	private void createMasterTotals(final Composite totalsContainer)
	{
		final Label achFTVALabel = new Label(totalsContainer, SWT.NONE);
		achFTVALabel.setText(Messages.ManagerPart_AqNoVAT);
		UIUtils.setFont(achFTVALabel);
		
		final int textWidth = 180;
		
		achFTVA = new Label(totalsContainer, SWT.NONE);
		final GridData achFTVAGD = new GridData();
		achFTVAGD.widthHint = textWidth;
		achFTVA.setLayoutData(achFTVAGD);
		achFTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		achFTVA.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		achFTVA.setAlignment(SWT.RIGHT);
		UIUtils.setFont(achFTVA);
		
		final Label achTVALabel = new Label(totalsContainer, SWT.NONE);
		achTVALabel.setText(Messages.ManagerPart_VATAq);
		UIUtils.setFont(achTVALabel);
		
		achTVA = new Label(totalsContainer, SWT.READ_ONLY);
		final GridData achTVAGD = new GridData();
		achTVAGD.widthHint = textWidth;
		achTVA.setLayoutData(achTVAGD);
		achTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		achTVA.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		achTVA.setAlignment(SWT.RIGHT);
		UIUtils.setFont(achTVA);
		
		final Label achTotalLabel = new Label(totalsContainer, SWT.NONE);
		achTotalLabel.setText(Messages.ManagerPart_AqTotal);
		UIUtils.setFont(achTotalLabel);
		
		achTotal = new Label(totalsContainer, SWT.READ_ONLY);
		final GridData achTotalGD = new GridData();
		achTotalGD.widthHint = textWidth;
		achTotal.setLayoutData(achTotalGD);
		achTotal.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		achTotal.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		achTotal.setAlignment(SWT.RIGHT);
		UIUtils.setFont(achTotal);
		
		final Label vanzFTVALabel = new Label(totalsContainer, SWT.NONE);
		vanzFTVALabel.setText(Messages.ManagerPart_SellNoVAT);
		UIUtils.setFont(vanzFTVALabel);
		
		vanzFTVA = new Label(totalsContainer, SWT.READ_ONLY);
		final GridData vanzFTVAGD = new GridData();
		vanzFTVAGD.widthHint = textWidth;
		vanzFTVA.setLayoutData(vanzFTVAGD);
		vanzFTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		vanzFTVA.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		vanzFTVA.setAlignment(SWT.RIGHT);
		UIUtils.setFont(vanzFTVA);
		
		final Label vanzTVALabel = new Label(totalsContainer, SWT.NONE);
		vanzTVALabel.setText(Messages.ManagerPart_SellVAT);
		UIUtils.setFont(vanzTVALabel);
		
		vanzTVA = new Label(totalsContainer, SWT.READ_ONLY);
		final GridData vanzTVAGD = new GridData();
		vanzTVAGD.widthHint = textWidth;
		vanzTVA.setLayoutData(vanzTVAGD);
		vanzTVA.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		vanzTVA.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		vanzTVA.setAlignment(SWT.RIGHT);
		UIUtils.setFont(vanzTVA);
		
		final Label vanzTotalLabel = new Label(totalsContainer, SWT.NONE);
		vanzTotalLabel.setText(Messages.ManagerPart_SellTotal);
		UIUtils.setFont(vanzTotalLabel);
		
		vanzTotal = new Label(totalsContainer, SWT.READ_ONLY);
		final GridData vanzTotalGD = new GridData();
		vanzTotalGD.widthHint = textWidth;
		vanzTotal.setLayoutData(vanzTotalGD);
		vanzTotal.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		vanzTotal.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		vanzTotal.setAlignment(SWT.RIGHT);
		UIUtils.setFont(vanzTotal);
	}
	
	private void addListeners()
	{
		receptii.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateSelectedTipOp(TIP_OP_RECEPTIE);
				anafInvoiceLines = List.of();
			}
		});
		
		iesiri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateSelectedTipOp(TIP_OP_IESIRE);
				anafInvoiceLines = List.of();
			}
		});
		
		etichete.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ImmutableList<BarcodePrintable> printables = BarcodePrintable.fromOperations(
						ImmutableList.copyOf(operationsTable.getSourceData()), bundle, log);
				
				if (!printables.isEmpty())
					new PrintBarcodeDialog(etichete.getShell(), printables, log, bundle).open();
			}
		});
		
		terti.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CatalogTertiPart.openPart(partService);
			}
		});
		
		produse.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CatalogProdusePart.openPart(partService);
			}
		});
		
		urmarireParteneri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				UrmarireParteneriPart.openPart(partService);
			}
		});
		
		autoNr.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				switch (tipOp)
				{
				case TIP_OP_RECEPTIE:
					nrReceptie.setText(String.valueOf(BusinessDelegate.autoNumber(toTipDoc(), doc.getText(), null)));
					break;
					
				case TIP_OP_IESIRE:
					final String generatedNr = String.valueOf(BusinessDelegate.autoNumber(toTipDoc(), doc.getText(), null));
					nrDoc.setText(generatedNr);
					nrReceptie.setText(generatedNr);
					break;

				default:
					throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
				}
			}
		});
		
		printare.addExportCallback(exportCode ->
		{
			switch (exportCode)
			{
			case 1: // Email
				sendDocIncarcatToEmail();
				break;
				
			case 2: // XML(UBL)
				sendDocIncarcatToXmlUbl();
				break;
				
			case 3: // eFactura
				sendDocIncarcatTo_eFactura();
				break;
			
			case 0: // Printare
			default:
				printDocIncarcat();
				break;
			}
		});
		
		oferta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (operationsTable.getSourceData().isEmpty())
					return;
				
				final ImmutableList<Product> products = BusinessDelegate.convertToProducts(operationsTable.getSourceData().stream().collect(toImmutableList()), bundle, log)
						.stream().sorted(Comparator.comparing(Product::getName))
						.collect(toImmutableList());
				if (!products.isEmpty())
					new PrintOfertaDiscountDialog(Display.getCurrent().getActiveShell(), bundle, log, products)
					.initialSelection(Optional.ofNullable(docIncarcat())
							.map(AccountingDocument::getPartner)
							.map(p -> p.getGrupeInteres().stream().findFirst().orElse(null))
							.map(PartnerGrupaInteresMapping::getGrupaInteres)
							.orElse(null))
					.open();
			}
		});
		
		modificaAntet.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				
				if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.ManagerPart_ModifyLines, Messages.ManagerPart_ModifyLinesMessage))
				{
					final ImmutableSet<Long> selectedOps = operationsTable.selection().stream().map(Operatiune::getId).collect(toImmutableSet());
					final ImmutableSet<Long> sourceOps = operationsTable.getSourceData().stream().map(Operatiune::getId).collect(toImmutableSet());
					
					final InvocationResult result = BusinessDelegate.modifyOperationAntet(toTipDoc(), doc.getText(), nrDoc.getText(), 
							extractLocalDate(dataDoc), nrReceptie.getText(), extractLocalDate(dataReceptie), selectedPartnerId(), rpz.getSelection(), 
							selectedOps.isEmpty() ? sourceOps : selectedOps);
					
					if (result.statusOk())
						updateDoc(result.extra(InvocationResult.ACCT_DOC_KEY));
					showResult(result);
				}
			}
		});
		
		schedule.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ScheduleDialog dialog = new ScheduleDialog(schedule.getShell(), sync, log, bundle, docIncarcat);
				if (dialog.open() == Window.OK)
					updateDoc(dialog.reloadedDoc());
			}
		});
		
		duplicate.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (docIncarcat == null)
					return;
				
				if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.ManagerPart_DuplicateDoc, Messages.ManagerPart_DuplicateDocMessage))
				{
					final AccountingDocument duplicateDoc = docIncarcat.duplicate();
					final Iterator<Operatiune> opIterator = docIncarcat.getOperatiuni_Stream()
							.sorted(Comparator.comparing(Operatiune::getId))
							.map(Operatiune::duplicate)
							.iterator();
					
					if (duplicateDoc.getOperatiuni().isEmpty())
						return;

					final InvocationResult result = BusinessDelegate.addOperationToUnpersistedDoc(duplicateDoc.getTipDoc(), duplicateDoc.getDoc(), duplicateDoc.getNrDoc(), 
							duplicateDoc.getDataDoc_toLocalDate(), duplicateDoc.getNrReceptie(), duplicateDoc.getDataReceptie().toLocalDate(),
							duplicateDoc.getPartner().getId(), duplicateDoc.isRpz(), opIterator.next(), null);
					
					showResult(result);
					if (result.statusOk())
					{
						AccountingDocument dbDuplicate = result.extra(InvocationResult.ACCT_DOC_KEY);
						
						while (opIterator.hasNext())
							dbDuplicate = BusinessDelegate.addOperationToDoc(dbDuplicate.getId(), opIterator.next(), null)
									.extra(InvocationResult.ACCT_DOC_KEY);
						
						dbDuplicate.setIndicatii(docIncarcat.getIndicatii());
						dbDuplicate.setPoints(docIncarcat.getPoints());
						dbDuplicate.setPhone(docIncarcat.getPhone());
						dbDuplicate.setTransportType(docIncarcat.getTransportType());
						dbDuplicate.setPayAtDriver(docIncarcat.isPayAtDriver());
						updateDoc(dbDuplicate);
						anafInvoiceLines = List.of();
					}
				}
			}
		});
		
		comaseazaOps.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				if (docIncarcat == null)
					return;

				if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.ManagerPart_MergeLines, Messages.ManagerPart_MergeLinesMessage))
				{
					final InvocationResult result = BusinessDelegate.comaseazaOpsFromDoc(docIncarcat.getId());

					if (result.statusOk())
						updateDoc(result.extra(InvocationResult.ACCT_DOC_KEY));
					showResult(result);
				}
			}
		});
		
		filtre.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				filtrePopup = new FiltrePopup(incarcaDoc.getShell(), localToDisplayLocation(operationsTable.getTable()), 
						new Point(operationsTable.getTable().getWidth(), 430),
						sync, allPartners, toTipOp(), ManagerPart.this::updateOperations, log);
				filtrePopup.open();
			}
		});
		
		docNou.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateDoc(null);
				anafInvoiceLines = List.of();
			}
		});
		
		adauga.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final boolean validToCreateDoc = !isEmpty(doc.getText()) &&
						!isEmpty(nrDoc.getText()) &&
						!isEmpty(nrReceptie.getText()) &&
						selectedPartnerId() != null;
				
				if (docIncarcat != null || validToCreateDoc)
					new AdaugaOpDialog(adauga.getShell(), localToDisplayLocation(docGestiune.getParent()), 
							sync, bundle, log, ManagerPart.this::adaugaOperatiune, ManagerPart.this::docIncarcat,
							authSession, ManagerPart.this::selectedPartnerId, anafInvoiceLines)
					.open();
			}
		});
		
		stergeRand.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ImmutableList<Operatiune> selection = ImmutableList.copyOf(operationsTable.selection());
				if (!selection.isEmpty() && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
						Messages.ManagerPart_DeleteOps, 
						NLS.bind(Messages.ManagerPart_DeleteOpsMessage, selection.size())))
					deleteOperations(selection);
			}
		});
		
		transfera.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final List<Operatiune> sel = operationsTable.selection();
				if (!sel.isEmpty() && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), 
						Messages.Transfer, 
						NLS.bind(Messages.ManagerPart_TransferMessage, sel.size())))
					transferOperations(sel);
			}
		});

		stergeTot.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (docIncarcat != null && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), 
						Messages.ManagerPart_DeleteAll, 
						NLS.bind(Messages.ManagerPart_DeleteAllMessage, docIncarcat.getOperatiuni().size())))
					deleteOperations(ImmutableList.copyOf(docIncarcat.getOperatiuni()));
			}
		});
		
		salvare.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		incarcaDoc.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				new ManagerIncarcaDocPopup(incarcaDoc.getShell(), localToDisplayLocation(docGestiune.getParent()), 
						sync, toTipDoc(), ManagerPart.this::updateDoc, log)
				.open();
			}
		});
		
		refresh.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				refresh();
			}
		});
		
		doc.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final boolean isFacturaOrAviz = doc.getText().trim().equalsIgnoreCase(AccountingDocument.FACTURA_NAME) ||
						doc.getText().trim().equalsIgnoreCase(AccountingDocument.AVIZ_NAME);
				rpz.setSelection(isFacturaOrAviz);
				rpz.setEnabled(!doc.getText().trim().equalsIgnoreCase(AccountingDocument.PROCES_VERBAL_NAME));
			}
		});
		
		saveInvoiceTitle.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				BusinessDelegate.updatePersistedProp("invoice_title", invoiceTitleText.getText());
			}
		});
		
		final KeyAdapter keyListener = new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT, e);
					e.doit = false;
				}
			}
		};
		
		doc.addKeyListener(keyListener);
		nrDoc.addKeyListener(keyListener);
		dataDoc.addKeyListener(keyListener);
		nrReceptie.addKeyListener(keyListener);
		dataReceptie.addKeyListener(keyListener);
		partner.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					doc.setFocus();
					e.doit = false;
				}
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
		nrReceptie.addFocusListener(selectAllListener);
	}
	
	@Override
	public void run(final NatTable table, final MouseEvent event)
	{
		// double click
		final Optional<AccountingDocument> selectedDoc = operationsTable.selection().stream()
				.map(Operatiune::getAccDoc)
				.findFirst();
		
		if (selectedDoc.isPresent())
			updateDoc(BusinessDelegate.reloadDoc(selectedDoc.get()));
		anafInvoiceLines = List.of();
	}
	
	private void deleteOperations(final ImmutableList<Operatiune> operations)
	{
		askSave();
		final ImmutableList<Operatiune> deletableOps = collectDeletableOperations(operations);

		final InvocationResult result = BusinessDelegate
				.deleteOperations(deletableOps.stream().map(Operatiune::getId).collect(toImmutableSet()));
		if (!result.statusOk())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
			return;
		}

		// 3. update bonCasa(may be deleted)
		updateDoc(BusinessDelegate.reloadDoc(docIncarcat));
		if (docIncarcat == null)
			anafInvoiceLines = List.of();
	}

	private void updateSelectedTipOp(final int tipOp)
	{
		this.tipOp = tipOp;
		updateDoc(null);
		
		switch (tipOp)
		{
		case TIP_OP_RECEPTIE:
			tipOpLabel.setText(Messages.ManagerPart_Incoming);
			doc.setItems(AccountingDocument.ALL_INTRARI_OPERATION_DOC_TYPES.toArray(new String[] {}));
			receptii.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
			iesiri.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
			break;
			
		case TIP_OP_IESIRE:
			tipOpLabel.setText(Messages.ManagerPart_Outgoing);
			doc.setItems(AccountingDocument.ALL_IESIRI_OPERATION_DOC_TYPES.toArray(new String[] {}));
			receptii.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
			iesiri.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
			break;

		default:
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
		}
	}
	
	/**
	 * @param newDoc nullsafe
	 */
	private void updateDoc(final AccountingDocument newDoc)
	{
		askSave();
		part.setDirty(false);
		docIncarcat = newDoc;
		final BigDecimal tvaPercent = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final String tvaReadable = Operatiune.tvaReadable(tvaPercent);
		
		doc.setText(safeString(docIncarcat, AccountingDocument::getDoc, EMPTY_STRING));
		nrDoc.setText(safeString(docIncarcat, AccountingDocument::getNrDoc));
		insertDate(dataDoc, Optional.ofNullable(docIncarcat).map(AccountingDocument::getDataDoc).orElse(LocalDateTime.now()));
		nrReceptie.setText(safeString(docIncarcat, AccountingDocument::getNrReceptie));
		insertDate(dataReceptie, Optional.ofNullable(docIncarcat).map(AccountingDocument::getDataReceptie).orElse(LocalDateTime.now()));
		selectPartner();
		tva.setText(tvaReadable);
		if (docIncarcat != null)
			rpz.setSelection(docIncarcat.isRpz());
		final String clientSelectedGest = safeString(ClientSession.instance().getLoggedUser(), User::getSelectedGestiune, Gestiune::getName);
		docGestiune.setText(safeString(docIncarcat, AccountingDocument::getGestiune, Gestiune::getName, clientSelectedGest));
		updateOperations(docIncarcat != null ?
				docIncarcat.getOperatiuni_Stream().sorted(Comparator.comparing(Operatiune::getId)).collect(toImmutableList()) :
				ImmutableList.of());
		transfera.setEnabled(doc != null && tipOp == TIP_OP_IESIRE);
		stergeTot.setEnabled(true);
	}
	
	private void updateOperations(final ImmutableList<Operatiune> operations)
	{
		// transfera and stergeTot not allowed if we filter by operations
		transfera.setEnabled(false);
		stergeTot.setEnabled(false);
		operationsTable.loadData(operations);
		final BigDecimal achizitieFaraTVA = operations.stream()
				.map(Operatiune::getValoareAchizitieFaraTVA)
				.filter(Objects::nonNull)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal achizitieTVA = operations.stream()
				.map(Operatiune::getValoareAchizitieTVA)
				.filter(Objects::nonNull)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal vanzareFaraTVA = operations.stream()
				.map(Operatiune::getValoareVanzareFaraTVA)
				.filter(Objects::nonNull)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		final BigDecimal vanzareTVA = operations.stream()
				.map(Operatiune::getValoareVanzareTVA)
				.filter(Objects::nonNull)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
		
		achFTVA.setText(displayBigDecimal(achizitieFaraTVA));
		achTVA.setText(displayBigDecimal(achizitieTVA));
		achTotal.setText(displayBigDecimal(achizitieFaraTVA.add(achizitieTVA)));
		vanzFTVA.setText(displayBigDecimal(vanzareFaraTVA));
		vanzTVA.setText(displayBigDecimal(vanzareTVA));
		vanzTotal.setText(displayBigDecimal(vanzareFaraTVA.add(vanzareTVA)));
	}
	
	public void refresh()
	{
		reloadPartners();
		updateDoc(BusinessDelegate.reloadDoc(docIncarcat));
	}
	
	private void reloadPartners()
	{
		allPartners = BusinessDelegate.allPartners();
		partner.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
		selectPartner();
	}
	
	private void selectPartner()
	{
		if (docIncarcat != null && docIncarcat.getPartner() != null)
			partner.select(allPartners.indexOf(docIncarcat.getPartner()));
		else
			partner.setText(EMPTY_STRING);
	}
	
	private InvocationResult adaugaOperatiune(final Operatiune op, final Optional<Product> p)
	{
		final InvocationResult result;
		op.setTipOp(toTipOp());
		
		// stoc warning
		final ProductGestiuneMapping stocMapping = p.map(pp -> pp.getStocuri().stream()
				.filter(stoc -> stoc.getGestiune().equals(op.getGestiune())).findFirst().get())
				.orElse(null);
		if (p.isPresent() && TipOp.IESIRE.equals(op.getTipOp()) && Product.shouldModifyStoc(p.get()) && 
				op.getCantitate().compareTo(BigDecimal.ZERO) >= 0 && op.getCantitate().compareTo(stocMapping.getStoc()) > 0)
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.ManagerPart_NegativeStock, Messages.ManagerPart_NegativeStockMessage);
		
		// select OtherGest if INTRARE Transfer
		Integer gestiuneId = null;
		if (AccountingDocument.TRANSFER_DOC_NAME.equalsIgnoreCase(doc.getText()))
		{
			final ImmutableList<Gestiune> gestiuni = allGestiuni.stream()
					.filter(g -> !g.equals(op.getGestiune()))
					.collect(toImmutableList());
			if (gestiuni.size() == 1)
				gestiuneId = gestiuni.get(0).getId();
			else
			{
				final SelectEntityDialog<Gestiune> gestiuneDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
						Messages.Transfer, Messages.ManagerPart_SelectInventory, Messages.ManagerPart_Inventory, gestiuni, Messages.OK, Messages.Cancel);
				final int dialogResult = gestiuneDialog.open();

				if (dialogResult != 0)
					return InvocationResult.canceled(Problem.code("NO_GESTIUNE"));

				gestiuneId = gestiuneDialog.selectedEntity().map(Gestiune::getId).get();
			}
		}
		
		if (docIncarcat != null)
			result = BusinessDelegate.addOperationToDoc(docIncarcat.getId(), op, gestiuneId);
		else
		{
			result = BusinessDelegate.addOperationToUnpersistedDoc(toTipDoc(), doc.getText(), nrDoc.getText(), 
					extractLocalDate(dataDoc), nrReceptie.getText(), extractLocalDate(dataReceptie),
					selectedPartnerId(), rpz.getSelection(), op, gestiuneId);
			
			if (result.statusOk() && ((AccountingDocument) result.extra(InvocationResult.ACCT_DOC_KEY)).getPartner().isInactivNullCheck())
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), Messages.Warning, Messages.ManagerPart_PartnerInactive);
		}
		
		showResult(result);
		if (result.statusOk()) {
			final AccountingDocument accDoc = result.extra(InvocationResult.ACCT_DOC_KEY);
			updateDoc(accDoc);
			updateProductSuppliers(p, op, accDoc);
			deleteOrders(p, op);
		}
		return result;
	}
	
	private void updateProductSuppliers(final Optional<Product> p, final Operatiune op, final AccountingDocument accDoc) {
		if (p.isEmpty() || !TipOp.INTRARE.equals(op.getTipOp()) || !Product.MARFA_CATEGORY.equalsIgnoreCase(op.getCategorie()) ||
				!AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
			return;
		
		final String organizationPartyId = ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName();
		RestCaller.put("/rest/s1/moqui-linic-legacy/products/suppliers")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(List.of(GenericValue.of("", "", 
						Map.of("organizationPartyId", organizationPartyId,
								"productId", p.get().getId(),
								"supplierId", accDoc.getPartner().getId(),
								"price", op.getPretUnitarAchizitieFaraTVA()))))))
				.async(t -> log.error(t));
	}

	private void deleteOrders(final Optional<Product> p, final Operatiune op) {
		if (p.isEmpty() || !TipOp.INTRARE.equals(op.getTipOp()) || !ClientSession.instance().getGestiune().equals(op.getGestiune()) ||
				NumberUtils.smallerThanOrEqual(op.getCantitate(), BigDecimal.ZERO))
			return;
		
		final GenericDataHolder ordersHolder = dataServices.holder(SupplierOrdersPart.DATA_HOLDER);
		RestCaller.delete("/rest/s1/moqui-linic-legacy/requirements")
		.internal(authSession.authentication())
		.addUrlParam("requirementId", "*")
		.addUrlParam("facilityId", ClientSession.instance().getGestiune().getImportName())
		.addUrlParam("requirementTypeEnumId", "RqTpInventory")
		.addUrlParam("statusId", "RqmtStOrdered")
		.addUrlParam("productId", p.get().getId().toString())
		.sync(GenericValue.class, t -> UIUtils.showException(t, sync))
		.ifPresent(result -> ordersHolder.getData().removeIf(gv -> gv.getInt(Product.ID_FIELD).equals(p.get().getId())));
	}

	private void transferOperations(final List<Operatiune> operations)
	{
		int gestiuneId;
		final Gestiune userGest = ClientSession.instance().getLoggedUser().getSelectedGestiune();
		final ImmutableList<Gestiune> gestiuni = BusinessDelegate.allGestiuni().stream()
				.filter(g -> !g.equals(userGest))
				.collect(toImmutableList());
		if (gestiuni.size() == 1)
			gestiuneId = gestiuni.get(0).getId();
		else
		{
			final SelectEntityDialog<Gestiune> gestiuneDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
					Messages.Transfer, Messages.ManagerPart_SelectInventory, Messages.ManagerPart_Inventory, gestiuni, Messages.OK, Messages.Cancel);
			final int dialogResult = gestiuneDialog.open();
			
			if (dialogResult != 0)
				return;
			
			gestiuneId = gestiuneDialog.selectedEntity().map(Gestiune::getId).get();
		}
		
		final InvocationResult result = BusinessDelegate
				.transferOperations(operations.stream().map(Operatiune::getId).collect(toImmutableSet()), gestiuneId);
		if (!result.statusOk())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), result.toTextCodes(),
					result.toTextDescription());
			return;
		}

		// 3. update bonCasa and reload affected products
		updateDoc(BusinessDelegate.reloadDoc(docIncarcat));
	}
	
	private void printDocIncarcat()
	{
		if (docIncarcat == null)
			return;
		
		try
		{
			switch (tipOp)
			{
			case TIP_OP_RECEPTIE:
				JasperReportManager.instance(bundle, log).printReceptie(bundle, docIncarcat);
				break;

			case TIP_OP_IESIRE:
				if (docIncarcat.isOfficialVanzariDoc())
				{
					if (isEmpty(safeString(docIncarcat.getPartner(), Partner::getDelegat, Delegat::getName)))
					{
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_MissingDelegate, Messages.ManagerPart_MissingDelegateMessage);
						return;
					}
						
					JasperReportManager.instance(bundle, log).printFactura(bundle, docIncarcat, docIncarcat.getPaidBy().stream().map(AccountingDocumentMapping::getPays).findFirst().orElse(null));
				}
				else
					JasperReportManager.instance(bundle, log).printNonOfficialDoc(bundle, docIncarcat, true);
				break;

			default:
				throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
			}
		}
		catch (final IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
	
	private void sendDocIncarcatToEmail()
	{
		if (docIncarcat == null)
			return;
		
		try
		{
			switch (tipOp)
			{
			case TIP_OP_RECEPTIE:
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessage);
				break;

			case TIP_OP_IESIRE:
				if (docIncarcat.isOfficialVanzariDoc())
				{
					if (isEmpty(safeString(docIncarcat.getPartner(), Partner::getDelegat, Delegat::getName)))
					{
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_MissingDelegate, Messages.ManagerPart_MissingDelegateMessage);
						return;
					}
					
					final boolean hasMailConfigured = Boolean.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
							.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
					if (!hasMailConfigured)
					{
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.Error, Messages.ManagerPart_MailSMTPErr);
						return;
					}
					
					JasperReportManager.instance(bundle, log).printFactura_ClientDuplicate(bundle, docIncarcat,
							docIncarcat.getPaidBy().stream().map(AccountingDocumentMapping::getPays).findFirst().orElse(null));
				}
				else
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessage);
				break;

			default:
				throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
			}
		}
		catch (final IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
	}
	
	private void sendDocIncarcatToXmlUbl()
	{
		if (docIncarcat == null)
			return;

		switch (tipOp)
		{
		case TIP_OP_RECEPTIE:
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessageXml);
			break;

		case TIP_OP_IESIRE:
			if (docIncarcat.isOfficialVanzariDoc())
			{
				if (isEmpty(safeString(docIncarcat.getPartner(), Partner::getDelegat, Delegat::getName)))
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_MissingDelegate, Messages.ManagerPart_MissingDelegateMessage);
					return;
				}

				final FileDialog chooser = new FileDialog(printare.getShell(), SWT.SAVE);
				chooser.setFileName(Messages.Invoice_+docIncarcat.getNrDoc()+".xml"); //$NON-NLS-2$
				final String filepath = chooser.open();
				
				if (isEmpty(filepath))
					return;
				
				new EFacturaDetailsWizardDialog(Display.getCurrent().getActiveShell(),
						new EFacturaFileWizard(log, docIncarcat, filepath)).open();
			}
			else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessageXml);
			break;

		default:
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
		}
	}
	
	private void sendDocIncarcatTo_eFactura()
	{
		if (docIncarcat == null)
			return;

		switch (tipOp)
		{
		case TIP_OP_RECEPTIE:
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessageAnaf);
			break;

		case TIP_OP_IESIRE:
			if (TipDoc.VANZARE.equals(docIncarcat.getTipDoc()) &&
					AccountingDocument.FACTURA_NAME.equalsIgnoreCase(docIncarcat.getDoc()))
			{
				if (isEmpty(safeString(docIncarcat.getPartner(), Partner::getDelegat, Delegat::getName)))
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_MissingDelegate, Messages.ManagerPart_MissingDelegateMessage);
					return;
				}
				
				if (!MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.ManagerPart_Report, 
						MessageFormat.format(Messages.ManagerPart_ReportMessage, docIncarcat.displayNameShort())))
					return;

				AnafReporter.reportInvoice(docIncarcat.getCompany().getId(), docIncarcat.getId());
			}
			else
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerPart_WrongDoc, Messages.ManagerPart_WrongDocMessageAnaf);
			break;

		default:
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
		}
	}
	
	private TipDoc toTipDoc()
	{
		switch (tipOp)
		{
		case TIP_OP_RECEPTIE:
			return TipDoc.CUMPARARE;
			
		case TIP_OP_IESIRE:
			return TipDoc.VANZARE;

		default:
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
		}
	}
	
	private TipOp toTipOp()
	{
		switch (tipOp)
		{
		case TIP_OP_RECEPTIE:
			return TipOp.INTRARE;
			
		case TIP_OP_IESIRE:
			return TipOp.IESIRE;

		default:
			throw new UnsupportedOperationException(NLS.bind(Messages.ManagerPart_OpTypeNotImpl, tipOp));
		}
	}
	
	/**
	 * @return can return null
	 */
	private Long selectedPartnerId()
	{
		return allPartners.stream()
				.filter(p -> globalIsMatch(partner.getText(), p.getName(), TextFilterMethod.EQUALS))
				.map(Partner::getId)
				.findFirst()
				.orElse(null);
	}
	
	private AccountingDocument docIncarcat()
	{
		return docIncarcat;
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.Save, Messages.SaveMessage))
			onSave();
	}
}
