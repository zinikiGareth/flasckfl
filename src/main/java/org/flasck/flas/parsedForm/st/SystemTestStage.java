package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parser.ut.IntroductionConsumer;

public class SystemTestStage extends TestStepHolder implements IntroductionConsumer {
	public final SystemTestName name;
	public final String desc;
	private final IntroductionConsumer topLevel;

	public SystemTestStage(SystemTestName name, String desc, IntroductionConsumer topLevel) {
		this.name = name;
		this.desc = desc;
		this.topLevel = topLevel;
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
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		topLevel.newIntroduction(errors, var);
	}
	
	public void gotoRoute(ErrorReporter errors, Expr route, IntroduceVar iv) {
		this.steps.add(new GotoRoute(route, iv));
	}
	
	@Override
	public String toString() {
		return "SystemTestStage[" + desc + "]";
	}
}
