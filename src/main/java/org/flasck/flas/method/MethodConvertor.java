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
import org.flasck.flas.parsedForm.CardStateRef;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.EventHandlerInContext;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.typechecker.Type.WhatAmI;
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

	// 1. Main entry points to convert different kinds of things
	public void convertContractMethods(Map<String, HSIEForm> functions, List<MethodInContext> methods) {
		for (MethodInContext m : methods)
			addFunction(functions, convertMIC(m));
	}

	public void convertEventHandlers(Map<String, HSIEForm> functions, List<EventHandlerInContext> eventHandlers) {
		for (EventHandlerInContext x : eventHandlers)
			addFunction(functions, convertEventHandler(x.scope, x.name, x.handler));
	}

	private void addFunction(Map<String, HSIEForm> functions, FunctionDefinition fd) {
		if (fd != null) {
			HSIEForm hs = hsie.handle(fd);
			if (hs != null)
				functions.put(hs.fnName, hs);
		}
	}

	// 2. Convert An individual element
	protected FunctionDefinition convertMIC(MethodInContext m) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		
		// Get the contract and from that find the method and thus the argument types
		List<Type> types = figureCMD(m);
		if (types == null)
			return null;

		if (m.method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		// Now process all of the method cases
		for (MethodCaseDefn mcd : m.method.cases)
			cases.add(new FunctionCaseDefn(null, mcd.intro.location, mcd.intro.name, mcd.intro.args, convertMessagesToActionList(mcd.intro.location, m.scope, mcd.intro.args, types, mcd.messages)));

		return new FunctionDefinition(m.method.intro.location, m.type, m.method.intro.name, m.method.intro.args.size(), cases);
	}

	public FunctionDefinition convertEventHandler(Scope scope, String card, EventHandlerDefinition eh) {
		List<Type> types = new ArrayList<Type>();
		for (@SuppressWarnings("unused") Object o : eh.intro.args) {
			types.add(tc.getType(null, "Any"));
		}
		if (eh.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		for (EventCaseDefn c : eh.cases) {
			cases.add(new FunctionCaseDefn(null, c.intro.location, c.intro.name, c.intro.args, convertMessagesToActionList(eh.intro.location, scope, eh.intro.args, types, c.messages)));
		}
		return new FunctionDefinition(eh.intro.location, HSIEForm.Type.EVENTHANDLER, eh.intro.name, eh.intro.args.size(), cases);
	}

	protected List<Type> figureCMD(MethodInContext m) {
		ContractDecl cd = contracts.get(m.fromContract);
		if (cd == null) {
			errors.message(m.contractLocation, "cannot find contract " + m.fromContract);
			return null;
		}
		ContractMethodDecl cmd = null;
		int idx = m.name.lastIndexOf(".");
		String mn = m.name.substring(idx+1);
		for (ContractMethodDecl md : cd.methods) {
			if (mn.equals(md.name)) { // TODO: should we check "dir"?
				cmd = md;
				break;
			}
		}
		if (cmd == null) {
			errors.message(m.contractLocation, "cannot find method " + mn + " in " + m.fromContract);
			return null;
		}
		
		List<Type> types = new ArrayList<Type>();
		boolean fail = false;
		for (Object o : cmd.args) {
			if (o instanceof TypedPattern) {
				TypedPattern to = (TypedPattern)o;
				String tn = to.type;
				Type t = tc.getType(to.typeLocation, tn);
				if (t == null) {
					errors.message(to.typeLocation, "there is no type " + tn);
					fail = true;
				}
				types.add(t);
			} else
				throw new UtilException("Cannot handle " + o.getClass().getName());
		}
		if (fail)
			return null;

		return types;
	}

	private Object convertMessagesToActionList(InputPosition location, Scope scope, List<Object> args, List<Type> types, List<MethodMessage> messages) {
		Object ret = scope.fromRoot(location, "Nil");
		for (int n = messages.size()-1;n>=0;n--) {
			MethodMessage mm = messages.get(n);
			Object me = convertMessageToAction(scope, args, types, mm);
			if (me == null) continue;
			InputPosition loc = ((Locatable)mm.expr).location();
			ret = new ApplyExpr(loc, scope.fromRoot(loc, "Cons"), me, ret);
		}
		return ret;
	}

	private Object convertMessageToAction(Scope scope, List<Object> margs, List<Type> types, MethodMessage mm) {
		if (mm.slot != null) {
			return convertAssignMessage(scope, margs, types, mm);
		} else if (mm.expr instanceof ApplyExpr) {
			
			// TODO: these two halves are very similar.  Try and refactor them back together at some point
			ApplyExpr root = (ApplyExpr) mm.expr;
			if (root instanceof ApplyExpr) {
				ApplyExpr fn = (ApplyExpr)root.fn;
				if (fn.fn instanceof AbsoluteVar && ((AbsoluteVar)fn.fn).id.equals("FLEval.field")) {
					Object sender = fn.args.get(0);
					StringLiteral method = (StringLiteral) fn.args.get(1);
					Type senderType = calculateExprType(margs, types, sender);
					if (senderType instanceof ContractImplements)
						return handleMethodCase(scope, (Locatable) sender, method, root.args);
					else
						return handleExprCase(scope, root);
				}
			}
			else if (root.fn instanceof AbsoluteVar) {
				// a case where we're building a message
				AbsoluteVar av = (AbsoluteVar) root.fn;
				String name = av.id;
				if (name.equals("FLEval.field")) {
					Object sender = root.args.get(0);
					StringLiteral method = (StringLiteral) root.args.get(1);
					Type senderType = calculateExprType(margs, types, sender);
					if (senderType instanceof ContractImplements)
						return handleMethodCase(scope, (Locatable) sender, method, new ArrayList<Object>());
					else
						return handleExprCase(scope, root);
				}
			}
		}
		InputPosition loc = null;
		if (mm.expr instanceof Locatable)
			loc = ((Locatable)mm.expr).location();
		errors.message(loc, "not a valid method message");
		return null;
	}

	protected Object convertAssignMessage(Scope scope, List<Object> margs, List<Type> types, MethodMessage mm) {
		Type exprType = calculateExprType(margs, types, mm.expr);
		Locatable slot = (Locatable) mm.slot.get(0);
		Object intoObj;
		StringLiteral slotName;
		Type slotType;
		if (slot instanceof CardMember) {
			CardMember cm = (CardMember) slot;
			intoObj = new CardStateRef(cm.location(), true); // TODO: only if from a handler ... how can we tell from here?
			slotName = new StringLiteral(cm.location(), cm.var);
			Type cti = tc.getType(cm.location(), cm.card);
			if (!(cti instanceof StructDefn))
				throw new UtilException("this should have been a struct");
			StructDefn sd = (StructDefn) cti;
			StructField sf = sd.findField(cm.var);
			if (sf == null) {
				errors.message(cm.location, "there is no card state member " + cm.var);
				return null;
			}
			if (sf.type instanceof ContractImplements) {
				errors.message(cm.location, "cannot assign to a contract var: " + cm.var);
				return null;
			}
			if (sf.type instanceof ContractService) {
				errors.message(cm.location, "cannot assign to a service var: " + cm.var);
				return null;
			}
			slotType = sf.type;
		} else if (slot instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda) slot;
			if (hl.type == null) {
				errors.message(slot.location(), "cannot assign to untyped handler lambda: " + hl.var);
				return null;
			}
			intoObj = hl;
			slotName = null;
			slotType = hl.type;
		} else if (slot instanceof LocalVar) {
			LocalVar lv = (LocalVar) slot;
			if (lv.type == null) {
				errors.message(lv.varLoc, "cannot use untyped argument as assign target: " + lv.var);
				return null;
			}
			intoObj = lv;
			slotName = null;
			slotType = lv.type;
		} else if (slot instanceof ExternalRef) {
			errors.message(slot.location(), "cannot assign to non-state member: " + ((ExternalRef)slot).uniqueName());
			return null;
		} else {
			throw new UtilException("Cannot handle slots of type " + slot.getClass());
		}
		if (mm.slot.size() > 1) {
			for (int i=1;i<mm.slot.size();i++) {
				LocatedToken si = (LocatedToken) mm.slot.get(i);
				if (!(slotType instanceof StructDefn)) {
					// There may be some valid cases mixed up in here; if so, fix them later
					errors.message(si.location(), "cannot extract member of a non-struct: " + si.text);
					return null;
				}
				StructDefn sd = (StructDefn) slotType;
				StructField sf = sd.findField(si.text);
				if (sf == null) {
					errors.message(si.location, "there is no field '" + si.text + "' in type " + sd);
					return null;
				}
				slotType = sf.type;
				if (slotName != null)
					intoObj = new ApplyExpr(si.location, scope.fromRoot(slot.location(), "."), intoObj, slotName);
				slotName = new StringLiteral(si.location, si.text);
			}
		} else if (slotName == null) {
			errors.message(slot.location(), "cannot assign directly to an object");
			return null;
		}
		if (!slotType.equals(exprType)) {
			errors.message(slot.location(), "cannot assign " + exprType + " to slot of type " + slotType);
			return null;
		}
		return new ApplyExpr(slot.location(), scope.fromRoot(slot.location(), "Assign"), intoObj, slotName, mm.expr);
	}

	private Object handleMethodCase(Scope scope, Locatable sender, StringLiteral method, List<Object> args) {
		// TODO: need to do all the remaining checking, e.g. method exists, types and the like ...
		return new ApplyExpr(sender.location(),	scope.fromRoot(sender.location(), "Send"), sender, method, asList(sender.location(), scope, args));
	}

	private Object handleExprCase(Scope scope, ApplyExpr root) {
		// TODO: need to check that it returns Action or [Action] at least
		return root;
	}

	protected Type calculateExprType(List<Object> margs, List<Type> types, Object expr) {
		List<Type> mytypes = new ArrayList<Type>();
//		System.out.println("Convert method: " + mm.slot + " <- " + mm.expr);
		List<String> args = new ArrayList<String>();
		for (int i=0;i<margs.size();i++) {
			Object x = margs.get(i);
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
		Type ret = tc.checkExpr(hsie.handleExprWith(expr, HSIEForm.Type.CONTRACT, args), mytypes);
		if (ret != null) {
			if (!margs.isEmpty()) {
				if (ret.iam != WhatAmI.FUNCTION)
					throw new UtilException("Should be function, but isn't");
				ret = ret.arg(margs.size());
			}
//			System.out.println("Type for method message - " + mm.slot + " <- " + mm.expr + " :: " + ret);
		}
		return ret;
	}

	private static Object asList(InputPosition loc, Scope scope, List<Object> args) {
		Object ret = scope.fromRoot(loc, "Nil");
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(loc, scope.fromRoot(loc, "Cons"), args.get(n), ret);
		}
		return ret;
	}
}
