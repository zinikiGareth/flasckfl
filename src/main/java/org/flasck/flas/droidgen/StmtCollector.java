package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.BlockExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;

public class StmtCollector {
	private final List<IExpr> stmts = new ArrayList<IExpr>();
	private final NewMethodDefiner meth;

	public StmtCollector(NewMethodDefiner meth) {
		this.meth = meth;
	}
	
	public IExpr asBlock() {
		if (stmts.isEmpty())
			return null;
		else if (stmts.size() == 1)
			return stmts.get(0);
		else
			return new BlockExpr(meth, stmts);
	}

	public void add(IExpr stmt) {
		stmts.add(stmt);
	}

}
