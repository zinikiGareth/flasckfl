package org.flasck.flas.tc3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.RepositoryReader;

public class StructFieldConstraints implements StructTypeConstraints {
	private final RepositoryReader repository;
	private final StructDefn sd;
	private final Map<StructField, UnifiableType> fields = new TreeMap<>(StructField.nameComparator);

	public StructFieldConstraints(RepositoryReader repository, StructDefn sd) {
		this.repository = repository;
		this.sd = sd;
	}

	@Override
	public UnifiableType field(CurrentTCState state, InputPosition pos, StructField fld) {
		if (!sd.fields.contains(fld))
			throw new RuntimeException("Field is not part of struct: " + fld);
		if (!fields.containsKey(fld))
			fields.put(fld, new TypeConstraintSet(repository, state, pos, "field_" + fld.name));
		return fields.get(fld);
	}

	@Override
	public Set<StructField> fields() {
		return fields.keySet();
	}

	@Override
	public UnifiableType get(StructField f) {
		return fields.get(f);
	}

}