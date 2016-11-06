package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

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
	public TypeInfo typeInfo() {
		throw new NotImplementedException("You need to pass unions across");
	}
	
	public TypeInfo typeInfo(Map<Var, TypeInfo> unions) {
		TypeInfo ti = unions.get(fv);
		if (!(ti instanceof TypeFunc))
			throw new UtilException(fv + " must be a function type");
		TypeFunc tf = (TypeFunc) ti;
		if (tf.args.size()-1 < argsApplied)
			throw new UtilException("Too many args applied to " + tf + ": " + argsApplied);
		else if (tf.args.size()-1 == argsApplied)
			return tf.args.get(tf.args.size()-1);
		else {
			List<TypeInfo> subargs = new ArrayList<TypeInfo>();
			for (int i=argsApplied;i<tf.args.size();i++)
				subargs.add(tf.args.get(i));
			return new TypeFunc(subargs);
		}
	}

	@Override
	public String toString() {
		return "App[" + fv + ":" + argsApplied +"]";
	}
}
