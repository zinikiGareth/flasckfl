package org.flasck.flas.parsedForm;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.zinutils.exceptions.UtilException;

public class FunctionIntro {
	public final String name;
	public final List<Object> args;

	public FunctionIntro(String name, List<Object> args) {
		this.name = name;
		this.args = args;
	}
	
	public Set<String> allVars() {
		Set<String> ret = new TreeSet<String>();
		gatherVars(ret);
		return ret;
	}
	
	public void gatherVars(Set<String> into) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern)
				into.add(((VarPattern)arg).var);
			else if (arg instanceof ConstructorMatch)
				gatherCtor(into, (ConstructorMatch) arg);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern)
				into.add(((TypedPattern)arg).var);
			else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(Set<String> into, ConstructorMatch cm) {
		for (Field x : cm.args) {
			if (x.patt instanceof VarPattern)
				into.add(((VarPattern)x.patt).var);
			else if (x.patt instanceof ConstructorMatch)
				gatherCtor(into, (ConstructorMatch)x.patt);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not gathering vars from " + x.patt.getClass());
		}
	}
	
	@Override
	public String toString() {
		return "FI[" + name + "/" + args.size() + "]";
	}
}
