package org.flasck.flas.compiler.jvmgen;

import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;

public interface JVMBlockCreator {
	void add(IExpr stmt);

	IExpr convert();

	boolean isEmpty();

	IExpr removeLast();

	IExpr singleton();

	NewMethodDefiner method();
}
