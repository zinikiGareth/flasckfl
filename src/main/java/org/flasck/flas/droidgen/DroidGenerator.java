package org.flasck.flas.droidgen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidGenerator {
	private final ByteCodeEnvironment bce;
	private final File androidDir;

	public DroidGenerator(HSIE hsie, ByteCodeEnvironment bce, File androidDir) {
		this.bce = bce;
		this.androidDir = androidDir;
	}
	
	public void generate(StructDefn value) {
		ByteCodeCreator bcc = new ByteCodeCreator(bce, value.name());
		Map<String, FieldInfo> fields = new TreeMap<String,FieldInfo>();
		for (StructField sf : value.fields) {
			FieldInfo fi = bcc.defineField(false, Access.PUBLIC, new JavaType("java.lang.Object"), sf.name);
			fields.put(sf.name, fi);
		}
		bcc.superclass("org.flasck.android.FLASObject");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", "org/flasck/android/FLASObject", "<init>").flush();
			ctor.returnVoid().flush();
		}
		for (StructField sf : value.fields) {
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_doFullEval");
			gen.returns("void");
			NewMethodDefiner dfe = gen.done();
			dfe.assign(fields.get(sf.name).asExpr(dfe), dfe.callVirtual("java.lang.Object", dfe.myThis(), "_fullOf", fields.get(sf.name).asExpr(dfe))).flush();
			dfe.returnVoid().flush();
		}
	}

	public void generate(String key, CardGrouping grp) {
		ByteCodeCreator bcc = new ByteCodeCreator(bce, grp.struct.name());
		bcc.superclass("org.flasck.android.FlasckActivity");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
		for (StructField sf : grp.struct.fields) {
			JavaType jt;
			if (sf.type.iam == WhatAmI.BUILTIN) {
				if (((Type)sf.type).name().equals("Number"))
					jt = JavaType.int_; // what about floats?
				else
					throw new UtilException("Not handled " + sf.type);
			} else if (sf.type instanceof ContractImplements || sf.type instanceof ContractDecl) {
				continue;
//				jt = new JavaType(sf.type.name());
			} else
				throw new UtilException("Not handled " + sf.type + " " + sf.type.getClass());
			bcc.defineField(false, Access.PROTECTED, jt, sf.name);
		}
		for (ContractGrouping x : grp.contracts) {
			if (x.referAsVar != null)
				bcc.defineField(false, Access.PROTECTED, new JavaType(javaNestedName(x.implName)), x.referAsVar);
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), javaNestedSimpleName(x.implName));
		}
		for (HandlerGrouping h : grp.handlers) {
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), javaNestedSimpleName(h.impl.name));
		}
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", "org.flasck.android.FlasckActivity", "<init>").flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "onCreate");
			PendingVar sis = gen.argument("android.os.Bundle", "savedState");
			gen.returns("void");
			NewMethodDefiner oc = gen.done();
			oc.setAccess(Access.PROTECTED);
			oc.callSuper("void", "org.flasck.android.FlasckActivity", "onCreate", sis.getVar()).flush();
			for (ContractGrouping x : grp.contracts) {
				System.out.println(x.type + " " + x.implName + " " + x.referAsVar);
				Expr impl = oc.makeNew(javaNestedName(x.implName), oc.myThis());
				if (x.referAsVar != null) {
					oc.assign(oc.getField(x.referAsVar), impl).flush();
					impl = oc.getField(x.referAsVar);
				}
				oc.callVirtual("void", oc.myThis(), "registerContract", oc.stringConst(x.type), oc.as(impl, "java.lang.Object")).flush();
			}
			oc.callSuper("void", "org.flasck.android.FlasckActivity", "ready").flush();
			oc.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "render");
			PendingVar into = gen.argument("java.lang.String", "into");
			gen.returns("void");
			NewMethodDefiner render = gen.done();
			render.returnVoid().flush();
		}
	}

	public void generateContract(String name, ContractImplements ci) {
		if (androidDir == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(bce, javaNestedName(name));
		bcc.superclass(ci.name());
		FieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(javaBaseName(name)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(name), javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(javaBaseName(name), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", ci.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
	}

	public void generateService(String key, ContractService value) {
		// TODO Auto-generated method stub
		
	}

	public void generateHandler(String name, HandlerImplements hi) {
		if (androidDir == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(bce, javaNestedName(name));
		bcc.superclass(hi.name());
		FieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(javaBaseName(name)), "_card");
		Map<String, FieldInfo> fs = new TreeMap<String, FieldInfo>();
		for (Object o : hi.boundVars) {
			String var = ((HandlerLambda)o).var;
			FieldInfo hli = bcc.defineField(false, Access.PRIVATE, new JavaType("java.lang.Object"), var);
			fs.put(var, hli);
		}
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(name), javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument("java.lang.Object", "card");
			Map<String, PendingVar> vm = new TreeMap<String, PendingVar>();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				PendingVar pvi = gen.argument("java.lang.Object", var);
				vm.put(var, pvi);
			}
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", hi.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic("org.flasck.android.FLEval", "java.lang.Object", "full", cardArg.getVar()), javaBaseName(name))).flush();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				ctor.assign(fs.get(var).asExpr(ctor), ctor.callStatic("org.flasck.android.FLEval", "java.lang.Object", "head", vm.get(var).getVar())).flush();
			}
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cardArg = gen.argument("java.lang.Object", "card");
			PendingVar argsArg = gen.argument("[java.lang.Object", "args");
			gen.returns("java.lang.Object");
			NewMethodDefiner eval = gen.done();
			eval.lenientMode(true);
			List<Expr> naList = new ArrayList<Expr>();
			naList.add(cardArg.getVar());
			for (int k=0;k<hi.boundVars.size();k++)
				naList.add(eval.arrayElt(argsArg.getVar(), eval.intConst(k)));
			Expr[] newArgs = new Expr[naList.size()];
			naList.toArray(newArgs);
			eval.ifOp(0xa2, eval.arraylen(argsArg.getVar()), eval.intConst(1), 
					eval.returnObject(eval.makeNew("org.flasck.android.FLCurry", cardArg.getVar(), eval.classConst(javaNestedName(name)), argsArg.getVar())), 
					eval.returnObject(eval.makeNew(javaNestedName(name), newArgs))).flush();
		}
		{
			
		}
	}

	public void generate(Collection<HSIEForm> forms) {
		if (androidDir == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.ziniki.CounterCard$B1");
		bcc.superclass("org.flasck.android.TextArea");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, "test.ziniki.CounterCard", "B1");
		FieldInfo card = bcc.defineField(true, Access.PRIVATE, new JavaType("test.ziniki.CounterCard"), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar counter = gen.argument("test.ziniki.CounterCard", "counter");
			PendingVar parent = gen.argument("org/flasck/android/Area", "parent");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", "org/flasck/android/TextArea", "<init>", parent.getVar(), ctor.as(ctor.aNull(), "java.lang.String")).flush();
			ctor.assign(card.asExpr(ctor), counter.getVar()).flush();
			ctor.callVirtual("void", ctor.getField(card.asExpr(ctor), "_wrapper"), "onAssign", ctor.stringConst("counter"), ctor.as(ctor.myThis(), "org.flasck.android.Area"), ctor.stringConst("_contentExpr")).flush();
			ctor.callVirtual("void", ctor.myThis(), "_contentExpr").flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_contentExpr");
			gen.returns("void");
			NewMethodDefiner meth = gen.done();
			Var str = meth.avar("java.lang.String", "str");
			meth.assign(str, meth.callStatic("java.lang.Integer", "java.lang.String", "toString", meth.getField(meth.getField(meth.myThis(), "_card"), "counter"))).flush();
			meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
			meth.returnVoid().flush();
		}
	}

	private String javaBaseName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx);
	}

	private String javaNestedName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx) + "$" + clz.substring(idx+1);
	}

	private String javaNestedSimpleName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(idx+1);
	}
}
