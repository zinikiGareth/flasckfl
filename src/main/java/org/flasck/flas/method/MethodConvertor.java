package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.EventHandlerInContext;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {
	private final ErrorResult errors;
	private final HSIE hsie;
	private final TypeChecker tc;
	private final Map<String, ContractDecl> contracts;

	public MethodConvertor(ErrorResult errors, HSIE hsie, TypeChecker tc, Map<String, ContractDecl> contracts) {
		this.errors = errors;
		this.hsie = hsie;
		this.tc = tc;
		this.contracts = contracts;
	}

	public void convert(Map<String, HSIEForm> functions, List<MethodInContext> methods) {
		for (MethodInContext m : methods) {
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			ContractDecl cd = contracts.get(m.fromContract);
			if (cd == null)
				throw new UtilException("Cannot find contract for method " + m.name + " called " + m.fromContract);
			ContractMethodDecl cmd = null;
			for (ContractMethodDecl md : cd.methods) {
				if (m.name.endsWith("." + md.name))
					cmd = md;
			}
			if (cmd == null)
				throw new UtilException("Cannot find method called " + m.name + " in " + m.fromContract);
			
			List<Type> types = new ArrayList<Type>();
			for (Object o : cmd.args) {
				if (o instanceof TypedPattern) {
					TypedPattern to = (TypedPattern)o;
					String tn = to.type;
					Type t = tc.getType(to.typeLocation, tn);
					if (t == null)
						throw new UtilException("Cannot find type " + tn); // TODO: should be a real error, I think
					types.add(t);
				} else
					throw new UtilException("Cannot handle " + o.getClass().getName());
			}
			for (MethodCaseDefn mcd : m.method.cases) {
				for (MethodMessage msg : mcd.messages) {
					List<Type> mytypes = new ArrayList<Type>();
					System.out.println("Convert method: " + msg.slot + " <- " + msg.expr);
					List<String> args = new ArrayList<String>();
					for (int i=0;i<mcd.intro.args.size();i++) {
						Object x = mcd.intro.args.get(i);
						if (x instanceof VarPattern) {
							mytypes.add(types.get(i));
							args.add(((VarPattern)x).var);
						} else if (x instanceof TypedPattern) {
							TypedPattern tx = (TypedPattern)x;
							args.add(tx.var);
							Type ty = tc.getType(tx.typeLocation, tx.type);
							// we have an obligation to check that ty is a sub-type of types.get(i);
							Type prev = types.get(i);
							if (prev.name().equals("Any"))
								;	// this has to be OK
							else
								throw new UtilException("Need to cover the case where the parent type is " + prev.name() + " and we want to restrict to " + tx.type);
							mytypes.add(ty);
						} else
							throw new UtilException("Cannot map " + x.getClass());
					}
					Type ty = tc.checkExpr(hsie.handleExprWith(msg.expr, HSIEForm.Type.CONTRACT, args), mytypes);
					if (ty != null)
						System.out.println("Type for method message - " + msg.slot + " <- " + msg.expr + " :: " + ty);
				}
				cases.add(new FunctionCaseDefn(null, mcd.intro.location, mcd.intro.name, mcd.intro.args, convert(mcd.intro.location, m.scope, mcd.messages)));
			}
			FunctionDefinition fd = new FunctionDefinition(m.method.intro.location, m.type, m.method.intro.name, m.method.intro.args.size(), cases);
			functions.put(m.method.intro.name, hsie.handle(fd));
		}
	}

	public void convertEvents(Map<String, HSIEForm> functions, List<EventHandlerInContext> eventHandlers) {
		for (EventHandlerInContext x : eventHandlers) {
			FunctionDefinition fd = MethodConvertor.convert(x.scope, x.name, x.handler);
			functions.put(x.name, hsie.handle(fd));
		}
	}

	public static FunctionDefinition convert(Scope scope, String card, EventHandlerDefinition eh) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (EventCaseDefn c : eh.cases) {
			cases.add(new FunctionCaseDefn(null, c.intro.location, c.intro.name, c.intro.args, convert(eh.intro.location, scope, c.messages)));
		}
		return new FunctionDefinition(eh.intro.location, HSIEForm.Type.EVENTHANDLER, eh.intro.name, eh.intro.args.size(), cases);
	}

	// TODO: this is more complicated than I make it appear here, but the proper thing requires typechecking
	private static Object convert(InputPosition location, Scope scope, List<MethodMessage> messages) {
		Object ret = scope.fromRoot(location, "Nil");
		for (int n = messages.size()-1;n>=0;n--) {
			MethodMessage mm = messages.get(n);
			Object me = convert(scope, mm);
			InputPosition loc = ((Locatable)mm.expr).location();
			ret = new ApplyExpr(loc, scope.fromRoot(loc, "Cons"), me, ret);
		}
		return ret;
	}

	// TODO: ALL OF THIS IS SPECIFICALLY MORE COMPLEX THAN THIS!!
	private static Object convert(Scope scope, MethodMessage mm) {
		if (mm.slot != null) {
			// we want an assign message
			LocatedToken slot = mm.slot.get(0);
			// TODO: somebody should check it really is a slot
			return new ApplyExpr(slot.location, scope.fromRoot(slot.location, "Assign"), ItemExpr.from(new ExprToken(slot.location, ExprToken.STRING, slot.text)), mm.expr);
		} else {
			ApplyExpr root = (ApplyExpr) mm.expr;
			ApplyExpr sender;
			List<Object> args;
			if (root.fn instanceof AbsoluteVar) {
				// a case where we're building a message
				AbsoluteVar av = (AbsoluteVar) root.fn;
				String name = av.id;
				if (name.equals("D3Action") || name.equals("CreateCard")) { // one of many (I think) OK cases
					return root; // I think this is fine just as it is, as long as it gets combined in a list
				} else if (name.equals("FLEval.field")) {
					sender = root;
					args = new ArrayList<Object>();
				} else {
//					System.out.println("unhandled case, with name = " + name + "; assuming it can be processed as an expression returning either one action or a list of actions");
					return root;
				}
			} else {
				ApplyExpr fn = (ApplyExpr)root.fn;
				if (!(fn.fn instanceof AbsoluteVar) || !((AbsoluteVar)fn.fn).id.equals("FLEval.field")) throw new UtilException("unhandled case");
				sender = fn;
				args = root.args;
			}
			Object target = sender.args.get(0);
			if (!(target instanceof CardMember)) throw new UtilException("Target must be on the card somewhere");
			return new ApplyExpr(root.location,
					scope.fromRoot(root.location, "Send"),
					new StringLiteral(((CardMember)target).location, ((CardMember)target).var),
					sender.args.get(1), asList(root.location, scope, args));
		}
	}

	private static Object asList(InputPosition loc, Scope scope, List<Object> args) {
		Object ret = scope.fromRoot(loc, "Nil");
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(loc, scope.fromRoot(loc, "Cons"), args.get(n), ret);
		}
		return ret;
	}
}
