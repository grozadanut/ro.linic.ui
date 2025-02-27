package ro.linic.ui.pos.parts;

import static ro.flexbiz.util.commons.NumberUtils.parseToInt;
import static ro.flexbiz.util.commons.StringUtils.notEmpty;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;

import jakarta.annotation.PostConstruct;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.ReceiptUIComponent;
import ro.linic.ui.pos.ui.DefaultReceiptUIComponent;

public class ReceiptPart {
	private static ILog log = ILog.get();
	public static final String PART_DESCRIPTOR_ID = "ro.linic.ui.pos.partdescriptor.receipt"; //$NON-NLS-1$
	
	private static final String UI_EXTENSION_POINT_ID = "ro.linic.ui.pos.base.receiptUIComponent"; // $NON-NLS-1$
	private static final String ATTR_CLASS = "class"; // $NON-NLS-1$
	private static final String ATTR_PRIORITY = "priority"; // $NON-NLS-1$
	
	private ReceiptUIComponent ui;
	
	@PostConstruct
	public void postConstruct(final Composite parent, final IEclipseContext ctx) {
		ui = createUI(ctx);
		ui.postConstruct(parent);
	}
	
	private ReceiptUIComponent createUI(final IEclipseContext ctx) {
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		return Arrays.stream(registry.getConfigurationElementsFor(UI_EXTENSION_POINT_ID))
				.filter(elmt -> notEmpty(elmt.getAttribute(ATTR_CLASS)))
				.sorted(Comparator.<IConfigurationElement, Integer>comparing(elmt -> parseToInt(elmt.getAttribute(ATTR_PRIORITY))).reversed())
				.findFirst()
				.map(elmt -> {
					try {
						final String bundleId = elmt.getNamespaceIdentifier();
						final String uiClass = elmt.getAttribute(ATTR_CLASS);
						final Bundle b = Platform.getBundle(bundleId);
						return (ReceiptUIComponent) ContextInjectionFactory.make(b.loadClass(uiClass), ctx);
					} catch (final ClassNotFoundException | IllegalArgumentException | SecurityException e) {
						log.error(e.getMessage(), e);
						return null;
					}
				})
				.orElseGet(() -> ContextInjectionFactory.make(DefaultReceiptUIComponent.class, ctx));
	}

	public boolean canCloseReceipt() {
		return ui.canCloseReceipt();
	}

	public void closeReceipt(final PaymentType paymentType) {
		ui.closeReceipt(paymentType);
	}
	
	@PersistState
	public void persistState() {
		ui.persistState();
	}
	
	@Focus
	public void setFocus() {
		ui.setFocus();
	}
}
