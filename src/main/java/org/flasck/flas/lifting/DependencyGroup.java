package org.flasck.flas.lifting;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.FunctionGroup;

public class DependencyGroup implements FunctionGroup {
	private Set<FunctionDefinition> group;

	public DependencyGroup(FunctionDefinition... fns) {
		this.group = new TreeSet<>();
		for (FunctionDefinition f : fns)
			group.add(f);
	}

	public DependencyGroup(Set<FunctionDefinition> tc) {
		this.group = tc;
	}

	@Override
	public Iterator<FunctionDefinition> iterator() {
		return group.iterator();
	}
	
	@Override
	public String toString() {
		return group.toString();
	}
}
