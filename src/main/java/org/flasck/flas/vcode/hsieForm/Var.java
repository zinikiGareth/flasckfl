package org.flasck.flas.vcode.hsieForm;

public class Var {
	public final int idx;

	public Var(int i) {
		this.idx = i;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idx;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof CreationOfVar)
			obj = ((CreationOfVar)obj).var;
		if (!(obj instanceof Var))
			return false;
		Var other = (Var) obj;
		return (idx == other.idx);
	}

	@Override
	public String toString() {
		return "v" + idx;
	}
}