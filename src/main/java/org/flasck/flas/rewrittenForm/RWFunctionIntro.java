package org.flasck.flas.rewrittenForm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.zinutils.exceptions.UtilException;

public class RWFunctionIntro {
	public final InputPosition location;
	public final List<Object> args;
	public final Map<String, LocalVar> vars;
	public final FunctionName fnName;

	public RWFunctionIntro(InputPosition location, FunctionName name, List<Object> args, Map<String, LocalVar> vars) {
		this.location = location;
		if (location == null)
			throw new UtilException("Null location");
		if (args == null)
			throw new UtilException("Cannot pass in null args");
		this.args = args;
		this.vars = (vars == null? new HashMap<>(): vars);
		this.fnName = name;
	}

	@Override
	public String toString() {
		return "FI[" + fnName.uniqueName() + "/" + args.size() + "]";
	}
}
