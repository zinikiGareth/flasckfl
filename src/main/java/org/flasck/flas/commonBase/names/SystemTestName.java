package org.flasck.flas.commonBase.names;

public class SystemTestName extends FunctionName implements JavaMethodNameProvider {

	public SystemTestName(PackageName container, String special) {
		super(null, container, special);
	}
	
	public SystemTestName(PackageName container, int cnt) {
		this(container, "stage" + cnt);
	}

	@Override
	public String javaMethodName() {
		return name;
	}
}
