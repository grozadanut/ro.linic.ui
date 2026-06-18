package ro.linic.ui.legacy.anaf;

import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResultEvenIfOK;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.wrappers.TwoEntityWrapper;
import ro.linic.ui.base.dialogs.InfoDialog;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.legacy.anaf.ReceivedMessage.AnafReceivedMessageType;
import ro.linic.ui.legacy.anaf.ReportedInvoice.ReportState;
import ro.linic.ui.legacy.dialogs.EditPersistedPropDialog;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.Messages;
import ro.linic.ui.legacy.session.RestCaller;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.security.services.AuthenticationSession;

public class AnafMoquiReporter {
	private static final ILog log = ILog.of(AnafMoquiReporter.class);

	public static Collection<ReportedInvoice> findReportedInvoicesById(final IEclipseContext ctx,
			final Collection<Long> ids) {
		if (!ids.iterator().hasNext())
			return List.of();

		return ro.linic.ui.http.RestCaller.get("/rest/s1/moqui-anaf-efactura/report/search/findAllById")
				.internal(ctx.get(AuthenticationSession.class))
				.addUrlParam("ids", ids.stream().map(String::valueOf).collect(Collectors.joining(",")))
				.sync(t -> UIUtils.showException(t, ctx.get(UISynchronize.class))).stream()
				.map(gv -> new ReportedInvoice(gv.getLong("invoiceId"), mapReportState(gv.getString("statusId")),
						gv.getString("uploadIndex"), gv.getString("downloadId"), gv.getString("errorMessage")))
				.collect(Collectors.toList());
	}

	private static ReportState mapReportState(final String statusId) {
		return switch (statusId) {
		case "AnafRepInvUploadError" -> ReportState.UPLOAD_ERROR;
		case "AnafRepInvWaitingValidation" -> ReportState.WAITING_VALIDATION;
		case "AnafRepInvRejectedInvalid" -> ReportState.REJECTED_INVALID;
		case "AnafRepInvSent" -> ReportState.SENT;
		default -> null;
		};
	}

	public static void initializeSettingsOnDemand(final IEclipseContext ctx) {
		final Optional<GenericValue> anafToken = ro.linic.ui.http.RestCaller.get("/rest/s1/moqui-anaf-efactura/token")
				.internal(ctx.get(AuthenticationSession.class))
				.sync(GenericValue.class, t -> log.error(t.getMessage(), t));
		
		if (anafToken.isEmpty() || isEmpty(anafToken.get().getString("accessToken"))) {
			final String registerUrl = ro.linic.ui.base.services.util.UIUtils.moquiBaseUrl() + "/qapps/moqui_anaf_efactura";
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				try {
					Desktop.getDesktop().browse(new URI(registerUrl));
				} catch (IOException | URISyntaxException e) {
					log.error(e.getMessage(), e);
					InfoDialog.open(ctx.getActive(Shell.class), ro.linic.ui.base.services.Messages.Error,
							NLS.bind(Messages.AnafMoquiReporter_OpenManualMessage, registerUrl));
				}
			} else
				InfoDialog.open(ctx.getActive(Shell.class), ro.linic.ui.base.services.Messages.Error,
						NLS.bind(Messages.AnafMoquiReporter_OpenManualMessage, registerUrl));
			
			return;
		}

		if (isEmpty(anafToken.map(gv -> gv.getString("taxId")).orElse(null)))
			throw new UnsupportedOperationException(
					"Discutati cu administratorul sistemului pentru a va configura CUI-ul pentru eFactura!");

		final ImmutableList<PersistedProp> props = BusinessDelegate.allPersistedProps();
		final String firmaNameV = findOrDefault(props, PersistedProp.FIRMA_NAME_KEY, "");
		final String firmaCuiV = findOrDefault(props, PersistedProp.FIRMA_CUI_KEY, "");
		final String firmaBillingAddressStreetV = findOrDefault(props, "firma_billing_primary_line", "");
		final String firmaBillingAddressCityV = findOrDefault(props, "firma_billing_city", "");
		final String firmaBillingAddressCodJudetV = findOrDefault(props, "firma_billing_cod_judet", "");

		if (isEmpty(firmaNameV) || isEmpty(firmaCuiV) || isEmpty(firmaBillingAddressStreetV)
				|| isEmpty(firmaBillingAddressCityV) || isEmpty(firmaBillingAddressCodJudetV)) {
			final Map<TwoEntityWrapper<String>, PersistedProp> propsToEdit = new HashMap<>();
			propsToEdit.put(new TwoEntityWrapper<>("01", "Nume Firma"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_NAME_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("02", "CUI"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_CUI_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("03", "Nr.Reg.Com."), BusinessDelegate.persistedProp(PersistedProp.FIRMA_REG_COM_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("04", "Serie factura"), BusinessDelegate.persistedProp(PersistedProp.SERIA_FACTURA_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("05", "Capital social"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_CAP_SOCIAL_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("06", "Telefon"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_PHONE_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("07", "Email"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_EMAIL_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("08", "IBAN"), BusinessDelegate.persistedProp(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY));
			propsToEdit.put(new TwoEntityWrapper<>("09", "Cod judet(ex: RO-BH)"), BusinessDelegate.persistedProp("firma_billing_cod_judet"));
			propsToEdit.put(new TwoEntityWrapper<>("10", "Oras"), BusinessDelegate.persistedProp("firma_billing_city"));
			propsToEdit.put(new TwoEntityWrapper<>("11", "Adresa"), BusinessDelegate.persistedProp("firma_billing_primary_line"));

			new EditPersistedPropDialog(ctx.getActive(Shell.class), propsToEdit,
					Messages.AnafMoquiReporter_EditPropsMessage).open();
		}
	}

	private static String findOrDefault(final List<PersistedProp> props, final String key, final String defaultValue) {
		return props.stream().filter(prop -> key.equalsIgnoreCase(prop.getKey())).findFirst()
				.map(PersistedProp::getValue).orElse(defaultValue);
	}

	public static void reportInvoice(final IEclipseContext ctx, final int companyId, final long invoiceId) {
		try {
			final Optional<String> invoiceJson = loadInvoice(ctx, invoiceId);
			if (invoiceJson.isEmpty())
				return;

			final Optional<String> reportJson = report(ctx, invoiceJson.get());
			if (reportJson.isEmpty())
				return;

			// close AccountingDocument from further editing
			showResultEvenIfOK(BusinessDelegate.setEditableToAllContaDocs(ImmutableSet.of(invoiceId), false));
		} catch (final IOException e) {
			e.printStackTrace();
			showException(e);
		} catch (final URISyntaxException e) {
			e.printStackTrace();
			showException(e);
		}

	}

	private static Optional<String> loadInvoice(final IEclipseContext ctx, final long invoiceId) throws IOException {
		return ro.linic.ui.http.RestCaller.get("/rest/s1/moqui-linic-legacy/invoice/" + invoiceId)
				.internal(ctx.get(AuthenticationSession.class))
				.syncRaw(BodyHandlers.ofString(), t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
				.map(HttpResponse::body);
	}

	private static Optional<String> report(final IEclipseContext ctx, final String invoiceJson)
			throws IOException, URISyntaxException {
		return ro.linic.ui.http.RestCaller.put("/rest/s1/moqui-anaf-efactura/report")
				.internal(ctx.get(AuthenticationSession.class))
				.body(BodyProvider.of(invoiceJson))
				.syncRaw(BodyHandlers.ofString(), t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
				.map(HttpResponse::body);
	}

	public static void forceCheckReportedInvoices(final IEclipseContext ctx) {
		final Optional<String> response = ro.linic.ui.http.RestCaller.post("/rest/s1/moqui-anaf-efactura/report/check")
		.internal(ctx.get(AuthenticationSession.class))
		.syncRaw(BodyHandlers.ofString(), t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
		.map(HttpResponse::body);

		if (response.isPresent())
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK", Messages.AnafReporter_ReloadBills);
	}

	public static void downloadResponse(final IEclipseContext ctx, final String downloadId) {
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setFileName(downloadId + ".zip");
		final String outputFileUri = dialog.open();

		if (isEmpty(outputFileUri))
			return;

		final Optional<Long> downloadResponse = ro.linic.ui.http.RestCaller
				.post("/rest/s1/moqui-anaf-efactura/report/download").internal(ctx.get(AuthenticationSession.class))
				.body(BodyProvider.of(GenericValue.of("", "", "downloadId", downloadId)))
				.syncRaw(BodyHandlers.ofInputStream(), t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
				.flatMap(r -> UIUtils.copyFileFromTo(r.body(), outputFileUri));

		if (downloadResponse.isPresent()) {
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

	public static void xmlFileToPdf() throws IOException {
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] { "*.xml;*.xmls" });
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
			RestCaller.post_WithSSL_DownloadFile(xmlToPdfUrl + xmlStandard, invoiceXml, outputFileUri, List.of(),
					Map.of("Content-Type", "text/plain"));

			try {
				// throttle requests to limit ANAF quota as per official specs:
				// 100 Requests / 1 minute
				// 50 Spike arrest / 10 seconds
				TimeUnit.SECONDS.sleep(1);
			} catch (final InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
			Desktop.getDesktop().browseFileDirectory(new File(firstFileUri));
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK", Messages.AnafReporter_AllSaved);
	}

	public static void xmlToPdf(final String invoiceXml, final String outputFilename) throws IOException {
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setFileName(outputFilename);
		dialog.setFilterExtensions(new String[] { "*.pdf" });
		final String outputFileUri = dialog.open();

		if (isEmpty(outputFileUri))
			return;

		final String xmlToPdfUrl = BusinessDelegate.persistedProp(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_KEY)
				.getValueOr(PersistedProp.ANAF_XML_TO_PDF_BASE_URL_DEFAULT);
		final String noValidation = BusinessDelegate.persistedProp("anaf_xml_to_pdf_novalidate").getValueOr("/DA");

		String xmlStandard = "FACT1";
		if (invoiceXml.contains("<CreditNote"))
			xmlStandard = "FCN";

		RestCaller.post_WithSSL_DownloadFile(xmlToPdfUrl + xmlStandard + noValidation, invoiceXml, outputFileUri,
				List.of(), Map.of("Content-Type", "text/plain"));

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
			Desktop.getDesktop().open(new File(outputFileUri));
		else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
			Desktop.getDesktop().browseFileDirectory(new File(outputFileUri));
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
					NLS.bind(Messages.FileSaved, outputFileUri));
	}

	public static Collection<ReceivedMessage> findAnafMessagesBetween(final IEclipseContext ctx, final LocalDateTime start,
			final LocalDateTime end) {
		return ro.linic.ui.http.RestCaller.get("/rest/s1/moqui-anaf-efactura/messages/search/between")
				.internal(ctx.get(AuthenticationSession.class))
				.addUrlParam("from", Timestamp.valueOf(start).toString())
				.addUrlParam("thru", Timestamp.valueOf(end).toString())
				.sync(t -> UIUtils.showException(t, ctx.get(UISynchronize.class))).stream()
				.map(gv -> new ReceivedMessage(gv.getLong("id"), Timestamp.valueOf(gv.getString("creationDate")).toLocalDateTime(),
						gv.getString("taxId"), gv.getString("uploadIndex"), gv.getString("details"), mapMessageState(gv.getString("statusId"))))
				.collect(Collectors.toList());
	}
	
	private static AnafReceivedMessageType mapMessageState(final String statusId) {
		return switch (statusId) {
		case "AnafRecMsgBillReceived" -> AnafReceivedMessageType.FACTURA_PRIMITA;
		case "AnafRecMsgBillSent" -> AnafReceivedMessageType.FACTURA_TRIMISA;
		case "AnafRecMsgBillErrors" -> AnafReceivedMessageType.ERORI_FACTURA;
		case "AnafRecMsgBuyerMessage" -> AnafReceivedMessageType.MESAJ_CUMPARATOR_TRANSMIS;
		default -> null;
		};
	}
}
