package ro.linic.ui.pos.dialogs;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Inject;
import ro.linic.ui.base.services.ui.TitleAreaDialogValidated;
import ro.linic.ui.pos.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.ProductDataUpdater;
import ro.linic.ui.pos.widgets.ProductWidget;

public class CreateProductDialog extends TitleAreaDialogValidated {
	private Product model = new Product();
	@Inject private ProductDataUpdater productUpdater;
	@Inject private IEclipseContext ctx;
	
	public CreateProductDialog(final Shell parent) {
		super(parent);
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.CreateProductDialog_Title);
		setMessage(Messages.CreateProductDialog_Message);
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		model.setType(Product.MARFA_CATEGORY);
		final DataBindingContext bindCtx = new DataBindingContext();
		final ProductWidget widget = new ProductWidget(container, bindCtx, model);
		ContextInjectionFactory.inject(widget, ctx);
		GridDataFactory.fillDefaults().applyTo(widget);
		
		TitleAreaDialogSupport.create(this, bindCtx);
		return area;
	}
	
	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected void okPressed() {
		final IStatus status = productUpdater.create(model);
		if (!status.isOK()) {
			setErrorMessage(status.getMessage());
			return;
		}

		super.okPressed();
	}
}
