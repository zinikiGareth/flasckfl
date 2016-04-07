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
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
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
import org.zinutils.bytecode.FieldInfo;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.ReturnX;
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
				jt = new JavaType(sf.type.name());
			} else if (sf.type instanceof ObjectDefn) {
				jt = new JavaType(sf.type.name());
			} else if (sf.type instanceof Type) {
				jt = new JavaType(sf.type.name());
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
		if (builder == null)
			return;
		for (HSIEForm f : forms) {
			logger.info("Considering form " + f);
			f.dump(logger);
			int idx = f.fnName.lastIndexOf(".");
			String inClz;
			String fn = f.fnName.substring(idx+1);
			boolean isStatic;
			if (f.mytype == CodeType.HANDLER || f.mytype == CodeType.CONTRACT || f.mytype == CodeType.SERVICE) {
				int idx2 = f.fnName.lastIndexOf(".", idx-1);
				String clz = f.fnName.substring(0, idx2);
				String sub = f.fnName.substring(idx2+1, idx);
				inClz = clz +"$"+sub;
				isStatic = false;
			} else if (f.mytype == CodeType.CARD || f.mytype == CodeType.EVENTHANDLER) {
				inClz = f.fnName.substring(0, idx);
				isStatic = false;
			} else if (f.mytype == CodeType.FUNCTION || f.mytype == CodeType.STANDALONE) {
				String pkg = f.fnName.substring(0, idx);
				inClz = pkg +".PackageFunctions";
				if (!builder.bce.hasClass(inClz)) {
					ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, inClz);
					bcc.superclass("java.lang.Object");
				}
				isStatic = true;
			} else
				throw new UtilException("Can't handle " + f.fnName + " of code type " + f.mytype);
			ByteCodeCreator bcc = builder.bce.get(inClz);
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, isStatic, fn);
			gen.returns("java.lang.Object");
			List<PendingVar> tmp = new ArrayList<PendingVar>();
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
			Expr blk = generateBlock(meth, svars, vars, f, f);
			if (blk != null)
				blk.flush();
//			meth.returnObject(meth.myThis()).flush();
		}
	}

	private Expr generateBlock(NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, HSIEForm f, HSIEBlock blk) {
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
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, s.ctor), generateBlock(meth, svars, vars, f, s), null));
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				Var hv = vars.get(c.var);
				Expr testVal = upcast(meth, exprValue(meth, c.value));
				stmts.add(meth.ifEquals(hv, testVal, generateBlock(meth, svars, vars, f, c), null));
			} else if (h instanceof BindCmd) {
//				into.add(JSForm.bind((BindCmd) h));
			} else if (h instanceof ReturnCmd) {
				ReturnCmd r = (ReturnCmd) h;
				if (r.var != null) {
					Var hv = vars.get(r.var.var);
					if (r.var.var.idx < f.nformal)
						stmts.add(meth.returnObject(hv));
					else {
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
				h.dumpOne(null, 0);
			}
		}
		if (stmts.isEmpty())
			return null;
		else if (stmts.size() == 1)
			return stmts.get(0);
		else
			return new BlockExpr(meth, stmts);
	}

	private Expr closure(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, HSIEBlock closure) {
		// Loop over everything in the closure pushing it onto the stack (in al)
		ExternalRef fn = ((PushCmd)closure.nestedCommands().get(0)).fn;
		Expr needsObject = null;
		boolean fromHandler = fntype == CodeType.AREA;
		if (fn != null) {
			if (fn instanceof ObjectReference || fn instanceof CardFunction) {
				needsObject = meth.myThis();
				fromHandler |= fn.fromHandler();
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
				if (pos != 0 && c.fn instanceof PackageVar) {
					Object defn = ((PackageVar)c.fn).defn;
					if (defn instanceof StructDefn && ((StructDefn)defn).fields.isEmpty())
						wantEval = true;
				}
				int idx = c.fn.uniqueName().lastIndexOf(".");
				String clz;
				if (idx == -1)
					clz = "org.flasck.android.builtin." + c.fn.uniqueName();
				else {
					String first = c.fn.uniqueName().substring(0, idx);
					String inside;
					String member;
					if ("FLEval".equals(first)) {
						inside = "org.flasck.android.FLEval";
						member = StringUtil.capitalize(c.fn.uniqueName().substring(idx+1));
					} else {
						inside = c.fn.uniqueName().substring(0, idx);
						member = c.fn.uniqueName().substring(idx+1);
					}
					clz = inside + "$" + member;
					meth.getBCC().addInnerClassReference(Access.PUBLICSTATIC, inside, member);
				}
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
				return meth.stringConst("Need a function pointer for method " + c.fn);
//				String jsname = c.fn.uniqueName();
//				int idx = jsname.lastIndexOf(".");
//				jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
//				sb.append(jsname);
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
			System.out.println("TLV: " + c.tlv.name);
			return meth.stringConst("HackTLV:" + c.tlv.name);
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

	public CGRContext area(String clz, String base) {
		ByteCodeCreator bcc = new ByteCodeCreator(builder.bce, javaNestedName(clz));
		String baseClz = "org.flasck.android.areas." + base;
		bcc.superclass(baseClz);
		bcc.addInnerClassReference(Access.PUBLICSTATIC, javaBaseName(clz), javaNestedSimpleName(clz));
		FieldInfo card = bcc.defineField(true, Access.PRIVATE, javaBaseName(clz), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(javaBaseName(clz), "cardArg");
			PendingVar parent = gen.argument("org/flasck/android/areas/Area", "parent");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", baseClz, "<init>", parent.getVar(), ctor.as(ctor.aNull(), "java.lang.String")).flush();
			ctor.assign(card.asExpr(ctor), cardArg.getVar()).flush();
//			ctor.callVirtual("void", ctor.getField(card.asExpr(ctor), "_wrapper"), "onAssign", ctor.stringConst("counter"), ctor.as(ctor.myThis(), "org.flasck.android.Area"), ctor.stringConst("_contentExpr")).flush();
//			ctor.callVirtual("void", ctor.myThis(), "_contentExpr").flush();
			return new CGRContext(bcc, ctor, cardArg.getVar(), parent.getVar());
//			ctor.returnVoid().flush();
			
		}
	}

	public void setSimpleClass(CGRContext cgrx, String css) {
		cgrx.ctor.callVirtual("void", cgrx.ctor.myThis(), "setCSS", cgrx.ctor.stringConst(css)).flush();
	}

	public void createNested(CGRContext cgrx, String v, String cn) {
		System.out.println("!! Creating nested area for " + cn + " assigning to " + v);
		Var storeAs = cgrx.ctor.avar(cn, v);
		cgrx.ctor.assign(storeAs, cgrx.ctor.makeNew(javaNestedName(cn), cgrx.card, cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"))).flush();
	}

	public void needAddHandlers(CGRContext cgrx) {
		GenericAnnotator ah = GenericAnnotator.newMethod(cgrx.bcc, false, "_add_handlers");
		ah.returns("java.lang.Object");
		MethodDefiner ahMeth = ah.done();
		cgrx.currentMethod = ahMeth;
		ahMeth.callStatic("android.util.Log", "void", "e", ahMeth.stringConst("Need to add the handlers"));
		ahMeth.returnObject(ahMeth.aNull()).flush();
	}

	public void contentExpr(CGRContext cgrx, HSIEForm form) {
		if (builder == null)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_contentExpr");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
		Var str = meth.avar("java.lang.String", "str");
		Expr blk = generateFunctionFromForm(meth, form);
		if (blk == null) return;
		meth.assign(str, blk).flush();
		meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
		meth.returnObject(meth.aNull()).flush();
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

		meth.callStatic("android.util.Log", "void", "e", meth.stringConst("Need to implement yoyo card"));
		meth.returnObject(meth.aNull()).flush();
	}

	protected Expr generateFunctionFromForm(NewMethodDefiner meth, HSIEForm form) {
		Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();
		Map<String, Var> svars = new HashMap<String, Var>();
		Expr blk = generateBlock(meth, svars, vars, form, form);
		if (blk instanceof ReturnX) {
			// nothing we can do
			System.out.println("Already Returned? Huh?");
			blk.flush();
			return null;
		} else if (blk.getType().equals("java.lang.String")) {
			// nothing to do ...
		} else if (blk.getType().equals("java.lang.Integer") || blk.getType().equals("int")) {
			blk = meth.callStatic("java.lang.Integer", "java.lang.String", "toString", blk);
		} else
			throw new UtilException("Cannot handle " + blk.getType());
		return blk;
	}

	public void onAssign(CGRContext cgrx, CardMember valExpr) {
		if (builder == null)
			return;
		cgrx.ctor.callVirtual("void", cgrx.ctor.getField(cgrx.ctor.getField("_card"), "_wrapper"), "onAssign", cgrx.ctor.stringConst(valExpr.var), cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"), cgrx.ctor.stringConst("_contentExpr")).flush();
	}
	
	public void addAssign(CGRContext cgrx, String call) {
		if (builder == null)
			return;
		int idx = call.lastIndexOf(".prototype");
		call = call.substring(idx+11);
		cgrx.ctor.voidExpr(cgrx.ctor.callVirtual("java.lang.Object", cgrx.ctor.myThis(), call)).flush();
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

	private String javaNestedName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx) + "$" + clz.substring(idx+1);
	}

	private String javaNestedSimpleName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(idx+1);
	}
}
