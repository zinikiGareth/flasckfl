package org.flasck.flas.tc3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.HaventConsideredThisException;

public class GroupChecker extends LeafAdapter implements ResultAware {
	private final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final ErrorReporter errors;
	private final RepositoryReader repository; 
	private final NestedVisitor sv;
	private CurrentTCState state;
	private TypeBinder currentFunction;
	private final Map<TypeBinder, PosType> memberTypes = new HashMap<>();
	private final Map<TypeBinder, PosType> resultTypes = new HashMap<>();

	public GroupChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state, ErrorMark mark) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		new FunctionChecker(errors, repository, sv, fn.name(), state, null);
		this.currentFunction = fn;
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		this.currentFunction = meth;
	}

	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		this.currentFunction = meth;
	}

	@Override
	public void visitTuple(TupleAssignment ta) {
		new FunctionChecker(errors, repository, sv, ta.name(), state, null);
		this.currentFunction = ta;
		sv.push(new ExpressionChecker(errors, repository, state, sv, ta.name().uniqueName(), false));
	}

	@Override
	public void leaveTupleMember(TupleMember tm) {
//		new FunctionChecker(errors, repository, sv, tm.name(), state, null);
//		this.currentFunction = tm;
//		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
		// This should have been bound previously in tuple assignment
		memberTypes.put(tm, new PosType(tm.location(), tm.type()));
	}
	
	@Override
	public void result(Object r) {
		PosType pt = (PosType)r;
		memberTypes.put(currentFunction, pt);
		resultTypes.put(currentFunction, extractResult(currentFunction, pt));
		if (pt != null) {
			logger.info("result of " + currentFunction + " is " + resultTypes.get(currentFunction).type);
		}
		this.currentFunction = null;
	}

	public static PosType extractResult(TypeBinder fn, PosType pt) {
		if (pt == null) {
			logger.info("  nothing deduced for " + fn);
			return null;
		}
		logger.debug("  deduced type of " + fn + " is: " + pt.type);
		int ac = fn.argCount();
		logger.debug("    argCount including everything = " + ac);
		if (ac == 0)
			return pt;
		else if (!(pt.type instanceof Apply)) {
			throw new HaventConsideredThisException("need to strip args from " + pt.type + " for " + fn + " but it is not an apply");
		} else {
			Apply ap = (Apply) pt.type;
			if (ac == ap.argCount()) {
				return new PosType(pt.pos, ap.tys.get(ac));
			} else if (ac > ap.argCount())
				throw new HaventConsideredThisException("want to strip " + ac + " args from " + pt.type + " for " + fn);
			else {
				List<Type> sl = ap.tys.subList(ac, ap.tys.size());
				return new PosType(pt.pos, new Apply(sl));
			}
		}
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		// I would like this to be here, but it needs to be more refined
		// Specifically, I think it should check dependencies and if they had errors
//		if (mark != null && !mark.hasMoreNow())
		state.groupDone(errors, memberTypes, resultTypes);
		sv.result(null);
	}

	public CurrentTCState testsWantToCheckState() {
		return state;
	}
}
