package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.Role;
import ro.colibri.entities.user.User;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.widgets.ImagePicker;

public class UserDialog extends TitleAreaDialog
{
	private Text email;
	private Text name;
	private Text phone;
	private Text password;
	private Button nologin;
	private Text cnp;
	private Text bonusSalarThreshold;
	private Text bonusSalarPercent;
	
	private Combo role;
	private Combo selectedGestiune;
	private Combo selectedCompany;
	private List masini;
	private ImagePicker semnaturaImage;
	private ImagePicker stampilaImage;
	
	private ImmutableList<Role> allRoles;
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<Company> allCompanies;
	private ImmutableList<Masina> allMasini;
	
	private User user;
	
	private Supplier<Boolean> okPressed;
	
	private Bundle bundle;
	private Logger log;

	public UserDialog(final Bundle bundle, final Logger log, final Shell parent, final User user)
	{
		super(parent);
		this.user = user;
		allRoles = BusinessDelegate.dbRoles();
		allMasini = BusinessDelegate.dbMasini();
		allCompanies = BusinessDelegate.allCompanies();
		this.bundle = bundle;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(contents);
		setTitle(Messages.UserDialog_Title);
		
		final Label emailLabel = new Label(contents, SWT.NONE);
		emailLabel.setText(Messages.UserDialog_Email);
		UIUtils.setFont(emailLabel);
		
		email = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(email);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(email);
		
		final Label nameLabel = new Label(contents, SWT.NONE);
		nameLabel.setText(Messages.UserDialog_Name);
		UIUtils.setFont(nameLabel);
		
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(name);
		
		final Label phoneLabel = new Label(contents, SWT.NONE);
		phoneLabel.setText(Messages.UserDialog_Phone);
		UIUtils.setFont(phoneLabel);
		
		phone = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(phone);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(phone);
		
		final Label passwordLabel = new Label(contents, SWT.NONE);
		passwordLabel.setText(Messages.UserDialog_Password);
		UIUtils.setFont(passwordLabel);
		
		password = new Text(contents, SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(password);
		
		nologin = new Button(contents, SWT.CHECK);
		nologin.setText(Messages.UserDialog_Nologin);
		UIUtils.setFont(nologin);
		
		final Label cnpLabel = new Label(contents, SWT.NONE);
		cnpLabel.setText(Messages.UserDialog_SSN);
		UIUtils.setFont(cnpLabel);
		
		cnp = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(cnp);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(cnp);
		
		final Label roleLabel = new Label(contents, SWT.NONE);
		roleLabel.setText(Messages.UserDialog_Role);
		UIUtils.setFont(roleLabel);
		
		role = new Combo(contents, SWT.DROP_DOWN);
		role.setItems(allRoles.stream().map(Role::getName).toArray(String[]::new));
		UIUtils.setFont(role);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(role);
		
		final Label gestiuneLabel = new Label(contents, SWT.NONE);
		gestiuneLabel.setText(Messages.UserDialog_Inventory);
		UIUtils.setFont(gestiuneLabel);
		
		selectedGestiune = new Combo(contents, SWT.DROP_DOWN);
		reloadGestiuni(Optional.ofNullable(user)
				.map(User::getSelectedCompany)
				.map(Company::getId)
				.orElse(null));
		UIUtils.setFont(selectedGestiune);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(selectedGestiune);
		
		final Label companyLabel = new Label(contents, SWT.NONE);
		companyLabel.setText(Messages.UserDialog_Company);
		UIUtils.setFont(companyLabel);
		
		selectedCompany = new Combo(contents, SWT.DROP_DOWN);
		selectedCompany.setItems(allCompanies.stream().map(Company::displayName).toArray(String[]::new));
		UIUtils.setFont(selectedCompany);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(selectedCompany);
		
		final Label bonusSalarThresholdLabel = new Label(contents, SWT.NONE);
		bonusSalarThresholdLabel.setText(Messages.UserDialog_IncomeBonusThreshold);
		UIUtils.setFont(bonusSalarThresholdLabel);
		
		bonusSalarThreshold = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(bonusSalarThreshold);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(bonusSalarThreshold);
		
		final Label bonusSalarPercentLabel = new Label(contents, SWT.NONE);
		bonusSalarPercentLabel.setText(Messages.UserDialog_IncomeBonusPercent);
		UIUtils.setFont(bonusSalarPercentLabel);
		
		bonusSalarPercent = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(bonusSalarPercent);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(bonusSalarPercent);
		
		final Label masiniLabel = new Label(contents, SWT.NONE);
		masiniLabel.setText(Messages.UserDialog_Cars);
		UIUtils.setFont(masiniLabel);
		
		masini = new List(contents, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		masini.setItems(allMasini.stream().map(Masina::displayName).toArray(String[]::new));
		UIUtils.setFont(masini);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 80).span(2, 1).applyTo(masini);
		
		final Label semnaturaLabel = new Label(contents, SWT.NONE);
		semnaturaLabel.setText(Messages.UserDialog_Signature);
		UIUtils.setFont(semnaturaLabel);
		
		semnaturaImage = new ImagePicker(contents, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(120, 120).applyTo(semnaturaImage.widget());
		UIUtils.setFont(semnaturaImage.widget());
		
		final Label stampilaLabel = new Label(contents, SWT.NONE);
		stampilaLabel.setText(Messages.UserDialog_Stamp);
		UIUtils.setFont(stampilaLabel);
		
		stampilaImage = new ImagePicker(contents, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(120, 120).applyTo(stampilaImage.widget());
		UIUtils.setFont(stampilaImage.widget());
		
		fillFields();
		addListeners();
		return contents;
	}
	
	private void addListeners()
	{
		nologin.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				password.setEnabled(!nologin.getSelection());
			}
		});
		
		selectedCompany.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				reloadGestiuni(selectedCompany()
						.map(Company::getId)
						.orElse(null));
			}
		});
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(super.getInitialSize().x, 850);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(email.getText()))
		{
			setErrorMessage(Messages.UserDialog_EmailMandatory);
			return;
		}
		if (isEmpty(cnp.getText()))
		{
			setErrorMessage(Messages.UserDialog_SSNMandatory);
			return;
		}
		if (!selectedGestiune().isPresent())
		{
			setErrorMessage(Messages.UserDialog_InventoryMandatory);
			return;
		}
		if (!selectedCompany().isPresent())
		{
			setErrorMessage(Messages.UserDialog_CompanyMandatory);
			return;
		}
		if (!selectedRole().isPresent())
		{
			setErrorMessage(Messages.UserDialog_RoleMandatory);
			return;
		}
		
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		email.setText(safeString(user, User::getEmail));
		name.setText(safeString(user, User::getName));
		phone.setText(safeString(user, User::getPhone));
		cnp.setText(safeString(user, User::getCnp));
		bonusSalarThreshold.setText(safeString(user, User::getBonusSalarThreshold, String::valueOf));
		bonusSalarPercent.setText(safeString(user, User::getBonusSalarPercent, PresentationUtils::displayPercentageRaw));
		
		role.select(allRoles.indexOf(user.getRole()));
		selectedGestiune.select(allGestiuni.indexOf(user.getSelectedGestiune()));
		selectedCompany.select(allCompanies.indexOf(user.getSelectedCompany()));
		masini.setSelection(user.getMasini().stream().mapToInt(allMasini::indexOf).toArray());
		
		final String userId = Optional.ofNullable(user).map(User::getId).map(String::valueOf).orElse(EMPTY_STRING);
		final String companyId = String.valueOf(ClientSession.instance().getLoggedUser().getSelectedCompany().getId());
		final Optional<String> semnaturaUuid = PersistedProp.uuidFromProp(ClientSession.instance().getBillSignatureProp(), userId, companyId);
		if (semnaturaUuid.isPresent())
			semnaturaImage.setImage(BusinessDelegate.imageFromUuid(bundle, log, semnaturaUuid.get(), true));
		final Optional<String> stampUuid = PersistedProp.uuidFromProp(ClientSession.instance().getBillStampProp(), userId, companyId);
		if (stampUuid.isPresent())
			stampilaImage.setImage(BusinessDelegate.imageFromUuid(bundle, log, stampUuid.get(), true));
	}
	
	private void fillUser()
	{
		user.setEmail(email.getText());
		user.setName(name.getText());
		user.setPhone(phone.getText());
		user.setCnp(cnp.getText());
		user.setPassword(password.getText());
		user.setBonusSalarThreshold(parseToInt(bonusSalarThreshold.getText()));
		user.setBonusSalarPercent(extractPercentage(bonusSalarPercent.getText()));
		
		user.setSelectedGestiune(selectedGestiune().orElse(null));
		user.setSelectedCompany(selectedCompany().orElse(null));
		user.getRoles().clear();
		selectedRole().ifPresent(role -> user.getRoles().add(role));
		user.getMasini().clear();
		user.getMasini().addAll(selectedMasini());
	}
	
	private void reloadGestiuni(final Integer companyId)
	{
		allGestiuni = BusinessDelegate.allGestiuni(companyId);
		selectedGestiune.setItems(allGestiuni.stream().map(Gestiune::getName).toArray(String[]::new));
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public User filledUser()
	{
		fillUser();
		return user;
	}
	
	public boolean faraLogin()
	{
		return nologin.getSelection();
	}
	
	public ImagePicker getSemnaturaImage()
	{
		return semnaturaImage;
	}
	
	public ImagePicker getStampilaImage()
	{
		return stampilaImage;
	}
	
	public Optional<Gestiune> selectedGestiune()
	{
		final int index = selectedGestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
	
	public Optional<Company> selectedCompany()
	{
		final int index = selectedCompany.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allCompanies.get(index));
	}
	
	public Optional<Role> selectedRole()
	{
		final int index = role.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allRoles.get(index));
	}
	
	public ImmutableSet<Long> selectedMasiniId()
	{
		final Builder<Long> builder = ImmutableSet.<Long>builder();
		for (final int masinaSelIndex : masini.getSelectionIndices())
			builder.add(allMasini.get(masinaSelIndex).getId());
		return builder.build();
	}
	
	public ImmutableSet<Masina> selectedMasini()
	{
		final Builder<Masina> builder = ImmutableSet.<Masina>builder();
		for (final int masinaSelIndex : masini.getSelectionIndices())
			builder.add(allMasini.get(masinaSelIndex));
		return builder.build();
	}
}
