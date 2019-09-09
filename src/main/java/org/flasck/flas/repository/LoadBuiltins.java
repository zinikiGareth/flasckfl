package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;

public class LoadBuiltins {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");

	public static void applyTo(Repository repository) {
		
		// Types
		new BuiltinRepositoryEntry("Any").loadInto(repository);
		new BuiltinRepositoryEntry("Card").loadInto(repository);
		new BuiltinRepositoryEntry("Croset").loadInto(repository);
		new BuiltinRepositoryEntry("List").loadInto(repository);
		new BuiltinRepositoryEntry("Map").loadInto(repository);
		repository.newStruct(new StructDefn(pos , FieldsType.STRUCT, null, "Nil", false));
		new BuiltinRepositoryEntry.Type("Number", 0).loadInto(repository);
		new BuiltinRepositoryEntry.Type("String", 0).loadInto(repository);
		new BuiltinRepositoryEntry("Type").loadInto(repository);
		
		// Operators
		new BuiltinRepositoryEntry.Op("+", 2).loadInto(repository);
		new BuiltinRepositoryEntry.Op("*", 2).loadInto(repository);
		
		// dubious backward compatibility
		
		new BuiltinRepositoryEntry("Crokeys").loadInto(repository);
		new BuiltinRepositoryEntry("Id").loadInto(repository);
	}

}
