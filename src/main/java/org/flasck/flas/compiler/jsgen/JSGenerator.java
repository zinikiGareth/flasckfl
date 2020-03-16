package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.commonBase.names.FunctionName;
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
import org.flasck.flas.parsedForm.ContractDeclDir;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.StructFieldHandler;
import org.flasck.flas.tc3.NamedType;
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
	private JSExpr evalRet;
	private ObjectAccessor currentOA;
	private StructFieldHandler structFieldHandler;
	private final List<FunctionName> methods = new ArrayList<>();
	private JSClassCreator ctrDown;
	private JSClassCreator ctrUp;
	private Set<UnitDataDeclaration> globalMocks = new HashSet<UnitDataDeclaration>();

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
	public void visitObjectAccessor(ObjectAccessor oa) {
		this.currentOA = oa;
	}
	
	@Override
	public void leaveObjectAccessor(ObjectAccessor oa) {
		this.currentOA = null;
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		switchVars.clear();
		if (fn.intros().isEmpty()) {
			this.meth = null;
			return;
		}
		String pkg = fn.name().packageName().jsName();
		String cxName = fn.name().inContext.jsName();
		jse.ensurePackageExists(pkg, cxName);
		if (currentOA == null)
			this.meth = jse.newFunction(pkg, cxName, false, fn.name().name);
		else
			this.meth = jse.newFunction(pkg, cxName, true, fn.name().name);
			
		this.meth.argument("_cxt");
		for (int i=0;i<fn.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.state = new JSFunctionStateStore(globalMocks);
	}

	// When generating a tuple assignment, we have to create a closure which is the "main thing"
	// and then (below) a closure extracting each member from this thing 
	@Override
	public void visitTuple(TupleAssignment e) {
		switchVars.clear();
		String pkg = e.name().packageName().jsName();
		String cxName = e.name().inContext.jsName();
		jse.ensurePackageExists(pkg, cxName);
		this.meth = jse.newFunction(pkg, cxName, false, e.name().name);
			
		this.meth.argument("_cxt");
		this.block = meth;
		this.state = new JSFunctionStateStore(globalMocks);
		sv.push(new ExprGeneratorJS(state, sv, this.block));
	}
	
	@Override
	public void visitTupleMember(TupleMember e) {
		switchVars.clear();
		String pkg = e.name().packageName().jsName();
		String cxName = e.name().inContext.jsName();
		jse.ensurePackageExists(pkg, cxName);
		this.meth = jse.newFunction(pkg, cxName, false, e.name().name);
			
		this.meth.argument("_cxt");
		this.block = meth;
		this.state = new JSFunctionStateStore(globalMocks);
		this.meth.returnObject(meth.defineTupleMember(e));
//		sv.push(new ExprGeneratorJS(state, sv, this.block));
	}
	
	@Override
	public void visitStructDefn(StructDefn obj) {
		if (!obj.generate)
			return;
		String pkg = ((SolidName)obj.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, obj.name().container().jsName());
		JSClassCreator ctr = jse.newClass(pkg, obj.name().jsName());
		JSBlockCreator ctor = ctr.constructor();
		ctor.stateField();
		this.meth = ctr.createMethod("eval", false);
		this.meth.argument("_cxt");
		this.evalRet = meth.newOf(obj.name());
		this.block = meth;
		this.structFieldHandler = sf -> {
			JSExpr arg = this.meth.argument(sf.name);
			this.meth.storeField(this.evalRet, sf.name, arg);
		};
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		if (!obj.generate)
			return;
		String pkg = ((SolidName)obj.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, obj.name().container().jsName());
		JSClassCreator ctr = jse.newClass(pkg, obj.name().jsName());
		JSBlockCreator ctor = ctr.constructor();
		ctor.stateField();
		this.meth = ctr.createMethod("eval", false);
		this.meth.argument("_cxt");
		this.evalRet = meth.newOf(obj.name());
		this.block = meth;
		this.structFieldHandler = sf -> {
			if (sf.init != null)
				new StructFieldGeneratorJS(state, sv, block, sf.name, evalRet);
		};
	}
	
	@Override
	public void visitStructField(StructField sf) {
		if (structFieldHandler != null)
			structFieldHandler.visitStructField(sf);
	}

	@Override
	public void visitStructFieldAccessor(StructField sf) {
		String pkg = sf.name().packageName().jsName();
		String cxName = sf.name().container().jsName();
		jse.ensurePackageExists(pkg, cxName);
		JSMethodCreator meth = jse.newFunction(pkg, cxName, true, "_field_" + sf.name);
		meth.argument("_cxt");
		meth.returnObject(meth.loadField(sf.name));
	}
	
	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		jse.methodList(obj.name(), methods);
		if (evalRet != null)
			meth.returnObject(evalRet);
		this.block = null;
		this.evalRet = null;
		this.meth = null;
		this.methods.clear();
	}
	
	@Override
	public void leaveStructDefn(StructDefn obj) {
		if (evalRet != null)
			meth.returnObject(evalRet);
		this.block = null;
		this.evalRet = null;
		this.meth = null;
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
		this.meth = jse.newFunction(pkg, pkg, currentOA != null, om.name().jsName().substring(pkg.length()+1));
		this.methods.add(om.name());
		this.meth.argument("_cxt");
		for (int i=0;i<om.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.state = new JSFunctionStateStore(globalMocks);
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
	public void leaveTuple(TupleAssignment ta) {
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
		ctrDown = jse.newClass(pkg, cd.name().jsName() + ".Down");
		JSMethodCreator downName = ctrDown.createMethod("name", true);
		downName.returnObject(new JSString(cd.name().uniqueName() + ".Down"));
		ctrUp = jse.newClass(pkg, cd.name().jsName() + ".Up");
		JSMethodCreator upName = ctrUp.createMethod("name", true);
		upName.returnObject(new JSString(cd.name().uniqueName() + ".Up"));
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		JSClassCreator clz;
		if (cmd.dir == ContractMethodDir.DOWN)
			clz = ctrDown;
		else
			clz = ctrUp;
		JSMethodCreator meth = clz.createMethod(cmd.name.name, true);
		meth.argument("_cxt");
		for (int k=0;k<cmd.args.size();k++) {
			meth.argument("_" + k);
		}
		meth.returnObject(new JSString("interface method for " + cmd.name.uniqueName()));
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		ctrUp = ctrDown = null;
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		UnitTestName clzName = e.name;
		if (currentOA != null)
			throw new NotImplementedException("I don't think you can nest a unit test in an accessor");
		String pkg = clzName.container().jsName();
		this.meth = jse.newFunction(pkg, pkg, false, clzName.baseName());
		this.block = meth;
		/*JSExpr cxt = */meth.argument("_cxt");
		runner = meth.argument("runner");
		this.state = new JSFunctionStateStore(globalMocks);
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		if (meth == null) {
			globalMocks.add(udd);
			return;
		}
		NamedType objty = udd.ofType.defn();
		if (objty instanceof ContractDeclDir) {
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
	public void visitUnitTestExpect(UnitTestExpect ute) {
		new DoExpectationGeneratorJS(state, sv, this.block);
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		new DoInvocationGeneratorJS(state, sv, this.block, this.runner);
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
