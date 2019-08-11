package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;

public class RepositoryResolver extends LeafAdapter implements Resolver {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final List<NameOfThing> scopeStack = new ArrayList<>();
	private NameOfThing scope;

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
	public void leaveFunction(FunctionDefinition fn) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		RepositoryEntry defn = find(scope, var.var);
		if (defn == null) {
			errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		var.bind(defn);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		final RepositoryEntry defn = find(scope, operator.op);
		if (defn == null) {
			errors.message(operator.location, "cannot resolve '" + operator.op + "'");
			return;
		}
		operator.bind(defn);
	}

	@Override
	public void visitTypeReference(TypeReference var) {
		final RepositoryEntry defn = find(scope, var.name());
		if (defn == null) {
			errors.message(var.location(), "cannot resolve '" + var.name() + "'");
			return;
		}
		var.bind(defn);
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		scope = cd.nameAsName();
	}
	
	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		for (Object a : cmd.args) {
			if (a instanceof TypedPattern) {
				TypedPattern p = (TypedPattern) a;
				visitTypeReference(p.type);
			}
				
		}
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		this.scope = null;
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		this.scope = e.name;
	}
	
	@Override
	public void leaveUnitTest(UnitTestCase e) {
		this.scope = null;
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
