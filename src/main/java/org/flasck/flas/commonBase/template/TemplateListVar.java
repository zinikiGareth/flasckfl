package org.flasck.flas.commonBase.template;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class TemplateListVar implements Locatable, Pushable {
	public final InputPosition location;
	public final FunctionName dataFunc;
	public final String simpleName;
	public final String realName;

	public TemplateListVar(InputPosition location, FunctionName fnName, IterVar iv) {
		this.location = location;
		this.dataFunc = fnName;
		this.simpleName = iv.var;
		this.realName = iv.uniqueName();
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushTLV(location, this);
	}

	@Override
	public String toString() {
		return "TLV[" + realName + "<-" + dataFunc.uniqueName() + "]";
	}
}
