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
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.LocalVar;
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
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeOfSomethingElse;
import org.flasck.flas.types.TypedObject;
import org.flasck.flas.types.Type.WhatAmI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {
	public static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final ErrorResult errors;
	private final Map<String, RWContractDecl> contracts;
	private final Type messageList;

	public MethodConvertor(ErrorResult errors, Rewriter rw) {
		this.errors = errors;
		this.contracts = rw.contracts;
		InputPosition posn = new InputPosition("builtin", 0, 0, "");
		this.messageList = ((Type)rw.getMe(posn, "List").defn).instance(posn, (Type) rw.getMe(posn, "Message").defn);
	}

	// 1. Main entry points to convert different kinds of things
	public void convertContractMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, RWMethodDefinition> methods) {
		for (RWMethodDefinition m : methods.values())
			addFunction(functions, convertMethod(rw, m));
	}

	public void convertEventHandlers(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, RWEventHandlerDefinition> eventHandlers) {
		for (RWEventHandlerDefinition x : eventHandlers.values())
			addFunction(functions, convertEventHandler(rw, x.name().containingCard().jsName(), x));
	}

	public void convertStandaloneMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Collection<RWMethodDefinition> methods) {
		for (RWMethodDefinition x : methods)
			addFunction(functions, convertStandalone(rw, x));
	}

	public void addFunction(Map<String, RWFunctionDefinition> functions, RWFunctionDefinition fd) {
		if (fd != null) {
			functions.put(fd.name(), fd);
		}
	}

	// 2. Convert An individual element
	protected RWFunctionDefinition convertMethod(Rewriter rw, RWMethodDefinition m) {
		logger.info("Converting " + (m.dir == RWMethodDefinition.DOWN?"down":"up") + " " + m.name().jsName());
		// Get the contract and from that find the method and thus the argument types
		List<Type> types;
		if (m.fromContract == null) {
			types = new ArrayList<Type>();
		} else {
			types = figureCMD(m);
			if (types == null)
				return null;
		}
		
		if (m.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		RWFunctionDefinition ret = new RWFunctionDefinition(m.name(), m.nargs(), true);

		// Now process all of the method cases
		Type ofType = null;
		for (RWMethodCaseDefn mcd : m.cases) {
			InputPosition loc = mcd.intro.location;
			if (mcd.intro.args.size() != types.size()) {
				if (!mcd.intro.args.isEmpty())
					loc = ((Locatable)mcd.intro.args.get(0)).location();
				errors.message(loc, "incorrect number of formal parameters to contract method '" + mcd.intro.fnName.jsName() +"': expected " + types.size() + " but was " + mcd.intro.args.size());
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
			ret.addCase(new RWFunctionCaseDefn(new RWFunctionIntro(loc,  mcd.intro.fnName, rwargs, mcd.intro.vars), ret.nextCase(), typedObject.expr));
			if (ofType == null)
				ofType = typedObject.type;
		}

		ret.gatherScopedVars();
		return ret;
	}

	public RWFunctionDefinition convertEventHandler(Rewriter rw, String card, RWEventHandlerDefinition eh) {
		List<Type> types = new ArrayList<Type>();
		if (eh.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		RWFunctionDefinition ret = new RWFunctionDefinition(eh.name(), eh.nargs(), true);
		for (RWEventCaseDefn c : eh.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, eh.location(), c.intro.args, types, c.messages, false);
			ret.addCase(new RWFunctionCaseDefn(c.intro, ret.nextCase(), typedObject.expr));
		}
		ret.gatherScopedVars();
		return ret;
	}

	public RWFunctionDefinition convertStandalone(Rewriter rw, RWMethodDefinition method) {
		logger.info("Converting standalone " + method.name().jsName());
		List<Object> margs = new ArrayList<Object>(/*mic.enclosingPatterns*/);
		// This seems likely to fail quite often :-)
		margs.addAll(method.cases.get(0).intro.args);
		List<Type> types = new ArrayList<Type>();
		for (@SuppressWarnings("unused") Object o : margs) {
			types.add(null);
		}
		if (method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		logger.info("Converting standalone " + method.name().jsName());
		List<RWFunctionCaseDefn> cases = new ArrayList<RWFunctionCaseDefn>();
		for (RWMethodCaseDefn c : method.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, method.location(), margs, types, c.messages, method.type.isHandler());
			cases.add(new RWFunctionCaseDefn(new RWFunctionIntro(c.intro.location, c.intro.fnName, margs, c.intro.vars), cases.size(), typedObject.expr));
		}
		RWFunctionDefinition ret = new RWFunctionDefinition(method.name(), margs.size(), true);
		ret.addCases(cases);
		ret.gatherScopedVars();
		return ret;
	}

	protected List<Type> figureCMD(RWMethodDefinition m) {
		if (m.fromContract == null) {
			errors.message(m.contractLocation, "cannot find contract " + m.fromContract);
			return null;
		}
		RWContractMethodDecl cmd = null;
		int idx = m.name().jsName().lastIndexOf(".");
		String mn = m.name().jsName().substring(idx+1);
		for (RWContractMethodDecl md : m.fromContract.methods) {
			if (mn.equals(md.name)) {
				if (m.dir == RWMethodDefinition.DOWN && md.dir.equals("up")) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is an up method");
					return null;
				}
				if (m.dir == RWMethodDefinition.UP && md.dir.equals("down")) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is a down method");
					return null;
				}
				cmd = md;
				break;
			}
		}
		if (cmd == null) {
			errors.message(m.contractLocation, "contract '" + m.fromContract.name() + "' does not have a method '" + mn +"' to implement");
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
						else if (margs.get(i) instanceof RWVarPattern && ((RWVarPattern)margs.get(i)).var.jsName().equals(m))
							t = types.get(i);
					}
					if (t == null)
						throw new UtilException("Can't handle this case yet (but I think it's an error): " + m + " not in " + margs);
					if (t instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) t, lv, method, args);
				}
				else if (sender instanceof ScopedVar) {
					ScopedVar vn = (ScopedVar) sender;
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
						else if (me.defn instanceof RWFunctionDefinition) {
							RWFunctionDefinition fd = (RWFunctionDefinition) me.defn;
							if (fd.nargs() > 0)
								errors.message(l.location(), "cannot use function " + me.id + " of arity " + fd.nargs() + " as constant");
							return new SendExpr(l.location(), l, method, args);
						}
						else
							throw new UtilException("Can't handle this case yet: " + me.defn.getClass());
					} else
						throw new UtilException("What is this?" + l.type);
				} else if (sender instanceof CardMember) {
					CardMember cm = (CardMember) sender;
					Type ot = cm.type;
					while (ot.iam == WhatAmI.INSTANCE)
						ot = ot.innerType();
					if (ot instanceof RWObjectDefn)
						return new SendExpr(((ApplyExpr)mm.expr).location(), sender, method, args);
					else
						return new TypeCheckMessages(root.location, root);
				} else
					throw new UtilException("Cannot handle this case: " + sender + " of " + sender.getClass());
			} else
				return new TypeCheckMessages(root.location, root);
		} else if (mm.expr instanceof HandlerLambda) {
			Locatable ex = (Locatable) mm.expr;
			return new TypeCheckMessages(ex.location(), new ApplyExpr(ex.location(), mm.expr, new ArrayList<>()));
		}
		InputPosition loc = null;
		if (mm.expr instanceof Locatable)
			loc = ((Locatable)mm.expr).location();
		errors.message(loc, "not a valid method message");
		return null;
	}

	protected Object convertAssignMessage(Rewriter rw, List<Object> margs, List<Type> types, RWMethodMessage mm, boolean fromHandler) {
		Locatable slot = (Locatable) mm.slot.get(0);
		InputPosition location = slot.location();
		Object intoObj;
		StringLiteral slotName;
		Type slotType;
		if (slot instanceof CardMember) {
			CardMember cm = (CardMember) slot;
			intoObj = new CardStateRef(cm.location(), fromHandler);
			CardGrouping grp = rw.cards.get(cm.card);
			RWStructDefn sd = grp.struct;
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
			slotName = new StringLiteral(cm.location(), cm.var);
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
				errors.message(lv.varLoc, "cannot use untyped argument as assign target: " + lv.uniqueName());
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
				location = si.location();
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
			}
		} else if (slotName == null) {
			errors.message(slot.location(), "cannot assign directly to an object");
			return null;
		}
		return new ApplyExpr(slot.location(), rw.getMe(slot.location(), "Assign"), intoObj, slotName, new AssertTypeExpr(location, slotType, mm.expr));
	}

	private Object handleMethodCase(Rewriter rw, InputPosition location, List<Object> margs, List<Type> types, TypeWithMethods senderType, Locatable sender, StringLiteral method, List<Object> args) {
		RWContractDecl cd = null;
		TypeWithMethods proto = senderType;
		Type methodType = null;
		if (senderType instanceof RWContractDecl) {
			proto = cd = (RWContractDecl) senderType;
			if (proto.hasMethod(method.text))
				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof RWContractService || senderType instanceof RWContractImplements) {
			proto = cd = (RWContractDecl) rw.getMe(senderType.location(), senderType.name()).defn;
			if (proto.hasMethod(method.text))
				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof RWObjectDefn) {
			RWObjectDefn od = (RWObjectDefn) senderType;
			if (senderType.hasMethod(method.text))
				methodType = od.getMethodType(method.text);
		}
		if (methodType == null) {
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
		// is it safe to assume methodType is a FUNCTION?  I will anyway, until proved otherwise
		if (methodType.arity() < args.size()) {
			errors.message(method.location, "too many arguments to " + method.text);
			return null;
		}
		if (methodType.arity() > args.size()) {
			errors.message(method.location, "missing arguments in call of " + method.text);
			return null;
		}
		return new SendExpr(location, sender, method, args);
	}
}
