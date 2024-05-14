package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.addToImmutableList;
import static ro.colibri.util.ListUtils.removeFromImmutableList;
import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.smallerThan;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.LobImage;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.user.Role;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.dialogs.ContBancarDialog;
import ro.linic.ui.legacy.dialogs.GestiuneDialog;
import ro.linic.ui.legacy.dialogs.MasinaDialog;
import ro.linic.ui.legacy.dialogs.RoleDialog;
import ro.linic.ui.legacy.dialogs.UserDialog;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;

public class SettingsPart
{
	public static final String PART_ID = "linic_gest_client.part.settings"; //$NON-NLS-1$
	
	private List users;
	private Button addUser;
	private Button editUser;
	private Button deleteUser;
	
	private List roles;
	private Button addRole;
	private Button editRole;
	private Button deleteRole;
	
	private List gestiuni;
	private Button addGestiune;
	private Button editGestiune;
	
	private List masini;
	private Button addMasina;
	private Button editMasina;
	private Button deleteMasina;
	
	private List conturiBancare;
	private Button addContBancar;
	private Button editContBancar;
	
	private Text firmaName;
	private Text firmaCui;
	private Text firmaRegCom;
	private Text firmaCapSocial;
	private Text firmaMainBank;
	private Text firmaMainBankAcc;
	private Text firmaSecondaryBank;
	private Text firmaSecondaryBankAcc;
	private Text firmaAddress;
	private Text firmaBillingAddress;
	private Text firmaPhone;
	private Text firmaEmail;
	private Text firmaWebsite;
	private Text tvaReadable;
	private DateTime migrationDate;
	private Text nrBonPromo;
	private Text prefixAroma;
	private Button save;

	private ImmutableList<User> allUsers;
	private ImmutableList<Role> allRoles;
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<Masina> allMasini;
	private ImmutableList<ContBancar> allConturiBancare;
	private BigDecimal tvaPercentDB;
	private String nrBonPromoDB;
	private String prefixAromaDB;
	private LocalDate migrationDateDB;
	private InvocationResult firmaDetails;
	
	@Inject private MPart part;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		allUsers = BusinessDelegate.dbUsers();
		allRoles = BusinessDelegate.dbRoles();
		allGestiuni = BusinessDelegate.allGestiuni();
		allMasini = BusinessDelegate.dbMasini();
		tvaPercentDB = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		migrationDateDB = LocalDate.parse(BusinessDelegate.persistedProp(PersistedProp.MIGRATION_DATE_KEY)
				.getValueOr(PersistedProp.MIGRATION_DATE_DEFAULT));
		firmaDetails = BusinessDelegate.firmaDetails();
		nrBonPromoDB = BusinessDelegate.persistedProp(PersistedProp.NR_BON_PROMO_KEY)
				.getValueOr(PersistedProp.NR_BON_PROMO_DEFAULT);
		prefixAromaDB = BusinessDelegate.persistedProp(PersistedProp.PREFIX_AROMA_KEY)
				.getValueOr(PersistedProp.PREFIX_AROMA_DEFAULT);
		allConturiBancare = BusinessDelegate.allConturiBancare();
		
		final ScrolledComposite scrollable = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		final Composite container = new Composite(scrollable, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		scrollable.setContent(container);
		
		createLeftColumn(container);
		createRightColumn(container);
		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		addListeners();
	}

	private void createLeftColumn(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).applyTo(container);
		
		final Label usersLabel = new Label(container, SWT.NONE);
		usersLabel.setText(Messages.SettingsPart_Users);
		UIUtils.setFont(usersLabel);
		
		users = new List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		users.setItems(allUsers.stream().map(u -> u.getId()+": "+u.displayName()).toArray(String[]::new)); //$NON-NLS-1$
		UIUtils.setFont(users);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 200).span(3, 1).applyTo(users);
		
		new Label(container, SWT.NONE);//layout
		
		addUser = new Button(container, SWT.PUSH);
		addUser.setText(Messages.SettingsPart_AddUser);
		addUser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		addUser.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(addUser);
		
		editUser = new Button(container, SWT.PUSH);
		editUser.setText(Messages.SettingsPart_EditUser);
		editUser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		editUser.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(editUser);
		
		deleteUser = new Button(container, SWT.PUSH);
		deleteUser.setText(Messages.SettingsPart_DeleteUser);
		deleteUser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		deleteUser.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(deleteUser);
		
		final Label rolesLabel = new Label(container, SWT.NONE);
		rolesLabel.setText(Messages.SettingsPart_Roles);
		UIUtils.setFont(rolesLabel);
		
		roles = new List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		roles.setItems(allRoles.stream().map(Role::getName).toArray(String[]::new));
		UIUtils.setFont(roles);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 100).span(3, 1).applyTo(roles);
		
		new Label(container, SWT.NONE);//layout
		
		addRole = new Button(container, SWT.PUSH);
		addRole.setText(Messages.SettingsPart_AddRole);
		addRole.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		addRole.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(addRole);
		
		editRole = new Button(container, SWT.PUSH);
		editRole.setText(Messages.SettingsPart_EditRole);
		editRole.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		editRole.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(editRole);
		
		deleteRole = new Button(container, SWT.PUSH);
		deleteRole.setText(Messages.SettingsPart_DeleteRole);
		deleteRole.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		deleteRole.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(deleteRole);
		
		final Label gestiuneLabel = new Label(container, SWT.NONE);
		gestiuneLabel.setText(Messages.SettingsPart_Inventories);
		UIUtils.setFont(gestiuneLabel);
		
		gestiuni = new List(container, SWT.SINGLE | SWT.BORDER);
		gestiuni.setItems(allGestiuni.stream().map(g -> g.getId()+": "+g.getName()).toArray(String[]::new)); //$NON-NLS-1$
		UIUtils.setFont(gestiuni);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(gestiuni);
		
		new Label(container, SWT.NONE);//layout
		
		addGestiune = new Button(container, SWT.PUSH);
		addGestiune.setText(Messages.SettingsPart_AddInventory);
		addGestiune.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		addGestiune.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(addGestiune);
		
		editGestiune = new Button(container, SWT.PUSH);
		editGestiune.setText(Messages.SettingsPart_EditInventory);
		editGestiune.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		editGestiune.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(editGestiune);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(editGestiune);
		
		final Label masiniLabel = new Label(container, SWT.NONE);
		masiniLabel.setText(Messages.SettingsPart_Cars);
		UIUtils.setFont(masiniLabel);
		
		masini = new List(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		masini.setItems(allMasini.stream().map(Masina::displayName).toArray(String[]::new));
		UIUtils.setFont(masini);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 200).span(3, 1).applyTo(masini);
		
		new Label(container, SWT.NONE);//layout
		
		addMasina = new Button(container, SWT.PUSH);
		addMasina.setText(Messages.SettingsPart_AddCar);
		addMasina.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		addMasina.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(addMasina);
		
		editMasina = new Button(container, SWT.PUSH);
		editMasina.setText(Messages.SettingsPart_EditCar);
		editMasina.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		editMasina.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(editMasina);
		
		deleteMasina = new Button(container, SWT.PUSH);
		deleteMasina.setText(Messages.SettingsPart_DeleteCar);
		deleteMasina.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		deleteMasina.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(deleteMasina);
		
		final Label conturiLabel = new Label(container, SWT.NONE);
		conturiLabel.setText(Messages.SettingsPart_BankAccts);
		UIUtils.setFont(conturiLabel);

		conturiBancare = new List(container, SWT.SINGLE | SWT.BORDER);
		conturiBancare.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		UIUtils.setFont(conturiBancare);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(conturiBancare);

		new Label(container, SWT.NONE);//layout

		addContBancar = new Button(container, SWT.PUSH);
		addContBancar.setText(Messages.SettingsPart_AddBankAcc);
		addContBancar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		addContBancar.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(addContBancar);

		editContBancar = new Button(container, SWT.PUSH);
		editContBancar.setText(Messages.SettingsPart_EditBankAcc);
		editContBancar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		editContBancar.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(editContBancar);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(editContBancar);
	}
	
	private void createRightColumn(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).applyTo(container);
		
		final Label firmaNameLabel = new Label(container, SWT.NONE);
		firmaNameLabel.setText(Messages.SettingsPart_Company);
		UIUtils.setFont(firmaNameLabel);
		
		firmaName = new Text(container, SWT.BORDER);
		firmaName.setText(firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		UIUtils.setFont(firmaName);
		GridDataFactory.fillDefaults().applyTo(firmaName);
		
		final Label firmaCuiLabel = new Label(container, SWT.NONE);
		firmaCuiLabel.setText(Messages.SettingsPart_TaxCode);
		UIUtils.setFont(firmaCuiLabel);
		
		firmaCui = new Text(container, SWT.BORDER);
		firmaCui.setText(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY));
		UIUtils.setFont(firmaCui);
		GridDataFactory.fillDefaults().applyTo(firmaCui);
		
		final Label firmaRegComLabel = new Label(container, SWT.NONE);
		firmaRegComLabel.setText(Messages.SettingsPart_RegCom);
		UIUtils.setFont(firmaRegComLabel);
		
		firmaRegCom = new Text(container, SWT.BORDER);
		firmaRegCom.setText(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY));
		UIUtils.setFont(firmaRegCom);
		GridDataFactory.fillDefaults().applyTo(firmaRegCom);
		
		final Label firmaCapSocialLabel = new Label(container, SWT.NONE);
		firmaCapSocialLabel.setText(Messages.SettingsPart_SocialCap);
		UIUtils.setFont(firmaCapSocialLabel);
		
		firmaCapSocial = new Text(container, SWT.BORDER);
		firmaCapSocial.setText(firmaDetails.extraString(PersistedProp.FIRMA_CAP_SOCIAL_KEY));
		UIUtils.setFont(firmaCapSocial);
		GridDataFactory.fillDefaults().applyTo(firmaCapSocial);
		
		final Label firmaMainBankLabel = new Label(container, SWT.NONE);
		firmaMainBankLabel.setText(Messages.SettingsPart_MainBank);
		UIUtils.setFont(firmaMainBankLabel);
		
		firmaMainBank = new Text(container, SWT.BORDER);
		firmaMainBank.setText(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_KEY));
		UIUtils.setFont(firmaMainBank);
		GridDataFactory.fillDefaults().applyTo(firmaMainBank);
		
		final Label firmaMainBankAccLabel = new Label(container, SWT.NONE);
		firmaMainBankAccLabel.setText(Messages.SettingsPart_MainIBAN);
		UIUtils.setFont(firmaMainBankAccLabel);
		
		firmaMainBankAcc = new Text(container, SWT.BORDER);
		firmaMainBankAcc.setText(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY));
		UIUtils.setFont(firmaMainBankAcc);
		GridDataFactory.fillDefaults().applyTo(firmaMainBankAcc);
		
		final Label firmaSecondaryBankLabel = new Label(container, SWT.NONE);
		firmaSecondaryBankLabel.setText(Messages.SettingsPart_SecondaryBank);
		UIUtils.setFont(firmaSecondaryBankLabel);
		
		firmaSecondaryBank = new Text(container, SWT.BORDER);
		firmaSecondaryBank.setText(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_KEY));
		UIUtils.setFont(firmaSecondaryBank);
		GridDataFactory.fillDefaults().applyTo(firmaSecondaryBank);
		
		final Label firmaSecondaryBankAccLabel = new Label(container, SWT.NONE);
		firmaSecondaryBankAccLabel.setText(Messages.SettingsPart_SecondaryIBAN);
		UIUtils.setFont(firmaSecondaryBankAccLabel);
		
		firmaSecondaryBankAcc = new Text(container, SWT.BORDER);
		firmaSecondaryBankAcc.setText(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_ACC_KEY));
		UIUtils.setFont(firmaSecondaryBankAcc);
		GridDataFactory.fillDefaults().applyTo(firmaSecondaryBankAcc);
		
		final Label firmaAddressLabel = new Label(container, SWT.NONE);
		firmaAddressLabel.setText(Messages.SettingsPart_Address);
		UIUtils.setFont(firmaAddressLabel);
		
		firmaAddress = new Text(container, SWT.BORDER);
		firmaAddress.setText(firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY));
		UIUtils.setFont(firmaAddress);
		GridDataFactory.fillDefaults().applyTo(firmaAddress);
		
		final Label firmaBillingAddressLabel = new Label(container, SWT.NONE);
		firmaBillingAddressLabel.setText(Messages.SettingsPart_MainAddress);
		UIUtils.setFont(firmaBillingAddressLabel);
		
		firmaBillingAddress = new Text(container, SWT.BORDER);
		firmaBillingAddress.setText(firmaDetails.extraString(PersistedProp.FIRMA_BILLING_ADDRESS_KEY));
		UIUtils.setFont(firmaBillingAddress);
		GridDataFactory.fillDefaults().applyTo(firmaBillingAddress);
		
		final Label firmaPhoneLabel = new Label(container, SWT.NONE);
		firmaPhoneLabel.setText(Messages.SettingsPart_Phone);
		UIUtils.setFont(firmaPhoneLabel);
		
		firmaPhone = new Text(container, SWT.BORDER);
		firmaPhone.setText(firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY));
		UIUtils.setFont(firmaPhone);
		GridDataFactory.fillDefaults().applyTo(firmaPhone);
		
		final Label firmaEmailLabel = new Label(container, SWT.NONE);
		firmaEmailLabel.setText(Messages.Email);
		UIUtils.setFont(firmaEmailLabel);
		
		firmaEmail = new Text(container, SWT.BORDER);
		firmaEmail.setText(firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		UIUtils.setFont(firmaEmail);
		GridDataFactory.fillDefaults().applyTo(firmaEmail);
		
		final Label firmaWebsiteLabel = new Label(container, SWT.NONE);
		firmaWebsiteLabel.setText(Messages.SettingsPart_Website);
		UIUtils.setFont(firmaWebsiteLabel);
		
		firmaWebsite = new Text(container, SWT.BORDER);
		firmaWebsite.setText(firmaDetails.extraString(PersistedProp.FIRMA_WEBSITE_KEY));
		UIUtils.setFont(firmaWebsite);
		GridDataFactory.fillDefaults().applyTo(firmaWebsite);
		
		final Label tvaLabel = new Label(container, SWT.NONE);
		tvaLabel.setText(Messages.SettingsPart_VATPerc);
		UIUtils.setFont(tvaLabel);
		
		tvaReadable = new Text(container, SWT.BORDER);
		tvaReadable.setText(Operatiune.tvaReadable(tvaPercentDB));
		UIUtils.setFont(tvaReadable);
		
		final Label migrationDateLabel = new Label(container, SWT.NONE);
		migrationDateLabel.setText(Messages.SettingsPart_MigrationDate);
		UIUtils.setFont(migrationDateLabel);
		
		migrationDate = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		insertDate(migrationDate, migrationDateDB);
		UIUtils.setFont(migrationDate);
		
		final Label promoLabel = new Label(container, SWT.NONE);
		promoLabel.setText(Messages.SettingsPart_PromoReceiptNo);
		UIUtils.setFont(promoLabel);
		
		nrBonPromo = new Text(container, SWT.BORDER);
		nrBonPromo.setText(nrBonPromoDB);
		UIUtils.setFont(nrBonPromo);
		
		final Label textAromaLabel = new Label(container, SWT.NONE);
		textAromaLabel.setText(Messages.SettingsPart_AromaPrefix);
		UIUtils.setFont(textAromaLabel);
		
		prefixAroma = new Text(container, SWT.BORDER);
		prefixAroma.setText(prefixAromaDB);
		UIUtils.setFont(prefixAroma);
		GridDataFactory.fillDefaults().applyTo(prefixAroma);
		
		new Label(container, SWT.NONE);//layout
		
		save = new Button(container, SWT.PUSH);
		save.setText(Messages.Save);
		save.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		save.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(save);
	}

	private void addListeners()
	{
		addUser.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final UserDialog userDialog = new UserDialog(bundle, log, Display.getCurrent().getActiveShell(), new User());
				userDialog.setOkSupplier(() -> adaugaUser(userDialog));
				userDialog.open();
			}
		});
		
		editUser.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedUser().isPresent())
				{
					final User loadedUser = BusinessDelegate.userById(selectedUser().get().getId());
					final UserDialog userDialog = new UserDialog(bundle, log, Display.getCurrent().getActiveShell(), loadedUser);
					userDialog.setOkSupplier(() -> editUser(userDialog));
					userDialog.open();
				}
			}
		});
		
		deleteUser.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<User> selectedUser = selectedUser();
				if (selectedUser.isPresent() && MessageDialog.openQuestion(deleteUser.getShell(), Messages.SettingsPart_DeleteUser, Messages.SettingsPart_DeleteUserTitle))
				{
					final InvocationResult result = BusinessDelegate.removeUser(selectedUser.get().getId());
					showResult(result);
					if (result.statusOk())
					{
						allUsers = removeFromImmutableList(allUsers, selectedUser.get());
						users.setItems(allUsers.stream().map(u -> u.getId()+": "+u.displayName()).toArray(String[]::new)); //$NON-NLS-1$
					}
				}
			}
		});
		
		addRole.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final RoleDialog roleDialog = new RoleDialog(Display.getCurrent().getActiveShell(), new Role());
				roleDialog.setOkSupplier(() -> adaugaRole(roleDialog));
				roleDialog.open();
			}
		});
		
		editRole.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedRole().isPresent())
				{
					final RoleDialog roleDialog = new RoleDialog(Display.getCurrent().getActiveShell(), selectedRole().get());
					roleDialog.setOkSupplier(() -> editRole(roleDialog));
					roleDialog.open();
				}
			}
		});
		
		deleteRole.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<Role> selectedRole = selectedRole();
				if (selectedRole.isPresent() && MessageDialog.openQuestion(deleteRole.getShell(), Messages.SettingsPart_DeleteRole, Messages.SettingsPart_DeleteRoleMessage))
				{
					final InvocationResult result = BusinessDelegate.deleteRole(selectedRole.get().getId());
					showResult(result);
					if (result.statusOk())
					{
						allRoles = removeFromImmutableList(allRoles, selectedRole.get());
						roles.setItems(allRoles.stream().map(Role::getName).toArray(String[]::new));
					}
				}
			}
		});
		
		addGestiune.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final GestiuneDialog gestiuneDialog = new GestiuneDialog(Display.getCurrent().getActiveShell(), new Gestiune());
				gestiuneDialog.setOkSupplier(() -> adaugaGestiune(gestiuneDialog));
				gestiuneDialog.open();
			}
		});
		
		editGestiune.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedGestiune().isPresent())
				{
					final GestiuneDialog gestiuneDialog = new GestiuneDialog(Display.getCurrent().getActiveShell(), selectedGestiune().get());
					gestiuneDialog.setOkSupplier(() -> editGestiune(gestiuneDialog));
					gestiuneDialog.open();
				}
			}
		});
		
		addMasina.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final MasinaDialog masinaDialog = new MasinaDialog(Display.getCurrent().getActiveShell(), new Masina());
				masinaDialog.setOkSupplier(() -> adaugaMasina(masinaDialog));
				masinaDialog.open();
			}
		});
		
		editMasina.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedMasina().isPresent())
				{
					final MasinaDialog masinaDialog = new MasinaDialog(Display.getCurrent().getActiveShell(), selectedMasina().get());
					masinaDialog.setOkSupplier(() -> editMasina(masinaDialog));
					masinaDialog.open();
				}
			}
		});
		
		deleteMasina.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<Masina> selectedMasina = selectedMasina();
				if (selectedMasina.isPresent() && MessageDialog.openQuestion(deleteMasina.getShell(), Messages.SettingsPart_DeleteCar, Messages.SettingsPart_DeleteCarMessage))
				{
					final InvocationResult result = BusinessDelegate.deleteMasina(selectedMasina.get().getId());
					showResult(result);
					if (result.statusOk())
					{
						allMasini = removeFromImmutableList(allMasini, selectedMasina.get());
						masini.setItems(allMasini.stream().map(Masina::displayName).toArray(String[]::new));
					}
				}
			}
		});
		
		addContBancar.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ContBancarDialog contBancarDialog = new ContBancarDialog(Display.getCurrent().getActiveShell(), new ContBancar());
				contBancarDialog.setOkSupplier(() -> adaugaContBancar(contBancarDialog));
				contBancarDialog.open();
			}
		});

		editContBancar.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedContBancar().isPresent())
				{
					final ContBancarDialog contBancarDialog = new ContBancarDialog(Display.getCurrent().getActiveShell(), selectedContBancar().get());
					contBancarDialog.setOkSupplier(() -> editContBancar(contBancarDialog));
					contBancarDialog.open();
				}
			}
		});
		
		save.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				onSave();
			}
		});
		
		final ModifyListener modifyListener = e -> part.setDirty(true);
		
		firmaName.addModifyListener(modifyListener);
		firmaCui.addModifyListener(modifyListener);
		firmaRegCom.addModifyListener(modifyListener);
		firmaCapSocial.addModifyListener(modifyListener);
		firmaMainBank.addModifyListener(modifyListener);
		firmaMainBankAcc.addModifyListener(modifyListener);
		firmaSecondaryBank.addModifyListener(modifyListener);
		firmaSecondaryBankAcc.addModifyListener(modifyListener);
		firmaAddress.addModifyListener(modifyListener);
		firmaBillingAddress.addModifyListener(modifyListener);
		firmaPhone.addModifyListener(modifyListener);
		firmaEmail.addModifyListener(modifyListener);
		firmaWebsite.addModifyListener(modifyListener);
		tvaReadable.addModifyListener(modifyListener);
		nrBonPromo.addModifyListener(modifyListener);
		prefixAroma.addModifyListener(modifyListener);
		migrationDate.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				part.setDirty(true);
			}
		});
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			BusinessDelegate.updateFirmaDetails(fillFirmaDetails());
			updateFirmaDetailFields(BusinessDelegate.firmaDetails());
			
			if (!Operatiune.tvaReadable(tvaPercentDB).equalsIgnoreCase(tvaReadable.getText()))
				updateTvaReadable();
			
			if (!nrBonPromoDB.equalsIgnoreCase(nrBonPromo.getText()))
				updateNrBonPromo();
			
			if (!prefixAromaDB.equalsIgnoreCase(prefixAroma.getText()))
				updatePrefixAroma();
			
			final LocalDate newMigrationDate = extractLocalDate(migrationDate);
			if (!newMigrationDate.equals(migrationDateDB))
			{
				BusinessDelegate.updatePersistedProp(PersistedProp.MIGRATION_DATE_KEY, newMigrationDate.toString());
				migrationDateDB = LocalDate.parse(BusinessDelegate.persistedProp(PersistedProp.MIGRATION_DATE_KEY)
						.getValueOr(PersistedProp.MIGRATION_DATE_DEFAULT));
				insertDate(migrationDate, migrationDateDB);
			}
			
			part.setDirty(false);
		}
	}
	
	private void updateTvaReadable()
	{
		final BigDecimal tvaPercent = extractPercentage(tvaReadable.getText());
		if (smallerThan(tvaPercent, BigDecimal.ZERO))
			return;
		
		BusinessDelegate.updatePersistedProp(PersistedProp.TVA_PERCENT_KEY, tvaPercent.toString());
		
		tvaPercentDB = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		tvaReadable.setText(Operatiune.tvaReadable(tvaPercentDB));
	}
	
	private void updateNrBonPromo()
	{
		final BigDecimal newNrBon = parse(nrBonPromo.getText());
		BusinessDelegate.updatePersistedProp(PersistedProp.NR_BON_PROMO_KEY, newNrBon.setScale(0, RoundingMode.HALF_EVEN).toString());
		
		nrBonPromoDB = BusinessDelegate.persistedProp(PersistedProp.NR_BON_PROMO_KEY)
				.getValueOr(PersistedProp.NR_BON_PROMO_DEFAULT);
		nrBonPromo.setText(nrBonPromoDB);
	}
	
	private void updatePrefixAroma()
	{
		BusinessDelegate.updatePersistedProp(PersistedProp.PREFIX_AROMA_KEY, prefixAroma.getText());
		
		prefixAromaDB = BusinessDelegate.persistedProp(PersistedProp.PREFIX_AROMA_KEY)
				.getValueOr(PersistedProp.PREFIX_AROMA_DEFAULT);
		prefixAroma.setText(prefixAromaDB);
	}
	
	private InvocationResult fillFirmaDetails()
	{
		final Builder<String, Object> details = ImmutableMap.<String, Object>builder();
		details.put(PersistedProp.FIRMA_NAME_KEY, firmaName.getText());
		details.put(PersistedProp.FIRMA_CUI_KEY, firmaCui.getText());
		details.put(PersistedProp.FIRMA_REG_COM_KEY, firmaRegCom.getText());
		details.put(PersistedProp.FIRMA_CAP_SOCIAL_KEY, firmaCapSocial.getText());
		details.put(PersistedProp.FIRMA_MAIN_BANK_KEY, firmaMainBank.getText());
		details.put(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY, firmaMainBankAcc.getText());
		details.put(PersistedProp.FIRMA_SECONDARY_BANK_KEY, firmaSecondaryBank.getText());
		details.put(PersistedProp.FIRMA_SECONDARY_BANK_ACC_KEY, firmaSecondaryBankAcc.getText());
		details.put(PersistedProp.FIRMA_ADDRESS_KEY, firmaAddress.getText());
		details.put(PersistedProp.FIRMA_BILLING_ADDRESS_KEY, firmaBillingAddress.getText());
		details.put(PersistedProp.FIRMA_PHONE_KEY, firmaPhone.getText());
		details.put(PersistedProp.FIRMA_EMAIL_KEY, firmaEmail.getText());
		details.put(PersistedProp.FIRMA_WEBSITE_KEY, firmaWebsite.getText());
		return InvocationResult.ok(details.build());
	}
	
	private void updateFirmaDetailFields(final InvocationResult invocationResult)
	{
		firmaDetails = invocationResult;
		firmaName.setText(firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY));
		firmaCui.setText(firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY));
		firmaRegCom.setText(firmaDetails.extraString(PersistedProp.FIRMA_REG_COM_KEY));
		firmaCapSocial.setText(firmaDetails.extraString(PersistedProp.FIRMA_CAP_SOCIAL_KEY));
		firmaMainBank.setText(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_KEY));
		firmaMainBankAcc.setText(firmaDetails.extraString(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY));
		firmaSecondaryBank.setText(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_KEY));
		firmaSecondaryBankAcc.setText(firmaDetails.extraString(PersistedProp.FIRMA_SECONDARY_BANK_ACC_KEY));
		firmaAddress.setText(firmaDetails.extraString(PersistedProp.FIRMA_ADDRESS_KEY));
		firmaBillingAddress.setText(firmaDetails.extraString(PersistedProp.FIRMA_BILLING_ADDRESS_KEY));
		firmaPhone.setText(firmaDetails.extraString(PersistedProp.FIRMA_PHONE_KEY));
		firmaEmail.setText(firmaDetails.extraString(PersistedProp.FIRMA_EMAIL_KEY));
		firmaWebsite.setText(firmaDetails.extraString(PersistedProp.FIRMA_WEBSITE_KEY));
	}
	
	private boolean adaugaUser(final UserDialog userDialog)
	{
		final User userToCreate = userDialog.filledUser();
		userToCreate.setSelectedGestiune(null);
		userToCreate.getRoles().clear();
		userToCreate.getMasini().clear();

		if (isEmpty(userToCreate.getPassword()) && !userDialog.faraLogin())
		{
			userDialog.setErrorMessage(Messages.SettingsPart_SetPass);
			return false;
		}
		
		if (userToCreate.getPassword().length() < 8 && !userDialog.faraLogin())
		{
			userDialog.setErrorMessage(Messages.SettingsPart_Pass8Chars);
			return false;
		}

		final InvocationResult result = BusinessDelegate.createUser_ByAdmin(userToCreate, userDialog.selectedRole().get().getId(),
				userDialog.selectedGestiune().get().getId(), userDialog.selectedMasiniId(), userDialog.faraLogin());
		
		if (result.statusOk())
		{
			final User createdUser = result.extra(InvocationResult.USER_KEY);
			allUsers = addToImmutableList(allUsers, createdUser);
			users.setItems(allUsers.stream().map(u -> u.getId()+": "+u.displayName()).toArray(String[]::new)); //$NON-NLS-1$
			updateImages(createdUser, userDialog);
			ClientSession.instance().reloadBillImages();
			return true;
		}
		else
		{
			userDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean editUser(final UserDialog userDialog)
	{
		final User user = userDialog.filledUser();

		if (!isEmpty(user.getPassword()) && user.getPassword().length() < 8 && !userDialog.faraLogin())
		{
			userDialog.setErrorMessage(Messages.SettingsPart_Pass8Chars);
			return false;
		}
		
		updateImages(user, userDialog);
		BusinessDelegate.updateUser(user, userDialog.faraLogin());
		if (user.getId() == ClientSession.instance().getLoggedUser().getId())
		{
			ClientSession.instance().setUsername(user.getEmail());
			if (!isEmpty(user.getPassword()))
				ClientSession.instance().setPassword(user.getPassword());
			ClientSession.instance().login();
		}
		else
			ClientSession.instance().reloadBillImages();
		
		allUsers = BusinessDelegate.dbUsers();
		users.setItems(allUsers.stream().map(u -> u.getId()+": "+u.displayName()).toArray(String[]::new)); //$NON-NLS-1$
		return true;
	}
	
	private void updateImages(final User user, final UserDialog userDialog)
	{
		// update images
		final int companyId = ClientSession.instance().getLoggedUser().getSelectedCompany().getId();
		final byte[] semnaturaImage = userDialog.getSemnaturaImage().getSelectedImage();
		final byte[] stampilaImage = userDialog.getStampilaImage().getSelectedImage();
		final LobImage semnaturaLobImg = new LobImage();
		semnaturaLobImg.setImage(semnaturaImage);
		final LobImage stampilaLobImg = new LobImage();
		stampilaLobImg.setImage(stampilaImage);
		BusinessDelegate.mergeImageLobs(ImmutableList.of(semnaturaLobImg, stampilaLobImg));
		// update bill uuids
		final String signatureProp = user.getId()+PersistedProp.PROP_VALUE_SEP+companyId+PersistedProp.PROP_VALUE_SEP+safeString(semnaturaLobImg.getImageUUID());
		final String stampProp = user.getId()+PersistedProp.PROP_VALUE_SEP+companyId+PersistedProp.PROP_VALUE_SEP+safeString(stampilaLobImg.getImageUUID());
		BusinessDelegate.updateBillImages(signatureProp, stampProp);
		// END
	}
	
	private boolean adaugaRole(final RoleDialog roleDialog)
	{
		final Role roleToCreate = roleDialog.filledRole();

		final InvocationResult result = BusinessDelegate.persistNewRole(roleToCreate, roleDialog.selectedPermissionNames());
		
		if (result.statusOk())
		{
			allRoles = addToImmutableList(allRoles, result.extra(InvocationResult.ROLE_KEY));
			roles.setItems(allRoles.stream().map(Role::getName).toArray(String[]::new));
			return true;
		}
		else
		{
			roleDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean editRole(final RoleDialog roleDialog)
	{
		final Role role = roleDialog.filledRole();
		final InvocationResult result = BusinessDelegate.updateRole(role.getId(), roleDialog.selectedPermissionNames(), role.getName());
		
		if (result.statusOk())
		{
			allRoles = BusinessDelegate.dbRoles();
			roles.setItems(allRoles.stream().map(Role::getName).toArray(String[]::new));
			return true;
		}
		else
		{
			roleDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean adaugaGestiune(final GestiuneDialog gestiuneDialog)
	{
		final Gestiune gestiuneToCreate = gestiuneDialog.filledGestiune();
		final InvocationResult result = BusinessDelegate.persistGestiune(gestiuneToCreate.getName(), gestiuneToCreate.getImportName());
		
		if (result.statusOk())
		{
			allGestiuni = addToImmutableList(allGestiuni, result.extra(InvocationResult.GESTIUNE_KEY));
			gestiuni.setItems(allGestiuni.stream().map(g -> g.getId()+": "+g.getName()).toArray(String[]::new)); //$NON-NLS-1$
			return true;
		}
		else
		{
			gestiuneDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean editGestiune(final GestiuneDialog gestiuneDialog)
	{
		final Gestiune gestiune = gestiuneDialog.filledGestiune();
		final InvocationResult result = BusinessDelegate.updateGestiune(gestiune.getId(), gestiune.getName(), gestiune.getImportName());
		
		if (result.statusOk())
		{
			allGestiuni = BusinessDelegate.allGestiuni();
			gestiuni.setItems(allGestiuni.stream().map(g -> g.getId()+": "+g.getName()).toArray(String[]::new)); //$NON-NLS-1$
			return true;
		}
		else
		{
			gestiuneDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean adaugaMasina(final MasinaDialog masinaDialog)
	{
		final Masina masinaToCreate = masinaDialog.filledMasina();
		final InvocationResult result = BusinessDelegate.persistMasina(masinaToCreate.getNr(), masinaToCreate.getMarca(), masinaToCreate.getCuloare(),
				masinaDialog.selectedGestiune().map(Gestiune::getId).get());
		
		if (result.statusOk())
		{
			allMasini = addToImmutableList(allMasini, result.extra(InvocationResult.MASINA_KEY));
			masini.setItems(allMasini.stream().map(Masina::displayName).toArray(String[]::new));
			return true;
		}
		else
		{
			masinaDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean editMasina(final MasinaDialog masinaDialog)
	{
		final Masina masina = masinaDialog.filledMasina();
		final InvocationResult result = BusinessDelegate.updateMasina(masina.getId(), masina.getNr(), masina.getMarca(), masina.getCuloare(),
				masinaDialog.selectedGestiune().map(Gestiune::getId).get());
		
		if (result.statusOk())
		{
			allMasini = BusinessDelegate.dbMasini();
			masini.setItems(allMasini.stream().map(Masina::displayName).toArray(String[]::new));
			return true;
		}
		else
		{
			masinaDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private boolean adaugaContBancar(final ContBancarDialog contBancarDialog)
	{
		final ContBancar contBancarToCreate = contBancarDialog.filledContBancar();
		final InvocationResult result = BusinessDelegate.persistContBancar(contBancarToCreate.getName(),
				contBancarToCreate.getIban(), contBancarToCreate.getBanca(), contBancarToCreate.getValuta());

		if (result.statusOk())
		{
			allConturiBancare = addToImmutableList(allConturiBancare, result.extra(InvocationResult.CONT_BANCAR_KEY));
			conturiBancare.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
			return true;
		}
		else
		{
			contBancarDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}

	private boolean editContBancar(final ContBancarDialog contBancarDialog)
	{
		final ContBancar contBancar = contBancarDialog.filledContBancar();
		final InvocationResult result = BusinessDelegate.updateContBancar(contBancar.getId(), contBancar.getName(),
				contBancar.getIban(), contBancar.getBanca(), contBancar.getValuta());

		if (result.statusOk())
		{
			allConturiBancare = BusinessDelegate.allConturiBancare();
			conturiBancare.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
			return true;
		}
		else
		{
			contBancarDialog.setErrorMessage(result.toTextDescriptionWithCode());
			return false;
		}
	}
	
	private Optional<User> selectedUser()
	{
		final int index = users.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allUsers.get(index));
	}
	
	private Optional<Role> selectedRole()
	{
		final int index = roles.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allRoles.get(index));
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiuni.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
	
	private Optional<Masina> selectedMasina()
	{
		final int index = masini.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allMasini.get(index));
	}
	
	private Optional<ContBancar> selectedContBancar()
	{
		final int index = conturiBancare.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allConturiBancare.get(index));
	}
}
