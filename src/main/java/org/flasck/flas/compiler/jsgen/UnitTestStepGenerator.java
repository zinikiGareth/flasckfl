package org.flasck.flas.compiler.jsgen;

import java.util.List;
import java.util.Set;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestNewDiv;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class UnitTestStepGenerator extends LeafAdapter {
	private final NestedVisitor sv;
	private final JSMethodCreator meth;
	private final JSFunctionState state;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private final Set<UnitDataDeclaration> globalMocks;
	private final List<JSExpr> explodingMocks;

	public UnitTestStepGenerator(NestedVisitor sv, JSMethodCreator meth, JSFunctionState state, JSBlockCreator block, JSExpr runner, Set<UnitDataDeclaration> globalMocks, List<JSExpr> explodingMocks) {
		this.sv = sv;
		this.meth = meth;
		this.state = state;
		this.block = block;
		this.runner = runner;
		this.globalMocks = globalMocks;
		this.explodingMocks = explodingMocks;
		sv.push(this);
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		UDDGeneratorJS.handleUDD(sv, meth, state, this.block, globalMocks, explodingMocks, udd);
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestShove(UnitTestShove a) {
		new HandleShoveClauseVisitorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect ute) {
		new DoExpectationGeneratorJS(state, sv, this.block);
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		new DoInvocationGeneratorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestSend(UnitTestSend uts) {
		new DoSendGeneratorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestRender(UnitTestRender e) {
		new DoUTRenderGeneratorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestEvent(UnitTestEvent ute) {
		new DoUTEventGeneratorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitUnitTestMatch(UnitTestMatch m) {
		new DoUTMatchGeneratorJS(state, sv, this.block, this.runner);
	}
	
	@Override
	public void visitUnitTestNewDiv(UnitTestNewDiv s) {
		this.block.newdiv(s.cnt);
	}
	
	@Override
	public void leaveUnitTestStep(UnitTestStep s) {
		sv.result(null);
	}

}
