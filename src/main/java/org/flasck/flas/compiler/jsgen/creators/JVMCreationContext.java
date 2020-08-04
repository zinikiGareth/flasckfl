package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;

public interface JVMCreationContext {

	void done();

	NewMethodDefiner method();
	IExpr helper();
	IExpr cxt();

	void assignTo(JSLocal jsLocal);
	void pushFunction(String fn);
	IExpr arg(JSExpr jsExpr);
	void closure(boolean wantObject, JSExpr[] args);
}
