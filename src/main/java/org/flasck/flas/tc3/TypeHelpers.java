package org.flasck.flas.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
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

	public static boolean isListLike(Type type) {
		return isList(type) || (type instanceof ObjectDefn && ((ObjectDefn)type).name().uniqueName().equals("Crobag"));
	}

	public static boolean isListString(Type type) {
		if (!(type instanceof PolyInstance))
			return false;
		
		PolyInstance pi = (PolyInstance) type;
		if (!pi.struct().equals(LoadBuiltins.list))
			return false;
		
		Type ty = pi.polys().get(0);
		if (!ty.equals(LoadBuiltins.string))
			return false;
		
		return true;
	}

	public static Type extractListPoly(Type etype) {
		if (!isList(etype))
			throw new NotImplementedException("not a list");
		return ((PolyInstance)etype).polys().get(0);
	}

	public static boolean isListMessage(InputPosition pos, Type check) {
		if (check instanceof ErrorType) {
			return false;
		}

		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			return true;
		}
		
		if (check instanceof Apply) {
			Apply ac = (Apply) check;
			if (ac.argCount() == 1) {
				if (ac.tys.get(0) == LoadBuiltins.contract)
					check = ac.tys.get(1);
			}
		}
		
		// a poly list is fine (cons or list) as long as the type is some kind of Message
		if (check instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) check;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list) {
				Type pv = pi.polys().get(0);
				if (LoadBuiltins.message.incorporates(pos, pv)) {
					return true;
				}
			} else {
				return false;
			}
		}
		if (LoadBuiltins.listMessages.incorporates(pos, check)) {
			return true;
		} else if (LoadBuiltins.message.incorporates(pos, check)) {
			return true;
		}

		return false;
	}

	public static boolean isEntity(NamedType arg) {
		if (arg instanceof StructDefn) {
			StructDefn s = (StructDefn) arg;
			return s.type == FieldsType.ENTITY;
		} else
			return false;
	}

}
