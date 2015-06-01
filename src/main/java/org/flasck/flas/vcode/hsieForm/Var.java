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
		if (getClass() != obj.getClass())
			return false;
		Var other = (Var) obj;
		if (idx != other.idx)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "v" + idx;
	}
}