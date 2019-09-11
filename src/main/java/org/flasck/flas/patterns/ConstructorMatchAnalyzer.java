package org.flasck.flas.patterns;

import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class ConstructorMatchAnalyzer extends LeafAdapter {
	private final HSITree hsiTree;
	private NestedVisitor sv;

	public ConstructorMatchAnalyzer(NestedVisitor sv, HSITree tree) {
		this.sv = sv;
		hsiTree = tree;
	}
	
	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
		sv.result(hsiTree);
	}
}
