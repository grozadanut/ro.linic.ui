package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.addToImmutableList;
import static ro.colibri.util.NumberUtils.add;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.subtract;
import static ro.colibri.util.PresentationUtils.safeString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import ro.colibri.embeddable.Verificat;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.parts.VerifyOperationsPart;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class VerifyDialog extends Dialog
{
	private Operatiune op;
	private boolean isAutoTransfer;
	private ImmutableList<Operatiune> verifiedOps;

	/**
	 * The input validator, or <code>null</code> if none.
	 */
	private IInputValidator validator;

	private Text transferText;
	private Text text;

	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;

	/**
	 * Error message string.
	 */
	private String errorMessage;

	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog will
	 * have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 *
	 * @param parentShell   the parent shell, or <code>null</code> to create a
	 *                      top-level shell
	 * @param dialogTitle   the dialog title, or <code>null</code> if none
	 * @param dialogMessage the dialog message, or <code>null</code> if none
	 * @param initialValue  the initial input value, or <code>null</code> if none
	 *                      (equivalent to the empty string)
	 * @param validator     an input validator, or <code>null</code> if none
	 */
	public VerifyDialog(final Shell parentShell, final Operatiune op)
	{
		super(parentShell);
		this.op = Preconditions.checkNotNull(op);
		isAutoTransfer = VerifyOperationsPart.isAutoTransfer_TransferLine(op);
	}

	@Override
	protected void buttonPressed(final int buttonId)
	{
		if (buttonId == IDialogConstants.OK_ID)
		{
			InvocationResult result;
			
			if (isAutoTransfer)
				result = verifyAutoTransfer();
			else if (TipOp.IESIRE.equals(op.getTipOp()))
				result = verifyIesiri();
			else // INTRARI
				result = verifyIntrari();
			
			if (result.statusCanceled())
			{
				setErrorMessage(result.toTextDescription());
				return;
			}
		}
		super.buttonPressed(buttonId);
	}

	private InvocationResult verifyIntrari()
	{
		// these are INTRARI; normal verification applies
		final BigDecimal verificat = add(parse(text.getText()),
				Optional.ofNullable(op.getVerificat())
				.map(Verificat::getVerificatCantitate)
				.orElse(null));
		final InvocationResult result = BusinessDelegate.verifyOperation(op.getId(), verificat);
		if(result.statusOk())
			verifiedOps = addToImmutableList(verifiedOps, result.extra(InvocationResult.OPERATIONS_KEY));
		return result;
	}
	
	private InvocationResult verifyIesiri()
	{
		// this might have a connected auto transfer,
		// so we need to add that verified quantity as well
		final BigDecimal verificat = add(parse(text.getText()),
				Optional.ofNullable(op.getVerificat())
				.map(Verificat::getVerificatCantitate)
				.orElse(null),
				Optional.ofNullable(op.getChildOp()) // op.getChildOp() can be null
				.map(Operatiune::getVerificat)
				.map(Verificat::getVerificatCantitate)
				.orElse(null));
		final InvocationResult result = BusinessDelegate.verifyOperation(op.getId(), verificat);
		if(result.statusOk())
			verifiedOps = addToImmutableList(verifiedOps, result.extra(InvocationResult.OPERATIONS_KEY));
		return result;
	}
	
	private InvocationResult verifyAutoTransfer()
	{
		/*
		 * if this is an auto transfer then we have 2 fields:
		 * 1. transferat this is the loaded op
		 * 2. livrat this is op.getOwner()
		 */
		final BigDecimal transferat = add(parse(transferText.getText()),
				Optional.ofNullable(op.getVerificat())
				.map(Verificat::getVerificatCantitate)
				.orElse(null));
		final BigDecimal livrat = add(parse(text.getText()),
				Optional.ofNullable(op.getVerificat())
				.map(Verificat::getVerificatCantitate)
				.orElse(null),
				Optional.ofNullable(op.getOwnerOp())
				.map(Operatiune::getVerificat)
				.map(Verificat::getVerificatCantitate)
				.orElse(null));
		
		// we need to negate here, because this is other gest,
		// but the value was negated beforehand for display purposes
		final InvocationResult transferResult = BusinessDelegate.verifyOperation(op.getId(), transferat.negate());
		
		if (transferResult.statusCanceled())
			return transferResult;
		
		verifiedOps = addToImmutableList(verifiedOps, transferResult.extra(InvocationResult.OPERATIONS_KEY));
		final InvocationResult result = BusinessDelegate.verifyOperation(op.getOwnerOp().getId(), livrat);
		if (result.statusOk())
			verifiedOps = addToImmutableList(verifiedOps, result.extra(InvocationResult.OPERATIONS_KEY));
		return result;
	}

	@Override
	protected void configureShell(final Shell shell)
	{
		super.configureShell(shell);
		shell.setText("Verifica");
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		// do this here because setting the text will set enablement on the ok
		// button
		text.setFocus();
		final BigDecimal ramas = subtract(op.getCantitate(),
				Optional.ofNullable(op.getVerificat())
				.map(Verificat::getVerificatCantitate)
				.orElse(null));
		transferText.setText(safeString(ramas, v -> v.setScale(2, RoundingMode.HALF_EVEN), BigDecimal::toString));
		text.setText(safeString(ramas, v -> v.setScale(2, RoundingMode.HALF_EVEN), BigDecimal::toString));
		text.selectAll();
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		// create composite
		final Composite composite = (Composite) super.createDialogArea(parent);
		// create message
		final Label label = new Label(composite, SWT.WRAP);
		label.setText(op.verifyNicename());
		final GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
				| GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		label.setFont(parent.getFont());
		
		final Composite textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().applyTo(textContainer);
		
		final Label transferLabel = new Label(textContainer, SWT.NONE);
		transferLabel.setText("Transferat");
		final GridData transferLabelGD = new GridData();
		transferLabelGD.exclude = !isAutoTransfer;
		transferLabel.setLayoutData(transferLabelGD);
		
		transferText = new Text(textContainer, getInputTextStyle());
		transferText.addModifyListener(e -> validateInput());
		final GridData transferTextGD = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		transferTextGD.exclude = !isAutoTransfer;
		transferText.setLayoutData(transferTextGD);
		
		final Label textLabel = new Label(textContainer, SWT.NONE);
		textLabel.setText("Livrat");
		
		text = new Text(textContainer, getInputTextStyle());
		text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		text.addModifyListener(e -> transferText.setText(text.getText())); // this also triggers validation
		
		errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		// Set the error message text
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=66292
		setErrorMessage(errorMessage);

		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Validates the input.
	 * <p>
	 * The default implementation of this framework method delegates the request to
	 * the supplied input validator object; if it finds the input invalid, the error
	 * message is displayed in the dialog's message line. This hook method is called
	 * whenever the text changes in the input field.
	 * </p>
	 */
	protected void validateInput()
	{
		String errorMessage = null;
		if (validator != null)
			errorMessage = validator.isValid(text.getText());
		// Bug 16256: important not to treat "" (blank error) the same as null
		// (no error)
		setErrorMessage(errorMessage);
	}

	/**
	 * Sets or clears the error message. If not <code>null</code>, the OK button is
	 * disabled.
	 *
	 * @param errorMessage the error message, or <code>null</code> to clear
	 * @since 3.0
	 */
	public void setErrorMessage(final String errorMessage)
	{
		this.errorMessage = errorMessage;
		if (errorMessageText != null && !errorMessageText.isDisposed())
		{
			errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); //$NON-NLS-1$
			// Disable the error message text control if there is no error, or
			// no error text (empty or whitespace only). Hide it also to avoid
			// color change.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
			final boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
			errorMessageText.setEnabled(hasError);
			errorMessageText.setVisible(hasError);
			errorMessageText.getParent().update();
			// Access the ok button by id, in case clients have overridden button creation.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
			final Control button = getButton(IDialogConstants.OK_ID);
			if (button != null)
				button.setEnabled(errorMessage == null);
		}
	}

	/**
	 * Returns the style bits that should be used for the input text field. Defaults
	 * to a single line entry. Subclasses may override.
	 *
	 * @return the integer style bits that should be used when creating the input
	 *         text
	 *
	 * @since 3.4
	 */
	protected int getInputTextStyle()
	{
		return SWT.SINGLE | SWT.BORDER;
	}
	
	/**
	 * Can be called after open() returns.
	 * 
	 * @return
	 */
	public ImmutableList<Operatiune> getVerifiedOps()
	{
		return verifiedOps;
	}
}