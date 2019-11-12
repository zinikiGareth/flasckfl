package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class ExpressionChecker extends LeafAdapter implements ResultAware {
	public static class ExprResult {
		public final Type type;
		
		public ExprResult(Type t) {
			this.type = t;
		}
	}

	public static class GuardResult implements Locatable {
		public final Type type;
		private final InputPosition pos;
		
		public GuardResult(InputPosition pos, Type t) {
			this.pos = pos;
			this.type = t;
		}

		@Override
		public InputPosition location() {
			return pos;
		}
	}

	private final NestedVisitor nv;
	private final CurrentTCState state;
	private final ErrorReporter errors;
	private InputPosition guardPos;

	public ExpressionChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		guardPos = c.guard.location();
	}

	@Override
	public void visitNumericLiteral(NumericLiteral number) {
		announce(LoadBuiltins.number);
	}

	@Override
	public void visitStringLiteral(StringLiteral s) {
		announce(LoadBuiltins.string);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (var == null || var.defn() == null)
			throw new NullPointerException("undefined var: " + var);
		if (var.defn() instanceof StructDefn) {
			announce((Type) var.defn());
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			if (fn.type() != null)
				announce(fn.type());
			else
				announce(state.requireVarConstraints(fn.location(), fn.name().uniqueName()));
		} else if (var.defn() instanceof StandaloneMethod) {
			StandaloneMethod fn = (StandaloneMethod) var.defn();
			announce(fn.type());
		} else if (var.defn() instanceof VarPattern) {
			VarPattern vp = (VarPattern) var.defn();
			announce(state.requireVarConstraints(vp.location(), vp.name().uniqueName()));
		} else if (var.defn() instanceof TypedPattern) {
			TypedPattern vp = (TypedPattern) var.defn();
			announce((Type) vp.type.defn());
		} else if (var.defn() instanceof CurryArgument) {
			announce((Type) new CurryArgumentType(((Locatable)var.defn()).location()));
		} else
			throw new RuntimeException("Cannot handle " + var.defn() + " of type " + var.defn().getClass());
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator var, int nargs) {
		if (var.defn() instanceof StructDefn) {
			announce((Type) var.defn());
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			announce(fn.type());
		} else
			throw new RuntimeException("Cannot handle " + var);
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new ApplyExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr) {
		nv.push(new MemberExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		announce((Type)r);
	}

	private void announce(Type ty) {
		if (guardPos != null)
			nv.result(new GuardResult(guardPos, ty));
		else
			nv.result(new ExprResult(ty));
	}
}
