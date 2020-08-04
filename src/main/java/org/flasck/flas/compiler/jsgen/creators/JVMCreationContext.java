package org.flasck.flas.compiler.jsgen.creators;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;

public interface JVMCreationContext {

	void done();

	NewMethodDefiner method();
	IExpr helper();
	IExpr cxt();

	void bind(JSExpr key, Slot slot);
	void assignTo(JSLocal jsLocal, JSExpr value);
	void pushFunction(JSExpr key, FunctionName name);
	IExpr arg(JSExpr jsExpr);
	void closure(JSExpr key, boolean wantObject, JSExpr[] args);
	void eval(JSExpr key, String clz, List<JSExpr> args);
	void returnExpr(JSExpr jsExpr);
}
