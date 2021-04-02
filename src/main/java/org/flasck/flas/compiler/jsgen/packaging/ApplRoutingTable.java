package org.flasck.flas.compiler.jsgen.packaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.RoutingAction;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class ApplRoutingTable {
	private final NameOfThing applName;
	private final ApplicationRouting routes;

	public ApplRoutingTable(NameOfThing applName, ApplicationRouting routes) {
		this.applName = applName;
		this.routes = routes;
	}

	public void write(IndentWriter iw) {
		iw.println(applName.jsName() + "._Application.prototype._routing = function() {");
		IndentWriter jw = iw.indent();
		jw.println("return {");
		IndentWriter kw = jw.indent();
		kw.println("enter: [");
		handleActions(kw.indent(), routes.enter);
		kw.println("],");
		kw.println("exit: [");
		handleActions(kw.indent(), routes.enter);
		kw.println("],");
		kw.println("routes: [");
		kw.println("]");
		jw.println("};");
		iw.println("};");
	}

	private void handleActions(IndentWriter lw, RoutingActions actions) {
		if (actions == null)
			return;
		String sep = "";
		for (RoutingAction ra : actions.actions) {
			lw.print("{ ");
			lw.print("action: '" + ra.action + "', ");
			lw.print("card: '" + ra.card.var +"', ");
			if (ra.expr != null) {
				// I think, in principle, we should be able to support expressions, not just strings
				// but this is all hard enough without dealing with that right now, and they have turingness inside load
				StringLiteral sl = (StringLiteral) ra.expr;
				// TODO: if string starts with {, it is a parameter and should be parameter: name without {} instead
				lw.print("value: '" + sl.text + "'");
			}
			lw.println(" }" + sep);
			sep = ",";
		}
	}

	public void generate(ByteCodeEnvironment bce) {
		if (bce == null)
			return;
		
		ByteCodeSink bcc = bce.getOrCreate(applName.javaName() + "._Application");
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, false, "_routing");
		gen.returns(Map.class.getName());
		NewMethodDefiner meth = gen.done();
		meth.lenientMode(false);
		Var v = meth.avar(Map.class.getName(), "ret");
		meth.assign(v, meth.makeNew(TreeMap.class.getName())).flush();

		genActions(meth, v, "enter", routes.enter);
		genActions(meth, v, "exit", routes.exit);
		meth.returnObject(v).flush();
	}
	
	private void genActions(NewMethodDefiner meth, Var v, String label, RoutingActions actions) {
		if (actions == null)
			return;
		Var list = meth.avar(List.class.getName(), "actions");
		meth.assign(list, meth.makeNew(ArrayList.class.getName())).flush();
		for (RoutingAction ra : actions.actions) {
			StringLiteral sl = (StringLiteral) ra.expr;
			if (sl != null) { // also consider parameters
				meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var), meth.stringConst(sl.text)))).flush();
			} else {
				meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var)))).flush();
			}
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst(label), J.OBJECT), meth.as(list, J.OBJECT))).flush();
	}

		/*
		for (String t : methods.templateNames()) {
			Var hl = meth.avar(List.class.getName(), "hl");
			meth.assign(hl, meth.makeNew(ArrayList.class.getName())).flush();
			for (TemplateTarget tt : methods.targets(t)) {
				HandlerInfo hi = methods.getHandler(tt.handler);
				IExpr classArgs = meth.arrayOf(Class.class.getName(), meth.classConst(J.FLEVALCONTEXT),
						meth.classConst("[L" + J.OBJECT + ";"));
				IExpr ehm = meth.callVirtual(Method.class.getName(),
						meth.classConst(cardName.javaName()), "getDeclaredMethod",
						meth.stringConst(hi.name.name), classArgs);

				IExpr ety, esl;
				if (tt.type != null) {
					ety = meth.stringConst(tt.type);
					esl = meth.stringConst(tt.slot);
				} else {
					ety = meth.as(meth.aNull(), J.STRING);
					esl = meth.as(meth.aNull(), J.STRING);
				}
				IExpr icond;
				if (tt.evcond != null) {
					icond = meth.box(meth.intConst(tt.evcond));
				} else
					icond = meth.as(meth.aNull(), J.INTEGER);
				IExpr ghi = meth.makeNew(J.HANDLERINFO, ety, esl, meth.box(meth.intConst(tt.option)), meth.stringConst(hi.event), ehm, icond);
				meth.voidExpr(meth.callInterface("boolean", hl, "add", meth.as(ghi, J.OBJECT))).flush();
			}
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst(t), J.OBJECT), meth.as(hl, J.OBJECT))).flush();
		}

		if (!methods.unboundHandlers().isEmpty()) {
			Var hl = meth.avar(List.class.getName(), "hl");
			meth.assign(hl, meth.makeNew(ArrayList.class.getName())).flush();
			for (HandlerInfo hi : methods.unboundHandlers()) {
				IExpr classArgs = meth.arrayOf(Class.class.getName(), meth.classConst(J.FLEVALCONTEXT),
						meth.classConst("[L" + J.OBJECT + ";"));
				IExpr ehm = meth.callVirtual(Method.class.getName(), meth.classConst(cardName.javaName()),
						"getDeclaredMethod", meth.stringConst(hi.name.name), classArgs);
				IExpr ghi = meth.makeNew(J.HANDLERINFO, meth.as(meth.aNull(), J.STRING),
						meth.as(meth.aNull(), J.STRING), meth.as(meth.aNull(), J.INTEGER),
						meth.stringConst(hi.event), ehm, meth.as(meth.aNull(), J.INTEGER));
				meth.voidExpr(meth.callInterface("boolean", hl, "add", meth.as(ghi, J.OBJECT))).flush();
			}
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("_"), J.OBJECT), meth.as(hl, J.OBJECT))).flush();
		}

		*/
}
