package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class TypeChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private ObjectActionHandler currentMethod;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		new ObjectDefnChecker(errors, repository, sv, obj);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		sv.push(new FunctionChecker(errors, sv, new FunctionGroupTCState(repository, new DependencyGroup()), meth));
		this.currentMethod = meth;
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		sv.push(new FunctionChecker(errors, sv, new FunctionGroupTCState(repository, new DependencyGroup()), meth));
		this.currentMethod = meth;
	}
	
	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
		sv.push(new GroupChecker(errors, sv, new FunctionGroupTCState(repository, grp)));
	}
	
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		new UDDChecker(errors, repository, sv);
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new UTAChecker(errors, repository, sv, a);
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect e) {
		new ExpectChecker(errors, repository, sv, e);
	}
	
	@Override
	public void result(Object r) {
		PosType result = (PosType) r;
		if (currentMethod != null) {
			currentMethod.bindType(result.type);
			currentMethod = null;
		}
	}
}
