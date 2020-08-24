package org.flasck.flas.commonBase.names;

import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.jvm.J;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.UtilException;

public class FunctionName implements NameOfThing, Comparable<NameOfThing> {
	public final InputPosition location;
	public final String name;
	public final NameOfThing inContext;
	private final static Map<String, String> bimap = new HashMap<>();
	{
		bimap.put("*", "mul");
		bimap.put("++", "strAppend");
	}
	
	private FunctionName(InputPosition location, NameOfThing cxt, String name) {
		this.location = location;
		this.name = name;
		this.inContext = cxt;
	}

	@Override
	public String baseName() {
		return name;
	}
	
	public static FunctionName function(InputPosition location, NameOfThing pkg, String name) {
		return new FunctionName(location, pkg, name);
	}

	public static FunctionName caseName(FunctionName inside, int cs) {
		return new FunctionName(inside.location, inside, "_" + cs);
	}

	public static FunctionName eventMethod(InputPosition location, NameOfThing cardName, String name) {
		return new FunctionName(location, cardName, name);
	}

	public static FunctionName contractDecl(InputPosition location, SolidName contractName, String name) {
		return new FunctionName(location, contractName, name);
	}

	public static FunctionName contractMethod(InputPosition location, NameOfThing ctr, String name) {
		return new FunctionName(location, ctr, name);
	}

	public static FunctionName handlerMethod(InputPosition location, HandlerName hn, String name) {
		return new FunctionName(location, hn, name);
	}
	
	public static FunctionName objectMethod(InputPosition location, NameOfThing on, String name) {
		return new FunctionName(location, on, name);
	}

	public static FunctionName objectCtor(InputPosition location, SolidName on, String name) {
		return new FunctionName(location, on, "_ctor_" + name);
	}

	public static FunctionName standaloneMethod(InputPosition location, NameOfThing pkg, String name) {
		return new FunctionName(location, pkg, name);
	}
	
	@Override
	public NameOfThing container() {
		return inContext;
	}

	public PackageName packageName() {
		NameOfThing ret = inContext;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		throw new RuntimeException("No PackageName found");
	}
	
	public NameOfThing containingCard() {
		if (inContext == null)
			return null;
		return inContext.containingCard();
	}

	@Override
	public String javaName() {
		if (inContext == null)
			return J.FLEVAL + "." + name;
		else if (inContext instanceof FunctionName)
			return inContext.javaName() + "_" + name;
		else
			return inContext.javaName() + "." + name;
	}

	@Override
	public String javaClassName() {
		if (inContext == null)
			return J.FLEVAL;
		else if (inContext instanceof FunctionName)
			return inContext.javaClassName();
		else
			return inContext.javaName();
	}

	public String javaMethodName() {
		if (inContext instanceof FunctionName)
			return ((FunctionName) inContext).javaMethodName() + "_" + name;
		else
			return name;
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
	public String jsUName() {
		throw new UtilException("I don't think so");
	}

	public String jsPName() {
		if (inContext instanceof FunctionName)
			return ((FunctionName) inContext).jsPName() + "." + name;
		else if (inContext == null || inContext instanceof PackageName)
			return inContext.jsName() + "." + name;
		else
			return inContext.jsName() + ".prototype." + name;
	}

	@Override
	public String javaPackageName() {
		return inContext.javaName();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionName))
			return false;
		return compareTo((FunctionName) obj) == 0;
	}
	
	public int compareTo(NameOfThing other) {
		return uniqueName().compareTo(other.uniqueName());
	}

	public String toString() {
		// Don't use this in code! It should only be used for debug
		return "Fn[" + uniqueName() + "]";
	}

	public boolean isUnitTest() {
		if (inContext instanceof UnitTestName)
			return true;
		if (inContext instanceof UnitTestFileName)
			return true;
		// are there other cases?
		return false;
	}

	public NameOfThing wrappingObject() {
		if (inContext == null || inContext instanceof PackageName)
			return null;
		else if (inContext instanceof FunctionName)
			return ((FunctionName)inContext).wrappingObject();
		else if (inContext instanceof CardName || inContext instanceof HandlerName || inContext instanceof CSName || inContext instanceof ObjectName)
			return inContext;
		else
			throw new HaventConsideredThisException("where is this function?");
	}
}
