package org.flasck.flas.patterns;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;

public class PatternAnalyzer extends LeafAdapter{
	private HSITree hsiTree;
	private final NestedVisitor sv;
	private int nslot = 0;
	private HSIOptions slot;
	
	public PatternAnalyzer(ErrorResult errors, Repository repository, NestedVisitor sv) {
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		hsiTree = new HSIPatternTree(fn.argCount());
	}
	
	@Override
	public void visitPattern(Object patt) {
		this.slot = hsiTree.get(nslot++);
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p) {
		HSITree nested = new HSIPatternTree(p.args.size());
		slot.addCM(p.ctor, nested);
		sv.push(new ConstructorMatchAnalyzer(sv, nested));
	}
	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		// TODO: this should actually bind a projection of the tree
		fi.bindTree(hsiTree);
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindHsi(hsiTree);
	}
}
