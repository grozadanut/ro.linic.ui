package ro.linic.ui.legacy.handlers;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.StringUtils.truncate;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.GenericDataHolder;
import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.parts.SupplierOrdersPart;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.security.services.AuthenticationSession;

public class EliminateFromOrdersHandler {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> orders, final DataServices dataServices,
			@Named(IServiceConstants.ACTIVE_SHELL) final Shell shell, final UISynchronize sync, final AuthenticationSession authSession) {
		if (!MessageDialog.openQuestion(shell,
				Messages.Delete, NLS.bind(Messages.DeleteConfirm, 
						truncate(orders.stream().map(gv -> gv.getString(Product.NAME_FIELD)).collect(Collectors.joining(LIST_SEPARATOR)), 2000))))
			return;
		
		final GenericDataHolder ordersHolder = dataServices.holder(SupplierOrdersPart.DATA_HOLDER);
		orders.forEach(gv -> {
			RestCaller.delete("/rest/s1/moqui-linic-legacy/requirements")
					.internal(authSession.authentication())
					.addUrlParam("requirementId", "*")
					.addUrlParam("facilityId", ClientSession.instance().getGestiune().getImportName())
					.addUrlParam("requirementTypeEnumId", "RqTpInventory")
					.addUrlParam("statusId", "RqmtStOrdered")
					.addUrlParam("productId", gv.getString(Product.ID_FIELD))
					.sync(GenericValue.class, t -> UIUtils.showException(t, sync))
					.ifPresent(result -> ordersHolder.getData().remove(gv));
		});
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) final List<GenericValue> orders) {
		return orders != null && !orders.isEmpty();
	}
}
