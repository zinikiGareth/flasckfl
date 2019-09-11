package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class TypeChecker extends LeafAdapter implements ResultAware {
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final List<Type> types = new ArrayList<>();
	private Type exprType;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		types.clear();
	}
	
	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		sv.push(new ExpressionChecker(repository, sv));
	}
	
	@Override
	public void result(Object r) {
		this.exprType = (Type) r;
	}

	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		HSITree tree = fi.hsiTree();
		if (tree.width() == 0)
			types.add(exprType);
		else {
			Type[] atypes = new Type[tree.width() + 1];
			for (int i=0;i<tree.width();i++) {
				atypes[i] = tree.get(i).minimalType(repository);
			}
			atypes[atypes.length-1] = exprType;
			types.add(new Apply(atypes));
		}
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (types.isEmpty())
			return;
		fn.bindType(consolidateType());
		types.clear();
	}

	private Type consolidateType() {
		// TODO: this actually needs to consolidate the types ...
		return types.get(0);
	}
}
