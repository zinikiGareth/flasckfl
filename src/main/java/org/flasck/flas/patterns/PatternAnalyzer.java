package org.flasck.flas.patterns;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HSITree;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;

public class PatternAnalyzer extends LeafAdapter{
	private HSITree hsiTree;
	
	public PatternAnalyzer(ErrorResult errors, Repository repository, NestedVisitor sv) {
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		hsiTree = new HSIPatternTree();
	}
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindHsi(hsiTree);
	}
}
