package ro.linic.ui.base.services.nattable;

import static ro.flexbiz.util.commons.PresentationUtils.EMPTY_STRING;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.swt.widgets.Composite;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ro.linic.ui.base.services.nattable.internal.FluentTableConfigurer;

public interface TableBuilder {
	interface TableConfigurer<T> {
		TableConfigurer<T> addConfiguration(IConfiguration config);
		/**
		 * These are for clients that require some data, such as the bodyDataProvider 
		 * when configuring the extra layers. Register a known key here, such as 
		 * BODY_DATA_PROVIDER_CONFIG_KEY and the passed function will be called 
		 * when configuring NatTable with the required object passed(eg.: bodyDataProvider). 
		 * Client should then return the IConfiguration they want to apply to the nattable 
		 * or null to skip.<br>
		 * For invalid keys or empty keys null is passed to the function.
		 */
		TableConfigurer<T> addConfiguration(String key, Function<Object, IConfiguration> configSupplier);
		TableConfigurer<T> withSummaryRow(AbstractRegistryConfiguration config);
		/**
		 * @param config defaults to DefaultSummaryRowConfiguration
		 */
		TableConfigurer<T> withSummaryRow();
		TableConfigurer<T> provideSelection(ESelectionService service);
		/**
		 * Connects the dirty state of this table with the dirty property of the 
		 * dirtyable param. MDirtyable dirty is set to true when this table is dirty, 
		 * but it is also set to false when this table becomes undirty.
		 * 
		 * @param dirtyable MDirtyable object such as MPart
		 */
		TableConfigurer<T> connectDirtyProperty(MDirtyable dirtyable);
		/**
		 * Register a handler that will be called when changes should be 
		 * saved to the database.
		 * 
		 * @param handler should return true if the save has succeeded, false otherwise
		 */
		TableConfigurer<T> saveToDbHandler(Function<List<UpdateCommand>, Boolean> handler);
		FullFeaturedNatTable<T> build(Composite parent);
	}
	
	public static <T> TableConfigurer<T> with(final Class<T> modelClass, final List<Column> columns, final EventList<T> sourceData) {
        return new FluentTableConfigurer<>(modelClass, columns, sourceData);
    }
	
	public static <T> TableConfigurer<T> with(final Class<T> entityClass, final List<Column> columns) {
        return new FluentTableConfigurer<>(entityClass, columns, GlazedLists.threadSafeList(GlazedLists.eventList(new ArrayList<>())));
    }
	
	public static <T> TableConfigurer<T> empty(final Class<T> entityClass) {
        return new FluentTableConfigurer<>(entityClass, List.of(new Column(0, EMPTY_STRING, EMPTY_STRING, DataLayer.DEFAULT_COLUMN_WIDTH)),
        		GlazedLists.threadSafeList(GlazedLists.eventList(new ArrayList<>())));
    }
}
