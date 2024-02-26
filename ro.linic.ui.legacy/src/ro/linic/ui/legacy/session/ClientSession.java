package ro.linic.ui.legacy.session;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.naming.Context;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.parts.VanzareBarPart;
import ro.linic.ui.legacy.service.SQLiteJDBC;

public class ClientSession
{
	private static ClientSession instance;
	
	public static final String INITIAL_CONTEXT_FACTORY_VALUE = "org.wildfly.naming.client.WildFlyInitialContextFactory";
	public static final String PROVIDER_URL_VALUE = System.getProperty(Context.PROVIDER_URL, "http-remoting://localhost:8080");
	
	private String username;
	private String password;
	private User loggedUser;
	private boolean offlineMode = false;
	private String billSignatureProp;
	private String billStampProp;
	
	private Properties properties = new Properties();
	private List<Control> hideStateControls = new ArrayList<>();
	private List<CLabel> workingModeControls = new ArrayList<>();
	
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
	
	public void addWorkingModeControl(final CLabel control)
	{
		this.workingModeControls.add(control);
		control.addDisposeListener(e -> workingModeControls.remove(control));
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
	
	public boolean isOfflineMode()
	{
		return offlineMode;
	}
	
	public void setOfflineMode(final boolean offlineMode, final EPartService partService, final Bundle bundle, final Logger log)
	{
		if (!this.offlineMode && offlineMode && !allBonuriClosed(partService))
		{
			if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Inchideti bonurile deschise?",
					"ATENTIE! Aveti bonuri incepute care vor fi inchise si vor aparea din nou doar dupa ce reporniti"
							+ " programul. Continuati?"))
			{
				partService.getParts().stream()
				.filter(p -> p.getElementId().equals(VanzareBarPart.PART_ID))
				.map(MPart::getObject)
				.filter(Objects::nonNull)
				.filter(VanzareBarPart.class::isInstance)
				.map(VanzareBarPart.class::cast)
				.filter(VanzareBarPart::bonTableNotEmpty)
				.forEach(vanzareBonDeschis -> vanzareBonDeschis.updateBonCasa(null, true));

				partService.getParts().stream()
				.filter(p -> p.getElementId().equals(VanzareBarPart.PART_DESCRIPTOR_ID))
				.map(MPart::getObject)
				.filter(Objects::nonNull)
				.filter(VanzareBarPart.class::isInstance)
				.map(VanzareBarPart.class::cast)
				.filter(VanzareBarPart::bonTableNotEmpty)
				.forEach(vanzareBonDeschis -> vanzareBonDeschis.updateBonCasa(null, true));
			}
			else
				return;
		}
		
		if (this.offlineMode && !offlineMode)
		{
			if (!allBonuriClosed(partService))
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Bonuri deschise!",
						"Inchideti bonurile inainte de a comuta intre modul OFFLINE - ONLINE!");
				return;
			}
			
			final InvocationResult result = SQLiteJDBC.instance(bundle, log).saveLocalToServer();
			showResult(result);
			if (result.statusCanceled())
				return;
		}
		
		this.offlineMode = offlineMode;
		
		final Color workingModeBgColor = offlineMode ?
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED) :
				Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
				
		workingModeControls.forEach(control -> 
		{
			control.setText(offlineMode ? "Offline" : "Online");
			control.setBackground(workingModeBgColor);
		});
	}
	
	private boolean allBonuriClosed(final EPartService partService)
	{
		if (partService != null)
		{
			if (partService.getParts().stream()
					.filter(p -> p.getElementId().equals(VanzareBarPart.PART_ID))
					.map(MPart::getObject)
					.filter(Objects::nonNull)
					.filter(VanzareBarPart.class::isInstance)
					.map(VanzareBarPart.class::cast)
					.filter(VanzareBarPart::bonTableNotEmpty)
					.findAny()
					.isPresent() || partService.getParts().stream()
						.filter(p -> p.getElementId().equals(VanzareBarPart.PART_DESCRIPTOR_ID))
						.map(MPart::getObject)
						.filter(Objects::nonNull)
						.filter(VanzareBarPart.class::isInstance)
						.map(VanzareBarPart.class::cast)
						.filter(VanzareBarPart::bonTableNotEmpty)
						.findAny()
						.isPresent())
			{
				return false;
			}
		}
		return true;
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
