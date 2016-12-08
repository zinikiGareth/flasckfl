package org.flasck.flas.rewrittenForm;

import org.zinutils.exceptions.UtilException;

public class AreaName {
	private final String simple;

	public AreaName(String simple) {
		this.simple = simple;
	}
	
	public String jsName() {
		return simple;
	}

	public String javaName() {
		int idx = simple.lastIndexOf("._");
		return simple.substring(0, idx+1) + simple.substring(idx+2);
	}
	
	@Override
	public String toString() {
		throw new UtilException("Yo!");
	}
}
