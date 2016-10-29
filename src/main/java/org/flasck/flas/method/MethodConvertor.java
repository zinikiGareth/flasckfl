package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.TypeWithMethods;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.EventHandlerInContext;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.MethodInContext;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWEventCaseDefn;
import org.flasck.flas.rewrittenForm.RWEventHandlerDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.typechecker.TypeOfSomethingElse;
import org.flasck.flas.typechecker.TypedObject;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {
	public static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final ErrorResult errors;
	private final HSIE hsie;
//	private final TypeChecker tc;
	private final Map<String, RWContractDecl> contracts;
	private final Type messageList;

	public MethodConvertor(ErrorResult errors, HSIE hsie, TypeChecker tc, Map<String, RWContractDecl> contracts) {
		this.errors = errors;
		this.hsie = hsie;
//		this.tc = tc;
		this.contracts = contracts;
		InputPosition posn = new InputPosition("builtin", 0, 0, "");
		this.messageList = tc.getType(posn, "List").instance(posn, tc.getType(null, "Message"));
	}

	// 1. Main entry points to convert different kinds of things
	public void convertContractMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, MethodInContext> methods) {
		for (MethodInContext m : methods.values())
			addFunction(functions, convertMIC(rw, m));
	}

	public void convertEventHandlers(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, EventHandlerInContext> eventHandlers) {
		for (EventHandlerInContext x : eventHandlers.values())
			addFunction(functions, convertEventHandler(rw, x.name, x.handler));
	}

	public void convertStandaloneMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Collection<MethodInContext> methods) {
		for (MethodInContext x : methods)
			addFunction(functions, convertStandalone(rw, x));
	}

	// TODO: HSIE: this shouldn't be necessary, as we should just add it earlier ...
	public void addFunction(Map<String, RWFunctionDefinition> functions, RWFunctionDefinition fd) {
		if (fd != null) {
			functions.put(fd.name(), fd);
		}
	}

	// 2. Convert An individual element
	protected RWFunctionDefinition convertMIC(Rewriter rw, MethodInContext m) {
		logger.info("converting " + m.direction + " " + m.name);
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

		RWFunctionDefinition ret = new RWFunctionDefinition(m.method.location(), m.type, m.method.name(), m.method.nargs(), true);

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
			
			List<Object> rwargs = new ArrayList<>();
			for (int i=0;i<mcd.intro.args.size();i++) {
				Object patt = mcd.intro.args.get(i);
				Type ti = types.get(i);
				if (patt instanceof RWVarPattern) {
					RWVarPattern vp = (RWVarPattern) patt;
					InputPosition ploc = vp.location();
					rwargs.add(new RWTypedPattern(ploc, ti, ploc, vp.var));
				} else if (patt instanceof RWTypedPattern) {
					RWTypedPattern tp = (RWTypedPattern) patt;
					if (tp.type.name().equals(ti.name())) // it's fine as it is
						rwargs.add(tp);
					else if (ti.name().equals("Any")) // it's fine to subclass Any
						rwargs.add(tp);
					else
						throw new UtilException("Cannot handle the case where we have contract defining " + ti + " and we use " + tp.type);
				} else
					throw new UtilException("Cannot handle pattern " + patt.getClass());
			}
			TypedObject typedObject = convertMessagesToActionList(rw, loc, mcd.intro.args, types, mcd.messages, m.type.isHandler());
			ret.cases.add(new RWFunctionCaseDefn(new RWFunctionIntro(loc,  mcd.intro.name, rwargs, mcd.intro.vars), ret.cases.size(), typedObject.expr));
			if (ofType == null)
				ofType = typedObject.type;
		}
//		if (ofType != null)
//			tc.addExternal(m.method.name(), ofType);
		
		return ret;
	}

	public RWFunctionDefinition convertEventHandler(Rewriter rw, String card, RWEventHandlerDefinition eh) {
		List<Type> types = new ArrayList<Type>();
//		for (int i=0;i<eh.nargs();i++) {
//			types.add(tc.getType(null, "Any"));
//		}
		if (eh.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		RWFunctionDefinition ret = new RWFunctionDefinition(eh.location(), HSIEForm.CodeType.EVENTHANDLER, eh.name(), eh.nargs(), true);
//		Type ofType = null;
		for (RWEventCaseDefn c : eh.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, eh.location(), c.intro.args, types, c.messages, false);
//			if (ofType == null)
//				ofType = typedObject.type;
			ret.cases.add(new RWFunctionCaseDefn(new RWFunctionIntro(c.intro.location, c.intro.name, c.intro.args, c.intro.vars), ret.cases.size(), typedObject.expr));
		}
//		if (ofType != null)
//			tc.addExternal(eh.name(), ofType);
		return ret;
	}

	public RWFunctionDefinition convertStandalone(Rewriter rw, MethodInContext mic) {
		RWMethodDefinition method = mic.method;
		logger.info("Converting standalone " + method.name());
		List<Object> margs = new ArrayList<Object>(/*mic.enclosingPatterns*/);
		// This seems likely to fail quite often :-)
		margs.addAll(method.cases.get(0).intro.args);
		List<Type> types = new ArrayList<Type>();
		for (@SuppressWarnings("unused") Object o : margs) {
			types.add(null);
		}
		if (method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		logger.info("Converting standalone " + mic.name);
		List<RWFunctionCaseDefn> cases = new ArrayList<RWFunctionCaseDefn>();
//		Type ofType = null;
		for (RWMethodCaseDefn c : method.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, method.location(), margs, types, c.messages, mic.type.isHandler());
//			if (ofType == null)
//				ofType = typedObject.type;
			cases.add(new RWFunctionCaseDefn(new RWFunctionIntro(c.intro.location, c.intro.name, margs, c.intro.vars), cases.size(), typedObject.expr));
		}
		RWFunctionDefinition ret = new RWFunctionDefinition(method.location(), mic.type, method.name(), margs.size(), true);
		ret.cases.addAll(cases);
//		if (ofType != null)
//			tc.addExternal(method.name(), ofType);
		return ret;
	}

	protected List<Type> figureCMD(MethodInContext m) {
		RWContractDecl cd = contracts.get(m.fromContract);
		if (cd == null) {
			errors.message(m.contractLocation, "cannot find contract " + m.fromContract);
			return null;
		}
		RWContractMethodDecl cmd = null;
		int idx = m.name.lastIndexOf(".");
		String mn = m.name.substring(idx+1);
		for (RWContractMethodDecl md : cd.methods) {
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
			if (o instanceof RWTypedPattern) {
				types.add(((RWTypedPattern)o).type);
			} else
				throw new UtilException("Cannot handle " + o.getClass().getName());
		}
		if (fail)
			return null;

		return types;
	}

	private TypedObject convertMessagesToActionList(Rewriter rw, InputPosition location, List<Object> args, List<Type> types, List<RWMethodMessage> messages, boolean fromHandler) {
		Object ret = rw.getMe(location, "Nil");
		PackageVar cons = rw.getMe(location, "Cons");
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
				if (sender instanceof CardMember && ((CardMember)sender).type instanceof TypeWithMethods)
					return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) ((CardMember)sender).type, (Locatable) sender, method, args);
				else if (sender instanceof LocalVar) {
					LocalVar lv = (LocalVar) sender;
					String m = lv.uniqueName();
					Type t = null;
					for (int i=0;t == null && i<margs.size();i++) {
						if (margs.get(i) instanceof RWTypedPattern && ((RWTypedPattern)margs.get(i)).var.equals(m))
							t = ((RWTypedPattern)margs.get(i)).type;
						else if (margs.get(i) instanceof RWVarPattern && ((RWVarPattern)margs.get(i)).var.equals(m))
							t = types.get(i);
					}
					if (t == null)
						throw new UtilException("Can't handle this case yet (but I think it's an error)");
					if (t instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) t, lv, method, args);
				}
				else if (sender instanceof VarNestedFromOuterFunctionScope) {
					VarNestedFromOuterFunctionScope vn = (VarNestedFromOuterFunctionScope) sender;
					String other = vn.uniqueName();
					PackageVar me = rw.getMe(vn.location, other);
					if (me.defn instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) me.defn, vn, method, args);
					else
						throw new UtilException("Can't handle this case yet");
				}
				else if (sender instanceof HandlerLambda) {
					HandlerLambda l = (HandlerLambda) sender;
					if (l.type instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) l.type, l, method, args);
					else if (l.type instanceof TypeOfSomethingElse) {
						String other = ((TypeOfSomethingElse) l.type).other();
						PackageVar me = rw.getMe(l.location, other);
						if (me.defn instanceof TypeWithMethods)
							return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) me.defn, l, method, args);
						else
							throw new UtilException("Can't handle this case yet");
					} else
						throw new UtilException("What is this?" + l.type);
				} else
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
//		Type exprType = calculateExprType(margs, types, mm.expr);
//		if (exprType == null)
//			return null;
		Locatable slot = (Locatable) mm.slot.get(0);
		Object intoObj;
		StringLiteral slotName;
//		Type slotType;
		if (slot instanceof CardMember) {
			CardMember cm = (CardMember) slot;
			intoObj = new CardStateRef(cm.location(), fromHandler);
			slotName = new StringLiteral(cm.location(), cm.var);
			/*
			Type cti = tc.getType(cm.location(), cm.card);
			if (!(cti instanceof RWStructDefn))
				throw new UtilException("this should have been a struct");
			RWStructDefn sd = (RWStructDefn) cti;
			RWStructField sf = sd.findField(cm.var);
			if (sf == null) {
				errors.message(cm.location, "there is no card state member " + cm.var);
				return null;
			}
			if (sf.type instanceof RWContractImplements) {
				errors.message(cm.location, "cannot assign to a contract var: " + cm.var);
				return null;
			}
			if (sf.type instanceof RWContractService) {
				errors.message(cm.location, "cannot assign to a service var: " + cm.var);
				return null;
			}
			slotType = sf.type;
			*/
		} else if (slot instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda) slot;
			if (hl.type == null || hl.type.name().equals("Any")) {
				errors.message(slot.location(), "cannot assign to untyped handler lambda: " + hl.var);
				return null;
			}
			intoObj = hl;
			slotName = null;
//			slotType = hl.type;
		} else if (slot instanceof LocalVar) {
			LocalVar lv = (LocalVar) slot;
			if (lv.type == null) {
				errors.message(lv.varLoc, "cannot use untyped argument as assign target: " + lv.uniqueName());
				return null;
			}
			intoObj = lv;
			slotName = null;
//			slotType = lv.type;
		} else if (slot instanceof ExternalRef) {
			errors.message(slot.location(), "cannot assign to non-state member: " + ((ExternalRef)slot).uniqueName());
			return null;
		} else {
			throw new UtilException("Cannot handle slots of type " + slot.getClass());
		}
		if (mm.slot.size() > 1) {
			for (int i=1;i<mm.slot.size();i++) {
				LocatedToken si = (LocatedToken) mm.slot.get(i);
				/*
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
					intoObj = new ApplyExpr(si.location, new PackageVar(si.location, "FLEval.field", null), intoObj, slotName);
				slotName = new StringLiteral(si.location, si.text);
				*/
			}
		} else if (slotName == null) {
			errors.message(slot.location(), "cannot assign directly to an object");
			return null;
		}
		/*
		if (exprType.name() != null && exprType.name().equals("MessageWrapper"))
			exprType = exprType.poly(0);
		if (!slotType.equals(exprType)) {
			boolean isOK = false;
			Type foo = slotType;
			if (slotType.iam == WhatAmI.INSTANCE)
				foo = slotType.innerType();
			if (foo instanceof RWUnionTypeDefn) {
				for (Type t : ((RWUnionTypeDefn)foo).cases) {
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
		*/
		return new ApplyExpr(slot.location(), rw.getMe(slot.location(), "Assign"), intoObj, slotName, mm.expr);
	}

	private Object handleMethodCase(Rewriter rw, InputPosition location, List<Object> margs, List<Type> types, TypeWithMethods senderType, Locatable sender, StringLiteral method, List<Object> args) {
		RWContractDecl cd = null;
		TypeWithMethods proto = senderType;
//		Type methodType = null;
		if (senderType instanceof RWContractDecl) {
			proto = cd = (RWContractDecl) senderType;
//			if (proto.hasMethod(method.text))
//				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof RWContractService || senderType instanceof RWContractImplements) {
			proto = cd = (RWContractDecl) rw.getMe(senderType.location(), senderType.name()).defn;
//			if (proto.hasMethod(method.text))
//				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof RWObjectDefn) {
			RWObjectDefn od = (RWObjectDefn) senderType;
//			if (senderType.hasMethod(method.text))
//				methodType = od.getMethod(method.text);
		}
		if (!proto.hasMethod(method.text)) {
			errors.message(method.location, "there is no method '" + method.text + "' in " + proto.name());
			return null;
		}
		if (senderType instanceof RWContractImplements && !cd.checkMethodDir(method.text, "up")) {
			errors.message(method.location, "can only call up methods on contract implementations");
			return null;
		}
		if (senderType instanceof RWContractService && !cd.checkMethodDir(method.text, "down")) {
			errors.message(method.location, "can only call down methods on service implementations");
			return null;
		}
		/* TODO: HSIE
		if (methodType == null)
			throw new UtilException("We should have figured out the type by now");
		List<Object> m1 = new ArrayList<>(margs);
		m1.add(new RWVarPattern(location, "__me.__m"));
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
			errors.message(method.location, "type checking error during method conversion"); // I don't actually see how this could happen ... maybe should throw exception?
			return null;
		}
		*/
		return new ApplyExpr(sender.location(),	rw.getMe(location, "Send"), sender, method, asList(sender.location(), rw, args));
	}

	private Object handleExprCase(Rewriter rw, List<Object> margs, List<Type> types, ApplyExpr expr) {
		/* TODO: HSIE
		Type t = calculateExprType(margs, types, expr);
		if (t == null)
			return null;
		if (t.iam == WhatAmI.FUNCTION) {
			errors.message(expr.location, "method expression must be of type Message or List[Message]");
			return null;
		}
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
		*/
		
		// TODO: HSIE: this is where we need to add the type constraint: typecheck(expr, "Message")
		return expr;
	}

	/* TODO: HSIE
	protected Type calculateExprType(List<Object> margs, List<Type> types, Object expr) {
		List<Type> mytypes = new ArrayList<Type>();
//		System.out.println("Convert method: " + mm.slot + " <- " + mm.expr);
		List<String> args = new ArrayList<String>();
		List<InputPosition> locs = new ArrayList<>();
		for (int i=0;i<margs.size();i++) {
			Object x = margs.get(i);
			if (x instanceof RWVarPattern) {
				mytypes.add(types.get(i));
				args.add(((RWVarPattern)x).var);
				locs.add(((RWVarPattern)x).varLoc);
			} else if (x instanceof RWTypedPattern) {
				RWTypedPattern tx = (RWTypedPattern)x;
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
	*/

	private static Object asList(InputPosition loc, Rewriter rw, List<Object> args) {
		Object ret = rw.getMe(loc, "Nil");
		for (int n = args.size()-1;n>=0;n--) {
			ret = new ApplyExpr(loc, rw.getMe(loc, "Cons"), args.get(n), ret);
		}
		return ret;
	}
}
