package org.flasck.flas.commonBase.names;

import org.flasck.flas.blockForm.InputPosition;
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
	public final InputPosition location;
	public final CodeType codeType;
	private final PackageName inPkg;
	private String name;
	
	// an old hack
	private final String jsname;

	@Deprecated // this is  too simplistic but was good for compatibility and small changes
	public FunctionName(String s) {
		this.location = null;
		this.codeType = CodeType.FUNCTION; // a random guess, but not used yet
		this.inPkg = null;
		this.jsname = s;
	}

	// This is the one true constructor that should be called by a bunch of statics
	public FunctionName(InputPosition location, CodeType codeType, PackageName pkg, String name) {
		this.location = location;
		this.codeType = codeType;
		this.inPkg = pkg;
		this.name = name;
		this.jsname = ((inPkg!=null && inPkg.jsName() != null && inPkg.jsName().length() > 0)?inPkg.jsName()+".":"")+name;
	}

	public static FunctionName function(InputPosition location, CodeType codeType, PackageName pkg, String name) {
		return new FunctionName(location, codeType, pkg, name);
	}

	public String jsName() {
		return jsname;
	}

	public String toString() {
		throw new UtilException("Yo!");
	}
}
