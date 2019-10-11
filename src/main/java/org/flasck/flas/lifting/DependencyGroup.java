package org.flasck.flas.lifting;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.FunctionGroup;

public class DependencyGroup implements FunctionGroup {
	private Set<FunctionDefinition> group = new TreeSet<>();

	@Override
	public Iterator<FunctionDefinition> iterator() {
		return group.iterator();
	}
}
