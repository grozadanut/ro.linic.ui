package ro.linic.ui.legacy.dialogs;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Partner;
import ro.colibri.util.LocalDateUtils;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.services.AuthenticationSession;

public class ReceptieEFacturaDialog extends TitleAreaDialog {
	public enum ReceiveType {
		RECEIVE("Receptioneaza"), MARK("Doar marcheaza");

		private final String name;

		private ReceiveType(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
	private static final List<ReceiveType> RECEIVE_TYPES = Arrays.asList(ReceiveType.values());

	// MODEL
	private ImmutableList<Partner> allPartners;
	private GenericValue anafInvoice;
	private List<GenericValue> anafInvoiceLines;

	private Combo supplier;
	private Text invoiceNumber;
	private Text issueDate;
	private Text taxExclusiveAmount;
	private Text taxTotal;
	private Text invoiceTotal;
	private Text linesSummary;
	private Combo receiveType;

	private Logger log;
	private AuthenticationSession authSession;

	public ReceptieEFacturaDialog(final Shell parent, final Logger log, final AuthenticationSession authSession,
			final GenericValue anafInvoice, final List<GenericValue> anafInvoiceLines) {
		super(parent);
		this.log = log;
		this.authSession = authSession;
		this.anafInvoice = anafInvoice;
		this.anafInvoiceLines = anafInvoiceLines;
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.ReceptieEFacturaDialog_Title);
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(3, true));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label supplierLabel = new Label(container, SWT.NONE);
		supplierLabel.setText(Messages.ReceptieEFacturaDialog_Supplier);
		GridDataFactory.swtDefaults().applyTo(supplierLabel);

		final Label invoiceNumberLabel = new Label(container, SWT.NONE);
		invoiceNumberLabel.setText(Messages.ReceptieEFacturaDialog_InvoiceNumber);
		GridDataFactory.swtDefaults().applyTo(invoiceNumberLabel);

		final Label issueDateLabel = new Label(container, SWT.NONE);
		issueDateLabel.setText(Messages.ReceptieEFacturaDialog_IssueDate);
		GridDataFactory.swtDefaults().applyTo(issueDateLabel);

		supplier = new Combo(container, SWT.DROP_DOWN);
		UIUtils.setFont(supplier);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(supplier);

		invoiceNumber = new Text(container, SWT.SINGLE | SWT.BORDER);
		invoiceNumber.setEditable(false);
		UIUtils.setFont(invoiceNumber);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(invoiceNumber);

		issueDate = new Text(container, SWT.SINGLE | SWT.BORDER);
		issueDate.setEditable(false);
		UIUtils.setFont(issueDate);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(issueDate);

		final Label taxExclusiveAmountLabel = new Label(container, SWT.NONE);
		taxExclusiveAmountLabel.setText(Messages.ReceptieEFacturaDialog_TaxExclusiveAmount);
		GridDataFactory.swtDefaults().applyTo(taxExclusiveAmountLabel);

		final Label taxTotalLabel = new Label(container, SWT.NONE);
		taxTotalLabel.setText(Messages.ReceptieEFacturaDialog_TaxTotal);
		GridDataFactory.swtDefaults().applyTo(taxTotalLabel);

		final Label invoiceTotalLabel = new Label(container, SWT.NONE);
		invoiceTotalLabel.setText(Messages.ReceptieEFacturaDialog_InvoiceTotal);
		GridDataFactory.swtDefaults().applyTo(invoiceTotalLabel);

		taxExclusiveAmount = new Text(container, SWT.SINGLE | SWT.BORDER);
		taxExclusiveAmount.setEditable(false);
		UIUtils.setFont(taxExclusiveAmount);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(taxExclusiveAmount);

		taxTotal = new Text(container, SWT.SINGLE | SWT.BORDER);
		taxTotal.setEditable(false);
		UIUtils.setFont(taxTotal);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(taxTotal);

		invoiceTotal = new Text(container, SWT.SINGLE | SWT.BORDER);
		invoiceTotal.setEditable(false);
		UIUtils.setFont(invoiceTotal);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(invoiceTotal);

		linesSummary = new Text(container, SWT.MULTI | SWT.BORDER);
		linesSummary.setEditable(false);
		UIUtils.setFont(linesSummary);
		GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(linesSummary);

		receiveType = new Combo(container, SWT.DROP_DOWN);
		UIUtils.setFont(receiveType);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(receiveType);

		fillFields();
		return area;
	}

	private void fillFields() {
		reloadPartners();
		final Optional<Partner> foundSupplier = allPartners.stream()
				.filter(p -> anafInvoice.getString("senderId").equalsIgnoreCase(p.getCodFiscal()))
				.findFirst();
		if (foundSupplier.isPresent())
			supplier.select(allPartners.indexOf(foundSupplier.get()));
		
		receiveType.setItems(RECEIVE_TYPES.stream().map(ReceiveType::getName).toArray(String[]::new));
		receiveType.select(RECEIVE_TYPES.indexOf(ReceiveType.RECEIVE));
		
		invoiceNumber.setText(anafInvoice.getString("invoiceNumber"));
		issueDate.setText(LocalDateUtils.displayLocalDate(new Timestamp(anafInvoice.getLong("issueDate")).toLocalDateTime().toLocalDate()));
		taxExclusiveAmount.setText(anafInvoice.getString("taxExclusiveAmount"));
		taxTotal.setText(anafInvoice.getString("taxTotal"));
		invoiceTotal.setText(anafInvoice.getString("invoiceTotal"));
		linesSummary.setText(linesSummary());
	}

	private String linesSummary() {
		final StringBuilder summary = new StringBuilder();
		
		for (final GenericValue line : anafInvoiceLines) {
			summary.append(line.getString("lineId")).append(PresentationUtils.SPACE)
			.append(line.getString("name")).append(PresentationUtils.SPACE)
			.append(line.getString("price")).append(PresentationUtils.SPACE)
			.append(line.getString("priceCurrency")).append(" X ")
			.append(line.getString("quantity")).append(PresentationUtils.SPACE)
			.append(line.getString("uom")).append(" = ")
			.append(line.getString("total")).append(PresentationUtils.SPACE)
			.append(line.getString("totalCurrency")).append(PresentationUtils.NEWLINE);
		}
		
		return summary.toString();
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 600);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.PROCEED_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		if (partner().isEmpty()) {
			setErrorMessage(Messages.ReceptieEFacturaDialog_PartnerMissingError);
			return;
		}
		if (type().isEmpty()) {
			setErrorMessage(Messages.ReceptieEFacturaDialog_TypeMissingError);
			return;
		}
		if ("SmsgConfirmed".equals(anafInvoice.getString("statusId"))) {
			setErrorMessage(Messages.ReceptieEFacturaDialog_InvoiceAlreadyReceivedError);
			return;
		}
		
		switch (type().get()) {
		case RECEIVE:
			
			break;
			
		case MARK:
			try {
				final HttpResponse<String> response = RestCaller.post("/rest/s1/moqui-linic-legacy/anafInvoices/receive")
						.internal(authSession.authentication())
						.addUrlParam("systemMessageId", anafInvoice.getString("id"))
						.asyncRaw(BodyHandlers.ofString())
						.get();
				
				if (response.statusCode() != 200) {
					setErrorMessage(response.body());
					return;
				}
				
				anafInvoice.put("statusId", "SmsgConfirmed");
			} catch (AuthenticationException | InterruptedException | ExecutionException e) {
				log.error(e);
				setErrorMessage(e.getMessage());
				return;
			}
			
			break;

		default:
			setErrorMessage(MessageFormat.format(Messages.ReceptieEFacturaDialog_UnknownTypeError, type().get()));
			return;
		}

		super.okPressed();
	}

	private void reloadPartners() {
		allPartners = BusinessDelegate.allPartners();
		supplier.setItems(allPartners.stream().map(Partner::getName).toArray(String[]::new));
	}

	private Optional<Partner> partner() {
		final int index = supplier.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.of(allPartners.get(index));
	}
	
	private Optional<ReceiveType> type() {
		final int index = receiveType.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.of(RECEIVE_TYPES.get(index));
	}
}
