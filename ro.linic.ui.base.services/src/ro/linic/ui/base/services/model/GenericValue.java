package ro.linic.ui.base.services.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ro.flexbiz.util.commons.HeterogeneousDataComparator;
import ro.flexbiz.util.commons.ListUtils;
import ro.linic.ui.base.services.nattable.components.IdSupplier;

public class GenericValue extends JavaBean implements Map<String, Object>, Comparable<Object>, IdSupplier {
	private final String entityName;
	private final String primaryKey;
	protected final HashMap<String, Object> valueMapInternal;

	private transient boolean modified = false;
	
	public static GenericValue of(final String entityName, final String primaryKey) {
		return new GenericValue(entityName, primaryKey);
	}
	
	public static GenericValue of(final String entityName, final String primaryKey, final String key, final Object value) {
		final GenericValue gv = new GenericValue(entityName, primaryKey);
		gv.put(key, value);
		return gv;
	}
	
	public static GenericValue of(final String entityName, final String primaryKey, final String key1, final Object value1,
			final String key2, final Object value2) {
		final GenericValue gv = new GenericValue(entityName, primaryKey);
		gv.put(key1, value1);
		gv.put(key2, value2);
		return gv;
	}
	
	public static GenericValue of(final String entityName, final String primaryKey, final String key1, final Object value1,
			final String key2, final Object value2, final String key3, final Object value3) {
		final GenericValue gv = new GenericValue(entityName, primaryKey);
		gv.put(key1, value1);
		gv.put(key2, value2);
		gv.put(key3, value3);
		return gv;
	}
	
	public static GenericValue of(final String entityName, final String primaryKey, final Map<? extends String, ? extends Object> map) {
		final GenericValue gv = new GenericValue(entityName, primaryKey);
		gv.putAll(map);
		return gv;
	}

	public GenericValue(final String entityName, final String primaryKey) {
		this.entityName = Objects.requireNonNull(entityName);
		this.primaryKey = Objects.requireNonNull(primaryKey);
		this.valueMapInternal = new HashMap<String, Object>();
	}
	
	private GenericValue() {
		this.entityName = "deserialized";
		this.primaryKey = "id";
		this.valueMapInternal = new HashMap<String, Object>();
	}
	
	public boolean isModified() {
		return modified;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public int size() {
		return valueMapInternal.size();
	}

	@Override
	public boolean isEmpty() {
		return valueMapInternal.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		return valueMapInternal.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return values().contains(value);
	}

	@Override
	public Object get(final Object key) {
		return valueMapInternal.get(key);
	}
	
	public String getString(final Object key) {
		return Optional.ofNullable(get(key)).map(Object::toString).orElse(null);
	}
	
	public Integer getInt(final Object key) {
		return get(key) instanceof Integer ? (Integer) get(key) : Integer.parseInt(getString(key));
	}
	
	public Long getLong(final Object key) {
		return get(key) instanceof Long ? (Long) get(key) : Long.parseLong(getString(key));
	}
	
	public BigDecimal getBigDecimal(final Object key) {
		return (BigDecimal) get(key);
	}
	
	public Boolean getBoolean(final Object key) {
		return get(key) instanceof Boolean ? (Boolean) get(key) : Boolean.parseBoolean(getString(key));
	}

	@Override
	public Object put(final String key, final Object value) {
		final Object curValue = valueMapInternal.get(key);
		valueMapInternal.put(key, value);

		if (curValue == null) {
			if (value != null) {
				modified = true;
				firePropertyChange(key, curValue, value);
			}
		} else {
			if (!curValue.equals(value)) {
				modified = true;
				firePropertyChange(key, curValue, value);
			}
		}

		return curValue;
	}

	@Override
	public Object remove(final Object key) {
		if (key instanceof CharSequence) {
			final String name = key.toString();
			if (valueMapInternal.containsKey(name)) {
				modified = true;
				firePropertyChange(name, valueMapInternal.get(key), null);
			}
			return valueMapInternal.remove(name);
		} else {
			return null;
		}
	}

	@Override
	public void putAll(final Map<? extends String, ? extends Object> map) {
		for (final Entry entry : map.entrySet()) {
			final String key = (String) entry.getKey();
			if (key == null)
				continue;
			put(key, entry.getValue());
		}
	}

	@Override
	public void clear() {
		modified = true;
		valueMapInternal.forEach((k, v) -> firePropertyChange(k, v, null));
		valueMapInternal.clear();
	}

	@Override
	public Set<String> keySet() {
		return valueMapInternal.keySet();
	}

	@Override
	public Collection<Object> values() {
		return valueMapInternal.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return valueMapInternal.entrySet();
	}
	
	@Override
	public Serializable getId() {
		return entityName + ": " + get(primaryKey);
	}

	@Override
	public int hashCode() {
		return entityName.hashCode() + valueMapInternal.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return this.compareTo(obj) == 0;
	}

	@Override
	public String toString() {
		return "[" + entityName + ": " + valueMapInternal.toString() + "]";
	}
	
	@Override
    public int compareTo(final Object t) {
        if (t == null)
        	return -1;
        if (!t.getClass().equals(GenericValue.class))
        	return -1;
        final GenericValue that = (GenericValue) t;

        // first entity names
        int result = entityName.compareTo(that.getEntityName());
        if (result != 0)
        	return result;

        // next compare all fields (will compare PK fields first, generally first in list)
        final Set<String> allFieldNames = new HashSet<String>();
        allFieldNames.addAll(this.valueMapInternal.keySet());
        allFieldNames.addAll(that.keySet());
        for (final String pkFieldName : allFieldNames) {
            result = compareFields(that, pkFieldName);
            if (result != 0)
            	return result;
        }

        // all the same, result should be 0
        return result;
    }
	
	private int compareFields(final GenericValue that, final String name) {
        final Comparable thisVal = (Comparable) this.valueMapInternal.get(name);
        final Comparable thatVal = (Comparable) that.get(name);
        // NOTE: nulls go earlier in the list
        if (thisVal == null) {
            return thatVal == null ? 0 : 1;
        } else {
            return (thatVal == null ? -1 : HeterogeneousDataComparator.INSTANCE.compare(thisVal, thatVal));
        }
    }

    public boolean mapMatches(final Map<String, Object> theMap) {
        boolean matches = true;
        for (final Entry<String, Object> entry : theMap.entrySet()) {
            if (!entry.getValue().equals(this.valueMapInternal.get(entry.getKey()))) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    /**
     * Only the keys that are found in <code>targetToCloneKey</code> will be cloned, 
	 * if the map is empty or null clones all keys.
	 * 
     * @param targetToCloneKey maps the key in the target data with this key in the cloned data
     */
	public GenericValue clone(final Map<String, String> targetToCloneKey) {
		if (targetToCloneKey == null || targetToCloneKey.isEmpty())
			return GenericValue.of(entityName, primaryKey, valueMapInternal);
		else
			return GenericValue.of(entityName, primaryKey,
					targetToCloneKey.entrySet().stream().collect(ListUtils.toMapOfNullables(Entry::getValue, e -> get(e.getKey()))));
	}
	
	/**
     * Only the keys that are found in <code>targetToSourceKey</code> will be updated, 
	 * if the map is empty or null updates all keys.
	 * 
     * @param targetToSourceKey maps the key in the target data with this key in the source data
     * @return this
     */
	public GenericValue update(final GenericValue target, final Map<String, String> targetToSourceKey) {
		if (targetToSourceKey == null || targetToSourceKey.isEmpty())
			putAll(target);
		else
			putAll(targetToSourceKey.entrySet().stream().collect(ListUtils.toMapOfNullables(Entry::getValue, e -> target.get(e.getKey()))));
		return this;
	}
}
