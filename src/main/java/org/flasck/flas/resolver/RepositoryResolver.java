package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
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

public class RepositoryResolver extends LeafAdapter implements Resolver {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final List<NameOfThing> scopeStack = new ArrayList<>();
	private NameOfThing scope;
	private Implements currentlyImplementing;

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
	public void visitAgentDefn(AgentDefinition sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitImplements(ImplementsContract ic) {
		currentlyImplementing = ic;
	}
	
	@Override
	public void visitProvides(Provides p) {
		currentlyImplementing = p;
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi, StateHolder sh) {
		currentlyImplementing = hi;
	}

	@Override
	public void leaveProvides(Provides p) {
		ContractDecl d = p.actualType();
		if (d == null) {
			if (p.implementsType().defn() != null)
				errors.message(p.implementsType().location(), p.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.SERVICE)
			errors.message(p.implementsType().location(), "cannot provide non-service contract");
		currentlyImplementing = null;
	}

	@Override
	public void leaveImplements(ImplementsContract ic) {
		ContractDecl d = ic.actualType();
		if (d == null) {
			if (ic.implementsType().defn() != null)
				errors.message(ic.implementsType().location(), ic.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.CONTRACT)
			errors.message(ic.implementsType().location(), "cannot implement " + d.type.toString().toLowerCase() + " contract");
		currentlyImplementing = null;
	}
	
	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
		ContractDecl d = hi.actualType();
		if (d == null) {
			if (hi.implementsType().defn() != null)
				errors.message(hi.implementsType().location(), hi.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.HANDLER)
			errors.message(hi.implementsType().location(), "handler cannot implement " + (d.type == ContractType.SERVICE?"service":"non-handler") + " contract");
		currentlyImplementing = null;
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
	public void leaveAgentDefn(AgentDefinition sd) {
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
		} else if (defn instanceof ObjectDefn) {
			// I actually think all the combinations are OK
			// nothing - create the default object
			// assign - copy from another object (if it's the same type - should we be checking that or do we need to wait for typecheck?)
			// fields - create the default object, then update
			// assign + fields - copy from another object & then update fields
		} else if (defn instanceof AgentDefinition) {
			// I've forgotten what this is all about, but write some tests and do something ...
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
