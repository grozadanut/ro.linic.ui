package ro.linic.ui.legacy.dialogs.components;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.legacy.dialogs.Messages;
import ro.linic.ui.legacy.session.UIUtils;

public class AnafInvoiceLineTray extends DialogTray {
	private static final int WIDGET_WIDTH = 200;
	
	private final GenericValue anafInvoiceLine;
	private final Consumer<SelectionEvent> disconnected;
	private final Consumer<SelectionEvent> markReceived;
	
	private Text code;
	private Text quantity;
	private Text name;
	private Text uom;
	private Text puaFaraTVA;
	private Text valAchFaraTVA;
	private Text valAchTVA;
	private Button disconnectLine;
	private Button ignoreLine;

	
	public AnafInvoiceLineTray(final GenericValue anafInvoiceLine, final Consumer<SelectionEvent> disconnected,
			final Consumer<SelectionEvent> markReceived) {
		this.anafInvoiceLine = Objects.requireNonNull(anafInvoiceLine);
		this.disconnected = disconnected;
		this.markReceived = markReceived;
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, true));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label codeLabel = new Label(container, SWT.NONE);
		codeLabel.setText(Messages.AdaugaProductDialog_Code);
		GridDataFactory.swtDefaults().span(3, 1).applyTo(codeLabel);
		
		code = new Text(container, SWT.SINGLE | SWT.BORDER);
		code.setEditable(false);
		UIUtils.setFont(code);
		GridDataFactory.fillDefaults().span(3, 1).hint(WIDGET_WIDTH, SWT.DEFAULT).applyTo(code);
		
		final Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText(Messages.AdaugaProductDialog_Name);
		GridDataFactory.swtDefaults().span(3, 1).applyTo(nameLabel);
		
		name = new Text(container, SWT.SINGLE | SWT.BORDER);
		name.setEditable(false);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().span(3, 1).hint(WIDGET_WIDTH, SWT.DEFAULT).applyTo(name);
		
		final Label quantityLabel = new Label(container, SWT.NONE);
		quantityLabel.setText(Messages.AdaugaOpDialog_Quantity);
		GridDataFactory.swtDefaults().applyTo(quantityLabel);
		
		final Label uomLabel = new Label(container, SWT.NONE);
		uomLabel.setText(Messages.UOM);
		GridDataFactory.swtDefaults().applyTo(uomLabel);
		
		new Label(container, SWT.NONE); // layout purpose

		quantity = new Text(container, SWT.SINGLE | SWT.BORDER);
		quantity.setEditable(false);
		UIUtils.setFont(quantity);
		GridDataFactory.swtDefaults().hint(WIDGET_WIDTH/2, SWT.DEFAULT).applyTo(quantity);
		
		uom = new Text(container, SWT.SINGLE | SWT.BORDER);
		uom.setEditable(false);
		UIUtils.setFont(uom);
		GridDataFactory.swtDefaults().hint(WIDGET_WIDTH/2, SWT.DEFAULT).applyTo(uom);
		
		new Label(container, SWT.NONE);  // layout purpose
		
		final Label puaFaraTVALabel = new Label(container, SWT.NONE);
		puaFaraTVALabel.setText(Messages.AdaugaOpDialog_PUAfTVA);
		GridDataFactory.swtDefaults().applyTo(puaFaraTVALabel);
		
		final Label valAchFaraTVALabel = new Label(container, SWT.NONE);
		valAchFaraTVALabel.setText(Messages.AdaugaOpDialog_VAfTVA);
		GridDataFactory.swtDefaults().applyTo(valAchFaraTVALabel);
		
		final Label valAchTVALabel = new Label(container, SWT.NONE);
		valAchTVALabel.setText(Messages.AdaugaOpDialog_VATVA);
		GridDataFactory.swtDefaults().applyTo(valAchTVALabel);
		
		puaFaraTVA = new Text(container, SWT.SINGLE | SWT.BORDER);
		puaFaraTVA.setEditable(false);
		UIUtils.setFont(puaFaraTVA);
		GridDataFactory.fillDefaults().hint(WIDGET_WIDTH, SWT.DEFAULT).applyTo(puaFaraTVA);
		
		valAchFaraTVA = new Text(container, SWT.SINGLE | SWT.BORDER);
		valAchFaraTVA.setEditable(false);
		UIUtils.setFont(valAchFaraTVA);
		GridDataFactory.fillDefaults().hint(WIDGET_WIDTH, SWT.DEFAULT).applyTo(valAchFaraTVA);
		
		valAchTVA = new Text(container, SWT.SINGLE | SWT.BORDER);
		valAchTVA.setEditable(false);
		UIUtils.setFont(valAchTVA);
		GridDataFactory.fillDefaults().hint(WIDGET_WIDTH, SWT.DEFAULT).applyTo(valAchTVA);
		
		disconnectLine = new Button(container, SWT.PUSH);
		disconnectLine.setText(Messages.AnafInvoiceLineTray_Disconnect);
		disconnectLine.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		disconnectLine.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(disconnectLine);
		UIUtils.setBoldBannerFont(disconnectLine);
		
		ignoreLine = new Button(container, SWT.PUSH);
		ignoreLine.setText(Messages.AnafInvoiceLineTray_IgnoreLine);
		ignoreLine.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		ignoreLine.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().applyTo(ignoreLine);
		UIUtils.setBoldBannerFont(ignoreLine);
		
		container.setTabList(new Control[] {});
		fillFields();
		addListeners();
		return container;
	}

	private void fillFields() {
		code.setText(PresentationUtils.safeString(anafInvoiceLine.getString("itemId")));
		quantity.setText(PresentationUtils.safeString(anafInvoiceLine.getString("quantity")));
		name.setText(PresentationUtils.safeString(anafInvoiceLine.getString("name")));
		uom.setText(PresentationUtils.safeString(anafInvoiceLine.getString("uom")));
		puaFaraTVA.setText(PresentationUtils.safeString(anafInvoiceLine.getString("price")));
		valAchFaraTVA.setText(PresentationUtils.safeString(anafInvoiceLine.getString("total")));
	}
	
	private void addListeners() {
		if (disconnected != null)
			disconnectLine.addSelectionListener(SelectionListener.widgetSelectedAdapter(this.disconnected));
		if (markReceived != null)
			ignoreLine.addSelectionListener(SelectionListener.widgetSelectedAdapter(this.markReceived));
	}
}
