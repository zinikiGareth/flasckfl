package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {

	public static FunctionDefinition convert(String card, String type, MethodDefinition m) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (MethodCaseDefn mcd : m.cases) {
			cases.add(new FunctionCaseDefn(null, card +"." +type+"."+mcd.intro.name, mcd.intro.args, convert(mcd.messages)));
		}
		// This feels very much hackishly the wrong place to put ".prototype."
		// Should we have a MethodDefinition as well which we can generate differently?
		return new FunctionDefinition(card +"." +type+".prototype."+m.intro.name, m.intro.args.size(), cases);
	}

	public static FunctionDefinition convert(String card, EventHandlerDefinition eh) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (EventCaseDefn c : eh.cases) {
			cases.add(new FunctionCaseDefn(null, c.intro.name, c.intro.args, convert(c.messages)));
		}
		return new FunctionDefinition(eh.intro.name, eh.intro.args.size(), cases);
	}

	// TODO: this is more complicated than I make it appear here, but the proper thing requires typechecking
	private static Object convert(List<MethodMessage> messages) {
		Object ret = new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Nil"));
		for (int n = messages.size()-1;n>=0;n--) {
			MethodMessage mm = messages.get(n);
			Object me = convert(mm);
			ret = new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Cons")), me, ret);
		}
		return ret;
	}

	// TODO: ALL OF THIS IS SPECIFICALLY MORE COMPLEX THAN THIS!!
	private static Object convert(MethodMessage mm) {
		if (mm.slot != null) {
			// we want an assign message
			String slot = mm.slot.get(0);
			if (!slot.startsWith("_card.") && !slot.startsWith("this."))
				throw new UtilException("slots must be in the card state");
			slot = slot.substring(6);
			return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Assign")), new ItemExpr(new ExprToken(ExprToken.STRING, slot)), mm.expr);
		} else {
			// we want some kind of invoke message
			ApplyExpr root = (ApplyExpr) mm.expr;
			ApplyExpr fn = (ApplyExpr)root.fn;
			if (!((ItemExpr)fn.fn).tok.text.equals("FLEval.field")) throw new UtilException("unhandled case");
			return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Send")),
					fn.args.get(0),
					fn.args.get(1),
					asList(root.args));
		}
	}

	private static Object asList(List<Object> args) {
		Object ret = new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Nil"));
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Cons")), args.get(n), ret);
		}
		return ret;
	}

	@Deprecated
	public static FunctionDefinition lift(CardDefinition card, FunctionDefinition fn) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (FunctionCaseDefn fcd : fn.cases) {
			List<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("_card"));
			args.addAll(fcd.intro.args);
			cases.add(new FunctionCaseDefn(fcd.innerScope().outer, fcd.intro.name, args, lift(card, fcd.expr)));
		}
		return new FunctionDefinition(fn.name, fn.nargs+1, cases);
	}

	private static Object lift(CardDefinition card, Object expr) {
//		System.out.println("trying to lift " + expr);
		if (expr instanceof ItemExpr) {
			ExprToken tok = ((ItemExpr) expr).tok;
			if (tok.type != ExprToken.IDENTIFIER)
				return expr;
			else if (card.state != null && card.state.hasMember(tok.text)) {
				return new ApplyExpr(".", ItemExpr.id("_card"), ItemExpr.str(tok.text));
			} else if (card.fnScope.contains(tok.text)) {
				// when a function is called normally, that should be handled above
				// this is the case where it is an argument
				return new ApplyExpr(expr, ItemExpr.id("_card"));
			} else
				return expr;
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			List<Object> args = new ArrayList<Object>();
			for (Object o : ae.args)
				args.add(lift(card, o));
			if (ae.fn instanceof ItemExpr && card.fnScope.contains(((ItemExpr)ae.fn).tok.text)) {
				// insert the extra argument as the first parameter
				args.add(0, ItemExpr.id("_card"));
				return new ApplyExpr(ae.fn, args);
			}
			return new ApplyExpr(lift(card, ae.fn), args);
		} else
			throw new UtilException("Cannot lift " + expr);
	}

}
