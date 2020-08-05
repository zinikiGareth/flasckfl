package org.flasck.flas.compiler.jsgen.creators;

import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public interface JVMCreationContext {

	void done(JSBlockCreator meth);

	NewMethodDefiner method();
	IExpr helper();
	IExpr cxt();

	void recordSlot(Slot s, IExpr e);
	void local(JSExpr key, IExpr e);
	void bindVar(JSExpr local, Var v);
	void block(JSBlock jsBlock, List<IExpr> blk);
	IExpr slot(Slot slot);
	IExpr stmt(JSExpr stmt);
	IExpr arg(JSExpr jsExpr);
	IExpr argAsIs(JSExpr jsExpr);
	IExpr argAs(JSExpr test, JavaType b);
	IExpr blk(JSBlockCreator blk);

	String figureName(NameOfThing fn);


}
