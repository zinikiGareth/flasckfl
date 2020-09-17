package org.flasck.flas.commonBase.names;

public class SystemTestName extends SolidName {

	public SystemTestName(UnitTestFileName container, String special) {
		super(container, special);
	}
	
	public SystemTestName(UnitTestFileName container, int cnt) {
		this(container, "_st" + cnt);
	}

}
