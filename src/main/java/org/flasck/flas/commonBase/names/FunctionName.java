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
	public final CardName inCard;
	private final AreaName area;
	
	// an old hack
	private final String jsname;
	private static CardName inCard2;

	@Deprecated // this is  too simplistic but was good for compatibility and small changes
	public FunctionName(String s) {
		this.location = null;
		this.codeType = CodeType.FUNCTION; // a random guess, but not used yet
		this.inPkg = null;
		this.inCard = null;
		this.area = null;
		this.jsname = s;
	}

	// This is the one true constructor that should be called by a bunch of statics
	private FunctionName(InputPosition location, CodeType codeType, PackageName pkg, CardName card, CSName csName, AreaName area, String name) {
		this.location = location;
		this.codeType = codeType;
		this.inPkg = pkg;
		this.inCard = card;
		this.area = area;
		this.name = name;
		if (area != null) {
			this.jsname = area.jsName() + "." + name;
		}
		else if (csName != null) {
			this.jsname = csName.jsName() + "." + name;
			System.out.println("jsname = " + this.jsname);
		}
		else if (card != null)
			this.jsname = card.jsName() + "." + name;
		else
			this.jsname = ((inPkg!=null && inPkg.jsName() != null && inPkg.jsName().length() > 0)?inPkg.jsName()+".":"")+name;
	}

	// I think the CodeType for this should just be "FUNCTION"; and you should use another type for other things
	// But the parser creates FI's with a "kind", so we kind of need it
	// Maybe we should have a "generic" or something
	public static FunctionName function(InputPosition location, CodeType codeType, PackageName pkg, CardName inCard, String name) {
		return new FunctionName(location, codeType, pkg, inCard, null, null, name);
	}
	
	public static FunctionName contractMethod(InputPosition location, CodeType kind, CSName csName, String name) {
		return new FunctionName(location, kind, csName.pkgName(), csName.cardName(), csName, null, name);
	}
	
	public static FunctionName eventMethod(InputPosition location, CodeType kind, CardName cardName, String name) {
		return new FunctionName(location, kind, cardName.pkg, cardName, null, null, name);
	}

	public static FunctionName areaMethod(InputPosition location, AreaName areaName, String fnName) {
		return new FunctionName(location, CodeType.AREA, areaName.cardName.pkg, areaName.cardName, null, areaName, fnName);
	}

	public String jsName() {
		return jsname;
	}

	public String toString() {
		throw new UtilException("Yo!");
	}
}
