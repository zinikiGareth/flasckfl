package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public interface JVMCreationContext {

	void done();

	NewMethodDefiner method();
	IExpr helper();
	IExpr cxt();

	void local(JSExpr key, IExpr e);
	void bindVar(JSLocal local, Var v);
	IExpr arg(JSExpr jsExpr);
	IExpr argAsIs(JSExpr jsExpr);

	String figureName(NameOfThing fn);
}
