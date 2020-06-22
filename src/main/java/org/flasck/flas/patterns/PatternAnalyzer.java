package org.flasck.flas.patterns;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Primitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.NotImplementedException;

public class PatternAnalyzer extends LeafAdapter {
	public final static Logger logger = LoggerFactory.getLogger("Patterns");
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
		logger.info("analyzing " + fn.name().uniqueName() + " with " + fn.argCount() + " total patterns");
		hsiTree = new HSIArgsTree(fn.argCount());
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		int quant = meth.argCount() + (meth.handler != null ? 1 : 0);
		logger.info("analyzing " + meth.name().uniqueName() + " with " + quant + " total patterns");
		hsiTree = new HSIArgsTree(quant);
		nslot = 0;
		current = null;
		hsiTree.consider(null);
	}

	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		logger.info("analyzing " + meth.name().uniqueName() + " with " + meth.argCount() + " total patterns");
		hsiTree = new HSIArgsTree(meth.argCount());
		nslot = 0;
		current = null;
		hsiTree.consider(null);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		nslot = 0;
		current = fi;
		hsiTree.consider(fi);
		logger.info("Considering intro " + fi.name().uniqueName());
	}
	
	@Override
	public void visitPattern(Pattern patt, boolean isNested) {
		this.slot = hsiTree.get(nslot++);
	}
	
	@Override
	public void visitVarPattern(VarPattern p, boolean isNested) {
		logger.info("var " + p.var);
		this.slot.addVar(p, current);
		this.slot.includes(this.current);
	}
	
	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
		logger.info((isNested?"nested ":"") + "typed  var " + p.var);
		if (!isNested)
			this.slot.addTyped(p, current);
		else
			this.slot.addVarWithType(p.type, p.var, current);
		this.slot.includes(this.current);
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		logger.info("Nesting constructor match for " + p.actual());
		HSITree nested = slot.requireCM(p.actual());
		nested.consider(current);
		new PatternAnalyzer(errors, repository, sv, nested, current);
	}
	
	@Override
	public void visitConstructorField(String field, Pattern patt, boolean isNested) {
		this.slot = ((HSICtorTree)hsiTree).field(field);
		this.slot.includes(this.current);
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
		logger.info("Done matching constructor " + p.actual());
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
		fi.bindTree(hsiTree);
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		logger.info("analyzed " + fn.name().uniqueName());
		fn.bindHsi(hsiTree);
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		logger.info("analyzed " + meth.name().uniqueName());
		meth.bindHsi(hsiTree);
	}

	@Override
	public void leaveObjectCtor(ObjectCtor meth) {
		logger.info("analyzed " + meth.name().uniqueName());
		meth.bindHsi(hsiTree);
	}
}
