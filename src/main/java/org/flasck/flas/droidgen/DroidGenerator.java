package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.android.AndroidLabel;
import org.flasck.flas.commonBase.android.AndroidLaunch;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.rewriter.CodeGenRegistry;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.bytecode.Annotation;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.FieldObject;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidGenerator implements RepoVisitor {
	private final boolean doBuild;
	private ByteCodeStorage bce;

	public DroidGenerator(HSIE hsie, boolean doBuild, ByteCodeStorage bce) {
		this.doBuild = doBuild;
		this.bce = bce;
	}

	public void registerWith(CodeGenRegistry rewriter) {
		if (doBuild)
			rewriter.registerCodeGenerator(this);
	}
	
	@Override
	public void visitStructDefn(RWStructDefn sd) {
		if (!doBuild || !sd.generate)
			return;
		ByteCodeSink bcc = bce.newClass(sd.name());
		DroidStructFieldGenerator fg = new DroidStructFieldGenerator(bcc, Access.PUBLIC);
		sd.visitFields(fg);
		bcc.superclass(J.FLAS_OBJECT);
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.FLAS_OBJECT, "<init>").flush();
			ctor.returnVoid().flush();
		}
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_doFullEval");
		gen.returns("void");
		NewMethodDefiner dfe = gen.done();
		DroidStructFieldInitializer fi = new DroidStructFieldInitializer(dfe, fg.fields);
		sd.visitFields(fi);
		dfe.returnVoid().flush();
	}

	@Override
	public void visitContractDecl(RWContractDecl cd) {
		if (!doBuild)
			return;
		ByteCodeSink bcc = bce.newClass(cd.name());
		bcc.superclass(J.CONTRACT_IMPL);
		bcc.makeAbstract();
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.CONTRACT_IMPL, "<init>").flush();
			ctor.returnVoid().flush();
		}
		
		for (RWContractMethodDecl m : cd.methods) {
			if (m.dir.equals("down")) {
				GenericAnnotator gm = GenericAnnotator.newMethod(bcc, false, m.name);
				gm.returns("java.lang.Object");
				int k = 0;
				for (@SuppressWarnings("unused") Object a : m.args)
					gm.argument("java.lang.Object", "arg"+(k++));
				gm.done();
			}
		}
	}

	public void visitCardGrouping(CardGrouping grp) {
		if (!doBuild)
			return;
		ByteCodeSink bcc = bce.newClass(grp.struct.name());
		bcc.superclass(J.FLASCK_ACTIVITY);
		bcc.inheritsField(false, Access.PUBLIC, J.WRAPPER, "_wrapper");
		grp.struct.visitFields(new DroidStructFieldGenerator(bcc, Access.PROTECTED));
		for (ContractGrouping x : grp.contracts) {
			if (x.referAsVar != null)
				bcc.defineField(false, Access.PROTECTED, new JavaType(DroidUtils.javaNestedName(x.implName.jsName())), x.referAsVar);
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), DroidUtils.javaNestedSimpleName(x.implName.jsName()));
		}
		for (HandlerGrouping h : grp.handlers) {
			bcc.addInnerClassReference(Access.PUBLICSTATIC, bcc.getCreatedName(), DroidUtils.javaNestedSimpleName(h.impl.hiName));
		}
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", J.FLASCK_ACTIVITY, "<init>").flush();
			ctor.returnVoid().flush();
		}
		{
			GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "onCreate");
			PendingVar sis = gen.argument("android.os.Bundle", "savedState");
			gen.returns("void");
			NewMethodDefiner oc = gen.done();
			oc.setAccess(Access.PROTECTED);
			oc.callSuper("void", J.FLASCK_ACTIVITY, "onCreate", sis.getVar()).flush();
			for (ContractGrouping x : grp.contracts) {
				IExpr impl = oc.makeNew(DroidUtils.javaNestedName(x.implName.jsName()), oc.myThis());
				if (x.referAsVar != null) {
					FieldExpr fe = oc.getField(x.referAsVar);
					oc.assign(fe, impl).flush();
					impl = fe;
				}
				oc.callVirtual("void", oc.myThis(), "registerContract", oc.stringConst(x.type), oc.as(impl, J.CONTRACT_IMPL)).flush();
			}
			oc.callSuper("void", J.FLASCK_ACTIVITY, "ready").flush();
			oc.returnVoid().flush();
		}
		// TODO: I feel this should come from the "app" definition file, NOT the "platform" spec ...
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

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		if (!doBuild)
			return;
		CSName name = (CSName) ci.realName;
		String un = name.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(un));
		bcc.superclass(ci.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(un)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(un), DroidUtils.javaNestedSimpleName(un));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(un), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", ci.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
		
	}

	public void visitServiceImpl(RWContractService cs) {
		if (!doBuild)
			return;
		String name = cs.realName.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(name));
		bcc.superclass(cs.name());
		IFieldInfo fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(name)), "_card");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(name), DroidUtils.javaNestedSimpleName(name));
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(name), "card");
			NewMethodDefiner ctor = gen.done();
			ctor.callSuper("void", cs.name(), "<init>").flush();
			ctor.assign(fi.asExpr(ctor), cardArg.getVar()).flush();
			ctor.returnVoid().flush();
		}
	}

	public void visitHandlerImpl(RWHandlerImplements hi) {
		if (!doBuild)
			return;
		String name = hi.handlerName.uniqueName();
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(name));
		bcc.superclass(hi.name());
		IFieldInfo fi = null;
		if (hi.inCard)
			fi = bcc.defineField(false, Access.PRIVATE, new JavaType(DroidUtils.javaBaseName(name)), "_card");
		Map<String, IFieldInfo> fs = new TreeMap<>();
		for (Object o : hi.boundVars) {
			String var = ((HandlerLambda)o).var;
			IFieldInfo hli = bcc.defineField(false, Access.PRIVATE, new JavaType("java.lang.Object"), var);
			fs.put(var, hli);
		}
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(name), DroidUtils.javaNestedSimpleName(name));
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
				ctor.assign(fi.asExpr(ctor), ctor.castTo(ctor.callStatic("org.flasck.android.FLEval", "java.lang.Object", "full", cardArg.getVar()), DroidUtils.javaBaseName(name))).flush();
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
					eval.returnObject(eval.makeNew("org.flasck.jvm.FLCurry", objArg, eval.classConst(DroidUtils.javaNestedName(name)), argsArg.getVar())), 
					eval.returnObject(eval.makeNew(DroidUtils.javaNestedName(name), newArgs))).flush();
		}
	}

	public void generate(Collection<HSIEForm> forms) {
		if (!doBuild)
			return;
		for (HSIEForm f : forms) {
			new FormGenerator(bce, f).generate();
		}
	}

	public NewMethodDefiner generateRender(String clz, String topBlock) {
		if (!doBuild)
			return null;
		ByteCodeSink bcc = bce.get(clz);
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "render");
		PendingVar into = gen.argument("java.lang.String", "into");
		gen.returns("void");
		NewMethodDefiner render = gen.done();
		render.makeNewVoid(DroidUtils.javaNestedName(topBlock), render.myThis(), (Expr)render.as(render.makeNew("org.flasck.android.areas.CardArea", render.getField(render.myThis(), "_wrapper"), (Expr)render.as(render.myThis(), J.FLASCK_ACTIVITY), into.getVar()), "org.flasck.android.areas.Area")).flush();
		render.returnVoid().flush();
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(topBlock), DroidUtils.javaNestedSimpleName(topBlock));
		return render;
	}

	public CGRContext area(String clz, String base, String customTag) {
		if (!doBuild)
			return null;
		ByteCodeSink bcc = bce.newClass(DroidUtils.javaNestedName(clz));
		String baseClz = "org.flasck.android.areas." + base;
		bcc.superclass(baseClz);
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.Wrapper"), "_wrapper");
		bcc.inheritsField(false, Access.PUBLIC, new JavaType("org.flasck.android.areas.Area"), "_parent");
		bcc.addInnerClassReference(Access.PUBLICSTATIC, DroidUtils.javaBaseName(clz), DroidUtils.javaNestedSimpleName(clz));
		IFieldInfo card = bcc.defineField(true, Access.PRIVATE, DroidUtils.javaBaseName(clz), "_card");
		{
			GenericAnnotator gen = GenericAnnotator.newConstructor(bcc, false);
			PendingVar cardArg = gen.argument(DroidUtils.javaBaseName(clz), "cardArg");
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
		IFieldInfo src = cgrx.bcc.defineField(true, Access.PUBLIC, cgrx.bcc.getCreatedName(), "_src_"+newVar);
		cgrx.bcc.defineField(false, Access.PUBLIC, "java.lang.Object", newVar);
		cgrx.ctor.assign(src.asExpr(cgrx.ctor), cgrx.ctor.myThis()).flush();
	}

	public void copyVar(CGRContext cgrx, String parentClass, String definedInType, String s) {
		if (cgrx == null)
			return;
		IFieldInfo src = cgrx.bcc.defineField(true, Access.PUBLIC, DroidUtils.javaNestedName(definedInType), "_src_"+s);
		cgrx.ctor.assign(src.asExpr(cgrx.ctor), cgrx.ctor.getField(cgrx.ctor.castTo(cgrx.parent, DroidUtils.javaNestedName(parentClass)), "_src_"+s)).flush();
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
		cgrx.ctor.assign(storeAs, (Expr) cgrx.ctor.makeNew(DroidUtils.javaNestedName(cn), cgrx.card, (Expr)cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"))).flush();
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

	public void setVarFormats(CGRContext cgrx, String tfn) {
		if (cgrx == null)
			return;
		GenericAnnotator svf = GenericAnnotator.newMethod(cgrx.bcc, false, "_setVariableFormats");
		svf.returns("java.lang.Object");
		MethodDefiner meth = svf.done();
		cgrx.currentMethod = meth;
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to set variable formats"))).flush();
		meth.callSuper("void", "org.flasck.android.Area", "_setCSSObj", meth.callVirtual("java.lang.String", meth.myThis(), tfn)).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void setText(CGRContext cgrx, String text) {
		if (!doBuild)
			return;
		cgrx.ctor.callVirtual("void", cgrx.ctor.myThis(), "_setText", cgrx.ctor.stringConst(text)).flush();
	}

	public void contentExpr(CGRContext cgrx, String tfn, boolean rawHTML) {
		if (!doBuild)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_contentExpr");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
		
//		The rest of this code is basically correct, it's just that we used to have an HSIE block here
		// that we converted into a Var.  Now we have a function to call, so we need to replace "str" with "tfn()"
		IExpr str = meth.callVirtual(JavaType.string.getActual(), meth.myThis(), tfn);
		if (rawHTML)
			meth.callSuper("void", "org.flasck.android.TextArea", "_insertHTML", str).flush();
		else
			meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void newListChild(CGRContext cgrx, String child) {
		if (!doBuild)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_newChild");
		PendingVar ck = gen.argument("org.flasck.android.builtin.Crokey", "crokey");
		gen.returns("org.flasck.android.areas.Area");
		NewMethodDefiner meth = gen.done();
		Var ret = meth.avar("org.flasck.android.areas.Area", "ret");
		meth.assign(ret, (Expr) meth.makeNew(DroidUtils.javaNestedName(child), meth.getField("_card"), (Expr)meth.as(meth.myThis(), "org.flasck.android.areas.Area"))).flush();
		FieldExpr crokeyid = new FieldObject(false, "org.flasck.android.builtin.Crokey", new JavaType("java.lang.Object"), "id").useOn(meth, ck.getVar());
		meth.callVirtual("void", ret, "bindVar", meth.stringConst("_crokey"), crokeyid).flush();
		meth.returnObject(ret).flush();
	}

	public void yoyoExpr(CGRContext cgrx, String tfn) {
		if (!doBuild)
			return;
		GenericAnnotator gen = GenericAnnotator.newMethod(cgrx.bcc, false, "_yoyoExpr");
		gen.returns("java.lang.Object");
		// TODO: HSIE: most of this was commented out when I got here (see 27a2f6cfdd5d90b9f9cfc6abaa193edee57b0904)
		NewMethodDefiner meth = gen.done();
//		Var str = meth.avar("java.lang.String", "str");
		IExpr blk = meth.callVirtual("java.lang.String", meth.myThis(), tfn);
		// TODO: if "blk" is null, that reflects the possibility of the method returning before we get here ... Huh?
		if (blk == null) return;
//		meth.assign(str, blk).flush();
//		meth.callSuper("void", "org.flasck.android.TextArea", "_assignToText", str).flush();
//		JSForm.assign(cexpr, "var card", form);
//		cexpr.add(JSForm.flex("this._updateToCard(card)"));

		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to implement yoyo card"))).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void onAssign(CGRContext cgrx, CardMember valExpr, String call) {
		if (!doBuild)
			return;
		// I think this is removing the "prototype" ... at some point, rationalize all this
		// so that we pass around a struct with "package" "class", "area", "method" or whatever we need ...
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		cgrx.ctor.callVirtual("void", cgrx.ctor.getField(cgrx.ctor.getField("_card"), "_wrapper"), "onAssign", (Expr)cgrx.ctor.as(cgrx.ctor.getField("_card"), "java.lang.Object"), cgrx.ctor.stringConst(valExpr.var), (Expr)cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"), cgrx.ctor.stringConst(call)).flush();
	}

	public void onAssign(CGRContext cgrx, Expr expr, String field, String call) {
		if (!doBuild)
			return;
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		cgrx.ctor.callVirtual("void", cgrx.ctor.getField(cgrx.ctor.getField("_card"), "_wrapper"), "onAssign", (Expr)cgrx.ctor.as(expr, "java.lang.Object"), cgrx.ctor.stringConst(field), (Expr)cgrx.ctor.as(cgrx.ctor.myThis(), "org.flasck.android.areas.Area"), cgrx.ctor.stringConst(call)).flush();
	}

	public void interested(CGRContext cgrx, String var, String call) {
		if (!doBuild)
			return;
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		NewMethodDefiner meth = cgrx.ctor;
		meth.callVirtual("void", meth.getField("_src_"+var), "_interested", (Expr)meth.as(meth.myThis(), "org.flasck.android.areas.Area"), meth.stringConst(call)).flush();
	}

	public void addAssign(CGRContext cgrx, String call) {
		if (!doBuild)
			return;
		int idx = call.lastIndexOf(".prototype");
		call = call.substring(idx+11);
		cgrx.ctor.voidExpr(cgrx.ctor.callVirtual("java.lang.Object", cgrx.ctor.myThis(), call)).flush();
	}

	public void assignToVar(CGRContext cgrx, String varName) {
		if (!doBuild)
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
		meth.callVirtual("void", wrapper, "removeOnCrosetReplace", croset, (Expr)meth.as(meth.myThis(), "org.flasck.android.areas.Area"), curr).flush();
		meth.assign(curr, obj).flush();
		meth.callVirtual("void", wrapper, "onCrosetReplace", croset, (Expr)meth.as(meth.myThis(), "org.flasck.android.areas.Area"), curr).flush();
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("calling _fireInterests"))).flush();
		meth.callVirtual("void", meth.myThis(), "_fireInterests").flush();
		meth.returnObject(meth.aNull()).flush();
	}

	public void done(CGRContext cgrx) {
		if (!doBuild)
			return;
		cgrx.ctor.returnVoid().flush();
	}
}
