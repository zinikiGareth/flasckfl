package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.NotImplementedException;

public class ConsolidateTypes implements Type, Locatable {
	private final InputPosition loc;
	public final List<UnifiableType> uts = new ArrayList<>();
	public final List<Type> types = new ArrayList<Type>();
	private Type result;

	public ConsolidateTypes(InputPosition loc, List<Type> types) {
		this.loc = loc;
		for (Type t : types) {
			if (t instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType)t;
				uts.add(ut);
				ut.consolidatesWith(this);
			} else
				this.types.add(t);
		}
	}

	public ConsolidateTypes(InputPosition loc, Type...types) {
		this(loc, Arrays.asList(types));
	}

	@Override
	public String signature() {
		throw new NotImplementedException(types + " -- " + uts);
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}
	
	public void consolidatesTo(Type result) {
		this.result = result;
	}
	
	public boolean isConsolidated() {
		return result != null;
	}
	
	public Type consolidatedAs() {
		if (result == null)
			throw new NotImplementedException("This type has not been consolidated");
		return result;
	}
	
	@Override
	public String toString() {
		return "Consolidate" + types;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
}
