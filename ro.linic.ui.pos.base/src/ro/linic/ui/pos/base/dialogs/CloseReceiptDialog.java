package ro.linic.ui.pos.base.dialogs;

import static ro.linic.util.commons.NumberUtils.add;
import static ro.linic.util.commons.NumberUtils.equal;
import static ro.linic.util.commons.NumberUtils.greaterThan;
import static ro.linic.util.commons.NumberUtils.parse;
import static ro.linic.util.commons.NumberUtils.smallerThan;
import static ro.linic.util.commons.NumberUtils.subtract;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jakarta.inject.Inject;
import ro.linic.ui.base.services.binding.BigDecimalToStringConverter;
import ro.linic.ui.base.services.ui.TitleAreaDialogValidated;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.services.ECRService;
import ro.linic.ui.pos.base.services.PosResources;
import ro.linic.ui.pos.base.services.ReceiptUpdater;

public class CloseReceiptDialog extends TitleAreaDialogValidated {
	private static final ILog log = ILog.of(CloseReceiptDialog.class);
	
	public static final int BUTTON_HEIGHT = 200;
	public static final int READ_ONLY_TEXT_WIDTH = 120;
	public static final int EDITABLE_TEXT_WIDTH = 100;
	
	private final Receipt receipt;
	private final DataBindingContext bindCtx;
	
	private Text taxCodeText;
	private Text receiptTotalText;
	
	private Text cashText;
	private boolean cashTextVisible = true;
	private Text cardText;
	private boolean cardTextVisible = true;
	private Text creditText;
	private boolean creditTextVisible = true;
	private Text mealTicketText;
	private boolean mealTicketTextVisible = true;
	private Text valueTicketText;
	private boolean valueTicketTextVisible = true;
	private Text voucherText;
	private boolean voucherTextVisible = true;
	private Text modernPaymentText;
	private boolean modernPaymentTextVisible = true;
	private Text otherText;
	private boolean otherTextVisible = true;
	private Map<PaymentType, BigDecimal> initialValues = Map.of();
	
	private Text outstandingTotalText;
	
	private Button closeReceiptButton;
	private Button cancelButton;
	
	@Inject private ReceiptUpdater receiptUpdater;
	@Inject private ECRService ecrService;
	
	private CloseReceiptDialog(final Shell parent, final Receipt receipt) {
		super(parent);
		this.receipt = Objects.requireNonNull(receipt);
		bindCtx = new DataBindingContext();
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.CloseReceipt);
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		GridLayoutFactory.createFrom(new GridLayout(3, false)).extendedMargins(0, 0, 0, 5).applyTo(container);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Composite textContainer = new Composite(container, SWT.NONE);
		GridLayoutFactory.createFrom(new GridLayout(2, false)).spacing(10, 5).applyTo(textContainer);

		UIUtils.setBoldFont(new Label(textContainer, SWT.NONE)).setText(Messages.CloseReceiptDialog_TaxCode);
		taxCodeText = new Text(textContainer, SWT.BORDER);
		UIUtils.setBoldFont(taxCodeText);
		GridDataFactory.swtDefaults().hint(READ_ONLY_TEXT_WIDTH, SWT.DEFAULT).applyTo(taxCodeText);
		
		UIUtils.setBoldFont(new Label(textContainer, SWT.NONE)).setText(Messages.CloseReceiptDialog_ReceiptTotal);
		receiptTotalText = new Text(textContainer, SWT.BORDER | SWT.READ_ONLY);
		receiptTotalText.setEditable(false);
		receiptTotalText.setText(receipt.total().toString());
		UIUtils.setFont(receiptTotalText);
		GridDataFactory.swtDefaults().hint(READ_ONLY_TEXT_WIDTH, SWT.DEFAULT).applyTo(receiptTotalText);
		
		final Label cashLabel = new Label(textContainer, SWT.NONE);
		cashLabel.setText(PosResources.getString(PaymentType.CASH.toString()));
		UIUtils.setFont(cashLabel);
		GridDataFactory.swtDefaults().exclude(!cashTextVisible).applyTo(cashLabel);
		cashText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(cashText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!cashTextVisible).applyTo(cashText);
		
		final Label cardLabel = new Label(textContainer, SWT.NONE);
		cardLabel.setText(PosResources.getString(PaymentType.CARD.toString()));
		UIUtils.setFont(cardLabel);
		GridDataFactory.swtDefaults().exclude(!cardTextVisible).applyTo(cardLabel);
		cardText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(cardText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!cardTextVisible).applyTo(cardText);
		
		final Label creditLabel = new Label(textContainer, SWT.NONE);
		creditLabel.setText(PosResources.getString(PaymentType.CREDIT.toString()));
		UIUtils.setFont(creditLabel);
		GridDataFactory.swtDefaults().exclude(!creditTextVisible).applyTo(creditLabel);
		creditText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(creditText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!creditTextVisible).applyTo(creditText);
		
		final Label mealTicketLabel = new Label(textContainer, SWT.NONE);
		mealTicketLabel.setText(PosResources.getString(PaymentType.MEAL_TICKET.toString()));
		UIUtils.setFont(mealTicketLabel);
		GridDataFactory.swtDefaults().exclude(!mealTicketTextVisible).applyTo(mealTicketLabel);
		mealTicketText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(mealTicketText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!mealTicketTextVisible).applyTo(mealTicketText);
		
		final Label valueTicketLabel = new Label(textContainer, SWT.NONE);
		valueTicketLabel.setText(PosResources.getString(PaymentType.VALUE_TICKET.toString()));
		UIUtils.setFont(valueTicketLabel);
		GridDataFactory.swtDefaults().exclude(!valueTicketTextVisible).applyTo(valueTicketLabel);
		valueTicketText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(valueTicketText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!valueTicketTextVisible).applyTo(valueTicketText);
		
		final Label voucherLabel = new Label(textContainer, SWT.NONE);
		voucherLabel.setText(PosResources.getString(PaymentType.VOUCHER.toString()));
		UIUtils.setFont(voucherLabel);
		GridDataFactory.swtDefaults().exclude(!voucherTextVisible).applyTo(voucherLabel);
		voucherText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(voucherText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!voucherTextVisible).applyTo(voucherText);
		
		final Label modernPaymentLabel = new Label(textContainer, SWT.NONE);
		modernPaymentLabel.setText(PosResources.getString(PaymentType.MODERN_PAYMENT.toString()));
		UIUtils.setFont(modernPaymentLabel);
		GridDataFactory.swtDefaults().exclude(!modernPaymentTextVisible).applyTo(modernPaymentLabel);
		modernPaymentText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(modernPaymentText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!modernPaymentTextVisible).applyTo(modernPaymentText);
		
		final Label otherLabel = new Label(textContainer, SWT.NONE);
		otherLabel.setText(PosResources.getString(PaymentType.OTHER.toString()));
		UIUtils.setFont(otherLabel);
		GridDataFactory.swtDefaults().exclude(!otherTextVisible).applyTo(otherLabel);
		otherText = new Text(textContainer, SWT.BORDER);
		UIUtils.setFont(otherText);
		GridDataFactory.swtDefaults().hint(EDITABLE_TEXT_WIDTH, SWT.DEFAULT).exclude(!otherTextVisible).applyTo(otherText);
		
		UIUtils.setBoldFont(new Label(textContainer, SWT.NONE)).setText(Messages.CloseReceiptDialog_OutstandingTotal);
		outstandingTotalText = new Text(textContainer, SWT.BORDER | SWT.READ_ONLY);
		outstandingTotalText.setEditable(false);
		UIUtils.setFont(outstandingTotalText);
		GridDataFactory.swtDefaults().hint(READ_ONLY_TEXT_WIDTH, SWT.DEFAULT).applyTo(outstandingTotalText);
		
		closeReceiptButton = createButton(container, IDialogConstants.OK_ID, Messages.CloseReceiptDialog_CloseButton, true);
		closeReceiptButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		closeReceiptButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(closeReceiptButton);
		GridDataFactory.swtDefaults().hint(SWT.DEFAULT, BUTTON_HEIGHT).grab(true, false).applyTo(closeReceiptButton);
		
		cancelButton = createButton(container, IDialogConstants.CANCEL_ID, Messages.Cancel, false);
		cancelButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		cancelButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(cancelButton);
		UIUtils.setBannerFont(cancelButton);
		GridDataFactory.swtDefaults().hint(SWT.DEFAULT, BUTTON_HEIGHT).applyTo(cancelButton);
		
		addListeners();
		bindValues();
		TitleAreaDialogSupport.create(this, bindCtx);
		final IStatus initialStatus = new AggregateValidationStatus(bindCtx.getBindings(), AggregateValidationStatus.MAX_SEVERITY)
				.getValue();
		if (!initialStatus.isOK())
			setErrorMessage(initialStatus.getMessage());
		return area;
	}
	
	private void addListeners() {
		final KeyListener keyCloseListener = new KeyAdapter() {
			@Override public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.F3 && closeReceiptButton.isEnabled())
					okPressed();
			}
		};
		
		taxCodeText.addKeyListener(keyCloseListener);
		receiptTotalText.addKeyListener(keyCloseListener);
		cashText.addKeyListener(keyCloseListener);
		cardText.addKeyListener(keyCloseListener);
		creditText.addKeyListener(keyCloseListener);
		mealTicketText.addKeyListener(keyCloseListener);
		valueTicketText.addKeyListener(keyCloseListener);
		voucherText.addKeyListener(keyCloseListener);
		modernPaymentText.addKeyListener(keyCloseListener);
		otherText.addKeyListener(keyCloseListener);
		outstandingTotalText.addKeyListener(keyCloseListener);
	}

	private void bindValues() {
		final IObservableValue<String> cashWidget = WidgetProperties.text(SWT.Modify).observe(cashText);
		final IObservableValue<String> cardWidget = WidgetProperties.text(SWT.Modify).observe(cardText);
		final IObservableValue<String> creditWidget = WidgetProperties.text(SWT.Modify).observe(creditText);
		final IObservableValue<String> mealTicketWidget = WidgetProperties.text(SWT.Modify).observe(mealTicketText);
		final IObservableValue<String> valueTicketWidget = WidgetProperties.text(SWT.Modify).observe(valueTicketText);
		final IObservableValue<String> voucherWidget = WidgetProperties.text(SWT.Modify).observe(voucherText);
		final IObservableValue<String> modernPaymentWidget = WidgetProperties.text(SWT.Modify).observe(modernPaymentText);
		final IObservableValue<String> otherWidget = WidgetProperties.text(SWT.Modify).observe(otherText);
		
		final IObservableValue<String> outstandingTotalWidget = WidgetProperties.text(SWT.Modify).observe(outstandingTotalText);
		final IObservableValue<BigDecimal> outstandingTotalModel = ComputedValue.create(() -> {
		    // since cashWidget.getValue() are tracked getter (see JavaDoc of
		    // getValue() method) the value of this ComputedValue gets recomputed once
		    // values are changed
		    final BigDecimal cashValue = parse(cashWidget.getValue());
		    final BigDecimal cardValue = parse(cardWidget.getValue());
		    final BigDecimal creditValue = parse(creditWidget.getValue());
		    final BigDecimal mealTicketValue = parse(mealTicketWidget.getValue());
		    final BigDecimal valueTicketValue = parse(valueTicketWidget.getValue());
		    final BigDecimal voucherValue = parse(voucherWidget.getValue());
		    final BigDecimal modernPaymentValue = parse(modernPaymentWidget.getValue());
		    final BigDecimal otherValue = parse(otherWidget.getValue());
		    final BigDecimal totalPaid = add(cashValue, cardValue, creditValue, mealTicketValue, valueTicketValue, voucherValue,
		    		modernPaymentValue, otherValue);

		    final BigDecimal outstTotal = subtract(receipt.total(), totalPaid);
		    outstandingTotalText.setText(outstTotal.toString());
		    return outstTotal;
		});
		final Binding outstandingTotalBind = bindCtx.bindValue(outstandingTotalWidget, outstandingTotalModel, UpdateValueStrategy.never(),
				UpdateValueStrategy.create(new BigDecimalToStringConverter())
				.setAfterGetValidator(outst -> equal(outst, BigDecimal.ZERO) ? ValidationStatus.ok() : 
					ValidationStatus.error(Messages.CloseReceiptDialog_OutstandingTotalNotZero)));
		ControlDecorationSupport.create(outstandingTotalBind, SWT.TOP | SWT.LEFT);

		final MultiValidator validator = new MultiValidator() {
			@Override
			protected IStatus validate() {
				final BigDecimal cashValue = parse(cashWidget.getValue());
				final BigDecimal cardValue = parse(cardWidget.getValue());
				final BigDecimal creditValue = parse(creditWidget.getValue());
				final BigDecimal mealTicketValue = parse(mealTicketWidget.getValue());
				final BigDecimal valueTicketValue = parse(valueTicketWidget.getValue());
				final BigDecimal voucherValue = parse(voucherWidget.getValue());
				final BigDecimal modernPaymentValue = parse(modernPaymentWidget.getValue());
				final BigDecimal otherValue = parse(otherWidget.getValue());

				if (smallerThan(cashValue, BigDecimal.ZERO) || smallerThan(cardValue, BigDecimal.ZERO) || smallerThan(creditValue, BigDecimal.ZERO) ||
						smallerThan(mealTicketValue, BigDecimal.ZERO) || smallerThan(valueTicketValue, BigDecimal.ZERO) || smallerThan(voucherValue, BigDecimal.ZERO) ||
						smallerThan(modernPaymentValue, BigDecimal.ZERO) || smallerThan(otherValue, BigDecimal.ZERO))
					return ValidationStatus.error(Messages.CloseReceiptDialog_NegativeNotAllowed);
				return ValidationStatus.ok();
			}
		};
		bindCtx.addValidationStatusProvider(validator);
		
		// set initial values
		initialValues.keySet().forEach(paymentType -> {
			final Text text = switch (paymentType) {
			case CASH -> cashText;
			case CARD -> cardText;
			case CREDIT -> creditText;
			case MEAL_TICKET -> mealTicketText;
			case VALUE_TICKET -> valueTicketText;
			case VOUCHER -> voucherText;
			case MODERN_PAYMENT -> modernPaymentText;
			case OTHER -> otherText;
			default -> throw new IllegalArgumentException("Unexpected value: " + paymentType);
			};
			text.setText(initialValues.getOrDefault(paymentType, BigDecimal.ZERO).toString());
		});
	}
	
	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
	}
	
	@Override
	protected void okPressed() {
		final IStatus result = receiptUpdater.closeReceipt(receipt.getId());
		
		if (!result.isOK()) {
			setErrorMessage(result.getMessage());
			return;
		}
		
		try {
			ecrService.printReceipt(receipt, payments(), Optional.ofNullable(taxCodeText.getText()))
			.thenAcceptAsync(printResult -> {
				if (!printResult.isOk())
					Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getCurrent().getActiveShell(),
							ro.linic.ui.base.services.Messages.Error, printResult.error()));
			});
		}
		catch (final Exception e) {
			log.error(e.getMessage(), e);
			setErrorMessage(e.getLocalizedMessage());
			return;
		}
		
		super.okPressed();
	}
	
	private Map<PaymentType, BigDecimal> payments() {
		final BigDecimal cashValue = parse(cashText.getText());
	    final BigDecimal cardValue = parse(cardText.getText());
	    final BigDecimal creditValue = parse(creditText.getText());
	    final BigDecimal mealTicketValue = parse(mealTicketText.getText());
	    final BigDecimal valueTicketValue = parse(valueTicketText.getText());
	    final BigDecimal voucherValue = parse(voucherText.getText());
	    final BigDecimal modernPaymentValue = parse(modernPaymentText.getText());
	    final BigDecimal otherValue = parse(otherText.getText());
	    
	    final Map<PaymentType, BigDecimal> payments = new HashMap<>();
	    
	    if (greaterThan(cashValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.CASH, cashValue);
	    
	    if (greaterThan(cardValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.CARD, cardValue);
	    
	    if (greaterThan(creditValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.CREDIT, creditValue);
	    
	    if (greaterThan(mealTicketValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.MEAL_TICKET, mealTicketValue);
	    
	    if (greaterThan(valueTicketValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.VALUE_TICKET, valueTicketValue);
	    
	    if (greaterThan(voucherValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.VOUCHER, voucherValue);
	    
	    if (greaterThan(modernPaymentValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.MODERN_PAYMENT, modernPaymentValue);
	    
	    if (greaterThan(otherValue, BigDecimal.ZERO))
	    	payments.put(PaymentType.OTHER, otherValue);
	    
		return payments;
	}

	public static class Builder {
		private CloseReceiptDialog dialog;
		
		public static Builder cash(final Shell shell, final Receipt receipt) {
			final CloseReceiptDialog dialog = new CloseReceiptDialog(shell, receipt);
			dialog.cashTextVisible = true;
			dialog.cardTextVisible = false;
			dialog.creditTextVisible = false;
			dialog.mealTicketTextVisible = false;
			dialog.valueTicketTextVisible = false;
			dialog.voucherTextVisible = false;
			dialog.modernPaymentTextVisible = false;
			dialog.otherTextVisible = false;
			return new Builder(dialog);
		}
		
		public static Builder card(final Shell shell, final Receipt receipt) {
			final CloseReceiptDialog dialog = new CloseReceiptDialog(shell, receipt);
			dialog.cashTextVisible = false;
			dialog.cardTextVisible = true;
			dialog.creditTextVisible = false;
			dialog.mealTicketTextVisible = false;
			dialog.valueTicketTextVisible = false;
			dialog.voucherTextVisible = false;
			dialog.modernPaymentTextVisible = false;
			dialog.otherTextVisible = false;
			return new Builder(dialog);
		}
		
		/**
		 * Partial payment both by cash and card
		 */
		public static Builder partial(final Shell shell, final Receipt receipt) {
			final CloseReceiptDialog dialog = new CloseReceiptDialog(shell, receipt);
			dialog.cashTextVisible = true;
			dialog.cardTextVisible = true;
			dialog.creditTextVisible = false;
			dialog.mealTicketTextVisible = false;
			dialog.valueTicketTextVisible = false;
			dialog.voucherTextVisible = false;
			dialog.modernPaymentTextVisible = false;
			dialog.otherTextVisible = false;
			return new Builder(dialog);
		}
		
		public static Builder other(final Shell shell, final Receipt receipt) {
			return new Builder(new CloseReceiptDialog(shell, receipt));
		}
		
		private Builder(final CloseReceiptDialog dialog) {
			this.dialog = dialog;
		}
		
		public Builder initialValues(final Map<PaymentType, BigDecimal> values) {
			this.dialog.initialValues = Objects.requireNonNull(values);
			return this;
		}
		
		public CloseReceiptDialog build() {
			return dialog;
		}
	}
}
