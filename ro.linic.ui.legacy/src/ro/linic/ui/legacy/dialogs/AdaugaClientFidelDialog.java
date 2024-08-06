package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.StringUtils.isEmpty;

import java.math.BigDecimal;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.colibri.embeddable.FidelityCard;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class AdaugaClientFidelDialog extends TitleAreaDialog
{
	private static final int TEXT_WIDTH = 150;
	
	private Text name;
	private Text telefon;
	private Text fidelityNumber;
	
	public AdaugaClientFidelDialog(final Shell parent)
	{
		super(parent);
	}
	
	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle(Messages.AdaugaPartnerDialog_Title);
		setMessage(Messages.AdaugaPartnerDialog_Message);
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText(Messages.AdaugaPartnerDialog_Name);
		UIUtils.setFont(nameLabel);
		GridDataFactory.swtDefaults().applyTo(nameLabel);
		
		name = new Text(container, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(name);
		
		final Label phoneLabel = new Label(container, SWT.NONE);
		phoneLabel.setText(Messages.AdaugaPartnerDialog_Phone);
		UIUtils.setFont(phoneLabel);
		GridDataFactory.swtDefaults().applyTo(phoneLabel);
		
		telefon = new Text(container, SWT.BORDER);
		UIUtils.setFont(telefon);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(telefon);
		
		final Label fidelityNrLabel = new Label(container, SWT.NONE);
		fidelityNrLabel.setText(Messages.AdaugaPartnerDialog_FidelityCard);
		UIUtils.setFont(fidelityNrLabel);
		GridDataFactory.swtDefaults().applyTo(fidelityNrLabel);
		
		fidelityNumber = new Text(container, SWT.BORDER);
		UIUtils.setFont(fidelityNumber);
		GridDataFactory.swtDefaults().span(2, 1).hint(TEXT_WIDTH, SWT.DEFAULT).applyTo(fidelityNumber);

		return area;
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(name.getText()))
			setErrorMessage(Messages.AdaugaPartnerDialog_EmptyNameError);
		else if (isEmpty(fidelityNumber.getText()))
			setErrorMessage(Messages.AdaugaPartnerDialog_EmptyNumberError);
		else
		{
			final InvocationResult result = BusinessDelegate.mergePartner(partnerFromFields());
			if (result.statusOk())
				super.okPressed();
			else
				setErrorMessage(result.toTextDescriptionWithCode());
		}
	}
	
	private Partner partnerFromFields()
	{
		final Partner partner = new Partner();
		partner.setName(name.getText());
		partner.setPhone(telefon.getText());
		final FidelityCard fidelityCard = new FidelityCard();
		if (isEmpty(fidelityNumber.getText()))
		{
			fidelityCard.setNumber(null);
			fidelityCard.setDiscountPercentage(null);
		}
		else
		{
			fidelityCard.setNumber(fidelityNumber.getText());
			fidelityCard.setDiscountPercentage(new BigDecimal("0.05"));
		}
		partner.setFidelityCard(fidelityCard);
		return partner;
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
}
