package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public class SystemTestStage extends TestStepHolder implements IntroductionConsumer {
	public final SystemTestName name;
	public boolean hasMock, hasApp;
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
	
	public void mockApplication(ErrorReporter errors, VarName vn, MockApplication mockApplication) {
		if (!"configure".equals(name.baseName())) {
			errors.message(vn.loc, "can only define application in configure");
			return;
		}
		if (hasMock) {
			errors.message(vn.loc, "cannot use both mocks and application in the same test");
			return;
		}
		hasApp = true;
		this.steps.add(new CreateMockApplication(mockApplication));
	}
	
	@Override
	public void data(ErrorReporter errors, UnitDataDeclaration dd) {
		if (hasApp) {
			errors.message(dd.location(), "cannot use both mocks and application in the same test");
			return;
		}
		hasMock = true;
		super.data(errors, dd);
	}
	
	public void gotoRoute(ErrorReporter errors, UnresolvedVar app, Expr route, IntroduceVar iv) {
		this.steps.add(new GotoRoute(app, route, iv));
	}
	
	public void userLogin(ErrorReporter errors, UnresolvedVar app, Expr user) {
		this.steps.add(new UserLogin(app, user));
	}
	
	@Override
	public String toString() {
		return "SystemTestStage[" + desc + "]";
	}
}
