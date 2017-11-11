package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class FunctionLiteral implements Locatable, Pushable {
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
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushFunc(location, this);
	}

	@Override
	public String toString() {
		return name.uniqueName() + "()";
	}

}
