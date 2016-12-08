package org.flasck.flas.rewrittenForm;

import org.zinutils.exceptions.UtilException;

// This wants to grow to take over the responsibility of knowing:
//  * The package
//  * The class (if any)
//  * The code type
//  * Any area or contract/service name
//  * The method name
// And be able to return jsName or javaName, or class name or basic name ...

// At the same time, I would like the function to take ever more advantage of this by:
//  * storing this function name
//  * offering the different options for the name (jsName, className, basicName, etc)
//  * eventually hiding the "name" var ...
public class FunctionName {
	private final String jsname;

	public FunctionName(String s) {
		this.jsname = s;
	}
	public String jsName() {
		return jsname;
	}

	public String toString() {
		throw new UtilException("Yo!");
	}
}
