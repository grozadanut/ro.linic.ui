package ro.linic.ui.legacy.widgets;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import ro.colibri.embeddable.Address;
import ro.colibri.util.AddressUtils;
import ro.linic.ui.legacy.widgets.components.LabelContentProposalProvider;

public class AddressWidget extends Composite
{
	/**
	 * ISO 3166-1:Alpha2 Country codes
	 * Example value: GB
	 */
	private Text country;
	/**
	 * The subdivision of a country.
	 * Example value: RO-BH
	 * <a href="https://ro.wikipedia.org/wiki/ISO_3166-2:RO">https://ro.wikipedia.org/wiki/ISO_3166-2:RO<a>
	 */
	private Text countrySubentity;
	private Text city;
	private Text postalZone;
	private Text primaryLine;
	
	private Address model;
	private DataBindingContext ctx;
	
	private static IContentProposal[] roCityNames()
	{
		return AddressUtils.romanianCities().stream()
				.map(city -> new ContentProposal(city.getName(), city.getName() + " - " + city.getCountrySubentityShort(), null))
				.toArray(IContentProposal[]::new);
	}
	
	private static IContentProposal[] roCountrySubentities()
	{
		return AddressUtils.judete.entrySet().stream()
				.map(entry -> new ContentProposal(entry.getKey(), entry.getValue(), null))
				.toArray(IContentProposal[]::new);
	}
	
	private static String cityProposalToRoSubentity(final IContentProposal proposal)
	{
		if (proposal == null)
			return EMPTY_STRING;
		
		final String label = proposal.getLabel();
		final String subentityShort = label.substring(label.lastIndexOf(" - ")+3, label.length());
		return "RO-" + subentityShort.toUpperCase();
	}
	
	public AddressWidget(final Composite parent, final int style)
	{
		super(parent, style);
		createComposite();
		ctx = new DataBindingContext();
		updateModel(new Address());
	}
	
	private void createComposite()
	{
		final GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		setLayout(layout);
		
		country = new Text(this, SWT.BORDER);
		country.setMessage("Tara");
		GridDataFactory.swtDefaults().applyTo(country);
		
		final LabelContentProposalProvider countrySubentityContentProvider = new LabelContentProposalProvider(roCountrySubentities());
		countrySubentity = new Text(this, SWT.BORDER);
		countrySubentity.setMessage("Judet");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(countrySubentity);
		final ContentProposalAdapter countrySubentityProposalAdapter = new ContentProposalAdapter(countrySubentity,
				new TextContentAdapter(), countrySubentityContentProvider, null, null);
		countrySubentityProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		
		postalZone = new Text(this, SWT.BORDER);
		postalZone.setMessage("Cod postal");
		GridDataFactory.swtDefaults().applyTo(postalZone);
		
		final LabelContentProposalProvider cityContentProvider = new LabelContentProposalProvider(roCityNames());
		city = new Text(this, SWT.BORDER);
		city.setMessage("Oras");
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(city);
		final ContentProposalAdapter cityProposalAdapter = new ContentProposalAdapter(city,
				new TextContentAdapter(), cityContentProvider, null, null);
		cityProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		cityProposalAdapter.addContentProposalListener(proposal -> 
		{
			countrySubentityProposalAdapter.setEnabled(false);
			countrySubentity.setText(cityProposalToRoSubentity(proposal));
			countrySubentityProposalAdapter.setEnabled(true);
		});
		
		primaryLine = new Text(this, SWT.BORDER);
		primaryLine.setMessage("Adresa");
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(primaryLine);
	}
	
	private void bindValues()
	{
		ctx.dispose();
		final IObservableValue<String> countryWidget = WidgetProperties.text(SWT.Modify).observe(country);
		final IObservableValue<String> countryModel = BeanProperties.value(Address.class, "country", String.class).observe(model);
		ctx.bindValue(countryWidget, countryModel);
		
		final IObservableValue<String> countrySubentityWidget = WidgetProperties.text(SWT.Modify).observe(countrySubentity);
		final IObservableValue<String> countrySubentityModel = BeanProperties.value(Address.class, "judet", String.class).observe(model);
		ctx.bindValue(countrySubentityWidget, countrySubentityModel);
		
		final IObservableValue<String> postalZoneWidget = WidgetProperties.text(SWT.Modify).observe(postalZone);
		final IObservableValue<String> postalZoneModel = BeanProperties.value(Address.class, "nr", String.class).observe(model);
		ctx.bindValue(postalZoneWidget, postalZoneModel);
		
		final IObservableValue<String> cityWidget = WidgetProperties.text(SWT.Modify).observe(city);
		final IObservableValue<String> cityModel = BeanProperties.value(Address.class, "oras", String.class).observe(model);
		ctx.bindValue(cityWidget, cityModel);
		
		final IObservableValue<String> primaryLineWidget = WidgetProperties.text(SWT.Modify).observe(primaryLine);
		final IObservableValue<String> primaryLineModel = BeanProperties.value(Address.class, "strada", String.class).observe(model);
		ctx.bindValue(primaryLineWidget, primaryLineModel);
	}
	
	@Override
	public void dispose()
	{
		ctx.dispose();
		super.dispose();
	}

	@Override
	public void setFont(final Font font)
	{
		super.setFont(font);
		country.setFont(font);
		countrySubentity.setFont(font);
		city.setFont(font);
		postalZone.setFont(font);
		primaryLine.setFont(font);
	}
	
	@Override
	public void setBackground(final Color color)
	{
		super.setBackground(color);
		country.setBackground(color);
		countrySubentity.setBackground(color);
		city.setBackground(color);
		postalZone.setBackground(color);
		primaryLine.setBackground(color);
	}
	
	@Override
	public void setForeground(final Color color)
	{
		super.setForeground(color);
		country.setForeground(color);
		countrySubentity.setForeground(color);
		city.setForeground(color);
		postalZone.setForeground(color);
		primaryLine.setForeground(color);
	}
	
	public void addModifyListener(final ModifyListener modifyListener)
	{
		country.addModifyListener(modifyListener);
		countrySubentity.addModifyListener(modifyListener);
		city.addModifyListener(modifyListener);
		postalZone.addModifyListener(modifyListener);
		primaryLine.addModifyListener(modifyListener);
	}
	
	public void removeModifyListener(final ModifyListener modifyListener)
	{
		country.removeModifyListener(modifyListener);
		countrySubentity.removeModifyListener(modifyListener);
		city.removeModifyListener(modifyListener);
		postalZone.removeModifyListener(modifyListener);
		primaryLine.removeModifyListener(modifyListener);
	}
	
	public void updateModel(final Address model)
	{
		this.model = model;
		bindValues();
	}
	
	public Address getModel()
	{
		return model;
	}
}
