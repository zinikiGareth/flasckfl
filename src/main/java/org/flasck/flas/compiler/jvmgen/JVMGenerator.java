package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HLSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.StructFieldHandler;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.testrunner.TestRunner;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class JVMGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public static class XCArg {
		public final int arg;
		public final IExpr expr;
	
		public XCArg(int arg, IExpr expr) {
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

	private final ByteCodeStorage bce;
	private final StackVisitor sv;
	private MethodDefiner meth;
	private IExpr runner;
	private ByteCodeSink clz;
	private IExpr fcx;
	private Var fargs;
	private final Map<Slot, IExpr> switchVars = new HashMap<>();
	private FunctionState fs;
	private List<IExpr> currentBlock;
	private ByteCodeSink oaClz;
	private ObjectAccessor currentOA;
	private StructFieldHandler structFieldHandler;
	private Set<UnitDataDeclaration> globalMocks = new HashSet<UnitDataDeclaration>();
	private final List<Var> explodingMocks = new ArrayList<>();
	private boolean isStandalone;
	private static final boolean leniency = false;
	private NewMethodDefiner agentctor;
	private ByteCodeSink agentClass;
	private Var agentcx;

	public JVMGenerator(ByteCodeStorage bce, StackVisitor sv) {
		this.bce = bce;
		this.sv = sv;
		sv.push(this);
	}
	
	private JVMGenerator(MethodDefiner meth, IExpr runner, Var args) {
		this.sv = new StackVisitor();
		sv.push(this);
		this.bce = null;
		this.meth = meth;
		this.runner = runner;
		this.fcx = runner;
		this.fargs = args;
		this.currentBlock = new ArrayList<IExpr>();
	}

	private JVMGenerator(ByteCodeSink clz) {
		this.sv = new StackVisitor();
		sv.push(this);
		this.bce = null;
		this.clz = clz;
	}

	@Override
	public void result(Object r) {
		if (r != null)
			currentBlock.add((IExpr) r);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty()) {
			this.clz = null;
			this.meth = null;
			return;
		}
		GenericAnnotator ann;
		if (oaClz != null) {
			this.clz = oaClz;
			ann = GenericAnnotator.newMethod(clz, false, currentOA.name().name);
		} else {
			this.clz = bce.newClass(fn.name().javaClassName());
			this.clz.generateAssociatedSourceFile();
			IFieldInfo fi = this.clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			fi.constValue(fn.argCount());
			ann = GenericAnnotator.newMethod(clz, true, "eval");
		}
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		PendingVar argsArg = ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		fcx = cxArg.getVar();
		fargs = argsArg.getVar();
		switchVars.clear();
		fs = new FunctionState(meth, (Var)fcx, null, fargs, runner );
		currentBlock = new ArrayList<IExpr>();
		if (oaClz != null) {
			StateHolder od = currentOA.getObject();
			if (od.state() != null) {
				fs.provideStateObject(meth.getField("state"));
			}
		}
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod om) {
		if (!om.isConverted()) {
			this.clz = null;
			this.meth = null;
			return;
		}
		GenericAnnotator ann;
		boolean wantObj, haveThis, wantParent;
		if (isStandalone) {
			this.clz = bce.newClass(om.name().javaClassName());
			this.clz.generateAssociatedSourceFile();
			IFieldInfo fi = this.clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			fi.constValue(om.argCount());
			ann = GenericAnnotator.newMethod(clz, true, "eval");
			wantObj = om.name().codeType.hasThis();
			haveThis = false;
			wantParent = false;
		} else {
			if (om.hasObject()) {
				this.clz = bce.get(om.getObject().name().javaName());
				wantParent = false;
			} else if (om.hasImplements()) {
				this.clz = bce.get(om.getImplements().name().javaClassName());
				wantParent = true;
			} else
				throw new NotImplementedException("Don't have one of those");
			IFieldInfo fi = this.clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "_nf_" + om.name().name);
			fi.constValue(om.argCount());
			ann = GenericAnnotator.newMethod(clz, false, om.name().name);
			wantObj = false; // because we already have "this"
			haveThis = true;
		}
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		PendingVar myThis = null;
		if (wantObj)
			myThis = ann.argument(J.OBJECT, "obj");
		PendingVar argsArg = ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		fcx = cxArg.getVar();
		fargs = argsArg.getVar();
		switchVars.clear();
		IExpr thisVar;
		if (myThis != null)
			thisVar = myThis.getVar();
		else if (haveThis) {
			thisVar = meth.myThis();
			if (wantParent) {
				NameOfThing cardClz = om.name().containingCard();
				thisVar = meth.castTo(meth.getField(thisVar, "_card"), cardClz.javaName());
			}
		} else
			thisVar = null;
		fs = new FunctionState(meth, (Var)fcx, thisVar, fargs, runner);
		currentBlock = new ArrayList<IExpr>();

		if (om.hasObject()) {
			ObjectDefn od = om.getObject();
			if (od.state() != null) {
				fs.provideStateObject(meth.castTo(meth.myThis(), J.FIELDS_CONTAINER_WRAPPER));
			}
		} else if (om.hasImplements()) {
			Implements m = om.getImplements();
			if (((StateHolder)m.getParent()).state() != null) {
				fs.provideStateObject(meth.castTo(meth.getField("_card"), J.FIELDS_CONTAINER_WRAPPER));
			}
		} else if (!isStandalone)
			throw new NotImplementedException("Don't have one of those");

	}
	
	@Override
	public void visitHandlerLambda(Pattern p) {
		if (fs != null) {
			// method with lambdas
			if (p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)p;
				fs.bindVar(currentBlock, tp.var.var, new HLSlot(tp.var.var), meth.aNull());
			} else
				throw new NotImplementedException("support varpattern " + p);
		}
	}
	
	@Override
	public void visitTuple(TupleAssignment ta) {
		this.clz = bce.newClass(ta.name().javaClassName());
		this.clz.generateAssociatedSourceFile();
		IFieldInfo fi = this.clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
		fi.constValue(0);
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "eval");
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		PendingVar argsArg = ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		fcx = cxArg.getVar();
		fargs = argsArg.getVar();
		switchVars.clear();
		fs = new FunctionState(meth, (Var)fcx, null, fargs, runner);
		currentBlock = new ArrayList<IExpr>();
		new ExprGenerator(fs, sv, currentBlock, false);
	}
	
	@Override
	public void visitTupleMember(TupleMember tm) {
		this.clz = bce.newClass(tm.name().javaClassName());
		this.clz.generateAssociatedSourceFile();
		IFieldInfo fi = this.clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
		fi.constValue(0);
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "eval");
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		PendingVar argsArg = ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		fcx = cxArg.getVar();
		fargs = argsArg.getVar();
		switchVars.clear();
		fs = new FunctionState(meth, (Var)fcx, null, fargs, runner);
		currentBlock = new ArrayList<IExpr>();
		currentBlock.add(meth.returnObject(meth.callInterface(J.OBJECT, fcx, "tupleMember", meth.callStatic(tm.ta.exprFnName().javaClassName(), J.OBJECT, "eval", fcx, meth.arrayOf(J.OBJECT)), meth.intConst(tm.which))));
	}
	
	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
		this.isStandalone = true;
	}
	
	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
		this.isStandalone = false;
	}
	
	@Override
	public void visitObjectAccessor(ObjectAccessor oa) {
		this.currentOA = oa;
		this.oaClz = bce.get(oa.name().container().javaName());
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		for (Slot slot : slots) {
			ArgSlot s = (ArgSlot) slot;
			IExpr in = meth.arrayItem(J.OBJECT, fargs, s.argpos());
			switchVars.put(s, in);
		}
	}

	@Override
	public void switchOn(Slot slot) {
		sv.push(new HSIGenerator(fs, sv, switchVars, slot, currentBlock));
	}

	@Override
	public void withConstructor(String ctor) {
	}
	
	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
	}

	@Override
	public void matchNumber(int i) {
	}

	@Override
	public void matchString(String s) {
	}

	@Override
	public void matchDefault() {
	}

	@Override
	public void defaultCase() {
	}

	@Override
	public void errorNoCase() {
	}

	@Override
	public void bind(Slot slot, String var) {
		fs.bindVar(currentBlock, var, slot, switchVars.get(slot));
	}

	// This is needed here as well as HSIGenerator to handle the no-switch case
	@Override
	public void startInline(FunctionIntro fi) {
		new GuardGenerator(fs, sv, currentBlock);
	}

	@Override
	public void endSwitch() {
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		makeBlock(meth, currentBlock).flush();
		currentBlock.clear();
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}
	
	@Override
	public void leaveObjectAccessor(ObjectAccessor oa) {
		this.oaClz = null;
	}
	
	@Override
	public void leaveObjectMethod(ObjectMethod om) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		if (currentBlock.isEmpty()) {
			// if we didn't generate anything, it's because we didn't have any messages
			// so return an empty list
			meth.returnObject(meth.arrayOf(J.OBJECT)).flush();
		} else {
			makeBlock(meth, currentBlock).flush();
		}
		currentBlock.clear();
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}

	@Override
	public void tupleExprComplete(TupleAssignment e) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		IExpr r = currentBlock.remove(currentBlock.size()-1);
		currentBlock.add(meth.returnObject(r));
		makeBlock(meth, currentBlock).flush();
		currentBlock.clear();
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}

	@Override
	public void leaveTupleMember(TupleMember sd) {
		if (meth == null) {
			// we elected not to generate, so just forget it ...
			return;
		}
		makeBlock(meth, currentBlock).flush();
		currentBlock.clear();
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}

	@Override
	public void visitStructDefn(StructDefn sd) {
		if (!sd.generate)
			return;
		String clzName = sd.name().javaName();
		ByteCodeSink bcc = bce.newClass(clzName);
		bcc.superclass(J.JVM_FIELDS_CONTAINER_WRAPPER);
		bcc.generateAssociatedSourceFile();
		bcc.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs").constValue(sd.argCount());
		bcc.inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.JVM_FIELDS_CONTAINER_WRAPPER, "<init>", cx.getVar()).flush();
			ctor.returnVoid().flush();
		}
		{ // eval(cx)
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar pargs = gen.argument("[" + J.OBJECT, "args");
			gen.returns(J.OBJECT);
			MethodDefiner meth = gen.done();
			Var args = pargs.getVar();
			Var ret = meth.avar(clzName, "ret");
			meth.assign(ret, meth.makeNew(clzName, cx.getVar())).flush();
			this.fs = new FunctionState(meth, cx.getVar(), null, null, runner);
			this.meth = meth;
			fs.evalRet = ret;
			this.currentBlock = new ArrayList<IExpr>();
			AtomicInteger ai = new AtomicInteger(0);
			this.structFieldHandler = sf -> {
				if (sf.name.equals("id"))
					return;
				if (sf.init != null) {
					new StructFieldGenerator(this.fs, sv, this.currentBlock, sf.name);
				} else {
					IExpr arg = meth.arrayElt(args, meth.intConst(ai.getAndIncrement()));
					IExpr svar = meth.getField(ret, "state");
					meth.callInterface("void", svar, "set", meth.stringConst(sf.name), arg).flush();
				}
			};
		}
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn od) {
		if (!od.generate)
			return;
		String clzName = od.name().javaName();
		ByteCodeSink bcc = bce.newClass(clzName);
		bcc.superclass(J.JVM_FIELDS_CONTAINER_WRAPPER);
		bcc.generateAssociatedSourceFile();
		bcc.inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		{ // ctor(cx)
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.JVM_FIELDS_CONTAINER_WRAPPER, "<init>", cx.getVar()).flush();
			ctor.returnVoid().flush();
		}
		{ // eval(cx)
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			gen.returns(J.OBJECT);
			MethodDefiner meth = gen.done();
			Var ret = meth.avar(clzName, "ret");
			meth.assign(ret, meth.makeNew(clzName, cx.getVar())).flush();
			this.fs = new FunctionState(meth, cx.getVar(), null, null, runner);
			this.meth = meth;
			fs.evalRet = ret;
			this.currentBlock = new ArrayList<IExpr>();
		}
		this.structFieldHandler = sf -> {
			if (sf.init != null)
				new StructFieldGenerator(this.fs, sv, this.currentBlock, sf.name);
		};
	}
	
	@Override
	public void visitAgentDefn(AgentDefinition ad) {
//		if (!od.generate)
//			return;
		String clzName = ad.name().javaName();
		agentClass = bce.newClass(clzName);
		agentClass.superclass(J.CONTRACT_HOLDER);
		agentClass.generateAssociatedSourceFile();
		agentClass.inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		agentClass.inheritsField(true, Access.PRIVATE, J.CONTRACTSTORE, "store");
		{ // ctor(cx)
			GenericAnnotator gen = GenericAnnotator.newConstructor(agentClass, false);
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			agentctor = gen.done();
			agentcx = cx.getVar();
			agentctor.callSuper("void", J.CONTRACT_HOLDER, "<init>", agentcx).flush();
		}
//		this.structFieldHandler = sf -> {
//			if (sf.init != null)
//				new StructFieldGenerator(this.fs, sv, this.currentBlock, sf.name);
//		};
	}
	
	@Override
	public void visitProvides(Provides p) {
		CSName csn = (CSName) p.name();
		ByteCodeSink providesClass = bce.newClass(csn.javaClassName());
		providesClass.superclass(J.OBJECT);
		providesClass.generateAssociatedSourceFile();
		IFieldInfo card = providesClass.defineField(true, Access.PRIVATE, J.OBJECT, "_card"); // Probably should be some superclass of card, service, agent ...
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(providesClass, false);
			/*PendingVar cx = */gen.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar parent = gen.argument(J.OBJECT, "card");
			MethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.OBJECT, "<init>").flush();
			ctor.assign(card.asExpr(ctor), parent.getVar()).flush();
			ctor.returnVoid().flush();
		}
		FieldExpr ctrs = agentClass.getField(agentctor, "store");
		agentctor.callInterface("void", ctrs, "recordContract",
			agentctor.stringConst(p.actualType().name().uniqueName()),
			agentctor.as(
				agentctor.makeNew(csn.javaClassName(), agentctor.getArgument(0), agentctor.as(agentctor.myThis(), J.OBJECT)),
				J.OBJECT
			)
		).flush();
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi) {
		HandlerName name = (HandlerName) hi.name();
		ByteCodeSink providesClass = bce.newClass(name.javaClassName());
		providesClass.superclass(J.OBJECT);
		providesClass.generateAssociatedSourceFile();
		IFieldInfo card = providesClass.defineField(true, Access.PRIVATE, J.OBJECT, "_card"); // Probably should be some superclass of card, service, agent ...
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(providesClass, false);
			/*PendingVar cx = */gen.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar parent = gen.argument(J.OBJECT, "card");
			MethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.OBJECT, "<init>").flush();
			ctor.assign(card.asExpr(ctor), parent.getVar()).flush();
			ctor.returnVoid().flush();
		}
//		FieldExpr ctrs = agentClass.getField(agentctor, "store");
//		agentctor.callInterface("void", ctrs, "recordContract",
//			agentctor.stringConst(hi.actualType().name().uniqueName()),
//			agentctor.as(
//				agentctor.makeNew(csn.javaClassName(), agentctor.getArgument(0), agentctor.as(agentctor.myThis(), J.OBJECT)),
//				J.OBJECT
//			)
//		).flush();
	}

	@Override
	public void visitRequires(RequiresContract rc) {
		FieldExpr ctrs = agentClass.getField(agentctor, "store");
		agentctor.callInterface("void", ctrs, "requireService",
			agentcx,
			agentctor.classConst(rc.actualType().name().javaClassName()),
			agentctor.stringConst(rc.referAsVar)
		).flush();
	}
	
	@Override
	public void visitStructField(StructField sf) {
		if (structFieldHandler != null)
			structFieldHandler.visitStructField(sf);
	}

	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		if (!obj.generate)
			return;
		if (this.currentBlock != null && !this.currentBlock.isEmpty())
			makeBlock(meth, currentBlock).flush();
		this.meth.returnObject(fs.evalRet).flush();
		this.meth = null;
	}
	
	@Override
	public void leaveStructDefn(StructDefn sd) {
		if (!sd.generate)
			return;
		if (this.currentBlock != null && !this.currentBlock.isEmpty())
			makeBlock(meth, currentBlock).flush();
		this.meth.returnObject(fs.evalRet).flush();
		this.meth = null;
	}

	@Override
	public void leaveAgentDefn(AgentDefinition s) {
		agentctor.returnVoid().flush();
		agentctor = null;
	}
	
	@Override
	public void visitStructFieldAccessor(StructField sf) {
		String cxName = sf.name().container().javaName();
		ByteCodeSink bcc = bce.get(cxName);
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_field_" + sf.name);
		gen.argument(J.FLEVALCONTEXT, "cxt");
		gen.argument("[" + J.OBJECT, "args");
		gen.returns(J.OBJECT);
		MethodDefiner meth = gen.done();
		IExpr ret = meth.callInterface(J.OBJECT, meth.getField("state"), "get", meth.stringConst(sf.name));
		meth.returnObject(ret).flush();
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		clz = bce.newClass(clzName);
		clz.generateAssociatedSourceFile();
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		PendingVar runner = ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		PendingVar pcx = ann.argument(J.FLEVALCONTEXT, "cxt");
		ann.returns(JavaType.void_);
		meth = ann.done();
		meth.lenientMode(leniency);
		this.runner = runner.getVar();
		this.fcx = pcx.getVar();
		this.fs = new FunctionState(meth, fcx, null, null, runner.getVar());
		this.currentBlock = new ArrayList<>();
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

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		if (meth == null || fs == null) {
			globalMocks.add(udd);
			return;
		}
		NamedType objty = udd.ofType.defn();
		if (objty instanceof ContractDecl) {
			ContractDecl cdd = (ContractDecl)objty;
			IExpr mc = meth.callInterface(J.OBJECT, fcx, "mockContract", meth.classConst(cdd.name().javaClassName()));
			Var v = meth.avar(J.OBJECT, fs.nextVar("v"));
			meth.assign(v, mc).flush();
			this.fs.addMock(udd, v);
			this.explodingMocks.add(v);
		} else if (objty instanceof ObjectDefn) {
			IExpr mc = meth.callStatic(objty.name().javaName(), J.OBJECT, "eval", this.fcx);
			Var v = meth.avar(J.OBJECT, fs.nextVar("v"));
			meth.assign(v, mc).flush();
			this.fs.addMock(udd, v);
		} else if (objty instanceof AgentDefinition) {
			AgentDefinition ad = (AgentDefinition)objty;
			IExpr agent = meth.makeNew(ad.name().javaName(), this.fcx);
			IExpr mc = meth.callInterface(J.MOCKAGENT, fcx, "mockAgent", meth.as(agent, J.CONTRACT_HOLDER));
			Var v = meth.avar(J.OBJECT, fs.nextVar("v"));
			meth.assign(v, mc).flush();
			this.fs.addMock(udd, v);
		} else {
			// see comment in JSGenerator
			throw new RuntimeException("not handled: " + objty);
		}
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitor(sv, this.fs, this.runner);
	}
	
	@Override
	public void visitUnitTestExpect(UnitTestExpect ute) {
		new DoExpectationGenerator(sv, this.fs, this.runner, this.currentBlock);
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		new DoInvocationGenerator(sv, this.fs, this.runner);
	}
	
	@Override
	public void visitUnitTestSend(UnitTestSend uts) {
		new DoSendGenerator(sv, this.fs, meth.as(this.runner, TestRunner.class.getName()));
	}
	
	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		if (currentBlock.size() != 1)
			throw new RuntimeException("Multiple result expressions");
		currentBlock.get(0).flush();
		currentBlock.clear();
	}
	
	@Override
	public void leaveUnitTest(UnitTestCase e) {
		for (Var v : explodingMocks) {
			meth.callInterface("void", meth.castTo(v, J.EXPECTING), "assertSatisfied", this.fcx).flush();
		}
		meth.returnVoid().flush();
		meth = null;
		this.currentBlock = null;
		explodingMocks.clear();
		clz.generate();
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (currentBlock.size() != 1)
			throw new RuntimeException("Multiple result expressions");
		currentBlock.get(0).flush();
		currentBlock.clear();
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		String topName = cd.name().javaName();
		clz = bce.newClass(topName);
		clz.makeInterface();
		clz.generateAssociatedSourceFile();
		switch (cd.type) {
		case CONTRACT:
			clz.implementsInterface(J.DOWN_CONTRACT);
			break;
		case SERVICE:
			clz.implementsInterface(J.UP_CONTRACT);
			break;
		case HANDLER:
			clz.implementsInterface(J.HANDLER_CONTRACT);
			break;
		}
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, false, cmd.name.name);
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		meth.argument(J.FLEVALCONTEXT, "_cxt");
		int i=1;
		TypeReference type = null;
		for (Object a : cmd.args) {
			type = null;
			if (a instanceof VarPattern) {
				VarPattern vp = (VarPattern) a;
				meth.argument(J.OBJECT, vp.var);
			} else if (a instanceof TypedPattern) {
				TypedPattern vp = (TypedPattern) a;
				meth.argument(J.OBJECT, vp.var.var);
				type = vp.type;
			} else {
				meth.argument(J.OBJECT, "a" + i);
			}
			i++;
		}
		if (type == null || !(type.defn() instanceof ContractDecl))
			meth.argument(J.OBJECT, "_ih");
		IFieldInfo fi = clz.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "_nf_" + cmd.name.name);
		fi.constValue(cmd.args.size());
	}

	public static IExpr makeBlock(MethodDefiner meth, List<IExpr> block) {
		if (block.isEmpty())
			throw new NotImplementedException("there must be at least one statement in a block");
		else if (block.size() == 1)
			return block.get(0);
		else
			return meth.block(block.toArray(new IExpr[block.size()]));
	}
	
	public static JVMGenerator forTests(MethodDefiner meth, IExpr runner, Var args) {
		JVMGenerator ret = new JVMGenerator(meth, runner, args);
		ret.fs = new FunctionState(meth, runner, null, args, runner);
		return ret;
	}

	public static JVMGenerator forTests(ByteCodeSink clz) {
		return new JVMGenerator(clz);
	}

	public NestedVisitor stackVisitor() {
		return sv;
	}

	public FunctionState state() {
		return fs;
	}
}
