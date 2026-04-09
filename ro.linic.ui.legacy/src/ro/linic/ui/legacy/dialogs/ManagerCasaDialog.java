package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
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

import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.parts.ReconciliationPart;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.pos.base.services.ECRDriver.Result;
import ro.linic.ui.pos.base.services.ECRService;
import ro.linic.ui.security.services.AuthenticationSession;

public class ManagerCasaDialog extends Dialog
{
	private static final ILog log = ILog.of(ManagerCasaDialog.class);
	
	public static void reconcileReceipts(final IEclipseContext ctx, final LocalDate reportDate) {
		final CompletableFuture<Result> result = ctx.get(ECRService.class).readReceipts(reportDate.atStartOfDay(), reportDate.atTime(LocalTime.MAX));
		try
		{
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, new ReconcileResult(ctx, reportDate, result));
		}
		catch (final InvocationTargetException ex)
		{
			log.error(ex.getMessage(), ex);
			showException(ex);
		}
		catch (final InterruptedException ex)
		{
			log.error(ex.getMessage(), ex);
		}
	}
	
	private Button anulareBonFiscal;
	private Button raportX;
	private Button raportD;
	private Button raportZ;
	private Button raportMF;
	private Button reconcileReceipts;
	private DateTime raportMFPeriod;
	
	private IEclipseContext ctx;
	private ECRService ecrService;
	
	public ManagerCasaDialog(final Shell parent, final IEclipseContext ctx)
	{
		super(parent);
		this.ctx = ctx;
		this.ecrService = ctx.get(ECRService.class);
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		getShell().setText(Messages.ManagerCasaDialog_Title);
		
		anulareBonFiscal = new Button(contents, SWT.PUSH);
		anulareBonFiscal.setText(Messages.ManagerCasaDialog_CancelReceipt);
		anulareBonFiscal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		anulareBonFiscal.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		anulareBonFiscal.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(anulareBonFiscal);
		
		raportX = new Button(contents, SWT.PUSH);
		raportX.setText(Messages.ManagerCasaDialog_ReportX);
		raportX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportX.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportX.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportX);
		
		raportD = new Button(contents, SWT.PUSH);
		raportD.setText(Messages.ManagerCasaDialog_ReportD);
		raportD.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportD.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportD.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportD);
		
		raportZ = new Button(contents, SWT.PUSH);
		raportZ.setText(Messages.ManagerCasaDialog_ReportZ);
		raportZ.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportZ.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportZ.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportZ);
		
		raportMF = new Button(contents, SWT.PUSH);
		raportMF.setText(Messages.ManagerCasaDialog_ReportMF);
		raportMF.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		raportMF.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		raportMF.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(raportMF);
		
		reconcileReceipts = new Button(contents, SWT.PUSH);
		reconcileReceipts.setText(Messages.ManagerCasaDialog_ReconcileReceipts);
		reconcileReceipts.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		reconcileReceipts.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		reconcileReceipts.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(reconcileReceipts);
		
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
				ecrService.cancelReceipt();
				okPressed();
			}
		});
		
		raportX.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				ecrService.reportX();
				okPressed();
			}
		});
		
		raportD.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				ecrService.reportD();
				okPressed();
			}
		});
		
		raportZ.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				ecrService.reportZ();
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
					final LocalDateTime startDateTime = reportDate.withDayOfMonth(1).atStartOfDay();
					final LocalDateTime endDateTime = reportDate.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
					final CompletableFuture<Result> result = ecrService.reportMF(startDateTime, endDateTime, chosenDirectory);
					try
					{
						new ProgressMonitorDialog(getParentShell()).run(true, true, new ShowResult(result));
					}
					catch (final InvocationTargetException e1)
					{
						log.error(e1.getMessage(), e1);
						showException(e1);
					}
					catch (final InterruptedException e1)
					{
						log.error(e1.getMessage(), e1);
					}
					okPressed();
				}
			}
		});
		
		reconcileReceipts.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final LocalDate reportDate = extractLocalDate(raportMFPeriod);
				reconcileReceipts(ctx, reportDate);
				okPressed();
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
	
	private class ShowResult implements IRunnableWithProgress
	{
		private CompletableFuture<Result> result;
		
		public ShowResult(final CompletableFuture<Result> result)
		{
			this.result = result;
		}

		@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			final SubMonitor sub = SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN);
			sub.beginTask(Messages.ManagerCasaDialog_ExportMF, IProgressMonitor.UNKNOWN);

			Result resultCode;
			try {
				resultCode = result.get();
			} catch (InterruptedException | ExecutionException e) {
				log.warn(e.getMessage(), e);
				return;
			}
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override public void run()
				{
					if (resultCode.isOk())
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.ManagerCasaDialog_Success, Messages.ManagerCasaDialog_ReportExported);
					else
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerCasaDialog_Error, NLS.bind(Messages.ManagerCasaDialog_ErrorCode, resultCode.error()));
				}
			});
		}
	}
	
	private static class ReconcileResult implements IRunnableWithProgress
	{
		private IEclipseContext ctx;
		private CompletableFuture<Result> result;
		private LocalDate date;
		
		public ReconcileResult(final IEclipseContext ctx,  final LocalDate date, final CompletableFuture<Result> result)
		{
			this.ctx = ctx;
			this.date = date;
			this.result = result;
		}

		@Override public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			final SubMonitor sub = SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN);
			sub.beginTask(Messages.ManagerCasaDialog_ReconcileReceipts, IProgressMonitor.UNKNOWN);

			Result resultCode;
			try {
				resultCode = result.get();
			} catch (InterruptedException | ExecutionException e) {
				log.warn(e.getMessage(), e);
				return;
			}
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override public void run()
				{
					if (resultCode.isOk())
						reconcile(resultCode.info().substring(resultCode.info().indexOf("\n")+1));
					else
						MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ManagerCasaDialog_Error, NLS.bind(Messages.ManagerCasaDialog_ErrorCode, resultCode.error()));
				}
			});
		}
		
		private void reconcile(final String receipts) {
			final Map<String, Object> body = Map.of("gestiuneId", ClientSession.instance().getGestiune().getId(),
					"date", date,
					"receipts", receipts);

			RestCaller.post("/rest/s1/moqui-linic-legacy/reconcile/receipts").internal(ctx.get(AuthenticationSession.class).authentication())
					.body(BodyProvider.of(HttpUtils.toJSON(body)))
					.async(t -> UIUtils.showException(t, ctx.get(UISynchronize.class)))
					.thenAccept(result -> ctx.get(DataServices.class).holder(ReconciliationPart.DATA_HOLDER).setData(result.stream().toList()))
					.thenRun(() -> ctx.get(UISynchronize.class).asyncExec(() -> ctx.get(EPartService.class).showPart(ReconciliationPart.PART_ID, PartState.ACTIVATE)));
		}
	}
}
