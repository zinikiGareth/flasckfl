package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.BlockExpr;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.NewMethodDefiner;

public class StmtCollector {
	private final List<Expr> stmts = new ArrayList<Expr>();
	private final NewMethodDefiner meth;

	public StmtCollector(NewMethodDefiner meth) {
		this.meth = meth;
	}
	
	public Expr asBlock() {
		if (stmts.isEmpty())
			return null;
		else if (stmts.size() == 1)
			return stmts.get(0);
		else
			return new BlockExpr(meth, stmts);
	}

	public void add(Expr stmt) {
		stmts.add(stmt);
	}

}
