package ro.linic.ui.base.services;

import java.util.List;
import java.util.Map;

import ro.linic.ui.base.services.model.GenericValue;

public interface GenericDataHolder extends DataHolder<GenericValue> {
	/**
	 * Adds the target data values to this holder, matching by <code>primaryKey</code>. 
	 * If GenericValues already exist with the <code>primaryKey</code>, they will be updated. 
	 * Only the keys that are found in <code>targetToHolderKey</code> will be taken from <code>data</code>, 
	 * if the map is empty or null takes all keys from target and maps them to the same name in source.
	 * <br/><br/>
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 * 
	 * @param targetData target data to add to this holder
	 * @param targetPrimaryKey key by which it will match the target data
	 * @param sourcePrimaryKey key by which it will match the source data
	 * @param targetToHolderKey maps the key in the target data with this key in the holder data
	 * @param addMissing if true and the source doesn't have GenericValues with the <code>primaryKey</code>, it will create new ones; if false only updates existing values
	 */
	GenericDataHolder update(List<GenericValue> targetData, String targetPrimaryKey, String sourcePrimaryKey, Map<String, String> targetToHolderKey,
			boolean addMissing);
	/**
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 */
	default GenericDataHolder update(final List<GenericValue> targetData, final String primaryKey) {
		return update(targetData, primaryKey, primaryKey, null, true);
	}
	/**
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 */
	default GenericDataHolder update(final List<GenericValue> targetData, final String primaryKey, final boolean addMissing) {
		return update(targetData, primaryKey, primaryKey, null, addMissing);
	}
	/**
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 */
	default GenericDataHolder update(final List<GenericValue> targetData, final String primaryKey, final Map<String, String> targetToHolderKey) {
		return update(targetData, primaryKey, primaryKey, targetToHolderKey, true);
	}
	/**
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 */
	default GenericDataHolder update(final List<GenericValue> targetData, final String targetPrimaryKey, final String sourcePrimaryKey) {
		return update(targetData, targetPrimaryKey, sourcePrimaryKey, null, true);
	}
	/**
	 * <b>NOTE</b>: Due to speed considerations you have to manually refresh the table after this method, by calling <code>table.natTable().refresh();</code>
	 */
	default GenericDataHolder update(final List<GenericValue> targetData, final String targetPrimaryKey, final String sourcePrimaryKey,
			final Map<String, String> targetToHolderKey) {
		return update(targetData, targetPrimaryKey, sourcePrimaryKey, targetToHolderKey, true);
	}
}
