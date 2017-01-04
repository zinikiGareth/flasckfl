package org.flasck.flas.droidgen;

import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.CaseChooser;
import org.zinutils.bytecode.ByteCodeSink;
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

public class DroidAreaGenerator implements AreaGenerator {
	final ByteCodeSink bcc;
	public final NewMethodDefiner ctor;
	final Var card;
	final Var parent;
	MethodDefiner currentMethod;

	public DroidAreaGenerator(ByteCodeSink bcc, NewMethodDefiner ctor, Var card, Var parent) {
		this.bcc = bcc;
		this.ctor = ctor;
		this.card = card;
		this.parent = parent;
	}

	@Override
	public void done() {
		ctor.returnVoid().flush();
	}

	@Override
	public void copyVar(AreaName parentClass, AreaName definedInType, String s) {
		IFieldInfo src = bcc.defineField(true, Access.PUBLIC, DroidUtils.javaNestedName(definedInType.javaName()), "_src_"+s);
		ctor.assign(src.asExpr(ctor), ctor.getField(ctor.castTo(parent, DroidUtils.javaNestedName(parentClass.javaName())), "_src_"+s)).flush();
	}

	@Override
	public void assignToVar(String varName) {
		IFieldInfo src = bcc.defineField(true, Access.PUBLIC, bcc.getCreatedName(), "_src_"+varName);
		bcc.defineField(false, Access.PUBLIC, "java.lang.Object", varName);
		ctor.assign(src.asExpr(ctor), ctor.myThis()).flush();
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_assignToVar");
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

	@Override
	public void assignToList(String listFn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAssign(String call) {
		int idx = call.lastIndexOf(".prototype");
		call = call.substring(idx+11);
		ctor.voidExpr(ctor.callVirtual("java.lang.Object", ctor.myThis(), call)).flush();
	}

	@Override
	public void interested(String var, String call) {
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		ctor.callVirtual("void", ctor.getField("_src_"+var), "_interested", (Expr)ctor.as(ctor.myThis(), "org.flasck.android.areas.Area"), ctor.stringConst(call)).flush();
	}

	@Override
	public void onAssign(Expr expr, String field, String call) {
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		ctor.callVirtual("void", ctor.getField(ctor.getField("_card"), "_wrapper"), "onAssign", (Expr)ctor.as(expr, "java.lang.Object"), ctor.stringConst(field), (Expr)ctor.as(ctor.myThis(), "org.flasck.android.areas.Area"), ctor.stringConst(call)).flush();
	}

	@Override
	public void onAssign(CardMember valExpr, String call) {
		// I think this is removing the "prototype" ... at some point, rationalize all this
		// so that we pass around a struct with "package" "class", "area", "method" or whatever we need ...
		int idx = call.lastIndexOf(".");
		if (idx != -1)
			call = call.substring(idx+1);
		ctor.callVirtual("void", ctor.getField(ctor.getField("_card"), "_wrapper"), "onAssign", (Expr)ctor.as(ctor.getField("_card"), "java.lang.Object"), ctor.stringConst(valExpr.var), (Expr)ctor.as(ctor.myThis(), "org.flasck.android.areas.Area"), ctor.stringConst(call)).flush();
	}

	@Override
	public void newListChild(AreaName childArea) {
		String child = childArea.javaName();
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_newChild");
		PendingVar ck = gen.argument("org.flasck.android.builtin.Crokey", "crokey");
		gen.returns("org.flasck.android.areas.Area");
		NewMethodDefiner meth = gen.done();
		Var ret = meth.avar("org.flasck.android.areas.Area", "ret");
		meth.assign(ret, (Expr) meth.makeNew(DroidUtils.javaNestedName(child), meth.getField("_card"), (Expr)meth.as(meth.myThis(), "org.flasck.android.areas.Area"))).flush();
		FieldExpr crokeyid = new FieldObject(false, "org.flasck.android.builtin.Crokey", new JavaType("java.lang.Object"), "id").useOn(meth, ck.getVar());
		meth.callVirtual("void", ret, "bindVar", meth.stringConst("_crokey"), crokeyid).flush();
		meth.returnObject(ret).flush();
	}

	@Override
	public void contentExpr(String tfn, boolean rawHTML) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_contentExpr");
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

	@Override
	public void needAddHandlers() {
		GenericAnnotator ah = GenericAnnotator.newMethod(bcc, false, "_add_handlers");
		ah.returns("java.lang.Object");
		MethodDefiner ahMeth = ah.done();
		currentMethod = ahMeth;
		ahMeth.voidExpr(ahMeth.callStatic("android.util.Log", "int", "e", ahMeth.stringConst("FlasckLib"), ahMeth.stringConst("Need to add the handlers"))).flush();
		ahMeth.returnObject(ahMeth.aNull()).flush();
	}

	@Override
	public void createNested(String v, AreaName nested) {
		String cn = nested.javaName();
		Var storeAs = ctor.avar(cn, v);
		ctor.assign(storeAs, (Expr) ctor.makeNew(DroidUtils.javaNestedName(cn), card, (Expr)ctor.as(ctor.myThis(), "org.flasck.android.areas.Area"))).flush();
	}

	@Override
	public void yoyoExpr(String tfn) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_yoyoExpr");
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

	@Override
	public void setText(String text) {
		ctor.callVirtual("void", ctor.myThis(), "_setText", ctor.stringConst(text)).flush();
	}

	@Override
	public void setVarFormats(String tfn) {
		GenericAnnotator svf = GenericAnnotator.newMethod(bcc, false, "_setVariableFormats");
		svf.returns("java.lang.Object");
		MethodDefiner meth = svf.done();
		currentMethod = meth;
		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to set variable formats"))).flush();
		meth.callSuper("void", "org.flasck.android.Area", "_setCSSObj", meth.callVirtual("java.lang.String", meth.myThis(), tfn)).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	@Override
	public void setSimpleClass(String css) {
		ctor.callVirtual("void", ctor.myThis(), "setCSS", ctor.stringConst(css)).flush();
	}

	@Override
	public Expr sourceFor(String name) {
		return ctor.getField(ctor.getField(ctor.myThis(), "_src_" + name), name);
	}

	@Override
	public Expr cardField(CardMember expr) {
		return ctor.getField(ctor.getField(ctor.myThis(), "_card"), ((CardMember)expr).var);
	}

	@Override
	public void handleTEA(RWTemplateExplicitAttr tea, int an) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void makeEditable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void supportDragging() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void makeItemDraggable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dropZone(List<String> droppables) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CaseChooser chooseCase(String sn) {
		return new DroidCaseChooser(sn);
	}
}
