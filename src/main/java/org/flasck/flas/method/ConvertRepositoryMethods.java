package org.flasck.flas.method;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class ConvertRepositoryMethods extends LeafAdapter {
	private final NestedVisitor sv;
	private final ErrorReporter errors;

	public ConvertRepositoryMethods(NestedVisitor sv, ErrorReporter errors) {
		this.sv = sv;
		this.errors = errors;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		new AccessorConvertor(sv, errors);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod e) {
		sv.push(new MethodConvertor(errors, sv, e));
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor e) {
		sv.push(new MethodConvertor(errors, sv, e));
	}
	
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		new UDDConvertor(sv, errors);
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert e) {
		new AccessorConvertor(sv, errors);
	}
	
	@Override
	public void visitUnitTestInvoke(UnitTestInvoke e) {
		sv.push(new MessageConvertor(errors, sv, null));
	}
}
