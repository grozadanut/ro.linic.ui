package ro.linic.ui.base.services.nattable.internal;

import static ro.linic.util.commons.PresentationUtils.EMPTY_STRING;
import static ro.linic.util.commons.PresentationUtils.safeString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.summaryrow.DefaultSummaryRowConfiguration;
import org.eclipse.swt.widgets.Composite;

import ca.odell.glazedlists.EventList;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder.TableConfigurer;

public class FluentTableConfigurer<T> implements TableConfigurer<T> {
	final private Class<T> entityClass;
	final private List<Column> columns;
	final private EventList<T> sourceData;
	final private Map<String, List<Function<Object, IConfiguration>>> dynamicConfigs = new HashMap<>();
	private AbstractRegistryConfiguration summaryConfig;
	private ESelectionService selectionService;
	
	public FluentTableConfigurer(final Class<T> entityClass, final List<Column> columns, final EventList<T> sourceData) {
		this.entityClass = Objects.requireNonNull(entityClass);
		this.columns = Objects.requireNonNull(columns);
		this.sourceData = Objects.requireNonNull(sourceData);
	}
	
	@Override
	public FullFeaturedNatTable<T> build(final Composite parent) {
		final FullFeaturedNatTable<T> table = new FullFeaturedNatTable<T>(this);
		table.postConstruct(parent);
		return table;
	}

	@Override
	public TableConfigurer<T> addConfiguration(final IConfiguration config) {
		final List<Function<Object, IConfiguration>> value = dynamicConfigs.getOrDefault(EMPTY_STRING,
				new ArrayList<Function<Object, IConfiguration>>());
		value.add(nill -> config);
		dynamicConfigs.put(EMPTY_STRING, value);
		return this;
	}

	@Override
	public TableConfigurer<T> addConfiguration(final String key, final Function<Object, IConfiguration> configSupplier) {
		final List<Function<Object, IConfiguration>> value = dynamicConfigs.getOrDefault(safeString(key),
				new ArrayList<Function<Object, IConfiguration>>());
		value.add(configSupplier);
		dynamicConfigs.put(safeString(key), value);
		return this;
	}

	@Override
	public TableConfigurer<T> withSummaryRow() {
		return withSummaryRow(new DefaultSummaryRowConfiguration());
	}
	
	@Override
	public TableConfigurer<T> withSummaryRow(final AbstractRegistryConfiguration config) {
		this.summaryConfig = config;
		return this;
	}
	
	@Override
	public TableConfigurer<T> provideSelection(final ESelectionService service) {
		this.selectionService = service;
		return this;
	}

	public Class<T> getEntityClass() {
		return entityClass;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public EventList<T> getSourceData() {
		return sourceData;
	}
	
	public Map<String, List<Function<Object, IConfiguration>>> getDynamicConfigs() {
		return dynamicConfigs;
	}
	
	public AbstractRegistryConfiguration getSummaryConfig() {
		return summaryConfig;
	}
	
	public ESelectionService getSelectionService() {
		return selectionService;
	}
}
