package org.flasck.flas.droidgen;

import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.CaseChooser;
import org.flasck.flas.template.EventHandlerGenerator;
import org.flasck.jvm.J;
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
import org.zinutils.exceptions.NotImplementedException;
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
		IFieldInfo src = bcc.defineField(true, Access.PUBLIC, definedInType.javaClassName(), "_src_"+s);
		ctor.assign(src.asExpr(ctor), ctor.getField(ctor.castTo(parent, parentClass.javaClassName()), "_src_"+s)).flush();
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
		IExpr curr = meth.getField(varName);
		IExpr wrapper = meth.getField("_wrapper");
		IExpr parent = meth.castTo(meth.getField("_parent"), J.LIST_AREA);
		FieldExpr croset = new FieldObject(false, J.LIST_AREA, new JavaType(J.CROSET), "_current").useOn(meth, parent);
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("In _assignToVar"))).flush();
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.callStatic("java.lang.String",  "java.lang.String", "valueOf", curr))).flush();
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.callStatic("java.lang.String",  "java.lang.String", "valueOf", obj))).flush();
		meth.ifOp(0xa6, curr, obj, meth.returnObject(meth.aNull()), null).flush();
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("survived first test"))).flush();
		meth.callVirtual("void", wrapper, "removeOnCrosetReplace", croset, (Expr)meth.as(meth.myThis(), J.AREA), curr).flush();
		meth.assign(curr, obj).flush();
		meth.callVirtual("void", wrapper, "onCrosetReplace", croset, (Expr)meth.as(meth.myThis(), J.AREA), curr).flush();
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("calling _fireInterests"))).flush();
		meth.callVirtual("void", meth.myThis(), "_fireInterests").flush();
		meth.returnObject(meth.aNull()).flush();
	}

	@Override
	public void assignToList(FunctionName listFn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAssign(FunctionName call, String passVar) {
		if (passVar != null)
			throw new NotImplementedException("Passed var: " + passVar);
		ctor.voidExpr(ctor.callVirtual("java.lang.Object", ctor.myThis(), call.name)).flush();
	}

	@Override
	public void interested(String var, FunctionName call) {
		ctor.callVirtual("void", ctor.getField("_src_"+var), "_interested", (Expr)ctor.as(ctor.myThis(), J.AREA), ctor.stringConst(call.name)).flush();
	}

	@Override
	public void onFieldAssign(Object expr, String field, FunctionName call) {
		IExpr dge = null;
		if (expr instanceof TemplateListVar) {
			String name = ((TemplateListVar)expr).simpleName;
			dge = ctor.getField(ctor.getField(ctor.myThis(), "_src_" + name), name);
		} else if (expr instanceof CardMember) {
			dge = ctor.getField(ctor.getField(ctor.myThis(), "_card"), ((CardMember)expr).var);
		} else
			throw new NotImplementedException();

		ctor.callVirtual("void", ctor.getField(ctor.getField("_card"), "_wrapper"), "onAssign", ctor.as(dge, J.OBJECT), ctor.stringConst(field), ctor.as(ctor.myThis(), J.IAREA), ctor.stringConst(call.name)).flush();
	}

	@Override
	public void onAssign(CardMember valExpr, FunctionName call) {
		ctor.callVirtual("void", ctor.getField(ctor.getField("_card"), "_wrapper"), "onAssign", ctor.as(ctor.getField("_card"), J.OBJECT), ctor.stringConst(valExpr.var), ctor.as(ctor.myThis(), J.IAREA), ctor.stringConst(call.name)).flush();
	}

	@Override
	public void newListChild(AreaName childArea) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_newChild");
		PendingVar ck = gen.argument(J.CROKEY, "crokey");
		gen.returns(J.AREA);
		NewMethodDefiner meth = gen.done();
		Var ret = meth.avar(J.AREA, "ret");
		meth.assign(ret, (Expr) meth.makeNew(childArea.javaClassName(), meth.getField("_card"), (Expr)meth.as(meth.myThis(), J.AREA))).flush();
		FieldExpr crokeyid = new FieldObject(false, J.CROKEY, new JavaType(J.OBJECT), "id").useOn(meth, ck.getVar());
		meth.callVirtual("void", ret, "bindVar", meth.stringConst("_crokey"), crokeyid).flush();
		meth.returnObject(ret).flush();
	}

	@Override
	public void contentExpr(FunctionName tfn, boolean rawHTML) {
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_contentExpr");
		gen.returns("java.lang.Object");
		NewMethodDefiner meth = gen.done();
		
//		The rest of this code is basically correct, it's just that we used to have an HSIE block here
		// that we converted into a Var.  Now we have a function to call, so we need to replace "str" with "tfn()"
		IExpr str = meth.callVirtual(JavaType.string.getActual(), meth.myThis(), tfn.name);
		if (rawHTML)
			meth.callSuper("void", J.TEXT_AREA, "_insertHTML", str).flush();
		else
			meth.callSuper("void", J.TEXT_AREA, "_assignToText", str).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	@Override
	public EventHandlerGenerator needAddHandlers() {
		GenericAnnotator ah = GenericAnnotator.newMethod(bcc, false, "_add_handlers");
		ah.returns("java.lang.Object");
		MethodDefiner ahMeth = ah.done();
		currentMethod = ahMeth;
//		ahMeth.voidExpr(ahMeth.callStatic("android.util.Log", "int", "e", ahMeth.stringConst("FlasckLib"), ahMeth.stringConst("Need to add the handlers"))).flush();
		return new DroidEventHandlerGenerator(ahMeth);
	}

	@Override
	public void createNested(String v, AreaName nested) {
		Var storeAs = ctor.avar(nested.javaClassName(), v);
		ctor.assign(storeAs, (Expr) ctor.makeNew(nested.javaClassName(), card, (Expr)ctor.as(ctor.myThis(), J.AREA))).flush();
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
//		meth.callSuper("void", J.TEXT_AREA, "_assignToText", str).flush();
//		JSForm.assign(cexpr, "var card", form);
//		cexpr.add(JSForm.flex("this._updateToCard(card)"));

//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to implement yoyo card"))).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	@Override
	public void setText(String text) {
		ctor.callVirtual("void", ctor.myThis(), "_setText", ctor.stringConst(text)).flush();
	}

	@Override
	public void setVarFormats(FunctionName tfn) {
		GenericAnnotator svf = GenericAnnotator.newMethod(bcc, false, "_setVariableFormats");
		svf.returns("java.lang.Object");
		MethodDefiner meth = svf.done();
		currentMethod = meth;
//		meth.voidExpr(meth.callStatic("android.util.Log", "int", "e", meth.stringConst("FlasckLib"), meth.stringConst("Need to set variable formats"))).flush();
		meth.callSuper("void", J.AREA, "_setCSSObj", meth.callVirtual(J.OBJECT, meth.myThis(), tfn.name)).flush();
		meth.returnObject(meth.aNull()).flush();
	}

	@Override
	public void setSimpleClass(String css) {
		ctor.callVirtual("void", ctor.myThis(), "setCSS", ctor.stringConst(css)).flush();
	}

	@Override
	public void handleTEA(RWTemplateExplicitAttr tea, int an) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void makeEditable(RWContentExpr ce, String field) {
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
	public CaseChooser chooseCase(FunctionName sn) {
		return new DroidCaseChooser(sn);
	}
}
