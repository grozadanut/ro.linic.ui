package ro.linic.ui.base.services;

import java.util.List;
import java.util.Map;

import ro.linic.ui.base.services.model.GenericValue;

public interface GenericDataHolder extends DataHolder<GenericValue> {
	/**
	 * Adds the target data values to this holder, matching by <code>primaryKey</code>. 
	 * If GenericValues already exist with the <code>primaryKey</code>, they will be updated, if not, new GenericValues will be added. 
	 * Only the keys that are found in <code>targetToHolderKey</code> will be taken from <code>data</code>, 
	 * if the map is empty or null takes all keys from target and maps them to the same name in source.
	 * 
	 * @param targetData target data to add to this holder
	 * @param primaryKey key by which it will match the source and target data
	 * @param targetToHolderKey maps the key in the target data with this key in the holder data
	 */
	void addOrUpdate(List<GenericValue> targetData, String targetPrimaryKey, String sourcePrimaryKey, Map<String, String> targetToHolderKey);
	default void addOrUpdate(final List<GenericValue> targetData, final String primaryKey) {
		addOrUpdate(targetData, primaryKey, primaryKey, null);
	}
	default void addOrUpdate(final List<GenericValue> targetData, final String targetPrimaryKey, final String sourcePrimaryKey) {
		addOrUpdate(targetData, targetPrimaryKey, sourcePrimaryKey, null);
	}
}
