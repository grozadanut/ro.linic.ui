package ro.linic.ui.legacy.anaf;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResultEvenIfOK;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.PersistedProp;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.Messages;
import ro.linic.ui.legacy.session.RestCaller;
import ro.linic.ui.legacy.session.RestCaller.HttpResponseW;
import ro.linic.ui.legacy.session.UIUtils;

public class AnafReporter
{
	public static Collection<ReportedInvoice> findReportedInvoicesById(final Collection<Long> ids)
	{
		if (!ids.iterator().hasNext())
			return List.of();
		
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String searchUrl = connectorBaseUrl + "/report/search/findAllById";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("ids", 
				ids.stream().map(String::valueOf).collect(Collectors.joining(","))));
		return RestCaller.get_Json_WithSSL(searchUrl, params, ReportedInvoice.class, null);
	}
	
	public static void reportInvoice(final int companyId, final long invoiceId)
	{
		try
		{
			final Optional<String> invoiceJson = loadInvoice(invoiceId);
			if (invoiceJson.isEmpty())
				return;
			
			final Optional<String> reportJson = report(companyId, invoiceJson.get());
			if (reportJson.isEmpty())
				return;
			
			// close AccountingDocument from further editing
			showResultEvenIfOK(BusinessDelegate.setEditableToAllContaDocs(ImmutableSet.of(invoiceId), false));
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			showException(e);
		}
		catch (final URISyntaxException e)
		{
			e.printStackTrace();
			showException(e);
		}
		
	}

	private static Optional<String> loadInvoice(final long invoiceId) throws IOException
	{
		final String invoiceBaseUrl = BusinessDelegate.persistedProp(PersistedProp.BILLING_BASE_URL_KEY)
				.getValueOr(PersistedProp.BILLING_BASE_URL_DEFAULT);
		final String invoiceUrl = invoiceBaseUrl + "/invoice/" + invoiceId;
		final Optional<HttpResponseW> invoiceResponse = RestCaller.get(invoiceUrl);
		
		if (invoiceResponse.isEmpty())
			return Optional.empty();
		
		if (HttpStatus.SC_OK != invoiceResponse.get().getStatusCode())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "GetInvoice ERROR "+invoiceId, 
					"GetInvoice STATUS: " + invoiceResponse.get().getStatusCode() + SPACE +
					invoiceResponse.get().getReasonPhrase());
			return Optional.empty();
		}
		final String invoiceJson = invoiceResponse.get().getEntity();
		return Optional.of(invoiceJson);
	}
	
	private static Optional<String> report(final int companyId, final String invoiceJson)
			throws IOException, URISyntaxException {
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String reportUrl = connectorBaseUrl + "/report";
		final Optional<HttpResponseW> reportResponse = RestCaller.put_WithSSL(reportUrl, invoiceJson,
				List.of(new BasicNameValuePair("companyId", String.valueOf(companyId))));
		
		if (reportResponse.isEmpty())
			return Optional.empty();
		
		if (HttpStatus.SC_UNAUTHORIZED == reportResponse.get().getStatusCode())
		{
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			{
			    final String registerUrl = connectorBaseUrl + "/auth/register/" + companyId;
				Desktop.getDesktop().browse(new URI(registerUrl));
			}
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OAuth2",
					Messages.AnafReporter_UnauthorizedMessage);
			return Optional.empty();
		}
		else if (HttpStatus.SC_OK != reportResponse.get().getStatusCode())
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Report ERROR", 
					"Report STATUS: " + reportResponse.get().getStatusCode() + SPACE +
					reportResponse.get().getReasonPhrase());
			return Optional.empty();
		}
		final String reportJson = reportResponse.get().getEntity();
		return Optional.of(reportJson);
	}
	
	public static void forceCheckReportedInvoices()
	{
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String checkUrl = connectorBaseUrl + "/report/check";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("companyId", 
				ClientSession.instance().getLoggedUser().getSelectedCompany().getId().toString()));
		final Optional<HttpResponseW> response = RestCaller.post_WithSSL(checkUrl, "", params);
		
		if (response.isEmpty())
			return;
		else if (HttpStatus.SC_OK != response.get().getStatusCode())
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Check ERROR", 
					"STATUS: " + response.get().getStatusCode() + SPACE +
					response.get().getReasonPhrase());
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
					response.get().getEntity() + NEWLINE +
					Messages.AnafReporter_ReloadBills);
	}
	
	public static void downloadResponse(final String downloadId)
	{
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setFileName(downloadId+".zip");
		final String outputFileUri = dialog.open();
		
		if (isEmpty(outputFileUri))
			return;
		
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String downloadUrl = connectorBaseUrl + "/report/download";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("companyId", 
				ClientSession.instance().getLoggedUser().getSelectedCompany().getId().toString()));
		final Optional<Long> downloadResponse = RestCaller.post_WithSSL_DownloadFile(downloadUrl, downloadId, outputFileUri, params);
		
		if (downloadResponse.isPresent())
		{
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
				try {
					Desktop.getDesktop().open(new File(outputFileUri));
				} catch (final IOException e) {
					e.printStackTrace();
					showException(e);
				}
			else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
				Desktop.getDesktop().browseFileDirectory(new File(outputFileUri));
			else
				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
						NLS.bind(Messages.FileSaved, outputFileUri));
		}
	}
	
	public static void xmlFileToPdf() throws IOException
	{
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.xml;*.xmls"});
		final String firstFileUri = dialog.open();
		
		if (isEmpty(firstFileUri))
			return;
		
		final String parentDirectory = dialog.getFilterPath();
		final String[] selectedFilePaths = dialog.getFileNames();
		final String xmlToPdfUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_DEFAULT);
		
		for (final String inputFileName : selectedFilePaths) {
			final Path inputFileAbsolutePath = Path.of(parentDirectory, inputFileName);
			final String outputFileUri = UIUtils.removeFileExtension(inputFileAbsolutePath.toString()) + ".pdf";
			final String invoiceXml = Files.readString(inputFileAbsolutePath);
			String xmlStandard = "FACT1";
			if (invoiceXml.contains("<CreditNote"))
				xmlStandard = "FCN";
			RestCaller.post_WithSSL_DownloadFile(xmlToPdfUrl+xmlStandard, invoiceXml, outputFileUri, List.of(),
					Map.of("Content-Type", "text/plain"));
			
			try
			{
				// throttle requests to limit ANAF quota as per official specs:
				// 100 Requests / 1 minute
				// 50 Spike arrest / 10 seconds
			    TimeUnit.SECONDS.sleep(1);
			}
			catch (final InterruptedException ie)
			{
			    Thread.currentThread().interrupt();
			}
		}
		
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
			Desktop.getDesktop().browseFileDirectory(new File(firstFileUri));
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK", Messages.AnafReporter_AllSaved);
	}
	
	public static void xmlToPdf(final String invoiceXml, final String outputFilename) throws IOException
	{
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setFileName(outputFilename);
		dialog.setFilterExtensions(new String[] {"*.pdf"});
		final String outputFileUri = dialog.open();
		
		if (isEmpty(outputFileUri))
			return;
		
		final String xmlToPdfUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_DEFAULT);
		String xmlStandard = "FACT1";
		if (invoiceXml.contains("<CreditNote"))
			xmlStandard = "FCN";
		
		RestCaller.post_WithSSL_DownloadFile(xmlToPdfUrl+xmlStandard, invoiceXml, outputFileUri, List.of(),
				Map.of("Content-Type", "text/plain"));
		
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
			Desktop.getDesktop().open(new File(outputFileUri));
		else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
			Desktop.getDesktop().browseFileDirectory(new File(outputFileUri));
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
					NLS.bind(Messages.FileSaved, outputFileUri));
	}
	
	public static Collection<ReceivedMessage> findAnafMessagesBetween(final LocalDateTime start, final LocalDateTime end)
	{
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String searchUrl = connectorBaseUrl + "/messages/search/between";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("start", start.toString()),
				new BasicNameValuePair("end", end.toString()));
		return RestCaller.get_Json_WithSSL(searchUrl, params, ReceivedMessage.class, null);
	}
	
	public static Job findInvoicesBetween(final AsyncLoadData<Invoice<?>> provider,
			final UISynchronize sync, final Logger log, final LocalDate start, final LocalDate end)
	{
		final Job job = Job.create("Loading Received Invoices", (ICoreRunnable) monitor ->
		{
			try
			{
				final SubMonitor sub = SubMonitor.convert(monitor, 3);
				sub.beginTask("Find received invoices", 3);
				final Stream<ReceivedInvoice> receivedInvoices = findReceivedInvoicesBetween(sync, start, end, sub.split(2));
				final Stream<ReceivedCreditNote> receivedCreditNotes = findCreditNotesBetween(sync, start, end, sub.split(1));
				final ImmutableList<Invoice<?>> data = Stream.concat(receivedInvoices, receivedCreditNotes)
						.sorted(Comparator.comparing(Invoice::getIssueDate))
						.collect(toImmutableList());
				
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(data));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});
		
		job.schedule();
		return job;
	}
	
	private static Stream<ReceivedInvoice> findReceivedInvoicesBetween(final UISynchronize sync, final LocalDate start, final LocalDate end,
			final IProgressMonitor monitor) {
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String searchUrl = connectorBaseUrl + "/invoices/search/between";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("start", start.toString()),
				new BasicNameValuePair("end", end.toString()));
		
		return RestCaller.get_Json_WithSSL(searchUrl, params, ReceivedInvoice.class, sync).stream();
	}
	
	private static Stream<ReceivedCreditNote> findCreditNotesBetween(final UISynchronize sync, final LocalDate start, final LocalDate end,
			final IProgressMonitor monitor) {
		final String connectorBaseUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_CONNECTOR_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_CONNECTOR_BASE_URL_DEFAULT);
		final String searchUrl = connectorBaseUrl + "/creditNotes/search/between";
		final List<NameValuePair> params = List.of(new BasicNameValuePair("start", start.toString()),
				new BasicNameValuePair("end", end.toString()));
		
		return RestCaller.get_Json_WithSSL(searchUrl, params, ReceivedCreditNote.class, sync).stream();
	}
}
