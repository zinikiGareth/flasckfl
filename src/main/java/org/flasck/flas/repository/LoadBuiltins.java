package org.flasck.flas.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Primitive;

public class LoadBuiltins {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");
	public static final Primitive number = new Primitive("Number");
	public static final Primitive string = new Primitive("String");
	public static final StructDefn nil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);

	public static void applyTo(Repository repository) {
		
		// Types
		new BuiltinRepositoryEntry("Any").loadInto(repository);
		new BuiltinRepositoryEntry("Card").loadInto(repository);
		new BuiltinRepositoryEntry("Croset").loadInto(repository);
		new BuiltinRepositoryEntry("List").loadInto(repository);
		new BuiltinRepositoryEntry("Map").loadInto(repository);
		new BuiltinRepositoryEntry("Type").loadInto(repository);
		
		repository.addEntry(number.name(), number);
		repository.addEntry(string.name(), string);
		repository.addEntry(new SolidName(null, "[]"), nil);
		repository.newStruct(nil);
		repository.newStruct(new StructDefn(pos, FieldsType.STRUCT, null, "True", false));
		repository.newStruct(new StructDefn(pos, FieldsType.STRUCT, null, "False", false));

		// Operators
		FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		plus.bindType(new Apply(number, number, number));
		repository.functionDefn(plus);
		FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2);
		mul.bindType(new Apply(number, number, number));
		repository.functionDefn(mul);
		
		// dubious backward compatibility
		
		new BuiltinRepositoryEntry("Crokeys").loadInto(repository);
		new BuiltinRepositoryEntry("Id").loadInto(repository);
	}

}
