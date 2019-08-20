package org.flasck.flas.compiler;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.BuiltinRepositoryEntry;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeStorage bce;
	private MethodDefiner meth;
	private List<IExpr> stack = new ArrayList<IExpr>();
	private IExpr runner;
	private ByteCodeSink clz;
	private ByteCodeSink upClz;
	private ByteCodeSink downClz;
	private int nextVar = 1;
	private IExpr fcx;
	private static final boolean leniency = false;

	public JVMGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}
	
	private JVMGenerator(MethodDefiner meth, IExpr runner) {
		this.bce = null;
		this.meth = meth;
		this.runner = runner;
		this.fcx = runner;
	}

	private JVMGenerator(ByteCodeSink clz, ByteCodeSink up, ByteCodeSink down) {
		this.bce = null;
		this.clz = clz;
		this.upClz = up;
		this.downClz = down;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		this.clz = bce.newClass(fn.name().javaClassName());
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "eval");
		PendingVar cxArg = ann.argument(J.FLEVALCONTEXT, "cxt");
		/* PendingVar argsArg = */ ann.argument("[" + J.OBJECT, "args");
		ann.returns(JavaType.object_);
		meth = ann.done();
		meth.lenientMode(leniency);
		nextVar = 1;
		fcx = cxArg.getVar();
	}
	
	// TODO: this should have been reduced to HSIE, which we should generate from
	// But I am hacking for now to get a walking skeleton up and running so we can E2E TDD
	// The actual traversal is done by the traverser ...

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (stack.size() != 1) {
			/* I don't think this is actually true ...
			// There is a complex case where we have a Struct Constructor with no args and we didn't generate code for it earlier because we don't put that in a closure
			// Do that now ...
			if (stack.size() == 0) {
				// I think this is actually part of the code around HSIE, but we don't have that yet, hence the complex chain here ...
				Expr expr = fn.intros().get(0).cases().get(0).expr;
				if (expr instanceof UnresolvedVar && ((UnresolvedVar)expr).defn() instanceof StructDefn) {
					StructDefn defn = (StructDefn) ((UnresolvedVar)expr).defn();
					makeClosure(defn, 0, defn.argCount());
				}
			}
			// retest ...
			if (stack.size() != 1)
			*/
			throw new RuntimeException("I was expecting a stack depth of 1, not " + stack.size());
		}
		meth.returnObject(stack.remove(0)).flush();
		this.meth = null;
		this.clz = null;
		this.fcx = null;
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		Object val = expr.value();
		if (val instanceof Integer)
			stack.add(meth.makeNew(J.NUMBER, meth.box(meth.intConst((int) val)), meth.castTo(meth.aNull(), "java.lang.Double")));
		else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(meth.stringConst(expr.text));
	}
	
	// I think at the moment I am mixing up three completely separate cases here
	// Basically this is just "leaveApplyExpr" with no args.
	// It is OK to call eval directly if we know if will complete quickly, i.e. it's a constructor
	// But if it is a regular var - i.e. a function of 0 args, it could be arbitrarily complex and should be a closure
	// And if it is the "first" token of an ApplyExpr, we need to just push "it" without eval or closure ...
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		NameOfThing name = defn.name();
		if (defn instanceof FunctionDefinition) {
			if (nargs == 0) {
				FunctionDefinition fn = (FunctionDefinition) defn;
				stack.add(meth.classConst(name.javaClassName()));
				makeClosure(defn, 0, fn.argCount());
			} else
				stack.add(meth.classConst(name.javaClassName()));
		} else if (defn instanceof StructDefn){
			// if the constructor has no args, eval it here
			// otherwise leave it until "leaveExpr" or "leaveFunction"
			if (nargs == 0 && ((StructDefn)defn).argCount() == 0) {
				List<IExpr> provided = new ArrayList<>();
				IExpr args = meth.arrayOf(J.OBJECT, provided);
				stack.add(meth.callStatic(name.javaClassName(), J.OBJECT, "eval", fcx, args));
			}
		} else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		String opName = resolveOpName(operator.op);
		stack.add(meth.classConst(opName));
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object fn = expr.fn;
		int expArgs = 0;
		RepositoryEntry defn = null;
		if (fn instanceof UnresolvedVar) {
			defn = ((UnresolvedVar)fn).defn();
			expArgs = ((WithTypeSignature)defn).argCount();
		} else if (fn instanceof UnresolvedOperator) {
			defn = ((UnresolvedOperator)fn).defn();
			expArgs = ((BuiltinRepositoryEntry)defn).argCount();
		}
		if (expr.args.isEmpty()) // then it's a spurious apply
			return;
		makeClosure(defn, expr.args.size(), expArgs);
	}

	private void makeClosure(RepositoryEntry defn, int depth, int expArgs) {
		List<IExpr> provided = new ArrayList<IExpr>();
		int k = stack.size()-depth;
		for (int i=0;i<depth;i++)
			provided.add(stack.remove(k));
		IExpr args = meth.arrayOf(J.OBJECT, provided);
		if (defn instanceof StructDefn && !provided.isEmpty()) {
			// do the creation immediately
			// Note that we didn't push anything onto the stack earlier ...
			// TODO: I think we need to cover the currying case separately ...
			IExpr ctor = meth.callStatic(defn.name().javaClassName(), J.OBJECT, "eval", args);
			stack.add(ctor);
		} else {
			IExpr fn = stack.remove(stack.size()-1);
			IExpr call;
			if (depth < expArgs)
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "curry", meth.as(fn, "java.lang.Object"), meth.intConst(expArgs), args);
			else
				call = meth.callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", meth.as(fn, "java.lang.Object"), args);
			Var v = meth.avar(J.FLCLOSURE, "v" + nextVar++);
			IExpr assign = meth.assign(v, call);
			assign.flush();
			stack.add(v);
		}
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
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		PendingVar runner = ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		ann.returns(JavaType.void_);
		meth = ann.done();
		meth.lenientMode(leniency);
		this.runner = runner.getVar();
		this.fcx = meth.as(this.runner, J.FLEVALCONTEXT); // I'm not sure about this
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth.returnVoid().flush();
		meth = null;
		clz.generate();
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (stack.size() != 2) {
			throw new RuntimeException("I was expecting a stack depth of 2, not " + stack.size());
		}
		IExpr lhs = meth.as(stack.get(0), J.OBJECT);
		IExpr rhs = meth.as(stack.get(1), J.OBJECT);
		meth.callVirtual("void", runner, "assertSameValue", lhs, rhs).flush();
		stack.clear();
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
				meth.argument(J.OBJECT, vp.var);
				type = vp.type;
			} else {
				meth.argument(J.OBJECT, "a" + i);
			}
			i++;
		}
		if (type == null || !(type.defn() instanceof ContractDecl))
			meth.argument(J.OBJECT, "_ih");
	}

	private String resolveOpName(String op) {
		String inner;
		switch (op) {
		case "+":
			inner = "Plus";
			break;
		case "*":
			inner = "Mul";
			break;
		default:
			throw new RuntimeException("There is no operator " + op);
		}
		return J.FLEVAL + "$" + inner;
	}
	
	public static JVMGenerator forTests(MethodDefiner meth, IExpr runner) {
		return new JVMGenerator(meth, runner);
	}

	public static JVMGenerator forTests(ByteCodeSink clz, ByteCodeSink up, ByteCodeSink down) {
		return new JVMGenerator(clz, up, down);
	}
}
