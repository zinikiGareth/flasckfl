package org.flasck.flas.newtypechecker;

import org.flasck.flas.vcode.hsieForm.Var;

/** This constraint represents the case where we "applied" a variable to a set of arguments.
 * We can conclude that the var must be a function, and the closure thus has the type of the arguments left over after it has been applied _n_ times.
 *
 * <p>
 * &copy; 2016 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class AppliedFnConstraint implements Constraint {
	private final Var fv;
	private final int argsApplied;

	public AppliedFnConstraint(Var fv, int applied) {
		this.fv = fv;
		this.argsApplied = applied;
	}

	@Override
	public String toString() {
		return "App[" + fv + ":" + argsApplied +"]";
	}
}
