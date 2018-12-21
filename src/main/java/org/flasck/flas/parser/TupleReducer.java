package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedOperator;

// TODO: I feel this should implement an interface
// This should emerge in doing Lists
public class TupleReducer {
	private InputPosition location;

	public TupleReducer(InputPosition location) {
		this.location = location;
	}
	
	public Expr reduce(List<Expr> args) {
		return new ApplyExpr(location, new UnresolvedOperator(location, "()"), args.toArray());
	}
}
