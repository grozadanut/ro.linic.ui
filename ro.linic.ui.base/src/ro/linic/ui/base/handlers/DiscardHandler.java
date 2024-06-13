package ro.linic.ui.base.handlers;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.osgi.util.NLS;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import ro.linic.ui.base.services.di.DiscardChanges;

public class DiscardHandler {
	@Inject private Logger logger;
	
	@CanExecute
	boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) final MDirtyable dirtyable) {
		return dirtyable == null ? false : dirtyable.isDirty();
	}

	@Execute
	void execute(@Named(IServiceConstants.ACTIVE_PART) final MPart part) {
		if (!part.isDirty())
			return;

		final Object client = part.getObject();
		try {
			ContextInjectionFactory.invoke(client, DiscardChanges.class, part.getContext());
		} catch (final InjectionException e) {
			log("Failed to discard contents of part", "Failed to discard contents of part ({0})", //$NON-NLS-1$ //$NON-NLS-2$
					part.getElementId(), e);
		} catch (final RuntimeException e) {
			log("Failed to discard contents of part via DI", //$NON-NLS-1$
					"Failed to discard contents of part ({0}) via DI", part.getElementId(), e); //$NON-NLS-1$
		}
	}
	
	private void log(final String unidentifiedMessage, final String identifiedMessage, final String id, final Exception e) {
		if (id == null || id.isEmpty()) {
			logger.error(e, unidentifiedMessage);
		} else {
			logger.error(e, NLS.bind(identifiedMessage, id));
		}
	}
}
