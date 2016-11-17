package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	public void rewrite(TypeChecker2 tc, HSIEForm h) {
		List<Rewrite> rewrites = new ArrayList<Rewrite>();
		Logger logger = LoggerFactory.getLogger("HSIE");
		logger.info("--- ApplyCurry to: " + h.fnName);
		h.dump(logger);
		for (HSIEBlock c : h.closures()) {
			PushReturn pc = (PushReturn) c.nestedCommands().get(0);
			if (pc instanceof PushString)
				continue;
			// The purpose of this is to stop us rewriting things we've already rewritten
			int lookFrom = 0;
			if (pc instanceof PushExternal) {
				ExternalRef ex = ((PushExternal)pc).fn;
				if (ex instanceof HandlerLambda)
					continue;
				if (ex instanceof CardMember)
					continue;
				if (ex instanceof VarNestedFromOuterFunctionScope)
					continue;
				if (ex.uniqueName().equals("FLEval.tuple"))
					continue;
				if (ex.uniqueName().equals("FLEval.field")) {
					HSIEBlock c1 = c.nestedCommands().get(1);
					if (c1 instanceof PushExternal) {
						PushExternal ofObj = (PushExternal) c1;
						PushString fld = (PushString)c.nestedCommands().get(2);
						if (ofObj.fn instanceof CardMember) {
							CardMember cm = (CardMember) ofObj.fn;
							if (cm.type instanceof RWObjectDefn) {
								RWObjectDefn od = (RWObjectDefn) cm.type;
								if (od.hasMethod(fld.sval.text)) {
									Type t = od.getMethod(fld.sval.text);
									c.pushAt(pc.location, 0, new PackageVar(pc.location, "FLEval.curry", null));
									c.removeAt(1);
									c.pushAt(pc.location, 1, new PackageVar(pc.location, "FLEval.method", null));
									c.pushAt(pc.location, 2, t.arity()+2);
								}
							}
						}
					}
					continue;
				}
				boolean scoping = (c instanceof ClosureCmd) && ((ClosureCmd)c).justScoping;
				Type t = tc.getTypeAsCtor(pc.location, ex.uniqueName());
				if (t.iam == WhatAmI.FUNCTION) {
					logger.debug("Considering applying curry to: " + ex + ": " + t.arity() + " " + (c.nestedCommands().size()-1) + (scoping?" with scoping":""));
					if (t.arity() > c.nestedCommands().size()-1) {
						c.pushAt(pc.location, 0, new PackageVar(null, "FLEval.curry", null));
						c.pushAt(pc.location, 2, t.arity());
						lookFrom  = 2;
					} else if (t.arity() > 0 && scoping) {
						int expected = t.arity() + c.nestedCommands().size()-1;
						c.pushAt(pc.location, 0, new PackageVar(null, "FLEval.curry", null));
						c.pushAt(pc.location, 2, expected);
						lookFrom = 2;
					} else if (t.arity() < c.nestedCommands().size()-1 && !scoping) {
						throw new UtilException("Have too many arguments for the function " + ex + " - error or need to replace f x y with (f x) y?");
					}
				}
			} else if (pc instanceof PushVar) { // the closure case, q.v.
			} else if (pc instanceof PushFunc) {
			} else {
				throw new UtilException("I don't think this can have passed typecheck");
			}
			// Go through all the arguments here (including the command) and see if any of them are in fact functions
			// that should be broken off into separate blocks and curried.
			// If we curried the function above, don't look again, but do look at the remaining arguments (hence lookFrom = 2)
			// To avoid contention, just build a list of things to rewrite here, and do that below ...
			for (int pos=lookFrom;pos<c.nestedCommands().size();pos++) {
				PushReturn pc2 = (PushReturn) c.nestedCommands().get(pos);
				if (pc2 instanceof PushExternal) {
					if (((PushExternal)pc2).fn instanceof CardFunction) {
						rewrites.add(new Rewrite(c, pos));
						continue;
					}
				}
			}
		}
		
		// If we collected any arguments that were 0-applied curried functions above, apply the closure now ...
		for (Rewrite r : rewrites) {
			PushExternal pc = (PushExternal) r.inside.nestedCommands().get(r.pos);
			ClosureCmd oclos = h.createClosure(pc.location);
			Type t = tc.getTypeAsCtor(pc.location, pc.fn.uniqueName());
			if (t.iam == WhatAmI.FUNCTION && t.arity() > 0) {
//				System.out.println("need to curry block for type = " + t);
				oclos.push(pc.location, new PackageVar(null, "FLEval.curry", null));
				oclos.push(pc.location, pc.fn);
				oclos.push(pc.location, t.arity());
			} else
				oclos.push(pc.location, pc.fn);
			r.inside.nestedCommands().set(r.pos, new PushVar(pc.location, new VarInSource(oclos.var, null, null)));
			Var myVar = ((ClosureCmd)r.inside).var;
			updateAllReturnCommands(h, myVar, oclos.var);
		}
	}

	private void updateAllReturnCommands(HSIEBlock h, Var before, Var newClos) {
		for (HSIEBlock x : h.nestedCommands()) {
			if (x instanceof PushVar)
				addClosureBefore((PushVar)x, before, newClos);
			updateAllReturnCommands(x, before, newClos);
		}
	}

	protected void addClosureBefore(PushVar rc, Var before, Var newClos) {
//		System.out.println("Adding " + newClos + " to " + rc + " before " + before);
		int at = -1;
		if (rc.var.var == before) {
			rc.deps.add(new VarInSource(newClos, null, null));
		} else {
			for (int i=0;i<rc.deps.size();i++)
				if (rc.deps.get(i).var == before) {
					at = i;
					break;
				}
			if (at == -1)
				throw new UtilException("Did not find " + before + " in " + rc.deps);
			rc.deps.add(at, new VarInSource(newClos, null, null));
		}
	}

}
