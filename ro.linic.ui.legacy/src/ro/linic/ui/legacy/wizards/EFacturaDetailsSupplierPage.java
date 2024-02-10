package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.AddressUtils.extractAddressLine;
import static ro.colibri.util.AddressUtils.extractCity;
import static ro.colibri.util.AddressUtils.extractCodJudet;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.colibri.util.StringUtils.stripBlankChars;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class EFacturaDetailsSupplierPage extends WizardPage
{
	private Text name;
	private Text address;
	private Text city;
	private Text codJudet;
	private Text phone;
	private Text email;
	private Text iban;
	
	private Logger log;
	
	public EFacturaDetailsSupplierPage(final Logger log)
	{
        super("FURNIZOR");
        setTitle("Verifica datele furnizorului");
        setMessage("Datele au fost preluate din baza de date si procesate in formatul necesar. Va rugam verificati daca procesarea s-a efectuat corect! (* - camp obligatoriu)");
        this.log = log;
    }
	
	@Override
	public void createControl(final Composite parent)
	{
		createWidgets(parent);
		addListeners();
		loadData();
		validate();
	}

	private void createWidgets(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText("Denumire");
		UIUtils.setFont(nameLabel);
		
		name = new Text(container, SWT.READ_ONLY | SWT.BORDER);
		name.setEditable(false);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(name);
		
		final Label ibanLabel = new Label(container, SWT.NONE);
		ibanLabel.setText("IBAN*");
		UIUtils.setFont(ibanLabel);
		
		iban = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(iban);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(iban);
		
		final Label judetLabel = new Label(container, SWT.NONE);
		judetLabel.setText("Judet(RO-2 litere)*");
		UIUtils.setFont(judetLabel);
		
		codJudet = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(codJudet);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(codJudet);
		
		final Label cityLabel = new Label(container, SWT.NONE);
		cityLabel.setText("Localitate*");
		UIUtils.setFont(cityLabel);
		
		city = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(city);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(city);
		
		final Label addressLabel = new Label(container, SWT.NONE);
		addressLabel.setText("Strada si nr*");
		UIUtils.setFont(addressLabel);
		
		address = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(address);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(address);
		
		final Label phoneLabel = new Label(container, SWT.NONE);
		phoneLabel.setText("Telefon");
		UIUtils.setFont(phoneLabel);
		
		phone = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(phone);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(phone);
		
		final Label emailLabel = new Label(container, SWT.NONE);
		emailLabel.setText("Email");
		UIUtils.setFont(emailLabel);
		
		email = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(email);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(email);
		
		setControl(container);
	}
	
	
	private void addListeners()
	{
		iban.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
		
		codJudet.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
		
		city.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
		
		address.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
	}
	
	private void validate()
	{
		if (isEmpty(iban.getText()))
		{
			setPageComplete(false);
			setErrorMessage("IBAN-ul este obligatoriu!");
			return;
		}
		
		if (isEmpty(codJudet.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Codul judetului este obligatoriu!");
			return;
		}
		
		if (isEmpty(city.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Localitatea este obligatorie!");
			return;
		}
		
		if (isEmpty(address.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Adresa este obligatorie!");
			return;
		}
		
		setErrorMessage(null);
		setPageComplete(true);
	}
	
	private void loadData()
	{
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		name.setText(firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		iban.setText(stripBlankChars(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_ACC_KEY)));
		phone.setText(firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY));
		email.setText(firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		
		final String billingAddress = firmaDetails.extraString(PersistedProp.FIRMA_BILLING_ADDRESS_KEY);
		address.setText(extractAddressLine(billingAddress));
		city.setText(extractCity(billingAddress));
		codJudet.setText(extractCodJudet(billingAddress));
	}
	
	public String address()
	{
		return address.getText();
	}
	
	public String city()
	{
		return city.getText();
	}
	
	public String codJudet()
	{
		return codJudet.getText();
	}
	
	public String phone()
	{
		return phone.getText();
	}
	
	public String email()
	{
		return email.getText();
	}
	
	public String iban()
	{
		return iban.getText();
	}
}
