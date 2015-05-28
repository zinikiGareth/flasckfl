package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.zinutils.exceptions.UtilException;

public class TypeChecker {
	public final ErrorResult errors = new ErrorResult();
	private final VariableFactory factory = new VariableFactory();
	private final List<HSIEForm> functionsToCheck;

	// TODO: I think this wants to become yet more complex: List<Set<HSIE>> to reflect the dependency management and simultaneous functions
	public TypeChecker(List<HSIEForm> functionsToCheck) {
		this.functionsToCheck = functionsToCheck;
	}
	
	public void typecheck() {
		TypeEnvironment gamma = new TypeEnvironment(); // should be based on everything we already know
		PhiSolution phi = new PhiSolution();
		for (HSIEForm hsie : functionsToCheck) {
			// This is the alleged type of the function, which, if non-null, we should store
			Object te = checkHSIE(phi, gamma, hsie);
		}
	}

	Object checkHSIE(PhiSolution phi, TypeEnvironment gamma, HSIEForm hsie) {
		List<TypeVar> vars = new ArrayList<TypeVar>();
		for (int i=0;i<hsie.nformal;i++) {
			TypeVar tv = factory.next();
			gamma = gamma.bind(hsie.vars.get(i), new TypeScheme(null, tv));
			vars.add(tv);
		}
		System.out.println(gamma);
		// what we need to do is to apply tcExpr to the right hand side with the new gamma
		Object rhs = checkBlock(phi, gamma, hsie);
		if (rhs == null)
			return null;
		// then we need to build an expr tv0 -> tv1 -> tv2 -> E with all the vars substituted
		for (int i=vars.size()-1;i>=0;i--)
			rhs = new TypeExpr("->", phi.meaning(vars.get(i)), rhs);
		return rhs;
	}

	private Object checkBlock(PhiSolution phi, TypeEnvironment gamma, HSIEBlock hsie) {
		for (HSIEBlock o : hsie.nestedCommands()) {
			if (o instanceof ReturnCmd)
				return tcExpr(phi, gamma, o);
			throw new UtilException("Missing cases");
		}
		throw new UtilException("We shouldn't get here");
	}

	Object tcExpr(PhiSolution phi, TypeEnvironment gamma, HSIEBlock cmd) {
		if (cmd instanceof ReturnCmd) {
			ReturnCmd r = (ReturnCmd) cmd;
			if (r.ival != null)
				return new TypeExpr("Number");
			if (r.var != null) {
				// phi is not updated
				TypeScheme old = gamma.valueOf(r.var);
				PhiSolution temp = new PhiSolution();
				for (TypeVar tv : old.schematicVars)
					temp.bind(tv, factory.next());
				return temp.subst(old.typeExpr);
			}
		}
		throw new UtilException("Missing cases");
	}
}
