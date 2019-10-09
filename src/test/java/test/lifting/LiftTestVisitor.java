package test.lifting;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository.Visitor;

public class LiftTestVisitor extends LeafAdapter implements Visitor {

	private Visitor v;

	public LiftTestVisitor(Visitor v) {
		this.v = v;
	}

	public void visitFunction(FunctionDefinition fn) {
		v.visitFunction(fn);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		v.visitPatternVar(varLoc, var);
	}

}
