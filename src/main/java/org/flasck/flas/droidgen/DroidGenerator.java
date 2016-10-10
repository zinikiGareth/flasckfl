package org.flasck.flas.droidgen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.PlatformSpec;
import org.flasck.flas.parsedForm.ScopedVar;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.android.AndroidLabel;
import org.flasck.flas.parsedForm.android.AndroidLaunch;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.Annotation;
import org.zinutils.bytecode.BlockExpr;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.FieldObject;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringUtil;

public class DroidGenerator {
	private final DroidBuilder builder;
	private final static Logger logger = LoggerFactory.getLogger("DroidGen");

	public DroidGenerator(HSIE hsie, DroidBuilder bldr) {
		this.builder = bldr;
	}
	
	public void generateAppObject() {
		if (builder == null)
			return;
		System.out.println("packages = " + builder.packages);
		// TODO: this package name needs to be configurable
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, "com.helpfulsidekick.chaddy.MainApplicationClass");
		bcc.superclass("org.flasck.android.FlasckApplication");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", "org/flasck/android/FLASObject", "<init>").flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "onCreate");
			gen.returns("void");
			NewMethodDefiner oc = gen.done();
			oc.setAccess(Access.PUBLIC);
			oc.callSuper("void", "org.flasck.android.FlasckActivity", "onCreate").flush();
			for (PackageInfo x : builder.packages) {
				oc.callVirtual("void", oc.myThis(), "bindPackage", oc.stringConst(x.local), oc.stringConst(x.remote), oc.intConst(x.version)).flush();
			}
			oc.returnVoid().flush();
		}
	}
	
	public void generate(StructDefn value) {
		if (builder == null || !value.generate)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, value.name());
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
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_doFullEval");
		gen.returns("void");
		NewMethodDefiner dfe = gen.done();
		for (StructField sf : value.fields) {
			dfe.assign(fields.get(sf.name).asExpr(dfe), dfe.callVirtual("java.lang.Object", dfe.myThis(), "_fullOf", fields.get(sf.name).asExpr(dfe))).flush();
		}
		dfe.returnVoid().flush();
	}

	public void generate(String key, CardGrouping grp) {
		if (builder == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, grp.struct.name());
		bcc.superclass("org.flasck.android.FlasckActivity");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
		for (StructField sf : grp.struct.fields) {
			JavaType jt;
			if (sf.type.iam == WhatAmI.BUILTIN) {
				if (((Type)sf.type).name().equals("Number"))
					jt = JavaType.int_; // what about floats?
				else if (((Type)sf.type).name().equals("String"))
					jt = JavaType.string;
				else
					throw new UtilException("Not handled " + sf.type);
			} else if (sf.type instanceof ContractImplements || sf.type instanceof ContractDecl) {
				jt = javaType(sf.type.name());
			} else if (sf.type instanceof ObjectDefn) {
				jt = javaType(sf.type.name());
			} else if (sf.type instanceof Type) {
				jt = javaType(sf.type.name());
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
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), javaNestedSimpleName(h.impl.hiName));
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
				Expr impl = oc.makeNew(javaNestedName(x.implName), oc.myThis());
				if (x.referAsVar != null) {
					oc.assign(oc.getField(x.referAsVar), impl).flush();
					impl = oc.getField(x.referAsVar);
				}
				oc.callVirtual("void", oc.myThis(), "registerContract", oc.stringConst(x.type), oc.as(impl, "org.flasck.android.ContractImpl")).flush();
			}
			oc.callSuper("void", "org.flasck.android.FlasckActivity", "ready").flush();
			oc.returnVoid().flush();
		}
		if (grp.platforms.containsKey("android")) {
			PlatformSpec spec = grp.platforms.get("android");
			for (Object d : spec.defns) {
				if (d instanceof AndroidLaunch)
					bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.MainActivity");
				else if (d instanceof AndroidLabel) {
					Annotation label = bcc.addRTVAnnotation("com.gmmapowell.quickbuild.annotations.android.Label");
					label.addParam("value", ((AndroidLabel)d).label);
				} else
					throw new UtilException("Cannot handle android platform spec of type " + d.getClass());
			}
		}
	}

	private JavaType javaType(String name) {
		if (name.indexOf(".") == -1)
			name = "org.flasck.android.builtin." + name;
		return new JavaType(name);
	}

	public void generateContractDecl(String name, ContractDecl cd) {
		if (builder == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, name);
		bcc.superclass("org.flasck.android.ContractImpl");
		bcc.makeAbstract();
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", cd.name(), "<init>").flush();
			ctor.returnVoid().flush();
		}
		
		for (ContractMethodDecl m : cd.methods) {
			if (m.dir.equals("down")) {
				System.out.println(name + " " + m.dir + " " + m.name);
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns("java.lang.Object");
				int k = 0;
				for (@SuppressWarnings("unused") Object a : m.args)
					gm.argument("java.lang.Object", "arg"+(k++));
				gm.done();
			}
		}
	}

	public void generateContractImpl(String name, ContractImplements ci) {
		if (builder == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, javaNestedName(name));
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

	public void generateService(String name, ContractService cs) {
		if (builder == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, javaNestedName(name));
		bcc.superclass(cs.name());
		FieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(javaBaseName(name)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(name), javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(javaBaseName(name), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", cs.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
	}

	public void generateHandler(String name, HandlerImplements hi) {
		if (builder == null)
			return;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, javaNestedName(name));
		bcc.superclass(hi.name());
		FieldInfo fi = null;
		if (hi.inCard)
			fi = bcc.defineField(false, Access.PRIVATE, new JavaType(javaBaseName(name)), "_card");
		Map<String, FieldInfo> fs = new TreeMap<String, FieldInfo>();
		for (Object o : hi.boundVars) {
			String var = ((HandlerLambda)o).var;
			FieldInfo hli = bcc.defineField(false, Access.PRIVATE, new JavaType("java.lang.Object"), var);
			fs.put(var, hli);
		}
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(name), javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = null;
			if (hi.inCard)
				cardArg = gen.argument("java.lang.Object", "card");
			Map<String, PendingVar> vm = new TreeMap<String, PendingVar>();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				PendingVar pvi = gen.argument("java.lang.Object", var);
				vm.put(var, pvi);
			}
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", hi.name(), "<init>").flush();
			if (hi.inCard)
				ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic("org.flasck.android.FLEval", "java.lang.Object", "full", cardArg.getVar()), javaBaseName(name))).flush();
			for (Object o : hi.boundVars) {
				String var = ((HandlerLambda)o).var;
				ctor.assign(fs.get(var).asExpr(ctor), ctor.callStatic("org.flasck.android.FLEval", "java.lang.Object", "head", vm.get(var).getVar())).flush();
			}
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar cardArg = null;
			if (hi.inCard)
				cardArg = gen.argument("java.lang.Object", "card");
			PendingVar argsArg = gen.argument("[java.lang.Object", "args");
			gen.returns("java.lang.Object");
			NewMethodDefiner eval = gen.done();
			List<Expr> naList = new ArrayList<Expr>();
			if (hi.inCard)
				naList.add(cardArg.getVar());
			for (int k=0;k<hi.boundVars.size();k++)
				naList.add(eval.arrayElt(argsArg.getVar(), eval.intConst(k)));
			Expr[] newArgs = new Expr[naList.size()];
			naList.toArray(newArgs);
			Expr objArg;
			if (hi.inCard)
				objArg = cardArg.getVar();
			else
				objArg = eval.aNull();
			eval.ifOp(0xa2, eval.arraylen(argsArg.getVar()), eval.intConst(hi.boundVars.size()), 
					eval.returnObject(eval.makeNew("org.flasck.android.FLCurry", objArg, eval.classConst(javaNestedName(name)), argsArg.getVar())), 
					eval.returnObject(eval.makeNew(javaNestedName(name), newArgs))).flush();
		}
		{
			
		}
	}

	public void generate(Collection<HSIEForm> forms) {
		if (builder == null)
			return;
		for (HSIEForm f : forms) {
			logger.error("Considering form " + f + " with type " + f.mytype);
			f.dump(logger);
			int idx = f.fnName.lastIndexOf(".");
			String inClz;
			String fn = f.fnName.substring(idx+1);
			boolean needTrampolineClass;
			boolean wantThis = false;
			if (f.mytype == CodeType.HANDLER || f.mytype == CodeType.CONTRACT || f.mytype == CodeType.SERVICE) {
				int idx2 = f.fnName.lastIndexOf(".", idx-1);
				String clz = f.fnName.substring(0, idx2);
				String sub = f.fnName.substring(idx2+1, idx);
				inClz = clz +"$"+sub;
				needTrampolineClass = false;
			} else if (f.mytype == CodeType.CARD || f.mytype == CodeType.EVENTHANDLER) {
				inClz = f.fnName.substring(0, idx);
				if (f.mytype == CodeType.CARD) {
					needTrampolineClass = true;
					wantThis = true;
				} else
					needTrampolineClass = false;  // or maybe true; I don't think we've worked with EVENTHANDLERs enough to know; I just know CARD functions need a trampoline
			} else if (f.mytype == CodeType.FUNCTION || f.mytype == CodeType.STANDALONE) {
				String pkg = f.fnName.substring(0, idx);
				inClz = pkg +".PACKAGEFUNCTIONS";
				if (!builder.bce.hasClass(inClz)) {
					ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, inClz);
					bcc.superclass("java.lang.Object");
				}
				needTrampolineClass = true;
			} else
				throw new UtilException("Can't handle " + f.fnName + " of code type " + f.mytype);
			ByteCodeCreator bcc = builder.bce.get(inClz);
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, needTrampolineClass && !wantThis, fn);
			gen.returns("java.lang.Object");
			List<PendingVar> tmp = new ArrayList<PendingVar>();
			if (f.mytype == CodeType.HANDLER) // and others?
				gen.argument("org.flasck.android.post.DeliveryAddress", "_fromDA");
			int j = 0;
			for (@SuppressWarnings("unused") Object s : f.scoped)
				tmp.add(gen.argument("java.lang.Object", "_s"+(j++)));
			for (int i=0;i<f.nformal;i++)
				tmp.add(gen.argument("java.lang.Object", "_"+i));
			MethodDefiner meth = gen.done();
			j = 0;
			Map<String, Var> svars = new HashMap<String, Var>();
			Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();
			for (String s : f.scoped) {
				svars.put(s, tmp.get(j).getVar());
				j++;
			}
			for (int i=0;i<f.nformal;i++)
				vars.put(f.vars.get(i), tmp.get(i+j).getVar());
			Expr blk = generateBlock(meth, svars, vars, f, f, null);
			if (blk != null)
				blk.flush();
//			meth.returnObject(meth.myThis()).flush();
			
			// for package-level methods (i.e. regular floating functions in a functional language), generate a nested class
			if (needTrampolineClass) {
				ByteCodeCreator inner = new ByteCodeCreator(builder.bce, inClz + "$" + fn);
				inner.superclass("java.lang.Object");
				System.out.println("Creating class " + inner);
				if (wantThis) {
					FieldInfo fi = inner.defineField(true, Access.PRIVATE, bcc.getCreatedName(), "_card");
					GenericAnnotator ctor = GenericAnnotator.newConstructor(inner, false);
					PendingVar arg = ctor.argument(bcc.getCreatedName(), "card");
					MethodDefiner c = ctor.done();
					c.callSuper("void", "java.lang.Object", "<init>").flush();
					c.assign(fi.asExpr(c), arg.getVar()).flush();
					c.returnVoid().flush();
				}
				GenericAnnotator g2 = GenericAnnotator.newMethod(inner, !wantThis, "eval");
				g2.returns("java.lang.Object");
				PendingVar args = g2.argument("[java.lang.Object", "args");
				MethodDefiner m2 = g2.done();
				Expr[] fnArgs = new Expr[tmp.size()];
				for (int i=0;i<tmp.size();i++) {
					fnArgs[i] = m2.arrayElt(args.getVar(), m2.intConst(i));
				}
				Expr doCall;
				if (wantThis)
					doCall = m2.callVirtual("java.lang.Object", m2.getField("_card"), fn, fnArgs);
				else
					doCall = m2.callStatic(inClz, "java.lang.Object", fn, fnArgs);
				
				m2.returnObject(doCall).flush();
			}
		}
	}

	private Expr generateBlock(NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, HSIEForm f, HSIEBlock blk, Var assignReturnTo) {
		List<Expr> stmts = new ArrayList<Expr>();
		for (HSIEBlock h : blk.nestedCommands()) {
			if (h instanceof Head) {
				Head hh = (Head) h;
				Var hv = vars.get(hh.v);
				stmts.add(meth.assign(hv, meth.callStatic("org.flasck.android.FLEval", "java.lang.Object", "head", hv)));
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, "org.flasck.android.FLError"), meth.returnObject(hv), null));
			} else if (h instanceof Switch) {
				Switch s = (Switch)h;
				Var hv = vars.get(s.var);
				String ctor = s.ctor;
				if (ctor.indexOf(".") == -1)
					ctor = "org.flasck.android.builtin." + ctor;
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, ctor), generateBlock(meth, svars, vars, f, s, assignReturnTo), null));
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				Var hv = vars.get(c.var);
				Expr testVal = upcast(meth, exprValue(meth, c.value));
				stmts.add(meth.ifEquals(hv, testVal, generateBlock(meth, svars, vars, f, c, assignReturnTo), null));
			} else if (h instanceof BindCmd) {
//				into.add(JSForm.bind((BindCmd) h));
			} else if (h instanceof ReturnCmd) {
				ReturnCmd r = (ReturnCmd) h;
				if (r.var != null) {
					Var hv = vars.get(r.var.var);
					if (r.var.var.idx < f.nformal) {
						if (assignReturnTo != null) {
							ensureString(stmts, meth, hv);
							stmts.add(meth.assign(assignReturnTo, hv));
						} else
							stmts.add(meth.returnObject(hv));
					} else {
						if (r.deps != null) {
							for (CreationOfVar cov : r.deps) {
								Var v = vars.get(cov.var);
								if (v == null) {
									v = meth.avar("java.lang.Object", cov.var.toString());
									vars.put(cov.var, v);
								}
								Expr cl = closure(f, meth, svars, vars, f.mytype, f.getClosure(cov.var));
								stmts.add(meth.assign(v, cl));
							}
						}
						Expr cl = closure(f, meth, svars, vars, f.mytype, f.getClosure(r.var.var));
						if (assignReturnTo != null) {
							ensureString(stmts, meth, hv);
							stmts.add(meth.assign(assignReturnTo, cl));
						} else
							stmts.add(meth.returnObject(cl));
					}
				} else {
					Expr expr = appendValue(f, meth, svars, vars, f.mytype, r, 0);
					stmts.add(meth.returnObject(expr));
				}
			} else if (h instanceof ErrorCmd) {
				stmts.add(meth.returnObject(meth.makeNew("org.flasck.android.FLError", meth.stringConst(meth.getName() + ": case not handled"))));
			} else {
				System.out.println("Cannot generate block:");
				h.dumpOne((Logger)null, 0);
			}
		}
		if (stmts.isEmpty())
			return null;
		else if (stmts.size() == 1)
			return stmts.get(0);
		else
			return new BlockExpr(meth, stmts);
	}

	private void ensureString(List<Expr> stmts, NewMethodDefiner meth, Var blk) {
		if (blk == null)
			return;
		else if (blk.getType().equals("java.lang.String")) {
			// nothing to do ...
		} else if (blk.getType().equals("java.lang.Integer") || blk.getType().equals("int")) {
			stmts.add(meth.callStatic("java.lang.Integer", "java.lang.String", "toString", blk));
		} else
			throw new UtilException("Cannot handle " + blk.getType());
	}

	private Expr closure(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, HSIEBlock closure) {
		// Loop over everything in the closure pushing it onto the stack (in al)
		ExternalRef fn = ((PushCmd)closure.nestedCommands().get(0)).fn;
		Expr needsObject = null;
		boolean fromHandler = fntype == CodeType.AREA;
		Object defn = fn;
		if (fn != null) {
			while (defn instanceof PackageVar)
				defn = ((PackageVar)defn).defn;
			if (defn instanceof ObjectReference || defn instanceof CardFunction) {
				needsObject = meth.myThis();
				fromHandler |= fn.fromHandler();
			} else if (defn instanceof HandlerImplements) {
				HandlerImplements hi = (HandlerImplements) defn;
				if (hi.inCard)
					needsObject = meth.myThis();
				System.out.println("Creating handler " + fn + " in block " + closure);
			} else if (fn.toString().equals("FLEval.curry")) {
				ExternalRef f2 = ((PushCmd)closure.nestedCommands().get(1)).fn;
				if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
					needsObject = meth.myThis();
					fromHandler |= f2.fromHandler();
				}
			}
		}
		if (needsObject != null && fromHandler)
			needsObject = meth.getField("_card");
		int pos = 0;
		boolean isField = false;
		List<Expr> al = new ArrayList<Expr>();
		for (HSIEBlock b : closure.nestedCommands()) {
			PushCmd c = (PushCmd) b;
			if (c.fn != null && pos == 0) {
				isField = "FLEval.field".equals(c.fn);
			}
			if (c.fn != null && isField && pos == 2)
				System.out.println("c.fn = " + c.fn);
			else
				al.add(upcast(meth, appendValue(form, meth, svars, vars, fntype, c, pos)));
			pos++;
		}
		Expr clz = al.remove(0);
		String t = clz.getType();
		if (!t.equals("java.lang.Class") && (needsObject != null || !t.equals("java.lang.Object"))) {
			return meth.aNull();
//			throw new UtilException("Type of " + clz + " is not a Class but " + t);
//			clz = meth.castTo(clz, "java.lang.Class");
		}
		if (needsObject != null)
			return meth.makeNew("org.flasck.android.FLClosure", meth.as(needsObject, "java.lang.Object"), clz, meth.arrayOf("java.lang.Object", al));
		else
			return meth.makeNew("org.flasck.android.FLClosure", clz, meth.arrayOf("java.lang.Object", al));
	}

	private Expr upcast(NewMethodDefiner meth, Expr expr) {
		if (expr.getType().equals("int"))
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", expr);
		return expr;
	}

	private static Expr appendValue(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, PushReturn c, int pos) {
		if (c.fn != null) {
			if (c.fn instanceof PackageVar || c.fn instanceof ObjectReference) {
				boolean wantEval = false;
				Object defn = null;
				if (c.fn instanceof PackageVar) {
					defn = c.fn;
					while (defn instanceof PackageVar)
						defn = ((PackageVar)defn).defn;
					if (pos != 0)
						if (defn instanceof StructDefn && ((StructDefn)defn).fields.isEmpty())
							wantEval = true;
				}
				int idx = c.fn.uniqueName().lastIndexOf(".");
				String inside;
				String dot;
				String member;
				if (idx == -1) {
					inside = "org.flasck.android.builtin";
					dot = ".";
					member = c.fn.uniqueName();
				} else {
					String first = c.fn.uniqueName().substring(0, idx);
					if ("FLEval".equals(first)) {
						inside = "org.flasck.android.FLEval";
						member = StringUtil.capitalize(c.fn.uniqueName().substring(idx+1));
					} else {
						inside = c.fn.uniqueName().substring(0, idx);
						member = c.fn.uniqueName().substring(idx+1);
					}
					dot = "$";
				}
				String clz;
				if (defn instanceof FunctionDefinition || defn instanceof MethodDefinition || (defn instanceof Type && ((Type)defn).iam == WhatAmI.FUNCTION)) {
					if (inside.equals("org.flasck.android.FLEval"))
						clz = inside + "$" + member;
					else
						clz = inside + ".PACKAGEFUNCTIONS$" + member;
				} else {
					clz = inside + dot + member;
				}
				meth.getBCC().addInnerClassReference(Access.PUBLICSTATIC, inside, member);
				if (!wantEval) { // handle the simple class case ...
					return meth.classConst(clz);
				} else {
					return meth.callStatic(clz, "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<Expr>()));
				}
			} else if (c.fn instanceof ScopedVar) {
				ScopedVar sv = (ScopedVar) c.fn;
				if (sv.definedLocally) {
					return null;
				}
				if (!svars.containsKey(c.fn.uniqueName()))
					throw new UtilException("ScopedVar not in scope: " + c.fn);
				return svars.get(c.fn.uniqueName());
			} else if (c.fn instanceof CardFunction) {
				String jnn = javaNestedName(c.fn.uniqueName());
				return meth.makeNew(jnn, meth.myThis());
			} else if (c.fn instanceof CardMember) {
				if (fntype == CodeType.CARD || fntype == CodeType.EVENTHANDLER)
					return meth.myThis();
				else if (fntype == CodeType.HANDLER || fntype == CodeType.CONTRACT || fntype == CodeType.AREA) {
					CardMember cm = (CardMember)c.fn;
					Expr field = meth.getField(meth.getField("_card"), cm.var);
					return field;
				} else
					throw new UtilException("Can't handle " + fntype + " for card member");
			} else if (c.fn instanceof HandlerLambda) {
				if (fntype == CodeType.HANDLER)
					return meth.getField(((HandlerLambda)c.fn).var);
				else
					throw new UtilException("Can't handle " + fntype + " with handler lambda");
			} else
				throw new UtilException("Can't handle " + c.fn + " of type " + c.fn.getClass());
		} else if (c.ival != null)
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(c.ival));
		else if (c.var != null)
			return vars.get(c.var.var);
		else if (c.sval != null)
			return meth.stringConst(c.sval.text);
		else if (c.tlv != null) {
			return meth.getField(meth.getField("_src_" + c.tlv.name), c.tlv.name);
//			sb.append("this._src_" + c.tlv.name + "." + c.tlv.name);
//		} else if (c.func != null) {
//			int x = c.func.name.lastIndexOf('.');
//			if (x == -1)
//				throw new UtilException("Invalid function name: " + c.func.name);
//			else
//				sb.append(c.func.name.substring(0, x+1) + "prototype" + c.func.name.substring(x));
		}
		else if (c.csr != null) {
			if (c.csr.fromHandler)
				return meth.getField("_card");
			else
				return meth.myThis();
		} else
			throw new UtilException("What are you pushing? " + c);
	}

	private Expr exprValue(NewMethodDefiner meth, Object value) {
		if (value instanceof Integer)
			return meth.intConst((Integer)value);
		else if (value instanceof Boolean)
			return meth.intConst(((Boolean)value)?1:0);
		else
			throw new UtilException("Cannot handle " + value.getClass());
	}

	public NewMethodDefiner generateRender(String clz, String topBlock) {
		if (builder == null)
			return null;
		ByteCodeCreator bcc = builder.bce.get(clz);
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "render");
		PendingVar into = gen.argument("java.lang.String", "into");
		gen.returns("void");
		NewMethodDefiner render = gen.done();
		render.makeNewVoid(javaNestedName(topBlock), render.myThis(), render.as(render.makeNew("org.flasck.android.areas.CardArea", render.getField(render.myThis(), "_wrapper"), render.as(render.myThis(), "org.flasck.android.FlasckActivity"), into.getVar()), "org.flasck.android.areas.Area")).flush();
		render.returnVoid().flush();
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(topBlock), javaNestedSimpleName(topBlock));
		return render;
	}

	public CGRContext area(String clz, String base, String customTag) {
		if (builder == null)
			return null;
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, javaNestedName(clz));
		String baseClz = "org.flasck.android.areas." + base;
		bcc.superclass(baseClz);
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.areas.Area"), "_parent");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(clz), javaNestedSimpleName(clz));
		FieldInfo card = bcc.defineField(true, Access.PRIVATE, javaBaseName(clz), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(javaBaseName(clz), "cardArg");
			PendingVar parent = gen.argument("org/flasck/android/areas/Area", "parent");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", baseClz, "<init>", parent.getVar(), customTag == null ? ctor.as(ctor.aNull(), "java.lang.String") : ctor.stringConst(customTag)).flush();
			ctor.assign(card.asExpr(ctor), cardArg.getVar()).flush();
			return new CGRContext(bcc, ctor, cardArg.getVar(), parent.getVar());
		}
	}

	public void newVar(CGRContext cgrx, String newVar) {
		if (cgrx == null)
			return;
		System.out.println("Creating var " + newVar + " in " + cgrx.bcc.getCreatedName());
		FieldInfo src = cgrx.bcc.defineField(true, Access.PUBLIC, cgrx.bcc.getCreatedName(), "_src_"+newVar);
		cgrx.bcc.defineField(false, Access.PUBLIC, "java.lang.Object", newVar);
		cgrx.ctor.assign(src.asExpr(cgrx.ctor), cgrx.ctor.myThis()).flush();
	}

	public void copyVar(CGRContext cgrx, String parentClass, String definedInType, String s) {
		if (cgrx == null)
			return;
		System.out.println("Copying var " + s + " from " + parentClass + " into " + cgrx.bcc.getCreatedName());
		FieldInfo src = cgrx.bcc.defineField(true, Access.PUBLIC, javaNestedName(definedInType), "_src_"+s);
		cgrx.ctor.assign(src.asExpr(cgrx.ctor), cgrx.ctor.getField(cgrx.ctor.castTo(cgrx.parent, javaNestedName(parentClass)), "_src_"+s)).flush();
	}

	public void setSimpleClass(CGRContext cgrx, String css) {
		if (cgrx == null)
			return;
		cgrx.ctor.callVirtual("void", cgrx.ctor.myThis(), "setCSS", cgrx.ctor.stringConst(css)).flush();
	}

	public void createNested(CGRContext cgrx, String v, String cn) {
		if (cgrx == null)
			return;
		Var storeAs = cgrx.ctor.avar(cn, v);
		cgrx.ctor.assign(storeAs, cgrx.ctor.makeNew(javaNestedName(cn), cgrx.card, cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"))).flush();
	}

	public void needAddHandlers(CGRContext cgrx) {
		if (cgrx == null)
			return;
		GenericAnnotator ah = GenericAnnotator.newMethod(cgrx.bcc, false, "_add_handlers");
		ah.returns("java.lang.Object");
		MethodDefiner ahMeth = ah.done();
		cgrx.currentMethod = ahMeth;
		ahMeth.voidExpr(ahMeth.callStatic("android.util.Log", "int", "e", ahMeth.stringConst("FlasckLib"), ahMeth.stringConst("Need to add the handlers"))).flush();
		ahMeth.returnObject(ahMeth.aNull()).flush();
	}

	public void setVarFormats(CGRContext cgrx, HSIEForm form) {
		if (cgrx == null)
			return;
		GenericAnnotator svf = GenericAnnotator.newMethod(cgrx.bcc, false, "_setVariableFormats");
		svf.returns("java.lang.Object");
		MethodDefiner meth = svf.done();
		cgrx.currentMethod = meth;
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to set variable formats"))).flush();
		Var fmts = generateFunctionFromForm(meth, form);
		meth.callSuper("void", "org.flasck.android.Area", "_setCSSObj", fmts).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void setText(CGRContext cgrx, String text) {
		if (builder == null)
			return;
		cgrx.ctor.callVirtual("void", cgrx.ctor.myThis(), "_setText", cgrx.ctor.stringConst(text)).flush();
	}

	public void contentExpr(CGRContext cgrx, HSIEForm form, boolean rawHTML) {
		if (builder == null)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_contentExpr");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
		Var str = generateFunctionFromForm(meth, form);
		if (rawHTML)
			meth.callSuper("void", "org.flasck.android.TextArea", "_insertHTML", str).flush();
		else
			meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void newListChild(CGRContext cgrx, String child) {
		if (builder == null)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_newChild");
		PendingVar ck = gen.argument("org.flasck.android.builtin.Crokey", "crokey");
		gen.returns("org.flasck.android.areas.Area");
		NewMethodDefiner meth = gen.done();
		Var ret = meth.avar("org.flasck.android.areas.Area", "ret");
		meth.assign(ret, meth.makeNew(javaNestedName(child), meth.getField("_card"), meth.as(meth.myThis(), "org.flasck.android.areas.Area"))).flush();
		FieldExpr crokeyid = new FieldObject(false, "org.flasck.android.builtin.Crokey", new JavaType("java.lang.Object"), "id").useOn(meth, ck.getVar());
		meth.callVirtual("void", ret, "bindVar", meth.stringConst("_crokey"), crokeyid).flush();
		meth.returnObject(ret).flush();
	}

	public void yoyoExpr(CGRContext cgrx, HSIEForm form) {
		if (builder == null)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_yoyoExpr");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
//		Var str = meth.avar("java.lang.String", "str");
		Expr blk = generateFunctionFromForm(meth, form);
		// TODO: if "blk" is null, that reflects the possibility of the method returning before we get here ... Huh?
		if (blk == null) return;
//		meth.assign(str, blk).flush();
//		meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
//		JSForm.assign(cexpr, "var card", form);
//		cexpr.add(JSForm.flex("this._updateToCard(card)"));

		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to implement yoyo card"))).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	protected Var generateFunctionFromForm(NewMethodDefiner meth, HSIEForm form) {
		Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();
		Map<String, Var> svars = new HashMap<String, Var>();
		Var myvar = meth.avar("java.lang.Object", "tmp");
		generateBlock(meth, svars, vars, form, form, myvar).flush();
		return myvar;
	}

	public void onAssign(CGRContext cgrx, CardMember valExpr, String call) {
		if (builder == null)
			return;
		// I think this is removing the "prototype" ... at some point, rationalize all this
		// so that we pass around a struct with "package" "class", "area", "method" or whatever we need ...
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		cgrx.ctor.callVirtual("void", cgrx.ctor.getField(cgrx.ctor.getField("_card"), "_wrapper"), "onAssign", cgrx.ctor.as(cgrx.ctor.getField("_card"), "java.lang.Object"), cgrx.ctor.stringConst(valExpr.var), cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"), cgrx.ctor.stringConst(call)).flush();
	}

	public void onAssign(CGRContext cgrx, Expr expr, String field, String call) {
		if (builder == null)
			return;
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		cgrx.ctor.callVirtual("void", cgrx.ctor.getField(cgrx.ctor.getField("_card"), "_wrapper"), "onAssign", cgrx.ctor.as(expr, "java.lang.Object"), cgrx.ctor.stringConst(field), cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"), cgrx.ctor.stringConst(call)).flush();
	}

	public void interested(CGRContext cgrx, String var, String call) {
		if (builder == null)
			return;
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		NewMethodDefiner meth = cgrx.ctor;
		meth.callVirtual("void", meth.getField("_src_"+var), "_interested", meth.as(meth.myThis(), "org.flasck.android.areas.Area"), meth.stringConst(call)).flush();
	}

	public void addAssign(CGRContext cgrx, String call) {
		if (builder == null)
			return;
		int idx = call.lastIndexOf(".prototype");
		call = call.substring(idx+11);
		cgrx.ctor.voidExpr(cgrx.ctor.callVirtual("java.lang.Object", cgrx.ctor.myThis(), call)).flush();
	}

	public void assignToVar(CGRContext cgrx, String varName) {
		if (builder == null)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_assignToVar");
		PendingVar arg = gen.argument("java.lang.Object", "obj");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
		Var obj = arg.getVar();
		FieldExpr curr = meth.getField(varName);
		FieldExpr wrapper = meth.getField("_wrapper");
		Expr parent = meth.castTo(meth.getField("_parent"), "org.flasck.android.areas.ListArea");
		FieldExpr croset = new FieldObject(false, "org.flasck.android.areas.ListArea", new JavaType("org.flasck.android.builtin.Croset"), "_current").useOn(meth, parent);
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("In _assignToVar"))).flush();
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.callStatic("java.lang.String",  "java.lang.String", "valueOf", curr))).flush();
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.callStatic("java.lang.String",  "java.lang.String", "valueOf", obj))).flush();
		meth.ifOp(0xa6, curr, obj, meth.returnObject(meth.aNull()), null).flush();
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("survived first test"))).flush();
		meth.callVirtual("void", wrapper, "removeOnCrosetReplace", croset, meth.as(meth.myThis(), "org.flasck.android.areas.Area"), curr).flush();
		meth.assign(curr, obj).flush();
		meth.callVirtual("void", wrapper, "onCrosetReplace", croset, meth.as(meth.myThis(), "org.flasck.android.areas.Area"), curr).flush();
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("calling _fireInterests"))).flush();
		meth.callVirtual("void", meth.myThis(), "_fireInterests").flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void done(CGRContext cgrx) {
		if (builder == null)
			return;
		cgrx.ctor.returnVoid().flush();
	}

	public void write() {
		if (builder == null)
			return;
		for (ByteCodeCreator bcc : builder.bce.all()) {
			File wto = new File(builder.qbcdir, FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
			bcc.writeTo(wto);
		}
	}

	private String javaBaseName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx);
	}

	private static String javaNestedName(String clz) {
		if (clz.indexOf("$") != -1)
			throw new UtilException("Nested of nested?");
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx) + "$" + clz.substring(idx+1);
	}

	private String javaNestedSimpleName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(idx+1);
	}
}
