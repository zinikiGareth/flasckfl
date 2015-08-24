package org.flasck.flas.typechecker;

public class TypeVar {
	public final GarneredFrom from;
	public final int ivar;
	
	public TypeVar(GarneredFrom from, int which) {
		this.from = from;
		this.ivar = which;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ivar;
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
		TypeVar other = (TypeVar) obj;
		if (ivar != other.ivar)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "tv_"+ivar;
	}
}
