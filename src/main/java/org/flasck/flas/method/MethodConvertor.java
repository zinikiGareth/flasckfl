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
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.ObjectWithState;
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
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.InstanceType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeOfSomethingElse;
import org.flasck.flas.types.TypeWithMethods;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.types.TypedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class MethodConvertor {
	public static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final ErrorResult errors;
	private final Type messageList;

	public MethodConvertor(ErrorResult errors, Rewriter rw) {
		this.errors = errors;
		InputPosition posn = new InputPosition("builtin", 0, 0, "");
		this.messageList = ((TypeWithName)rw.getMe(posn, new SolidName(null, "List")).defn).instance(posn, (Type) rw.getMe(posn, new SolidName(null, "Message")).defn);
	}

	// 1. Main entry points to convert different kinds of things
	public void convertContractMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, RWMethodDefinition> methods) {
		for (RWMethodDefinition m : methods.values())
			addFunction(functions, convertMethod(rw, m));
	}

	public void convertEventHandlers(Rewriter rw, Map<String, RWFunctionDefinition> functions, Map<String, RWEventHandlerDefinition> eventHandlers) {
		for (RWEventHandlerDefinition x : eventHandlers.values())
			addFunction(functions, convertEventHandler(rw, x));
	}

	public void convertStandaloneMethods(Rewriter rw, Map<String, RWFunctionDefinition> functions, Collection<RWMethodDefinition> methods) {
		for (RWMethodDefinition x : methods)
			addFunction(functions, convertStandalone(rw, x));
	}

	public void addFunction(Map<String, RWFunctionDefinition> functions, RWFunctionDefinition fd) {
		if (fd != null) {
			functions.put(fd.uniqueName(), fd);
		}
	}

	// 2. Convert An individual element
	protected RWFunctionDefinition convertMethod(Rewriter rw, RWMethodDefinition m) {
		logger.info("Converting " + (m.dir == RWMethodDefinition.DOWN?"down":"up") + " " + m.name().uniqueName());
		// Get the contract and from that find the method and thus the argument types
		List<TypeWithName> types;
		List<TypeWithName> atypes;
		List<RWTypedPattern> handlers = new ArrayList<>();
		if (m.fromContract == null) {
			types = null;
			atypes = new ArrayList<>();
		} else {
			atypes = types = figureCMD(m, handlers);
			if (types == null)
				return null;
		}
		
		if (m.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		// TODO: this code for IdempotentHandlers goes away (or at least changes) when we
		// have the parser do the work for us
		boolean isPost = true;
		RWTypedPattern handler = null;
		if (!handlers.isEmpty()) {
			isPost = false;
			handler = handlers.get(0);
		} else if (!atypes.isEmpty()) {
			TypeWithName htype = atypes.get(atypes.size()-1);
			if (htype instanceof RWContractDecl)
				isPost = false;
		}
		RWFunctionDefinition ret = new RWFunctionDefinition(m.name(), m.nargs()+(isPost?1:0), true);

		// Now process all of the method cases
		Type ofType = null;
		for (RWMethodCaseDefn mcd : m.cases) {
			InputPosition loc = mcd.intro.location;
			if (types != null && mcd.intro.args.size() != types.size() + (isPost?0:1)) {
				if (!mcd.intro.args.isEmpty())
					loc = ((Locatable)mcd.intro.args.get(0)).location();
				errors.message(loc, "incorrect number of formal parameters to contract method '" + mcd.intro.fnName.uniqueName() +"': expected " + types.size() + " but was " + mcd.intro.args.size());
				continue;
			}
			
			List<Object> rwargs = new ArrayList<>();
			for (int i=0;i<mcd.intro.args.size();i++) {
				Object patt = mcd.intro.args.get(i);
				TypeWithName ti = null;
				if (types != null) {
					if (i >= types.size())
						ti = handlers.get(0).type;
					else
						ti = types.get(i);
				}
				if (patt instanceof RWVarPattern) {
					RWVarPattern vp = (RWVarPattern) patt;
					InputPosition ploc = vp.location();
					if (ti == null) {
						ti = rw.types.get("Any");
						atypes.add(ti);
					}
					rwargs.add(new RWTypedPattern(ploc, ti, ploc, vp.var));
				} else if (patt instanceof RWTypedPattern) {
					RWTypedPattern tp = (RWTypedPattern) patt;
					if (ti == null) {
						atypes.add(tp.type); 
						rwargs.add(tp);
					} else if (tp.type.nameAsString().equals(ti.nameAsString())) // it's fine as it is
						rwargs.add(tp);
					else if (ti.nameAsString().equals("Any")) // it's fine to subclass Any
						rwargs.add(tp);
					else
						throw new UtilException("Cannot handle the case where we have contract defining " + ti + " and we use " + tp.type);
				} else
					throw new UtilException("Cannot handle pattern " + patt.getClass());
			}
			if (isPost)
				rwargs.add(new RWTypedPattern(m.location(), rw.types.get("Any"), m.location(), new VarName(m.location(), m.name().inContext, "_ih")));
			TypedObject typedObject = convertMessagesToActionList(rw, loc, mcd.intro.args, atypes, mcd.messages, m.type.isHandler(), handler);
			ret.addCase(new RWFunctionCaseDefn(new RWFunctionIntro(loc,  mcd.intro.fnName, rwargs, mcd.intro.vars), ret.nextCase(), typedObject.expr));
			if (ofType == null)
				ofType = typedObject.type;
		}

		ret.gatherScopedVars();
		return ret;
	}

	public RWFunctionDefinition convertEventHandler(Rewriter rw, RWEventHandlerDefinition eh) {
		List<TypeWithName> types = new ArrayList<>();
		if (eh.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		RWFunctionDefinition ret = new RWFunctionDefinition(eh.name(), eh.nargs(), true);
		for (RWEventCaseDefn c : eh.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, eh.location(), c.intro.args, types, c.messages, false, null);
			ret.addCase(new RWFunctionCaseDefn(c.intro, ret.nextCase(), typedObject.expr));
		}
		ret.gatherScopedVars();
		return ret;
	}

	public RWFunctionDefinition convertStandalone(Rewriter rw, RWMethodDefinition method) {
		logger.info("Converting standalone " + method.name().uniqueName());
		List<Object> margs = new ArrayList<Object>(/*mic.enclosingPatterns*/);
		// This seems likely to fail quite often :-)
		margs.addAll(method.cases.get(0).intro.args);
		List<TypeWithName> types = new ArrayList<>();
		for (@SuppressWarnings("unused") Object o : margs) {
			types.add(null);
		}
		if (method.cases.isEmpty())
			throw new UtilException("Method without any cases - valid or not valid?");

		logger.info("Converting standalone " + method.name().uniqueName());
		List<RWFunctionCaseDefn> cases = new ArrayList<RWFunctionCaseDefn>();
		for (RWMethodCaseDefn c : method.cases) {
			TypedObject typedObject = convertMessagesToActionList(rw, method.location(), margs, types, c.messages, method.type.isHandler(), null);
			cases.add(new RWFunctionCaseDefn(new RWFunctionIntro(c.intro.location, c.intro.fnName, margs, c.intro.vars), cases.size(), typedObject.expr));
		}
		RWFunctionDefinition ret = new RWFunctionDefinition(method.name(), margs.size(), true);
		ret.addCases(cases);
		ret.gatherScopedVars();
		return ret;
	}

	protected List<TypeWithName> figureCMD(RWMethodDefinition m, List<RWTypedPattern> handlers) {
		if (m.fromContract == null) {
			errors.message(m.contractLocation, "cannot find contract " + m.fromContract);
			return null;
		}
		RWContractMethodDecl cmd = null;
		String mn = m.name().name;
		for (RWContractMethodDecl md : m.fromContract.methods) {
			if (mn.equals(md.name)) {
				if (m.dir == RWMethodDefinition.DOWN && md.dir.equals(ContractMethodDir.UP)) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is an up method");
					return null;
				}
				if (m.dir == RWMethodDefinition.UP && md.dir.equals(ContractMethodDir.DOWN)) {
					errors.message(m.contractLocation, "cannot implement '" + md.name + "' because it is a down method");
					return null;
				}
				cmd = md;
				break;
			}
		}
		if (cmd == null) {
			errors.message(m.contractLocation, "contract '" + m.fromContract.nameAsString() + "' does not have a method '" + mn +"' to implement");
			return null;
		}
		
		if (cmd.handler != null)
			handlers.add(cmd.handler);

		List<TypeWithName> types = new ArrayList<>();
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

	private TypedObject convertMessagesToActionList(Rewriter rw, InputPosition location, List<Object> args, List<TypeWithName> types, List<RWMethodMessage> messages, boolean fromHandler, RWTypedPattern methodHandler) {
		Object ret = rw.getMe(location, new SolidName(null, "Nil"));
		PackageVar cons = rw.getMe(location, new SolidName(null, "Cons"));
		for (int n = messages.size()-1;n>=0;n--) {
			RWMethodMessage mm = messages.get(n);
			Object me = convertMessageToAction(rw, args, types, mm, fromHandler, methodHandler);
			if (me == null) continue;
			InputPosition loc = ((Locatable)mm.expr).location();
			ret = new ApplyExpr(loc, cons, me, ret);
		}
		List<Type> fnargs = new ArrayList<Type>(types);
		fnargs.add(messageList);
		return new TypedObject(new FunctionType(location, fnargs), ret);
	}

	private Object convertMessageToAction(Rewriter rw, List<Object> margs, List<TypeWithName> types, RWMethodMessage mm, boolean fromHandler, RWTypedPattern methodHandler) {
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
			/** This hack is here to handle the fact that we don't specifically call out
			 * the subscription or idempotent handler in the parser yet.  When we do, this should go away
			 */
			Object handler = null;
			if (!args.isEmpty()) {
				Object hexpr = args.get(args.size()-1);
				if (hexpr instanceof ApplyExpr) {
					final ApplyExpr ae = (ApplyExpr)hexpr;
					if (ae.fn instanceof ObjectReference) {
						// it's a handler ...
						args.remove(args.size()-1);
						handler = hexpr;
					} else if (ae.fn instanceof ScopedVar) {
						ScopedVar vn = (ScopedVar) ae.fn;
						PackageVar pv = rw.getMe(vn.location, vn.id);
						if (pv != null && pv.defn instanceof RWHandlerImplements) {
							args.remove(args.size()-1);
							handler = hexpr;
						}
					}
				}
			}
			/** End Hack */
			if (root.fn instanceof BuiltinOperation && ((BuiltinOperation)root.fn).isField()) {
				Object sender = root.args.get(0);
				StringLiteral method = (StringLiteral) root.args.get(1);
				if (sender instanceof CardMember && ((CardMember)sender).type instanceof TypeWithMethods)
					return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) ((CardMember)sender).type, (Locatable) sender, method, args, handler);
				else if (sender instanceof LocalVar) {
					LocalVar lv = (LocalVar) sender;
					String m = lv.uniqueName();
					Type t = null;
					for (int i=0;t == null && i<margs.size();i++) {
						if (i>=types.size())
							t = methodHandler.type;
						else if (margs.get(i) instanceof RWTypedPattern && ((RWTypedPattern)margs.get(i)).var.uniqueName().equals(m))
							t = ((RWTypedPattern)margs.get(i)).type;
						else if (margs.get(i) instanceof RWVarPattern && ((RWVarPattern)margs.get(i)).var.uniqueName().equals(m))
							t = types.get(i);
					}
					if (t == null)
						throw new UtilException("Can't handle this case yet (but I think it's an error): " + m + " not in " + margs);
					if (t instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) t, lv, method, args, handler);
				}
				else if (sender instanceof ScopedVar) {
					ScopedVar vn = (ScopedVar) sender;
					PackageVar me = rw.getMe(vn.location, vn.id);
					if (me.defn instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) me.defn, vn, method, args, handler);
					else
						throw new UtilException("Can't handle this case yet");
				}
				else if (sender instanceof HandlerLambda) {
					HandlerLambda l = (HandlerLambda) sender;
					if (l.type instanceof TypeWithMethods)
						return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) l.type, l, method, args, handler);
					else if (l.type instanceof TypeOfSomethingElse) {
						NameOfThing other = ((TypeOfSomethingElse) l.type).getTypeName();
						PackageVar me = rw.getMe(l.location, other);
						if (me.defn instanceof TypeWithMethods)
							return handleMethodCase(rw, root.location, margs, types, (TypeWithMethods) me.defn, l, method, args, handler);
						else if (me.defn instanceof RWFunctionDefinition) {
							RWFunctionDefinition fd = (RWFunctionDefinition) me.defn;
							if (fd.nargs() > 0)
								errors.message(l.location(), "cannot use function " + me.id + " of arity " + fd.nargs() + " as constant");
							return new SendExpr(l.location(), l, method, args, handler);
						}
						else
							throw new UtilException("Can't handle this case yet: " + me.defn.getClass());
					} else
						throw new UtilException("What is this?" + l.type);
				} else if (sender instanceof CardMember) {
					CardMember cm = (CardMember) sender;
					Type ot = cm.type;
					while (ot instanceof InstanceType) 
						ot = ((InstanceType)ot).innerType();
					if (ot instanceof RWObjectDefn)
						return new SendExpr(((ApplyExpr)mm.expr).location(), sender, method, args, handler);
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

	protected Object convertAssignMessage(Rewriter rw, List<Object> margs, List<TypeWithName> types, RWMethodMessage mm, boolean fromHandler) {
		Locatable slot = (Locatable) mm.slot.get(0);
		InputPosition location = slot.location();
		Object intoObj;
		StringLiteral slotName;
		TypeWithName slotType;
		if (slot instanceof CardMember) {
			CardMember cm = (CardMember) slot;
			intoObj = new CardStateRef(cm.location(), fromHandler);
			final Object cd = rw.getMe(cm.location(), cm.card).defn;
			RWStructField sf;
			if (cd instanceof ObjectWithState) {
				ObjectWithState grp = (ObjectWithState) cd;
				RWStructDefn sd = grp.getState();
				sf = sd.findField(cm.var);
			} else if (cd instanceof RWStructDefn) {
				sf = ((RWStructDefn)cd).findField(cm.var);
			} else {
				errors.message(cm.location, "could not figure out how to get a state member " + cm.var);
				return null;
			}
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
			slotType = (TypeWithName) sf.type;
		} else if (slot instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda) slot;
			if (hl.type == null) {
				errors.message(slot.location(), "cannot assign to untyped handler lambda: " + hl.var);
				return null;
			}
			if (!(hl.type instanceof TypeWithName)) {
				errors.message(slot.location(), "cannot assign to handler lambda '" + hl.var + "' of type " + hl.type);
				return null;
			}
			TypeWithName hlType = (TypeWithName) hl.type;
			if (hlType.nameAsString().equals("Any")) {
				errors.message(slot.location(), "cannot assign to untyped handler lambda: " + hl.var);
				return null;
			}
			intoObj = hl;
			slotName = null;
			slotType = hlType;
		} else if (slot instanceof LocalVar) {
			LocalVar lv = (LocalVar) slot;
			if (lv.type == null) {
				errors.message(lv.varLoc, "cannot use untyped argument as assign target: " + lv.uniqueName());
				return null;
			}
			intoObj = lv;
			slotName = null;
			slotType = (TypeWithName) lv.type;
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
					errors.message(si.location(), "cannot extract member '" + si.text + "' of a non-struct: '" + slotType.nameAsString() + "'");
					return null;
				}
				RWStructDefn sd = (RWStructDefn) slotType;
				RWStructField sf = sd.findField(si.text);
				if (sf == null) {
					errors.message(si.location, "there is no field '" + si.text + "' in type " + sd);
					return null;
				}
				slotType = (TypeWithName) sf.type;
				if (slotName != null)
					intoObj = new ApplyExpr(si.location, BuiltinOperation.FIELD.at(si.location), intoObj, slotName);
				slotName = new StringLiteral(si.location, si.text);
			}
		} else if (slotName == null) {
			errors.message(slot.location(), "cannot assign directly to an object");
			return null;
		}
		return new ApplyExpr(slot.location(), rw.getMe(slot.location(), new SolidName(null, "Assign")), intoObj, slotName, new AssertTypeExpr(location, slotType, mm.expr));
	}

	private Object handleMethodCase(Rewriter rw, InputPosition location, List<Object> margs, List<TypeWithName> types, TypeWithMethods senderType, Locatable sender, StringLiteral method, List<Object> args, Object handler) {
		RWContractDecl cd = null;
		TypeWithMethods proto = senderType;
		FunctionType methodType = null;
		if (senderType instanceof RWContractDecl) {
			proto = cd = (RWContractDecl) senderType;
			if (senderType.hasMethod(method.text))
				methodType = senderType.getMethodType(method.text);
		} else if (senderType instanceof RWContractService || senderType instanceof RWContractImplements) {
			proto = cd = (RWContractDecl) rw.getMe(senderType.location(), senderType.getTypeName()).defn;
			if (proto.hasMethod(method.text))
				methodType = cd.getMethodType(method.text);
		} else if (senderType instanceof RWObjectDefn) {
			proto = senderType;
			if (senderType.hasMethod(method.text))
				methodType = senderType.getMethodType(method.text);
		}
		if (methodType == null) {
			errors.message(method.location, "there is no method '" + method.text + "' in " + proto.nameAsString());
			return null;
		}
		if (senderType instanceof RWContractImplements && !cd.checkMethodDir(method.text, ContractMethodDir.UP)) {
			errors.message(method.location, "can only call up methods on contract implementations");
			return null;
		}
		if (senderType instanceof RWContractService && !cd.checkMethodDir(method.text, ContractMethodDir.DOWN)) {
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
		return new SendExpr(location, sender, method, args, handler);
	}
}
