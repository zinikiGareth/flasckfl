package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;

public class SystemTestGenerator extends LeafAdapter {
	private final NestedVisitor sv;
	private final JSStorage jse;
	private JSClassCreator ctr;
	private JSFunctionState state;
	private JSExpr runner;

	public SystemTestGenerator(NestedVisitor sv, JSStorage jse, SystemTest st) {
		this.sv = sv;
		this.jse = jse;
		sv.push(this);
		createClass(st);
	}

	private void createClass(SystemTest st) {
		NameOfThing name = st.name();
		String pkg = name.packageName().jsName();
		jse.ensurePackageExists(pkg, name.container().jsName());
		ctr = jse.newClass(pkg, name);
		ctr.field(false, Access.PRIVATE, new PackageName(J.TESTHELPER), "_runner");
		JSMethodCreator ctor = ctr.constructor();
		JSVar runner = ctor.argument(J.TESTHELPER, "_runner");
		ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.setField(false, "_runner", runner);
		// store runner in object
		ctor.clear();
		ctor.returnVoid();
	}

	@Override
	public void visitSystemTestStage(SystemTestStage s) {
		JSMethodCreator meth = ctr.createMethod(s.name.baseName(), true);
		state = new JSFunctionStateStore(meth);
		meth.argument(J.FLEVALCONTEXT, "_cxt");
		meth.returnsType("void");
		this.runner = meth.field("_runner");
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitorJS(state, sv, state.meth(), this.runner);
	}

	@Override
	public void leaveSystemTestStage(SystemTestStage s) {
		state.meth().returnVoid();
		state = null;
	}

	@Override
	public void leaveSystemTest(SystemTest st) {
		sv.result(null);
	}
}
