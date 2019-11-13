package org.flasck.flas.compiler.jsgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.exceptions.NotImplementedException;

public class JSGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public static class XCArg {
		public final int arg;
		public final JSExpr expr;

		public XCArg(int arg, JSExpr expr) {
			this.arg = arg;
			this.expr = expr;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof XCArg))
				return false;
			XCArg o = (XCArg) obj;
			return o.arg == arg && o.expr == expr;
		}
		
		@Override
		public int hashCode() {
			return arg ^ expr.hashCode();
		}
		
		@Override
		public String toString() {
			return arg + ":" + expr;
		}
	}

	private final JSStorage jse;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private JSExpr runner;
	private final Map<Slot, String> switchVars = new HashMap<>();
	private NestedVisitor sv;
	private JSFunctionState state;

	public JSGenerator(JSStorage jse, StackVisitor sv) {
		this.jse = jse;
		this.sv = sv;
		if (sv != null)
			sv.push(this);
	}

	public JSGenerator(JSMethodCreator meth, JSExpr runner, NestedVisitor sv, JSFunctionState state) {
		this.sv = sv;
		this.jse = null;
		if (meth == null)
			throw new RuntimeException("Meth cannot be null");
		this.meth = meth;
		this.block = meth;
		this.runner = runner;
		if (sv != null)
			sv.push(this);
		this.state = state;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		switchVars.clear();
		if (fn.intros().isEmpty()) {
			this.meth = null;
			return;
		}
		String pkg = fn.name().packageName().jsName();
		jse.ensurePackageExists(pkg, fn.name().inContext.jsName());
		this.meth = jse.newFunction(pkg, fn.name().jsName().substring(pkg.length()+1));
		this.meth.argument("_cxt");
		for (int i=0;i<fn.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.state = new JSFunctionStateStore();
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		if (!obj.generate)
			return;
		String pkg = ((SolidName)obj.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, obj.name().container().jsName());
		JSClassCreator ctr = jse.newClass(pkg, obj.name().jsName());
		JSBlockCreator ctor = ctr.constructor();
		ctor.fieldObject("state", "FieldsContainer");
		JSMethodCreator meth = ctr.createMethod("eval", false);
		meth.returnObject(meth.newOf(obj.name()));
		this.block = meth;
	}
	
	@Override
	public void visitStructField(StructField sf) {
		if (sf.init != null)
			sv.push(new ExprGeneratorJS(state, sv, block));
	}

	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		this.block = null;
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod om) {
		switchVars.clear();
		if (!om.isConverted()) {
			this.meth = null;
			return;
		}
		String pkg = om.name().packageName().jsName();
		jse.ensurePackageExists(pkg, om.name().inContext.jsName());
		this.meth = jse.newFunction(pkg, om.name().jsName().substring(pkg.length()+1));
		this.meth.argument("_cxt");
		for (int i=0;i<om.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.state = new JSFunctionStateStore();
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		for (Slot s : slots) {
			switchVars.put(s, "_" + switchVars.size());
		}
	}

	@Override
	public void switchOn(Slot slot) {
		sv.push(new JSHSIGenerator(state, sv, switchVars, slot, this.block));
	}

	// This is needed here as well as HSIGenerator to handle the no-switch case
	@Override
	public void startInline(FunctionIntro fi) {
		sv.push(new GuardGeneratorJS(state, sv, this.block));
	}

	@Override
	public void withConstructor(String string) {
		throw new NotImplementedException();
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		throw new NotImplementedException();
	}

	@Override
	public void matchNumber(int i) {
		throw new NotImplementedException();
	}

	@Override
	public void matchString(String s) {
		throw new NotImplementedException();
	}

	@Override
	public void matchDefault() {
		throw new NotImplementedException();
	}

	@Override
	public void defaultCase() {
		throw new NotImplementedException();
	}

	@Override
	public void errorNoCase() {
		throw new NotImplementedException();
	}

	@Override
	public void bind(Slot slot, String var) {
		this.block.bindVar(switchVars.get(slot), var);
	}

	@Override
	public void endSwitch() {
		throw new NotImplementedException();
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		this.meth = null;
		this.state = null;
	}

	@Override
	public void leaveObjectMethod(ObjectMethod om) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		this.meth = null;
		this.state = null;
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
		String pkg = ((SolidName)cd.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, cd.name().container().jsName());
		JSClassCreator ctr = jse.newClass(pkg, cd.name().jsName());
		JSMethodCreator meth = ctr.createMethod("name", true);
		meth.returnObject(new JSString(cd.name().uniqueName()));
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		UnitTestName clzName = e.name;
		meth = jse.newFunction(clzName.container().jsName(), clzName.baseName());
		this.block = meth;
		/*JSExpr cxt = */meth.argument("_cxt");
		runner = meth.argument("runner");
		this.state = new JSFunctionStateStore();
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		if (meth == null)
			throw new RuntimeException("Global UDDs are not yet handled");
		RepositoryEntry objty = udd.ofType.defn();
		if (objty instanceof ContractDecl) {
			JSExpr mock = meth.mockContract((SolidName) objty.name());
			state.addMock(udd, mock);
		} else if (objty instanceof ObjectDefn) {
			JSExpr obj = meth.createObject((SolidName) objty.name());
			state.addMock(udd, obj);
		} else {
			/* It seems to me that this requires us to traverse the whole of 
			 * the inner expression.  I'm not quite sure what is the best way to handle that.
			 * Another option on the traverser? A signal back to the traverser (how?) that
			 * says "traverse this"?  Creating a subtraverser here?
			 * 
			 * Reviewing this today, I don't see why you wouldn't want to traverse it all the time
			 * But probably have individual visit/leave combos for uddExpr and each uddField
			 * All ended by leaveUDD
			 */
			throw new RuntimeException("not handled: " + objty);
		}
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitorJS(state, sv, this.block, this.runner);
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth = null;
		state = null;
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
	}
	
	@Override
	public void result(Object r) {
		if (r != null)
			block.returnObject((JSExpr)r);
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv) {
		return new JSGenerator(meth, runner, nv, null);
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv, JSFunctionState state) {
		return new JSGenerator(meth, runner, nv, state);
	}
}
