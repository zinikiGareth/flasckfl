package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.ModuleExtensible;
import org.flasck.flas.compiler.modules.TraversalProcessor;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessRestrictions;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleTypeReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.parsedForm.st.GotoRoute;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryEntry.ValidContexts;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Tuple;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeHelpers;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.CardType;
import org.ziniki.splitter.FieldType;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class RepositoryResolver extends LeafAdapter implements Resolver, ModuleExtensible {
	private final static Logger logger = LoggerFactory.getLogger("Resolver");
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final List<NameOfThing> scopeStack = new ArrayList<>();
	private NameOfThing scope;
	private Implements currentlyImplementing;
	private Template currentTemplate;
	private UnresolvedVar currShoveExpr;
	private Set<String> currentBindings;
	private NestingChain templateNestingChain;
	private RepositoryEntry inside;
	private boolean assigning;
	private final Iterable<TraversalProcessor> modules;
	private boolean lookDownwards;
	private ApplicationRoutingResolver applicationRouting;

	public RepositoryResolver(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
		modules = ServiceLoader.load(TraversalProcessor.class);
	}

	@Override
	public void resolveAll() {
		repository.traverse(this);
	}

	public void currentScope(NameOfThing scope) {
		this.scope = scope;
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		scopeStack.add(0, scope);
		this.scope = fn.name();
		this.inside = fn;
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		scopeStack.add(0, scope);
		this.scope = fi.name();
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		scopeStack.add(0, scope);
		this.scope = meth.name();
		this.inside = meth;
		if (currentlyImplementing != null && currentlyImplementing.actualType() != null) {
			ContractDecl cd = currentlyImplementing.actualType();
			ContractMethodDecl cm = cd.getMethod(meth.name().name);
			if (cm != null) {
				if (meth.argCount() < cm.args.size()) {
					InputPosition loc = meth.location().locAtEnd();
					if (!meth.args().isEmpty())
						loc = meth.args().get(meth.args().size()-1).location().locAtEnd();
					errors.message(loc, "insufficient arguments provided to contract method '" + meth.name().name + "'");
				} else if (meth.argCount() > cm.args.size()) {
					InputPosition loc = meth.args().get(cm.args.size()).location();
					errors.message(loc, "excess arguments provided to contract method '" + meth.name().name + "'");
				}
				meth.bindFromContract(cm);
			} else
				errors.message(meth.location(), "there is no method '" + meth.name().name + "' on '" + cd.name().uniqueName() + "'");
		}
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor ctor) {
		scopeStack.add(0, scope);
		this.scope = ctor.name();
		this.inside = ctor;
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		RepositoryEntry defn = find(p.location(), scope, p.ctor);
		if (defn == null) {
			errors.message(p.location, "cannot find type '" + p.ctor + "'");
			return;
		} else if (!(defn instanceof StructDefn)) {
			errors.message(p.location, p.ctor + " is not a struct defn");
			return;
		} else
			p.bind((StructDefn) defn);
	}
	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveObjectCtor(ObjectCtor oa) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void visitTuple(TupleAssignment ta) {
		scopeStack.add(0, scope);
		this.scope = ta.scopePackage();
	}

	@Override
	public void leaveTuple(TupleAssignment e) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitStructDefn(StructDefn sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		scopeStack.add(0, scope);
		this.scope = ud.name();
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		scopeStack.add(0, scope);
		this.scope = cd.name();
	}
	
	@Override
	public void visitAgentDefn(AgentDefinition sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitServiceDefn(ServiceDefinition sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitImplements(ImplementsContract ic) {
		scopeStack.add(0, scope);
		this.scope = ic.name();
		currentlyImplementing = ic;
	}
	
	@Override
	public void visitProvides(Provides p) {
		scopeStack.add(0, scope);
		this.scope = p.name();
		currentlyImplementing = p;
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi) {
		scopeStack.add(0, scope);
		this.scope = hi.name();
		currentlyImplementing = hi;
	}

	@Override
	public void leaveProvides(Provides p) {
		currentlyImplementing = null;
		ContractDecl d = p.actualType();
		if (d == null) {
			if (p.implementsType().namedDefn() != null)
				errors.message(p.implementsType().location(), p.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.SERVICE) {
			errors.message(p.implementsType().location(), "cannot provide non-service contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : p.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(p.location(), "must implement required contract method " + cmd.name.uniqueName());
		}
	}

	@Override
	public void leaveImplements(ImplementsContract ic) {
		currentlyImplementing = null;
		ContractDecl d = ic.actualType();
		if (d == null) {
			if (ic.implementsType().namedDefn() != null)
				errors.message(ic.implementsType().location(), ic.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.CONTRACT) {
			errors.message(ic.implementsType().location(), "cannot implement " + d.type.toString().toLowerCase() + " contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : ic.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(ic.location(), "must implement required contract method " + cmd.name.uniqueName());
		}
	}
	
	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
		currentlyImplementing = null;
		ContractDecl d = hi.actualType();
		if (d == null) {
			if (hi.implementsType().namedDefn() != null)
				errors.message(hi.implementsType().location(), hi.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.HANDLER) {
			errors.message(hi.implementsType().location(), "handler cannot implement " + (d.type == ContractType.SERVICE?"service":"non-handler") + " contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : hi.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(hi.location(), "must implement required contract method " + cmd.name.uniqueName());
		}

	}

	@Override
	public void leaveStructDefn(StructDefn sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void leaveObjectDefn(ObjectDefn sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveCardDefn(CardDefinition cd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveAgentDefn(AgentDefinition sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveServiceDefn(ServiceDefinition sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		String s = expr.asName();
		if (s != null) {
			RepositoryEntry e = repository.get(s);
			if (e != null) {
				expr.bind(e, true);
				return true;
			}
		}
		return false;
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr, boolean done) {
		// don't bother if we've already dealt with it ...
		if (expr.boundEarly())
			return;
		
		Expr from = expr.from;
		String var;
		if (expr.fld instanceof UnresolvedVar) {
			UnresolvedVar fld = (UnresolvedVar) expr.fld;
			var = fld.var;
		} else
			return;
		RepositoryEntry defn;
		if (from instanceof MemberExpr) {
			defn = expr.defn();
			if (defn == null) // if we couldn't figure it out before ...
				return;
		} else if (expr.from instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) expr.from;
			defn = uv.defn();
			if (defn == null) // some kind of error
				return;
		} else if (expr.from instanceof TypeReference) {
			TypeReference uv = (TypeReference) expr.from;
			defn = (RepositoryEntry) uv.namedDefn();
			if (defn == null) // some kind of error
				return;
		} else if (expr.from instanceof ApplyExpr) {
			// this is hard to say the least ...
			return;
		} else if (expr.from instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr.from;
			NamedType nt = ce.type.namedDefn();
			if (nt == null) // some kind of error
				return;
			processMemberOfType(expr, nt, var);
			return;
		} else
			throw new NotImplementedException("cannot handle elt " + expr.from.getClass());
		if (defn instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) defn;
			ObjectActionHandler ctor = od.getConstructor(var);
			if (ctor == null) {
				errors.message(expr.fld.location(), "object " + od.name().uniqueName() + " does not have a ctor " + var);
				return;
			}
			NameOfThing card = scope.containingCard();
			if (card == null && !(scope instanceof UnitTestName) && !((FunctionName)scope).isUnitTest()) {
				errors.message(expr.fld.location(), "object " + od.name().uniqueName() + " cannot be created outside card, object or test scope");
				return;
			}
			expr.bind(ctor, false);
		} else if (defn instanceof UnitDataDeclaration) {
			NamedType nt = ((UnitDataDeclaration) defn).ofType.namedDefn();
			processMemberOfType(expr, nt, var);
		} else if (defn instanceof TypedPattern) {
			processMemberOfType(expr, ((TypedPattern)defn).type.namedDefn(), var);
		} else if (defn instanceof StructField) {
			NamedType sft = ((StructField)defn).type.namedDefn();
			processMemberOfType(expr, sft, var);
		} else if (defn instanceof TemplateNestedField) {
			TemplateNestedField tnf = (TemplateNestedField) defn;
			if (tnf.type() == null)
				throw new CantHappenException("cannot handle TNF without type");
			processMemberOfType(expr, (NamedType) tnf.type(), var);
		} else if (defn instanceof ObjectContract) {
			ObjectContract oc = (ObjectContract) defn;
			ContractDecl cd = (ContractDecl) oc.implementsType().namedDefn();
			processMemberOfType(expr, cd, var);
		} else if (defn instanceof RequiresContract) {
			RequiresContract rc = (RequiresContract) defn;
			ContractDecl cd = (ContractDecl) rc.implementsType().namedDefn();
			processMemberOfType(expr, cd, var);
		} else if (defn instanceof HandlerLambda) {
			NamedType sft = ((TypedPattern)((HandlerLambda)defn).patt).type.namedDefn();
			processMemberOfType(expr, sft, var);
		} else if (defn instanceof IntroduceVar) {
			IntroduceVar iv = (IntroduceVar) defn;
			if (iv.introducedAs() != null)
				processMemberOfType(expr, (NamedType) iv.introducedAs(), var);
		} else if (defn instanceof FunctionDefinition || defn instanceof VarPattern) {
			// there's nothing more we can do here unless we have a return type ... wait until we have a type 
		} else
			throw new NotImplementedException("cannot handle " + defn.getClass());
	}
	
	private void processMemberOfType(MemberExpr expr, NamedType nt, String var) {
		if (nt == null) {
			// no type was found ... this should have been an error at some point
			if (!errors.hasErrors())
				throw new CantHappenException("this should have been an error somewhere: expr = " + expr);
			return;
		}
		if (nt instanceof PolyInstance)
			nt = ((PolyInstance)nt).struct();
		if (nt instanceof StructDefn) {
			StructDefn sd = (StructDefn) nt;
			StructField sf = sd.findField(var);
			if (LoadBuiltins.event.hasCase(sd)) {
				if ("source".equals(var)) {
					expr.bind(LoadBuiltins.event, false); // needs to be something more precise
					return;
				}
			}
			if (sf == null) {
				errors.message(expr.fld.location(), "there is no field '" + var + "' in " + nt.name().uniqueName());
				return;
			}
			expr.bind(sf, false);
		} else if (nt instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) nt;
			if (od.getConstructor(var) != null) {
				errors.message(expr.fld.location(), "cannot call constructor on an instance; use type name");
				return;
			}
			FieldAccessor acor = od.getAccessor(var);
			if (acor != null) {
				if (((RepositoryEntry)acor).validContexts() == ValidContexts.TESTS) { // && isMain
					errors.message(expr.fld.location(), ((RepositoryEntry)acor).name().uniqueName() + " may only be used in tests");
					return;
				}
				expr.bind((RepositoryEntry) acor, false);
				return;
			}
			ObjectMethod om = od.getMethod(var);
			if (om != null) {
				expr.bind((RepositoryEntry) om, false);
				return;
			}
			if (od.state() != null) {
				StructField sf = od.state().findField(var);
				if (sf == null) {
					// If the top thing is a UDD, it can be OK and we can sort that out later although I'd like to bring that code (see MemberExpressionChecker.handleStateHolderUDD) here
//					throw new NotImplementedException("no member " + var + " in " + cd);
					return;
				}
				expr.bind(sf, false);
				return;
			}
			errors.message(expr.fld.location(), "object " + od.name().uniqueName() + " does not have a method, acor or member " + var);
		} else if (nt instanceof CardDefinition) {
			CardDefinition cd = (CardDefinition) nt;
			if (cd.state() != null) {
				StructField sf = cd.state().findField(var);
				if (sf == null) {
					// If the top thing is a UDD, it can be OK and we can sort that out later although I'd like to bring that code (see MemberExpressionChecker.handleStateHolderUDD) here
//					throw new NotImplementedException("no member " + var + " in " + cd);
					return;
				}
				expr.bind(sf, false);
				return;
			}
			throw new NotImplementedException("no member " + var + " in " + cd);
		} else if (nt instanceof AgentDefinition) {
			AgentDefinition cd = (AgentDefinition) nt;
			if (cd.state() != null) {
				StructField sf = cd.state().findField(var);
				if (sf == null) {
					// If the top thing is a UDD, it can be OK and we can sort that out later although I'd like to bring that code (see MemberExpressionChecker.handleStateHolderUDD) here
//					throw new NotImplementedException("no member " + var + " in " + cd);
					return;
				}
				expr.bind(sf, false);
				return;
			}
			throw new NotImplementedException("no member " + var + " in " + cd);
		} else if (nt instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) nt;
			ObjectMethod method = hi.getMethod(var);
			if (method == null)
				throw new NotImplementedException("no method " + var + " in " + hi);
			expr.bind(method, false);
		} else if (nt instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) nt;
			ContractMethodDecl method = cd.getMethod(var);
			if (method == null) {
				errors.message(expr.location, "there is no method '" + var + "' on '" + cd.name().uniqueName() + "'");
			} else
				expr.bind((RepositoryEntry) method, false);
		} else if (nt instanceof ApplicationRouting) {
			ApplicationRouting ar = (ApplicationRouting) nt;
			RepositoryEntry card = ar.getCard(var);
			if (card == null) {
				errors.message(expr.fld.location(), "there is no routing entry for " + var);
			} else {
				expr.bind(card, false);
			}
		} else if (nt instanceof UnionTypeDefn) {
			errors.message(expr.fld.location(), "cannot access members of unions");
		} else
			throw new NotImplementedException("cannot handle member of type " + nt.getClass());
	}

	@Override
	public void visitAssignSlot(Expr slot) {
		assigning = true;
	}
	
	@Override
	public void leaveAssignMessage(AssignMessage msg) {
		assigning = false;
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = null;
		if (templateNestingChain != null) {
			defn = templateNestingChain.resolve(this, var);
		}
		if (applicationRouting != null) {
			defn = applicationRouting.resolve(this, var);
		}
		if (defn == null)
			defn = find(var.location, scope, var.var);
		if (defn == null) {
			if (templateNestingChain != null && templateNestingChain.isEmpty())
				errors.message(var.location, "there is no bound chain for '" + this.currentTemplate.name().baseName() + "' when resolving '" + var.var + "'");
			else
				errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		if (inside instanceof ObjectCtor && !assigning && defn instanceof StructField) {
			StructField sf = (StructField) defn;
			if (sf.container instanceof StateDefinition && sf.init == null) {
				errors.message(var.location(), "cannot use uninitialized field '" + var.var + "' in constructor " + inside.name().uniqueName());
				return;
			}
		}
		var.bind(defn);
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
 		if (operator.op.equals("-") && nargs == 1) {
 			operator.bind(LoadBuiltins.unaryMinus);
 			return;
 		}
 		
		final RepositoryEntry defn = find(operator.location, scope, operator.op);
		if (defn == null) {
			errors.message(operator.location, "cannot resolve '" + operator.op + "'");
			return;
		}
		operator.bind(defn);
	}

	@Override
	public void visitTypeReference(TypeReference ref, boolean expectPolys, int exprNargs) {
		if (ref instanceof FunctionTypeReference) {
			handleFunctionTypeReference((FunctionTypeReference) ref, expectPolys, exprNargs);
			return;
		} else if (ref instanceof TupleTypeReference) {
			handleTupleTypeReference((TupleTypeReference) ref, expectPolys, exprNargs);
			return;
		}
		String tn = ref.name();
		RepositoryEntry defn = find(ref.location(), scope, tn);
		if (defn == null) {
			if (expectPolys && PolyTypeToken.validate(tn) && inside instanceof FunctionDefinition) {
				defn = ((FunctionDefinition)inside).allocatePoly(ref.location(), tn);
			} else if (lookDownwards && (defn = findInside(ref.location(), tn)) != null) {
				;
			} else {
				errors.message(ref.location(), "cannot resolve '" + tn + "'");
				return;
			}
		}
		
		if (ref.isDynamic()) {
			throw new NotImplementedException();
		}
		
		if (!(defn instanceof NamedType)) {
			errors.message(ref.location(), defn.name().uniqueName() + " is not a type");
			return;
		}
		
		if (expectPolys) {
			NamedType nt = (NamedType) defn;
			if (nt instanceof PolyHolder) {
				PolyHolder ph = (PolyHolder)nt;
				if (ph.hasPolys()) {
					List<PolyType> nd = ph.polys();
					List<TypeReference> nu = ref.polys();
					int ndp = nd.size();
					if (ndp != nu.size()) {
						errors.message(ref.location().locAtEnd(), "expected " + ndp + " poly vars");
						return;
					}
					ErrorMark mark = errors.mark();
					List<Type> bound = new ArrayList<>();
					for (TypeReference tr : nu) {
						visitTypeReference(tr, expectPolys, exprNargs);
						if (mark.hasMoreNow())
							return;
						Type arg = tr.defn();
						if (arg == null)
							throw new CantHappenException("have null type in resolved reference");
						if (tn.equals("Crobag") && !TypeHelpers.isEntity(arg)) {
							if (arg != null)
								errors.message(tr.location(), "a Crobag can only contain entities, not " + arg.signature());
							return;
						}
						bound.add(arg);
					}
					// it needs to bind to a polyinstance
					ref.bind(new PolyInstance(ref.location(), (NamedType) defn, bound));
					return;
				}
			}
		} else if (ref.hasPolys()) {
			errors.message(ref.location(), "poly vars are not required or allowed here");
		}
		
		ref.bind((NamedType) defn);
	}

	private void handleFunctionTypeReference(FunctionTypeReference ref, boolean expectPolys, int exprNargs) {
		List<Type> boundTo = new ArrayList<>();
		for (TypeReference k : ref.args) {
			visitTypeReference(k, expectPolys, exprNargs);
			boundTo.add(k.namedDefn());
		}
		ref.bind(new Apply(boundTo));
	}

	private void handleTupleTypeReference(TupleTypeReference ref, boolean expectPolys, int exprNargs) {
		List<Type> boundTo = new ArrayList<>();
		Tuple tt = new Tuple(ref.location(), ref.members.size());
		for (TypeReference k : ref.members) {
			visitTypeReference(k, expectPolys, exprNargs);
			boundTo.add(k.defn());
		}
		ref.bind(new PolyInstance(ref.location(), tt, boundTo));
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		scopeStack.add(0, scope);
		scope = cd.name();
	}
	
	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		boolean dobind = true;
		for (TypedPattern tp : cmd.args) {
			if (tp.type.namedDefn() instanceof ContractDecl) {
				errors.message(tp.typeLocation, "method arguments may not be contracts");
				dobind = false;
			}
		}
		if (cmd.handler != null) {
			if (!(cmd.handler.type.namedDefn() instanceof ContractDecl)) {
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
				dobind = false;
			} else if (((ContractDecl)cmd.handler.type.namedDefn()).type != ContractType.HANDLER) {
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
				dobind = false;
			}
		}
		if (dobind)
			cmd.bindType();
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitApplicationRouting(ApplicationRouting e) {
		scopeStack.add(0, scope);
		scope = e.packageName();
		applicationRouting = new ApplicationRoutingResolver(errors, e);
	}
	
	@Override
	public void visitSubRouting(SubRouting r) {
		applicationRouting.nest();
		if (r.path.startsWith("{") && r.path.endsWith("}"))
			applicationRouting.parameter(r.location(), r.path.substring(1, r.path.length()-1));
	}
	
	@Override
	public void leaveCardAssignment(CardBinding card) {
		applicationRouting.leaveCardAssignment(card);
	}
	
	@Override
	public void leaveSubRouting(SubRouting r) {
		applicationRouting.unnest();
	}
	
	@Override
	public void leaveApplicationRouting(ApplicationRouting e) {
		this.scope = scopeStack.remove(0);
		applicationRouting = null;
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		currentTemplate = t;
		currentBindings = new TreeSet<>();
		TemplateName name = t.name();
		CardData webInfo = null;
		webInfo = repository.findWeb(name.baseName());
		if (webInfo == null) {
			errors.message(name.location(), "there is no web template defined for " + name.baseName());
			return;
		}
		if (isFirst && webInfo.type() != CardType.CARD) {
			errors.message(name.location(), "first web template must be a card template, not item " + name.baseName());
			return;
		} else if (!isFirst && webInfo.type() != CardType.ITEM) {
			errors.message(name.location(), "secondary web templates must be item templates, not card " + name.baseName());
			return;
		}
		t.bindWebInfo(webInfo);
		inside = t;
	}
	
	@Override
	public void afterTemplateChainTypes(Template t) {
		if (t.nestingChain() != null) {
			// we want to add the item type onto the resolution chain
			// Note that it's not a strict error for it not to be there, but if it's not, you will get undefined errors if you assume it is
			// Note that this really should be a chain:
			// for templates nested inside templates, each one is in the list
			// it should also allow a var
			// basically this type is not good enough, but get there through tests
			templateNestingChain = t.nestingChain();
			t.nestingChain().resolvedTypes(errors);
		}
	}

	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		CardData ce = currentTemplate.webinfo();
		if (ce == null) { // an undefined template should already have been reported ...
			return;
		}
		String slot = b.assignsTo.text;
		InputPosition slotLoc = b.assignsTo.location();
		if (!ce.hasField(slot)) {
			errors.message(slotLoc, "there is no slot " + slot + " in " + currentTemplate.name().baseName());
			return;
		}
		if (currentBindings.contains(slot)) {
			errors.message(slotLoc, "cannot bind to " + slot + " multiple times");
			return;
		}
		currentBindings.add(slot);
		FieldType type = ce.get(slot);
		switch (type) {
		case STYLE:
			if (b.doesAssignment())
				errors.message(slotLoc, "style field cannot be assigned to");
			break;
		case CONTENT:
		case IMAGE:
		case LINK:
			if (!b.doesAssignment())
				errors.message(slotLoc, "content field must be assigned to");
			break;
		case CONTAINER:
			if (!b.doesAssignment())
				errors.message(slotLoc, "container field must be assigned to");
			break;
		case PUNNET:
			if (!b.doesAssignment())
				errors.message(slotLoc, "container field must be assigned to");
			break;
		case CARD:
		case ITEM:
			errors.message(slotLoc, "cannot add bindings for field of type " + type.toString().toLowerCase());
			break;
		}
		b.assignsTo.fieldType(type);
	}
	
	@Override
	public void visitTemplateEvent(TemplateEvent te) {
		RepositoryEntry defn = find(te.location(), scope, te.handler);
		boolean isEH = false;
		if (defn == null) {
			errors.message(te.location(), "cannot resolve '" + te.handler + "'");
			return;
		}
		if (defn instanceof ObjectMethod) {
			ObjectMethod om = (ObjectMethod) defn;
			if (om.isEvent()) {
				isEH = true;
				te.bindHandler(om);
			}
		}
		if (!isEH) {
			errors.message(te.location(), defn.name().uniqueName() + " is not an event handler");
			return;
		}
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		if (option.sendsTo != null) {
			// consider that it might be an object sending to an object template
			ObjectDefn object = null;
			Expr oe = option.expr;
			if (oe instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) oe;
				if (uv.defn() instanceof StructField && ((StructField)uv.defn()).type.namedDefn() instanceof ObjectDefn)
					object = (ObjectDefn) ((StructField)uv.defn()).type.namedDefn();
			}
			String tname = option.sendsTo.name.baseName();
			if (object != null) {
				Template otd = object.getTemplate(tname);
				if (otd == null) {
					errors.message(option.sendsTo.location(), "template " + tname + " is not defined for object " + object.name().baseName());
				} else if (otd.nestingChain() != null) {
					errors.message(option.sendsTo.location(), "cannot send to internal template " + tname + " for " + object.name().baseName());
				} else
					option.sendsTo.bindTo(otd);
			} else {
				RepositoryEntry defn = find(option.sendsTo.location(), scope, tname);
				if (defn == null)
					errors.message(option.sendsTo.location(), "template " + tname + " is not defined");
				else if (!(defn instanceof Template))
					errors.message(option.sendsTo.location(), "cannot send to " + tname + " which is not a template");
				else {
					Template template = (Template)defn;
					option.sendsTo.bindTo(template);
					Type ty = figureTemplateValueType(oe);
					if (ty != null)
						((Template)template).canUse(ty);
				}
			}
		} else {
			// It might be a list or crobag.  I feel we should be able to do something with this, but I'm not sure what.
			// To be clear, it feels intuitive that you should be able to specify a field that's a list and go and find a template for that
			// And we do this very easily if it says what type it is, but we don't seem to have what it takes to infer it just from the fields that it uses.
			// And I feel in 90% of cases we should be able to.
			
			Type ofType = isListish(option.expr);
			// My predicted logic runs like this:
			//   Here we tell all other templates that don't already have template chains that "ofType" is something they might want to consider.
			//   They may end up with a lot of these.
			//   In resolve, if they can't figure it out, then they look at all the entries in "to consider"
			//     They mark each that matches (may be more than one)
			//     If none match, report the error as we currently do
			//   On leaving the whole of template processing code, go back and look at all the things that "considered"
			//     If they have 1 entry, assign that as the type
			//     If they have > 1, it's ambiguous
			//     0 => either there was a resolve error earlier or the template doesn't use any values (empty or constant)
			//   It's also an error to not conclude that exactly one template matches from here ...
			logger.info("I feel we should be able to do something with the fact that we are filling a container of " + ofType);
		}
	}

	// TODO: there are undoubtedly other valid cases here, but I remind you that we
	// are RIDICULOUSLY early in the process and thus failure is always an option ... they can specify a type by hand
	private Type isListish(Expr expr) {
		Object defn;
		if (expr instanceof UnresolvedVar)
			defn = ((UnresolvedVar)expr).defn();
		else
			return null;
		if (defn == null)
			return null;
		Type t;
		if (defn instanceof StructField) {
			t = ((StructField)defn).type();
		} else
			return null;
		if (TypeHelpers.isListLike(t))
			return TypeHelpers.extractListPoly(t);
		else
			return null;
	}

	private Type figureTemplateValueType(Expr oe) {
		if (oe instanceof StringLiteral)
			return LoadBuiltins.string;
		else if (oe instanceof NumericLiteral)
			return LoadBuiltins.number;
		else if (oe instanceof UnresolvedVar) {
			RepositoryEntry rd = ((UnresolvedVar)oe).defn();
			if (rd == null) {
				throw new CantHappenException("unbound var: " + oe);
			} else if (rd instanceof StructField) {
				StructField sf = (StructField)rd;
				Type st = sf.type();
				if (st == null) // it could not be resolved
					return null;
				if (st instanceof PolyInstance) {
					PolyInstance pi = (PolyInstance)st;
					NamedType pis = pi.struct();
					if (pis.equals(LoadBuiltins.list) || pis.equals(LoadBuiltins.crobag))
						st = pi.polys().get(0);
				}
				if (st instanceof StructDefn)
					return st;
				else if (st instanceof Primitive)
					return st;
				else {// TODO: there may be other cases
					errors.message(oe.location(), "expected a struct value, not " + st.signature());
					return null;
				}
			} else if (rd instanceof TemplateNestedField) {
				TemplateNestedField tnf = (TemplateNestedField) rd;
				Type ty = tnf.type();
				if (ty == null) {
					errors.message(oe.location(), "cannot infer types here; explicitly type chained element " + ((UnresolvedVar)oe).var);
				} else if (TypeHelpers.isList(ty)) {
					ty = TypeHelpers.extractListPoly(ty);
				}
				return ty;	 
			} else
				throw new NotImplementedException("not handling " + rd.getClass());
		} else if (oe instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) oe;
			if (me.from instanceof UnresolvedVar) {
				Type ty = figureTemplateValueType(me.from);
				if (ty instanceof StructDefn) {
					StructDefn sd = (StructDefn) ty;
					StructField fld = sd.findField(((UnresolvedVar)me.fld).var);
					if (fld == null) {
						errors.message(me.fld.location(), "no field " + ((UnresolvedVar)me.fld).var);
						return null;
					}
					return fld.type();
				} else {
					errors.message(oe.location(), "insufficient information to deduce type of expression");
					return null;
				}
			}
			return null;
		} else {
			// It is true that there is insufficient information to deduce the type of the expression,
			// but that is no reason not to proceed.
//			errors.message(oe.location(), "insufficient information to deduce type of expression");
			return null;
		}
	}

	@Override
	public void leaveTemplate(Template t) {
		currentTemplate = null;
		templateNestingChain = null;
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		scopeStack.add(0, scope);
		this.scope = e.name;
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		scopeStack.add(0, scope);
		this.scope = udd.name;
		this.lookDownwards = true;
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		this.lookDownwards = false;
		this.scope = scopeStack.remove(0);
		checkValidityOfUDDConstruction(udd);
	}
	
	@Override
	public void visitUnitTestShove(UnitTestShove s) {
		currShoveExpr = null;
	}
	
	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		if (currShoveExpr == null) {
			visitUnresolvedVar(v, 0);
			if (v.defn() == null) {
				// v itself was not resolved
				// an error probably occurred
				return;
			}
			currShoveExpr = v;
		} else if (currShoveExpr.defn() instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) currShoveExpr.defn();
			NamedType ty = udd.ofType.namedDefn();
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct();
			if (ty instanceof StateHolder) {
				StateHolder st = (StateHolder) ty;
				if (st.state() == null) {
					errors.message(v.location, ty + " does not have state");
					return;
				}
				StructField f = st.state().findField(v.var);
				if (f == null) {
					errors.message(v.location, "there is no field " + v.var + " in " + ty.name().uniqueName());
					return;
				}
				v.bind(f);
				currShoveExpr = v;
			} else if (ty instanceof StructDefn) {
				StructDefn sd = (StructDefn) ty;
				StructField f = sd.findField(v.var);
				if (f == null) {
					errors.message(v.location, "there is no field " + v.var + " in " + ty.name().uniqueName());
					return;
				}
				v.bind(f);
				currShoveExpr = v;
			} else {
				errors.message(v.location, "cannot shove into " + v.var);
			}
		} else {
			// I'm sure there are some legitimate cases here, but we should probably also recognize that others are sensible compile-time errors
			throw new NotImplementedException("Cannot handle shove of var " + currShoveExpr.defn());
		}
	}
	@Override
	public void visitUnitTestSend(UnitTestSend s) {
		scopeStack.add(0, scope);
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void leaveUnitTestRender(UnitTestRender r) {
		UnitDataDeclaration udd = (UnitDataDeclaration) r.card.defn();
		ObjectDefn od = (ObjectDefn) udd.ofType.namedDefn();
		Template otd = od.getTemplate(r.template.name.baseName());
		if (otd == null) {
			errors.message(r.template.location(), "there is no template " + r.template.name.baseName());
			return;
		}
		r.template.bindTo(otd);
	}

	@Override
	public void visitGotoRoute(GotoRoute gr) {
		if (gr.iv == null)
			return;
		
		ApplicationRouting ar = repository.get(scope.packageName().uniqueName() + "_Routing");
		if (ar != null) {
			gr.iv.bindType(ar);
		} else {
			errors.message(gr.iv.location, "there is no routing table for " + scope.packageName().uniqueName());
		}
	}
	
	private void checkValidityOfUDDConstruction(UnitDataDeclaration udd) {
		NamedType defn = udd.ofType.namedDefn();
		if (defn == null) {
			if (!errors.hasErrors())
				throw new RuntimeException("the UDD type did not get resolved");
			else
				return;
		}
		if (defn instanceof PolyInstance)
			defn = ((PolyInstance)defn).struct();
		if (defn instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) defn;
			if (udd.expr != null && cd.type != ContractType.HANDLER) {
				errors.message(udd.location(), "a contract data declaration may not be initialized");
			}
			if (!udd.fields.isEmpty()) {
				errors.message(udd.location(), "a contract data declaration does not have fields to initialize");
			}
		} else if (defn instanceof StructDefn) {
			StructDefn sd = (StructDefn) defn;
			if (udd.expr == null && udd.fields.isEmpty() && sd.argCount() != 0) {
				errors.message(udd.location(), "either an expression or at least one field assignment must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof UnionTypeDefn) {
			if (udd.expr == null) {
				errors.message(udd.location(), "an expression must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) defn;
			if (udd.expr == null && udd.fields.isEmpty() && hi.argCount() != 0) {
				errors.message(udd.name.location, "an expression must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof ObjectDefn) {
			if (udd.expr == null) {
				errors.message(udd.name.location, "an expression must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof CardDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "cards may not be initialized");
			}
		} else if (defn instanceof AgentDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "agents may not be initialized");
			}
		} else if (defn instanceof ServiceDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "services may not be initialized");
			}
		} else
			throw new RuntimeException("udd not handled: " + defn.getClass());
	}
	
	@Override
	public void leaveUnitTest(TestStepHolder e) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitSystemTestStage(SystemTestStage e) {
		scopeStack.add(0, scope);
		this.scope = e.name;
	}

	@Override
	public void leaveSystemTestStage(SystemTestStage e) {
		this.scope = scopeStack.remove(0);
	}


	
	private RepositoryEntry find(InputPosition pos, NameOfThing s, String var) {
		RepositoryEntry ret = recfind(s, var);
		if (ret == null)
			return ret;
		
		if (ret instanceof AccessRestrictions) {
			((AccessRestrictions)ret).check(errors, pos, s);
		}
		return ret;
	}

	private RepositoryEntry recfind(NameOfThing s, String var) {
		if (s == null) {
			return repository.get(var);
		}
		String name = s.uniqueName() + "." + var;
		final RepositoryEntry defn = repository.get(name);
		if (defn != null)
			return defn;
		else
			return recfind(s.container(), var);
	}

	// Try and find an inner nested definition IF we are in a privileged context that can break scoping rules
	private RepositoryEntry findInside(InputPosition loc, String tn) {
		String scope = this.scope.packageName().uniqueName();
		return repository.findNested(errors, loc, scope, tn);
	}

	@Override
	public <T extends TraversalProcessor> T forModule(Class<T> extension, Class<? extends RepositoryVisitor> phase) {
		for (TraversalProcessor tp : modules) {
			if (tp.is(extension) && phase.isInstance(this))
				return extension.cast(tp);
		}
		return null;
	}

	public void pushScope(NameOfThing name) {
		this.scopeStack.add(0, scope);
		scope = name;
	}

	public void popScope() {
		this.scopeStack.remove(0);
	}
}
