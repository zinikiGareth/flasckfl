package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {

	public static FunctionDefinition convert(String type, MethodDefinition m) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (MethodCaseDefn mcd : m.cases) {
			cases.add(new FunctionCaseDefn(mcd.intro.name, mcd.intro.args, convert(mcd.messages)));
		}
		return new FunctionDefinition(m.intro, cases);
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
			return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Assign")), new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, mm.slot.get(0))), mm.expr);
		} else {
			// we want some kind of invoke message
			ApplyExpr root = (ApplyExpr) mm.expr;
			ApplyExpr fn = (ApplyExpr)root.fn;
			if (!((ItemExpr)fn.fn).tok.text.equals(".")) throw new UtilException("unhandled case");
			return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Invoke")), fn.args.get(0), fn.args.get(1), asList(root.args));
		}
	}

	private static Object asList(List<Object> args) {
		Object ret = new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Nil"));
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Cons")), args.get(n), ret);
		}
		return ret;
	}

}
