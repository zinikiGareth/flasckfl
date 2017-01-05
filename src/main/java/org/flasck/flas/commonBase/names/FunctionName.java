package org.flasck.flas.commonBase.names;


import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.HandlerName;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

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
public class FunctionName implements NameOfThing, Comparable<FunctionName> {
	public final InputPosition location;
	public final CodeType codeType;
	public final String name;
	public final NameOfThing inContext;
	
	private FunctionName(InputPosition location, CodeType codeType, NameOfThing cxt, String name) {
		this.location = location;
		this.codeType = codeType;
		this.name = name;
		this.inContext = cxt;
	}

	public static FunctionName function(InputPosition location, NameOfThing pkg, String name) {
		return new FunctionName(location, CodeType.FUNCTION, pkg, name);
	}

	public static FunctionName functionInCardContext(InputPosition location, CardName card, String name) {
		return new FunctionName(location, CodeType.CARD, card, name);
	}

	public static FunctionName eventMethod(InputPosition location, CardName cardName, String name) {
		return new FunctionName(location, CodeType.EVENTHANDLER, cardName, name);
	}

	public static FunctionName contractDecl(InputPosition location, SolidName contractName, String name) {
		return new FunctionName(location, CodeType.DECL, contractName, name);
	}

	public static FunctionName contractMethod(InputPosition location, CSName csName, String name) {
		return new FunctionName(location, CodeType.CONTRACT, csName, name);
	}

	public static FunctionName serviceMethod(InputPosition location, CSName csName, String name) {
		return new FunctionName(location, CodeType.SERVICE, csName, name);
	}
	
	public static FunctionName handlerMethod(InputPosition location, HandlerName csName, String name) {
		return new FunctionName(location, CodeType.HANDLER, csName, name);
	}
	
	public static FunctionName areaMethod(InputPosition location, AreaName areaName, String fnName) {
		return new FunctionName(location, CodeType.AREA, areaName, fnName);
	}

	public CardName containingCard() {
		if (inContext == null)
			return null;
		return inContext.containingCard();
	}

	public String uniqueName() {
		if (inContext == null || inContext.uniqueName() == null || inContext.uniqueName().length() == 0)
			return name;
		else
			return inContext.uniqueName() + "." + name;
	}
	
	public String jsName() {
		if (inContext == null || inContext.jsName() == null || inContext.jsName().length() == 0)
			return name;
		else
			return inContext.jsName() + "." + name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionName))
			return false;
		return compareTo((FunctionName) obj) == 0;
	}
	
	public int compareTo(FunctionName other) {
		int cs = 0;
		if (inContext != null && other.inContext == null)
			return -1;
		else if (inContext == null && other.inContext != null)
			return 1;
		else if (inContext != null && other.inContext != null)
			cs = inContext.compareTo(other.inContext);
		if (cs != 0)
			return cs;
		return name.compareTo(other.name);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof FunctionName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((FunctionName)other);
	}

	public String toString() {
		throw new UtilException("Do not call toString() on a FunctionName; call jsName(), javaName() or other appropriate method");
	}

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}
}
