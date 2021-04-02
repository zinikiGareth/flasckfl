package org.flasck.flas.compiler.jsgen.packaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.parsedForm.assembly.RoutingAction;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

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
		common(kw, routes);
		jw.println("};");
		iw.println("};");
	}

	private void common(IndentWriter kw, SubRouting r) {
		kw.println("enter: [");
		handleActions(kw.indent(), r.enter);
		kw.println("],");
		kw.println("exit: [");
		handleActions(kw.indent(), r.exit);
		kw.println("],");
		kw.println("routes: [");
		handleRoutes(kw.indent(), r.routes);
		kw.println("]");
	}

	private void handleActions(IndentWriter lw, RoutingActions actions) {
		if (actions == null)
			return;
		boolean sep = false;
		for (RoutingAction ra : actions.actions) {
			if (sep)
				lw.println(",");
			sep = true;
			lw.print("{ ");
			lw.print("action: '" + ra.action + "', ");
			lw.print("card: '" + ra.card.var + "'");
			if (ra.expr != null) {
				if (ra.expr instanceof StringLiteral) {
					StringLiteral sl = (StringLiteral) ra.expr;
					// TODO: if string starts with {, it is a parameter and should be parameter: name without {} instead
					lw.print(", str: '" + sl.text + "'");
				} else if (ra.expr instanceof UnresolvedVar) {
					// for now assume it's a card in the current card list thing
					UnresolvedVar uv = (UnresolvedVar) ra.expr;
					lw.print(", ref: '" + uv.var + "'");
				}
			}
			lw.print(" }");
		}
		lw.println("");
	}

	private void handleRoutes(IndentWriter lw, List<SubRouting> routes) {
		if (routes.isEmpty())
			return;
		boolean sep = false;
		for (SubRouting r : routes) {
			if (sep)
				lw.println(",");
			sep = true;
			lw.println("{");
			IndentWriter mw = lw.indent();
			mw.println("path: '" + r.path + "', ");
			mw.println("cards: [");
			boolean s2 = false;
			IndentWriter nw = mw.indent();
			for (CardBinding ca : r.assignments) {
				if (s2)
					nw.println(",");
				nw.print("{ ");
				nw.print("name: '" + ca.var.var + "', ");
				nw.print("card: " + ca.cardType.defn().name().jsName());
				nw.print(" }");
			}
			nw.println("");
			mw.println("],");
			
			common(mw, r);
			
			lw.print("}");
		}
		lw.println("");
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

		jvmcommon(meth, v, routes);
		meth.returnObject(v).flush();
	}

	private void jvmcommon(NewMethodDefiner meth, Var v, SubRouting r) {
		genActions(meth, v, "enter", r.enter);
		genActions(meth, v, "exit", r.exit);
		genRoutes(meth, v, r.routes);
	}
	
	private void genActions(NewMethodDefiner meth, Var v, String label, RoutingActions actions) {
		if (actions == null)
			return;
		Var list = meth.avar(List.class.getName(), "actions");
		meth.assign(list, meth.makeNew(ArrayList.class.getName())).flush();
		for (RoutingAction ra : actions.actions) {
			if (ra.expr == null) {
				meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var)))).flush();
			} else if (ra.expr instanceof StringLiteral) {
				StringLiteral sl = (StringLiteral) ra.expr;
				meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var), meth.stringConst(sl.text)))).flush();
			} else if (ra.expr instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) ra.expr;
				meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var), meth.stringConst(uv.var)))).flush();
			} else
				throw new NotImplementedException();
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst(label), J.OBJECT), meth.as(list, J.OBJECT))).flush();
	}

	private void genRoutes(NewMethodDefiner meth, Var v, List<SubRouting> routes) {
		Var list = meth.avar(List.class.getName(), "routes");
		meth.assign(list, meth.makeNew(ArrayList.class.getName())).flush();
		for (SubRouting r : routes) {
			Var inner = meth.avar(Map.class.getName(), "inner");
			meth.assign(inner, meth.makeNew(TreeMap.class.getName())).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, list, "add", inner)).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("path"), J.OBJECT), meth.as(meth.stringConst(r.path), J.OBJECT))).flush();
			Var cards = meth.avar(List.class.getName(), "cards");
			meth.assign(cards, meth.makeNew(ArrayList.class.getName())).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("cards"), J.OBJECT), meth.as(cards, J.OBJECT))).flush();
			
			for (CardBinding ca : r.assignments) {
				meth.voidExpr(meth.callInterface(J.OBJECT, cards, "add", meth.makeNew(J.FLCARDASSIGNMENT, meth.stringConst(ca.var.var), meth.stringConst(ca.cardType.defn().name().javaName())))).flush();
//				if (s2)
//					nw.println(",");
//				nw.print("{ ");
//				nw.print("name: '" + ca.var.var + "', ");
//				nw.print("card: " + ca.cardType.defn().name().jsName());
//				nw.print(" }");
			}
			
			jvmcommon(meth, inner, r);
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("routes"), J.OBJECT), meth.as(list, J.OBJECT))).flush();
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
