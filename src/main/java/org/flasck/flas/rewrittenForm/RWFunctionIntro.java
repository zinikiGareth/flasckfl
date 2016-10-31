package org.flasck.flas.rewrittenForm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class RWFunctionIntro {
	public final InputPosition location;
	public final String name;
	public final List<Object> args;
	public final Map<String, LocalVar> vars;

	public RWFunctionIntro(InputPosition location, String name, List<Object> args, Map<String, LocalVar> vars) {
		this.location = location;
		if (location == null)
			throw new UtilException("Null location");
		this.name = name;
		if (args == null)
			throw new UtilException("Cannot pass in null args");
		this.args = args;
		this.vars = (vars == null? new HashMap<>(): vars);
	}
	
	@Override
	public String toString() {
		return "FI[" + name + "/" + args.size() + "]";
	}
}
