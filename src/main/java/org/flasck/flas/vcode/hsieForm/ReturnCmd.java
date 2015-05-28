package org.flasck.flas.vcode.hsieForm;

import java.util.List;

public class ReturnCmd extends PushReturn {
	public final List<Var> deps;

	public ReturnCmd(Var var, List<Var> deps) {
		super(var);
		this.deps = deps;
	}

	public ReturnCmd(int i) {
		super(i);
		this.deps = null;
	}

	public ReturnCmd(String fn) {
		super(fn);
		this.deps = null;
	}

	@Override
	public String toString() {
		return "RETURN " + ((var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:"ERR") + (deps != null?" " + deps:"");
	}
}
