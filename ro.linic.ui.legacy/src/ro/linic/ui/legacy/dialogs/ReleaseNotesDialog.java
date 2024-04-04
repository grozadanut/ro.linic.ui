package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.StringUtils.isEmpty;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ReleaseNotesDialog extends TitleAreaDialog
{
	public static final String RELEASE_FILEPATH_KEY = "release_filepath"; //$NON-NLS-1$
	
	private Logger log;
	
	public ReleaseNotesDialog(final Shell parent, final Logger log)
	{
		super(parent);
		this.log = log;
	}
	
	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle(Messages.ReleaseNotesDialog_Title);
		setMessage(Messages.ReleaseNotesDialog_Message);
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);
		try
		{
			final Browser browser = new Browser(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);
			final String releaseFilepath = System.getProperty(RELEASE_FILEPATH_KEY);
			
			if (!isEmpty(releaseFilepath))
				browser.setUrl(releaseFilepath.trim());
		}
		catch (final Exception e)
		{
			log.error(e);
		}
		return area;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(800, 600);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.SHEET | SWT.RESIZE;
	}
}
