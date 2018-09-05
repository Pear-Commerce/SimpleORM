package com.ericdmartell.maga.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.utils.MAGAException;

public abstract class MAGAObject implements Serializable, Cloneable {
	public long id;
	
	public MAGAObject clone() {
		try {
			return (MAGAObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new MAGAException(e);
		}
	}
	
	public Map<MAGAAssociation, List<MAGAObject>> templateAssociations = null;


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
	
	
	
}
