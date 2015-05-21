package org.flasck.flas.vcode.hsieForm;

import java.util.List;

public class ReturnCmd extends HSIEBlock {
	public final Var var;
	public final List<Var> deps;
	public final Integer ival;
	public final String fn;

	public ReturnCmd(Var var, List<Var> deps) {
		this.var = var;
		this.deps = deps;
		this.ival = null;
		this.fn = null;
	}

	public ReturnCmd(int i) {
		this.var = null;
		this.deps = null;
		this.ival = i;
		this.fn = null;
	}

	public ReturnCmd(String fn) {
		this.var = null;
		this.deps = null;
		this.ival = null;
		this.fn = fn;
	}

	@Override
	public String toString() {
		return "RETURN " + ((var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:"ERR") + (deps != null?" " + deps:"");
	}
}
