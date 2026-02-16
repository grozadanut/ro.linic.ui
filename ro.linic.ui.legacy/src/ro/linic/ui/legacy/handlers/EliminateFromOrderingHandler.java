package ro.linic.ui.legacy.handlers;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.StringUtils.truncate;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.parts.SupplierOrderPart;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.security.services.AuthenticationSession;

public class EliminateFromOrderingHandler {
	private static final ILog log = ILog.of(EliminateFromOrderingHandler.class);
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> products, final DataServices dataServices,
			@Named(IServiceConstants.ACTIVE_SHELL) final Shell shell, final EPartService partService, final AuthenticationSession authSession) {
		UIUtils.askSave(shell, partService);
		if (!MessageDialog.openQuestion(shell,
				Messages.Delete, NLS.bind(Messages.DeleteConfirm, 
						truncate(products.stream().map(gv -> gv.getString(Product.NAME_FIELD)).collect(Collectors.joining(LIST_SEPARATOR)), 2000))))
			return;
		
		products.forEach(gv -> {
			try {
				final InvocationResult result = BusinessDelegate.eliminateProductFromOrdering(gv.getInt(Product.ID_FIELD));
				ro.linic.ui.legacy.session.UIUtils.showResult(result);
			} catch (final Exception e) {
				// might throw NPE if the product was deleted from colibri_stat_db but not Moqui
				log.error(e.getMessage(), e);
			}
			
			// MOQUI
			final String pId = gv.getString(Product.ID_FIELD);
			RestCaller.delete("/rest/s1/mantle/products/"+pId)
					.internal(authSession.authentication())
					.sync(GenericValue.class, t -> UIUtils.showException(t, t.getMessage()));
		});
		
		dataServices.holder(SupplierOrderPart.DATA_HOLDER).remove(products);
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> products) {
		return products != null && !products.isEmpty();
	}
}
