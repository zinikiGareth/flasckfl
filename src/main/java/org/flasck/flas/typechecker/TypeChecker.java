package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.zinutils.exceptions.UtilException;

public class TypeChecker {
	public final ErrorResult errors = new ErrorResult();
	private final VariableFactory factory = new VariableFactory();
	private final Map<String, Object> knowledge = new HashMap<String, Object>();

	public TypeChecker() {
	}
	
	public void addExternal(String name, Object type) {
		knowledge.put(name, type);
	}

	public void typecheck(Set<HSIEForm> functionsToCheck) {
		TypeEnvironment gamma = new TypeEnvironment(); // should be based on everything we already know
		PhiSolution phi = new PhiSolution(errors);
		for (HSIEForm hsie : functionsToCheck) {
			// This is the alleged type of the function, which, if non-null, we should store
			Object te = checkHSIE(phi, gamma, hsie);
		}
		// TODO: I think we now need to do some level of unification on the remaining solution
		// TODO: Then we probably need to re-apply the solution to all of the types we just had
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
		Object rhs = checkBlock(phi, gamma, hsie, hsie);
		if (rhs == null)
			return null;
		// then we need to build an expr tv0 -> tv1 -> tv2 -> E with all the vars substituted
		for (int i=vars.size()-1;i>=0;i--)
			rhs = new TypeExpr("->", phi.meaning(vars.get(i)), rhs);
		return rhs;
	}

	private Object checkBlock(PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock hsie) {
		for (HSIEBlock o : hsie.nestedCommands()) {
			if (o instanceof ReturnCmd)
				return checkExpr(phi, gamma, form, o);
			throw new UtilException("Missing cases");
		}
		throw new UtilException("We shouldn't get here");
	}

	Object checkExpr(PhiSolution phi, TypeEnvironment gamma, HSIEForm form, HSIEBlock cmd) {
		if (cmd instanceof PushReturn) {
			PushReturn r = (PushReturn) cmd;
			if (r.ival != null)
				return new TypeExpr("Number");
			else if (r.var != null) {
				HSIEBlock c = form.getClosure(r.var);
				if (c == null) {
					// phi is not updated
					// assume it must be a bound var; we will fail to get the existing type scheme if not
					TypeScheme old = gamma.valueOf(r.var);
					PhiSolution temp = new PhiSolution(errors);
					for (TypeVar tv : old.schematicVars)
						temp.bind(tv, factory.next());
					return temp.subst(old.typeExpr);
				} else {
					// c is a closure, which must be a function application
					List<Object> args = new ArrayList<Object>();
					for (HSIEBlock b : c.nestedCommands()) {
						Object te = checkExpr(phi, gamma, form, b);
						args.add(te);
					}
					Object Tf = args.get(0);
					for (int i=1;i<args.size();i++)
						Tf = checkSingleApplication(phi, Tf, args.get(i));
					return Tf;
				}
			} else if (r.fn != null) {
				// phi is not updated
				// I am going to say that by getting here, we know that it must be an external
				// all lambdas should be variables by now
				Object te = knowledge.get(r.fn);
				if (te == null) {
					// This is probably a failure on our part rather than user error
					// We should not be able to get here if r.fn is not already an external which has been resolved
					errors.message((Block)null, "There is no type for " + r.fn); // We need some way to report error location
					return null;
				} else
					return te;
			} else
				throw new UtilException("What are you returning?");
		} else
			throw new UtilException("Missing cases");
	}

	private Object checkSingleApplication(PhiSolution phi, Object Tf, Object Tx) {
		TypeVar Tr = factory.next();
		TypeExpr Tf2 = new TypeExpr("->", Tx, Tr);
		phi.unify(Tf, Tf2);
		if (errors.hasErrors())
			return null;
		return phi.meaning(Tr);
	}
}
