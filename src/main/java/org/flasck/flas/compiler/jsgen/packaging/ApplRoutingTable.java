package org.flasck.flas.compiler.jsgen.packaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.zinutils.bytecode.IExpr;
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
		if (r.hasTitle()) {
			kw.println("title: '" + r.getTitle() + "',");
		}
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

		AtomicInteger rn = new AtomicInteger(0);
		jvmcommon(meth, v, routes, rn);
		meth.returnObject(v).flush();
	}

	private void jvmcommon(NewMethodDefiner meth, Var v, SubRouting r, AtomicInteger rn) {
		if (r.hasTitle()) {
			meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("title"), J.OBJECT), meth.as(meth.stringConst(r.getTitle()), J.OBJECT))).flush();
		}
		genActions(meth, v, "enter", r.enter, rn);
		genActions(meth, v, "exit", r.exit, rn);
		genRoutes(meth, v, r.routes, rn);
	}
	
	private void genActions(NewMethodDefiner meth, Var v, String label, RoutingActions actions, AtomicInteger rn) {
		if (actions == null)
			return;
		Var list = meth.avar(List.class.getName(), "actions_" + rn.incrementAndGet());
		meth.assign(list, meth.makeNew(ArrayList.class.getName())).flush();
		for (RoutingAction ra : actions.actions) {
			IExpr mn;
			if (ra.expr == null) {
				mn = meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var));
			} else if (ra.expr instanceof StringLiteral) {
				StringLiteral sl = (StringLiteral) ra.expr;
				mn = meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var), meth.as(meth.stringConst(sl.text), J.OBJECT));
			} else if (ra.expr instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) ra.expr;
				mn = meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.action), meth.stringConst(ra.card.var), meth.as(meth.stringConst(uv.var), J.OBJECT));
			} else
				throw new NotImplementedException();
			meth.voidExpr(meth.callInterface("boolean", list, "add", meth.as(mn, J.OBJECT))).flush();
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst(label), J.OBJECT), meth.as(list, J.OBJECT))).flush();
	}

	private void genRoutes(NewMethodDefiner meth, Var v, List<SubRouting> routes, AtomicInteger rn) {
		Var list = meth.avar(List.class.getName(), "routes_" + rn.incrementAndGet());
		meth.assign(list, meth.makeNew(ArrayList.class.getName())).flush();
		for (SubRouting r : routes) {
			Var inner = meth.avar(Map.class.getName(), "inner_" + rn.incrementAndGet());
			meth.assign(inner, meth.makeNew(TreeMap.class.getName())).flush();
			meth.voidExpr(meth.callInterface("boolean", list, "add", meth.as(inner, J.OBJECT))).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, inner, "put", meth.as(meth.stringConst("path"), J.OBJECT), meth.as(meth.stringConst(r.path), J.OBJECT))).flush();
			Var cards = meth.avar(List.class.getName(), "cards_" + rn.incrementAndGet());
			meth.assign(cards, meth.makeNew(ArrayList.class.getName())).flush();
			meth.voidExpr(meth.callInterface(J.OBJECT, inner, "put", meth.as(meth.stringConst("cards"), J.OBJECT), meth.as(cards, J.OBJECT))).flush();
			
			for (CardBinding ca : r.assignments) {
				IExpr mn = meth.makeNew(J.FLCARDASSIGNMENT, meth.stringConst(ca.var.var), meth.stringConst(ca.cardType.defn().name().javaName()));
				meth.voidExpr(meth.callInterface("boolean", cards, "add", meth.as(mn, J.OBJECT))).flush();
			}
			
			jvmcommon(meth, inner, r, rn);
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("routes"), J.OBJECT), meth.as(list, J.OBJECT))).flush();
	}
}
