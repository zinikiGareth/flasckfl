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
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class ExpressionChecker extends LeafAdapter implements ResultAware {
	public static class ExprResult extends PosType {
		public ExprResult(InputPosition pos, Type t) {
			super(pos, t);
		}
	}

	public static class GuardResult extends PosType {
		public GuardResult(InputPosition pos, Type t) {
			super(pos, t);
		}
	}

	private final NestedVisitor nv;
	private final CurrentTCState state;
	private final ErrorReporter errors;
	private InputPosition guardPos;
	private InputPosition exprPos;

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
		announce(number.location, LoadBuiltins.number);
	}

	@Override
	public void visitStringLiteral(StringLiteral s) {
		announce(s.location(), LoadBuiltins.string);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		InputPosition pos = var.location();
		if (var == null || var.defn() == null)
			throw new NullPointerException("undefined var: " + var);
		RepositoryEntry defn = var.defn();
		if (defn instanceof StructDefn) {
			announce(pos, (Type) defn);
		} else if (defn instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (fn.type() != null)
				announce(pos, fn.type());
			else
				announce(pos, state.requireVarConstraints(fn.location(), fn.name().uniqueName()));
		} else if (defn instanceof TupleMember) {
			TupleMember tm = (TupleMember) defn;
			announce(pos, state.requireVarConstraints(tm.location(), tm.name().uniqueName()));
		} else if (defn instanceof StandaloneMethod) {
			StandaloneMethod fn = (StandaloneMethod) defn;
			if (fn.hasType())
				announce(pos, fn.type());
			else
				announce(pos, state.requireVarConstraints(fn.location(), fn.name().uniqueName()));
		} else if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.type() != null)
				announce(pos, vp.type());
			else
				announce(pos, state.requireVarConstraints(vp.location(), vp.name().uniqueName()));
		} else if (defn instanceof TypedPattern) {
			TypedPattern vp = (TypedPattern) defn;
			announce(pos, (Type) vp.type.defn());
		} else if (defn instanceof StructField) {
			StructField sf = (StructField) defn;
			announce(pos, (Type) sf.type.defn());
		} else if (defn instanceof CurryArgument) {
			announce(pos, (Type) new CurryArgumentType(((Locatable)defn).location()));
		} else if (defn instanceof RequiresContract) {
			announce(pos, ((RequiresContract)defn).implementsType().defn());
		} else if (defn instanceof UnitDataDeclaration) {
			announce(pos, ((UnitDataDeclaration)defn).ofType.defn());
		} else
			throw new RuntimeException("Cannot handle " + defn + " of type " + defn.getClass());
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator op, int nargs) {
		if (op.defn() instanceof StructDefn) {
			announce(op.location(), (Type) op.defn());
		} else if (op.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) op.defn();
			// This handles the tuple case
			announce(op.location(), fn.type());
		} else
			throw new RuntimeException("Cannot handle " + op);
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		this.exprPos = expr.location;
		nv.push(new ApplyExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr) {
		this.exprPos = expr.location;
		nv.push(new MemberExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		announce(exprPos, (Type)r);
	}

	private void announce(InputPosition pos, Type ty) {
		if (ty == null)
			throw new NotImplementedException("Cannot announce that a type is null");
		if (guardPos != null)
			nv.result(new GuardResult(guardPos, ty));
		else
			nv.result(new ExprResult(pos, ty));
	}
}
