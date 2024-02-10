package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.colibri.entities.comercial.ContBancar;
import ro.linic.ui.legacy.session.UIUtils;

public class ContBancarDialog extends TitleAreaDialog
{
	private Text name;
	private Text iban;
	private Text banca;
	private Text valuta;
	
	private ContBancar contBancar;
	
	private Supplier<Boolean> okPressed;

	public ContBancarDialog(final Shell parent, final ContBancar contBancar)
	{
		super(parent);
		this.contBancar = contBancar;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle("Cont Bancar");
		
		final Label nameLabel = new Label(contents, SWT.NONE);
		nameLabel.setText("Nume");
		UIUtils.setFont(nameLabel);
		
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(name);
		
		final Label ibanLabel = new Label(contents, SWT.NONE);
		ibanLabel.setText("IBAN");
		UIUtils.setFont(ibanLabel);
		
		iban = new Text(contents, SWT.SINGLE | SWT.BORDER);
		iban.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(iban);
		
		final Label bancaLabel = new Label(contents, SWT.NONE);
		bancaLabel.setText("Banca");
		UIUtils.setFont(bancaLabel);
		
		banca = new Text(contents, SWT.SINGLE | SWT.BORDER);
		banca.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(banca);
		
		final Label valutaLabel = new Label(contents, SWT.NONE);
		valutaLabel.setText("Valuta");
		UIUtils.setFont(valutaLabel);
		
		valuta = new Text(contents, SWT.SINGLE | SWT.BORDER);
		valuta.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(valuta);
		
		fillFields();
		return contents;
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(name.getText()))
		{
			setErrorMessage("Numele este obligatoriu!");
			return;
		}
		
		if (isEmpty(iban.getText()))
		{
			setErrorMessage("IBAN-ul este obligatoriu!");
			return;
		}
		
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		name.setText(safeString(contBancar, ContBancar::getName));
		iban.setText(safeString(contBancar, ContBancar::getIban));
		banca.setText(safeString(contBancar, ContBancar::getBanca));
		valuta.setText(safeString(contBancar, ContBancar::getValuta));
	}
	
	private void fillContBancar()
	{
		contBancar.setName(name.getText());
		contBancar.setIban(iban.getText());
		contBancar.setBanca(banca.getText());
		contBancar.setValuta(valuta.getText());
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public ContBancar filledContBancar()
	{
		fillContBancar();
		return contBancar;
	}
}
