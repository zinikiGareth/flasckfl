package org.flasck.flas.lifting;

import java.util.Iterator;
import java.util.List;

import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;

public class FunctionGroupOrdering implements FunctionGroups {
	private final List<FunctionGroup> ordering;

	public FunctionGroupOrdering(List<FunctionGroup> ordering) {
		this.ordering = ordering;
	}

	@Override
	public Iterator<FunctionGroup> iterator() {
		return ordering.iterator();
	}

	@Override
	public int size() {
		return ordering.size();
	}
}
