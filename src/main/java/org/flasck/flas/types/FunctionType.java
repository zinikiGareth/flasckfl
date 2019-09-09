package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

@Deprecated
public class FunctionType extends ArgsType {

	public FunctionType(InputPosition loc, List<Type> args) {
		super(loc, args);
		if (args.size() < 1)
			throw new UtilException("Can you have a function/method type with less than 1 arg? (the result)  Really?");
	}

	public int arity() {
		return args.size() - 1;
	}
	
	protected void show(StringBuilder sb) {
		if (args.size() == 1)
			sb.append("->");
		showArgs(sb, "->");
	}
}
