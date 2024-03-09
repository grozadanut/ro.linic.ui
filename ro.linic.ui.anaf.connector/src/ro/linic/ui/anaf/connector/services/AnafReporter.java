package ro.linic.ui.anaf.connector.services;

import java.io.IOException;

public interface AnafReporter {
	void xmlToPdf(final String xml, final String outputFilename) throws IOException;
}
