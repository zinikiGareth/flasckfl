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
import org.flasck.flas.resolver.ApplicationRoutingResolver.ParameterRepositoryEntry;
import org.flasck.jvm.J;
import org.flasck.jvm.container.ArgType;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.Expr;
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
		kw.println("secure: " + r.requiresSecurity +",");
		kw.println("cards: [");
		boolean s2 = false;
		IndentWriter lw = kw.indent();
		for (CardBinding ca : r.assignments) {
			if (s2)
				lw.println(",");
			lw.print("{ ");
			lw.print("name: '" + ca.var.var + "', ");
			lw.print("card: " + ca.cardType.defn().name().jsName());
			lw.print(" }");
			s2 = true;
		}
		lw.println("");
		kw.println("],");
		kw.println("enter: [");
		handleActions(kw.indent(), r.enter);
		kw.println("],");
		kw.println("at: [");
		handleActions(kw.indent(), r.at);
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
			lw.print("card: '" + ra.card.var + "', ");
			lw.print("contract: '" + ra.contract.name() + "', ");
			lw.print("action: '" + ra.action + "', args: [");
			boolean first = true;
			for (org.flasck.flas.commonBase.Expr e : ra.exprs) {
				if (!first)
					lw.print(", ");
				first = false;
				if (e instanceof StringLiteral) {
					StringLiteral sl = (StringLiteral) e;
					lw.print("{ str: '" + sl.text + "' }");
				} else if (e instanceof UnresolvedVar) {
					// for now assume it's a card in the current card list thing
					UnresolvedVar uv = (UnresolvedVar) e;
					if (uv.defn() instanceof ParameterRepositoryEntry)
						lw.print("{ param: '" + uv.var + "' }");
					else
						lw.print("{ ref: '" + uv.var + "' }");
				}
			}
			lw.print("] }");
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
			if (r.path.startsWith("{"))
				mw.println("param: '" + r.path.substring(1, r.path.length()-1) + "', ");
			else
				mw.println("path: '" + r.path + "', ");
			
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
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("secure"), J.OBJECT), meth.as(meth.box(meth.boolConst(r.requiresSecurity)), J.OBJECT))).flush();
		Var cards = meth.avar(List.class.getName(), "cards_" + rn.incrementAndGet());
		meth.assign(cards, meth.makeNew(ArrayList.class.getName())).flush();
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("cards"), J.OBJECT), meth.as(cards, J.OBJECT))).flush();
		
		for (CardBinding ca : r.assignments) {
			IExpr mn = meth.makeNew(J.FLCARDASSIGNMENT, meth.stringConst(ca.var.var), meth.stringConst(ca.cardType.defn().name().javaName()));
			meth.voidExpr(meth.callInterface("boolean", cards, "add", meth.as(mn, J.OBJECT))).flush();
		}
		genActions(meth, v, "enter", r.enter, rn);
		genActions(meth, v, "at", r.at, rn);
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
			List<IExpr> exprs = new ArrayList<>();
			for (org.flasck.flas.commonBase.Expr e : ra.exprs) {
				String val;
				ArgType at;
				if (e instanceof StringLiteral) {
					val = ((StringLiteral) e).text;
					at = ArgType.STRING;
				} else if (e instanceof UnresolvedVar) {
					UnresolvedVar uv = (UnresolvedVar) e;
					val = uv.var;
					if (uv.defn() instanceof ParameterRepositoryEntry)
						at = ArgType.PARAM;
					else
						at = ArgType.CARDREF;
				} else
					throw new NotImplementedException();
				Expr ate = meth.staticField(ArgType.class.getName(), ArgType.class.getName(), at.name());
				exprs.add(meth.makeNew(J.FLROUTINGARG, ate, meth.stringConst(val)));
			}
			mn = meth.makeNew(J.FLROUTINGACTION, meth.stringConst(ra.card.var), meth.stringConst(ra.contract.name()), meth.stringConst(ra.action), meth.arrayOf(J.FLROUTINGARG, exprs));
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
			if (r.path.startsWith("{"))
				meth.voidExpr(meth.callInterface(J.OBJECT, inner, "put", meth.as(meth.stringConst("param"), J.OBJECT), meth.as(meth.stringConst(r.path.substring(1, r.path.length()-1)), J.OBJECT))).flush();
			else
				meth.voidExpr(meth.callInterface(J.OBJECT, inner, "put", meth.as(meth.stringConst("path"), J.OBJECT), meth.as(meth.stringConst(r.path), J.OBJECT))).flush();
			
			jvmcommon(meth, inner, r, rn);
		}
		meth.voidExpr(meth.callInterface(J.OBJECT, v, "put", meth.as(meth.stringConst("routes"), J.OBJECT), meth.as(list, J.OBJECT))).flush();
	}
}
