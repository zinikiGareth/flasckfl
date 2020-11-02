package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashSet;
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
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxPump;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
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

	public SystemTestGenerator(NestedVisitor sv, JSStorage jse, SystemTest st) {
		this.sv = sv;
		this.jse = jse;
		sv.push(this);
		createClass(st);
	}
	
	public void shareWith(SystemTestModule module) {
		module.inject(meth, state, block, runner);
	}

	private void createClass(SystemTest st) {
		NameOfThing name = st.name();
		String pkg = name.packageName().jsName();
		jse.ensurePackageExists(pkg, name.container().jsName());
		clz = jse.newSystemTest(st);
		clz.field(false, Access.PRIVATE, new PackageName(J.TESTHELPER), "_runner");
		JSMethodCreator ctor = clz.constructor();
		this.runner = ctor.argument(J.TESTHELPER, "runner");
		ctor.setField(false, "_runner", runner);
		ctor.initContext(false);
		ctor.clear();
		ctor.returnVoid();
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		UDDGeneratorJS.handleUDD(sv, meth, state, this.block, globalMocks, explodingMocks, udd);
	}
	
	@Override
	public void visitUnitTestSend(UnitTestSend uts) {
		new DoSendGeneratorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void visitSystemTestStage(SystemTestStage s) {
		this.meth = clz.createMethod(s.name.baseName(), true);
		state = new JSFunctionStateStore(meth, this.mocks);
		meth.returnsType("void");
		this.runner = meth.field("_runner");
		meth.initContext(true);
		this.block = meth;
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitorJS(state, sv, state.meth(), this.runner);
	}

	@Override
	public void visitUnitTestMatch(UnitTestMatch m) {
		new DoUTMatchGeneratorJS(state, sv, this.block, this.runner);
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
	public void leaveSystemTestStage(SystemTestStage s) {
		if (s.name.baseName().equals("configure")) {
			Map<UnitDataDeclaration, JSExpr> asfields = new TreeMap<>(mocks);
			mocks.clear();
			for (Entry<UnitDataDeclaration, JSExpr> e : asfields.entrySet()) {
				String mn = "_mock_" + e.getKey().name.baseName();
				clz.field(false, Access.PRIVATE, new PackageName(J.OBJECT), mn);
				this.meth.setField(false, mn, e.getValue());
				mocks.put(e.getKey(), this.meth.field(mn));
			}
		} else if (s.name.baseName().equals("finally")) {
			// TODO: this suggests we should probably always create these ...
//			throw new NotImplementedException("we should check all the mocks");
		}
		state.meth().returnVoid();
		state = null;
	}

	@Override
	public void leaveSystemTest(SystemTest st) {
		sv.result(null);
	}
}
