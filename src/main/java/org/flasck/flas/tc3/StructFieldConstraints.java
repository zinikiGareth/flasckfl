package org.flasck.flas.tc3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.RepositoryReader;

public class StructFieldConstraints implements StructTypeConstraints {
	private final StructDefn sd;
	private final Map<String, UnifiableType> polys = new TreeMap<>();
	private final Map<StructField, UnifiableType> fields = new TreeMap<>(StructField.nameComparator);

	public StructFieldConstraints(RepositoryReader repository, CurrentTCState state, InputPosition pos, StructDefn sd) {
		this.sd = sd;
		if (sd.hasPolys()) 
			for (PolyType pt : sd.polys()) {
				polys.put(pt.shortName(), state.createUT(pos, "poly var " + pt.shortName()));
			}
	}

	@Override
	public UnifiableType field(CurrentTCState state, InputPosition pos, StructField fld) {
		if (!sd.fields.contains(fld))
			throw new RuntimeException("Field is not part of struct: " + fld);
		if (!fields.containsKey(fld)) {
			UnifiableType ut = state.createUT(pos, "field " + fld.name);
			fields.put(fld, ut);
			ut.sameAs(pos, TypeChecker.instantiateFreshPolys(state, new TreeMap<String, UnifiableType>(polys), new PosType(pos,fld.type())).type);
		}
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
