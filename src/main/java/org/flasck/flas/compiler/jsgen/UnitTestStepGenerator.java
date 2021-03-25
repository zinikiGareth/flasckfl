package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxPump;
import org.flasck.flas.parsedForm.st.GotoRoute;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInput;
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
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;

public class UnitTestStepGenerator extends LeafAdapter implements SharesState {
	private final NestedVisitor sv;
	private final JSClassCreator clz;
	private final JSFunctionState ostate;
	private final JSExpr runner;
	private final Set<UnitDataDeclaration> globalMocks;
	private final List<JSExpr> parentExplodingMocks;
	private final List<JSExpr> explodingMocks;
	private final JSMethodCreator meth;
	private final JSFunctionState state;
	private final JSBlockCreator block;
	private final TreeMap<UnitDataDeclaration, JSExpr> mocks;
	private final TreeMap<IntroduceVar, JSExpr> introductions;
	private final HashMap<NameOfThing, JSExpr> containers;
	private final String baseName;

	public UnitTestStepGenerator(NestedVisitor sv, JSStorage jse, JSClassCreator clz, JSMethodCreator meth, JSFunctionState state, JSBlockCreator block, JSExpr runner, Set<UnitDataDeclaration> globalMocks, List<JSExpr> explodingMocks, boolean includeJs, NameOfThing testName, int stepNum) {
		this.sv = sv;
		this.clz = clz;
		this.ostate = state;
		this.runner = runner;
		this.globalMocks = globalMocks;
		this.parentExplodingMocks = explodingMocks;
		this.explodingMocks = new ArrayList<>();
		if (sv != null)
			sv.push(this);
		
		if (testName != null) {
			baseName = testName.baseName() + "_step_" + stepNum;
			this.meth = clz.createMethod(baseName, true);
			this.meth.argument(J.FLEVALCONTEXT, "_cxt");
			this.meth.returnsType("void");
			this.meth.helper(runner);
			this.block = this.meth;
			if (!includeJs)
				this.meth.noJS();
		} else {
			// for global UDDs
			this.baseName = null;
			this.meth = meth;
			this.block = meth;
		}
		mocks = new TreeMap<>(ostate.mocks());
		introductions = new TreeMap<>(IntroduceVar.comparator);
		introductions.putAll(ostate.introductions());
		containers = new HashMap<>();
		containers.putAll(ostate.containers());
		this.state = new JSFunctionStateStore(this.meth, mocks, introductions, containers);
	}

	@Override
	public void shareWith(SystemTestModule module) {
		module.inject(null, meth, state, block, runner);
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
	public void visitUnitTestInput(UnitTestInput ute) {
		new DoUTInputGeneratorJS(state, sv, this.block, this.runner);
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
	public void visitAjaxCreate(AjaxCreate ac) {
		new AjaxCreator(clz, state, sv, this.block, this.runner, ac);
	}

	@Override
	public void visitAjaxPump(AjaxPump ap) {
		JSExpr member = block.member(new PackageName(J.AJAXMOCK), ap.var.baseName());
		block.callMethod("void", member, "pump");
	}

	@Override
	public void visitGotoRoute(GotoRoute gr) {
		new DoRouteGenerator(state, sv, this.block, this.runner);
	}
	
	@Override
	public void leaveUnitTestStep(UnitTestStep s) {
		Map<UnitDataDeclaration, JSExpr> asfields = new TreeMap<>(mocks);
		Map<JSExpr, JSExpr> mapmocks = new HashMap<>();
		for (Entry<UnitDataDeclaration, JSExpr> e : asfields.entrySet()) {
			if (ostate.mocks().containsKey(e.getKey()))
				continue;
			String mn = "_mock_" + e.getKey().name.baseName();
			clz.field(false, Access.PRIVATE, new PackageName(J.OBJECT), mn);
			this.meth.setField(false, mn, e.getValue());
			JSExpr fm = this.meth.field(mn);
			ostate.mocks().put(e.getKey(), fm);
			mapmocks.put(e.getValue(), fm);
		}
		for (JSExpr e : explodingMocks) {
			parentExplodingMocks.add(mapmocks.get(e));
		}
		TreeMap<IntroduceVar, JSExpr> asflds = new TreeMap<>(introductions);
		for (Entry<IntroduceVar, JSExpr> e : asflds.entrySet()) {
			if (ostate.introductions().containsKey(e.getKey()))
				continue;
			String bn = "_iv_" + e.getKey().var;
			if (!clz.hasField(bn)) {
				clz.field(false, Access.PRIVATE, new PackageName(J.OBJECT), bn);
			}
			this.meth.setField(false, bn, e.getValue());
			ostate.introductions().put(e.getKey(), this.meth.field(bn));
		}
		if (s != null)
			block.returnVoid();
		if (sv != null)
			sv.result(null);
	}

	public String name() {
		return baseName;
	}

	public void assertSatisfied(JSExpr m) {
		state.meth().assertSatisfied(m);
		state.meth().returnVoid();
	}

}
