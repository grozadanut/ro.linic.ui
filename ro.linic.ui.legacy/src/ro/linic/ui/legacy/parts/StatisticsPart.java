package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.insertDate;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.wrappers.LastYearStats;
import ro.colibri.wrappers.ProductProfitability;
import ro.colibri.wrappers.RaionProfitability;
import ro.colibri.wrappers.SalesPerHours;
import ro.colibri.wrappers.SalesPerOperators;
import ro.linic.ui.base.dialogs.InfoDialog;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.service.components.JasperChartSerie;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.ProductProfitabilityNatTable;
import ro.linic.ui.legacy.tables.RaionProfitabilityNatTable;
import ro.linic.ui.legacy.widgets.NatComboWidget;

public class StatisticsPart
{
	public static final String PART_ID = "linic_gest_client.part.statistics"; //$NON-NLS-1$
	
	private static final String RAION_TABLE_STATE_PREFIX = "statistics.raion_profitability_nt"; //$NON-NLS-1$
	private static final String TABLE_STATE_PREFIX = "statistics.product_profitability_nt"; //$NON-NLS-1$
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "statistics.horizontal_sash"; //$NON-NLS-1$
	
	private NatComboWidget docTypes;
	private Combo gestiune;
	private Button maxim;
	private Button ziCurenta;
	private Button lunaCurenta;
	private Button anCurent;
	private DateTime from;
	private DateTime to;
	private Button execute;
	private RaionProfitabilityNatTable raionProfTable;
	private ProductProfitabilityNatTable productProfTable;
	private SashForm horizontalSash;
	
	private Button salesPerHours;
	private Button salesPerOperators;
	private Button lastYearStats;
	private Button pointsPerCar;
	
	private ImmutableList<Gestiune> allGestiuni;
	
	private Job loadJob;
	private ImmutableMap<RaionProfitability, List<ProductProfitability>> loadedProfitability = ImmutableMap.of();
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		this.allGestiuni = BusinessDelegate.allGestiuni();
		
		parent.setLayout(new GridLayout());
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(7, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		new Label(container, SWT.NONE);//layout
		new Label(container, SWT.NONE);//layout
		
		maxim = new Button(container, SWT.PUSH);
		maxim.setText(Messages.Max);
		maxim.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(maxim);
		GridDataFactory.swtDefaults().applyTo(maxim);
		
		ziCurenta = new Button(container, SWT.PUSH);
		ziCurenta.setText(Messages.Today);
		ziCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(ziCurenta);
		GridDataFactory.swtDefaults().applyTo(ziCurenta);
		
		lunaCurenta = new Button(container, SWT.PUSH);
		lunaCurenta.setText(Messages.ThisMonth);
		lunaCurenta.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(lunaCurenta);
		GridDataFactory.swtDefaults().applyTo(lunaCurenta);
		
		anCurent = new Button(container, SWT.PUSH);
		anCurent.setText(Messages.ThisYear);
		anCurent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		UIUtils.setBoldFont(anCurent);
		GridDataFactory.swtDefaults().applyTo(anCurent);
		
		new Label(container, SWT.NONE);//layout
		
		docTypes = new NatComboWidget(container, SWT.BORDER | SWT.MULTI | SWT.CHECK);
		docTypes.setItems(AccountingDocument.ALL_IESIRI_OPERATION_DOC_TYPES.toArray(String[]::new));
		docTypes.setSelection(new String[] {AccountingDocument.FACTURA_NAME, AccountingDocument.BON_CASA_NAME});
		UIUtils.setFont(docTypes);
		
		gestiune = new Combo(container, SWT.DROP_DOWN);
		gestiune.setItems(allGestiuni.stream().map(Gestiune::displayName).toArray(String[]::new));
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
		execute.setText(Messages.Execute);
		execute.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		execute.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(execute);
		
		final Composite tableContainer = new Composite(container, SWT.NONE);
		tableContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().span(7, 1).grab(true, true).applyTo(tableContainer);
		
		horizontalSash = new SashForm(tableContainer, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(horizontalSash);

		raionProfTable = new RaionProfitabilityNatTable();
		raionProfTable.postConstruct(horizontalSash);
		raionProfTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(raionProfTable.getTable());
		loadState(RAION_TABLE_STATE_PREFIX, raionProfTable.getTable(), part);
		
		productProfTable = new ProductProfitabilityNatTable();
		productProfTable.postConstruct(horizontalSash);
		productProfTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(productProfTable.getTable());
		loadState(TABLE_STATE_PREFIX, productProfTable.getTable(), part);
		
		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".0", "180")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".1", "220")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalSash.setWeights(verticalWeights);
		
		createButtonsArea(container);
		
		addListeners();
		loadData();
	}
	
	private void createButtonsArea(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false).span(7, 1).applyTo(container);
		
		salesPerHours = new Button(container, SWT.PUSH | SWT.WRAP);
		salesPerHours.setText(Messages.StatisticsPart_HourSales);
		salesPerHours.setToolTipText(SalesPerHours.hint());
		salesPerHours.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		salesPerHours.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(salesPerHours);
		
		salesPerOperators = new Button(container, SWT.PUSH | SWT.WRAP);
		salesPerOperators.setText(Messages.StatisticsPart_UserSales);
		salesPerOperators.setToolTipText(SalesPerOperators.hint());
		salesPerOperators.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		salesPerOperators.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(salesPerOperators);
		
		lastYearStats = new Button(container, SWT.PUSH | SWT.WRAP);
		lastYearStats.setText(Messages.StatisticsPart_ShowLastYearStats);
		lastYearStats.setToolTipText(LastYearStats.hint());
		lastYearStats.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		lastYearStats.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(lastYearStats);
		
		pointsPerCar = new Button(container, SWT.PUSH | SWT.WRAP);
		pointsPerCar.setText(Messages.StatisticsPart_ShowCarPoints);
		pointsPerCar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		pointsPerCar.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldBannerFont(pointsPerCar);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(RAION_TABLE_STATE_PREFIX, raionProfTable.getTable(), part);
		saveState(TABLE_STATE_PREFIX, productProfTable.getTable(), part);
		
		final int[] verticalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".0", String.valueOf(verticalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX+".1", String.valueOf(verticalWeights[1])); //$NON-NLS-1$
	}
	
	@PreDestroy
	public void preDestroy()
	{
		cancelLoadJob();
	}
	
	public void cancelLoadJob()
	{
		if (loadJob != null)
			loadJob.cancel();
		if (!execute.isDisposed())
			execute.setEnabled(true);
		if (!lastYearStats.isDisposed())
			lastYearStats.setEnabled(true);
		if (!salesPerHours.isDisposed())
			salesPerHours.setEnabled(true);
		if (!salesPerOperators.isDisposed())
			salesPerOperators.setEnabled(true);
		if (!pointsPerCar.isDisposed())
			pointsPerCar.setEnabled(true);
	}
	
	private void addListeners()
	{
		execute.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData();
			}
		});
		
		raionProfTable.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override public void selectionChanged(final SelectionChangedEvent event)
			{
				final List<RaionProfitability> selection = event.getStructuredSelection().toList();
				productProfTable.loadData(selection.stream()
						.flatMap(rp -> loadedProfitability.get(rp).stream())
						.collect(toImmutableList()));
			}
		});
		
		salesPerHours.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printSalesPerHours();
			}
		});
		
		salesPerOperators.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printSalesPerOperators();
			}
		});
		
		lastYearStats.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printLastYearStats();
			}
		});
		
		pointsPerCar.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				printPointsPerCar();
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
	}
	
	private void loadData()
	{
		cancelLoadJob();
		execute.setEnabled(false);

		loadJob = BusinessDelegate.profitability(new AsyncLoadResult<ImmutableMap<RaionProfitability, List<ProductProfitability>>>()
		{
			@Override public void success(final ImmutableMap<RaionProfitability, List<ProductProfitability>> data)
			{
				execute.setEnabled(true);
				loadedProfitability = data;
				raionProfTable.loadData(loadedProfitability.keySet());
			}

			@Override public void error(final String details)
			{
				execute.setEnabled(true);
				MessageDialog.openError(execute.getShell(), Messages.ErrorFiltering, details);
			}
		}, sync, extractLocalDate(from), extractLocalDate(to), selectedGestiune().orElse(null), selectedDocTypes());
		
//		BusinessDelegate.productTurnover(new AsyncLoadResult<ImmutableList<ProductTurnover>>()
//		{
//			@Override public void success(final ImmutableList<ProductTurnover> data)
//			{
//				ConfirmDialog.open(Display.getCurrent().getActiveShell(), "Results", data.stream()
//						.map(pt -> MessageFormat.format("{0}", pt.displayName()))
//						.collect(Collectors.joining(PresentationUtils.NEWLINE)));
//			}
//
//			@Override public void error(final String details)
//			{
//				MessageDialog.openError(execute.getShell(), "Eroare la filtrare", details);
//			}
//		}, sync, extractLocalDate(from), extractLocalDate(to), selectedGestiune().orElse(null), selectedDocTypes());
	}
	
	private void printSalesPerHours()
	{
		cancelLoadJob();
		salesPerHours.setEnabled(false);
		
		final Gestiune gest = selectedGestiune().orElse(null);
		loadJob = BusinessDelegate.filteredOperations(new AsyncLoadData<Operatiune>()
		{
			@Override public void success(final ImmutableList<Operatiune> data)
			{
				salesPerHours.setEnabled(true);
				try
				{
					JasperReportManager.instance(bundle, log)
					.printBarchart(bundle, Messages.StatisticsPart_HourDaySales, gest, JasperChartSerie.fromVanzari(SalesPerHours.from(data)));
					JasperReportManager.instance(bundle, log)
					.printBarchart(bundle, Messages.StatisticsPart_OutgMinute, gest, JasperChartSerie.fromOpsPerMinute(SalesPerHours.from(data)));
				}
				catch (IOException | JRException e)
				{
					log.error(e);
					showException(e);
				}
			}

			@Override public void error(final String details)
			{
				salesPerHours.setEnabled(true);
				MessageDialog.openError(salesPerHours.getShell(), Messages.Error, details);
			}
		}, sync,
		TipOp.IESIRE, null, extractLocalDate(from), extractLocalDate(to),
				selectedDocTypes(), null, null, null, null, gest, null, null, 1000000, POSTGRES_MIN.toLocalDate(), POSTGRES_MAX.toLocalDate(), log);
	}
	
	private void printSalesPerOperators()
	{
		cancelLoadJob();
		salesPerOperators.setEnabled(false);
		
		final Gestiune gest = selectedGestiune().orElse(null);
		loadJob = BusinessDelegate.salesPerOperators(new AsyncLoadResult<SalesPerOperators>()
		{
			@Override public void success(final SalesPerOperators result)
			{
				salesPerOperators.setEnabled(true);
				try
				{
					JasperReportManager.instance(bundle, log)
					.printBarchart(bundle, Messages.StatisticsPart_OperatorsSales, gest, JasperChartSerie.fromVanzari(result));
					JasperReportManager.instance(bundle, log)
					.printBarchart(bundle, Messages.StatisticsPart_OutgMinute, gest, JasperChartSerie.fromOpsPerMinute(result));
				}
				catch (IOException | JRException e)
				{
					log.error(e);
					showException(e);
				}
			}
			
			@Override public void error(final String details)
			{
				salesPerOperators.setEnabled(true);
				MessageDialog.openError(salesPerOperators.getShell(), Messages.Error, details);
			}
		}, sync, gest, selectedDocTypes());
	}
	
	private void printLastYearStats()
	{
		cancelLoadJob();
		lastYearStats.setEnabled(false);
		
		final Gestiune gest = selectedGestiune().orElse(null);
		loadJob = BusinessDelegate.lastYearStats(new AsyncLoadResult<LastYearStats>()
		{
			@Override public void success(final LastYearStats result)
			{
				lastYearStats.setEnabled(true);
				try
				{
					JasperReportManager.instance(bundle, log)
					.printBarchart(bundle, Messages.StatisticsPart_LastYearStats, gest, JasperChartSerie.from(result));
				}
				catch (IOException | JRException e)
				{
					log.error(e);
					showException(e);
				}
			}
			
			@Override public void error(final String details)
			{
				lastYearStats.setEnabled(true);
				MessageDialog.openError(lastYearStats.getShell(), Messages.Error, details);
			}
		}, sync, gest);
	}
	
	private void printPointsPerCar()
	{
		cancelLoadJob();
		pointsPerCar.setEnabled(false);
		
		loadJob = BusinessDelegate.pointsPerCar(new AsyncLoadResult<String>()
		{
			@Override public void success(final String result)
			{
				pointsPerCar.setEnabled(true);
				try
				{
					InfoDialog.open(pointsPerCar.getShell(), Messages.StatisticsPart_CarPoints, result);
				}
				catch (final Exception e)
				{
					log.error(e);
					showException(e);
				}
			}
			
			@Override public void error(final String details)
			{
				pointsPerCar.setEnabled(true);
				MessageDialog.openError(pointsPerCar.getShell(), Messages.Error, details);
			}
		}, sync, extractLocalDate(from), extractLocalDate(to));
	}
	
	private Optional<Gestiune> selectedGestiune()
	{
		final int index = gestiune.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allGestiuni.get(index));
	}
	
	private ImmutableSet<String> selectedDocTypes()
	{
		return ImmutableSet.copyOf(docTypes.getSelection());
	}
}
