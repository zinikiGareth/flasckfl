package org.flasck.flas.compiler.jvmgen;

import java.util.Map;

import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public interface JVMBlockCreator {
	NewMethodDefiner method();
	FunctionState state();

	void add(IExpr stmt);
	IExpr removeLast();
	boolean isEmpty();

	Map<String, Var> stashed();
	IExpr hasStashed(String myName);
	IExpr stash(String myName, IExpr e);

	IExpr singleton();
	IExpr convert();
}
