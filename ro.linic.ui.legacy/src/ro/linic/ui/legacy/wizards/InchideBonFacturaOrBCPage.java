package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.ListUtils.replaceInImmutableList;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.smallerThanOrEqual;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.Product;
import ro.colibri.security.Permissions;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ListUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.mapper.AccDocMapper;
import ro.linic.ui.legacy.service.CasaMarcat;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.services.ECRService;

public class InchideBonFacturaOrBCPage extends WizardPage
{
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> jobHandle;
	private AccountingDocument bonCasa;
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	private boolean casaActive;
	private ECRService ecrService;
	
	private ImmutableList<Partner> allPartners;
	
	private Combo tipDoc;
	private Combo partner;
	private Button abandon;
	private Button inchide;
	private Text total;
	private Button addFidelityPoints;
	private Text achitat;
	private Combo contBancar;
	private Text paidDocNr;
	
	private Text delegatName;
	private Text delegatCNP;
	private Text delegatCI;
	private Text delegatElib;
	private Text delegatAuto;
	private Label saveDelegatLabel;
	private Button saveDelegat;
	
	private boolean partnersAreUpdating = false;
	private ImmutableList<ContBancar> allConturiBancare = ImmutableList.of();
	
	public InchideBonFacturaOrBCPage(final AccountingDocument bonCasa, final boolean casaActive, final IEclipseContext ctx,
			final Bundle bundle, final Logger log)
	{
		super("InchideBonFacturaOrBCPage");
		this.bonCasa = bonCasa;
		this.sync = ctx.get(UISynchronize.class);
		this.ecrService = ctx.get(ECRService.class);
		this.bundle = bundle;
		this.log = log;
		this.casaActive = casaActive;
		allConturiBancare = BusinessDelegate.allConturiBancare();
	}

	@Override
	public void createControl(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(6, false);
		container.setLayout(layout);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		final Label tipDocLabel = new Label(container, SWT.NONE);
		tipDocLabel.setText("Tip Doc");
		tipDocLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		tipDocLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		tipDocLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		tipDoc = new Combo(container, SWT.DROP_DOWN);
		final GridData tipDocGD = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		tipDocGD.minimumWidth = InchideBonWizard.DROPDOWN_WIDTH;
		tipDocGD.horizontalSpan = 3;
		tipDoc.setLayoutData(tipDocGD);
		UIUtils.setFont(tipDoc);
		tipDoc.setItems(AccountingDocument.VANZARE_NO_BONCASA_DOC_TYPES.toArray(new String[] {}));
		tipDoc.select(AccountingDocument.VANZARE_NO_BONCASA_DOC_TYPES.indexOf(AccountingDocument.FACTURA_NAME));
		
		createDelegatComposite(container);
		
		abandon = new Button(container, SWT.PUSH);
		abandon.setText("Abandon");
		final GridData abandonGD = new GridData(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT);
		abandonGD.horizontalAlignment = SWT.RIGHT;
		abandonGD.grabExcessHorizontalSpace = false;
		abandonGD.verticalSpan = 5;
		abandon.setLayoutData(abandonGD);
		abandon.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		abandon.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(abandon);
		
		final Label partnerLabel = new Label(container, SWT.NONE);
		partnerLabel.setText("Partener");
		partnerLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		partnerLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		partnerLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		partner = new Combo(container, SWT.DROP_DOWN);
		final GridData partnerGD = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		partnerGD.minimumWidth = InchideBonWizard.DROPDOWN_WIDTH;
		partnerGD.horizontalSpan = 3;
		partner.setLayoutData(partnerGD);
		UIUtils.setFont(partner);
		
		final Label totalLabel = new Label(container, SWT.NONE);
		totalLabel.setText("Total");
		totalLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		totalLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		totalLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		total = new Text(container, SWT.READ_ONLY | SWT.BORDER);
		final GridData totalGD = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		totalGD.widthHint = InchideBonWizard.READ_ONLY_TEXT_WIDTH;
		total.setLayoutData(totalGD);
		UIUtils.setBoldFont(total);
		updateTotal(bonCasa.getPartner());
		
		addFidelityPoints = new Button(container, SWT.CHECK);
		addFidelityPoints.setText("Adauga Puncte de Fidelitate");
		addFidelityPoints.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		addFidelityPoints.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(addFidelityPoints);
		
		final Label achitatLabel = new Label(container, SWT.NONE);
		achitatLabel.setText("Achitat");
		achitatLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		achitatLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		achitatLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		achitat = new Text(container, SWT.SINGLE | SWT.BORDER);
		final GridData achitatGD = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		achitatGD.widthHint = InchideBonWizard.READ_ONLY_TEXT_WIDTH;
		achitat.setLayoutData(achitatGD);
		UIUtils.setBoldFont(achitat);
//		achitat.setText(displayBigDecimal(bonCasa.getTotal()));
		
		contBancar = new Combo(container, SWT.DROP_DOWN);
		contBancar.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		UIUtils.setFont(contBancar);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(contBancar);
		
		paidDocNr = new Text(container, SWT.SINGLE | SWT.BORDER);
		paidDocNr.setMessage("Nr chitanta bancara");
		UIUtils.setFont(paidDocNr);
		refreshPaidDocNrEnablement();
		
		inchide = new Button(container, SWT.PUSH);
		inchide.setText("Inchide si printeaza documentul - F3");
		final GridData inchideGD = new GridData();
		inchideGD.horizontalSpan = 4;
		inchideGD.grabExcessHorizontalSpace = true;
		inchideGD.horizontalAlignment = SWT.FILL;
		inchide.setLayoutData(inchideGD);
		inchide.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchide.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setBoldBannerFont(inchide);
		
		container.setTabList(new Control[]{tipDoc, partner, achitat, inchide, abandon});
		
		// required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
		
		addListeners();
		reloadPartners();
		refreshAddDiscountEnablement();
	}
	
	private void createDelegatComposite(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		final GridData containerGD = new GridData();
		containerGD.verticalSpan = 5;
		containerGD.horizontalAlignment = SWT.RIGHT;
		containerGD.widthHint = InchideBonWizard.DELEGAT_CONTAINER_WIDTH;
		containerGD.heightHint = InchideBonWizard.DELEGAT_CONTAINER_HEIGHT;
		container.setLayoutData(containerGD);
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText("Delegat");
		nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		nameLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		nameLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setFont(nameLabel);
		
		delegatName = new Text(container, SWT.SINGLE);
		final GridData nameGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		delegatName.setLayoutData(nameGD);
		UIUtils.setFont(delegatName);
		
		final Label cnpLabel = new Label(container, SWT.NONE);
		cnpLabel.setText("CNP");
		cnpLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		cnpLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		cnpLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setFont(cnpLabel);
		
		delegatCNP = new Text(container, SWT.SINGLE);
		final GridData cnpGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		delegatCNP.setLayoutData(cnpGD);
		UIUtils.setFont(delegatCNP);
		
		final Label serieCILabel = new Label(container, SWT.NONE);
		serieCILabel.setText("CI");
		serieCILabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		serieCILabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		serieCILabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setFont(serieCILabel);
		
		delegatCI = new Text(container, SWT.SINGLE);
		final GridData ciGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		delegatCI.setLayoutData(ciGD);
		UIUtils.setFont(delegatCI);
		
		final Label elibLabel = new Label(container, SWT.NONE);
		elibLabel.setText("Elib");
		elibLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		elibLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		elibLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setFont(elibLabel);
		
		delegatElib = new Text(container, SWT.SINGLE);
		final GridData elibGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		delegatElib.setLayoutData(elibGD);
		UIUtils.setFont(delegatElib);
		
		final Label autoLabel = new Label(container, SWT.NONE);
		autoLabel.setText("Auto");
		autoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		autoLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		autoLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		UIUtils.setFont(autoLabel);
		
		delegatAuto = new Text(container, SWT.SINGLE);
		final GridData eutoGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		delegatAuto.setLayoutData(eutoGD);
		UIUtils.setFont(delegatAuto);
		
		saveDelegatLabel = new Label(container, SWT.NONE);
		saveDelegatLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		saveDelegatLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		saveDelegatLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		
		saveDelegat = new Button(container, SWT.PUSH);
		final GridData saveGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		saveDelegat.setLayoutData(saveGD);
		saveDelegat.setText("Salvare delegat");
		saveDelegat.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		saveDelegat.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		UIUtils.setFont(saveDelegat);
		
		updateDelegatFields();
	}
	
	private void updateDelegatFields()
	{
		delegatName.setText(safeString(bonCasa.getPartner(), Partner::getDelegat, Delegat::getName));
		delegatCNP.setText(safeString(bonCasa.getPartner(), Partner::getDelegat, Delegat::getCnp));
		delegatCI.setText(safeString(bonCasa.getPartner(), Partner::getDelegat, Delegat::getSerieCI));
		delegatElib.setText(safeString(bonCasa.getPartner(), Partner::getDelegat, Delegat::getElib));
		delegatAuto.setText(safeString(bonCasa.getPartner(), Partner::getDelegat, Delegat::getAuto));
		saveDelegat.setEnabled(ClientSession.instance().hasPermission(Permissions.UPDATE_DELEGAT));
	}
	
	private void reloadPartners()
	{
		partnersAreUpdating = true;
		allPartners = BusinessDelegate.allPartners();
		partner.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
		if (bonCasa.getPartner() != null)
			partner.select(allPartners.indexOf(bonCasa.getPartner()));
		partnersAreUpdating = false;
	}
	
	private void replacePartner(final Partner old, final Partner newP)
	{
		partnersAreUpdating = true;
		allPartners = replaceInImmutableList(allPartners, old, newP);
		partner.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
		if (Objects.equals(bonCasa.getPartner(), old))
			bonCasa.setPartner(newP);
		if (bonCasa.getPartner() != null)
			partner.select(allPartners.indexOf(bonCasa.getPartner()));
		partnersAreUpdating = false;
	}
	
	private void closeDoc()
	{
		if (isEmpty(tipDoc.getText()))
		{
			setErrorMessage("Selectati tipul documentului!");
			return;
		}
		
		final boolean isFactura = globalIsMatch(tipDoc.getText(), AccountingDocument.FACTURA_NAME, TextFilterMethod.EQUALS);
		
		if (isFactura)
		{
			if (bonCasa.getPartner() == null)
			{
				setErrorMessage("Pentru factura trebuie selectat un partener!");
				return;
			}
			if (isEmpty(safeString(bonCasa.getPartner().getDelegat(), Delegat::getName)))
			{
				setErrorMessage("Introduceti un Delegat!");
				return;
			}
			
			// check partner at ANAF if factura
			final InvocationResult result = BusinessDelegate.verifyPartnerAtAnaf(bonCasa.getPartner().getId());
			if (result.statusCanceled())
			{
				final int buttonIndex = MessageDialog.open(MessageDialog.QUESTION, getShell(), result.toTextCodes(), result.toTextDescription(), SWT.NONE, "Continuare", "Abandon");
				if(buttonIndex != 0)
					return;
			}
			else if (((Partner) result.extra(InvocationResult.PARTNER_KEY)).isInactivNullCheck())
			{
				final int buttonIndex = MessageDialog.open(MessageDialog.QUESTION, getShell(), "ATENTIE", "In urma verificarii la ANAF, partenerul figureaza ca INACTIV!!!", SWT.NONE, "Continuare", "Abandon");
				if(buttonIndex != 0)
					return;
			}
		}
		
		// close doc
		final InvocationResult result = BusinessDelegate.closeFacturaBCAviz(bonCasa.getId(), tipDoc.getText(), parse(achitat.getText()), 
				contBancar(), casaActive, paidDocNr(), addFidelityPoints.getSelection());
		AccountingDocument docToPrint = null;
		AccountingDocument chitantaToPrint = null;
		
		if (result.statusOk() || globalIsMatch(result.toTextCodes(), InvocationResult.DOC_INCHIS_ERR, TextFilterMethod.CONTAINS))
		{
			docToPrint = result.extra(InvocationResult.ACCT_DOC_KEY);
			chitantaToPrint = result.extra(InvocationResult.CHITANTA_KEY);
		}
		else
		{
			setErrorMessage(result.toTextDescriptionWithCode());
			return;
		}
		
		final List<AccountingDocument> bonuriCasa = result.extra(InvocationResult.BONURI_CASA_KEY);
		try
		{
			// scoate Bon Fiscal, daca e cazul
			if (casaActive)
				ecrService.printReceipt(AccDocMapper.toReceipt(bonuriCasa),
						contBancar() != null ? PaymentType.CARD : PaymentType.CASH)
				.thenAcceptAsync(new CasaMarcat.UpdateDocStatus(bonuriCasa.stream()
						.map(AccountingDocument::getId).collect(Collectors.toSet()), false));
		}
		catch (final Exception ex)
		{
			log.error(ex);
			showException(ex, "Eroare in printarea bonului de casa. Scoateti bonul de casa manual!");
			BusinessDelegate.closeBonCasa_Failed(bonuriCasa.stream()
					.map(AccountingDocument::getId).collect(ListUtils.toImmutableSet()));
		}
		
		// print doc and chitanta
		if (docToPrint != null)
		{
			try
			{
				docToPrint.setOperatiuni(new HashSet<>(AccountingDocument.extractOperations(docToPrint)));
				if (isFactura || globalIsMatch(tipDoc.getText(), AccountingDocument.AVIZ_NAME, TextFilterMethod.EQUALS))
					JasperReportManager.instance(bundle, log).printFactura(bundle, docToPrint, chitantaToPrint);
				else
					JasperReportManager.instance(bundle, log).printNonOfficialDoc(bundle, docToPrint, false);
			}
			catch (IOException | JRException e)
			{
				log.error(e);
			}
		}
		
		setPageComplete(true);
		setErrorMessage(null);
		((InchideBonWizardDialog) getWizard().getContainer()).finishPressed();
	}
	
	public ContBancar contBancar()
	{
		final int index = contBancar.getSelectionIndex();
		if (index == -1)
			return null;
		
		return allConturiBancare.get(index);
	}
	
	public String paidDocNr()
	{
		return contBancar() != null ? paidDocNr.getText() : null;
	}
	
	private void refreshPaidDocNrEnablement()
	{
		paidDocNr.setEditable(contBancar() != null);
	}
	
	private void refreshAddDiscountEnablement()
	{
		final boolean canAddDiscount = canAddDiscount();
		addFidelityPoints.setEnabled(canAddDiscount);
		addFidelityPoints.setSelection(canAddDiscount);
	}

	private void addListeners()
	{
		inchide.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				closeDoc();
			}
		});
		
		final KeyAdapter closeDocKeyListener = new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.F3)
					closeDoc();
			}
		};
		tipDoc.addKeyListener(closeDocKeyListener);
		partner.addKeyListener(closeDocKeyListener);
		achitat.addKeyListener(closeDocKeyListener);
		contBancar.addKeyListener(closeDocKeyListener);
		
		abandon.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				((InchideBonWizardDialog) getWizard().getContainer()).cancelPressed();
			}
		});
		
		partner.addModifyListener(e ->
		{
			if (!partnersAreUpdating)
			{
				if (jobHandle != null)
					jobHandle.cancel(false);
				
				jobHandle = executorService.schedule(() ->
				{
					sync.syncExec(() ->
					{
						final Optional<Partner> newPartner = selectedPartner();
						if (Objects.equals(newPartner.orElse(null), bonCasa.getPartner()))
							return;
						
						bonCasa.setPartner(newPartner.orElse(null));
						final InvocationResult result = BusinessDelegate.changeDocPartner(bonCasa.getId(), newPartner.map(Partner::getId), false);
						setErrorMessage(result.statusOk() ? null : result.toTextDescriptionWithCode());
						updateDelegatFields();
						refreshAddDiscountEnablement();
						updateTotal(newPartner.orElse(null));
					});
				}, 200, TimeUnit.MILLISECONDS); // on average it takes 40-60ms between calls when scrolling
			}
		});
		
		achitat.addModifyListener(e -> refreshAddDiscountEnablement());
		
//		tipDoc.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				if (globalIsMatch(tipDoc.getText(), AccountingDocument.FACTURA_NAME, TextFilterMethod.EQUALS))
//					achitat.setText(displayBigDecimal(bonCasa.getTotal()));
//				else
//					achitat.setText("0");
//			}
//		});
		
		saveDelegat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (bonCasa.getPartner() != null)
				{
					final Delegat delegat = bonCasa.getPartner().getDelegat() == null ? new Delegat() : bonCasa.getPartner().getDelegat();
					delegat.setName(delegatName.getText());
					delegat.setCnp(delegatCNP.getText());
					delegat.setSerieCI(delegatCI.getText());
					delegat.setElib(delegatElib.getText());
					delegat.setAuto(delegatAuto.getText());
					
					final InvocationResult result = BusinessDelegate.updateDelegat(bonCasa.getPartner().getId(), delegat);
					showResult(result);
					if (result.statusOk())
					{
						replacePartner(bonCasa.getPartner(), result.extra(InvocationResult.PARTNER_KEY));
						saveDelegatLabel.setImage(GUIHelper.getImage("checked")); // nattable image
						saveDelegatLabel.requestLayout();
					}
				}
			}
		});
		
		contBancar.addModifyListener(e -> refreshPaidDocNrEnablement());
	}
	
	public boolean canAddDiscount()
	{
		if (!ClientSession.instance().hasPermission(Permissions.ADD_CLIENT_DOCS))
			return false;
		
		if (smallerThanOrEqual(parse(achitat.getText()), BigDecimal.ZERO))
			return false;
		
		final Partner selPartner = bonCasa.getPartner();
		return selPartner != null && selPartner.hasFidelityCard();
	}
	
	private void updateTotal(final Partner partner)
	{
		if (partner != null && !bonCasa.getOperatiuni_Stream().filter(Product::isDiscount).findAny().isPresent())
			total.setText(displayBigDecimal(bonCasa.getCalculatedTotalAfterDiscount(partner.discountGrupeInteres(), partner.cappedAdaosGrupeInteres())));
		else
			total.setText(displayBigDecimal(bonCasa.getTotal(), 2, RoundingMode.HALF_EVEN));
	}
	
	private Optional<Partner> selectedPartner()
	{
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
	
		return Optional.of(allPartners.get(index));
	}
}
