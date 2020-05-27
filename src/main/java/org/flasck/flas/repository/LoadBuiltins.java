package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Tuple;
import org.flasck.flas.tc3.Type;

public class LoadBuiltins {
	public static final InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");

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
	public static final TypeReference clickEventTR = new TypeReference(pos, "ClickEvent");
	public static final TypeReference callMeTR = new TypeReference(pos, "CallMe");
	
	// "Primitive" types
	public static final PolyType polyA = new PolyType(pos, new SolidName(null, "A")); 
	public static final Primitive any = new Primitive(pos, "Any");
	// TODO: I think we want subclasses of Any called "Entity", "Deal", "Offer", etc
	// Not quite sure what etc. includes because I don't think "Primitive" and "Struct" hold any value
	// Entity is obviously useful because we use it in Data Contracts
	// Deal & Offer feel like they would come up in conversations about Commerce
	// Countables, Currency and ValueStore need to go here too
	public static final Primitive contract = new Primitive(pos, "Contract"); // Should this be in 3 parts? (CONTRACT, SERVICE, HANDLER?)
	public static final StructDefn error = new StructDefn(pos, FieldsType.STRUCT, null, "Error", false);
	public static final Primitive number = new Primitive(pos, "Number");
	public static final Primitive string = new Primitive(pos, "String");
	public static final Type idempotentHandler = contract; // This may or may not be correct ...
	
	// This is another really weird thing ... it has arguments really, so needs to be parameterized a variable amount
	// Probably needs its own class to handle it properly
	public static final Tuple tuple = new Tuple(pos, "Tuple");

	// Booleans
	public static final StructDefn falseT = new StructDefn(pos, FieldsType.STRUCT, null, "False", false);
	public static final StructDefn trueT = new StructDefn(pos, FieldsType.STRUCT, null, "True", false);
	public static final UnionTypeDefn bool = new UnionTypeDefn(pos, false, new SolidName(null, "Boolean"));

	// Lists
	public static final StructDefn nil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
	public static final StructDefn cons = new StructDefn(pos, FieldsType.STRUCT, null, "Cons", false, polyA);
	public static final UnionTypeDefn list = new UnionTypeDefn(pos, false, new SolidName(null, "List"), polyA);
	public static final PolyInstance listAny = new PolyInstance(pos, list, Arrays.asList(any));

	// Crobags
	public static final ObjectDefn crobag = new ObjectDefn(pos, pos, new SolidName(null, "Crobag"), false, Arrays.asList(polyA));
	
	// Messages
	public static final StructDefn debug = new StructDefn(pos, FieldsType.STRUCT, null, "Debug", false);
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	public static final StructDefn assign = new StructDefn(pos, FieldsType.STRUCT, null, "Assign", false);
	public static final UnionTypeDefn message = new UnionTypeDefn(pos, false, new SolidName(null, "Message"));

	// Events
	public static final StructDefn clickEvent = new StructDefn(pos, FieldsType.STRUCT, null, "ClickEvent", false);
	public static final UnionTypeDefn event = new UnionTypeDefn(pos, false, new SolidName(null, "Event"));
	
	// The type "operator"
	private static StructDefn type = new StructDefn(pos, FieldsType.STRUCT, null, "Type", false);

	// The function that probes state in UDDs
	public static UnresolvedVar probeState = new UnresolvedVar(pos, "_probe_state");
	public static UnresolvedVar getUnderlying = new UnresolvedVar(pos, "_underlying");

	// Contracts
	public static final ContractDecl lifecycle = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(null, "Lifecycle"), false);
	public static final ContractDecl repeater = new ContractDecl(pos, pos, ContractType.SERVICE, new SolidName(null, "Repeater"), false);
	public static final ContractDecl callMeHandler = new ContractDecl(pos, pos, ContractType.HANDLER, new SolidName(null, "CallMe"), false);
	
	// Builtin operators
	public static final FunctionDefinition isType = new FunctionDefinition(FunctionName.function(pos, null, "istype"), 2, false);
	public static final FunctionDefinition isEqual = new FunctionDefinition(FunctionName.function(pos, null, "=="), 2, false);
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2, false);
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2, false);
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2, false);
	public static final FunctionDefinition not = new FunctionDefinition(FunctionName.function(pos, null, "!"), 1, false);
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1, false);
	public static final FunctionDefinition strlen = new FunctionDefinition(FunctionName.function(pos, null, "strlen"), 1, false);
	public static final FunctionDefinition concat = new FunctionDefinition(FunctionName.function(pos, null, "++"), 2, false);
	public static final FunctionDefinition makeTuple = new FunctionDefinition(FunctionName.function(pos, null, "()"), -1, false);
	public static final FunctionDefinition handleSend = new FunctionDefinition(FunctionName.function(pos, null, "->"), 2, false);

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
		clickEventTR.bind(clickEvent);
		callMeTR.bind(callMeHandler);
	
		// add fields to structs
		error.addField(new StructField(pos, error, false, stringTR, "message"));
		cons.addField(new StructField(pos, cons, false, polyATR, "head"));
		cons.addField(new StructField(pos, cons, false, listATR, "tail"));
		debug.addField(new StructField(pos, debug, false, stringTR, "message"));
		send.addField(new StructField(pos, send, false, contractTR, "sendto"));
		send.addField(new StructField(pos, send, false, stringTR, "meth"));
		send.addField(new StructField(pos, send, false, listAnyTR, "args"));
		assign.addField(new StructField(pos, assign, false, anyTR, "on"));
		assign.addField(new StructField(pos, assign, false, stringTR, "fld"));
		assign.addField(new StructField(pos, assign, false, anyTR, "value"));
		
		// is this ok as a string or should it be something else?
		type.addField(new StructField(pos, type, false, stringTR, "type"));

		// add cases to unions
		bool.addCase(falseTR);
		bool.addCase(trueTR);
		list.addCase(nilTR);
		list.addCase(consATR);
		message.addCase(debugTR);
		message.addCase(assignTR);
		message.addCase(sendTR);
		event.addCase(clickEventTR);
		
		probeState.bind(new FunctionDefinition(FunctionName.function(pos, null, "_probe_state"), 2, false));
		getUnderlying.bind(new FunctionDefinition(FunctionName.function(pos, null, "_underlying"), 1, false));

		// add methods to contracts
		lifecycle.addMethod(new ContractMethodDecl(pos, pos, pos, false, FunctionName.contractMethod(pos, lifecycle.name(), "init"), new ArrayList<TypedPattern>(), null));
		lifecycle.addMethod(new ContractMethodDecl(pos, pos, pos, false, FunctionName.contractMethod(pos, lifecycle.name(), "closing"), new ArrayList<TypedPattern>(), null));

		{
			TypedPattern cmh = new TypedPattern(pos, callMeTR, new VarName(pos, callMeHandler.name(), "handler"));
			ContractMethodDecl callMe = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, repeater.name(), "callMe"), new ArrayList<TypedPattern>(), cmh);
			callMe.bindType();
			repeater.addMethod(callMe);
		}
		{
			ContractMethodDecl callback = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, repeater.name(), "call"), new ArrayList<TypedPattern>(), null);
			callback.bindType();
			callMeHandler.addMethod(callback);
		}
		
		// specify function types
		{
			Type pa = new PolyType(pos, new SolidName(null, "A"));
			isEqual.bindType(new Apply(pa, pa, bool));
		}
		isType.bindType(new Apply(type, any, bool));
		plus.bindType(new Apply(number, number, number));
		minus.bindType(new Apply(number, number, number));
		mul.bindType(new Apply(number, number, number));
		div.bindType(new Apply(number, number, number));
		not.bindType(new Apply(bool, bool));
		length.bindType(new Apply(list, number));
		strlen.bindType(new Apply(string, number));
		concat.bindType(new Apply(string, string, string));
		makeTuple.bindType(tuple);
		handleSend.bindType(new Apply(new Apply(contract, send), contract, send)); // TODO: "contract" arg (in both places) should be specifically "Handler" I think
		
		// add all current functions to list for dependency resolution
		allFunctions.add(isEqual);
		allFunctions.add(isType);
		allFunctions.add(plus);
		allFunctions.add(minus);
		allFunctions.add(mul);
		allFunctions.add(div);
		allFunctions.add(not);
		allFunctions.add(length);
		allFunctions.add(strlen);
		allFunctions.add(concat);
		allFunctions.add(handleSend);
	}
	
	public static void applyTo(ErrorReporter errors, Repository repository) {
		repository.addEntry(errors, any.name(), any);
		repository.addEntry(errors, contract.name(), contract);
		repository.newStruct(errors, error);
		repository.addEntry(errors, number.name(), number);
		repository.addEntry(errors, string.name(), string);

		repository.addEntry(errors, falseT.name(), falseT);
		repository.addEntry(errors, trueT.name(), trueT);
		repository.addEntry(errors, bool.name(), bool);
		
		repository.addEntry(errors, new SolidName(null, "[]"), nil);
		repository.newStruct(errors, nil);
		repository.newStruct(errors, cons);
		repository.newUnion(errors, list);
		
		repository.newStruct(errors, debug);
		repository.newStruct(errors, send);
		repository.newStruct(errors, assign);
		repository.newUnion(errors, message);

		repository.newStruct(errors, clickEvent);
		repository.newUnion(errors, event);

		repository.functionDefn(errors, isType);
		repository.functionDefn(errors, isEqual);
		repository.functionDefn(errors, plus);
		repository.functionDefn(errors, minus);
		repository.functionDefn(errors, mul);
		repository.functionDefn(errors, div);
		repository.functionDefn(errors, not);
		repository.functionDefn(errors, length);
		repository.functionDefn(errors, strlen);
		repository.functionDefn(errors, concat);
		repository.functionDefn(errors, makeTuple);
		repository.functionDefn(errors, handleSend);

		// not yet thought through for backward compatibility
		StructDefn card = new StructDefn(pos, FieldsType.STRUCT, null, "Card", false);
		repository.newStruct(errors, card);
		StructDefn croset = new StructDefn(pos, FieldsType.STRUCT, null, "Croset", false);
		repository.newStruct(errors, croset);
		StructDefn map = new StructDefn(pos, FieldsType.STRUCT, null, "Map", false);
		repository.newStruct(errors, map);
		repository.newStruct(errors, type);

		// builtin contracts
		repository.newContract(errors, lifecycle);
		repository.newContract(errors, repeater);
		repository.newContract(errors, callMeHandler);

		// dubious backward compatibility

		StructDefn crokeys = new StructDefn(pos, FieldsType.STRUCT, null, "Crokeys", false);
		repository.newStruct(errors, crokeys);
		StructDefn id = new StructDefn(pos, FieldsType.STRUCT, null, "Id", false);
		repository.newStruct(errors, id);
	}
}
