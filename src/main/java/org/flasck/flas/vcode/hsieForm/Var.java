package org.flasck.flas.vcode.hsieForm;

public class Var {
	public final int idx;

	public Var(int i) {
		this.idx = i;
	}
	
	@Override
	public String toString() {
		return "v" + idx;
	}
}