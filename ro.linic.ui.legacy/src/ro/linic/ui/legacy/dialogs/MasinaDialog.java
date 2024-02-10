package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Masina;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class MasinaDialog extends TitleAreaDialog
{
	private Text nr;
	private Text marca;
	private Text culoare;
	private Combo gestiune;
	
	private Masina masina;
	
	private ImmutableList<Gestiune> allGestiuni;
	private Supplier<Boolean> okPressed;

	public MasinaDialog(final Shell parent, final Masina masina)
	{
		super(parent);
		this.masina = masina;
		allGestiuni = BusinessDelegate.allGestiuni();
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle("Masina");
		
		final Label nrLabel = new Label(contents, SWT.NONE);
		nrLabel.setText("Numar");
		UIUtils.setFont(nrLabel);
		
		nr = new Text(contents, SWT.SINGLE | SWT.BORDER);
		nr.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(nr);
		
		final Label marcaLabel = new Label(contents, SWT.NONE);
		marcaLabel.setText("Marca");
		UIUtils.setFont(marcaLabel);
		
		marca = new Text(contents, SWT.SINGLE | SWT.BORDER);
		marca.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(marca);
		
		final Label culoareLabel = new Label(contents, SWT.NONE);
		culoareLabel.setText("Culoare");
		UIUtils.setFont(culoareLabel);
		
		culoare = new Text(contents, SWT.SINGLE | SWT.BORDER);
		culoare.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(culoare);
		
		final Label gestiuneLabel = new Label(contents, SWT.NONE);
		gestiuneLabel.setText("Gestiune");
		UIUtils.setFont(gestiuneLabel);
		
		gestiune = new Combo(contents, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::getName).toArray(String[]::new));
		gestiune.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(gestiune);
		
		fillFields();
		return contents;
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(nr.getText()))
		{
			setErrorMessage("Numarul este obligatoriu!");
			return;
		}
		
		if (!selectedGestiune().isPresent())
		{
			setErrorMessage("Gestiunea este obligatorie!");
			return;
		}
		
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		nr.setText(safeString(masina, Masina::getNr));
		marca.setText(safeString(masina, Masina::getMarca));
		culoare.setText(safeString(masina, Masina::getCuloare));
		gestiune.select(allGestiuni.indexOf(masina.getGestiune()));
	}
	
	private void fillMasina()
	{
		masina.setNr(nr.getText());
		masina.setMarca(marca.getText());
		masina.setCuloare(culoare.getText());
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public Masina filledMasina()
	{
		fillMasina();
		return masina;
	}
	
	public Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
}
