package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class JVMGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	private final ByteCodeStorage bce;
	private final StackVisitor sv;
	private MethodDefiner meth;
	private IExpr runner;
	private ByteCodeSink clz;
	private ByteCodeSink upClz;
	private ByteCodeSink downClz;
	private IExpr fcx;
	private Var fargs;
	private final Map<Slot, IExpr> switchVars = new HashMap<>();
	private FunctionState fs;
	private IExpr resultExpr;
	private List<IExpr> currentBlock;
	private static final boolean leniency = false;

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

	private JVMGenerator(ByteCodeSink clz, ByteCodeSink up, ByteCodeSink down) {
		this.sv = new StackVisitor();
		sv.push(this);
		this.bce = null;
		this.clz = clz;
		this.upClz = up;
		this.downClz = down;
	}

	@Override
	public void result(Object r) {
		if (resultExpr != null)
			throw new RuntimeException("More than one result expr at once");
		resultExpr = (IExpr) r;
	}

	@Override
	public boolean isHsi() {
		return true;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty()) {
			this.clz = null;
			this.meth = null;
			return;
		}
		this.clz = bce.newClass(fn.name().javaClassName());
		this.clz.generateAssociatedSourceFile();
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "eval");
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		PendingVar argsArg = ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		fcx = cxArg.getVar();
		fargs = argsArg.getVar();
		switchVars.clear();
		fs = new FunctionState(meth, (Var)fcx, fargs);
		currentBlock = new ArrayList<IExpr>();
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
	}

	// This is needed here as well as HSIGenerator to handle the no-switch case
	@Override
	public void startInline(FunctionIntro fi) {
		sv.push(new ExprGenerator(fs, sv));
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
		currentBlock.add(resultExpr);
		makeBlock(meth, currentBlock).flush();
		resultExpr = null;
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}
	@Override
	public void visitStructDefn(StructDefn sd) {
		if (!sd.generate)
			return;
		ByteCodeSink bcc = bce.newClass(sd.name().javaName());
		bcc.generateAssociatedSourceFile();
		/*
		DroidStructFieldGenerator fg = new DroidStructFieldGenerator(bcc, Access.PUBLIC);
		if (sd.ty == FieldsDefn.FieldsType.STRUCT) {
			sd.visitFields(fg);
		}
		*/
		String base = sd.type == FieldsDefn.FieldsType.STRUCT?J.FLAS_STRUCT:J.FLAS_ENTITY; 
		bcc.superclass(base);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "_cxt");
			NewMethodDefiner ctor = gen.done();
			IExpr[] args = new IExpr[0];
			if (sd.type == FieldsDefn.FieldsType.ENTITY)
				args = new IExpr[] { cx.getVar(), ctor.as(ctor.aNull(), J.BACKING_DOCUMENT) };
			ctor.callSuper("void", base, "<init>", args).flush();
			/* TODO: initialize fields
			for (int i=0;i<sd.fields.size();i++) {
				StructField fld = sd.fields.get(i);
				if (fld.name.equals("id"))
					continue;
				if (fld.init != null) {
					final IExpr initVal = ctor.callStatic(J.FLCLOSURE, J.FLCLOSURE, "obj", ctor.as(ctor.myThis(), J.OBJECT), ctor.as(ctor.classConst(fld.init.javaNameAsNestedClass()), J.OBJECT), ctor.arrayOf(J.OBJECT, new ArrayList<>()));
					if (sd.ty == FieldsDefn.FieldsType.STRUCT)
						ctor.assign(ctor.getField(ctor.myThis(), fld.name), initVal).flush();
					else
						ctor.callSuper("void", J.FLAS_ENTITY, "closure", ctor.stringConst(fld.name), ctor.as(initVal, J.OBJECT)).flush();
				}
			}
			*/
			ctor.returnVoid().flush();
		}
		if (sd.type == FieldsDefn.FieldsType.ENTITY) {
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "_cxt");
			PendingVar doc = gen.argument(J.BACKING_DOCUMENT, "doc");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", base, "<init>", cx.getVar(), doc.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
		/*
		if (!sd.fields.isEmpty()) {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar pv = gen.argument("[java.lang.Object", "args");
			gen.returns(sd.name());
			MethodDefiner meth = gen.done();
			Var v = pv.getVar();
			Var ret = meth.avar(sd.name(), "ret");
			meth.assign(ret, meth.makeNew(sd.name(), cx.getVar())).flush();
			int ap = 0;
			for (int i=0;i<sd.fields.size();i++) {
				RWStructField fld = sd.fields.get(i);
				if (fld.name.equals("id"))
					continue;
				if (fld.init != null)
					continue;
				final IExpr val = meth.arrayElt(v, meth.intConst(ap++));
				if (sd.ty == FieldsDefn.FieldsType.STRUCT)
					meth.assign(meth.getField(ret, fld.name), val).flush();
				else
					meth.callVirtual("void", ret, "closure", meth.stringConst(fld.name), val).flush();
			}
			meth.returnObject(ret).flush();
		}
		
		if (sd.ty == FieldsDefn.FieldsType.STRUCT) {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_doFullEval");
			PendingVar cx = gen.argument(J.FLEVALCONTEXT, "cxt");
			gen.returns("void");
			NewMethodDefiner dfe = gen.done();
			DroidStructFieldInitializer fi = new DroidStructFieldInitializer(dfe, cx.getVar(), fg.fields);
			sd.visitFields(fi);
			dfe.returnVoid().flush();
		}
		
		if (sd.ty == FieldsDefn.FieldsType.STRUCT) {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "toWire");
			gen.argument(J.WIREENCODER, "wire");
			PendingVar pcx = gen.argument(J.ENTITYDECODINGCONTEXT, "cx");
			gen.returns(J.OBJECT);
			NewMethodDefiner meth = gen.done();
			Var cx = pcx.getVar();
			Var ret = meth.avar(J.JOBJ, "ret");
			meth.assign(ret, meth.callInterface(J.JOBJ, cx, "jo")).flush();
			meth.voidExpr(meth.callInterface(J.JOBJ, ret, "put", meth.stringConst("_struct"), meth.as(meth.stringConst(sd.name()), J.OBJECT))).flush();
			meth.returnObject(ret).flush();
		}
		
		if (sd.ty == FieldsDefn.FieldsType.STRUCT) {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "fromWire");
			PendingVar pcx = gen.argument(J.FLEVALCONTEXT, "cx");
			gen.argument(J.JOBJ, "jo");
			gen.returns(sd.name());
			NewMethodDefiner meth = gen.done();
			Var cx = pcx.getVar();
			Var ret = meth.avar(J.OBJECT, "ret");
			meth.assign(ret, meth.makeNew(sd.name(), cx)).flush();
			meth.returnObject(ret).flush();
		} else {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "fromWire");
			PendingVar pcx = gen.argument(J.FLEVALCONTEXT, "cx");
			PendingVar wire = gen.argument(J.JDOC, "wire");
			gen.returns(sd.name());
			NewMethodDefiner meth = gen.done();
			Var cx = pcx.getVar();
			meth.returnObject(meth.makeNew(sd.name(), cx, meth.callStatic(J.BACKING_DOCUMENT, J.BACKING_DOCUMENT, "from", cx, wire.getVar()))).flush();
		}
		*/
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		clz = bce.newClass(clzName);
		clz.generateAssociatedSourceFile();
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		PendingVar runner = ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		ann.returns(JavaType.void_);
		meth = ann.done();
		meth.lenientMode(leniency);
		this.runner = runner.getVar();
		this.fcx = meth.getField(this.runner, "cxt");
//		this.fcx = meth.as(this.runner, J.FLEVALCONTEXT); // I'm not sure about this
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth.returnVoid().flush();
		meth = null;
		clz.generate();
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new CaptureAssertionClauseVisitor(sv, this.meth, this.runner, this.fcx);
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		resultExpr.flush();
		resultExpr = null;
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		String topName = cd.name().javaName();
		String upName = topName + "$Up";
		String downName = topName + "$Down";
		clz = bce.newClass(topName);
		upClz = bce.newClass(upName);
		downClz = bce.newClass(downName);

		clz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Up");
		upClz.generateAssociatedSourceFile();
		upClz.makeInterface();
		upClz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Up");
		upClz.implementsInterface(J.UP_CONTRACT);

		clz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Down");
		downClz.generateAssociatedSourceFile();
		downClz.makeInterface();
		downClz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Down");
		downClz.implementsInterface(J.UP_CONTRACT);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		ByteCodeSink in;
		if (cmd.dir == ContractMethodDir.DOWN)
			in = downClz;
		else
			in = upClz;
		GenericAnnotator ann = GenericAnnotator.newMethod(in, false, cmd.name.name);
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
		ret.fs = new FunctionState(meth, runner, args);
		return ret;
	}

	public static JVMGenerator forTests(ByteCodeSink clz, ByteCodeSink up, ByteCodeSink down) {
		return new JVMGenerator(clz, up, down);
	}

	public NestedVisitor stackVisitor() {
		return sv;
	}
}