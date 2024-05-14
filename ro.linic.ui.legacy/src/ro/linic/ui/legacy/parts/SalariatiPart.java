package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableMap;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.setBannerFont;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ShowAllColumnsCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.CIM;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PontajZilnic.PontajDayType;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.PontajLine;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.CIMDialog;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.PontajTable;
import ro.linic.ui.legacy.tables.SalariiNatTable;

public class SalariatiPart
{
	public static final String PART_ID = "linic_gest_client.part.salariati";

	private static final String PONTAJ_TABLE_STATE_PREFIX = "salariati.pontaj_nt";
	private static final String SALARII_TABLE_STATE_PREFIX = "salariati.salarii_nt";
	
	private Combo gestiune;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button execute;
	private PontajTable table;
	private Label monthGestLabel;
	private Button cimButton;
	private Button printPontaj;
	
	private SalariiNatTable salariiTable;
	private Button genereazaTichete;
	private Button genereazaSalarii;
	private Button platesteSalarii;
	private Button printSalarii;
	private Button adaugaOp;
	private Button stergeOp;
	
	private LocalDate selMonth;
	private Gestiune selGestiune;
	
	private ImmutableList<Gestiune> allGestiuni;
	private Job loadJob;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		this.allGestiuni = BusinessDelegate.allGestiuni();
		
		parent.setLayout(new GridLayout());
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(6, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		new Label(container, SWT.NONE);//layout
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText("Maxim");
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText("ZiCrt");
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText("LunaCrt");
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText("AnCrt");
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		new Label(container, SWT.NONE);//layout
		
		gestiune = new Combo(container, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::displayName).toArray(String[]::new));
		gestiune.select(allGestiuni.indexOf(ClientSession.instance().getLoggedUser().getSelectedGestiune()));
		UIUtils.setFont(gestiune);
		
		from = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(from);
		insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(from);
		
		to = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(to);
		insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(to);
		
		execute = new Button(container, SWT.PUSH | SWT.WRAP);
		execute.setText("Executa filtrarea");
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);
		
		monthGestLabel = new Label(container, SWT.NONE);
		setBannerFont(monthGestLabel);
		GridDataFactory.fillDefaults().span(6, 1).applyTo(monthGestLabel);
		
		final Composite buttonsContainer = new Composite(container, SWT.NONE);
		buttonsContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().span(6, 1).grab(true, false).applyTo(buttonsContainer);
		
		cimButton = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		cimButton.setText("CIM");
		cimButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		cimButton.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(cimButton);
		GridDataFactory.swtDefaults().applyTo(cimButton);
		
		printPontaj = new Button(buttonsContainer, SWT.PUSH | SWT.WRAP);
		printPontaj.setText("Printare pontaj");
		printPontaj.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printPontaj.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(printPontaj);
		GridDataFactory.swtDefaults().applyTo(printPontaj);
		
		table = new PontajTable();
		table.afterChange(op -> part.setDirty(true));
		table.postConstruct(container);
		table.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().span(6, 1).grab(true, true).applyTo(table.getTable());
		loadState(PONTAJ_TABLE_STATE_PREFIX, table.getTable(), part);
		
		final Label hintLabel = new Label(container, SWT.NONE);
		hintLabel.setText(ImmutableList.copyOf(PontajDayType.values()).stream()
				.map(pdt -> MessageFormat.format("{0} - {1}", pdt.displayName(), pdt.getDescription()))
				.collect(Collectors.joining(LIST_SEPARATOR)));
		GridDataFactory.fillDefaults().span(6, 1).applyTo(hintLabel);
		
		final Composite bottomButtonsContainer = new Composite(container, SWT.NONE);
		bottomButtonsContainer.setLayout(new GridLayout(6, false));
		GridDataFactory.fillDefaults().span(6, 1).grab(true, false).applyTo(bottomButtonsContainer);
		
		genereazaTichete = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		genereazaTichete.setText("Genereaza Tichete de Masa");
		genereazaTichete.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		genereazaTichete.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(genereazaTichete);
		GridDataFactory.swtDefaults().applyTo(genereazaTichete);
		
		genereazaSalarii = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		genereazaSalarii.setText("Genereaza Salarii");
		genereazaSalarii.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		genereazaSalarii.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(genereazaSalarii);
		GridDataFactory.swtDefaults().applyTo(genereazaSalarii);
		
		platesteSalarii = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		platesteSalarii.setText("Plateste Salarii");
		platesteSalarii.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		platesteSalarii.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(platesteSalarii);
		GridDataFactory.swtDefaults().applyTo(platesteSalarii);
		
		printSalarii = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		printSalarii.setText("Printeaza Salarii");
		printSalarii.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printSalarii.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(printSalarii);
		GridDataFactory.fillDefaults().applyTo(printSalarii);
		
		adaugaOp = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		adaugaOp.setText("Adauga");
		adaugaOp.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		adaugaOp.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(adaugaOp);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.RIGHT, SWT.FILL).applyTo(adaugaOp);
		
		stergeOp = new Button(bottomButtonsContainer, SWT.PUSH | SWT.WRAP);
		stergeOp.setText("Sterge");
		stergeOp.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
		stergeOp.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(stergeOp);
		GridDataFactory.swtDefaults().applyTo(stergeOp);
		
		salariiTable = new SalariiNatTable(log);
		salariiTable.afterChange(op -> part.setDirty(true));
		salariiTable.postConstruct(container);
		salariiTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).span(6, 1).applyTo(salariiTable.getTable());
		loadState(SALARII_TABLE_STATE_PREFIX, salariiTable.getTable(), part);
		
		addListeners();
		loadData();
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(PONTAJ_TABLE_STATE_PREFIX, table.getTable(), part);
		saveState(SALARII_TABLE_STATE_PREFIX, salariiTable.getTable(), part);
	}
	
	private void addListeners()
	{
		execute.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				askSave();
				loadData();
			}
		});
		
		maxim.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, POSTGRES_MIN);
				insertDate(to, POSTGRES_MAX);
			}
		});
		
		ziCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now());
				insertDate(to, LocalDate.now());
			}
		});
		
		lunaCurenta.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
			}
		});
		
		anCurent.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				insertDate(from, LocalDate.now().with(TemporalAdjusters.firstDayOfYear()));
				insertDate(to, LocalDate.now().with(TemporalAdjusters.lastDayOfYear()));
			}
		});
		
		cimButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Optional<User> selUser = table.selection().stream()
						.map(PontajLine::getUser)
						.findFirst();
				if (selUser.isPresent())
				{
					final Optional<CIM> loadedCim = BusinessDelegate.cimForUser(selUser.get().getId());
					final CIMDialog cimDialog = new CIMDialog(bundle, log, Display.getCurrent().getActiveShell(),
							loadedCim.orElseGet(() ->
							{
								final CIM cim = new CIM();
								cim.setUser(selUser.get());
								return cim;
							}));
					cimDialog.setOkSupplier(() -> 
					{
						final CIM cim = cimDialog.filledCim();
						final InvocationResult result = BusinessDelegate.mergeCim(cim, selUser.get().getId());
						showResult(result);
						return result.statusOk();
					});
					cimDialog.open();
				}
			}
		});
		
		printPontaj.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log).printPontaj(bundle,
							table.getSourceData().stream()
							.filter(ol -> !ol.isPontajeEmpty())
							.collect(toImmutableList()), selMonth);
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});
		
		genereazaTichete.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Gestiune selGestiune = selectedGestiune().orElse(null);
				final LocalDate fromDate = extractLocalDate(from);
				if (MessageDialog.openQuestion(genereazaTichete.getShell(), "Genereaza Tichete Masa",
						"Doriti sa generati tichete de masa pentru angajati pentru luna "+displayLocalDate(fromDate, DateTimeFormatter.ofPattern("MM uuuu"))+"?"))
					salariiTable.loadData(BusinessDelegate.calculateTicheteMasa(selGestiune, fromDate));
			}
		});

		genereazaSalarii.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Gestiune selGestiune = selectedGestiune().orElse(null);
				final LocalDate fromDate = extractLocalDate(from);
				if (MessageDialog.openQuestion(genereazaSalarii.getShell(), "Genereaza Salarii",
						"Doriti sa generati salariile pentru angajati pentru luna "+displayLocalDate(fromDate, DateTimeFormatter.ofPattern("MM uuuu"))+"?"))
					salariiTable.loadData(BusinessDelegate.calculateSalarii(selGestiune, fromDate));
			}
		});

		platesteSalarii.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					final ImmutableList<AccountingDocument> accDocs = salariiTable.selectedAccDocs().isEmpty() ?
							salariiTable.sourceAccDocs() : salariiTable.selectedAccDocs();
							final ImmutableMap<Partner, AccountingDocument> payableDocsByPartner = accDocs.stream()
									.filter(accDoc -> greaterThan(accDoc.totalUnlinked(), BigDecimal.ZERO))
									.collect(toImmutableMap(AccountingDocument::getPartner, Function.identity()));
							final AtomicReference<BigDecimal> totalDePlata = new AtomicReference<>(payableDocsByPartner.values().stream()
									.map(AccountingDocument::totalUnlinked)
									.reduce(BigDecimal::add)
									.orElse(BigDecimal.ZERO));

							if (payableDocsByPartner.size() == 1)
							{
								final InputDialog inputDialog = new InputDialog(Display.getCurrent().getActiveShell(),
										"Plateste Salarii", "Doriti sa platiti salariile? Total de plata: ",
										safeString(totalDePlata.get(), BigDecimal::toString),
										newVal -> greaterThan(parse(newVal), totalDePlata.get()) ? "Valoarea maxima de plata este: "+totalDePlata.get() : null);
								
								if (inputDialog.open() == Window.OK)
									totalDePlata.set(parse(inputDialog.getValue()));
								else
									return;
							}
							else
							{
								if (!MessageDialog.openQuestion(platesteSalarii.getShell(), "Plateste Salarii",
										"Doriti sa platiti salariile? Total de plata: "+totalDePlata))
									return;
							}
							
							payableDocsByPartner.entrySet().stream()
							.map(entry -> BusinessDelegate.platesteDocs(ImmutableSet.of(entry.getValue().getId()),
									entry.getKey().getId(), payableDocsByPartner.size() == 1 ? totalDePlata.get() : entry.getValue().totalUnlinked(),
									false, null, AccountingDocument.MONETAR_NAME, entry.getValue().getNrDoc(), LocalDateTime.now(), true))
							.forEach(UIUtils::showResult);

							JasperReportManager.instance(bundle, log).printSalarii(bundle, payableDocsByPartner.values());
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});

		printSalarii.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					final ImmutableList<AccountingDocument> accDocs = salariiTable.selectedAccDocs().isEmpty() ?
							salariiTable.sourceAccDocs() : salariiTable.selectedAccDocs();
							JasperReportManager.instance(bundle, log)
							.printSalarii(bundle, accDocs.stream().filter(accDoc -> !accDoc.getOperatiuni().isEmpty()).collect(toImmutableList()));
				}
				catch (IOException | JRException ex)
				{
					log.error(ex);
					showException(ex);
				}
			}
		});

		adaugaOp.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final Gestiune selGestiune = selectedGestiune().orElse(null);
				salariiTable.selectedAccDoc().ifPresent(accDoc ->
				{
					final Operatiune op = Operatiune.createSalarExtraLine(ClientSession.instance().getLoggedUser());
					final InvocationResult result = BusinessDelegate.addOperationToDoc(accDoc.getId(), op, null);
					showResult(result);
					if (result.statusOk())
						salariiTable.loadData(BusinessDelegate.salariiForMonth(selGestiune, selMonth));
				});
			}
		});

		stergeOp.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final ImmutableList<Operatiune> selection = salariiTable.selectedOps();
				if (!selection.isEmpty() && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
						"Stergeti randuri?", 
						"Sunteti sigur ca doriti sa stergeti toate randurile("+selection.size()+") selectate?"))
					deleteOperations(selection);
			}
		});
	}
	
	private void loadData()
	{
		askSave();
		cancelLoadJob();
		execute.setEnabled(false);

		selMonth = extractLocalDate(from);
		selGestiune = selectedGestiune().orElse(null);
		monthGestLabel.setText(MessageFormat.format("Gest: {0}, Luna: {1}",
				safeString(selGestiune, Gestiune::displayName),
				displayLocalDate(selMonth, DateTimeFormatter.ofPattern("MM uuuu"))));
		
		loadJob = BusinessDelegate.pontaje(new AsyncLoadData<PontajLine>()
		{
			@Override public void success(final ImmutableList<PontajLine> data)
			{
				salariiTable.loadData(BusinessDelegate.salariiForMonth(selGestiune, selMonth));
				
				execute.setEnabled(true);
				table.loadData(data);
				
				table.getTable().doCommand(new ShowAllColumnsCommand());
				for (int i = 31; i > selMonth.lengthOfMonth(); i--)
					table.getTable().doCommand(new ColumnHideCommand(table.getSelectionLayer(), i));
			}

			@Override public void error(final String details)
			{
				execute.setEnabled(true);
				MessageDialog.openError(execute.getShell(), "Eroare la filtrare", details);
			}
		}, sync, selGestiune, selMonth);
	}
	
	private void deleteOperations(final ImmutableList<Operatiune> operations)
	{
		askSave();
		final InvocationResult result = BusinessDelegate
				.deleteOperations(operations.stream().map(Operatiune::getId).collect(toImmutableSet()));
		showResult(result);
		if (result.statusOk())
			salariiTable.loadData(BusinessDelegate.salariiForMonth(selGestiune, selMonth));
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			// Save pontaje
			final InvocationResult result = BusinessDelegate.mergePontaje(table.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<PontajLine>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.flatMap(line ->
					{
						return line.getPontajeZilnice().entrySet().stream()
						.map(entry -> 
						{
							entry.getValue().setGestiune(selGestiune);
							entry.getValue().setUser(line.getUser());
							entry.getValue().setDate(selMonth.withDayOfMonth(entry.getKey()));
							return entry.getValue();
						});
					})
					.collect(toImmutableSet()));
			showResult(result);
			
			// Save salarii
			BusinessDelegate.mergeOperations(salariiTable.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<Operatiune>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.collect(toImmutableSet()));

			part.setDirty(false);
			if (result.statusCanceled())
				part.setDirty(true);
			else
				loadData();
		}
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Salveaza", "Salvati modificarile facute?"))
			onSave();
	}
	
	public void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
		if (!execute.isDisposed())
			execute.setEnabled(true);
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
}
