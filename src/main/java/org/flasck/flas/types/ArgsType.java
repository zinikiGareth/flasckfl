package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class ArgsType extends Type {
	protected final List<Type> args;

	public ArgsType(InputPosition loc, WhatAmI wai, List<Type> args) {
		super(loc, wai);
		this.args = args;
	}

	public Type arg(int i) {
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Can only ask for the argument of a function or tuple");
		return args.get(i);
	}
	
	protected void showArgs(StringBuilder sb, String withSep) {
		String sep = "";
		for (Type t : args) {
			if (t == null) {
				sb.append("--NULL--");
				return;
			}
			sb.append(sep);
			sep = withSep;
			if (iam == WhatAmI.FUNCTION && t.iam == WhatAmI.FUNCTION) {
				sb.append("(");
				t.show(sb);
				sb.append(")");
			} else
				t.show(sb);
		}
	}
}
