package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class RWMethodDefinition implements Locatable {
	public final List<RWMethodCaseDefn> cases = new ArrayList<>();
	private final InputPosition location;
	private final String name;
	private final int nargs;
	public final Set<ScopedVar> scopedVars = new TreeSet<ScopedVar>();
	
	public RWMethodDefinition(InputPosition location, String name, int nargs) {
		this.location = location;
		this.name = name;
		this.nargs = nargs;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public String name() {
		return name;
	}
	
	public int nargs() {
		return nargs;
	}

	public void gatherScopedVars() {
		for (RWMethodCaseDefn c : cases) {
			c.gatherScopedVars(scopedVars);
		}
	}
	
	@Override
	public String toString() {
		return name + "/" + nargs;
	}
}
