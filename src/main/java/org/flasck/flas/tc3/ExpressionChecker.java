package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
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

	public static class IgnoreMe {
	}

	private final NestedVisitor nv;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final ErrorReporter errors;
	private InputPosition guardPos;
	private InputPosition exprPos;
	private final String fnCxt;
	private final boolean inTemplate;

	public ExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		guardPos = c.guard.location();
	}

	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		new CheckTypeExprChecker(errors, repository, state, nv, fnCxt, inTemplate);
	}
	
	@Override
	public void visitTypeExpr(TypeExpr expr) {
		new TypeExprChecker(errors, repository, state, nv, fnCxt, inTemplate);
	}
	
	@Override
	public void visitCastExpr(CastExpr expr) {
		new CastExprChecker(errors, repository, state, nv, fnCxt, inTemplate);
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
	public void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState) {
		if (isObjState && !wouldWantState)
			nv.result(new IgnoreMe());
		else
			announce(expr.location(), expr.type);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		InputPosition pos = var.location();
		if (var == null || var.defn() == null)
			throw new NullPointerException("undefined var: " + var);
		RepositoryEntry defn = var.defn();
		if (defn instanceof StructDefn || defn instanceof ObjectDefn || defn instanceof HandlerImplements) {
			throw new CantHappenException("should be TypeReferences");
		} else if (defn instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (fn.hasType())
				announce(pos, fn.type());
			else if (state.getMember(fn.name()) == null)
				announce(pos, new ErrorType());
			else
				announce(pos, state.getMember(fn.name()));
		} else if (defn instanceof TupleMember) {
			TupleMember tm = (TupleMember) defn;
			if (tm.type() != null)
				announce(pos, tm.type());
			else
				announce(pos, state.requireVarConstraints(tm.location(), tm.name().uniqueName(), tm.name().uniqueName()));
		} else if (defn instanceof StandaloneMethod) {
			StandaloneMethod fn = (StandaloneMethod) defn;
			if (fn.hasType())
				announce(pos, fn.type());
			else
				announce(pos, state.getMember(fn.name()));
		} else if (defn instanceof ObjectMethod) {
			ObjectMethod meth = (ObjectMethod) defn;
			if (meth.hasType())
				announce(pos, meth.type());
			else {
				announce(pos, state.getMember(meth.name()));
				// announce(pos, state.requireVarConstraints(meth.location(), meth.name().uniqueName()));
//				throw new NotImplementedException("I think this is invalid and " + meth + " should have a type defined, but it could be recursive - try adding the UT as per the comment");
			}
		} else if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.type() != null)
				announce(pos, vp.type());
			else
				announce(pos, state.requireVarConstraints(vp.location(), fnCxt, vp.name().uniqueName()));
		} else if (defn instanceof TypedPattern) {
			TypedPattern vp = (TypedPattern) defn;
			announce(pos, (Type) vp.type.defn());
		} else if (defn instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda)defn;
			announce(pos, (Type) hl.type());
		} else if (defn instanceof StructField) {
			StructField sf = (StructField) defn;
			announce(pos, (Type) sf.type.defn());
		} else if (defn instanceof TemplateNestedField) {
			TemplateNestedField tnf = (TemplateNestedField) defn;
			announce(pos, tnf.type());
		} else if (defn instanceof RequiresContract) {
			announce(pos, ((RequiresContract)defn).implementsType().defn());
		} else if (defn instanceof ObjectContract) {
			announce(pos, ((ObjectContract)defn).implementsType().defn());
		} else if (defn instanceof UnitDataDeclaration) {
			announce(pos, ((UnitDataDeclaration)defn).ofType.defn());
		} else if (defn instanceof IntroduceVar) {
			Type ia = ((IntroduceVar)defn).introducedAs();
			if (ia == null)
				ia = new ErrorType();
			announce(pos, ia);
		} else
			throw new RuntimeException("Cannot handle " + defn + " of type " + defn.getClass());
	}
	
	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		InputPosition pos = var.location();
		if (var == null || var.defn() == null)
			throw new NullPointerException("undefined var: " + var);
		NamedType defn = var.defn();
		if (defn instanceof StructDefn || defn instanceof ObjectDefn || defn instanceof HandlerImplements || defn instanceof Primitive) {
			announce(pos, defn);
		} else
			throw new RuntimeException("Cannot handle " + var.defn());
	}
	
	@Override
	public void visitAnonymousVar(AnonymousVar var) {
		announce(var.location(), (Type) new CurryArgumentType(var.location()));
	}
	
	@Override
	public void visitIntroduceVar(IntroduceVar var) {
		UnifiableType ut = state.createUT(var.location(), var.name().uniqueName());
		state.bindIntroducedVarToUT(var, ut);
		announce(var.location(), ut);
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
	public void visitHandleExpr(InputPosition location, Expr expr, Expr handler) {
		this.exprPos = location;
		nv.push(new MessageHandlerExpressionChecker(errors, repository, state, nv, fnCxt));
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		this.exprPos = expr.location;
		nv.push(new ApplyExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		this.exprPos = expr.location;
		if (!expr.boundEarly())
			nv.push(new MemberExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
		return expr.boundEarly();
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		if (expr.boundEarly()) {
			if (expr.defn() instanceof Type)
				announce(exprPos, (Type)expr.defn());
			else if (expr.defn() instanceof FunctionDefinition)
				announce(exprPos, ((FunctionDefinition)expr.defn()).type());
			else
				throw new HaventConsideredThisException("non-function package names");
		}
	}
	
	@Override
	public void result(Object r) {
		announce(exprPos, (Type)r);
	}

	private void announce(InputPosition pos, Type ty) {
		if (ty == null)
			throw new NotImplementedException("Cannot announce that a type is null");
		if (guardPos != null)
			nv.result(new GuardResult(pos, ty));
		else
			nv.result(new ExprResult(pos, ty));
	}

	public static Type check(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, String fnCxt, boolean inTemplate, Expr expr) {
		StackVisitor sv = new StackVisitor();
		CaptureChecker cc = new CaptureChecker(errors, repository, state, sv, fnCxt, inTemplate);
		Traverser t = new Traverser(sv);
		t.visitExpr(expr, 0);
		return ((ExprResult) cc.get()).type;
	}
}
