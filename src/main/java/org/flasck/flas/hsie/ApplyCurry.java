package org.flasck.flas.hsie;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.zinutils.exceptions.UtilException;

public class ApplyCurry {

	public void rewrite(TypeChecker tc, HSIEForm h) {
		for (HSIEBlock c : h.closures()) {
//			c.dumpOne(4);
			PushCmd pc = (PushCmd) c.nestedCommands().get(0);

			if (pc.fn != null) {
				if (pc.fn instanceof CardMember)
					continue;
				if (pc.fn.uniqueName().equals("FLEval.tuple"))
					continue;
				Type t = tc.getTypeDefn(pc.fn.uniqueName());
				if (t.arity() != c.nestedCommands().size()-1) {
//					System.out.println("need to curry block for type = " + t);
					c.pushAt(0, new AbsoluteVar("FLEval.curry", null));
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
