package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.flexbiz.util.commons.StringUtils.sanitizePhoneNumber;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.flexbiz.util.commons.StringUtils;
import ro.linic.ui.base.services.UtilServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.widgets.AddressWidget;
import ro.linic.ui.security.services.AuthenticationSession;

public class AdaugaPartenerColibriDialog extends TitleAreaDialog {
	private static final int TEXT_WIDTH = 150;

	private Button persoanaFizica;
	private Button persoanaJuridica;
	private Text name;
	private Text telefon;
	private Text partnerCode;
	private Text cui;
	private Button preiaDate;
	private Text regCom;
	private AddressWidget adresa;
	private Text adresaLivrare;
	private Text delegatName;

	private Bundle bundle;
	private Logger log;
	private IEclipseContext ctx;

	public AdaugaPartenerColibriDialog(final Shell parent, final Bundle bundle, final Logger log, final IEclipseContext ctx) {
		super(parent);
		this.bundle = bundle;
		this.log = log;
		this.ctx = ctx;
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.AdaugaPartnerDialog_Title);
		setMessage(Messages.AdaugaPartnerDialog_Message);
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		final Label partnerTypeLabel = new Label(container, SWT.NONE);
		partnerTypeLabel.setText(Messages.AdaugaPartnerDialog_PartnerType);
		UIUtils.setFont(partnerTypeLabel);

		persoanaFizica = new Button(container, SWT.RADIO);
		persoanaFizica.setText(Messages.AdaugaPartnerDialog_Individual);
		UIUtils.setBoldFont(persoanaFizica);
		GridDataFactory.swtDefaults().applyTo(persoanaFizica);

		persoanaJuridica = new Button(container, SWT.RADIO);
		persoanaJuridica.setText(Messages.AdaugaPartnerDialog_Company);
		UIUtils.setBoldFont(persoanaJuridica);
		GridDataFactory.swtDefaults().applyTo(persoanaJuridica);

		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText(Messages.AdaugaPartnerDialog_Name);
		UIUtils.setBoldFont(nameLabel);
		GridDataFactory.swtDefaults().applyTo(nameLabel);

		name = new Text(container, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(name);
		
		final Label phoneLabel = new Label(container, SWT.NONE);
		phoneLabel.setText(Messages.AdaugaPartnerDialog_Phone);
		UIUtils.setBoldFont(phoneLabel);
		GridDataFactory.swtDefaults().applyTo(phoneLabel);

		telefon = new Text(container, SWT.BORDER);
		UIUtils.setFont(telefon);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(telefon);
		
		final Label partnerCodeLabel = new Label(container, SWT.NONE);
		partnerCodeLabel.setText(Messages.AdaugaPartnerColibriDialog_PartnerCode);
		UIUtils.setBoldFont(partnerCodeLabel);
		GridDataFactory.swtDefaults().applyTo(partnerCodeLabel);

		partnerCode = new Text(container, SWT.BORDER);
		UIUtils.setFont(partnerCode);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(partnerCode);

		final Label cuiLabel = new Label(container, SWT.NONE);
		cuiLabel.setText(Messages.AdaugaPartnerDialog_TaxCode);
		UIUtils.setFont(cuiLabel);
		GridDataFactory.swtDefaults().applyTo(cuiLabel);

		cui = new Text(container, SWT.BORDER);
		cui.setMessage("RO");
		UIUtils.setFont(cui);
		GridDataFactory.swtDefaults().hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(cui);

		preiaDate = new Button(container, SWT.PUSH);
		preiaDate.setText(Messages.AdaugaPartnerDialog_RetrieveInfo);
		preiaDate.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		preiaDate.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(preiaDate);
		GridDataFactory.swtDefaults().applyTo(preiaDate);

		final Label regComLabel = new Label(container, SWT.NONE);
		regComLabel.setText(Messages.AdaugaPartnerDialog_RegistrationId);
		UIUtils.setFont(regComLabel);
		GridDataFactory.swtDefaults().applyTo(regComLabel);

		regCom = new Text(container, SWT.BORDER);
		regCom.setMessage("J05/");
		UIUtils.setFont(regCom);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(regCom);

		final Label addresaLabel = new Label(container, SWT.NONE);
		addresaLabel.setText(Messages.AdaugaPartnerDialog_BillingAddress);
		UIUtils.setFont(addresaLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLabel);

		adresa = new AddressWidget(container, SWT.NONE);
		UIUtils.setFont(adresa);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresa);
		adresa.getModel().setOras("Marghita");
		adresa.getModel().setCountry("RO");
		adresa.getModel().setJudet("RO-BH");

		final Label addresaLivrareLabel = new Label(container, SWT.NONE);
		addresaLivrareLabel.setText(Messages.AdaugaPartnerDialog_DeliveryAddress);
		UIUtils.setFont(addresaLivrareLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLivrareLabel);

		adresaLivrare = new Text(container, SWT.BORDER);
		UIUtils.setFont(adresaLivrare);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresaLivrare);

		final Label delegatLabel = new Label(container, SWT.NONE);
		delegatLabel.setText(Messages.AdaugaPartnerDialog_Delegate);
		UIUtils.setFont(delegatLabel);
		GridDataFactory.swtDefaults().applyTo(delegatLabel);

		delegatName = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatName);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatName);

		updateFieldVisibility();
		addListeners();
		return area;
	}

	private void addListeners() {
		persoanaFizica.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateFieldVisibility();
			}
		});

		persoanaJuridica.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateFieldVisibility();
			}
		});

		preiaDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Partner p = new Partner();
				p.setCodFiscal(isEmpty(cui.getText()) ? null : cui.getText());
				final InvocationResult result = BusinessDelegate.verifyPartnerAtAnaf(p);
				if (result.statusOk()) {
					setErrorMessage(null);
					final Partner updatedPartner = result.extra(InvocationResult.PARTNER_KEY);
					updateFields(updatedPartner);
				} else
					setErrorMessage(result.toTextDescriptionWithCode());
			}
		});
	}

	@Override
	protected void okPressed() {
		if (isEmpty(name.getText()))
			setErrorMessage(Messages.AdaugaPartnerDialog_EmptyNameError);
		else {
			final InvocationResult result = BusinessDelegate.mergePartner(partnerFromFields());
			if (result.statusOk()) {
				final Partner newPartner = result.extra(InvocationResult.PARTNER_KEY);
				final Map<String, Object> partnerBody = Map.of("partyId", newPartner.getId(),
						"partnerCode", partnerCode.getText(),
						"isPerson", !isPersJuridica(),
						"name", name.getText(),
						"phone", telefon.getText());

				if (RestCaller.post("/rest/s1/moqui-linic-legacy/partners")
						.internal(ctx.get(AuthenticationSession.class).authentication())
						.body(BodyProvider.of(HttpUtils.toJSON(partnerBody)))
						.sync(GenericValue.class, t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
						.isPresent() && StringUtils.notEmpty(telefon.getText())) {
					sendWelcomeSMSSequence(telefon.getText(), name.getText(), partnerCode.getText());
				}
				
				if (!isPersJuridica())
					try {
						JasperReportManager.instance(bundle, log).printPartenerColibri(bundle, name.getText());
					} catch (IOException | JRException e) {
						log.error(e);
						showException(e);
					}
				super.okPressed();
			} else
				setErrorMessage(result.toTextDescriptionWithCode());
		}
	}

	private void sendWelcomeSMSSequence(final String phone, final String name, final String partnerCode) {
		final List<String> toPhoneNumbers = java.util.List.of(sanitizePhoneNumber(phone));
		
		UtilServices.sendSms(getShell(), ctx, name, toPhoneNumbers,
				MessageFormat.format(Messages.AdaugaPartnerColibriDialog_WelcomeMessage1, name, partnerCode));
		UtilServices.sendSms(getShell(), ctx, name, toPhoneNumbers, Messages.AdaugaPartnerColibriDialog_WelcomeMessage2);
		UtilServices.sendSms(getShell(), ctx, name, toPhoneNumbers, Messages.AdaugaPartnerColibriDialog_WelcomeMessage3);
		UtilServices.sendSms(getShell(), ctx, name, toPhoneNumbers, Messages.AdaugaPartnerColibriDialog_WelcomeMessage4);
	}

	private Partner partnerFromFields() {
		final Partner partner = new Partner();
		partner.setName("L2 "+name.getText());
		partner.setCodFiscal(isEmpty(cui.getText()) ? null : cui.getText());
		partner.setRegCom(regCom.getText());
		partner.setAddress(adresa.getModel());
		partner.setDeliveryAddress(adresaLivrare.getText());
		partner.setPhone(telefon.getText());
		final Delegat delegat = new Delegat();
		delegat.setName(delegatName.getText());
		partner.setDelegat(delegat);
		return partner;
	}

	private void updateFields(final Partner partner) {
		if (partner != null && partner.isInactivNullCheck())
			setErrorMessage(Messages.AdaugaPartnerDialog_InactivePartnerWarning);

		name.setText(safeString(partner, Partner::getName));
		cui.setText(safeString(partner, Partner::getCodFiscal));
		regCom.setText(safeString(partner, Partner::getRegCom));
		adresa.updateModel(Optional.ofNullable(partner).map(Partner::getAddress).orElse(new Address()));
		adresaLivrare.setText(safeString(partner, Partner::getDeliveryAddress));
		telefon.setText(safeString(partner, Partner::getPhone));
		delegatName.setText(safeString(partner, Partner::getDelegat, Delegat::getName));
	}

	private void updateFieldVisibility() {
		if (isPersJuridica()) {
			name.setVisible(true);
			cui.setVisible(true);
			preiaDate.setVisible(true);
			regCom.setVisible(true);
			adresa.setVisible(true);
			adresaLivrare.setVisible(true);
			telefon.setVisible(true);
			delegatName.setVisible(true);
			partnerCode.setVisible(true);
		} else { // pers fizica
			name.setVisible(true);
			cui.setVisible(false);
			preiaDate.setVisible(false);
			regCom.setVisible(false);
			adresa.setVisible(true);
			adresaLivrare.setVisible(true);
			telefon.setVisible(true);
			delegatName.setVisible(false);
			partnerCode.setVisible(true);
		}
	}

	private boolean isPersJuridica() {
		return persoanaJuridica.getSelection();
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}
}
