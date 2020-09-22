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
	
	public void ajaxCreate(ErrorReporter errors, AjaxCreate ac) {
		if (!"configure".equals(name.baseName())) {
			errors.message(ac.location(), "cannot have ajax create in a test stage");
			return;
		}
		this.steps.add(ac);
	}
	
	public void ajaxPump(ErrorReporter errors, AjaxPump pump) {
		if (!name.baseName().startsWith("stage")) {
			errors.message(pump.location(), "ajax pump can only be used in a test stage");
			return;
		}
		this.steps.add(pump);
	}
	
	@Override
	public String toString() {
		return "SystemTestStage[" + desc + "]";
	}
}
