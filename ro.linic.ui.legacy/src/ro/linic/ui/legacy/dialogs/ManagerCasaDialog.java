package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.legacy.service.CasaMarcat;
import ro.linic.ui.legacy.session.UIUtils;

public class ManagerCasaDialog extends Dialog
{
	private Button anulareBonFiscal;
	private Button raportX;
	private Button raportD;
	private Button raportZ;
	private Button raportMF;
	private DateTime raportMFPeriod;
	
	private Logger log;
	
	public ManagerCasaDialog(final Shell parent, final Logger log)
	{
		super(parent);
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		getShell().setText("Manager Casa");
		
		anulareBonFiscal = new Button(contents, SWT.PUSH);
		anulareBonFiscal.setText("Anuleaza Bon Fiscal");
		anulareBonFiscal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		anulareBonFiscal.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		anulareBonFiscal.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(anulareBonFiscal);
		
		raportX = new Button(contents, SWT.PUSH);
		raportX.setText("raport X");
		raportX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportX.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportX.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportX);
		
		raportD = new Button(contents, SWT.PUSH);
		raportD.setText("raport D");
		raportD.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportD.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportD.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportD);
		
		raportZ = new Button(contents, SWT.PUSH);
		raportZ.setText("raport Z");
		raportZ.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportZ.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportZ.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportZ);
		
		raportMF = new Button(contents, SWT.PUSH);
		raportMF.setText("raport MF");
		raportMF.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportMF.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportMF.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportMF);
		
		raportMFPeriod = new DateTime(contents, SWT.CALENDAR | SWT.CALENDAR_WEEKNUMBERS);
		
		addListeners();
		return contents;
	}
	
	private void addListeners()
	{
		anulareBonFiscal.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CasaMarcat.instance(log).anulareBonFiscal();
				okPressed();
			}
		});
		
		raportX.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CasaMarcat.instance(log).raportX();
				okPressed();
			}
		});
		
		raportD.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CasaMarcat.instance(log).raportD();
				okPressed();
			}
		});
		
		raportZ.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				CasaMarcat.instance(log).raportZ();
				okPressed();
			}
		});
		
		raportMF.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final LocalDate reportDate = extractLocalDate(raportMFPeriod);
				final DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
				final String chosenDirectory = dialog.open();
				
				if (!isEmpty(chosenDirectory))
				{
					final Path resultPath = CasaMarcat.instance(log).raportMF(reportDate, chosenDirectory);
					try
					{
						new ProgressMonitorDialog(getParentShell()).run(true, true, new ShowResult(resultPath));
					}
					catch (final InvocationTargetException e1)
					{
						log.error(e1);
						showException(e1);
					}
					catch (final InterruptedException e1)
					{
						log.error(e1);
					}
					okPressed();
				}
			}
		});
	}

	@Override
	protected Control createButtonBar(final Composite parent)
	{
		buttonBar = super.createButtonBar(parent);
		final GridData gd = new GridData();
		gd.exclude = true;
		buttonBar.setLayoutData(gd);
		return buttonBar;
	}
	
	private static class ShowResult implements IRunnableWithProgress
	{
		private Path resultPath;
		
		public ShowResult(final Path resultPath)
		{
			this.resultPath = resultPath;
		}

		@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			if (resultPath == null)
			{
				monitor.done();
				return;
			}
			
			monitor.beginTask("Exporta Raport MF", IProgressMonitor.UNKNOWN);
			
			while (Files.notExists(resultPath))
			{
				if (monitor.isCanceled())
					throw new InterruptedException("Interrupted by user!");
				Thread.sleep(200);
			}
			
			try
			{
				Thread.sleep(200);
				try (Stream<String> resultLines = Files.lines(resultPath))
				{
					final String resultCode = resultLines.collect(Collectors.joining(LIST_SEPARATOR));
					Display.getDefault().asyncExec(new Runnable()
					{
						@Override public void run()
						{
							if (!resultCode.trim().startsWith("0:"))
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare", "Cod: "+resultCode);
							else
							{
								MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Succes", "Raportul a fost exportat!");
								try
								{
									Files.deleteIfExists(resultPath);
								}
								catch (final IOException e)
								{
									showException(e);
								}
							}
						}
					});
				}
				monitor.done();
			}
			catch (final IOException e)
			{
				throw new InvocationTargetException(e);
			}
		}
	}
}
