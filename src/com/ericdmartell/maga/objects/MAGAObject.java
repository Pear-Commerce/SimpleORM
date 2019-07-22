package com.ericdmartell.maga.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gnu.trove.map.hash.THashMap;

public abstract class MAGAObject<T extends MAGAObject<T>> implements Serializable, Cloneable {

	public long id;

	public T clone() {
		try {
			return (T) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new MAGAException(e);
		}
	}

	@JsonIgnore
	public Map<MAGAAssociation, List<MAGAObject>> templateAssociations = null;

	private Map<String, Object> pristineIndexValues = new THashMap<>();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MAGAObject other = (MAGAObject) obj;
		if (id == 0) {
			if (other.id != 0)
				return false;
		} else if (id != other.id)
			return false;
		return true;
	}

	@JsonIgnore
	public Map<String, Object> getPristineCacheIndexValues() {
		return pristineIndexValues;
	}

	/**
	 * We want to keep around old versions of indexed values so we can properly dirty indexes when this object changes
	 */
	public void savePristineCacheIndexValues() {
		List<String> indexedFieldNames = ReflectionUtils.getCacheIndexedColumns(getClass());

		for (String indexedFieldName : indexedFieldNames) {
			pristineIndexValues.put(indexedFieldName, ReflectionUtils.getFieldValue(this, indexedFieldName));
		}
	}

}
