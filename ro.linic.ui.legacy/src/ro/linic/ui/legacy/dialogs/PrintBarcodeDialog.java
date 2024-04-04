package ro.linic.ui.legacy.dialogs;

import java.util.Optional;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.service.PeripheralService;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.service.components.BarcodePrintable.LabelType;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.PrintBarcodeNatTable;

public class PrintBarcodeDialog extends Dialog
{
	private Combo labelType;
	private Button setLabelType;
	private Combo gestiune;
	private PrintBarcodeNatTable table;
	private ImmutableList<BarcodePrintable> inputData;
	
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<LabelType> allLabelTypes = ImmutableList.copyOf(LabelType.values());
	
	private Logger log;
	private Bundle bundle;
	
	public PrintBarcodeDialog(final Shell parent, final ImmutableList<BarcodePrintable> inputData,
			final Logger log, final Bundle bundle)
	{
		super(parent);
		this.inputData = inputData;
		this.log = log;
		this.bundle = bundle;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		this.allGestiuni = BusinessDelegate.allGestiuni();
		
		final Composite contents = (Composite) super.createDialogArea(parent);
		getShell().setText(Messages.PrintBarcodeDialog_Title);
		
		final Composite upperContainer = new Composite(contents, SWT.NONE);
		upperContainer.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(upperContainer);
		
		new Label(upperContainer, SWT.NONE).setText(Messages.PrintBarcodeDialog_LabelType);
		labelType = new Combo(upperContainer, SWT.DROP_DOWN);
		labelType.setItems(allLabelTypes.stream().map(LabelType::displayName).toArray(String[]::new));
		UIUtils.setFont(labelType);
		GridDataFactory.swtDefaults().applyTo(labelType);
		
		setLabelType = new Button(upperContainer, SWT.PUSH);
		setLabelType.setText(Messages.PrintBarcodeDialog_ForAll);
		UIUtils.setFont(setLabelType);
		GridDataFactory.swtDefaults().applyTo(setLabelType);
		
		table = new PrintBarcodeNatTable();
		table.postConstruct(contents);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getTable());
		table.loadData(inputData);
		
		gestiune = new Combo(contents, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::displayName).toArray(String[]::new));
		gestiune.setEnabled(ClientSession.instance().hasPermission(Permissions.SUPERADMIN_ROLE));
		gestiune.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		UIUtils.setFont(gestiune);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.TOP).applyTo(gestiune);
		
		setLabelType.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final LabelType selLabelType = selectedLabelType().orElse(null);
				table.getSourceData().stream().forEach(bp -> bp.setLabelType(selLabelType));
				table.getTable().refresh(false);
			}
		});
		
		return contents;
	}
	
	@Override
	protected void okPressed()
	{
		PeripheralService.printPrintables(table.getFilteredSortedData(), System.getProperty(PeripheralService.BARCODE_PRINTER_KEY,
				PeripheralService.BARCODE_PRINTER_DEFAULT), log, bundle, true, selectedGestiune());
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.PrintBarcodeDialog_Print, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(860, 400);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
	
	private Optional<LabelType> selectedLabelType()
	{
		final int index = labelType.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allLabelTypes.get(index));
	}
}
