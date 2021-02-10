package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractImplementor;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractProvider;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.CantHappenException;

public class UTSendChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final UnitTestSend send;

	public UTSendChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestSend s) {
		this.errors = errors;
		this.sv = sv;
		this.send = s;
		sv.push(this);
		// push the expression checker immediately to capture the unresolved var
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}

	@Override
	public void result(Object r) {
		PosType type = (PosType) r;
		Type ty = type.type;
		boolean isAllowed = ty instanceof ContractImplementor || ty instanceof ContractProvider;
		if (!isAllowed && ty instanceof ContractDecl && ((ContractDecl)ty).type == ContractType.HANDLER)
			isAllowed = true;
		if (!isAllowed) {
			errors.message(send.card.location(), "cannot send contract messages to " + send.card.var);
			return;
		}
		ContractDecl cd = null;
		NameOfThing cn = send.contract.defn().name();
		if (ty instanceof ContractImplementor) {
			ImplementsContract ic = ((ContractImplementor)ty).implementsContract(cn);
			if (ic != null)
				cd = ic.actualType();
		}
		if (ty instanceof ContractProvider) {
			Provides ic = ((ContractProvider)ty).providesContract(cn);
			if (ic != null)
				cd = ic.actualType();
		}
		if (ty instanceof ContractDecl) {
			cd = (ContractDecl) ty; 
		}
		if (cd != null) {
			int nargs;
			String meth;
			boolean haveHandler = false;
			if (send.expr instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) send.expr;
				meth = uv.var;
				nargs = 0;
			} else if (send.expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) send.expr;
				if (ae.fn instanceof UnresolvedOperator) {
					if (((UnresolvedOperator)ae.fn).op.equals("->")) {
						Expr as = (Expr) ae.args.get(0);
						if (as instanceof UnresolvedVar) {
							meth = ((UnresolvedVar)as).var;
							nargs = 0;
						} else {
							ae = (ApplyExpr) as;
							meth = ((UnresolvedVar) ae.fn).var;
							nargs = ae.args.size();
						}
						haveHandler = true;
					} else {
						errors.message(send.card.location(), "syntax error");
						return;
					}
				} else {
					meth = ((UnresolvedVar) ae.fn).var;
					nargs = ae.args.size();
				}
			} else
				throw new CantHappenException("What is this? " + send.expr.getClass());
			ContractMethodDecl method = cd.getMethod(meth);
			if (method == null) {
				errors.message(send.card.location(), "contract " + cn + " does not offer method " + meth);
				return;
			}
			// TODO: should we treat the handler specially here with -> as we do elsewhere?
			if (haveHandler && method.handler == null) {
				errors.message(send.card.location(), "contract method " + cn + "." + meth + " does not expect a handler");
				return;
			} else if (!haveHandler && method.handler != null) {
				errors.message(send.card.location(), "contract method " + cn + "." + meth + " expects a handler");
				return;
			}
			if (nargs != method.args.size()) {
				errors.message(send.card.location(), "contract method " + cn + "." + meth + " expects " + method.args.size() + " not " + nargs);
				return;
			}
		} else {
			errors.message(send.card.location(), send.card.var + " does not offer contract " + cn);
			return;
		}
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		sv.result(null);
	}

}
