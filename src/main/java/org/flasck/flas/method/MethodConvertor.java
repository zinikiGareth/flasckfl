package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
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
			if (!slot.startsWith("_card."))
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

	public static FunctionDefinition lift(CardDefinition card, FunctionDefinition fn) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (FunctionCaseDefn fcd : fn.cases) {
			List<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("_card"));
			args.addAll(fcd.intro.args);
			cases.add(new FunctionCaseDefn(fcd.innerScope().outer, fcd.intro.name, args, fcd.expr));
		}
		return new FunctionDefinition(fn.name, fn.nargs+1, cases);
	}

}
