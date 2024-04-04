package ro.linic.ui.base.services.impl;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.service.component.annotations.Component;

import ro.linic.ui.base.services.LocaleService;

@Component(service = IContextFunction.class, property = "service.context.key=ro.linic.ui.base.services.LocaleService")
public class LocaleServiceContextFunction extends ContextFunction {

	@Override
	public Object compute(final IEclipseContext context, final String contextKey) {
		final LocaleService lcService = ContextInjectionFactory.make(LocaleServiceImpl.class, context);
		context.set(LocaleService.class, lcService);
		return lcService;
	}
}
