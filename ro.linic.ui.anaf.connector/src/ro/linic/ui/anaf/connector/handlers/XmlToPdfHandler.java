 
package ro.linic.ui.anaf.connector.handlers;

import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import ro.linic.ui.anaf.connector.services.AnafReporter;


public class XmlToPdfHandler {
	@Execute
	public void execute(final AnafReporter reporter) throws IOException {
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.xml;*.xmls"});
		final String firstFileUri = dialog.open();
		
		if (isEmpty(firstFileUri))
			return;
		
		final String parentDirectory = dialog.getFilterPath();
		final String[] selectedFilePaths = dialog.getFileNames();
		
		for (final String inputFileName : selectedFilePaths) {
			final Path inputFileAbsolutePath = Path.of(parentDirectory, inputFileName);
			final String outputFileUri = FilenameUtils.removeExtension(inputFileAbsolutePath.toString()) + ".pdf";
			final String invoiceXml = Files.readString(inputFileAbsolutePath);
			reporter.xmlToPdf(invoiceXml, outputFileUri);
			
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
	}
}