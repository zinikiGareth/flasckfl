package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.parsedForm.ut.TestStepHolder;

public class SystemTestStage extends TestStepHolder {
	public final SystemTestName name;
	public final String desc;

	public SystemTestStage(SystemTestName name, String desc) {
		this.name = name;
		this.desc = desc;
	}
}
