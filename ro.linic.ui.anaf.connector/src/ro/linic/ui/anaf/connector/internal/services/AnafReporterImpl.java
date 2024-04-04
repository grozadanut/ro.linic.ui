package ro.linic.ui.anaf.connector.internal.services;

import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.StringUtils.isEmpty;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import ro.linic.ui.anaf.connector.Messages;
import ro.linic.ui.anaf.connector.internal.preferences.PreferenceKey;
import ro.linic.ui.anaf.connector.services.AnafReporter;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.RestCaller;
import ro.linic.util.commons.HttpStatusCode;

@Component
public class AnafReporterImpl implements AnafReporter {
	private static final Logger log = Logger.getLogger(AnafReporterImpl.class.getName());
	
	@Override
	public void xmlToPdf(final String xml, final String outputFileUri) throws IOException {
		if (isEmpty(xml) || isEmpty(outputFileUri))
			return;
		
		String xmlStandard = "FACT1";
		if (xml.contains("<CreditNote"))
			xmlStandard = "FCN";
		
		final IEclipsePreferences prefNode = ConfigurationScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
		
		// {0} - standard: Specifica tipul xml-ului ce trebuie transmis. Valorile acceptate sunt FACT1 si FCN.
		// {1} - novld: Daca este prezent si are valoare DA, xml-ul nu v-a fi validat. Valoarea acceptata este DA. Fara validare nu garantam corectitudinea informatiilor din pdf-ul generat
		final String xmlToPdfUrl = MessageFormat.format("https://webservicesp.anaf.ro/prod/FCTEL/rest/transformare/{0}/{1}",
				xmlStandard,
				prefNode.getBoolean(PreferenceKey.XML_TO_PDF_VALIDATE, PreferenceKey.XML_TO_PDF_VALIDATE_DEF) ? EMPTY_STRING : "DA");

		RestCaller.post(xmlToPdfUrl)
				.addHeader("Content-Type", "text/plain")
				.body(BodyProvider.of(xml))
				.async(BodyHandlers.ofInputStream())
				.thenApply(resp -> writeResponseToFile(resp, outputFileUri))
				.thenAccept(this::openFile);
	}
	
	private String writeResponseToFile(final HttpResponse<InputStream> resp, final String outputFileUri) {
		if (HttpStatusCode.OK.getValue() != resp.statusCode())
			throw new UnsupportedOperationException("Response status code: "+resp.statusCode());
		
		String resultFileUri;
		final Optional<String> contentHeader = resp.headers().firstValue("content-type");
		if (contentHeader.isPresent() && contentHeader.get().equalsIgnoreCase("application/json"))
			resultFileUri = FilenameUtils.removeExtension(outputFileUri)+"_errors.txt";
		else
			resultFileUri = outputFileUri;
		
		try {
			FileUtils.copyInputStreamToFile(resp.body(), new File(resultFileUri));
		} catch (final IOException e) {
			log.log(Level.SEVERE, "Error writting file: "+resultFileUri, e);
		}
		return resultFileUri;
	}
	
	private void openFile(final String fileUri) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
			try {
				Desktop.getDesktop().open(new File(fileUri));
			} catch (final IOException e) {
				log.log(Level.WARNING, "Error opening file: "+fileUri, e);
			}
		else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR))
			Desktop.getDesktop().browseFileDirectory(new File(fileUri));
		else
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK",
					NLS.bind(Messages.AnafReporterImpl_FileSaved, fileUri));
	}
}
