package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessRestrictions;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
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
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeHelpers;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.CardType;
import org.ziniki.splitter.FieldType;
import org.ziniki.splitter.NoMetaDataException;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class RepositoryResolver extends LeafAdapter implements Resolver {
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

	public RepositoryResolver(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
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
					InputPosition loc = meth.location();
					if (!meth.args().isEmpty())
						loc = meth.args().get(meth.args().size()-1).location();
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
	public void visitHandlerImplements(HandlerImplements hi, StateHolder sh) {
		scopeStack.add(0, scope);
		this.scope = hi.name();
		currentlyImplementing = hi;
	}

	@Override
	public void leaveProvides(Provides p) {
		currentlyImplementing = null;
		ContractDecl d = p.actualType();
		if (d == null) {
			if (p.implementsType().defn() != null)
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
			if (ic.implementsType().defn() != null)
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
			if (hi.implementsType().defn() != null)
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
	public void leaveMemberExpr(MemberExpr expr) {
		Expr from = expr.from;
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
		} else
			throw new NotImplementedException("cannot handle elt " + expr.from.getClass());
		UnresolvedVar fld = (UnresolvedVar) expr.fld;
		if (defn instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) defn;
			ObjectCtor ctor = od.getConstructor(fld.var);
			if (ctor == null) {
				errors.message(expr.fld.location(), "object " + od.name().uniqueName() + " does not have a ctor " + fld.var);
				return;
			}
			NameOfThing card = scope.containingCard();
			if (card == null && !((FunctionName)scope).isUnitTest()) {
				errors.message(expr.fld.location(), "object " + od.name().uniqueName() + " cannot be created outside card or object scope");
				return;
			}
			expr.bind(ctor);
		} else if (defn instanceof UnitDataDeclaration) {
			NamedType nt = ((UnitDataDeclaration) defn).ofType.defn();
			processMemberOfType(expr, nt, fld.var);
		} else if (defn instanceof TypedPattern) {
			processMemberOfType(expr, ((TypedPattern)defn).type.defn(), fld.var);
		} else if (defn instanceof StructField) {
			NamedType sft = ((StructField)defn).type.defn();
			processMemberOfType(expr, sft, fld.var);
		} else if (defn instanceof TemplateNestedField) {
			TemplateNestedField tnf = (TemplateNestedField) defn;
			if (tnf.type() == null)
				throw new CantHappenException("cannot handle TNF without type");
			processMemberOfType(expr, (NamedType) tnf.type(), fld.var);
		} else if (defn instanceof ObjectContract) {
			ObjectContract oc = (ObjectContract) defn;
			ContractDecl cd = (ContractDecl) oc.implementsType().defn();
			processMemberOfType(expr, cd, fld.var);
		} else if (defn instanceof RequiresContract) {
			RequiresContract rc = (RequiresContract) defn;
			ContractDecl cd = (ContractDecl) rc.implementsType().defn();
			processMemberOfType(expr, cd, fld.var);
		} else if (defn instanceof HandlerLambda) {
			NamedType sft = ((TypedPattern)((HandlerLambda)defn).patt).type.defn();
			processMemberOfType(expr, sft, fld.var);
		} else if (defn instanceof FunctionDefinition || defn instanceof VarPattern || defn instanceof IntroduceVar) {
			// there's nothing more we can do here unless we have a return type ... wait until we have a type 
		} else
			throw new NotImplementedException("cannot handle " + defn.getClass());
	}
	
	private void processMemberOfType(MemberExpr expr, NamedType nt, String var) {
		if (nt instanceof PolyInstance)
			nt = ((PolyInstance)nt).struct();
		if (nt instanceof StructDefn) {
			StructDefn sd = (StructDefn) nt;
			StructField sf = sd.findField(var);
			if (LoadBuiltins.event.hasCase(sd)) {
				if ("source".equals(var)) {
					expr.bind(LoadBuiltins.event); // needs to be something more precise
					return;
				}
			}
			if (sf == null) {
				errors.message(expr.fld.location(), "there is no field '" + var + "' in " + nt.name().uniqueName());
				return;
			}
			expr.bind(sf);
		} else if (nt instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) nt;
			FieldAccessor acor = od.getAccessor(var);
			if (acor != null) {
				expr.bind((RepositoryEntry) acor);
				return;
			}
			ObjectMethod om = od.getMethod(var);
			if (om != null) {
				expr.bind((RepositoryEntry) om);
				return;
			}
			if (od.state() != null) {
				StructField sf = od.state().findField(var);
				if (sf == null) {
					// If the top thing is a UDD, it can be OK and we can sort that out later although I'd like to bring that code (see MemberExpressionChecker.handleStateHolderUDD) here
//					throw new NotImplementedException("no member " + var + " in " + cd);
					return;
				}
				expr.bind(sf);
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
				expr.bind(sf);
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
				expr.bind(sf);
				return;
			}
			throw new NotImplementedException("no member " + var + " in " + cd);
		} else if (nt instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) nt;
			ObjectMethod method = hi.getMethod(var);
			if (method == null)
				throw new NotImplementedException("no method " + var + " in " + hi);
			expr.bind(method);
		} else if (nt instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) nt;
			ContractMethodDecl method = cd.getMethod(var);
			if (method == null)
				throw new NotImplementedException("no method " + var + " in " + cd);
			expr.bind((RepositoryEntry) method);
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
		if (defn == null)
			defn = find(var.location, scope, var.var);
		if (defn == null) {
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
		final RepositoryEntry defn = find(operator.location, scope, operator.op);
		if (defn == null) {
			errors.message(operator.location, "cannot resolve '" + operator.op + "'");
			return;
		}
		operator.bind(defn);
	}

	@Override
	public void visitTypeReference(TypeReference ref, boolean expectPolys) {
		String tn = ref.name();
		final RepositoryEntry defn = find(ref.location(), scope, tn);
		if (defn == null) {
			errors.message(ref.location(), "cannot resolve '" + tn + "'");
			return;
		} else if (!(defn instanceof NamedType)) {
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
						errors.message(ref.location(), "expected " + ndp + " poly vars");
						return;
					}
					ErrorMark mark = errors.mark();
					List<Type> bound = new ArrayList<>();
					for (TypeReference tr : nu) {
						visitTypeReference(tr, expectPolys);
						if (mark.hasMoreNow())
							return;
						bound.add(tr.defn());
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
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		scopeStack.add(0, scope);
		scope = cd.name();
	}
	
	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		for (TypedPattern tp : cmd.args) {
			if (tp.type.defn() instanceof ContractDecl)
				errors.message(tp.typeLocation, "method arguments may not be contracts");
		}
		if (cmd.handler != null) {
			if (!(cmd.handler.type.defn() instanceof ContractDecl))
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
			else if (((ContractDecl)cmd.handler.type.defn()).type != ContractType.HANDLER)
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
		}
		cmd.bindType();
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		currentTemplate = t;
		currentBindings = new TreeSet<>();
		TemplateName name = t.name();
		CardData webInfo = null;
		try {
			webInfo = repository.findWeb(name.baseName());
		} catch (NoMetaDataException ex) {
			// webInfo will be null and be caught below
		}
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
				if (uv.defn() instanceof StructField && ((StructField)uv.defn()).type.defn() instanceof ObjectDefn)
					object = (ObjectDefn) ((StructField)uv.defn()).type.defn();
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
		}
	}

	private Type figureTemplateValueType(Expr oe) {
		if (oe instanceof StringLiteral)
			return LoadBuiltins.string;
		else if (oe instanceof NumericLiteral)
			return LoadBuiltins.number;
		else if (oe instanceof UnresolvedVar) {
			RepositoryEntry rd = ((UnresolvedVar)oe).defn();
			if (rd instanceof StructField) {
				StructField sf = (StructField)rd;
				Type st = sf.type();
				if (st == null) // it could not be resolved
					return null;
				if (st instanceof PolyInstance) {
					PolyInstance pi = (PolyInstance)st;
					NamedType pis = pi.struct();
					if (pis.equals(LoadBuiltins.list))
						st = pi.getPolys().get(0);
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
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
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
			NamedType ty = udd.ofType.defn();
			if (ty instanceof StateHolder) {
				StateHolder st = (StateHolder) ty;
				if (st.state() == null) {
					errors.message(v.location, v.var + " does not have state");
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
		ObjectDefn od = (ObjectDefn) udd.ofType.defn();
		Template otd = od.getTemplate(r.template.name.baseName());
		if (otd == null) {
			errors.message(r.template.location(), "there is no template " + r.template.name.baseName());
			return;
		}
		r.template.bindTo(otd);
	}
	
	private void checkValidityOfUDDConstruction(UnitDataDeclaration udd) {
		NamedType defn = udd.ofType.defn();
		if (defn == null) {
			if (!errors.hasErrors())
				throw new RuntimeException("the UDD type did not get resolved");
			else
				return;
		}
		if (defn instanceof PolyInstance)
			defn = ((PolyInstance)defn).struct();
		if (defn instanceof ContractDecl) {
			if (udd.expr != null || !udd.fields.isEmpty()) {
				errors.message(udd.location(), "a contract data declaration may not be initialized");
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
	public void leaveUnitTest(UnitTestCase e) {
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

}
