package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.types.Type;

public class LocalVar implements Expr {
	public final InputPosition varLoc;
	public final FunctionName fnName;
	public final NameOfThing caseName;
	public final VarName var;
	public final InputPosition typeLoc;
	public final Type type;

	public LocalVar(FunctionName fnName, NameOfThing caseName, InputPosition varLoc, String var, InputPosition typeLoc, Type type) {
		this.fnName = fnName;
		this.varLoc = varLoc;
		this.caseName = caseName;
		this.var = new VarName(varLoc, caseName, var);
		this.typeLoc = typeLoc;
		this.type = type;
	}
	
	public String uniqueName() {
		return this.var.uniqueName();
	}
	
	@Override
	public InputPosition location() {
		return varLoc;
	}
	
	@Override
	public String toString() {
		return uniqueName();
	}
}
