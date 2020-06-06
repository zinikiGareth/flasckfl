package org.flasck.flas.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.repository.LoadBuiltins;
import org.zinutils.exceptions.NotImplementedException;

public class TypeHelpers {
	public static Type listMessage(InputPosition pos) {
		return new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
	}

	public static boolean isPrimitive(Type type) {
		if (type instanceof Primitive)
			return true;
		else
			return false;
	}

	public static boolean isList(Type type) {
		if (type == LoadBuiltins.nil)
			return true;
		
		if (!(type instanceof PolyInstance))
			return false;
		
		PolyInstance pi = (PolyInstance) type;
		if (pi.struct().equals(LoadBuiltins.list))
			return true;
		if (pi.struct().equals(LoadBuiltins.cons))
			return true;
		return false;
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

	public static boolean isListMessage(InputPosition pos, Type check) {
		if (check instanceof ErrorType) {
			return false;
		}

		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			return true;
		}
		
		if (check instanceof EnsureListMessage) {
			return true;
		}
		
		// a poly list is fine (cons or list) as long as the type is some kind of Message
		if (check instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) check;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				check = pi.getPolys().get(0);
			else {
				return false;
			}
		}
		if (LoadBuiltins.message.incorporates(pos, check)) {
			return true;
		}
		return false;
	}

}
