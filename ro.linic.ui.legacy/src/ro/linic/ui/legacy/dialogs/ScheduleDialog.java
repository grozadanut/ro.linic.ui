package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDateTime;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
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
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.embeddable.Address;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.TransportType;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.StringUtils;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.components.UsedAddressContentProposal;
import ro.linic.ui.legacy.dialogs.components.UsedAddressContentProposalProvider;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.DocumentNatTable;
import ro.linic.ui.legacy.tables.DocumentNatTable.SourceLoc;

public class ScheduleDialog extends Dialog
{
	// MODEL
	private ImmutableList<AccountingDocument> allScheduledDocs = ImmutableList.of();
	private AccountingDocument doc;
	private ImmutableList<Partner> allPartners;
	private ImmutableList<Masina> allCars;
	private ImmutableList<TransportType> allTransportTypes = ImmutableList.copyOf(TransportType.values());
	private String emailSignature;
	private AccountingDocument reloadedDoc;
	
	private Combo partner;
	private UsedAddressContentProposalProvider usedAddressProvider;
	private ContentProposalAdapter deliveryProposalAdapter;
	private ContentProposalAdapter phoneProposalAdapter;
	private ContentProposalAdapter indicatiiProposalAdapter;
	private Text deliveryAddress;
	private Text phone;
	private Text indicatiiDoc;
	
	private List masina;
	private List transportType;
	private DateTime transportDate;
	private DateTime transportTime;
	private Button payAtDriver;
	
	private DocumentNatTable table;
	
	private Button printDocs;
	private Button printDocsComasat;
	
	private UISynchronize sync;
	private Logger log;
	private Bundle bundle;
	
	public ScheduleDialog(final Shell parent, final UISynchronize sync, final Logger log, final Bundle bundle,
			final AccountingDocument doc)
	{
		super(parent);
		this.sync = sync;
		this.log = log;
		this.bundle = bundle;
		this.doc = doc;
		
		emailSignature = BusinessDelegate.persistedProp(PersistedProp.EMAIL_SIGNATURE_KEY)
				.getValueOr(PersistedProp.EMAIL_SIGNATURE_DEFAULT);
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(3, false));
		getShell().setText("Programeaza");
		
		final Label partnerLabel = new Label(contents, SWT.NONE);
		partnerLabel.setText("Partener");
		UIUtils.setFont(partnerLabel);
		
		partner = new Combo(contents, SWT.DROP_DOWN);
		UIUtils.setFont(partner);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(partner);
		
		final Label deliveryLabel = new Label(contents, SWT.NONE);
		deliveryLabel.setText("Adresa");
		UIUtils.setFont(deliveryLabel);
		
		usedAddressProvider = new UsedAddressContentProposalProvider(log);
		deliveryAddress = new Text(contents, SWT.BORDER);
		deliveryAddress.setMessage("ORADEA");
		UIUtils.setFont(deliveryAddress);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(deliveryAddress);
		deliveryProposalAdapter = new ContentProposalAdapter(deliveryAddress, new TextContentAdapter(), usedAddressProvider, null, null);
		deliveryProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		deliveryProposalAdapter.addContentProposalListener(new UpdateUsedAddressListener());
		
		final Label phoneLabel = new Label(contents, SWT.NONE);
		phoneLabel.setText("Telefon");
		UIUtils.setFont(phoneLabel);
		GridDataFactory.fillDefaults().applyTo(phoneLabel);
		
		phone = new Text(contents, SWT.BORDER);
		phone.setMessage("07x");
		UIUtils.setFont(phone);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(phone);
		phoneProposalAdapter = new ContentProposalAdapter(phone, new TextContentAdapter(), usedAddressProvider, null, null);
		phoneProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		phoneProposalAdapter.addContentProposalListener(new UpdateUsedAddressListener());
		
		final Label indicatiiDocLabel = new Label(contents, SWT.NONE);
		indicatiiDocLabel.setText("Indicatii");
		UIUtils.setFont(indicatiiDocLabel);
		GridDataFactory.fillDefaults().applyTo(indicatiiDocLabel);
		
		indicatiiDoc = new Text(contents, SWT.BORDER);
		UIUtils.setFont(indicatiiDoc);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(indicatiiDoc);
		indicatiiProposalAdapter = new ContentProposalAdapter(indicatiiDoc, new TextContentAdapter(), usedAddressProvider, null, null);
		indicatiiProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		indicatiiProposalAdapter.addContentProposalListener(new UpdateUsedAddressListener());
		
		final Label masinaLabel = new Label(contents, SWT.NONE);
		masinaLabel.setText("Tip/Masina");
		UIUtils.setFont(masinaLabel);
		GridDataFactory.fillDefaults().applyTo(masinaLabel);
		
		transportType = new List(contents, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		UIUtils.setFont(transportType);
		GridDataFactory.fillDefaults().applyTo(transportType);
		transportType.setItems(allTransportTypes.stream()
				.map(TransportType::displayName)
				.toArray(String[]::new));

		masina = new List(contents, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		UIUtils.setFont(masina);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, transportType.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)
		.grab(true, false).applyTo(masina);
		
		final Label transportDateTimeLabel = new Label(contents, SWT.NONE);
		transportDateTimeLabel.setText("Data si ora");
		UIUtils.setFont(transportDateTimeLabel);
		GridDataFactory.fillDefaults().applyTo(transportDateTimeLabel);
		
		final Composite transportDateCont = new Composite(contents, SWT.NONE);
		transportDateCont.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(transportDateCont);
		
		transportDate = new DateTime(transportDateCont, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(transportDate);
		
		transportTime = new DateTime(transportDateCont, SWT.TIME | SWT.SHORT);
		UIUtils.setFont(transportTime);
		
		final Label payAtDriverLabel = new Label(contents, SWT.NONE);
		payAtDriverLabel.setText("Achitare");
		UIUtils.setFont(payAtDriverLabel);
		GridDataFactory.fillDefaults().applyTo(payAtDriverLabel);
		
		payAtDriver = new Button(contents, SWT.CHECK);
		payAtDriver.setText("Achitare la sofer");
		UIUtils.setFont(payAtDriver);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(payAtDriver);
		
		table = new DocumentNatTable(SourceLoc.SCHEDULE_DIALOG, bundle, log);
		table.postConstruct(contents);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(table.getTable());
		
		final Composite bottomButtonsCont = new Composite(contents, SWT.NONE);
		bottomButtonsCont.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().span(3, 1).applyTo(bottomButtonsCont);
		
		printDocs = new Button(bottomButtonsCont, SWT.PUSH);
		printDocs.setText("Printeaza documentele selectate");
		UIUtils.setFont(printDocs);
		
		printDocsComasat = new Button(bottomButtonsCont, SWT.PUSH);
		printDocsComasat.setText("Printeaza documentele selectate comasate");
		UIUtils.setFont(printDocsComasat);
		
		addListeners();
		loadData();
		selectDocFields();
		// DateTime Bug fix: if we call transportDate.setEnabled(false); before the widget is painted,
		// the calendar dropdown remains blocked in the disabled state, even if we enable it afterwards,
		// and we cannot click anywhere inside, only use the keyboard or the numbers on the widget
		sync.asyncExec(new Runnable()
		{
			@Override public void run()
			{
				updateDateEnablement();
			}
		});
		return contents;
	}
	
	private void addListeners()
	{
		partner.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateTransportFields();
				updatePayAtDriver();
			}
		});
		
		masina.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				filterData();
			}
		});
		
		transportType.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				updateDateEnablement();
			}
		});
		
		transportDate.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				filterData();
			}
		});
		
		printDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, table.selectedAccDocs(), true);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});
		
		printDocsComasat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, JasperReportManager.comasarePtPrint(table.selectedAccDocs(), true), false);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});
	}
	
	private void loadData()
	{
		reloadPartners();
		reloadCars();
		
		BusinessDelegate.undeliveredDocs(new AsyncLoadData<AccountingDocument>()
		{
			@Override public void success(final ImmutableList<AccountingDocument> data)
			{
				allScheduledDocs = data;
				table.loadData(data);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea documentelor", details);
			}
		}, sync, null, null);
	}
	
	private void filterData()
	{
		Stream<AccountingDocument> filteredData = allScheduledDocs.stream()
				.filter(doc -> (doc.getTransportType() != null && !doc.getTransportType().equals(TransportType.SCHEDULED)) ||
						doc.getTransportDateTime() == null ||
						doc.getTransportDateTime().toLocalDate().equals(extractLocalDate(transportDate)));
		
		final Optional<Masina> selMasina = masina();
		if (selMasina.isPresent())
			filteredData = filteredData.filter(doc -> Objects.equals(doc.getMasina(), selMasina.get()));
			
		table.loadData(filteredData.collect(toImmutableList()));
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(1100, 750);
	}
	
	@Override
	protected void okPressed()
	{
		if (doc != null)
		{
			final boolean reprogramat = doc.getPartner() != null || doc.isShouldTransport() && doc.getTransportDateTime() != null;
			final String oldAppointment = doc.clientExplanation();
			
			showResult(BusinessDelegate.changeDocPartner(doc.getId(), partner().map(Partner::getId), false));
			final InvocationResult scheduleResult = BusinessDelegate.scheduleDoc(doc.getId(), masina().map(Masina::getId).orElse(null),
					extractLocalDateTime(transportDate, transportTime), indicatiiDoc.getText(), deliveryAddress.getText(),
					phone.getText(), transportType().orElse(TransportType.FIRST_DELIVERY), payAtDriver.getSelection());
			showResult(scheduleResult);

			if (scheduleResult.statusOk())
			{
				reloadedDoc = scheduleResult.extra(InvocationResult.ACCT_DOC_KEY);
				notifyClients(reprogramat, oldAppointment, reloadedDoc);
				super.okPressed();
			}
		}
	}
	
	private void notifyClients(final boolean reprogramat, final String oldAppointment, final AccountingDocument reloadedDoc)
	{
		if (!partner().isPresent() || !partner().get().isNotifyAppointment())
			return;

		if (reprogramat)
			openRescheduleEmailDialog(oldAppointment, reloadedDoc);
		else
			openEmailDialog(reloadedDoc);
	}

	private void openEmailDialog(final AccountingDocument reloadedDoc)
	{
		final String partnerName = partner().map(Partner::getName).orElse(EMPTY_STRING);

		final StringBuilder messageSB = new StringBuilder();
		messageSB.append("Catre ").append(partnerName).append(NEWLINE).append(NEWLINE)
		.append("Buna ziua,").append(NEWLINE).append(NEWLINE)
		.append("Va rugam sa aveti in vedere programarea serviciilor dupa cum urmeaza:").append(NEWLINE)
		.append(reloadedDoc.clientExplanation()).append(NEWLINE).append(NEWLINE)
		.append(emailSignature);

		SendEmailDialog.open(Display.getCurrent().getActiveShell(), log, 
				partner().map(Partner::getEmail).orElse(EMPTY_STRING), "Programare servicii", messageSB.toString(), null, EMPTY_STRING);
	}

	private void openRescheduleEmailDialog(final String oldAppointment, final AccountingDocument reloadedDoc)
	{
		final String partnerName = partner().map(Partner::getName).orElse(EMPTY_STRING);

		final StringBuilder messageSB = new StringBuilder();
		messageSB.append("Catre ").append(partnerName).append(NEWLINE).append(NEWLINE)
		.append("Buna ziua,").append(NEWLINE).append(NEWLINE)
		.append("Va informam ca programarea urmatoare:").append(NEWLINE)
		.append(oldAppointment).append(NEWLINE)
		.append("a fost reprogramata in:").append(NEWLINE)
		.append(reloadedDoc.clientExplanation()).append(NEWLINE).append(NEWLINE)
		.append(emailSignature);

		SendEmailDialog.open(Display.getCurrent().getActiveShell(), log, 
				partner().map(Partner::getEmail).orElse(EMPTY_STRING), "Reprogramare servicii curatenie", messageSB.toString(), null, EMPTY_STRING);
		}
	
	private void updateTransportFields()
	{
		final Optional<Partner> selPartner = partner();
		deliveryAddress.setText(Optional.ofNullable(doc).map(AccountingDocument::addressNullable)
				.orElse(selPartner.map(Partner::deliveryAddressNullable)
						.orElse(selPartner.map(Partner::getAddress).map(Address::displayName).orElse(EMPTY_STRING))));
		phone.setText(Optional.ofNullable(doc).map(AccountingDocument::getPhone).filter(StringUtils::notEmpty)
				.orElseGet(() -> selPartner.map(Partner::getPhone).orElse(EMPTY_STRING)));
		usedAddressProvider.setSelectedPartner(selPartner.orElse(null));
	}
	
	private void updatePayAtDriver()
	{
		final Optional<Partner> selPartner = partner();
		selPartner.ifPresent(p -> payAtDriver.setSelection(p.getTermenPlata() == null || p.getTermenPlata() <= 0));
	}
	
	private void updateDateEnablement()
	{
		switch (transportType().orElse(TransportType.FIRST_DELIVERY))
		{
		case SCHEDULED:
			transportDate.setEnabled(true);
			transportTime.setEnabled(true);
			break;

		case FIRST_DELIVERY:
		case URGENT:
		case CLIENT_PICKUP:
		case WAIT_ORDER:
		default:
			transportDate.setEnabled(false);
			transportTime.setEnabled(false);
		}
	}
	
	private void selectDocFields()
	{
		if (doc != null)
		{
			partner.select(allPartners.indexOf(doc.getPartner()));
			updateTransportFields();
			masina.select(allCars.indexOf(doc.getMasina()));
			final LocalDateTime transport = Optional.ofNullable(doc).map(AccountingDocument::getTransportDateTime).orElse(LocalDateTime.now());
			insertDate(transportDate, transport);
			insertDate(transportTime, transport);
			indicatiiDoc.setText(safeString(doc.getIndicatii()));
		}
		
		transportType.select(allTransportTypes.indexOf(Optional.ofNullable(doc)
				.map(AccountingDocument::getTransportType)
				.orElse(TransportType.FIRST_DELIVERY)));
		payAtDriver.setSelection(Optional.ofNullable(doc)
				.map(AccountingDocument::isPayAtDriver)
				.orElse(true));
	}
	
	private void reloadPartners()
	{
		allPartners = BusinessDelegate.allPartners();
		partner.setItems(allPartners.stream()
				.map(Partner::getName)
				.toArray(String[]::new));
	}
	
	private void reloadCars()
	{
		allCars = BusinessDelegate.dbMasini();
		masina.setItems(allCars.stream()
				.map(Masina::getNr)
				.toArray(String[]::new));
		if (allCars.size() == 1)
			masina.select(0);
	}
	
	private Optional<Partner> partner()
	{
		final int index = partner.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allPartners.get(index));
	}
	
	private Optional<Masina> masina()
	{
		final int index = masina.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allCars.get(index));
	}
	
	private Optional<TransportType> transportType()
	{
		final int index = transportType.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allTransportTypes.get(index));
	}
	
	public AccountingDocument reloadedDoc()
	{
		return reloadedDoc;
	}
	
	private class UpdateUsedAddressListener implements IContentProposalListener
	{
		@Override public void proposalAccepted(final IContentProposal proposal)
		{
			final UsedAddressContentProposal usedAddressProposal = (UsedAddressContentProposal) proposal;
			deliveryProposalAdapter.setEnabled(false);
			phoneProposalAdapter.setEnabled(false);
			indicatiiProposalAdapter.setEnabled(false);
			
			deliveryAddress.setText(usedAddressProposal.extractTextFromContent(UsedAddressContentProposal.ADDRESS_INDEX));
			phone.setText(usedAddressProposal.extractTextFromContent(UsedAddressContentProposal.PHONE_INDEX));
			indicatiiDoc.setText(usedAddressProposal.extractTextFromContent(UsedAddressContentProposal.INDICATII_INDEX));
			
			deliveryAddress.setSelection(deliveryAddress.getText().length());
			phone.setSelection(phone.getText().length());
			indicatiiDoc.setSelection(indicatiiDoc.getText().length());
			
			deliveryProposalAdapter.setEnabled(true);
			phoneProposalAdapter.setEnabled(true);
			indicatiiProposalAdapter.setEnabled(true);
		}
	}
}
