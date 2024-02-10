package ro.linic.ui.legacy.session;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.naming.Context;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;

public class ClientSession
{
	private static ClientSession instance;
	
	public static final String INITIAL_CONTEXT_FACTORY_VALUE = "org.wildfly.naming.client.WildFlyInitialContextFactory";
	public static final String PROVIDER_URL_VALUE = System.getProperty(Context.PROVIDER_URL, "http-remoting://localhost:8080");
	
	private String username;
	private String password;
	private User loggedUser;
	private String billSignatureProp;
	private String billStampProp;
	
	private Properties properties = new Properties();
	private List<Control> hideStateControls = new ArrayList<>();
	
	// caching
	private ImmutableList<PersistedProp> allPersistedProps;
	
	public static ClientSession instance()
	{
		if (instance == null)
			instance = new ClientSession();
		
		return instance;
	}
	
	private ClientSession()
	{
	}
	
	public User login(final Logger log)
	{
		loggedUser = BusinessDelegate.login();
		MessagingService.instance().closeSession(log);
		MessagingService.instance().restoreListeners(log);
		reloadBillImages();
		return loggedUser;
	}
	
	public void reloadBillImages()
	{
		final InvocationResult billImages = BusinessDelegate.billImages();
		billSignatureProp = billImages.extraString(PersistedProp.BILL_SIGNATURE_KEY);
		billStampProp = billImages.extraString(PersistedProp.BILL_STAMP_KEY);
	}
	
	public boolean hasPermission(final String permission)
	{
		return isLoggedIn() && getLoggedUser().getRole().hasPermission(permission);
	}
	
	public boolean hasStrictPermission(final String permission)
	{
		return isLoggedIn() && getLoggedUser().getRole().hasStrictPermission(permission);
	}
	
	public void toggleHideUnofficialDocs(final Logger log)
	{
		BusinessDelegate.toggleHideUnofficialDocs();
		login(log);
		
		final Color bgColor = isLoggedIn() && getLoggedUser().isHideUnofficialDocs() ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				
		hideStateControls.forEach(control -> control.setBackground(bgColor));
	}
	
	public void addHideStateControl(final Control control)
	{
		this.hideStateControls.add(control);
		control.addDisposeListener(e -> hideStateControls.remove(control));
	}
	
	public void setUsername(final String username)
	{
		this.username = username;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public void setPassword(final String password)
	{
		this.password = password;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public boolean isLoggedIn()
	{
		return loggedUser != null;
	}
	
	public User getLoggedUser()
	{
		return loggedUser;
	}
	
	public Properties getProperties()
	{
		return properties;
	}
	
	public String getBillSignatureProp()
	{
		return billSignatureProp;
	}
	
	public Optional<String> loggedUserSignature()
	{
		final String userId = Optional.ofNullable(getLoggedUser()).map(User::getId).map(String::valueOf).orElse(EMPTY_STRING);
		final String companyId = Optional.ofNullable(getLoggedUser()).map(User::getSelectedCompany)
				.map(Company::getId).map(String::valueOf).orElse(EMPTY_STRING);
		return PersistedProp.uuidFromProp(billSignatureProp, userId, companyId);
	}
	
	public String getBillStampProp()
	{
		return billStampProp;
	}
	
	public Optional<String> loggedUserStamp()
	{
		final String userId = Optional.ofNullable(getLoggedUser()).map(User::getId).map(String::valueOf).orElse(EMPTY_STRING);
		final String companyId = Optional.ofNullable(getLoggedUser()).map(User::getSelectedCompany)
				.map(Company::getId).map(String::valueOf).orElse(EMPTY_STRING);
		return PersistedProp.uuidFromProp(billStampProp, userId, companyId);
	}
	
	public ImmutableList<PersistedProp> allPersistedProps()
	{
		if (allPersistedProps == null)
			allPersistedProps = BusinessDelegate.allPersistedProps_NO_CACHE();
		
		return allPersistedProps;
	}
	
	public ClientSession resetPersistedPropsCache()
	{
		allPersistedProps = null;
		return this;
	}
}
