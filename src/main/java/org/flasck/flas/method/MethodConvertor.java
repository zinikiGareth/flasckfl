package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardStateRef;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TypeWithMethods;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.EventHandlerInContext;
import org.flasck.flas.rewrittenForm.MethodInContext;
import org.flasck.flas.rewrittenForm.RWEventCaseDefn;
import org.flasck.flas.rewrittenForm.RWEventHandlerDefinition;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.typechecker.TypedObject;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {
	private final ErrorResult errors;
	private final HSIE hsie;
	private final TypeChecker tc;
	private final Map<String, ContractDecl> contracts;
	private final Type messageList;

	public MethodConvertor(ErrorResult errors, HSIE hsie, TypeChecker tc, Map<String, ContractDecl> contracts) {
		this.errors = errors;
		this.hsie = hsie;
		this.tc = tc;
		this.contracts = contracts;
		this.messageList = tc.getType(new InputPosition("builtin", 0, 0, ""), "List").instance(new InputPosition("builtin", 0, 0, ""), tc.getType(null, "Message"));
	}

	// 1. Main entry points to convert different kinds of things
	public void convertContractMethods(Rewriter rw, Map<String, HSIEForm> functions, List<MethodInContext> methods) {
		for (MethodInContext m : methods)
			addFunction(functions, convertMIC(rw, m));
	}

	public void convertEventHandlers(Rewriter rw, Map<String, HSIEForm> functions, List<EventHandlerInContext> eventHandlers) {
		for (EventHandlerInContext x : eventHandlers)
			addFunction(functions, convertEventHandler(rw, x.name, x.handler));
	}

	public void convertStandaloneMethods(Rewriter rw, Map<String, HSIEForm> functions, Collection<MethodInContext> methods) {
		for (MethodInContext x : methods)
			addFunction(functions, convertStandalone(rw, x));
	}

	public void addFunction(Map<String, HSIEForm> functions, FunctionDefinition fd) {
		if (fd != null) {
			HSIEForm hs = hsie.handle(null, fd);
			if (hs != null)
				functions.put(hs.fnName, hs);
		}
	}

	// 2. Convert An individual element
	protected FunctionDefinition convertMIC(Rewriter rw, MethodInContext m) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		
		if (m.direction == MethodInContext.STANDALONE)
			System.out.println("converting " + m.name);
		// Get the contract and from that find the method and thus the argument types
		List<Type> types;
		if (m.fromContract == null) {
			types = new ArrayList<Type>();
		} else {
			types = figureCMD(m);
			if (types == null)
				return null;
		}
		
		if (m.method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		// Now process all of the method cases
		Type ofType = null;
		for (RWMethodCaseDefn mcd : m.method.cases) {
			InputPosition loc = mcd.intro.location;
			if (mcd.intro.args.size() != types.size()) {
				if (!mcd.intro.args.isEmpty())
					loc = ((Locatable)mcd.intro.args.get(0)).location();
				errors.message(loc, "incorrect number of formal parameters to contract method '" + mcd.intro.name +"': expected " + types.size() + " but was " + mcd.intro.args.size());
				continue;
			}
			TypedObject typedObject = convertMessagesToActionList(rw, loc, mcd.intro.args, types, mcd.messages, m.type.isHandler());
			cases.add(new FunctionCaseDefn(loc, mcd.intro.name, mcd.intro.args, typedObject.expr));
			if (ofType == null)
				ofType = typedObject.type;
		}
		if (ofType != null)
			tc.addExternal(m.method.intro.name, ofType);
		
		return new FunctionDefinition(m.method.intro.location, m.type, m.method.intro.name, m.method.intro.args.size(), cases);
	}

	public FunctionDefinition convertEventHandler(Rewriter rw, String card, RWEventHandlerDefinition eh) {
		List<Type> types = new ArrayList<Type>();
		for (@SuppressWarnings("unused") Object o : eh.intro.args) {
			types.add(tc.getType(null, "Any"));
		}
		if (eh.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		Type ofType = null;
		for (RWEventCaseDefn c : eh.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, eh.intro.location, eh.intro.args, types, c.messages, false);
			if (ofType == null)
				ofType = typedObject.type;
			cases.add(new FunctionCaseDefn(c.intro.location, c.intro.name, c.intro.args, typedObject.expr));
		}
		if (ofType != null)
			tc.addExternal(eh.intro.name, ofType);
		return new FunctionDefinition(eh.intro.location, HSIEForm.CodeType.EVENTHANDLER, eh.intro.name, eh.intro.args.size(), cases);
	}

	public FunctionDefinition convertStandalone(Rewriter rw, MethodInContext mic) {
		RWMethodDefinition method = mic.method;
		List<Object> margs = new ArrayList<Object>(/*mic.enclosingPatterns*/);
		margs.addAll(method.intro.args);
		List<Type> types = new ArrayList<Type>();
		for (@SuppressWarnings("unused") Object o : margs) {
			types.add(tc.getType(null, "Any"));
		}
		if (method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		Type ofType = null;
		for (RWMethodCaseDefn c : method.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, method.intro.location, margs, types, c.messages, mic.type.isHandler());
			if (ofType == null)
				ofType = typedObject.type;
			cases.add(new FunctionCaseDefn(c.intro.location, c.intro.name, margs, typedObject.expr));
		}
		if (ofType != null)
			tc.addExternal(method.intro.name, ofType);
		return new FunctionDefinition(method.intro.location, mic.type, method.intro.name, margs.size(), cases);
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
			if (mn.equals(md.name)) {
				if (m.direction == MethodInContext.DOWN && md.dir.equals("up")) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is an up method");
					return null;
				}
				if (m.direction == MethodInContext.UP && md.dir.equals("down")) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is a down method");
					return null;
				}
				cmd = md;
				break;
			}
		}
		if (cmd == null) {
			errors.message(m.contractLocation, "contract '" + m.fromContract + "' does not have a method '" + mn +"' to implement");
			return null;
		}
		
		List<Type> types = new ArrayList<Type>();
		boolean fail = false;
		for (Object o : cmd.args) {
			if (o instanceof TypedPattern) {
				types.add(((TypedPattern)o).type);
			} else
				throw new UtilException("Cannot handle " + o.getClass().getName());
		}
		if (fail)
			return null;

		return types;
	}

	private TypedObject convertMessagesToActionList(Rewriter rw, InputPosition location, List<Object> args, List<Type> types, List<RWMethodMessage> messages, boolean fromHandler) {
		Object ret = rw.structs.get("Nil");
		RWStructDefn cons = rw.structs.get("Cons");
		for (int n = messages.size()-1;n>=0;n--) {
			RWMethodMessage mm = messages.get(n);
			Object me = convertMessageToAction(rw, args, types, mm, fromHandler);
			if (me == null) continue;
			InputPosition loc = ((Locatable)mm.expr).location();
			ret = new ApplyExpr(loc, cons, me, ret);
		}
		List<Type> fnargs = new ArrayList<Type>(types);
		fnargs.add(messageList);
		return new TypedObject(Type.function(location, fnargs), ret);
	}

	private Object convertMessageToAction(Rewriter rw, List<Object> margs, List<Type> types, RWMethodMessage mm, boolean fromHandler) {
//		System.out.println("Converting " + mm);
		if (mm.slot != null) {
			return convertAssignMessage(rw, margs, types, mm, fromHandler);
		} else if (mm.expr instanceof ApplyExpr) {
			ApplyExpr root = (ApplyExpr) mm.expr;
			List<Object> args;
			if (root.fn instanceof ApplyExpr) {
				args = root.args; 
				root = (ApplyExpr)root.fn;
			} else
				args = new ArrayList<Object>();
			if (root.fn instanceof PackageVar && ((PackageVar)root.fn).id.equals("FLEval.field")) {
				Object sender = root.args.get(0);
				StringLiteral method = (StringLiteral) root.args.get(1);
				Type senderType = calculateExprType(margs, types, sender);
				if (senderType == null)
					return null;
				while (senderType.iam == WhatAmI.INSTANCE)
					senderType = senderType.innerType();
				if (senderType instanceof TypeWithMethods)
					return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) senderType, (Locatable) sender, method, args);
				else
					return handleExprCase(rw, margs, types, root);
			} else
				return handleExprCase(rw, margs, types, root);
		}
		InputPosition loc = null;
		if (mm.expr instanceof Locatable)
			loc = ((Locatable)mm.expr).location();
		errors.message(loc, "not a valid method message");
		return null;
	}

	protected Object convertAssignMessage(Rewriter rw, List<Object> margs, List<Type> types, RWMethodMessage mm, boolean fromHandler) {
		Type exprType = calculateExprType(margs, types, mm.expr);
		if (exprType == null)
			return null;
		Locatable slot = (Locatable) mm.slot.get(0);
		Object intoObj;
		StringLiteral slotName;
		Type slotType;
		if (slot instanceof CardMember) {
			CardMember cm = (CardMember) slot;
			intoObj = new CardStateRef(cm.location(), fromHandler);
			slotName = new StringLiteral(cm.location(), cm.var);
			Type cti = tc.getType(cm.location(), cm.card);
			if (!(cti instanceof RWStructDefn))
				throw new UtilException("this should have been a struct");
			RWStructDefn sd = (RWStructDefn) cti;
			RWStructField sf = sd.findField(cm.var);
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
			if (hl.type == null || hl.type.name().equals("Any")) {
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
				if (!(slotType instanceof RWStructDefn)) {
					// There may be some valid cases mixed up in here; if so, fix them later
					errors.message(si.location(), "cannot extract member '" + si.text + "' of a non-struct: '" + slotType.name() + "'");
					return null;
				}
				RWStructDefn sd = (RWStructDefn) slotType;
				RWStructField sf = sd.findField(si.text);
				if (sf == null) {
					errors.message(si.location, "there is no field '" + si.text + "' in type " + sd);
					return null;
				}
				slotType = sf.type;
				if (slotName != null)
					intoObj = new ApplyExpr(si.location, rw.functions.get("."), intoObj, slotName);
				slotName = new StringLiteral(si.location, si.text);
			}
		} else if (slotName == null) {
			errors.message(slot.location(), "cannot assign directly to an object");
			return null;
		}
		if (exprType.name() != null && exprType.name().equals("MessageWrapper"))
			exprType = exprType.poly(0);
		if (!slotType.equals(exprType)) {
			boolean isOK = false;
			Type foo = slotType;
			if (slotType.iam == WhatAmI.INSTANCE)
				foo = slotType.innerType();
			if (foo instanceof UnionTypeDefn) {
				for (Type t : ((UnionTypeDefn)foo).cases) {
					Type u = t;
					if (slotType.iam == WhatAmI.INSTANCE)
						u = t.applyInstanceVarsFrom(slotType);
					if (u.equals(exprType)) {
						isOK = true;
						break;
					}
				}
			}
			if (!isOK) {
				errors.message(slot.location(), "cannot assign " + exprType + " to slot of type " + slotType);
				return null;
			}
		}
		return new ApplyExpr(slot.location(), rw.structs.get("Assign"), intoObj, slotName, mm.expr);
	}

	private Object handleMethodCase(Rewriter rw, InputPosition location, List<Object> margs, List<Type> types, TypeWithMethods senderType, Locatable sender, StringLiteral method, List<Object> args) {
		ContractDecl cd = null;
		TypeWithMethods proto = senderType;
		Type methodType = null;
		if (senderType instanceof ContractDecl) {
			proto = cd = (ContractDecl) senderType;
			if (proto.hasMethod(method.text))
				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof ContractService || senderType instanceof ContractImplements) {
			proto = cd = (ContractDecl) tc.getType(senderType.location(), senderType.name());
			if (proto.hasMethod(method.text))
				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) senderType;
			if (senderType.hasMethod(method.text))
				methodType = od.getMethod(method.text);
		}
		if (!proto.hasMethod(method.text)) {
			errors.message(method.location, "there is no method '" + method.text + "' in " + proto.name());
			return null;
		}
		if (senderType instanceof ContractImplements && !cd.checkMethodDir(method.text, "up")) {
			errors.message(method.location, "can only call up methods on contract implementations");
			return null;
		}
		if (senderType instanceof ContractService && !cd.checkMethodDir(method.text, "down")) {
			errors.message(method.location, "can only call down methods on service implementations");
			return null;
		}
		if (methodType == null)
			throw new UtilException("We should have figured out the type by now");
		List<Object> m1 = new ArrayList<>(margs);
		m1.add(new VarPattern(location, "__m"));
		List<Type> t1 = new ArrayList<>(types);
		t1.add(methodType);
		Type ct = calculateExprType(m1, t1, new ApplyExpr(location, new LocalVar("__me", location, "__m", location, methodType), args));
		if (ct == null)
			return null; // should have reported the error already
		if (ct.iam == WhatAmI.FUNCTION) {
			errors.message(method.location, "missing arguments in call of " + method.text);
			return null;
		}
		if (ct.iam != WhatAmI.STRUCT || !ct.name().equals("Send")) {
			errors.message(method.location, "type checking error"); // I don't actually see how this could happen ... maybe should throw exception?
			return null;
		}
		return new ApplyExpr(sender.location(),	rw.structs.get("Send"), sender, method, asList(sender.location(), rw, args));
	}

	private Object handleExprCase(Rewriter rw, List<Object> margs, List<Type> types, ApplyExpr expr) {
		Type t = calculateExprType(margs, types, expr);
		if (t == null)
			return null;
		if (t.name().equals("Nil"))
			return expr;
		if (t.iam == WhatAmI.INSTANCE) {
			// to be an instance, it must be a List of one of the types
			if (!t.name().equals("List") && !t.name().equals("Cons")) {
				errors.message(expr.location, "method expression must be of type Message or List[Message], not " + t.name());
				return null;
			}
			// if it is a list, check what it's a list of ...
			t = t.poly(0);
		}
		if (t.iam != WhatAmI.STRUCT && t.iam != WhatAmI.UNION) {
			errors.message(expr.location, "method expression must be of type Message or List[Message], not " + t.name());
			return null;
		}
		String name = t.name();
		if (!name.equals("Nil") && !name.equals("Message") && !name.equals("Send") && !name.equals("Assign") && !name.equals("CreateCard") && !name.equals("D3Action") && !name.equals("Debug")) {
			errors.message(expr.location, "expression must be of type Message or List[Message], not " + name);
			return null;
		}
		return expr;
	}

	protected Type calculateExprType(List<Object> margs, List<Type> types, Object expr) {
		List<Type> mytypes = new ArrayList<Type>();
//		System.out.println("Convert method: " + mm.slot + " <- " + mm.expr);
		List<String> args = new ArrayList<String>();
		List<InputPosition> locs = new ArrayList<>();
		for (int i=0;i<margs.size();i++) {
			Object x = margs.get(i);
			if (x instanceof VarPattern) {
				mytypes.add(types.get(i));
				args.add(((VarPattern)x).var);
				locs.add(((VarPattern)x).varLoc);
			} else if (x instanceof TypedPattern) {
				TypedPattern tx = (TypedPattern)x;
				args.add(tx.var);
				Type ty = tx.type;
				// we have an obligation to check that ty is a sub-type of types.get(i);
				Type prev = types.get(i);
				if (prev == null)
					; // not a lot we can do, but there's probably an error out there somewhere
				else if (prev.name().equals("Any"))
					;	// this has to be OK
				else if (prev.name().equals(ty.name())) {
					;	// this should be OK - but do we need to consider poly vars?
				}
				else {
					errors.message(tx.typeLocation, "cannot change method type from " + prev.name() + " to " + ty.name());
				}
				mytypes.add(ty);
				locs.add(tx.typeLocation);
			} else
				throw new UtilException("Cannot map " + x.getClass());
		}
		HSIEForm hs = hsie.handleExprWith(expr, HSIEForm.CodeType.CONTRACT, args);
		Type ret = tc.checkExpr(hs, mytypes, locs);
		if (ret != null) {
//			System.out.println("Returned type was " + ret + " and margs was " + margs);
			if (!margs.isEmpty()) {
				if (ret.iam != WhatAmI.FUNCTION)
					throw new UtilException("Should be function, but isn't");
				if (ret.arity() > margs.size()) {
					List<Type> tmp = new ArrayList<>();
					for (int i=margs.size();i<=ret.arity();i++)
						tmp.add(ret.arg(i));
					ret = Type.function(ret.location(), tmp);
				} else if (ret.arity() < margs.size())
					throw new UtilException("I don't think this should be possible");
				else
					ret = ret.arg(margs.size());
			}
//			System.out.println("Type for method message - " + mm.slot + " <- " + mm.expr + " :: " + ret);
		}
		return ret;
	}

	private static Object asList(InputPosition loc, Rewriter rw, List<Object> args) {
		Object ret = rw.structs.get("Nil");
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(loc, rw.structs.get("Cons"), args.get(n), ret);
		}
		return ret;
	}
}
