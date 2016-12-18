package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;

public class FunctionLiteral implements Locatable {
	public final InputPosition location;
	public final FunctionName name;

	public FunctionLiteral(InputPosition location, FunctionName fnName) {
		this.location = location;
		this.name = fnName;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return name.uniqueName() + "()";
	}

}
