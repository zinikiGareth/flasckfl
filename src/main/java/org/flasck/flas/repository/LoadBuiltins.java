package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
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

	// Type References used here ...
	public static final TypeReference polyATR = new TypeReference(pos, "A");
	public static final TypeReference anyTR = new TypeReference(pos, "Any");
	public static final TypeReference contractTR = new TypeReference(pos, "Contract");
	public static final TypeReference stringTR = new TypeReference(pos, "String");
	public static final TypeReference numberTR = new TypeReference(pos, "Number");
	public static final TypeReference typeTR = new TypeReference(pos, "Type");
	public static final TypeReference falseTR = new TypeReference(pos, "False");
	public static final TypeReference trueTR = new TypeReference(pos, "True"); 
	public static final TypeReference nilTR = new TypeReference(pos, "Nil"); 
	public static final TypeReference consATR = new TypeReference(pos, "Cons", polyATR);
	public static final TypeReference listATR = new TypeReference(pos, "List", polyATR);
	public static final TypeReference listAnyTR = new TypeReference(pos, "List", anyTR);
	public static final TypeReference randomTR = new TypeReference(pos, "Random");
	public static final TypeReference debugTR = new TypeReference(pos, "Debug");
	public static final TypeReference sendTR = new TypeReference(pos, "Send");
	public static final TypeReference assignTR = new TypeReference(pos, "Assign");
	public static final TypeReference clickEventTR = new TypeReference(pos, "ClickEvent");
	
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
	public static final StructDefn assignItem = new StructDefn(pos, FieldsType.STRUCT, null, "AssignItem", false, polyA);

	// Random
	public static final ObjectDefn random = new ObjectDefn(pos, pos, new ObjectName(null, "Random"), false, new ArrayList<>());
	private static ObjectCtor randomSeed;
	private static ObjectCtor randomUnseeded;
	static ObjectAccessor randomNext;
	static ObjectMethod randomUsed;
	
	// Crobags
	public static final ObjectDefn crobag = new ObjectDefn(pos, pos, new ObjectName(null, "Crobag"), false, Arrays.asList(polyA));
	
	// Messages
	public static final StructDefn debug = new StructDefn(pos, FieldsType.STRUCT, null, "Debug", false);
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	public static final StructDefn assign = new StructDefn(pos, FieldsType.STRUCT, null, "Assign", false);
	public static final StructDefn assignCons = new StructDefn(pos, FieldsType.STRUCT, null, "AssignCons", false);
	public static final UnionTypeDefn message = new UnionTypeDefn(pos, false, new SolidName(null, "Message"));
	public static final Type listMessages = new PolyInstance(LoadBuiltins.pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));

	// Events
	public static final StructDefn clickEvent = new StructDefn(pos, FieldsType.STRUCT, null, "ClickEvent", false);
	public static final StructField source = new StructField(pos, pos, clickEvent, true, anyTR, "source", new CurrentContainer(pos, clickEvent));
	public static final UnionTypeDefn event = new UnionTypeDefn(pos, false, new SolidName(null, "Event"));
	
	// The type "operator"
	private static StructDefn type = new StructDefn(pos, FieldsType.STRUCT, null, "Type", false);

	// The function that probes state in UDDs
	public static UnresolvedVar probeState = new UnresolvedVar(pos, "_probe_state");
	public static UnresolvedVar getUnderlying = new UnresolvedVar(pos, "_underlying");

	// Builtin operators
	public static final FunctionDefinition isType = new FunctionDefinition(FunctionName.function(pos, null, "istype"), 2, null).dontGenerate();
	public static final FunctionDefinition isEqual = new FunctionDefinition(FunctionName.function(pos, null, "=="), 2, null).dontGenerate();
	public static final FunctionDefinition isGE = new FunctionDefinition(FunctionName.function(pos, null, ">="), 2, null).dontGenerate();
	public static final FunctionDefinition isGT = new FunctionDefinition(FunctionName.function(pos, null, ">"), 2, null).dontGenerate();
	public static final FunctionDefinition isLE = new FunctionDefinition(FunctionName.function(pos, null, "<="), 2, null).dontGenerate();
	public static final FunctionDefinition isLT = new FunctionDefinition(FunctionName.function(pos, null, "<"), 2, null).dontGenerate();
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, null).dontGenerate();
	public static final FunctionDefinition unaryMinus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 1, null).dontGenerate();
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2, null).dontGenerate();
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2, null).dontGenerate();
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2, null).dontGenerate();
	public static final FunctionDefinition mod = new FunctionDefinition(FunctionName.function(pos, null, "%"), 2, null).dontGenerate();
	public static final FunctionDefinition not = new FunctionDefinition(FunctionName.function(pos, null, "!"), 1, null).dontGenerate();
	public static final FunctionDefinition and = new FunctionDefinition(FunctionName.function(pos, null, "&&"), 2, null).dontGenerate();
	public static final FunctionDefinition or = new FunctionDefinition(FunctionName.function(pos, null, "||"), 2, null).dontGenerate();
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1, null).dontGenerate();
	public static final FunctionDefinition replace = new FunctionDefinition(FunctionName.function(pos, null, "replace"), 3, null).dontGenerate();
	public static final FunctionDefinition nth = new FunctionDefinition(FunctionName.function(pos, null, "nth"), 2, null).dontGenerate();
	public static final FunctionDefinition item = new FunctionDefinition(FunctionName.function(pos, null, "item"), 2, null).dontGenerate();
	public static final FunctionDefinition take = new FunctionDefinition(FunctionName.function(pos, null, "take"), 2, null).dontGenerate();
	public static final FunctionDefinition drop = new FunctionDefinition(FunctionName.function(pos, null, "drop"), 2, null).dontGenerate();
	public static final FunctionDefinition append = new FunctionDefinition(FunctionName.function(pos, null, "append"), 2, null).dontGenerate();
	public static final FunctionDefinition strlen = new FunctionDefinition(FunctionName.function(pos, null, "strlen"), 1, null).dontGenerate();
	public static final FunctionDefinition concat = new FunctionDefinition(FunctionName.function(pos, null, "++"), 2, null).dontGenerate();
	public static final FunctionDefinition concatLists = new FunctionDefinition(FunctionName.function(pos, null, "concatLists"), 1, null).dontGenerate();
	public static final FunctionDefinition makeTuple = new FunctionDefinition(FunctionName.function(pos, null, "()"), -1, null).dontGenerate();
	public static final FunctionDefinition handleSend = new FunctionDefinition(FunctionName.function(pos, null, "->"), 2, null).dontGenerate();
	public static final FunctionDefinition dispatch = new FunctionDefinition(FunctionName.function(pos, null, "dispatch"), 1, null).dontGenerate();
	public static final FunctionDefinition show = new FunctionDefinition(FunctionName.function(pos, null, "show"), 1, null).dontGenerate();
	public static final FunctionDefinition expr = new FunctionDefinition(FunctionName.function(pos, null, "expr"), 1, null).dontGenerate();

	static {
		// bind TRs
		polyATR.bind(polyA);
		anyTR.bind(any);
		contractTR.bind(contract);
		stringTR.bind(string);
		numberTR.bind(number);
		typeTR.bind(type);
		consATR.bind(cons);
		listATR.bind(list);
		listAnyTR.bind(listAny);
		falseTR.bind(falseT);
		trueTR.bind(trueT);
		nilTR.bind(nil);
		randomTR.bind(random);
		debugTR.bind(debug);
		sendTR.bind(send);
		assignTR.bind(assign);
		clickEventTR.bind(clickEvent);
	
		// add fields to structs
		error.addField(new StructField(pos, error, true, stringTR, "message"));
		StructField head = new StructField(pos, cons, true, polyATR, "head");
		head.fullName(new VarName(pos, cons.name(), "head"));
		cons.addField(head);
		StructField aihead = new StructField(pos, assignItem, true, polyATR, "head");
		aihead.fullName(new VarName(pos, assignItem.name(), "head"));
		assignItem.addField(aihead);
		StructField tail = new StructField(pos, cons, true, listATR, "tail");
		tail.fullName(new VarName(pos, cons.name(), "tail"));
		cons.addField(tail);
		debug.addField(new StructField(pos, debug, true, stringTR, "message"));
		send.addField(new StructField(pos, send, false, contractTR, "sendto"));
		send.addField(new StructField(pos, send, false, stringTR, "meth"));
		send.addField(new StructField(pos, send, false, listAnyTR, "args"));
		assign.addField(new StructField(pos, assign, false, anyTR, "on"));
		assign.addField(new StructField(pos, assign, false, stringTR, "fld"));
		assign.addField(new StructField(pos, assign, false, anyTR, "value"));
		assignCons.addField(new StructField(pos, assignCons, false, anyTR, "on"));
		assignCons.addField(new StructField(pos, assignCons, false, anyTR, "value"));
		source.fullName(new VarName(pos, clickEvent.name(), "source"));
		clickEvent.addField(source);
		
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
		
		probeState.bind(new FunctionDefinition(FunctionName.function(pos, null, "_probe_state"), 2, null));
		getUnderlying.bind(new FunctionDefinition(FunctionName.function(pos, null, "_underlying"), 1, null));

		// add methods to objects
		{
			{
				FunctionName ctorSeed = FunctionName.objectCtor(pos, random.name(), "seed");
				randomSeed = new ObjectCtor(pos, random, ctorSeed, Arrays.asList(new TypedPattern(pos, numberTR, new VarName(pos, ctorSeed, "seed"))));
				randomSeed.dontGenerate();
				randomSeed.bindType(new Apply(number, random));
				random.addConstructor(randomSeed);
			}
			{
				FunctionName ctorUnseeded = FunctionName.objectCtor(pos, random.name(), "unseeded");
				randomUnseeded = new ObjectCtor(pos, random, ctorUnseeded, new ArrayList<>());
				randomUnseeded.dontGenerate();
				randomUnseeded.bindType(random);
				random.addConstructor(randomUnseeded);
			}
			{
				FunctionName afn = FunctionName.function(pos, random.name(), "next");
				FunctionDefinition acor = new FunctionDefinition(afn, 1, random);
				acor.bindType(new Apply(number, new PolyInstance(pos, list, Arrays.asList(number))));
				randomNext = new ObjectAccessor(random, acor);
				randomNext.dontGenerate();
				random.addAccessor(randomNext);
			}
			{
				FunctionName used = FunctionName.objectMethod(pos, random.name(), "used");
				randomUsed = new ObjectMethod(pos, used, Arrays.asList(new TypedPattern(pos, numberTR, new VarName(pos, used, "quant"))), null, random);
				randomUsed.dontGenerate();
				randomUsed.bindType(new Apply(number, listMessages));
				random.addMethod(randomUsed);
			}
		}
		
		// specify function types
		{
			Type pa = new PolyType(pos, new SolidName(null, "A"));
			isEqual.bindType(new Apply(pa, pa, bool));
		}
		isGE.bindType(new Apply(number, number, bool));
		isGT.bindType(new Apply(number, number, bool));
		isLE.bindType(new Apply(number, number, bool));
		isLT.bindType(new Apply(number, number, bool));
		isType.bindType(new Apply(type, any, bool));
		plus.bindType(new Apply(number, number, number));
		unaryMinus.bindType(new Apply(number, number));
		minus.bindType(new Apply(number, number, number));
		mul.bindType(new Apply(number, number, number));
		div.bindType(new Apply(number, number, number));
		mod.bindType(new Apply(number, number, number));
		not.bindType(new Apply(bool, bool));
		and.bindType(new Apply(bool, bool, bool));
		or.bindType(new Apply(bool, bool, bool));
		length.bindType(new Apply(list, number));
		replace.bindType(new Apply(list, number, polyA, list));
		nth.bindType(new Apply(number, list, polyA));
		item.bindType(new Apply(number, list, assignItem));
		take.bindType(new Apply(number, list, list));
		drop.bindType(new Apply(number, list, list));
		append.bindType(new Apply(list, polyA, list));
		strlen.bindType(new Apply(string, number));
		concat.bindType(new Apply(string, string, string));
		concatLists.bindType(new Apply(new PolyInstance(pos, list, Arrays.asList(new PolyInstance(pos, list, Arrays.asList(polyA)))), new PolyInstance(pos, list, Arrays.asList(polyA))));
		makeTuple.bindType(tuple);
		handleSend.bindType(new Apply(new Apply(contract, send), contract, send)); // TODO: "contract" arg (in both places) should be specifically "Handler" I think
		dispatch.bindType(new Apply(listMessages, listMessages));
		dispatch.restrict(new UTOnlyRestriction("dispatch"));
		show.bindType(new Apply(any, string));
		expr.bindType(new Apply(any, string));
	}
	
	public static void applyTo(ErrorReporter errors, Repository repository) {
		repository.addEntry(errors, any.name(), any);
		repository.addEntry(errors, contract.name(), contract);
		repository.newStruct(errors, error);
		repository.addEntry(errors, number.name(), number);
		repository.addEntry(errors, string.name(), string);
		repository.newStruct(errors, type);

		repository.addEntry(errors, falseT.name(), falseT);
		repository.addEntry(errors, trueT.name(), trueT);
		repository.addEntry(errors, bool.name(), bool);
		
		repository.addEntry(errors, new SolidName(null, "[]"), nil);
		repository.newStruct(errors, nil);
		repository.newStruct(errors, cons);
		repository.newUnion(errors, list);
		
		repository.newObject(errors, random);
		repository.newObjectMethod(errors, randomSeed);
		repository.newObjectMethod(errors, randomUnseeded);
		repository.newObjectAccessor(errors, randomNext);
		repository.newObjectMethod(errors, randomUsed);

		repository.newStruct(errors, debug);
		repository.newStruct(errors, send);
		repository.newStruct(errors, assign);
		repository.newUnion(errors, message);

		repository.newStruct(errors, clickEvent);
		repository.newUnion(errors, event);

		repository.functionDefn(errors, isType);
		repository.functionDefn(errors, isEqual);
		repository.functionDefn(errors, isGE);
		repository.functionDefn(errors, isGT);
		repository.functionDefn(errors, isLE);
		repository.functionDefn(errors, isLT);
		repository.functionDefn(errors, plus);
		repository.functionDefn(errors, minus);
		repository.functionDefn(errors, mul);
		repository.functionDefn(errors, div);
		repository.functionDefn(errors, mod);
		repository.functionDefn(errors, not);
		repository.functionDefn(errors, and);
		repository.functionDefn(errors, or);
		repository.functionDefn(errors, length);
		repository.functionDefn(errors, replace);
		repository.functionDefn(errors, nth);
		repository.functionDefn(errors, item);
		repository.functionDefn(errors, drop);
		repository.functionDefn(errors, take);
		repository.functionDefn(errors, append);
		repository.functionDefn(errors, strlen);
		repository.functionDefn(errors, concat);
		repository.functionDefn(errors, concatLists);
		repository.functionDefn(errors, makeTuple);
		repository.functionDefn(errors, handleSend);
		repository.functionDefn(errors, dispatch);
		repository.functionDefn(errors, show);
		repository.functionDefn(errors, expr);
	}
}
