package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public class SystemTestStage extends TestStepHolder implements IntroductionConsumer {
	public final SystemTestName name;
	public final String desc;
	private final IntroductionConsumer topLevel;

	public SystemTestStage(SystemTestName name, String desc, IntroductionConsumer topLevel) {
		this.name = name;
		this.desc = desc;
		this.topLevel = topLevel;
	}
	
	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		topLevel.newIntroduction(errors, var);
	}
	
	@Override
	public void data(ErrorReporter errors, UnitDataDeclaration dd) {
		super.data(errors, dd);
	}
	
	@Override
	public String toString() {
		return "SystemTestStage[" + desc + "]";
	}
}
