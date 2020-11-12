package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSCompare;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSFromCard;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.EventHolder;
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
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.StructFieldHandler;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain.Link;
import org.flasck.jvm.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.NotImplementedException;

public class JSGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	private final static Logger logger = LoggerFactory.getLogger("Generator");
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

	private final RepositoryReader repository;
	private final JSStorage jse;
	private final NestedVisitor sv;
	private final Map<EventHolder, EventTargetZones> eventMap;
	private JSMethodCreator meth;
	private JSBlockCreator block;
	private JSExpr runner;
	private final Map<Slot, JSExpr> switchVars = new HashMap<>();
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
	private boolean currentContractIsHandler;
	private boolean testServices;
	private UnitTestName testName;
	private final List<JSExpr> utsteps = new ArrayList<>();
	private JSClassCreator utclz;

	public JSGenerator(RepositoryReader repository, JSStorage jse, StackVisitor sv, Map<EventHolder, EventTargetZones> eventMap) {
		this.repository = repository;
		this.jse = jse;
		this.sv = sv;
		this.eventMap = eventMap;
		if (sv != null)
			sv.push(this);
	}

	public JSGenerator(JSMethodCreator meth, JSExpr runner, NestedVisitor sv, JSFunctionState state) {
		this.repository = null;
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
		if (fn.intros().isEmpty() || !fn.generate) {
			this.meth = null;
			return;
		}
		String pkg = fn.name().packageName().jsName();
		NameOfThing cxName;
		if (fn.name().containingCard() != null)
			cxName = fn.name().wrappingObject();
		else
			cxName = fn.name().inContext;
		jse.ensurePackageExists(pkg, cxName.jsName());
		this.meth = jse.newFunction(fn.name(), pkg, cxName, fn.hasState(), fn.name().name);
		
		this.meth.argument("_cxt");
		this.meth.argumentList();
		for (int i=0;i<fn.argCountWithoutHolder();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		this.state = new JSFunctionStateStore(meth);
		if (fn.hasState()) {
			loadContainers(this.state, fn.name());
		}
	}

	// When generating a tuple assignment, we have to create a closure which is the "main thing"
	// and then (below) a closure extracting each member from this thing 
	@Override
	public void visitTuple(TupleAssignment e) {
		switchVars.clear();
		String pkg = e.name().packageName().jsName();
		NameOfThing cxName = e.name().inContext;
		jse.ensurePackageExists(pkg, cxName.jsName());
		this.meth = jse.newFunction(e.name(), pkg, cxName, false, e.name().name);
			
		this.meth.argument("_cxt");
		this.meth.argumentList();
		this.block = meth;
		this.state = new JSFunctionStateStore(meth);
		sv.push(new ExprGeneratorJS(state, sv, this.block, false));
	}
	
	@Override
	public void visitTupleMember(TupleMember e) {
		switchVars.clear();
		String pkg = e.name().packageName().jsName();
		NameOfThing cxName = e.name().inContext;
		jse.ensurePackageExists(pkg, cxName.jsName());
		this.meth = jse.newFunction(e.name(), pkg, cxName, false, e.name().name);
			
		this.meth.argument("_cxt");
		this.meth.argumentList();
		this.block = meth;
		this.state = new JSFunctionStateStore(meth);
		this.meth.returnObject(meth.defineTupleMember(e));
//		sv.push(new ExprGeneratorJS(state, sv, this.block));
	}
	
	@Override
	public void visitStructDefn(StructDefn obj) {
		if (!obj.generate)
			return;
		String pkg = ((SolidName)obj.name()).packageName().jsName();
		jse.ensurePackageExists(pkg, obj.name().container().jsName());
		jse.struct(obj);
		JSClassCreator ctr = jse.newClass(pkg, obj.name());
		ctr.inheritsFrom(null, J.JVM_FIELDS_CONTAINER_WRAPPER);
		ctr.implementsJava(J.AREYOUA);
		ctr.implementsJava(J.ISDATA);
		ctr.inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
		ctr.wantsTypeName();
		JSMethodCreator ctor = ctr.constructor();
		JSVar cx = ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.superArg(cx);
		ctor.superArg(ctor.string(obj.name.uniqueName()));
		ctor.stateField(true);
		ctor.storeField(true, this.evalRet, "_type", ctor.string(obj.name.uniqueName()));
		ctor.returnVoid();
		JSMethodCreator areYouA = ctr.createMethod("_areYouA", true);
		areYouA.argument(J.EVALCONTEXT, "_cxt");
		areYouA.argument(J.STRING, "ty");
		areYouA.returnsType("boolean");
		JSExpr aya = areYouA.arg(1);
		JSIfCreator ifblk = areYouA.ifTrue(new JSCompare(aya, areYouA.string(obj.name().jsName())));
		ifblk.trueCase().returnObject(ifblk.trueCase().literal("true"));
		JSBlockCreator fc = ifblk.falseCase();
		for (UnionTypeDefn u : repository.unionsContaining(obj)) {
			JSIfCreator ifu = fc.ifTrue(new JSCompare(aya, areYouA.string(u.name().jsName())));
			ifu.trueCase().returnObject(ifblk.trueCase().literal("true"));
			fc = ifu.falseCase();
		}
		fc.returnObject(fc.literal("false"));
		this.meth = ctr.createMethod("eval", false);
		this.meth.argument(J.FLEVALCONTEXT, "_cxt");
		this.meth.argumentList();
		this.evalRet = meth.newOf(obj.name());
		this.block = meth;
		this.structFieldHandler = sf -> {
			if (sf.name.equals("id"))
				return;
			if (sf.init == null) {
				JSExpr arg = this.meth.argument(sf.name);
				this.meth.storeField(false, this.evalRet, sf.name, arg);
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
		templateCreator = jse.newClass(pkg, obj.name());
		templateCreator.inheritsFrom(new PackageName("FLObject"), J.FLOBJECT);
		templateCreator.implementsJava(J.AREYOUA);
		templateCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
		templateCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.FLCARD), "_card");
		for (ObjectContract c : obj.contracts) {
			templateCreator.field(false, Access.PROTECTED, c.implementsType().defn().name(), c.varName().baseName());
		}

		JSMethodCreator areYouA = templateCreator.createMethod("_areYouA", true);
		areYouA.argument(J.EVALCONTEXT, "_cxt");
		areYouA.argument(J.STRING, "ty");
		areYouA.returnsType("boolean");
		areYouA.returnCompare(areYouA.arg(1), areYouA.string(obj.name().jsName()));
		JSMethodCreator ud = templateCreator.createMethod("_updateDisplay", true);
		ud.argument("_cxt");
		ud.returnsType("void");
		JSIfCreator ifcard = ud.ifTrue(ud.literal("this._card"));
		ifcard.trueCase().assertable(ud.literal("this._card"), "_updateDisplay", ud.literal("this._card._renderTree"));
		ud.returnVoid();
		JSMethodCreator ctor = templateCreator.constructor();
		JSVar cx = ctor.argument(J.FLEVALCONTEXT, "_cxt");
		JSVar ic = ctor.argument("_incard");
		ctor.superArg(cx);
		ctor.superArg(ic);
		ctor.setField(true, "_card", ctor.arg(1));
		ctor.stateField(true);
		ctor.returnVoid();
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(obj, methods);
		jse.eventMap(obj.name(), eventMap.get(obj));
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
		NameOfThing cxName = sf.name().container();
		JSMethodCreator meth = jse.newFunction(null, pkg, cxName, true, "_field_" + sf.name);
		meth.argument("_cxt");
		meth.argumentList();
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
		if (!om.generate)
			return;
		logger.info("visiting object method " + om.name());
		switchVars.clear();
		if (!om.isConverted()) {
			this.meth = null;
			return;
		}
		String pkg = om.name().packageName().jsName();
		jse.ensurePackageExists(pkg, om.name().inContext.jsName());
		this.meth = jse.newFunction(om.name(), pkg, om.name().container(), currentOA != null || om.contractMethod() != null || om.hasObject() || om.isEvent() || om.hasState(), om.name().name);
		if (om.hasImplements()) {
			if (om.getImplements().getParent() instanceof ServiceDefinition) {
				this.meth.noJS();
			}
			Implements impl = om.getImplements();
			this.methodMap.get(impl).add(om.name());
		} else if (om.hasObject()) {
			this.methodMap.get(om.getObject()).add(om.name());
		}
		this.meth.argument("_cxt");
		this.meth.argumentList();
		int i;
		for (i=0;i<om.argCountWithoutHolder();i++)
			this.meth.argument("_" + i);
		if (om.contractMethod() != null) {
			this.meth.argument("_" + i);
		}
		this.block = meth;
		this.state = new JSFunctionStateStore(meth);
		loadContainers(state, om.name());
	}

	@Override
	public void visitObjectCtor(ObjectCtor oc) {
		if (!oc.generate)
			return;
		switchVars.clear();
		String pkg = oc.name().packageName().jsName();
		jse.ensurePackageExists(pkg, oc.name().inContext.jsName());
		this.meth = jse.newFunction(null, pkg, oc.name().container(), false, oc.name().name);
		this.meth.argumentList();
		this.meth.argument(J.FLEVALCONTEXT, "_cxt");
		this.meth.argument(J.FLCARD, "_card");
		int i;
		for (i=0;i<oc.argCount();i++)
			this.meth.argument("_" + i);
		this.block = meth;
		
		ObjectDefn od = oc.getObject();
		JSExpr ocret = meth.newOf(od.name(), Arrays.asList(this.meth.arg(1)));
		JSExpr ocmsgs = meth.makeArray(new ArrayList<JSExpr>());
		this.state = new JSFunctionStateStore(meth);
		loadContainers(state, oc.name());
		this.state.objectCtor(ocret, ocmsgs);
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
			if (((ArgSlot)s).isContainer())
				continue;
			switchVars.put(s, new JSVar("_" + switchVars.size()));
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
	public void withConstructor(NameOfThing string) {
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
		this.block.bindVar(slot, switchVars.get(slot), var);
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
		currentContract = jse.newClass(pkg, cd.name());
		currentContract.justAnInterface();
		JSMethodCreator ctor = currentContract.constructor();
		ctor.argument(J.FLEVALCONTEXT, "_cxt");
		jse.contract(cd);
		if (cd.type == ContractType.HANDLER) {
			this.currentContractIsHandler = true;
			this.currentContract.inheritsFrom(new PackageName("IdempotentHandler"), J.OBJECT);
			this.currentContract.implementsJava(J.IDEMPOTENTHANDLER);
		}
		JSMethodCreator ctrName = currentContract.createMethod("name", true);
		ctrName.returnObject(new JSString(cd.name().uniqueName()));
		JSMethodCreator methods = currentContract.createMethod("_methods", true);
		methods.noJVM();
		List<JSExpr> names = new ArrayList<>();
		for (ContractMethodDecl m : cd.methods)
			names.add(methods.string(m.name.name));
		methods.returnObject(methods.jsArray(names));
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		currentContract.field(true, Access.PUBLICSTATIC, new PackageName("int"), "_nf_"+cmd.name.name, cmd.args.size());
		JSMethodCreator meth = currentContract.createMethod(cmd.name.name, true);
		meth.argument(J.FLEVALCONTEXT, "_cxt");
		meth.argumentList();
		for (int k=0;k<cmd.args.size();k++) {
			meth.argument("_" + k);
		}
		meth.handlerArg();
		meth.returnObject(new JSString("interface method for " + cmd.name.uniqueName()));

		if (this.currentContractIsHandler && (cmd.name.name.equals("success") || cmd.name.name.contentEquals("failure")))
			meth.noJVM();
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		currentContract.constructor().returnVoid();
		currentContract = null;
		this.currentContractIsHandler = false;
	}

	@Override
	public void visitAgentDefn(AgentDefinition ad) {
		String pkg = ad.name().container().jsName();
		jse.ensurePackageExists(pkg, pkg);
		agentCreator = jse.newClass(pkg, ad.name());
		agentCreator.inheritsFrom(null, J.FLAGENT);
		agentCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
		agentCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.CONTRACTSTORE), "store");
		JSMethodCreator ctor = agentCreator.constructor();
		JSVar ctrCxt = ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.superArg(ctrCxt);
		ctor.stateField(true);
		ctor.fieldObject("_contracts", new PackageName("ContractStore"));
		JSMethodCreator updateDisplay = agentCreator.createMethod("_updateDisplay", true);
		updateDisplay.argument(J.FLEVALCONTEXT, "_cxt");
		updateDisplay.argument(J.RENDERTREE, "_renderTree");
		updateDisplay.returnsType("void");
		updateDisplay.returnVoid();
		JSMethodCreator meth = agentCreator.createMethod("name", true);
		meth.argument("_cxt");
		meth.returnObject(new JSString(ad.name().uniqueName()));
		JSMethodCreator ctrProvider = agentCreator.createMethod("_contract", false);
		ctrProvider.noJVM();
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
		agentCreator = jse.newClass(pkg, cd.name());
		templateCreator = agentCreator;
		agentCreator.inheritsFrom(new PackageName("FLCard"), J.FLCARD);
		agentCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
		agentCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.CONTRACTSTORE), "store");
		JSMethodCreator ctor = agentCreator.constructor();
		JSVar ctrCxt = ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.superArg(ctrCxt);
		ctor.fieldObject("_contracts", new PackageName("ContractStore"));
		if (!cd.templates.isEmpty()) {
			ctor.superArg(ctor.string(cd.templates.get(0).webinfo().id()));
			ctor.setField(new JSThis(), "_template", ctor.string(cd.templates.get(0).webinfo().id()));
		} else {
			ctor.superArg(ctor.string(null));
			JSMethodCreator updateDisplay = agentCreator.createMethod("_updateDisplay", true);
			updateDisplay.argument(J.FLEVALCONTEXT, "_cxt");
			updateDisplay.argument(J.RENDERTREE, "_renderTree");
			updateDisplay.returnsType("void");
			updateDisplay.returnVoid();
		}
		ctor.stateField(true);
		JSMethodCreator meth = agentCreator.createMethod("name", true);
		meth.argument("_cxt");
		meth.returnObject(new JSString(cd.name().uniqueName()));
		JSMethodCreator ctrProvider = agentCreator.createMethod("_contract", false);
		ctrProvider.noJVM();
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
	public void visitServiceDefn(ServiceDefinition sd) {
		String pkg = sd.name().container().jsName();
		jse.ensurePackageExists(pkg, pkg);
		agentCreator = jse.newClass(pkg, sd.name());
		agentCreator.notJS();
		templateCreator = agentCreator;
		agentCreator.inheritsFrom(null, J.CONTRACT_HOLDER);
		agentCreator.inheritsField(true, Access.PROTECTED, new PackageName(J.CONTRACTSTORE), "store");
		JSMethodCreator ctor = agentCreator.constructor();
		JSVar ctrCxt = ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.superArg(ctrCxt);
		JSMethodCreator meth = agentCreator.createMethod("name", true);
		meth.argument("_cxt");
		meth.returnObject(new JSString(sd.name().uniqueName()));
		containerIdx = new AtomicInteger(1);
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(sd, methods);
	}
	
	@Override
	public void visitImplements(ImplementsContract ic) {
		CSName csn = (CSName)ic.name();
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.recordContract(ic.actualType().name(), csn);
		JSClassCreator svc = jse.newClass(csn.packageName().jsName(), csn);
		JSMethodCreator svcCtor = svc.constructor();
		svcCtor.argument(J.FLEVALCONTEXT, "_cxt");
		svc.field(true, Access.PRIVATE, new PackageName(J.OBJECT), "_card");
		svcCtor.argument(J.OBJECT, "_incard");
		svcCtor.setField(false, "_card", new JSVar("_incard"));
		svcCtor.returnVoid();
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(ic, methods);
		jse.methodList(ic.name(), methods);
	}
	
	@Override
	public void visitProvides(Provides p) {
		CSName csn = (CSName)p.name();
		JSBlockCreator ctor = agentCreator.constructor();
		ctor.recordContract(p.actualType().name(), csn);
		JSClassCreator svc = jse.newClass(csn.packageName().jsName(), csn);
		if (!agentCreator.wantJS())
			svc.notJS();
		JSMethodCreator svcCtor = svc.constructor();
		svcCtor.argument(J.FLEVALCONTEXT, "_cxt");
		svc.field(true, Access.PRIVATE, new PackageName(J.OBJECT), "_card");
		// TODO: we need to "declare" the field _card here for the benefit of the JVM generator
		// TODO: we probably also need to start declaring base classes - because that is currently assumed to be struct
		svcCtor.argument(J.OBJECT, "_incard");
		svcCtor.setField(false, "_card", new JSVar("_incard"));
		svcCtor.returnVoid();
		List<FunctionName> methods = new ArrayList<>();
		methodMap.put(p, methods);
		if (agentCreator.wantJS()) {
			jse.methodList(p.name(), methods);
		}
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi) {
		String pkg = hi.name().packageName().jsName();
		jse.ensurePackageExists(pkg, hi.name().container().jsName());
		new HIGeneratorJS(sv, jse, methodMap, hi);
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
		ctor.requireContract(rc.referAsVar, rc.actualType().name());
	}
	
	@Override
	public void leaveAgentDefn(AgentDefinition s) {
		agentCreator.constructor().returnVoid();
		agentCreator = null;
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		String name = "_updateDisplay";
		if (!isFirst) {
			name = "_updateTemplate" + t.position();
		}
		
		JSMethodCreator updateDisplay = templateCreator.createMethod(name, true);
		updateDisplay.argument(J.FLEVALCONTEXT, "_cxt");
		updateDisplay.argument(J.RENDERTREE, "_renderTree");
		Iterator<Link> links = null;
		Link n1 = null;
		NestingChain chain = t.nestingChain();
		if (chain != null) {
			links = chain.iterator();
			n1 = links.next();
			updateDisplay.argument(n1.name().var);
			updateDisplay.argument(List.class.getName(), "_tc");
		}
		updateDisplay.returnsType("void");
		this.state = new JSFunctionStateStore(updateDisplay);
		this.loadContainers(state, FunctionName.function(t.kw, templateCreator.name(), name));
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
		agentCreator.constructor().returnVoid();
		agentCreator = null;
		templateCreator = null;
		containerIdx = null;
	}

	@Override
	public void leaveServiceDefn(ServiceDefinition s) {
		agentCreator.constructor().returnVoid();
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
		UnitTestName clzName = e.name;
		if (currentOA != null)
			throw new NotImplementedException("I don't think you can nest a unit test in an accessor");
		NameOfThing pkg = clzName.container();
		jse.ensurePackageExists(pkg.jsName(), e.name.container().jsName());
		this.utclz = jse.newUnitTest(e);
		utclz.field(false, Access.PRIVATE, new PackageName(J.TESTHELPER), "_runner");
		JSMethodCreator ctor = utclz.constructor();
		JSVar r = ctor.argument(J.TESTHELPER, "runner");
		ctor.argument(J.FLEVALCONTEXT, "_cxt");
		ctor.setField(false, "_runner", r);
		ctor.initContext(false);
//		ctor.clear();
		ctor.returnVoid();
		this.meth = utclz.createMethod("dotest", true);
		this.meth.argument(J.FLEVALCONTEXT, "_cxt");
		meth.returnsType(List.class.getName());
		this.runner = meth.field("_runner");
		this.meth.helper(runner);
		this.testServices = true;
		this.testName = clzName;
		if (involvesServices(e)) {
			this.meth.noJS();
			this.testServices = false;
		}
		this.block = meth;
//		meth.clear();
//		meth.initContext(false);
		this.state = new JSFunctionStateStore(meth);
		this.state.container(new PackageName("_DisplayUpdater"), runner);
		explodingMocks.clear();
		// Make sure we declare contracts first - others may use them
		for (UnitDataDeclaration udd : globalMocks) {
			if (udd.ofType.defn() instanceof ContractDecl)
				visitUnitDataDeclaration(udd);
		}
		// and then declare non-contracts
		for (UnitDataDeclaration udd : globalMocks) {
			if (!(udd.ofType.defn() instanceof ContractDecl))
				visitUnitDataDeclaration(udd);
		}
		utsteps.clear();

	}

	private boolean involvesServices(TestStepHolder e) {
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

	// handle global mocks ...
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		UDDGeneratorJS.handleUDD(sv, meth, state, this.block, globalMocks, explodingMocks, udd);
	}
	
	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		UnitTestStepGenerator sg = new UnitTestStepGenerator(sv, jse, utclz, meth, state, this.block, this.runner, globalMocks, explodingMocks, testServices, testName, utsteps.size()+1);
		utsteps.add(meth.string(sg.name()));
	}

	@Override
	public void leaveUnitTest(TestStepHolder e) {
		for (JSExpr m : explodingMocks) {
			meth.assertSatisfied(m);
		}
		// but we need the runner to call this itself
//		meth.testComplete();
		state.meth().returnObject(state.meth().makeArray(utsteps));
		meth = null;
		state = null;
	}

	@Override
	public void visitSystemTest(SystemTest st) {
		new SystemTestGenerator(sv, jse, st);
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

	private void loadContainers(JSFunctionState fs, FunctionName name) {
		NameOfThing top = name.wrappingObject();
		JSExpr first = null;
		if (top == null)
			return; // there are no containers
		
		// So top will always be "this" if it has one (or the equivalent if not)
		if (name.name.startsWith("_ctor_")) {
			first = new JSVar("v1"); // I'm concerned about how tightly coupled this is ... thoughts?
			fs.container(top, first);
		} else
			fs.container(top, new JSThis());
		do {
			if (top instanceof CardName || top instanceof ObjectName) {
				if (runner != null)
					fs.container(new PackageName("_DisplayUpdater"), runner);
				else {
					if (first == null)
						first = new JSThis();
					fs.container(new PackageName("_DisplayUpdater"), first);
				}
			}
			top = top.container();
			if (top instanceof CardName || top instanceof ObjectName) {
				JSFromCard fc = new JSFromCard(top);
				if (first == null)
					first = fc;
				fs.container(top, fc);
			}
		} while (top != null && !(top instanceof PackageName));
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv) {
		return new JSGenerator(meth, runner, nv, null);
	}

	public static JSGenerator forTests(JSMethodCreator meth, JSExpr runner, NestedVisitor nv, JSFunctionState state) {
		return new JSGenerator(meth, runner, nv, state);
	}
}
