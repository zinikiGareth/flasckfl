package org.flasck.flas.lifting;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.repository.FunctionGroup;

public class DependencyGroup implements FunctionGroup {
	private Set<StandaloneDefn> functions;

	public DependencyGroup(StandaloneDefn... fns) {
		this.functions = new TreeSet<>();
		for (StandaloneDefn f : fns)
			functions.add(f);
	}
	
	public DependencyGroup(Set<StandaloneDefn> functions) {
		this.functions = functions;
	}

	@Override
	public boolean isEmpty() {
		return functions.isEmpty();
	}

	@Override
	public Iterable<StandaloneDefn> functions() {
		return functions;
	}
	
	@Override
	public String toString() {
		return functions.toString();
	}
}
