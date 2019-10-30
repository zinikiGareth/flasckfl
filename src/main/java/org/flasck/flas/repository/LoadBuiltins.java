package org.flasck.flas.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Primitive;

public class LoadBuiltins {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");
	public static final Primitive any = new Primitive(pos, "Any");
	public static final Primitive number = new Primitive(pos, "Number");
	public static final Primitive string = new Primitive(pos, "String");
	public static final StructDefn falseT = new StructDefn(pos, FieldsType.STRUCT, null, "False", false);
	public static final StructDefn trueT = new StructDefn(pos, FieldsType.STRUCT, null, "True", false);
	public static final UnionTypeDefn bool = new UnionTypeDefn(pos, false, new SolidName(null, "Boolean"));
	public static final StructDefn nil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
	public static final StructDefn cons = new StructDefn(pos, FieldsType.STRUCT, null, "Cons", false, new PolyType(pos, "A"));
	public static final UnionTypeDefn list = new UnionTypeDefn(pos, false, new SolidName(null, "List"), new PolyType(pos, "A"));
	public static final StructDefn error = new StructDefn(pos, FieldsType.STRUCT, null, "Error", false);
	public static final CurryArgument ca = new CurryArgument(pos);
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2);
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2);
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2);
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1);

	static {
		// add fields to structs
		cons.addField(new StructField(pos, false, new TypeReference(pos, "A"), "head"));
		cons.addField(new StructField(pos, false, new TypeReference(pos, "List", new TypeReference(pos, "A")), "tail"));
		error.addField(new StructField(pos, false, new TypeReference(pos, "String"), "message"));

		// add cases to unions
		bool.addCase(new TypeReference(pos, "False").bind(falseT));
		bool.addCase(new TypeReference(pos, "True").bind(trueT));
		list.addCase(new TypeReference(pos, "Nil").bind(nil));
		list.addCase(new TypeReference(pos, "Cons", new TypeReference(pos, "A")).bind(cons));
		
		// specify function types
		plus.bindType(new Apply(number, number, number));
		minus.bindType(new Apply(number, number, number));
		mul.bindType(new Apply(number, number, number));
		div.bindType(new Apply(number, number, number));
		length.bindType(new Apply(list, number));
	}
	
	public static void applyTo(Repository repository) {
		// Types
		new BuiltinRepositoryEntry("Card").loadInto(repository);
		new BuiltinRepositoryEntry("Croset").loadInto(repository);
		new BuiltinRepositoryEntry("Map").loadInto(repository);
		new BuiltinRepositoryEntry("Type").loadInto(repository);
		
		repository.addEntry(any.name(), any);
		repository.addEntry(number.name(), number);
		repository.addEntry(string.name(), string);
		repository.addEntry(falseT.name(), falseT);
		repository.addEntry(trueT.name(), trueT);
		repository.addEntry(bool.name(), bool);
		repository.addEntry(new SolidName(null, "[]"), nil);
		repository.addEntry(new SolidName(null, "_"), ca);
		repository.newStruct(nil);
		repository.newStruct(cons);
		repository.newUnion(list);
		repository.newStruct(error);

		repository.functionDefn(plus);
		repository.functionDefn(minus);
		repository.functionDefn(mul);
		repository.functionDefn(div);
		repository.functionDefn(length);
		
		// dubious backward compatibility
		
		new BuiltinRepositoryEntry("Crokeys").loadInto(repository);
		new BuiltinRepositoryEntry("Id").loadInto(repository);
	}

}
