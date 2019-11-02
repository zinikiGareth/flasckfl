package org.flasck.flas.patterns;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Primitive;
import org.zinutils.exceptions.NotImplementedException;

public class PatternAnalyzer extends LeafAdapter {
	private HSITree hsiTree;
	private final NestedVisitor sv;
	private int nslot;
	private HSIOptions slot;
	private FunctionIntro current;
	private ErrorReporter errors;
	private RepositoryReader repository;
	
	public PatternAnalyzer(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	public PatternAnalyzer(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, HSITree tree, FunctionIntro current) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.hsiTree = tree;
		this.current = current;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		hsiTree = new HSIArgsTree(fn.argCount());
	}
	
	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
		throw new RuntimeException("HERE!");
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		nslot = 0;
		current = fi;
		hsiTree.consider(fi);
	}
	
	@Override
	public void visitPattern(Object patt, boolean isNested) {
		this.slot = hsiTree.get(nslot++);
	}
	
	@Override
	public void visitVarPattern(VarPattern p, boolean isNested) {
		this.slot.addVar(p, current);
		this.slot.includes(this.current);
	}
	
	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
		if (!isNested)
			this.slot.addTyped(p, current);
		else
			this.slot.addVarWithType(p.type, p.var, current);
		this.slot.includes(this.current);
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		HSITree nested = slot.requireCM(p.actual());
		nested.consider(current);
		new PatternAnalyzer(errors, repository, sv, nested, current);
	}
	
	@Override
	public void visitConstructorField(String field, Object patt, boolean isNested) {
		this.slot = ((HSICtorTree)hsiTree).field(field);
		this.slot.includes(this.current);
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
		sv.result(hsiTree);
	}

	@Override
	public void visitConstPattern(ConstPattern p, boolean isNested) {
		Primitive ty;
		switch (p.type) {
		case ConstPattern.INTEGER:
			ty = repository.get("Number");
			break;
		case ConstPattern.STRING:
			ty = repository.get("String");
			break;
		default:
			throw new NotImplementedException("Cannot handle " + p.type);
		}
		this.slot.addConstant(ty, p.value, current);
		this.slot.includes(this.current);
	}
	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		// TODO: this should actually bind a projection of the tree
		fi.bindTree(hsiTree);
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindHsi(hsiTree);
	}

	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
		throw new RuntimeException("HERE!");
	}
}
