package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class ExpectChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final String fnCxt;
	private boolean isHandler;
	private Type mock;
	private List<Type> args = new ArrayList<>();
	private Type handler;

	public ExpectChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestExpect e) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
		state = new FunctionGroupTCState(repository, new DependencyGroup());
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
	}
	
	@Override
	public void visitExpr(Expr e, int nargs) {
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
	}
	
	@Override
	public void expectHandlerNext() {
		this.isHandler = true;
	}

	@Override
	public void result(Object r) {
		Type t = ((ExprResult)r).type;
		if (mock == null)
			mock = t;
		else if (isHandler)
			handler = t;
		else
			args.add(t);
	}
	
	@Override
	public void leaveUnitTestExpect(UnitTestExpect e) {
		// Check for cascades
		if (mock instanceof ErrorType || handler instanceof ErrorType) {
			sv.result(mock);
			return;
		}
		for (Type t : args) {
			if (t instanceof ErrorType) {
				sv.result(null);
				return;
			}
		}
		if (!(mock instanceof ContractDecl)) {
			errors.message(e.ctr.location(), "expect requires a contract variable");
			sv.result(null);
			return;
		}
		
		ContractDecl cd = (ContractDecl)mock;
		String methName = e.method.var;
		ContractMethodDecl m = cd.getMethod(methName);
		if (m == null) {
			errors.message(e.method.location(), "there is no method " + methName + " in " + cd.name().uniqueName());
			sv.result(null);
			return;
		}
		
		if (m.args.size() != args.size()) {
			errors.message(e.method.location(), "incorrect number of arguments for " + cd.name().uniqueName() + "." + methName);
			sv.result(null);
			return;
		}
		
		for (int i=0; i<m.args.size(); i++) {
			InputPosition aloc = e.args.get(i).location();
			Type atype = m.args.get(i).type();
			if (!atype.incorporates(aloc, args.get(i))) {
				errors.message(aloc, "type error: " + atype + " " + args.get(i));
				sv.result(new ErrorType());
				return;
			}
		}
		
		if (e.handler != null && !(e.handler instanceof AnonymousVar)) {
			if (m.handler == null) {
				errors.message(e.handler.location(), "contract method " + m.name.uniqueName() + " does not expect a handler");
				sv.result(new ErrorType());
				return;
			}
			NamedType htype = m.handler.type.namedDefn();
			if (!htype.incorporates(e.handler.location(), handler)) {
				errors.message(e.handler.location(), "type error: " + htype + " " + handler);
				sv.result(new ErrorType());
				return;
			}
		}
		
		state.groupDone(errors, new HashMap<>(), new HashMap<>());
		state.bindIntroducedVarTypes(errors);
		sv.result(null);
	}
}
