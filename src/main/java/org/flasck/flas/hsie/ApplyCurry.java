package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class ApplyCurry {
	class Rewrite {
		HSIEBlock inside;
		int pos;

		public Rewrite(HSIEBlock inside, int pos) {
			this.inside = inside;
			this.pos = pos;
		}
	}
	
	public void rewrite(TypeChecker tc, HSIEForm h) {
		List<Rewrite> rewrites = new ArrayList<Rewrite>();
		
		for (HSIEBlock c : h.closures()) {
//			c.dumpOne(4);
			PushCmd pc = (PushCmd) c.nestedCommands().get(0);
			if (pc.sval != null)
				continue;
			if (pc.fn != null) {
				if (pc.fn instanceof HandlerLambda)
					continue;
				if (pc.fn instanceof CardMember)
					continue;
				if (pc.fn.uniqueName().equals("FLEval.tuple"))
					continue;
				Type t = tc.getTypeAsCtor(pc.fn.uniqueName());
				if (t.arity() != c.nestedCommands().size()-1) {
					c.pushAt(pc.location, 0, new AbsoluteVar(null, "FLEval.curry", null));
					c.pushAt(pc.location, 2, t.arity());
				}
			} else if (pc.var != null) { // the closure case, q.v.
				 
			} else {
				throw new UtilException("I don't think this can have passed typecheck");
			}
			for (int pos=0;pos<c.nestedCommands().size();pos++) {
				PushCmd pc2 = (PushCmd) c.nestedCommands().get(pos);
				if (pc2.fn != null) {
					if (pc2.fn instanceof CardFunction) {
						rewrites.add(new Rewrite(c, pos));
						continue;
					}
				}
			}
		}
		for (Rewrite r : rewrites) {
			PushCmd pc = (PushCmd) r.inside.nestedCommands().get(r.pos);
			Var v = h.allocateVar();
			HSIEBlock oclos = h.closure(v);
			Type t = tc.getTypeAsCtor(pc.fn.uniqueName());
			if (t.arity() > 0) {
//				System.out.println("need to curry block for type = " + t);
				oclos.push(pc.location, new AbsoluteVar(null, "FLEval.curry", null));
				oclos.push(pc.location, pc.fn);
				oclos.push(pc.location, t.arity());
			} else
				oclos.push(pc.location, pc.fn);
			r.inside.nestedCommands().set(r.pos, new PushCmd(pc.location, v));
			ReturnCmd rc = (ReturnCmd) h.nestedCommands().get(0);
			int at = -1;
			Var myVar = ((ClosureCmd)r.inside).var;
			if (rc.var == myVar) {
				rc.deps.add(v);
			} else {
				for (int i=0;i<rc.deps.size();i++)
					if (rc.deps.get(i) == myVar) {
						at = i;
						break;
					}
				if (at == -1)
					throw new UtilException("Did not find " + myVar + " in " + rc.deps);
				rc.deps.add(at, v);
			}
		}
	}

}
