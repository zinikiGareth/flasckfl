package org.flasck.flas.commonBase.names;

public class UnitTestName extends SolidName {

	public UnitTestName(UnitTestFileName container, int cnt) {
		super(container, "_ut" + cnt);
	}

	public UnitTestName(NameOfThing container, String step) {
		super(container, step);
	}

}
