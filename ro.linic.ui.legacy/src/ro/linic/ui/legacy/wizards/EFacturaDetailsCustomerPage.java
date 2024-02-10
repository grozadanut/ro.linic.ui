package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ro.colibri.embeddable.Address;
import ro.colibri.entities.comercial.Partner;
import ro.linic.ui.legacy.session.UIUtils;

public class EFacturaDetailsCustomerPage extends WizardPage
{
	private Partner buyer;
	
	private Text name;
	private Text countryCode;
	private Text codJudet;
	private Text city;
	private Text addressLine;
	
	public EFacturaDetailsCustomerPage(final Partner buyer)
	{
        super("CUMPARATOR");
        setTitle("Verifica datele cumparatorului");
        setMessage("Datele au fost preluate din baza de date si procesate in formatul necesar. Va rugam verificati daca procesarea s-a efectuat corect! (* - camp obligatoriu)");
        this.buyer = buyer;
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
		
		final Label countryLabel = new Label(container, SWT.NONE);
		countryLabel.setText("Codul tarii*");
		UIUtils.setFont(countryLabel);
		
		countryCode = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(countryCode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(countryCode);
		
		final Label judetLabel = new Label(container, SWT.NONE);
		judetLabel.setText("Judet(RO-2 litere)");
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
		
		addressLine = new Text(container, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(addressLine);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(addressLine);
		
		setControl(container);
	}
	
	private void addListeners()
	{
		countryCode.addModifyListener(new ModifyListener()
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
		
		addressLine.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
	}
	
	public void validate()
	{
		if (isEmpty(countryCode.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Codul tarii este obligatoriu!");
			return;
		}
		
		if (isEmpty(city.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Localitatea este obligatorie!");
			return;
		}
		
		if (isEmpty(addressLine.getText()))
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
		name.setText(safeString(buyer, Partner::getName));
		countryCode.setText(safeString(buyer, Partner::getAddress, Address::getCountry, "RO"));
		addressLine.setText(safeString(buyer, Partner::getAddress, Address::getStrada));
		city.setText(safeString(buyer, Partner::getAddress, Address::getOras));
		codJudet.setText(safeString(buyer, Partner::getAddress, Address::getJudet));
	}
	
	public Address getAddress()
	{
		final Address address = new Address();
		address.setCountry(countryCode.getText());
		address.setJudet(codJudet.getText());
		address.setOras(city.getText());
		address.setStrada(addressLine.getText());
		return address;
	}
}
