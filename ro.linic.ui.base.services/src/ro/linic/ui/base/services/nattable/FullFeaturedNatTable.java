package ro.linic.ui.base.services.nattable;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.core.runtime.ILog;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.blink.BlinkConfigAttributes;
import org.eclipse.nebula.widgets.nattable.blink.BlinkingCellResolver;
import org.eclipse.nebula.widgets.nattable.blink.IBlinkingCellResolver;
import org.eclipse.nebula.widgets.nattable.columnChooser.command.DisplayColumnChooserCommandHandler;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.e4.selection.E4SelectionListener;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultSummaryRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionUtils;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.summaryrow.FixedSummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.summaryrow.SummaryRowLayer;
import org.eclipse.nebula.widgets.nattable.ui.menu.HeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.widgets.Composite;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ro.linic.ui.base.services.Messages;
import ro.linic.ui.base.services.nattable.components.FullFeaturedContentTooltip;
import ro.linic.ui.base.services.nattable.internal.BodyMenuConfiguration;
import ro.linic.ui.base.services.nattable.internal.CustomGeneralConfiguration;
import ro.linic.ui.base.services.nattable.internal.DefaultRowIdAccessor;
import ro.linic.ui.base.services.nattable.internal.DiscardDataChangesDelegateCommandHandler;
import ro.linic.ui.base.services.nattable.internal.FluentTableConfigurer;
import ro.linic.ui.base.services.nattable.internal.FullFeaturedBodyLayerStack;
import ro.linic.ui.base.services.nattable.internal.FullFeaturedColumnHeaderLayerStack;
import ro.linic.ui.base.services.nattable.internal.LinicThemeConfiguration;
import ro.linic.ui.base.services.nattable.internal.MDirtyableUpdateDataChangeHandler;
import ro.linic.ui.base.services.nattable.internal.SaveDataChangesDelegateCommandHandler;

public class FullFeaturedNatTable<T> {
	private static ILog log = ILog.of(FullFeaturedNatTable.class);
	
	private static final String BLINK_CONFIG_LABEL = "blinkConfigLabel";
	
	public static final String BODY_DATA_PROVIDER_CONFIG_KEY = "bodyDataProvider"; //$NON-NLS-1$
	public static final String CONFIG_REGISTRY_CONFIG_KEY = "configRegistry"; //$NON-NLS-1$

	private PropertyChangeListener propertyChangeListener;
	private ListDataProvider<T> bodyDataProvider;
	private SelectionLayer selectionLayer;
	private NatTable natTable;
	private FilterList<T> filterList;
	
	final private FluentTableConfigurer<T> configurer;
	
	public FullFeaturedNatTable(final FluentTableConfigurer<T> configurer) {
		this.configurer = configurer;
	}

	public void postConstruct(final Composite parent) {
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final ColumnGroupModel columnGroupModel = new ColumnGroupModel();

		// Body
		ObservableElementList<T> observableElementList;
		SortedList<T> sortedList;
		FilterList<T> filteredHeaderData;
		configurer.getSourceData().getReadWriteLock().readLock().lock();
		try {
			observableElementList = new ObservableElementList<>(
					configurer.getSourceData(), GlazedLists.beanConnector(configurer.getModelClass()));
			filteredHeaderData = new FilterList<>(observableElementList);
			filterList = new FilterList<>(filteredHeaderData);
			sortedList = new SortedList<>(filterList, null);
		} finally {
			configurer.getSourceData().getReadWriteLock().readLock().unlock();
		}

		final FullFeaturedBodyLayerStack<T> bodyLayer = new FullFeaturedBodyLayerStack<>(configurer,
				sortedList, new DefaultRowIdAccessor<>(), configRegistry, columnGroupModel);

		this.bodyDataProvider = bodyLayer.getBodyDataProvider();
		this.propertyChangeListener = bodyLayer.getGlazedListEventsLayer();
		this.selectionLayer = bodyLayer.getSelectionLayer();
		
		if (configurer.getSelectionService() != null) {
			// create a E4SelectionListener and configure it for providing selection
			// on cell selection
			final E4SelectionListener<T> esl = new E4SelectionListener<>(configurer.getSelectionService(), selectionLayer,
					bodyDataProvider);
			esl.setFullySelectedRowsOnly(false);
			esl.setHandleSameRowSelection(false);
			selectionLayer.addLayerListener(esl);
		}
		
		// update MDirtyable dirty property
		if (configurer.getDirtyable() != null) {
			// update dirty state on cell value change
			bodyLayer.getDataChangeLayer().addLayerListener(new MDirtyableUpdateDataChangeHandler(
					bodyLayer.getDataChangeLayer(), configurer.getDirtyable()));
			// update dirty state on discard data changes command
			bodyLayer.getDataChangeLayer().registerCommandHandler(new DiscardDataChangesDelegateCommandHandler(
					bodyLayer.getDataChangeLayer(), configurer.getDirtyable()));
		}
		// 1. save changes to database
		// 2. update dirty state on save data changes command
		bodyLayer.getDataChangeLayer().registerCommandHandler(new SaveDataChangesDelegateCommandHandler<>(
				bodyLayer.getColumnPropertyAccessor(), bodyLayer.getDataChangeLayer(), configurer));

		// blinking
		registerBlinkingConfigCells(configRegistry);

		// Column header
		final FullFeaturedColumnHeaderLayerStack<T> columnHeaderLayer = new FullFeaturedColumnHeaderLayerStack<>(
				sortedList, filteredHeaderData, configurer.getColumns(), bodyLayer, selectionLayer,
				columnGroupModel, configRegistry);

		// Row header
		final DefaultRowHeaderDataProvider rowHeaderDataProvider = new DefaultSummaryRowHeaderDataProvider(
				this.bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		rowHeaderDataLayer.setDefaultColumnWidth(60);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayer, selectionLayer);

		// Corner
		final DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(
				columnHeaderLayer.getColumnHeaderDataProvider(), rowHeaderDataProvider);
		final DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		final ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);

		// Grid
		final GridLayer gridLayer = new GridLayer(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer);
		final ILayer underlyingLayer = createUnderlyingLayer(bodyLayer, gridLayer, configRegistry);

		this.natTable = new NatTable(parent, underlyingLayer, false);
		this.natTable.setConfigRegistry(configRegistry);
		this.natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
		// Popup menu
		this.natTable.addConfiguration(new HeaderMenuConfiguration(this.natTable) {
			@Override
			protected PopupMenuBuilder createColumnHeaderMenu(final NatTable natTable) {
				return super.createColumnHeaderMenu(natTable).withColumnChooserMenuItem();
			}
		});
		this.natTable.addConfiguration(new BodyMenuConfiguration(this.natTable));
		this.natTable.addConfiguration(new SingleClickSortConfiguration());
		this.natTable.addConfiguration(new CustomGeneralConfiguration<>(configurer.getColumns(), bodyDataProvider, configurer.getClickConsumers()));
		new FullFeaturedContentTooltip(this.natTable, configurer.getColumns());

		// Column chooser
		final DisplayColumnChooserCommandHandler columnChooserCommandHandler = new DisplayColumnChooserCommandHandler(
				selectionLayer, bodyLayer.getColumnHideShowLayer(),
				columnHeaderLayer.getColumnHeaderLayer(), columnHeaderLayer.getColumnHeaderDataLayer(),
				columnHeaderLayer.getColumnGroupHeaderLayer(), columnGroupModel);
		bodyLayer.registerCommandHandler(columnChooserCommandHandler);
		
		// hide columns
		bodyLayer.getColumnHideShowLayer().hideColumnIndexes(configurer.getColumns().stream()
				.filter(Column::hidden)
				.map(Column::index)
				.toList());

		// Extra configuration
		for (final Entry<String, List<Function<Object, IConfiguration>>> entry : configurer.getDynamicConfigs().entrySet()) {
			final Object argument = switch (entry.getKey()) {
			case BODY_DATA_PROVIDER_CONFIG_KEY -> bodyDataProvider;
			case CONFIG_REGISTRY_CONFIG_KEY -> configRegistry;
			default -> null;
			};
			
			entry.getValue().stream()
			.map(f -> f.apply(argument))
			.filter(Objects::nonNull)
			.forEach(this.natTable::addConfiguration);
		}
		
		this.natTable.configure();
		this.natTable.setTheme(new LinicThemeConfiguration());
	}

	private ILayer createUnderlyingLayer(final FullFeaturedBodyLayerStack<T> bodyLayer, final GridLayer gridLayer,
			final ConfigRegistry configRegistry) {
		if (configurer.getSummaryConfig() == null)
			return gridLayer;
		
		// Summary
		final FixedSummaryRowLayer summaryRowLayer = new FixedSummaryRowLayer(bodyLayer.getBodyDataLayer(), gridLayer, configRegistry, false);
		summaryRowLayer.addConfiguration(configurer.getSummaryConfig());
		summaryRowLayer.setSummaryRowLabel(Messages.Summary);
		try {
			final Field field = SummaryRowLayer.class.getDeclaredField("summaryRowHeight");
			field.setAccessible(true);
			field.set(summaryRowLayer, 30);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		final CompositeLayer composite = new CompositeLayer(1, 2);
		composite.setChildLayer("GRID", gridLayer, 0, 0);
		composite.setChildLayer("SUMMARY", summaryRowLayer, 0, 1);
		return composite;
	}

	private void registerBlinkingConfigCells(final ConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(BlinkConfigAttributes.BLINK_RESOLVER, getBlinkResolver(),
				DisplayMode.NORMAL);

		// Styles
		final Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL,
				BLINK_CONFIG_LABEL);
	}

	private IBlinkingCellResolver getBlinkResolver() {
		return new BlinkingCellResolver() {
			private final String[] configLabels = new String[1];
			@Override
			public String[] resolve(final Object oldValue, final Object newValue) {
				if (!Objects.equals(oldValue, newValue))
					this.configLabels[0] = BLINK_CONFIG_LABEL;
				return this.configLabels;
			}
		};
	}
	
	public NatTable natTable() {
		return natTable;
	}
	
	public FilterList<T> filterList() {
		return filterList;
	}
	
	public List<T> selection() {
		return SelectionUtils.getSelectedRowObjects(this.selectionLayer, this.bodyDataProvider, false);
	}
	
	public SelectionLayer selectionLayer() {
		return this.selectionLayer;
	}
	
	public ListDataProvider<T> bodyDataProvider() {
		return this.bodyDataProvider;
	}
}
