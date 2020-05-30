package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.RepositoryReader;

public class StructFieldConstraints implements StructTypeConstraints {
	private final FunctionName fn;
	private final StructDefn sd;
	private final Map<String, UnifiableType> polys = new TreeMap<>();
	private final Map<StructField, UnifiableType> fields = new TreeMap<>(StructField.nameComparator);
	private final PolyInstance pi;

	public StructFieldConstraints(RepositoryReader repository, FunctionName fn, CurrentTCState state, InputPosition pos, StructDefn sd) {
		this.fn = fn;
		this.sd = sd;
		if (sd.hasPolys()) {
			List<Type> pvs = new ArrayList<>();
			for (PolyType pt : sd.polys()) {
				UnifiableType pv = state.createUT(pos, fn.uniqueName() + " " + sd.name().uniqueName() + "." + pt.shortName());
				pvs.add(pv);
				polys.put(pt.shortName(), pv);
			}
			this.pi = new PolyInstance(pos, sd, pvs);
		} else
			this.pi = null;
	}

	@Override
	public UnifiableType field(CurrentTCState state, InputPosition pos,  StructField fld) {
		if (!sd.fields.contains(fld))
			throw new RuntimeException("Field is not part of struct: " + fld);
		if (!fields.containsKey(fld)) {
			UnifiableType ut = state.createUT(pos, fn.uniqueName() + " " + sd.name().uniqueName() + "." + fld.name);
			fields.put(fld, ut);
			ut.sameAs(pos, TypeChecker.instantiateFreshPolys(null, state, new TreeMap<String, UnifiableType>(polys), new PosType(pos,fld.type())).type);
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

	public NamedType polyInstance() {
		if (pi != null)
			return pi;
		else
			return sd;
	}

}
