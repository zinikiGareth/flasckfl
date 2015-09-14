package org.flasck.flas.typechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.FLASError;

public class UnificationError extends FLASError {

	public UnificationError(TypeExpr te1, TypeExpr te2) {
		super(bestLocation(te1.from, te2.from), reportMessage(te1, te2, false));
		TypeChecker.logger.warn(this.toString());
	}

	private static InputPosition bestLocation(GarneredFrom from1, GarneredFrom from2) {
		if (from1 != null && from1.posn != null)
			return from1.posn;
		if (from2 != null && from2.posn != null)
			return from2.posn;
		return null;
	}

	private static String reportMessage(TypeExpr te1, TypeExpr te2, boolean switchedAlready) {
		if (!switchedAlready && ((te1.from == null && te2.from != null) || (te1.from.posn != null && (te2.from != null || te2.from.posn == null))))
			return reportMessage(te2, te1, true);
		
		if (te1.from == null || te2.from == null)
			return "unification failed: " + te1 + " <> " + te2;
		
		if (te1.from.type != null && te1.from.arg >= 0) { // we were trying to call a function, the most likely case
			int arity = ((Type)te1.from.type).arity();
			int calledWith = figureArgsInDeepCall(te2);
			if (arity < calledWith)
				return "called with too many arguments";
			else if (arity > calledWith)
				return "called with too few arguments"; // but I think this should possibly be curried?
			else if (!te1.type.name().equals(te2.type.name()))
				return "cannot pass " + te2.type.name() + " as " + te1.type.name();
			return "some unidentified function error";
		}
		
		if (!te1.type.name().equals(te2.type.name()))
			return "inconsistent types: " + te2.type.name() + " and " + te1.type.name();

		return "unification failed with previously unreported case " + te1 + " <> " + te2;
	}

	private static int figureArgsInDeepCall(Object o) {
		if (o instanceof TypeExpr) {
			TypeExpr te = (TypeExpr) o;
			if (te.type.name().equals("->"))
				return 1 + figureArgsInDeepCall(te.args.get(1));
			return 0;
		}
		return 0;
	}
}
