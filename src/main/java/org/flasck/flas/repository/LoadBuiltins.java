package org.flasck.flas.repository;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.CurryArgument;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneDefn;
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

	public static Set<StandaloneDefn> allFunctions = new TreeSet<>();
	
	// Type References used here ...
	public static final TypeReference polyATR = new TypeReference(pos, "A");
	public static final TypeReference anyTR = new TypeReference(pos, "Any");
	public static final TypeReference contractTR = new TypeReference(pos, "Contract");
	public static final TypeReference stringTR = new TypeReference(pos, "String");
	public static final TypeReference numberTR = new TypeReference(pos, "Number");
	public static final TypeReference falseTR = new TypeReference(pos, "False");
	public static final TypeReference trueTR = new TypeReference(pos, "True"); 
	public static final TypeReference nilTR = new TypeReference(pos, "Nil"); 
	public static final TypeReference consATR = new TypeReference(pos, "Cons", polyATR);
	public static final TypeReference listATR = new TypeReference(pos, "List", polyATR);
	public static final TypeReference listAnyTR = new TypeReference(pos, "List", anyTR);
	public static final TypeReference debugTR = new TypeReference(pos, "Debug");
	public static final TypeReference sendTR = new TypeReference(pos, "Send");
	public static final TypeReference assignTR = new TypeReference(pos, "Assign");
	
	// "Primitive" types
	public static final PolyType polyA = new PolyType(pos, "A"); 
	public static final Primitive any = new Primitive(pos, "Any");
	// TODO: I think we want subclasses of Any called "Entity", "Deal", "Offer", etc
	// Not quite sure what etc. includes because I don't think "Primitive" and "Struct" hold any value
	// Entity is obviously useful because we use it in Data Contracts
	// Deal & Offer feel like they would come up in conversations about Commerce
	// Countables, Currency and ValueStore need to go here too
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
	public static final StructDefn cons = new StructDefn(pos, FieldsType.STRUCT, null, "Cons", false, polyA);
	public static final UnionTypeDefn list = new UnionTypeDefn(pos, false, new SolidName(null, "List"), polyA);
	public static final PolyInstance listAny = new PolyInstance(list, Arrays.asList(any));
	
	// Messages
	public static final StructDefn debug = new StructDefn(pos, FieldsType.STRUCT, null, "Debug", false);
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	public static final StructDefn assign = new StructDefn(pos, FieldsType.STRUCT, null, "Assign", false);
	public static final UnionTypeDefn message = new UnionTypeDefn(pos, false, new SolidName(null, "Message"));

	// The type "operator"
	private static StructDefn type = new StructDefn(pos, FieldsType.STRUCT, null, "Type", false);
	
	// Builtin operators
	public static final FunctionDefinition isEqual = new FunctionDefinition(FunctionName.function(pos, null, "=="), 2);
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2);
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2);
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2);
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1);
	public static final FunctionDefinition strlen = new FunctionDefinition(FunctionName.function(pos, null, "strlen"), 1);
	public static final FunctionDefinition concat = new FunctionDefinition(FunctionName.function(pos, null, "++"), 2);

	// This is a weird thing but it seems to fit best here
	public static final CurryArgument ca = new CurryArgument(pos);


	static {
		// bind TRs
		polyATR.bind(polyA);
		anyTR.bind(any);
		contractTR.bind(contract);
		stringTR.bind(string);
		numberTR.bind(number);
		consATR.bind(cons);
		listATR.bind(list);
		listAnyTR.bind(listAny);
		falseTR.bind(falseT);
		trueTR.bind(trueT);
		nilTR.bind(nil);
		debugTR.bind(debug);
		sendTR.bind(send);
		assignTR.bind(assign);
	
		// add fields to structs
		error.addField(new StructField(pos, false, stringTR, "message"));
		cons.addField(new StructField(pos, false, polyATR, "head"));
		cons.addField(new StructField(pos, false, listATR, "tail"));
		debug.addField(new StructField(pos, false, stringTR, "message"));
		send.addField(new StructField(pos, false, contractTR, "sendto"));
		send.addField(new StructField(pos, false, stringTR, "meth"));
		send.addField(new StructField(pos, false, listAnyTR, "args"));
		assign.addField(new StructField(pos, false, anyTR, "on"));
		assign.addField(new StructField(pos, false, stringTR, "fld"));
		assign.addField(new StructField(pos, false, anyTR, "value"));
		
		// is this ok as a string or should it be something else?
		type.addField(new StructField(pos, false, stringTR, "type"));

		// add cases to unions
		bool.addCase(falseTR);
		bool.addCase(trueTR);
		list.addCase(nilTR);
		list.addCase(consATR);
		message.addCase(debugTR);
		message.addCase(assignTR);
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
		strlen.bindType(new Apply(string, number));
		concat.bindType(new Apply(string, string, string));
		
		// add all current functions to list for dependency resolution
		allFunctions.add(plus);
		allFunctions.add(minus);
		allFunctions.add(mul);
		allFunctions.add(div);
		allFunctions.add(length);
		allFunctions.add(strlen);
		allFunctions.add(concat);
	}
	
	public static void applyTo(Repository repository) {
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
		repository.newStruct(assign);
		repository.newUnion(message);

		repository.functionDefn(isEqual);
		repository.functionDefn(plus);
		repository.functionDefn(minus);
		repository.functionDefn(mul);
		repository.functionDefn(div);
		repository.functionDefn(length);
		repository.functionDefn(strlen);
		repository.functionDefn(concat);

		// not yet thought through for backward compatibility
		StructDefn card = new StructDefn(pos, FieldsType.STRUCT, null, "Card", false);
		repository.newStruct(card);
		StructDefn croset = new StructDefn(pos, FieldsType.STRUCT, null, "Croset", false);
		repository.newStruct(croset);
		StructDefn map = new StructDefn(pos, FieldsType.STRUCT, null, "Map", false);
		repository.newStruct(map);
		repository.newStruct(type);


		// dubious backward compatibility

		StructDefn crokeys = new StructDefn(pos, FieldsType.STRUCT, null, "Crokeys", false);
		repository.newStruct(crokeys);
		StructDefn id = new StructDefn(pos, FieldsType.STRUCT, null, "Id", false);
		repository.newStruct(id);
	}
}
