package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
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
public class FunctionName implements NameOfThing {
	private final CodeType codeType;
	private String inPkg;
	private String name;
	
	// an old hack
	private final String jsname;

	@Deprecated // this is  too simplistic but was good for compatibility and small changes
	public FunctionName(String s) {
		this.codeType = CodeType.FUNCTION; // a random guess, but not used yet
		this.jsname = s;
	}

	// This is the one true constructor that should be called by a bunch of statics
	public FunctionName(CodeType codeType, String inPkg, String name) {
		this.codeType = codeType;
		this.inPkg = inPkg;
		this.name = name;
		this.jsname = (inPkg!=null?inPkg+".":"")+name;
	}

	public static FunctionName function(String inPkg, String name) {
		return new FunctionName(CodeType.FUNCTION, inPkg, name);
	}

	public String jsName() {
		return jsname;
	}

	public String toString() {
		throw new UtilException("Yo!");
	}
}
