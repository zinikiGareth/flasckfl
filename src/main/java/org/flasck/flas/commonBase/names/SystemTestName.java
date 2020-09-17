package org.flasck.flas.commonBase.names;

public class SystemTestName extends SolidName implements JavaMethodNameProvider {

	public SystemTestName(UnitTestFileName container, String special) {
		super(container, special);
	}
	
	public SystemTestName(UnitTestFileName container, int cnt) {
		this(container, "stage" + cnt);
	}

	@Override
	public String javaMethodName() {
		return name;
	}
}
