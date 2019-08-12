package org.flasck.flas.compiler;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
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
import org.zinutils.exceptions.NotImplementedException;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeStorage bce;
	private MethodDefiner meth;
	private List<IExpr> stack = new ArrayList<IExpr>();
	private IExpr runner;
	private ByteCodeSink clz;
	private ByteCodeSink upClz;
	private ByteCodeSink downClz;

	public JVMGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}
	
	private JVMGenerator(MethodDefiner meth, IExpr runner) {
		this.bce = null;
		this.meth = meth;
		this.runner = runner;
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
		ann.returns(JavaType.object_);
		meth = ann.done();
	}
	
	// TODO: this should have been reduced to HSIE, which we should generate from
	// But I am hacking for now to get a walking skeleton up and running so we can E2E TDD
	// The actual traversal is done by the traverser ...

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (stack.size() != 1) {
			throw new RuntimeException("I was expecting a stack depth of 1, not " + stack.size());
		}
		meth.returnObject(stack.get(0)).flush();
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
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		FunctionDefinition defn = (FunctionDefinition)var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		stack.add(meth.callStatic(defn.name().javaClassName(), "java.lang.Object", "eval", new IExpr[0]));
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
		this.runner = runner.getVar();
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth.returnVoid().flush();
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
		String topName = cd.nameAsName().javaName();
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
	
	public static JVMGenerator forTests(MethodDefiner meth, IExpr runner) {
		return new JVMGenerator(meth, runner);
	}

	public static JVMGenerator forTests(ByteCodeSink clz, ByteCodeSink up, ByteCodeSink down) {
		return new JVMGenerator(clz, up, down);
	}
}
