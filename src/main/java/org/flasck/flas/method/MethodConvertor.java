package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {

	public static void convert(Map<String, FunctionDefinition> functions, Scope scope) {
		for (Entry<String, ScopeEntry> x : scope) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition) {
				CardDefinition card = (CardDefinition) val;
				
				int pos = 0;
				for (ContractImplements ci : card.contracts) {
					for (MethodDefinition m : ci.methods) {
						FunctionDefinition fd = convert(card.innerScope(), name, "_C"+pos, HSIEForm.Type.CONTRACT, m);
						functions.put(fd.name, fd);
					}
					pos++;
				}
				
				pos = 0;
				for (HandlerImplements hi : card.handlers) {
					for (MethodDefinition m : hi.methods) {
						FunctionDefinition fd = convert(card.innerScope(), name, "_H"+pos, HSIEForm.Type.HANDLER, m);
						functions.put(fd.name, fd);
					}
					pos++;
				}

				for (Entry<String, ScopeEntry> x2 : card.innerScope()) {
					if (x2.getValue().getValue() instanceof EventHandlerDefinition) {
						FunctionDefinition fd = convert(card.innerScope(), name, (EventHandlerDefinition)x2.getValue().getValue());
						functions.put(fd.name, fd);
					}
				}
			}
		}
	}

	public static FunctionDefinition convert(Scope scope, String card, String type, Type ft, MethodDefinition m) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (MethodCaseDefn mcd : m.cases) {
			cases.add(new FunctionCaseDefn(null, mcd.intro.name, mcd.intro.args, convert(scope, mcd.messages)));
		}
		return new FunctionDefinition(ft, m.intro.name, m.intro.args.size(), cases);
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
			String slot = mm.slot.get(0);
			// TODO: somebody should check it really is a slot
			return new ApplyExpr(scope.fromRoot("Assign"), ItemExpr.from(new ExprToken(ExprToken.STRING, slot)), mm.expr);
		} else {
			// we want some kind of invoke message
			ApplyExpr root = (ApplyExpr) mm.expr;
			ApplyExpr fn = (ApplyExpr)root.fn;
			if (!(fn.fn instanceof AbsoluteVar) || ((AbsoluteVar)fn.fn).id.equals(".")) throw new UtilException("unhandled case");
			return new ApplyExpr(scope.fromRoot("Send"),
					fn.args.get(0),
					fn.args.get(1),
					asList(scope, root.args));
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
