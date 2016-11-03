package org.flasck.flas.newtypechecker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.zinutils.exceptions.UtilException;

public class TypeChecker2 {
	private final ErrorResult errors;
	private final Map<String, TypeInfo> localKnowledge = new HashMap<String, TypeInfo>();
	private int nextTv;
	
	public TypeChecker2(ErrorResult errors) {
		this.errors = errors;
	}

	public void populateTypes(Rewriter rewriter) {
		// TODO Auto-generated method stub
		
	}

	// Typecheck a set of HSIE forms in parallel ...
	public void typecheck(Set<HSIEForm> forms) {
		
		// initialize the state for doing the checking ...
		localKnowledge.clear();
		nextTv = 1;
		for (HSIEForm f : forms) {
			System.out.println("Checking type of " + f.fnName);
			localKnowledge.put(f.fnName, nextVar());
		}
		
		// now look at all the closures we have
		for (HSIEForm f : forms) {
			for (ClosureCmd c : f.closures()) {
				System.out.println("Need to check " + f.fnName + " " + c.var);
				HSIEBlock cmd = c.nestedCommands().get(0);
				if (cmd instanceof PushVar) {
					// this is a tricky case
				} else {
					TypeInfo ti = getTypeOf(cmd);
					if (ti == null)
						continue;
				}
			}
		}
		
		// and now look at the switching blocks
		for (HSIEForm f : forms) {
			processHSI(f, f);
		}		
	}

	private void processHSI(HSIEForm f, HSIEBlock blk) {
		for (HSIEBlock c : blk.nestedCommands()) {
			if (c instanceof Head || c instanceof ErrorCmd)
				continue;
			if (c instanceof Switch) {
				Switch sw = (Switch) c;
				// TODO: state with certainty that sw.ctor is an option for sw.var
				processHSI(f, sw);
			} else if (c instanceof BindCmd) {
				BindCmd b = (BindCmd)c;
				// TODO: set up the definition of b.var based on the type of the field that we've got
			} else if (c instanceof IFCmd) {
				IFCmd sw = (IFCmd) c;
				// TODO: state with certainty that Boolean is an option for sw.var (in this context, is a requirement - but what is the nature of a context?)
				processHSI(f, sw);
			} else if (c instanceof PushReturn) {
				PushReturn pr = (PushReturn) c;
				if (pr instanceof PushVar)
					System.out.println("Need to add a constraint to " + ((PushVar)pr).var);
				else
					System.out.println("Can return " + getTypeOf(pr));
			} else 
				System.out.println("Handle " + c);
		}
	}

	private TypeInfo getTypeOf(HSIEBlock cmd) {
		if (cmd instanceof PushExternal) {
			String name = ((PushExternal)cmd).fn.uniqueName();
			TypeInfo ret = localKnowledge.get(name);
			if (ret == null) {
				errors.message(cmd.location, "the name '" + name + "' cannot be resolved for typechecking");
			}
			return ret;
		} else
			throw new UtilException("Need to determine type of " + cmd.getClass());
	}

	private TypeVar nextVar() {
		return new TypeVar(nextTv++);
	}

}
