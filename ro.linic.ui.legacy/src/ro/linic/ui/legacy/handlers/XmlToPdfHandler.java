 
package ro.linic.ui.legacy.handlers;

import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;

import org.eclipse.e4.core.di.annotations.Execute;

import ro.linic.ui.legacy.anaf.AnafReporter;

public class XmlToPdfHandler
{
	@Execute
	public void execute()
	{
		try
		{
			AnafReporter.xmlFileToPdf();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			showException(e);
		}
	}
}