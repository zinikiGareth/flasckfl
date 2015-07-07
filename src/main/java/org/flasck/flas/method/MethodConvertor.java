package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.EventHandlerInContext;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {

	public static void convert(Map<String, FunctionDefinition> functions, List<MethodInContext> methods) {
		for (MethodInContext m : methods) {
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			for (MethodCaseDefn mcd : m.method.cases) {
				cases.add(new FunctionCaseDefn(null, mcd.intro.name, mcd.intro.args, convert(m.scope, mcd.messages)));
			}
			functions.put(m.method.intro.name, new FunctionDefinition(m.type, m.method.intro.name, m.method.intro.args.size(), cases));
		}
	}

	public static void convertEvents(Map<String, FunctionDefinition> functions, List<EventHandlerInContext> eventHandlers) {
		for (EventHandlerInContext x : eventHandlers) {
			FunctionDefinition fd = MethodConvertor.convert(x.scope, x.name, x.handler);
			functions.put(x.name, fd);
		}
	}

	public static FunctionDefinition convert(Scope scope, String card, EventHandlerDefinition eh) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (EventCaseDefn c : eh.cases) {
			cases.add(new FunctionCaseDefn(null, c.intro.name, c.intro.args, convert(scope, c.messages)));
		}
		return new FunctionDefinition(Type.EVENTHANDLER, eh.intro.name, eh.intro.args.size(), cases);
	}

	// TODO: this is more complicated than I make it appear here, but the proper thing requires typechecking
	private static Object convert(Scope scope, List<MethodMessage> messages) {
		Object ret = scope.fromRoot("Nil");
		for (int n = messages.size()-1;n>=0;n--) {
			MethodMessage mm = messages.get(n);
			Object me = convert(scope, mm);
			ret = new ApplyExpr(scope.fromRoot("Cons"), me, ret);
		}
		return ret;
	}

	// TODO: ALL OF THIS IS SPECIFICALLY MORE COMPLEX THAN THIS!!
	private static Object convert(Scope scope, MethodMessage mm) {
		if (mm.slot != null) {
			// we want an assign message
			LocatedToken slot = mm.slot.get(0);
			// TODO: somebody should check it really is a slot
			return new ApplyExpr(scope.fromRoot("Assign"), ItemExpr.from(new ExprToken(ExprToken.STRING, slot.text)), mm.expr);
		} else {
			ApplyExpr root = (ApplyExpr) mm.expr;
			if (root.fn instanceof AbsoluteVar) {
				// a case where we're building a message
				AbsoluteVar av = (AbsoluteVar) root.fn;
				String name = av.id;
				if (name.equals("D3Action")) { // one of many (I think) OK cases
					return root; // I think this is fine just as it is, as long as it gets combined in a list
				} else
					throw new UtilException("unhandled case");
			} else {
				ApplyExpr fn = (ApplyExpr)root.fn;
				if (!(fn.fn instanceof AbsoluteVar) || !((AbsoluteVar)fn.fn).id.equals("FLEval.field")) throw new UtilException("unhandled case");
				Object target = fn.args.get(0);
				if (!(target instanceof CardMember)) throw new UtilException("Target must be on the card somewhere");
				return new ApplyExpr(scope.fromRoot("Send"),
						new StringLiteral(((CardMember)target).var),
						fn.args.get(1),
						asList(scope, root.args));
			}
		}
	}

	private static Object asList(Scope scope, List<Object> args) {
		Object ret = scope.fromRoot("Nil");
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(scope.fromRoot("Cons"), args.get(n), ret);
		}
		return ret;
	}
}
