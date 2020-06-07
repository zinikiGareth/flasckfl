package org.flasck.flas.lifting;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.repository.FunctionGroup;

public class DependencyGroup implements FunctionGroup {
	private Set<LogicHolder> functions;

	public DependencyGroup(LogicHolder... fns) {
		this.functions = new TreeSet<>();
		for (LogicHolder f : fns)
			functions.add(f);
	}
	
	public DependencyGroup(Set<LogicHolder> functions) {
		this.functions = functions;
	}

	@Override
	public boolean isEmpty() {
		return functions.isEmpty();
	}

	@Override
	public Iterable<LogicHolder> functions() {
		return functions;
	}
	
	@Override
	public String toString() {
		return functions.toString();
	}
}
