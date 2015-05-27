package org.flasck.flas.typechecker;

import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;

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
		for (HSIEForm hsie : functionsToCheck)
			checkHSIE(gamma, hsie);
	}

	private void checkHSIE(TypeEnvironment gamma, HSIEForm hsie) {
		for (int i=0;i<hsie.nformal;i++) {
			TypeVar tv = factory.next();
			gamma = gamma.bind(hsie.vars.get(i), new TypeScheme(null, tv));
		}
		System.out.println(gamma);
		checkBlock(hsie);
	}

	private void checkBlock(HSIEBlock hsie) {
		for (HSIEBlock o : hsie.nestedCommands()) {
			tcExpr(o);
		}
	}

	public Object tcExpr(HSIEBlock cmd) {
		if (cmd instanceof ReturnCmd) {
			ReturnCmd r = (ReturnCmd) cmd;
			if (r.ival != null)
				return new TypeExpr("Number", null);
		}
		return null;
	}
}
