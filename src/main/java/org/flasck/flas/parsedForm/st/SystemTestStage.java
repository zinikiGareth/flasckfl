package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.TestStepHolder;

public class SystemTestStage extends TestStepHolder {
	public final SystemTestName name;
	public final String desc;

	public SystemTestStage(SystemTestName name, String desc) {
		this.name = name;
		this.desc = desc;
	}
	
	public void ajax(ErrorReporter errors, AjaxCreate ac) {
		if (!"configure".equals(name.baseName())) {
			errors.message(ac.location(), "cannot have ajax create in a test stage");
		}
	}
	
	@Override
	public String toString() {
		return "SystemTestStage[" + desc + "]";
	}
}
