package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.NotImplementedException;

public class JVMBlock implements JVMBlockCreator {
	private final List<IExpr> stmts = new ArrayList<>();
	private final NewMethodDefiner meth;
	
	public JVMBlock(NewMethodDefiner meth) {
		this.meth = meth;
	}

	public JVMBlock(JVMBlockCreator currentBlock) {
		this.meth = currentBlock.method();
	}

	@Override
	public NewMethodDefiner method() {
		return meth;
	}

	@Override
	public void add(IExpr stmt) {
		stmts.add(stmt);
	}

	@Override
	public boolean isEmpty() {
		return stmts.isEmpty();
	}
	
	@Override
	public IExpr removeLast() {
		return stmts.remove(stmts.size()-1);
	}

	@Override
	public IExpr singleton() {
		if (stmts.size() != 1)
			throw new RuntimeException("Multiple result expressions");
		IExpr ret = stmts.get(0);
		stmts.clear();
		return ret;
	}

	@Override
	public IExpr convert() {
		IExpr ret;
		if (stmts.isEmpty())
			throw new NotImplementedException("there must be at least one statement in a block");
		else if (stmts.size() == 1)
			ret = stmts.get(0);
		else
			ret = meth.block(stmts.toArray(new IExpr[stmts.size()]));
		stmts.clear();
		return ret;
	}

}
