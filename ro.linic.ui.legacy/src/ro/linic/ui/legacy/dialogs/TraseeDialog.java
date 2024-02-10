package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.LocalDateUtils.between;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.TransportType;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.Partner;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.DocumentNatTable;
import ro.linic.ui.legacy.tables.DocumentNatTable.SourceLoc;

public class TraseeDialog extends Dialog
{
	// MODEL
	private ImmutableList<AccountingDocument> allScheduledDocs = ImmutableList.of();
	private ImmutableList<Partner> allPartners;
	private ImmutableList<Masina> allCars;
	
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	
	private Combo partner;
	private Combo masina;
	private DateTime from;
	private DateTime to;
	
	private DocumentNatTable table;
	
	private Button printDocs;
	private Button printDocsComasat;
	
	private Job loadJob;
	private UISynchronize sync;
	private Logger log;
	private Bundle bundle;
	
	public TraseeDialog(final Shell parent, final UISynchronize sync, final Logger log, final Bundle bundle)
	{
		super(parent);
		this.sync = sync;
		this.log = log;
		this.bundle = bundle;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(5, false));
		getShell().setText("Trasee");
		
		final Label partnerLabel = new Label(contents, SWT.NONE);
		partnerLabel.setText("Partener");
		UIUtils.setFont(partnerLabel);
		
		partner = new Combo(contents, SWT.DROP_DOWN);
		UIUtils.setFont(partner);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(partner);
		
		final Label masinaLabel = new Label(contents, SWT.NONE);
		masinaLabel.setText("Masina");
		UIUtils.setFont(masinaLabel);
		GridDataFactory.fillDefaults().applyTo(masinaLabel);
		
		masina = new Combo(contents, SWT.DROP_DOWN);
		UIUtils.setFont(masina);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(masina);
		
		new Label(contents, SWT.NONE); //layout purpose
		
		maxim = new Button(contents, SWT.PUSH);
		maxim.setText("Maxim");
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(contents, SWT.PUSH);
		ziCurenta.setText("ZiCrt");
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(contents, SWT.PUSH);
		lunaCurenta.setText("LunaCrt");
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(contents, SWT.PUSH);
		anCurent.setText("AnCrt");
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		final Label fromToLabel = new Label(contents, SWT.NONE);
		fromToLabel.setText("De la pana la");
		UIUtils.setFont(fromToLabel);
		GridDataFactory.fillDefaults().applyTo(fromToLabel);
		
		from = new DateTime(contents, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		
		to = new DateTime(contents, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		
		table = new DocumentNatTable(SourceLoc.SCHEDULE_DIALOG, bundle, log);
		table.postConstruct(contents);
		GridDataFactory.fillDefaults().grab(true, true).span(5, 1).applyTo(table.getTable());
		
		final Composite bottomButtonsCont = new Composite(contents, SWT.NONE);
		bottomButtonsCont.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().span(5, 1).applyTo(bottomButtonsCont);
		
		printDocs = new Button(bottomButtonsCont, SWT.PUSH);
		printDocs.setText("Printeaza documentele selectate");
		UIUtils.setFont(printDocs);
		
		printDocsComasat = new Button(bottomButtonsCont, SWT.PUSH);
		printDocsComasat.setText("Printeaza documentele selectate comasate");
		UIUtils.setFont(printDocsComasat);
		
		addListeners();
		loadData();
		return contents;
	}
	
	private void addListeners()
	{
		partner.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				filterData();
			}
		});
		
		masina.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				filterData();
			}
		});
		
		from.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				filterData();
			}
		});
		
		to.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				filterData();
			}
		});
		
		maxim.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
				filterData();
			}
		});
		
		ziCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
				filterData();
			}
		});
		
		lunaCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
				filterData();
			}
		});
		
		anCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
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
		
		cancelJob();
		loadJob = BusinessDelegate.undeliveredDocs(new AsyncLoadData<AccountingDocument>()
		{
			@Override public void success(final ImmutableList<AccountingDocument> data)
			{
				allScheduledDocs = data;
				filterData();
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea documentelor", details);
			}
		}, sync, null, null);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(1000, 600);
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Printare", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void okPressed()
	{
		try
		{
			final ImmutableList<AccountingDocument> docsToPrint = table.getSourceList().stream()
					.filter(AccountingDocument.class::isInstance)
					.map(AccountingDocument.class::cast)
					.collect(toImmutableList());
			
			JasperReportManager.instance(bundle, log).printDocs(bundle, docsToPrint, true);
		}
		catch (IOException | JRException ex)
		{
			log.error(ex);
			showException(ex);
		}
		
		super.okPressed();
	}
	
	@Override
	public boolean close()
	{
		cancelJob();
		return super.close();
	}
	
	private void cancelJob()
	{
		if (loadJob != null)
			loadJob.cancel();
	}
	
	private void filterData()
	{
		Stream<AccountingDocument> filteredData = allScheduledDocs.stream()
				.filter(doc -> (doc.getTransportType() != null && !doc.getTransportType().equals(TransportType.SCHEDULED)) ||
						doc.getTransportDateTime() == null ||
				between(doc.getTransportDateTime().toLocalDate(), extractLocalDate(from), extractLocalDate(to)));
		
		final Optional<Partner> selPartner = partner();
		if (selPartner.isPresent())
			filteredData = filteredData.filter(doc -> Objects.equals(doc.getPartner(), selPartner.get()));
		
		final Optional<Masina> selMasina = masina();
		if (selMasina.isPresent())
			filteredData = filteredData.filter(doc -> Objects.equals(doc.getMasina(), selMasina.get()));
			
		table.loadData(filteredData.collect(toImmutableList()));
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
}
