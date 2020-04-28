package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.NamedType;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.CardType;

public class RepositoryResolver extends LeafAdapter implements Resolver {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final List<NameOfThing> scopeStack = new ArrayList<>();
	private NameOfThing scope;
	private Implements currentlyImplementing;
	private Template currentTemplate;

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
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		RepositoryEntry defn = find(scope, p.ctor);
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
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = find(scope, var.var);
		if (defn == null) {
			errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		var.bind(defn);
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		final RepositoryEntry defn = find(scope, operator.op);
		if (defn == null) {
			errors.message(operator.location, "cannot resolve '" + operator.op + "'");
			return;
		}
		operator.bind(defn);
	}

	@Override
	public void visitTypeReference(TypeReference var) {
		String tn = var.name();
		final RepositoryEntry defn = find(scope, tn);
		if (defn == null) {
			errors.message(var.location(), "cannot resolve '" + tn + "'");
			return;
		} else if (!(defn instanceof NamedType)) {
			errors.message(var.location(), defn.name().uniqueName() + " is not a type");
			return;
		}
		
		var.bind((NamedType) defn);
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
	}
	
	@Override
	public void visitTemplateReference(TemplateReference refersTo, boolean isFirst) {
		TemplateName name = refersTo.name;
		CardData webInfo = repository.findWeb(name.baseName());
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
		// TODO: if !isFirst, it must already be referred to
		// ALSO TODO: collect the references during traversal
		// And drive from golden tests ...
		refersTo.bindTo(webInfo);
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		System.out.println(b.slot);
		System.out.println(currentTemplate);
		System.out.println(currentTemplate.defines);
		CardData ce = currentTemplate.defines.defn();
		System.out.println(ce);
		if (!ce.hasField(b.slot)) {
			errors.message(b.slotLoc, "there is no slot " + b.slot + " in " + currentTemplate.defines.name.baseName());
		}
	}
	
	@Override
	public void leaveTemplate(Template t) {
		currentTemplate = null;
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
	public void visitUnitTestSend(UnitTestSend s) {
		scopeStack.add(0, scope);
//		RepositoryEntry defn = find(scope, s.contract.name());
//		if (defn != null)
//			this.scope = defn.name();
		// otherwise it is left as it is, which will fail, but errors will occur when the traverser vistri
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		this.scope = scopeStack.remove(0);
	}
	
	private void checkValidityOfUDDConstruction(UnitDataDeclaration udd) {
		NamedType defn = udd.ofType.defn();
		if (defn == null) {
			if (!errors.hasErrors())
				throw new RuntimeException("the UDD type did not get resolved");
			else
				return;
		}
		if (defn instanceof ContractDecl) {
			if (udd.expr != null || !udd.fields.isEmpty()) {
				errors.message(udd.location(), "a contract data declaration may not be initialized");
			}
		} else if (defn instanceof StructDefn) {
			StructDefn sd = (StructDefn) defn;
			if (udd.expr == null && udd.fields.isEmpty() && sd.argCount() != 0) {
				errors.message(udd.location(), "either an expression or at least one field assignment must be specified for " + defn.name().uniqueName());
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

	private RepositoryEntry find(NameOfThing s, String var) {
		if (s == null) {
			return repository.get(var);
		}
		final RepositoryEntry defn = repository.get(s.uniqueName() + "." + var);
		if (defn != null)
			return defn;
		else
			return find(s.container(), var);
	}

}
