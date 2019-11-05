package org.flasck.flas.repository;

import java.util.Arrays;

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
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;

public class LoadBuiltins {
	private static InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");
	
	// Type References used here ...
	private static final TypeReference polyATR = new TypeReference(pos, "A");
	private static final TypeReference anyTR = new TypeReference(pos, "Any");
	private static final TypeReference contractTR = new TypeReference(pos, "Contract");
	private static final TypeReference stringTR = new TypeReference(pos, "String");
	private static final TypeReference falseTR = new TypeReference(pos, "False");
	private static final TypeReference trueTR = new TypeReference(pos, "True"); 
	private static final TypeReference nilTR = new TypeReference(pos, "Nil"); 
	private static final TypeReference consATR = new TypeReference(pos, "Cons", polyATR);
	private static final TypeReference listATR = new TypeReference(pos, "List", polyATR);
	private static final TypeReference listAnyTR = new TypeReference(pos, "List", anyTR);
	private static final TypeReference debugTR = new TypeReference(pos, "Debug");
	private static final TypeReference sendTR = new TypeReference(pos, "Send");
	
	// "Primitive" types
	public static final Primitive any = new Primitive(pos, "Any");
	public static final Primitive contract = new Primitive(pos, "Contract");
	public static final StructDefn error = new StructDefn(pos, FieldsType.STRUCT, null, "Error", false);
	public static final Primitive number = new Primitive(pos, "Number");
	public static final Primitive string = new Primitive(pos, "String");

	// Booleans
	public static final StructDefn falseT = new StructDefn(pos, FieldsType.STRUCT, null, "False", false);
	public static final StructDefn trueT = new StructDefn(pos, FieldsType.STRUCT, null, "True", false);
	public static final UnionTypeDefn bool = new UnionTypeDefn(pos, false, new SolidName(null, "Boolean"));

	// Lists
	public static final StructDefn nil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
	public static final StructDefn cons = new StructDefn(pos, FieldsType.STRUCT, null, "Cons", false, new PolyType(pos, "A"));
	public static final UnionTypeDefn list = new UnionTypeDefn(pos, false, new SolidName(null, "List"), new PolyType(pos, "A"));
	public static final PolyInstance listAny = new PolyInstance(list, Arrays.asList(any));
	
	// Messages
	public static final StructDefn debug = new StructDefn(pos, FieldsType.STRUCT, null, "Debug", false);
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	public static final UnionTypeDefn message = new UnionTypeDefn(pos, false, new SolidName(null, "Message"));
	
	// Builtin operators
	public static final FunctionDefinition isEqual = new FunctionDefinition(FunctionName.function(pos, null, "=="), 2);
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2);
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2);
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2);
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1);

	// This is a weird thing but it seems to fit best here
	public static final CurryArgument ca = new CurryArgument(pos);

	static {
		// bind TRs
		anyTR.bind(any);
		contractTR.bind(contract);
		stringTR.bind(string);
		consATR.bind(cons);
		listATR.bind(list);
		listAnyTR.bind(listAny);
		falseTR.bind(falseT);
		trueTR.bind(trueT);
		nilTR.bind(nil);
		debugTR.bind(debug);
	
		// add fields to structs
		error.addField(new StructField(pos, false, stringTR, "message"));
		cons.addField(new StructField(pos, false, polyATR, "head"));
		cons.addField(new StructField(pos, false, listATR, "tail"));
		debug.addField(new StructField(pos, false, stringTR, "message"));
		send.addField(new StructField(pos, false, contractTR, "sendto"));
		send.addField(new StructField(pos, false, stringTR, "meth"));
		send.addField(new StructField(pos, false, listAnyTR, "args"));

		// add cases to unions
		bool.addCase(falseTR);
		bool.addCase(trueTR);
		list.addCase(nilTR);
		list.addCase(consATR);
		message.addCase(debugTR);
		message.addCase(sendTR);
		
		// specify function types
		{
			Type pa = new PolyType(pos, "A");
			isEqual.bindType(new Apply(pa, pa, bool));
		}
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
		repository.addEntry(contract.name(), contract);
		repository.newStruct(error);
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
		
		repository.newStruct(debug);
		repository.newStruct(send);
		repository.newUnion(message);

		repository.functionDefn(isEqual);
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
