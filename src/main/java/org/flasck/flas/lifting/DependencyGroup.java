package org.flasck.flas.lifting;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.repository.FunctionGroup;

public class DependencyGroup implements FunctionGroup {
	private Set<FunctionDefinition> functions;
	private Set<StandaloneMethod> standalones;

	public DependencyGroup(FunctionDefinition... fns) {
		this.functions = new TreeSet<>();
		for (FunctionDefinition f : fns)
			functions.add(f);
		this.standalones = new HashSet<StandaloneMethod>();
	}

	public DependencyGroup(Set<FunctionDefinition> tc) {
		this.functions = tc;
	}

	public DependencyGroup(Set<FunctionDefinition> functions, Set<StandaloneMethod> standalones) {
		this.functions = functions;
		this.standalones = standalones;
	}

	@Override
	public Iterable<FunctionDefinition> functions() {
		return functions;
	}
	
	@Override
	public Iterable<StandaloneMethod> standalones() {
		return standalones;
	}

	@Override
	public String toString() {
		return functions.toString();
	}
}
