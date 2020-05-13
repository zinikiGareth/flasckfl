package org.flasck.flas.tc3;

import org.flasck.flas.repository.LoadBuiltins;
import org.zinutils.exceptions.NotImplementedException;

public class TypeHelpers {
	public static boolean isPrimitive(Type type) {
		if (type instanceof Primitive)
			return true;
		else
			return false;
	}

	public static boolean isList(Type type) {
		if (!(type instanceof PolyInstance))
			return false;
		
		PolyInstance pi = (PolyInstance) type;
		if (!pi.struct().equals(LoadBuiltins.list))
			return false;
		
		return true;
	}

	public static boolean isListString(Type type) {
		if (!(type instanceof PolyInstance))
			return false;
		
		PolyInstance pi = (PolyInstance) type;
		if (!pi.struct().equals(LoadBuiltins.list))
			return false;
		
		Type ty = pi.getPolys().get(0);
		if (!ty.equals(LoadBuiltins.string))
			return false;
		
		return true;
	}

	public static Type extractListPoly(Type etype) {
		if (!isList(etype))
			throw new NotImplementedException("not a list");
		return ((PolyInstance)etype).getPolys().get(0);
	}

}
