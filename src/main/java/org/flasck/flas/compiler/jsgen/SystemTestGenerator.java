package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;

public class SystemTestGenerator extends LeafAdapter {
	private final NestedVisitor sv;
	private final JSStorage jse;
	private JSClassCreator clz;
	private JSFunctionState state;
	private JSExpr runner;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private Set<UnitDataDeclaration> globalMocks = new HashSet<UnitDataDeclaration>();
	private final List<JSExpr> explodingMocks = new ArrayList<>();
	private final Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	private final Map<IntroduceVar, JSExpr> introductions = new TreeMap<>(IntroduceVar.comparator);
	private final Map<NameOfThing, JSExpr> containers = new HashMap<>();
	private final Map<String, JSExpr> applications = new HashMap<>();
	private final List<JSExpr> steps = new ArrayList<>();
	private SystemTestName stageName;
	
	public SystemTestGenerator(NestedVisitor sv, JSStorage jse, SystemTest st) {
		this.sv = sv;
		this.jse = jse;
		sv.push(this);
		createClass(st);
	}
	
	public void shareWith(SystemTestModule module) {
		module.inject(jse, meth, state, block, runner);
	}

	private void createClass(SystemTest st) {
		NameOfThing name = st.name();
		PackageName pkg = name.packageName();
		jse.ensurePackageExists(pkg, name.container().jsName());
		clz = jse.newSystemTest(st);
		clz.field(false, Access.PRIVATE, new PackageName(J.TESTHELPER), "_runner");
		JSMethodCreator ctor = clz.constructor();
		this.runner = ctor.argument(J.TESTHELPER, "runner");
		ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.setField(false, "_runner", runner);
		ctor.initContext(false);
//		ctor.clear();
		ctor.returnVoid();
	}

	@Override
	public void visitSystemTestStage(SystemTestStage s) {
		this.stageName = s.name;
		this.meth = clz.createMethod(s.name.baseName(), true);
		this.meth.argument(J.FLEVALCONTEXT, "_cxt");
		state = new JSFunctionStateStore(meth, this.mocks, this.introductions, this.containers, this.applications);
		meth.returnsType(List.class.getName());
		this.runner = meth.field("_runner");
//		meth.initContext(true);
		this.block = meth;
		steps.clear();
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		UDDGeneratorJS.handleUDD(sv, meth, state, this.block, globalMocks, explodingMocks, udd);
	}
	
	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		UnitTestStepGenerator sg = new UnitTestStepGenerator(sv, jse, clz, meth, state, this.block, this.runner, globalMocks, explodingMocks, true, stageName, steps.size()+1);
		steps.add(meth.string(sg.name()));
	}

	@Override
	public void leaveSystemTestStage(SystemTestStage s) {
		state.meth().returnObject(state.meth().makeArray(steps));
		state = null;
	}

	@Override
	public void leaveSystemTest(SystemTest st) {
		sv.result(null);
	}
}
