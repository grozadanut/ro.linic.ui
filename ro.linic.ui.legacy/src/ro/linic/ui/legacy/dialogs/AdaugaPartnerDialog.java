package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.e4.core.services.log.Logger;
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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.Delegat;
import ro.colibri.embeddable.FidelityCard;
import ro.colibri.embeddable.PartnerGrupaInteresMappingId;
import ro.colibri.entities.comercial.GrupaInteres;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.mappings.PartnerGrupaInteresMapping;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.widgets.AddressWidget;

public class AdaugaPartnerDialog extends TitleAreaDialog
{
	private static final int TEXT_WIDTH = 150;
	
	private Button persoanaFizica;
	private Button persoanaJuridica;
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
	private Text fidelityDiscountPercentage;
	private Text termenPlata;
	private List grupa;
	
	private ImmutableList<GrupaInteres> allGrupe;
	
	private Bundle bundle;
	private Logger log;
	
	public AdaugaPartnerDialog(final Shell parent, final Bundle bundle, final Logger log)
	{
		super(parent);
		this.bundle = bundle;
		this.log = log;
		allGrupe = BusinessDelegate.grupeInteres_Sync();
	}
	
	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle("Adauga Partener");
		setMessage("Creati un partener nou persoana fizica sau juridica");
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label partnerTypeLabel = new Label(container, SWT.NONE);
		partnerTypeLabel.setText("Tip Partener");
		UIUtils.setFont(partnerTypeLabel);
		
		persoanaFizica = new Button(container, SWT.RADIO);
		persoanaFizica.setText("Persoana Fizica");
		UIUtils.setBoldFont(persoanaFizica);
		GridDataFactory.swtDefaults().applyTo(persoanaFizica);
		
		persoanaJuridica = new Button(container, SWT.RADIO);
		persoanaJuridica.setText("Firma");
		UIUtils.setBoldFont(persoanaJuridica);
		GridDataFactory.swtDefaults().applyTo(persoanaJuridica);
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText("Nume");
		UIUtils.setFont(nameLabel);
		GridDataFactory.swtDefaults().applyTo(nameLabel);
		
		name = new Text(container, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(name);
		
		final Label cuiLabel = new Label(container, SWT.NONE);
		cuiLabel.setText("Cod fiscal");
		UIUtils.setFont(cuiLabel);
		GridDataFactory.swtDefaults().applyTo(cuiLabel);
		
		cui = new Text(container, SWT.BORDER);
		cui.setMessage("RO");
		UIUtils.setFont(cui);
		GridDataFactory.swtDefaults().hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(cui);
		
		preiaDate = new Button(container, SWT.PUSH);
		preiaDate.setText("Preia date");
		preiaDate.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		preiaDate.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(preiaDate);
		GridDataFactory.swtDefaults().applyTo(preiaDate);
		
		final Label regComLabel = new Label(container, SWT.NONE);
		regComLabel.setText("RegComert");
		UIUtils.setFont(regComLabel);
		GridDataFactory.swtDefaults().applyTo(regComLabel);
		
		regCom = new Text(container, SWT.BORDER);
		regCom.setMessage("J05/");
		UIUtils.setFont(regCom);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(regCom);
		
		final Label addresaLabel = new Label(container, SWT.NONE);
		addresaLabel.setText("Adresa facturare");
		UIUtils.setFont(addresaLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLabel);
		
		adresa = new AddressWidget(container, SWT.NONE);
		UIUtils.setFont(adresa);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresa);
		adresa.getModel().setOras("Marghita");
		adresa.getModel().setCountry("RO");
		adresa.getModel().setJudet("RO-BH");
		
		final Label addresaLivrareLabel = new Label(container, SWT.NONE);
		addresaLivrareLabel.setText("Adresa livrare");
		UIUtils.setFont(addresaLivrareLabel);
		GridDataFactory.swtDefaults().applyTo(addresaLivrareLabel);
		
		adresaLivrare = new Text(container, SWT.BORDER);
		UIUtils.setFont(adresaLivrare);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adresaLivrare);
		
		final Label phoneLabel = new Label(container, SWT.NONE);
		phoneLabel.setText("Telefon");
		UIUtils.setFont(phoneLabel);
		GridDataFactory.swtDefaults().applyTo(phoneLabel);
		
		telefon = new Text(container, SWT.BORDER);
		UIUtils.setFont(telefon);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(telefon);
		
		final Label emailLabel = new Label(container, SWT.NONE);
		emailLabel.setText("Email");
		UIUtils.setFont(emailLabel);
		GridDataFactory.swtDefaults().applyTo(emailLabel);
		
		email = new Text(container, SWT.BORDER);
		UIUtils.setFont(email);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(email);
		
		final Label bancaLabel = new Label(container, SWT.NONE);
		bancaLabel.setText("Banca");
		UIUtils.setFont(bancaLabel);
		GridDataFactory.swtDefaults().applyTo(bancaLabel);
		
		banca = new Text(container, SWT.BORDER);
		UIUtils.setFont(banca);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(banca);
		
		final Label contLabel = new Label(container, SWT.NONE);
		contLabel.setText("Cont");
		UIUtils.setFont(contLabel);
		GridDataFactory.swtDefaults().applyTo(contLabel);
		
		cont = new Text(container, SWT.BORDER);
		UIUtils.setFont(cont);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(cont);
		
		final Label delegatLabel = new Label(container, SWT.NONE);
		delegatLabel.setText("Delegat");
		UIUtils.setFont(delegatLabel);
		GridDataFactory.swtDefaults().applyTo(delegatLabel);
		
		delegatName = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatName);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatName);
		
		final Label cnpLabel = new Label(container, SWT.NONE);
		cnpLabel.setText("CNP");
		UIUtils.setFont(cnpLabel);
		GridDataFactory.swtDefaults().applyTo(cnpLabel);
		
		delegatCNP = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatCNP);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatCNP);
		
		final Label seriaLabel = new Label(container, SWT.NONE);
		seriaLabel.setText("CI seria/Nr");
		UIUtils.setFont(seriaLabel);
		GridDataFactory.swtDefaults().applyTo(seriaLabel);
		
		delegatCI = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatCI);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatCI);
		
		final Label elibLabel = new Label(container, SWT.NONE);
		elibLabel.setText("Elib de");
		UIUtils.setFont(elibLabel);
		GridDataFactory.swtDefaults().applyTo(elibLabel);
		
		delegatElib = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatElib);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatElib);
		
		final Label autoLabel = new Label(container, SWT.NONE);
		autoLabel.setText("Auto");
		UIUtils.setFont(autoLabel);
		GridDataFactory.swtDefaults().applyTo(autoLabel);
		
		delegatAuto = new Text(container, SWT.BORDER);
		UIUtils.setFont(delegatAuto);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(delegatAuto);
		
		final Label fidelityNrLabel = new Label(container, SWT.NONE);
		fidelityNrLabel.setText("Card fidelitate");
		UIUtils.setFont(fidelityNrLabel);
		GridDataFactory.swtDefaults().applyTo(fidelityNrLabel);
		
		fidelityNumber = new Text(container, SWT.BORDER);
		UIUtils.setFont(fidelityNumber);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(fidelityNumber);

		final Label fidelityDiscLabel = new Label(container, SWT.NONE);
		fidelityDiscLabel.setText("%Discount");
		UIUtils.setFont(fidelityDiscLabel);
		GridDataFactory.swtDefaults().applyTo(fidelityDiscLabel);
		
		fidelityDiscountPercentage = new Text(container, SWT.BORDER);
		UIUtils.setFont(fidelityDiscountPercentage);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH/2, SWT.DEFAULT).applyTo(fidelityDiscountPercentage);
		
		final Label termenPlataLabel = new Label(container, SWT.NONE);
		termenPlataLabel.setText("Termen plata");
		UIUtils.setFont(termenPlataLabel);
		GridDataFactory.swtDefaults().applyTo(termenPlataLabel);
		
		termenPlata = new Text(container, SWT.BORDER);
		UIUtils.setFont(termenPlata);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(termenPlata);
		
		final Label grupaLabel = new Label(container, SWT.NONE);
		grupaLabel.setText("Grupe interes");
		UIUtils.setFont(grupaLabel);
		GridDataFactory.swtDefaults().applyTo(grupaLabel);
		
		grupa = new List(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		grupa.setItems(allGrupe.stream().map(GrupaInteres::displayName).toArray(String[]::new));
		UIUtils.setFont(grupa);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 100).applyTo(grupa);

		updateFieldVisibility();
		addListeners();
		return area;
	}
	
	private void addListeners()
	{
		persoanaFizica.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateFieldVisibility();
			}
		});
		
		persoanaJuridica.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateFieldVisibility();
			}
		});
		
		preiaDate.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Partner p = new Partner();
				p.setCodFiscal(isEmpty(cui.getText()) ? null : cui.getText());
				final InvocationResult result = BusinessDelegate.verifyPartnerAtAnaf(p);
				if (result.statusOk())
				{
					setErrorMessage(null);
					final Partner updatedPartner = result.extra(InvocationResult.PARTNER_KEY);
					updateFields(updatedPartner);
				}
				else
					setErrorMessage(result.toTextDescriptionWithCode());
			}
		});
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(name.getText()))
			setErrorMessage("Trebuie specificat un nume pentru partener!");
		else
		{
			final InvocationResult result = BusinessDelegate.mergePartner(partnerFromFields());
			if (result.statusOk())
			{
				if (!isPersJuridica())
					try
					{
						JasperReportManager.instance(bundle, log).printGDPR(bundle, name.getText());
					}
					catch (IOException | JRException e)
					{
						log.error(e);
						showException(e);
					}
				super.okPressed();
			}
			else
				setErrorMessage(result.toTextDescriptionWithCode());
		}
	}
	
	private Partner partnerFromFields()
	{
		final Partner partner = new Partner();
		partner.setName(name.getText());
		partner.setCodFiscal(isEmpty(cui.getText()) ? null : cui.getText());
		partner.setRegCom(regCom.getText());
		partner.setAddress(adresa.getModel());
		partner.setDeliveryAddress(adresaLivrare.getText());
		partner.setPhone(telefon.getText());
		partner.setEmail(email.getText());
		partner.setBanca(banca.getText());
		partner.setIban(cont.getText());
		final Delegat delegat = new Delegat();
		delegat.setName(delegatName.getText());
		delegat.setCnp(delegatCNP.getText());
		delegat.setSerieCI(delegatCI.getText());
		delegat.setElib(delegatElib.getText());
		delegat.setAuto(delegatAuto.getText());
		partner.setDelegat(delegat);
		final FidelityCard fidelityCard = new FidelityCard();
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
		partner.setFidelityCard(fidelityCard);
		final Integer termenPlataParsed = parseToInt(termenPlata.getText());
		partner.setTermenPlata(termenPlataParsed > 0 ? termenPlataParsed : null);
		
		partner.getGrupeInteres().clear();
		partner.getGrupeInteres().addAll(selectedGrupeMappings(partner));
		return partner;
	}
	
	private void updateFields(final Partner partner)
	{
		if (partner != null && partner.isInactivNullCheck())
			setErrorMessage("ATENTIE!!! In urma verificarii la ANAF, partenerul figureaza ca INACTIV!!!");
		
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
	}

	private void updateFieldVisibility()
	{
		if (isPersJuridica())
		{
			name.setVisible(true);
			cui.setVisible(true);
			preiaDate.setVisible(true);
			regCom.setVisible(true);
			adresa.setVisible(true);
			adresaLivrare.setVisible(true);
			telefon.setVisible(true);
			email.setVisible(true);
			banca.setVisible(true);
			cont.setVisible(true);
			delegatName.setVisible(true);
			delegatCNP.setVisible(true);
			delegatCI.setVisible(true);
			delegatElib.setVisible(true);
			delegatAuto.setVisible(true);
			fidelityNumber.setVisible(true);
			fidelityDiscountPercentage.setVisible(true);
			termenPlata.setVisible(true);
			grupa.setVisible(true);
		}
		else // pers fizica
		{
			name.setVisible(true);
			cui.setVisible(false);
			preiaDate.setVisible(false);
			regCom.setVisible(false);
			adresa.setVisible(true);
			adresaLivrare.setVisible(true);
			telefon.setVisible(true);
			email.setVisible(true);
			banca.setVisible(false);
			cont.setVisible(false);
			delegatName.setVisible(true);
			delegatCNP.setVisible(true);
			delegatCI.setVisible(false);
			delegatElib.setVisible(false);
			delegatAuto.setVisible(false);
			fidelityNumber.setVisible(true);
			fidelityDiscountPercentage.setVisible(true);
			termenPlata.setVisible(true);
			grupa.setVisible(true);
		}
	}
	
	private boolean isPersJuridica()
	{
		return persoanaJuridica.getSelection();
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
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
