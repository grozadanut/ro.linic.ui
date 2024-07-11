package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.parseToInt;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.setFont;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import jakarta.ejb.EJBException;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.ServiceLocator;

public class LoginDialog extends TitleAreaDialog
{
	private static final ILog log = ILog.of(LoginDialog.class);
	
	public static final String DB_USERS_PROP = "all_db_users"; //$NON-NLS-1$
	public static final String DB_COMPANIES_PROP = "all_db_companies"; //$NON-NLS-1$
	
	private static final String PROP_ROW_SEP = ";"; //$NON-NLS-1$
	private static final String PROP_VALUE_SEP = ":"; //$NON-NLS-1$
	private static final String PROP_INNER_ROW_SEP = ">"; //$NON-NLS-1$
	private static final String PROP_INNER_VALUE_SEP = "-"; //$NON-NLS-1$
	
	private ImmutableMap<User, List<Company>> users;
	private ImmutableMap<Company, List<Gestiune>> companies;
	
	// Widgets
	private Text email;
	private org.eclipse.swt.widgets.List username;
	private Text password;
	private org.eclipse.swt.widgets.List company;
	private org.eclipse.swt.widgets.List gestiune;
//	private org.eclipse.swt.widgets.Button roLocale;
//	private org.eclipse.swt.widgets.Button enLocale;
	
	private IEclipsePreferences prefs;
	private IEclipseContext ctx;
	
	public static String toProp(final ImmutableMap<User, List<Company>> users)
	{
		return users.entrySet().stream()
				.map(e -> e.getKey().getId()+PROP_VALUE_SEP+e.getKey().getEmail()+PROP_VALUE_SEP+e.getKey().getName()
						+PROP_VALUE_SEP+safeString(e.getKey().getSelectedCompany(), Company::getId, String::valueOf)
						+PROP_VALUE_SEP+safeString(e.getKey().getSelectedGestiune(), Gestiune::getId, String::valueOf)
						+PROP_VALUE_SEP+toProp(e.getValue()))
				.collect(Collectors.joining(PROP_ROW_SEP));
	}
	
	private static String toProp(final List<Company> companies)
	{
		return companies.stream()
				.map(c -> c.getId()+PROP_INNER_VALUE_SEP+c.getName())
				.collect(Collectors.joining(PROP_INNER_ROW_SEP));
	}
	
	public static String toPropComp(final ImmutableMap<Company, List<Gestiune>> companies)
	{
		return companies.entrySet().stream()
				.map(e -> e.getKey().getId()+PROP_VALUE_SEP+e.getKey().getName()
						+PROP_VALUE_SEP+toPropGest(e.getValue()))
				.collect(Collectors.joining(PROP_ROW_SEP));
	}
	
	private static String toPropGest(final List<Gestiune> gestiuni)
	{
		return gestiuni.stream()
				.map(g -> g.getId()+PROP_INNER_VALUE_SEP+g.getName())
				.collect(Collectors.joining(PROP_INNER_ROW_SEP));
	}
	
	private static ImmutableMap<User, List<Company>> fromProp(final String propValue)
	{
		if (isEmpty(propValue))
			return ImmutableMap.of();
		
		final com.google.common.collect.ImmutableMap.Builder<User, List<Company>> b = ImmutableMap.<User, List<Company>>builder();
		final String[] rows = propValue.split(PROP_ROW_SEP);
		
		for (final String userRow : rows)
		{
			final String[] userToken = userRow.split(PROP_VALUE_SEP);
			final User user = new User();
			user.setId(parseToInt(userToken[0]));
			user.setEmail(userToken[1]);
			user.setName(userToken[2]);
			user.setSelectedCompany(new Company().setId(parseToInt(userToken[3])));
			user.setSelectedGestiune(new Gestiune().setId(parseToInt(userToken[4])));
			final String[] companyRows = userToken[5].split(PROP_INNER_ROW_SEP);
			final List<Company> companies = new ArrayList<Company>();
			for (final String companyRow : companyRows)
			{
				final String[] companyToken = companyRow.split(PROP_INNER_VALUE_SEP);
				final Company company = new Company();
				company.setId(Integer.parseInt(companyToken[0]));
				company.setName(companyToken[1]);
				companies.add(company);
			}
			b.put(user, companies);
		}
		
		return b.build();
	}
	
	private static ImmutableMap<Company, List<Gestiune>> fromPropComp(final String propValue)
	{
		if (isEmpty(propValue))
			return ImmutableMap.of();
		
		final com.google.common.collect.ImmutableMap.Builder<Company, List<Gestiune>> b = ImmutableMap.<Company, List<Gestiune>>builder();
		final String[] rows = propValue.split(PROP_ROW_SEP);
		
		for (final String companyRow : rows)
		{
			final String[] companyToken = companyRow.split(PROP_VALUE_SEP);
			final Company company = new Company();
			company.setId(parseToInt(companyToken[0]));
			company.setName(companyToken[1]);
			final String[] gestRows = companyToken[2].split(PROP_INNER_ROW_SEP);
			final List<Gestiune> gestiuni = new ArrayList<Gestiune>();
			for (final String gestRow : gestRows)
			{
				final String[] gestToken = gestRow.split(PROP_INNER_VALUE_SEP);
				final Gestiune gestiune = new Gestiune();
				gestiune.setId(Integer.parseInt(gestToken[0]));
				gestiune.setName(gestToken[1]);
				gestiuni.add(gestiune);
			}
			b.put(company, gestiuni);
		}
		
		return b.build();
	}

	public LoginDialog(final Shell parent, final IEclipsePreferences prefs, final IEclipseContext ctx)
	{
		super(parent);
		users = fromProp(prefs.get(DB_USERS_PROP, EMPTY_STRING));
		companies = fromPropComp(prefs.get(DB_COMPANIES_PROP, EMPTY_STRING));
		this.prefs = prefs;
		this.ctx = ctx;
	}

	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle(Messages.LoginDialog_Title);
		setMessage(Messages.LoginDialog_Message);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label userLabel = new Label(container, SWT.NULL);
		userLabel.setText(Messages.LoginDialog_User);
		setFont(userLabel);
		
		email = new Text(container, SWT.SINGLE | SWT.BORDER);
		setFont(email);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(email);
		
		final Label passLabel = new Label(container, SWT.NULL);
		passLabel.setText(Messages.LoginDialog_Password);
		setFont(passLabel);

		password = new Text(container, SWT.PASSWORD | SWT.BORDER);
		setFont(password);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(password);

		username = new org.eclipse.swt.widgets.List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		username.setLayoutData(new GridData(GridData.FILL_BOTH));
		username.setItems(users.keySet().stream().map(User::displayName).toArray(String[]::new));
		setFont(username);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(username);
		
		final Label companyLabel = new Label(container, SWT.NONE);
		companyLabel.setText(Messages.LoginDialog_Company);
		setFont(companyLabel);
		company = new org.eclipse.swt.widgets.List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		company.setLayoutData(new GridData(GridData.FILL_BOTH));
		setFont(company);
		GridDataFactory.fillDefaults().applyTo(company);
		
		final Label gestiuneLabel = new Label(container, SWT.NONE);
		gestiuneLabel.setText(Messages.LoginDialog_Inventory);
		setFont(gestiuneLabel);
		gestiune = new org.eclipse.swt.widgets.List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		gestiune.setLayoutData(new GridData(GridData.FILL_BOTH));
		setFont(gestiune);
		GridDataFactory.fillDefaults().applyTo(gestiune);
		
//		final Composite bottomBar = new Composite(parent, SWT.NONE);
//		bottomBar.setLayout(new GridLayout(2, false));
//		GridDataFactory.swtDefaults().span(2, 1).align(SWT.FILL, SWT.TOP).applyTo(bottomBar);
//		
//		roLocale = new Button(bottomBar, SWT.RADIO);
//		roLocale.setImage(Icons.createImageResource(FrameworkUtil.getBundle(getClass()), Icons.RO_FLAG_32x32_PATH, ILog.get()).orElse(null));
//		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(roLocale);
//		
//		enLocale = new Button(bottomBar, SWT.RADIO);
//		enLocale.setImage(Icons.createImageResource(FrameworkUtil.getBundle(getClass()), Icons.USA_FLAG_32x32_PATH, ILog.get()).orElse(null));
//		
//		final Locale locale = Locale.getDefault();
//		if (locale != null && "ro".equalsIgnoreCase(locale.getLanguage()))
//			roLocale.setSelection(true);
//		else
//			enLocale.setSelection(true);
		
		username.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				email.setText(getSelectedUser().map(User::getEmail).orElse(EMPTY_STRING));
				refreshCompany();
				refreshGestiune();
				validate();
			}
		});
		company.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				refreshGestiune();
				validate();
			}
		});
		gestiune.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				validate();
			}
		});
		email.addModifyListener(e -> validate());
		password.addModifyListener(e -> validate());
		
//		roLocale.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(final SelectionEvent e) {
//				ctx.get(LocaleService.class).changeLocale(new Locale("ro", "RO"));
//			}
//		});
//		
//		roLocale.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(final SelectionEvent e) {
//				ctx.get(LocaleService.class).changeLocale(Locale.ENGLISH);
//			}
//		});
		
		return area;
	}
	
	private void refreshCompany()
	{
		final Optional<User> selUser = getSelectedUser();
		
		if (!selUser.isPresent())
			company.removeAll();
		else
		{
			final Company usersCompany = selUser.get().getSelectedCompany();
			company.setItems(users.get(selUser.get()).stream().map(Company::displayName).toArray(String[]::new));
			users.get(selUser.get()).stream()
			.filter(c -> c.getId() == usersCompany.getId())
			.findFirst()
			.ifPresent(c -> company.setSelection(new String[] {c.displayName()}));
		}
	}
	
	private void refreshGestiune()
	{
		final Optional<User> selUser = getSelectedUser();
		final Optional<Company> selCompany = getSelectedCompany();
		
		if (!selUser.isPresent() || !selCompany.isPresent())
			gestiune.removeAll();
		else
		{
			final Gestiune usersGest = selUser.get().getSelectedGestiune();
			gestiune.setItems(companies.get(selCompany.get()).stream().map(Gestiune::displayName).toArray(String[]::new));
			companies.get(selCompany.get()).stream()
			.filter(g -> g.getId() == usersGest.getId())
			.findFirst()
			.ifPresent(g -> gestiune.setSelection(new String[] {g.displayName()}));
		}
	}
	
	private void validate()
	{
		if (isEmpty(email.getText()))
		{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setErrorMessage(Messages.LoginDialog_MissingUser);
			return;
		}
		
		if (isEmpty(password.getText()))
		{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setErrorMessage(Messages.LoginDialog_MissingPassword);
			return;
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		setErrorMessage(null);
	}
	
	@Override
	protected void okPressed()
	{
		ServiceLocator.clearCache();
		ClientSession.instance().setUsername(email.getText());
		ClientSession.instance().setPassword(password.getText());

		try
		{
			final Optional<User> selUser = getSelectedUser();
			final Optional<Gestiune> selGest = getSelectedGestiune();
			if (selUser.isPresent() && selGest.isPresent())
			{
				try {
					final InvocationResult result = BusinessDelegate.changeGestiune(selUser.get().getId(), selGest.get().getId());
					if (result.statusCanceled())
					{
						setErrorMessage(result.toTextDescriptionWithCode());
						return;
					}
				} catch (final Exception e) {
					log.error(e.getMessage(), e);
				}
			}
			
			final User user = ClientSession.instance().login();
			if (user != null)
			{
				prefs.put(DB_USERS_PROP, toProp(BusinessDelegate.usersWithCompanyRoles()));
				prefs.put(DB_COMPANIES_PROP, toPropComp(BusinessDelegate.companiesWithGestiuni()));
				try {
					prefs.flush();
				} catch (final BackingStoreException e) {
					log.error(e.getMessage(), e);
				}
				super.okPressed();
			}
			else
				setErrorMessage(Messages.LoginDialog_UserNotFound);
		}
		catch (final EJBException e)
		{
			log.error(e.getMessage(), e);
			if (e.getCausedByException() instanceof ConnectException)
				setErrorMessage(Messages.LoginDialog_ConnectionError);
			else
				setErrorMessage(Messages.LoginDialog_WrongPassword);
		}
	}
	
	private Optional<User> getSelectedUser()
	{
		final int selectionIndex = username.getSelectionIndex();
		if (selectionIndex == -1)
			return Optional.empty();
		
		return Optional.ofNullable(users.keySet().asList().get(selectionIndex));
	}
	
	private Optional<Company> getSelectedCompany()
	{
		if (company.getSelection().length == 0)
			return Optional.empty();
		
		return companies.keySet().stream()
				.filter(c -> globalIsMatch(c.displayName(), company.getSelection()[0], TextFilterMethod.EQUALS))
				.findFirst();
	}
	
	private Optional<Gestiune> getSelectedGestiune()
	{
		if (gestiune.getSelection().length == 0)
			return Optional.empty();
		
		return companies.getOrDefault(getSelectedCompany().orElse(null), ImmutableList.of()).stream()
				.filter(g -> globalIsMatch(g.displayName(), gestiune.getSelection()[0], TextFilterMethod.EQUALS))
				.findFirst();
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(400, 600);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
}