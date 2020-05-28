package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSFromCard;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestNewDiv;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.StructFieldHandler;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain.Link;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
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
	private final NestedVisitor sv;
	private final Map<CardDefinition, EventTargetZones> eventMap;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private JSExpr runner;
	private final Map<Slot, String> switchVars = new HashMap<>();
	private JSFunctionState state;
	private JSExpr evalRet;
	private ObjectAccessor currentOA;
	private StructFieldHandler structFieldHandler;
	private Map<Object, List<FunctionName>> methodMap = new HashMap<>();
	private JSClassCreator currentContract;
	private Set<UnitDataDeclaration> globalMocks = new HashSet<UnitDataDeclaration>();
	private final List<JSExpr> explodingMocks = new ArrayList<>();
	private JSClassCreator agentCreator;
	private JSClassCreator templateCreator;
	private AtomicInteger containerIdx;

	public JSGenerator(JSStorage jse, StackVisitor sv, Map<CardDefinition, EventTargetZones> eventMap) {
		this.jse = jse;
		this.sv = sv;
		this.eventMap = eventMap;
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
		this.eventMap = null;
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
		this.meth = jse.newFunction(pkg, cxName, fn.hasState(), fn.name().name);
			
		this.meth.argument("_cxt");
		for (int i=0;i<fn.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		JSExpr st = null;
		if (fn.hasState()) {
			if (fn.state() instanceof ObjectDefn) { // for acors at least ... what about just random nested functions?
				st = new JSThis();
			} else
				st = new JSLiteral("_0");
		}
		this.state = new JSFunctionStateStore(meth, st);
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
		this.state = new JSFunctionStateStore(meth, null);
		sv.push(new ExprGeneratorJS(state, sv, this.block, false));
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
		this.state = new JSFunctionStateStore(meth, null);
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
		ctor.storeField(this.evalRet, "_type", ctor.string(obj.name.uniqueName()));
		JSMethodCreator areYouA = ctr.createMethod("_areYouA", true);
		areYouA.argument("ty");
		areYouA.returnCompare(areYouA.arg(0), areYouA.string(obj.name().jsName()));
		this.meth = ctr.createMethod("eval", false);
		this.meth.argument("_cxt");
		this.evalRet = meth.newOf(obj.name());
		this.block = meth;
		this.structFieldHandler = sf -> {
			if (sf.name.equals("id"))
				return;
			if (sf.init == null) {
				JSExpr arg = this.meth.argument(sf.name);
				this.meth.storeField(this.evalRet, sf.name, arg);
			} else {
				new StructFieldGeneratorJS(state, sv, block, sf.name, evalRet);
			}
		};
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		if (!obj.generate)
			return;
		String pkg = ((SolidName)obj.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, obj.name().container().jsName());
		jse.object(obj);
		templateCreator = jse.newClass(pkg, obj.name().jsName());
		templateCreator.inheritsFrom(new PackageName("FLObject"));
		JSBlockCreator ctor = templateCreator.constructor();
		ctor.stateField();
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(obj, methods);
		jse.methodList(obj.name(), methods);
		containerIdx = new AtomicInteger(1);
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
		meth.returnObject(meth.loadField(new JSThis(), sf.name));
	}
	
	@Override
	public void leaveStructDefn(StructDefn obj) {
		if (evalRet != null)
			meth.returnObject(evalRet);
		this.block = null;
		this.evalRet = null;
		this.meth = null;
		this.structFieldHandler = null;
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod om) {
		if (om.hasImplements() && om.getImplements().getParent() instanceof ServiceDefinition) {
			new DontGenerateJSServices(sv);
			return;
		}
		switchVars.clear();
		if (!om.isConverted()) {
			this.meth = null;
			return;
		}
		JSExpr container = null;
		String pkg = om.name().packageName().jsName();
		jse.ensurePackageExists(pkg, om.name().inContext.jsName());
		this.meth = jse.newFunction(pkg, om.name().container().jsName(), currentOA != null || om.contractMethod() != null || om.hasObject() || om.isEvent(), om.name().name);
		if (om.hasImplements()) {
			Implements impl = om.getImplements();
			this.methodMap.get(impl).add(om.name());
			if (impl instanceof HandlerImplements && ((HandlerImplements)impl).getParent() == null)
				container = new JSThis();
			else
				container = new JSFromCard();
		} else if (om.hasObject()) {
			this.methodMap.get(om.getObject()).add(om.name());
			container = new JSThis();
		} else if (om.isEvent()) {
			container = new JSThis();
		} else if (om.hasState()) {
			container = new JSLiteral("_0");
		}
		this.meth.argument("_cxt");
		int i;
		for (i=0;i<om.argCount();i++)
			this.meth.argument("_" + i);
		if (om.contractMethod() != null) {
			this.meth.argument("_" + i);
		}
		this.block = meth;
		this.state = new JSFunctionStateStore(meth, container);
	}

	@Override
	public void visitObjectCtor(ObjectCtor oc) {
		String pkg = oc.name().packageName().jsName();
		jse.ensurePackageExists(pkg, oc.name().inContext.jsName());
		this.meth = jse.newFunction(pkg, oc.name().container().jsName(), false, oc.name().name);
		this.meth.argument("_cxt");
		int i;
		for (i=0;i<oc.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		
		ObjectDefn od = oc.getObject();
		JSExpr ocret = meth.newOf(od.name());
		JSExpr container = ocret; 
		this.state = new JSFunctionStateStore(meth, container);
		this.state.objectCtor(ocret);
		for (ObjectContract ctr : od.contracts) {
			String cname = "_ctr_" + ctr.varName().var;
			meth.argument(cname);
			meth.copyContract(ocret, ctr.varName().var, cname);
		}
	}
	
	@Override
	public void visitStateDefinition(StateDefinition state) {
		if (this.state != null && this.state.ocret() != null) {
			new ObjectCtorStateGeneratorJS(this.state, sv, this.block);
		}
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
		if (state.ocret() != null)
			new ObjectCtorGeneratorJS(state, sv, this.meth);
		else
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
	public void leaveTupleMember(TupleMember tm) {
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
		this.templateCreator = null;
	}

	@Override
	public void leaveObjectCtor(ObjectCtor oc) {
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
		currentContract = jse.newClass(pkg, cd.name().jsName());
		jse.contract(cd);
		if (cd.type == ContractType.HANDLER)
			this.currentContract.inheritsFrom(new PackageName("IdempotentHandler"));
		JSMethodCreator ctrName = currentContract.createMethod("name", true);
		ctrName.returnObject(new JSString(cd.name().uniqueName()));
		JSMethodCreator methods = currentContract.createMethod("_methods", true);
		List<JSExpr> names = new ArrayList<>();
		for (ContractMethodDecl m : cd.methods)
			names.add(methods.string(m.name.name));
		methods.returnObject(methods.jsArray(names));
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		JSMethodCreator meth = currentContract.createMethod(cmd.name.name, true);
		meth.argument("_cxt");
		for (int k=0;k<cmd.args.size();k++) {
			meth.argument("_" + k);
		}
		meth.returnObject(new JSString("interface method for " + cmd.name.uniqueName()));
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		currentContract = null;
	}

	@Override
	public void visitAgentDefn(AgentDefinition ad) {
		String pkg = ad.name().container().jsName();
		jse.ensurePackageExists(pkg, pkg);
		agentCreator = jse.newClass(pkg, ad.name().jsName());
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.stateField();
		ctor.fieldObject("_contracts", "ContractStore");
		JSMethodCreator meth = agentCreator.createMethod("name", true);
		meth.argument("_cxt");
		meth.returnObject(new JSString(ad.name().uniqueName()));
		JSMethodCreator ctrProvider = agentCreator.createMethod("_contract", false);
		ctrProvider.argument("_cxt");
		ctrProvider.argument("_ctr");
		this.structFieldHandler = sf -> {
			if (sf.init != null) {
				new StructFieldGeneratorJS(state, sv, ctor, sf.name, new JSThis());
			}
		};
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		String pkg = cd.name().container().jsName();
		jse.ensurePackageExists(pkg, pkg);
		agentCreator = jse.newClass(pkg, cd.name().jsName());
		templateCreator = agentCreator;
		agentCreator.inheritsFrom(new PackageName("FLCard"));
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.fieldObject("_contracts", "ContractStore");
		if (!cd.templates.isEmpty()) {
			ctor.setField(new JSThis(), "_template", ctor.string(cd.templates.get(0).webinfo().id()));
		}
		ctor.stateField();
		JSMethodCreator meth = agentCreator.createMethod("name", true);
		meth.argument("_cxt");
		meth.returnObject(new JSString(cd.name().uniqueName()));
		JSMethodCreator ctrProvider = agentCreator.createMethod("_contract", false);
		ctrProvider.argument("_cxt");
		ctrProvider.argument("_ctr");
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(cd, methods);
		jse.methodList(cd.name(), methods);
		jse.eventMap(cd.name(), eventMap.get(cd));
		this.structFieldHandler = sf -> {
			if (sf.init != null) {
				new StructFieldGeneratorJS(state, sv, ctor, sf.name, new JSThis());
			}
		};
		containerIdx = new AtomicInteger(1);
	}
	
	@Override
	public void visitServiceDefn(ServiceDefinition ad) {
		new DontGenerateJSServices(sv);
	}
	
	@Override
	public void visitImplements(ImplementsContract ic) {
		CSName csn = (CSName)ic.name();
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.recordContract(ic.actualType().name().jsName(), csn.jsName());
		JSClassCreator svc = jse.newClass(csn.packageName().jsName(), csn.jsName());
		svc.arg("_card");
		svc.constructor().setField("_card", new JSLiteral("_card"));
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(ic, methods);
		jse.methodList(ic.name(), methods);
	}
	
	@Override
	public void visitProvides(Provides p) {
		CSName csn = (CSName)p.name();
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.recordContract(p.actualType().name().jsName(), csn.jsName());
		JSClassCreator svc = jse.newClass(csn.packageName().jsName(), csn.jsName());
		svc.arg("_card");
		svc.constructor().setField("_card", new JSLiteral("_card"));
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(p, methods);
		jse.methodList(p.name(), methods);
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi, StateHolder sh) {
		new HIGeneratorJS(sv, jse, methodMap, hi, sh);
	}

	@Override
	public void leaveProvides(Provides p) {
		this.block = null;
		this.evalRet = null;
		this.meth = null;
	}
	
	@Override
	public void visitRequires(RequiresContract rc) {
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.requireContract(rc.referAsVar, rc.actualType().name().jsName());
	}
	
	@Override
	public void leaveAgentDefn(AgentDefinition s) {
		agentCreator = null;
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		String name = "_updateDisplay";
		if (!isFirst) {
			name = "_updateTemplate" + t.position();
		}
		
		JSMethodCreator updateDisplay = templateCreator.createMethod(name, true);
		updateDisplay.argument("_cxt");
		updateDisplay.argument("_renderTree");
		Iterator<Link> links = null;
		Link n1 = null;
		NestingChain chain = t.nestingChain();
		if (chain != null) {
			links = chain.iterator();
			n1 = links.next();
			updateDisplay.argument(n1.name().var);
			updateDisplay.argument("_tc");
		}
		this.state = new JSFunctionStateStore(updateDisplay, new JSThis());
		JSExpr source;
		if (n1 != null) {
			Map<String, JSExpr> tom = new LinkedHashMap<>();
			source = updateDisplay.boundVar(n1.name().var);
			popVar(tom, n1, source);
			JSExpr tc = updateDisplay.boundVar("_tc");
			int pos = 0;
			while (links.hasNext())
				popVar(tom, links.next(), updateDisplay.arrayElt(tc, pos++));
			state.provideTemplateObject(tom);
		} else
			source = updateDisplay.literal("null");
		new TemplateProcessorJS(state, sv, templateCreator, containerIdx, updateDisplay, source, t);
	}
	
	private void popVar(Map<String, JSExpr> tom, Link l, JSExpr expr) {
		tom.put(l.name().var, expr);
	}

	@Override
	public void leaveCardDefn(CardDefinition s) {
		agentCreator = null;
		templateCreator = null;
		containerIdx = null;
	}

	@Override
	public void leaveObjectDefn(ObjectDefn s) {
		templateCreator = null;
		containerIdx = null;
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		if (involvesServices(e)) {
			new DontGenerateJSServices(sv);
			return;
		}
		UnitTestName clzName = e.name;
		if (currentOA != null)
			throw new NotImplementedException("I don't think you can nest a unit test in an accessor");
		String pkg = clzName.container().jsName();
		this.meth = jse.newFunction(pkg, pkg, false, clzName.baseName());
		this.block = meth;
		runner = meth.argument("runner");
		meth.clear();
		meth.initContext(e.name.packageName());
		this.state = new JSFunctionStateStore(meth, null);
		explodingMocks.clear();
		// Make sure we declare contracts first - others may use them
		for (UnitDataDeclaration udd : globalMocks) {
			if (udd.ofType.defn() instanceof ContractDecl)
				visitUnitDataDeclaration(udd);
		}
		for (UnitDataDeclaration udd : globalMocks) {
			if (!(udd.ofType.defn() instanceof ContractDecl))
				visitUnitDataDeclaration(udd);
		}
	}

	private boolean involvesServices(UnitTestCase e) {
		// This is actually more complicated than this, because the mocks could be global ...
		for (UnitTestStep s : e.steps) {
			if (s instanceof UnitDataDeclaration) {
				UnitDataDeclaration udd = (UnitDataDeclaration) s;
				if (udd.ofType.defn() instanceof ServiceDefinition)
					return true;
			}
		}
		return false;
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		if (meth == null) {
			globalMocks.add(udd);
			return;
		}
		NamedType objty = udd.ofType.defn();
		if (objty instanceof PolyInstance)
			objty = ((PolyInstance)objty).struct();
		if (objty instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) objty;
			JSExpr mock;
			if (cd.type == ContractType.HANDLER)
				mock = meth.mockHandler((SolidName) objty.name());
			else
				mock = meth.mockContract((SolidName) objty.name());
			state.addMock(udd, mock);
			explodingMocks.add(mock);
		} else if (objty instanceof AgentDefinition) {
			JSExpr obj = meth.createAgent((CardName) objty.name());
			state.addMock(udd, obj);
		} else if (objty instanceof CardDefinition) {
			JSExpr obj = meth.createCard((CardName) objty.name());
			state.addMock(udd, obj);
		} else if (objty instanceof StructDefn || objty instanceof UnionTypeDefn) {
			new UDDGeneratorJS(sv, meth, state, this.block);
		} else if (objty instanceof ObjectDefn) {
			new UDDGeneratorJS(sv, meth, state, this.block);
		} else if (objty instanceof HandlerImplements) {
			new UDDGeneratorJS(sv, meth, state, this.block);
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
			throw new RuntimeException("not handled: " + objty + " of " + objty.getClass());
		}
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
	public void leaveUnitTest(UnitTestCase e) {
		for (JSExpr m : explodingMocks) {
			meth.assertSatisfied(m.asVar());
		}
		meth.testComplete();
		meth = null;
		state = null;
	}

	@Override
	public void traversalDone() {
		jse.complete();
	}
	
	@Override
	public void result(Object r) {
		if (r != null) {
			block.returnObject((JSExpr)r);
		}
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv) {
		return new JSGenerator(meth, runner, nv, null);
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv, JSFunctionState state) {
		return new JSGenerator(meth, runner, nv, state);
	}
}
