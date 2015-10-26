package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.ScopedVar;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
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
//		Logger logger = LoggerFactory.getLogger("Hello");
		for (HSIEBlock c : h.closures()) {
//			logger.info("-----");
//			c.dumpOne(logger, 4);
			PushCmd pc = (PushCmd) c.nestedCommands().get(0);
			if (pc.sval != null)
				continue;
			if (pc.fn != null) {
				if (pc.fn instanceof HandlerLambda)
					continue;
				if (pc.fn instanceof CardMember)
					continue;
				if (pc.fn instanceof ScopedVar)
					continue;
				if (pc.fn.uniqueName().equals("FLEval.tuple"))
					continue;
				if (pc.fn.uniqueName().equals("FLEval.field")) {
					PushCmd ofObj = (PushCmd) c.nestedCommands().get(1);
					PushCmd fld = (PushCmd)c.nestedCommands().get(2);
					if (ofObj.fn instanceof CardMember) {
						CardMember cm = (CardMember) ofObj.fn;
						if (cm.type instanceof ObjectDefn) {
							ObjectDefn od = (ObjectDefn) cm.type;
							if (od.hasMethod(fld.sval.text)) {
								Type t = od.getMethod(fld.sval.text);
								c.pushAt(pc.location, 0, new PackageVar(pc.location, "FLEval.curry", null));
								c.removeAt(1);
								c.pushAt(pc.location, 1, new PackageVar(pc.location, "FLEval.method", null));
								c.pushAt(pc.location, 2, t.arity()+2);
							}
						}
					}
					continue;
				}
				Type t = tc.getTypeAsCtor(pc.location, pc.fn.uniqueName());
				if (t.iam != Type.WhatAmI.FUNCTION)
					;
				else if (t.arity() > c.nestedCommands().size()-1 + pc.inheritArgs.size()) {
					c.pushAt(pc.location, 0, new PackageVar(null, "FLEval.curry", null));
					c.pushAt(pc.location, 2, t.arity());
				} else if (t.arity() < c.nestedCommands().size()-1 && !((ClosureCmd)c).justScoping) {
					throw new UtilException("Have too many arguments for the function " + pc.fn + " - error or need to replace f x y with (f x) y?");
				}
			} else if (pc.var != null) { // the closure case, q.v.
			} else if (pc.func != null) {
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
			ClosureCmd oclos = h.closure(v);
			Type t = tc.getTypeAsCtor(pc.location, pc.fn.uniqueName());
			if (t.iam == WhatAmI.FUNCTION && t.arity() > 0) {
//				System.out.println("need to curry block for type = " + t);
				oclos.push(pc.location, new PackageVar(null, "FLEval.curry", null));
				oclos.push(pc.location, pc.fn);
				oclos.push(pc.location, t.arity());
			} else
				oclos.push(pc.location, pc.fn);
			r.inside.nestedCommands().set(r.pos, new PushCmd(pc.location, new CreationOfVar(v, null, null)));
			Var myVar = ((ClosureCmd)r.inside).var;
			updateAllReturnCommands(h, myVar, v);
		}
	}

	private void updateAllReturnCommands(HSIEBlock h, Var before, Var newClos) {
		for (HSIEBlock x : h.nestedCommands()) {
			if (x instanceof ReturnCmd)
				addClosureBefore((ReturnCmd)x, before, newClos);
			updateAllReturnCommands(x, before, newClos);
		}
	}

	protected void addClosureBefore(ReturnCmd rc, Var before, Var newClos) {
//		System.out.println("Adding " + newClos + " to " + rc + " before " + before);
		int at = -1;
		if (rc.var.var == before) {
			rc.deps.add(new CreationOfVar(newClos, null, null));
		} else {
			for (int i=0;i<rc.deps.size();i++)
				if (rc.deps.get(i).var == before) {
					at = i;
					break;
				}
			if (at == -1)
				throw new UtilException("Did not find " + before + " in " + rc.deps);
			rc.deps.add(at, new CreationOfVar(newClos, null, null));
		}
	}

}
