package ro.linic.ui.legacy.session;

import static ro.colibri.util.ServerConstants.EAR_VERSION;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import ro.colibri.beans.RemoteService;

public class ServiceLocator
{
	private static final Map<String, RemoteService> servicesCache;

	// The app name is the application name of the deployed EJBs. This is typically
	// the ear name
	// without the .ear suffix. However, the application name could be overridden in
	// the application.xml of the
	// EJB deployment on the server.
	// Since we haven't deployed the application as a .ear, the app name for us will
	// be an empty string
	private static final String appName = "colibri_stat_ear-"+EAR_VERSION;

	// This is the module name of the deployed EJBs on the server. This is typically
	// the jar name of the
	// EJB deployment, without the .jar suffix, but can be overridden via the
	// ejb-jar.xml
	// In this example, we have deployed the EJBs in a jboss-as-ejb-remote-app.jar,
	// so the module name is
	// jboss-as-ejb-remote-app
	private static final String moduleName = "colibri_stat_ejb";

	static
	{
		servicesCache = new HashMap<>();
	}

	public static String jndiNameStateless(final Class beanClass, final Class remoteInterface)
	{
		// The EJB name which by default is the simple class name of the bean
		// implementation class
		final String beanName = beanClass.getSimpleName();
		// the remote view fully qualified class name
		final String viewClassName = remoteInterface.getName();
		// ejb:<app-name>/<module-name>/<bean-name>!<fully-qualified-classname-of-the-remote-interface>
		return "ejb:" + appName + "/" + moduleName + "/" + beanName + "!" + viewClassName;
	}

	public static <C extends RemoteService> C getBusinessService(final Class beanClass, final Class remoteInterface)
	{
		final String jndiName = jndiNameStateless(beanClass, remoteInterface);
		final C businessService = (C) servicesCache.get(jndiName);

		if (businessService != null)
			return businessService;

		try
		{
			final Properties jndiProps = new Properties();
			jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, ClientSession.INITIAL_CONTEXT_FACTORY_VALUE);
			jndiProps.put(Context.PROVIDER_URL, ClientSession.PROVIDER_URL_VALUE);
			jndiProps.put(Context.SECURITY_PRINCIPAL, ClientSession.instance().getUsername());
			jndiProps.put(Context.SECURITY_CREDENTIALS, ClientSession.instance().getPassword());
			final InitialContext context = new InitialContext(jndiProps);
			final C businessServiceLoaded = (C) context.lookup(jndiName);
			servicesCache.put(jndiName, businessServiceLoaded);
			context.close();
			return businessServiceLoaded;
		}
		catch (final NamingException e)
		{
			throw new UnsupportedOperationException("ServiceLocator - NamingException "+e.getMessage());
		}
	}
	
	public static void clearCache()
	{
		servicesCache.clear();
	}
}
