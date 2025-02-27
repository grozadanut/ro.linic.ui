package ro.linic.ui.pos.handlers;

import static ro.flexbiz.util.commons.PresentationUtils.LIST_SEPARATOR;
import static ro.flexbiz.util.commons.StringUtils.truncate;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;
import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.ProductDataUpdater;

public class DeleteProductsHandler {
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) final List<Product> products, 
			final ProductDataUpdater updater, @Named(IServiceConstants.ACTIVE_SHELL) final Shell shell,
			final EPartService partService) {
		if (!MessageDialog.openQuestion(shell, Messages.Delete, NLS.bind(Messages.DeleteConfirm, 
						truncate(products.stream().map(Product::getName).collect(Collectors.joining(LIST_SEPARATOR)), 2000))))
			return;
		
		UIUtils.askSave(shell, partService);
		UIUtils.showResult(updater.delete(products.stream()
				.map(Product::getId)
				.collect(Collectors.toList())));
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) final List<Product> products) {
		return products != null && !products.isEmpty();
	}
}
