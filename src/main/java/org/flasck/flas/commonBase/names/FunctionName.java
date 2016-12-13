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
	private final CSName csName;
	
	private FunctionName(InputPosition location, CodeType codeType, PackageName pkg, CardName card, CSName csName, AreaName area, String name) {
		this.location = location;
		this.codeType = codeType;
		this.inPkg = pkg;
		this.inCard = card;
		this.csName = csName;
		this.area = area;
		this.name = name;
	}

	public static FunctionName functionKind(InputPosition location, CodeType codeType, PackageName pkg, CardName inCard, String name) {
		return new FunctionName(location, codeType, pkg, inCard, null, null, name);
	}
	
	public static FunctionName function(InputPosition location, PackageName pkg, String name) {
		return new FunctionName(location, CodeType.FUNCTION, pkg, null, null, null, name);
	}

	public static FunctionName functionInCardContext(InputPosition location, CardName inCard, String name) {
		return new FunctionName(location, CodeType.CARD, inCard.pkg, inCard, null, null, name);
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
		if (area != null) {
			return area.jsName() + "." + name;
		}
		else if (csName != null) {
			return csName.jsName() + "." + name;
		}
		else if (inCard != null) {
			return inCard.jsName() + "." + name;
		}
		else
			return ((inPkg!=null && inPkg.jsName() != null && inPkg.jsName().length() > 0)?inPkg.jsName()+".":"")+name;
	}

	public String toString() {
		throw new UtilException("Do not call toString() on a FunctionName; call jsName(), javaName() or other appropriate method");
	}
}
