package org.flasck.flas.hsie;

import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.zinutils.exceptions.UtilException;

public class ApplyCurry {

	public void rewrite(TypeChecker tc, HSIEForm h) {
		for (HSIEBlock c : h.closures()) {
			c.dumpOne(4);
			PushCmd pc = (PushCmd) c.nestedCommands().get(0);

			// This is a very weird case
			if (pc.fn != null) { // the normal case
				if (pc.fn.equals("FLEval.tuple"))
					continue;
				Type t = tc.getTypeDefn(pc.fn.uniqueName());
				if (t.arity() != c.nestedCommands().size()-1) {
					System.out.println("need to curry block for type = " + t);
					c.pushAt(0, "FLEval.curry");
					c.pushAt(2, t.arity());
				}
			} else if (pc.var != null) { // the closure case, q.v.
				
			} else {
				System.out.println(pc);
				throw new UtilException("I don't think this can have passed typecheck");
			}
		}
	}

}
