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
	public final String name;
	public final CardName inCard;
	public final NameOfThing inContext;
	
	private FunctionName(InputPosition location, CodeType codeType, NameOfThing cxt, String name) {
		this.location = location;
		this.codeType = codeType;
		this.name = name;
		this.inContext = cxt;
		
		// This wants to go away and be recovered from inContext.containingCard()
		this.inCard = cxt == null? null: cxt.containingCard();
	}

	@Deprecated // I would like the caller of this to think more carefully and call the correct method ...
	public static FunctionName functionKind(InputPosition location, CodeType codeType, NameOfThing pkg, String name) {
		return new FunctionName(location, codeType, pkg, name);
	}
	
	public static FunctionName function(InputPosition location, NameOfThing pkg, String name) {
		return new FunctionName(location, CodeType.FUNCTION, pkg, name);
	}

	public static FunctionName functionInCardContext(InputPosition location, CardName inCard, String name) {
		return new FunctionName(location, CodeType.CARD, inCard, name);
	}

	public static FunctionName contractMethod(InputPosition location, CSName csName, String name) {
		return new FunctionName(location, CodeType.CONTRACT, csName, name);
	}
	
	public static FunctionName eventMethod(InputPosition location, CardName cardName, String name) {
		return new FunctionName(location, CodeType.EVENTHANDLER, cardName, name);
	}

	public static FunctionName areaMethod(InputPosition location, AreaName areaName, String fnName) {
		return new FunctionName(location, CodeType.AREA, areaName, fnName);
	}

	public CardName containingCard() {
		return inContext.containingCard();
	}

	public String jsName() {
		if (inContext == null || inContext.jsName() == null || inContext.jsName().length() == 0)
			return name;
		else
			return inContext.jsName() + "." + name;
//		if (area != null) {
//			return area.jsName() + "." + name;
//		}
//		else if (csName != null) {
//			return csName.jsName() + "." + name;
//		}
//		else if (inCard != null) {
//			return inCard.jsName() + "." + name;
//		}
//		else
//			return ((inPkg!=null && inPkg.jsName() != null && inPkg.jsName().length() > 0)?inPkg.jsName()+".":"")+name;
	}

	public String toString() {
		throw new UtilException("Do not call toString() on a FunctionName; call jsName(), javaName() or other appropriate method");
	}
}
