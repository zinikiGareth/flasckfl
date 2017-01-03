package org.flasck.flas.droidgen;

import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.template.TemplateGenerator;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.FieldObject;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;

public class DroidTemplateGenerator implements TemplateGenerator {
	private final boolean doBuild;
	private ByteCodeStorage bce;

	public DroidTemplateGenerator(boolean doBuild, ByteCodeStorage bce) {
		this.doBuild = doBuild;
		this.bce = bce;
	}

	@Override
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

	@Override
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
			return new CGRContext(this, bcc, ctor, cardArg.getVar(), parent.getVar());
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
