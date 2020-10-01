package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
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
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryEntry.ValidContexts;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Tuple;
import org.flasck.flas.tc3.Type;

public class LoadBuiltins {
	public static final InputPosition pos = new InputPosition("BuiltIn", 1, 0, "<<builtin>>");

	/* "Primitive" types */

	//   -> string
	public static final TypeReference stringTR = new TypeReference(pos, "String");
	public static final Primitive string = new Primitive(pos, "String");
	static {
		stringTR.bind(string);
	}

	//   -> error
	public static final StructDefn error = new StructDefn(pos, FieldsType.STRUCT, null, "Error", false);
	static {
		error.addField(new StructField(pos, error, true, stringTR, "message"));
	}

	//   -> any
	public static final Primitive any = new Primitive(pos, "Any");
	public static final TypeReference anyTR = new TypeReference(pos, "Any");
	static {
		anyTR.bind(any);
	}
	
	/* TODO: I think we want subclasses of Any called "Entity", "Deal", "Offer", etc
	 * Not quite sure what etc. includes because I don't think "Primitive" and "Struct" hold any value
	 * Entity is obviously useful because we use it in Data Contracts
	 * Deal & Offer feel like they would come up in conversations about Commerce
	 * Countables, Currency and ValueStore need to go here too
	 */
	public static final Primitive entity = any;
	public static final TypeReference entityTR = new TypeReference(pos, "Entity");
	static {
		entityTR.bind(entity);
	}

	//   -> number
	public static final TypeReference numberTR = new TypeReference(pos, "Number");
	public static final Primitive number = new Primitive(pos, "Number");
	static {
		numberTR.bind(number);
	}

	//   -> uri
	public static final TypeReference uriTR = new TypeReference(pos, "Uri");
	public static final Primitive uri = new Primitive(pos, "Uri");
	static {
		uriTR.bind(uri);
	}

	//   -> id
	public static final Primitive id = new Primitive(pos, "Id");
	static {
	}
	
	/* Primitives related to time & date */
	
	//   -> Instant (a specific moment in the history of the universe)
	public static final TypeReference instantTR = new TypeReference(pos, "Instant");
	public static final Primitive instant = new Primitive(pos, "Instant");
	static {
		instantTR.bind(instant);
	}

	//   -> Interval (the time elapsed between two instants)
	public static final TypeReference intervalTR = new TypeReference(pos, "Interval");
	public static final Primitive interval = new Primitive(pos, "Interval");
	static {
		intervalTR.bind(interval);
	}

	/* Weird things that need to exist and therefore feel "Primitive" */
	
	//   -> contract
	public static final TypeReference contractTR = new TypeReference(pos, "Contract");
	public static final Primitive contract = new Primitive(pos, "Contract"); // Should this be in 3 parts? (CONTRACT, SERVICE, HANDLER?)
	public static final Primitive idempotentHandler = contract; // This may or may not be correct ...
	static {
		contractTR.bind(contract);
	}

	//   -> tuple
	public static final Tuple tuple = new Tuple(pos, "Tuple");

	
	/* Booleans */

	//   -> Bool (the union)
	public static final UnionTypeDefn bool = new UnionTypeDefn(pos, false, new SolidName(null, "Boolean"));

	//   -> False
	public static final TypeReference falseTR = new TypeReference(pos, "False");
	public static final StructDefn falseT = new StructDefn(pos, FieldsType.STRUCT, null, "False", false);
	static {
		falseTR.bind(falseT);
		bool.addCase(falseTR);
	}

	//   -> True
	public static final TypeReference trueTR = new TypeReference(pos, "True"); 
	public static final StructDefn trueT = new StructDefn(pos, FieldsType.STRUCT, null, "True", false);
	static {
		trueTR.bind(trueT);
		bool.addCase(trueTR);
	}

	/* Lists */
	
	//   -> List (the union)
	private static TypeReference listATR_A = new TypeReference(pos, "A");
	public static final UnionTypeDefn list = new UnionTypeDefn(pos, false, new SolidName(null, "List"), new ArrayList<>());
	private static PolyType listA_A = new PolyType(pos, new SolidName(list.name(), "A"));
	static {
		listATR_A.bind(listA_A);
		list.polys().add(listA_A);
	}

	//   -> Nil
	public static final StructDefn nil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
	public static final TypeReference nilTR = new TypeReference(pos, "Nil");
	static {
		nilTR.bind(nil);
		list.addCase(nilTR);
	}
	
	//   -> Cons
	private static TypeReference consATR_A = new TypeReference(pos, "A");
	public static final StructDefn cons = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(null, "Cons"), false, new ArrayList<>());
	public static final TypeReference consATR = new TypeReference(pos, "Cons", consATR_A);
	static {
		consATR.bind(cons);
		PolyType cta = new PolyType(pos, new SolidName(cons.name(), "A"));
		cons.polys().add(cta);
		consATR_A.bind(cta);
		StructField head = new StructField(pos, cons, true, consATR_A, "head");
		head.fullName(new VarName(pos, cons.name(), "head"));
		cons.addField(head);
		PolyInstance pil = new PolyInstance(pos, list, Arrays.asList(cta));
		TypeReference piltr = new TypeReference(pos, "List", consATR_A);
		piltr.bind(pil);
		StructField tail = new StructField(pos, cons, true, piltr, "tail");
		tail.fullName(new VarName(pos, cons.name(), "tail"));
		cons.addField(tail);
		TypeReference lc = new TypeReference(pos, "Cons", listATR_A);
		lc.bind(new PolyInstance(pos, cons, Arrays.asList(listA_A)));
		list.addCase(lc);
	}

	//   -> List TR
	public static final TypeReference listATR = new TypeReference(pos, "List", listATR_A);
	static {
		listATR.bind(list);
	}
	
	//   -> List[Any]
	public static final PolyInstance listAny = new PolyInstance(pos, list, Arrays.asList(any));
	public static final TypeReference listAnyTR = new TypeReference(pos, "List", anyTR);
	static {
		listAnyTR.bind(listAny);
	}

	//   -> AssignItem (is similar to Cons but can be used in assignments in methods)
	public static final StructDefn assignItem = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(null, "AssignItem"), false, new ArrayList<>());
	static {
		TypeReference aitr = new TypeReference(pos, "A");
		PolyType aip = new PolyType(pos, new SolidName(assignItem.name(), "A"));
		aitr.bind(aip);
		assignItem.polys().add(aip);
		StructField aihead = new StructField(pos, assignItem, true, aitr, "head");
		aihead.fullName(new VarName(pos, assignItem.name(), "head"));
		assignItem.addField(aihead);
	}

	/*  Hashes (associative arrays) */

	//   -> Hash
	public static final TypeReference hashTR = new TypeReference(pos, "Hash");
	public static final StructDefn hash = new StructDefn(pos, FieldsType.STRUCT, null, "Hash", false);
	static {
		hashTR.bind(hash);
	}
	
	//   -> HashPair (each element of a hash is a hash pair, but this is internal, the result of the : operator)
	public static final StructDefn hashPairType = new StructDefn(pos, FieldsType.STRUCT, null, "_HashPair", false);


	/*  Messages */
	
	//   -> Message (the union)
	public static final TypeReference messageTR = new TypeReference(pos, "Message");
	public static final UnionTypeDefn message = new UnionTypeDefn(pos, false, new SolidName(null, "Message"));
	//   -> List[Message]
	public static final TypeReference consMessagesTR = new TypeReference(pos, "Cons", messageTR);
	public static final NamedType consMessages = new PolyInstance(LoadBuiltins.pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.message));
	public static final TypeReference listMessagesTR = new TypeReference(pos, "List", messageTR);
	public static final NamedType listMessages = new PolyInstance(LoadBuiltins.pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
	static {
		messageTR.bind(message);
		consMessagesTR.bind(consMessages);
		listMessagesTR.bind(listMessages);
		message.addCase(nilTR);
		message.addCase(consMessagesTR);
		message.addCase(listMessagesTR);
	}

	//   -> Debug
	public static final TypeReference debugTR = new TypeReference(pos, "Debug");
	public static final StructDefn debug = new StructDefn(pos, FieldsType.STRUCT, null, "Debug", false);
	static {
		debugTR.bind(debug);
		debug.addField(new StructField(pos, debug, true, stringTR, "message"));
		message.addCase(debugTR);
	}
	
	//   -> Send
	public static final TypeReference sendTR = new TypeReference(pos, "Send");
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	static {
		sendTR.bind(send);
		send.addField(new StructField(pos, send, false, contractTR, "sendto"));
		send.addField(new StructField(pos, send, false, stringTR, "meth"));
		send.addField(new StructField(pos, send, false, listAnyTR, "args"));
		message.addCase(sendTR);
	}
	
	//   -> Assign
	public static final TypeReference assignTR = new TypeReference(pos, "Assign");
	public static final StructDefn assign = new StructDefn(pos, FieldsType.STRUCT, null, "Assign", false);
	static {
		assignTR.bind(assign);
		assign.addField(new StructField(pos, assign, false, anyTR, "on"));
		assign.addField(new StructField(pos, assign, false, stringTR, "fld"));
		assign.addField(new StructField(pos, assign, false, anyTR, "value"));
		message.addCase(assignTR);
	}
	
	//   -> AssignCons (this and assignitem seem somewhat duplicative)
	public static final StructDefn assignCons = new StructDefn(pos, FieldsType.STRUCT, null, "AssignCons", false);
	static {
		assignCons.addField(new StructField(pos, assignCons, false, anyTR, "on"));
		assignCons.addField(new StructField(pos, assignCons, false, anyTR, "value"));
		// why is this not added to message?
	}
	

	/* Events */

	//   -> Event (the union)
	public static final UnionTypeDefn event = new UnionTypeDefn(pos, false, new SolidName(null, "Event"));
	
	//   -> ClickEvent
	public static final TypeReference clickEventTR = new TypeReference(pos, "ClickEvent");
	public static final StructDefn clickEvent = new StructDefn(pos, FieldsType.STRUCT, null, "ClickEvent", false);
	public static final StructField source = new StructField(pos, pos, clickEvent, true, anyTR, "source", new CurrentContainer(pos, clickEvent));
	static {
		clickEventTR.bind(clickEvent);
		clickEvent.addField(source);
		source.fullName(new VarName(pos, clickEvent.name(), "source"));
		event.addCase(clickEventTR);
	}

	/* Objects */

	// Random

	public static final TypeReference randomTR = new TypeReference(pos, "Random");
	public static final ObjectDefn random = new ObjectDefn(pos, pos, new ObjectName(null, "Random"), false, new ArrayList<>());
	static {
		randomTR.bind(random);
	}
	
	//   -> ctor Random.seed
	private static ObjectCtor randomSeed;
	static {
		FunctionName ctorSeed = FunctionName.objectCtor(pos, random.name(), "seed");
		randomSeed = new ObjectCtor(pos, random, ctorSeed, Arrays.asList(new TypedPattern(pos, numberTR, new VarName(pos, ctorSeed, "seed"))));
		randomSeed.dontGenerate();
		randomSeed.bindType(new Apply(number, random));
		random.addConstructor(randomSeed);
	}

	//   -> ctor Random.unseeded
	private static ObjectCtor randomUnseeded;
	static {
		FunctionName ctorUnseeded = FunctionName.objectCtor(pos, random.name(), "unseeded");
		randomUnseeded = new ObjectCtor(pos, random, ctorUnseeded, new ArrayList<>());
		randomUnseeded.dontGenerate();
		randomUnseeded.bindType(random);
		random.addConstructor(randomUnseeded);
	}

	//   -> method Ranndom.next
	private static ObjectAccessor randomNext;
	static {
		FunctionName afn = FunctionName.function(pos, random.name(), "next");
		FunctionDefinition acor = new FunctionDefinition(afn, 1, random);
		acor.bindType(new Apply(number, new PolyInstance(pos, list, Arrays.asList(number))));
		randomNext = new ObjectAccessor(random, acor);
		randomNext.dontGenerate();
		random.addAccessor(randomNext);
	}

	//   -> method Ranndom.used
	private static ObjectMethod randomUsed;
	static {
		FunctionName used = FunctionName.objectMethod(pos, random.name(), "used");
		randomUsed = new ObjectMethod(pos, used, Arrays.asList(new TypedPattern(pos, numberTR, new VarName(pos, used, "quant"))), null, random);
		randomUsed.dontGenerate();
		randomUsed.bindType(new Apply(number, listMessages));
		random.addMethod(randomUsed);
	}

	// SlideWindow (how we know to move forwards & backwards
	public static final ContractDecl crobagSlideWindow = new ContractDecl(pos, pos, ContractType.HANDLER, new SolidName(null, "SlideWindow"), false);
	// TODO: previous
	// TODO: next
	
	// CrobagWindow  (the handler for the window method)
	public static final ContractDecl crobagWindowHandler = new ContractDecl(pos, pos, ContractType.HANDLER, new SolidName(null, "CrobagWindow"), false);

    //   -> handler method 'next'	
	static {
		FunctionName next = FunctionName.contractMethod(pos, crobagWindowHandler.name(), "next");
		ContractMethodDecl nextcmd = new ContractMethodDecl(pos, pos, pos, false, next, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, next, "from")), new TypedPattern(pos, numberTR, new VarName(pos, next, "size"))), null);
		crobagWindowHandler.addMethod(nextcmd);
	}

    //   -> handler method 'done'	
	static {
		FunctionName done = FunctionName.contractMethod(pos, crobagWindowHandler.name(), "done");
		ContractMethodDecl donecmd = new ContractMethodDecl(pos, pos, pos, false, done, new ArrayList<>(), new TypedPattern(pos, new TypeReference(pos, "SlideWindow"), new VarName(pos, done, "slide")));
		crobagWindowHandler.addMethod(donecmd);
	}

	
	// Crobag[A]
	public static final TypeReference crobagTR = new TypeReference(pos, "Crobag");
	public static final ObjectDefn crobag = new ObjectDefn(pos, pos, new ObjectName(null, "Crobag"), false, new ArrayList<>());
	static {
		PolyType cp = new PolyType(pos, new SolidName(crobag.name(), "A"));
		crobagTR.bind(crobag);
		crobag.polys().add(cp);
	}
	
	//   -> ctor Crobag.new
	private static ObjectCtor crobagNew;
	static {
		FunctionName ctorNew = FunctionName.objectCtor(pos, crobag.name(), "new");
		crobagNew = new ObjectCtor(pos, crobag, ctorNew, new ArrayList<>());
		crobagNew.dontGenerate();
		crobagNew.bindType(crobag);
		crobag.addConstructor(crobagNew);
	}
	
	//   -> method Crobag.add
	private static ObjectMethod crobagInsert;
	static {
		FunctionName add = FunctionName.objectMethod(pos, crobag.name(), "insert");
		crobagInsert = new ObjectMethod(pos, add, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, add, "key")), new TypedPattern(pos, entityTR, new VarName(pos, add, "value"))), null, crobag);
		crobagInsert.dontGenerate();
		crobagInsert.bindType(new Apply(string, entity, listMessages));
		crobag.addMethod(crobagInsert);
	}

	//   -> method Crobag.put
	private static ObjectMethod crobagPut;
	static {
		FunctionName add = FunctionName.objectMethod(pos, crobag.name(), "put");
		crobagPut = new ObjectMethod(pos, add, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, add, "key")), new TypedPattern(pos, entityTR, new VarName(pos, add, "value"))), null, crobag);
		crobagPut.dontGenerate();
		crobagPut.bindType(new Apply(string, entity, listMessages));
		crobag.addMethod(crobagPut);
	}

	//   -> method Crobag.upsert
	private static ObjectMethod crobagUpsert;
	static {
		FunctionName add = FunctionName.objectMethod(pos, crobag.name(), "upsert");
		crobagUpsert = new ObjectMethod(pos, add, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, add, "key")), new TypedPattern(pos, entityTR, new VarName(pos, add, "value"))), null, crobag);
		crobagUpsert.dontGenerate();
		crobagUpsert.bindType(new Apply(string, entity, listMessages));
		crobag.addMethod(crobagUpsert);
	}

	//   -> method Crobag.window
	private static ObjectMethod crobagWindow;
	static {
		FunctionName window = FunctionName.objectMethod(pos, crobag.name(), "window");
		crobagWindow = new ObjectMethod(pos, window, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, window, "from")), new TypedPattern(pos, numberTR, new VarName(pos, window, "size"))), new VarPattern(pos, new VarName(pos, window, "handler")), crobag);
		crobagWindow.dontGenerate();
		crobagWindow.bindType(new Apply(string, number, crobagWindowHandler, listMessages));
		crobag.addMethod(crobagWindow);
	}
	
	//   -> test acor Crobag.size
	private static ObjectAccessor crobagSize;
	static {
		FunctionName size = FunctionName.objectMethod(pos, crobag.name(), "size");
		FunctionDefinition sizeFn = new FunctionDefinition(size, 0, crobag);
		crobagSize = new ObjectAccessor(crobag, sizeFn);
		crobagSize.dontGenerate();
		crobagSize.validContexts(ValidContexts.TESTS);
		sizeFn.bindType(number);
		crobag.addAccessor(crobagSize);
	}
	

	// Types
	public static final TypeReference typeTR = new TypeReference(pos, "Type");
	public static StructDefn type = new StructDefn(pos, FieldsType.STRUCT, null, "Type", false);
	static {
		typeTR.bind(type);
		type.addField(new StructField(pos, type, false, stringTR, "type"));
	}

	
	/* Functions */
	
	// Builtin operators
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
	public static final FunctionDefinition concat = new FunctionDefinition(FunctionName.function(pos, null, "++"), 2, null).dontGenerate();

	// syntax support
	public static final FunctionDefinition makeTuple = new FunctionDefinition(FunctionName.function(pos, null, "()"), -1, null).dontGenerate();
	public static final FunctionDefinition handleSend = new FunctionDefinition(FunctionName.function(pos, null, "->"), 2, null).dontGenerate();
	public static final FunctionDefinition hashPair = new FunctionDefinition(FunctionName.function(pos, null, ":"), 2, null).dontGenerate();

	// internal functions
	public static final FunctionDefinition isType = new FunctionDefinition(FunctionName.function(pos, null, "istype"), 2, null).dontGenerate();
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1, null).dontGenerate();
	public static final FunctionDefinition replace = new FunctionDefinition(FunctionName.function(pos, null, "replace"), 3, null).dontGenerate();
	public static final FunctionDefinition nth = new FunctionDefinition(FunctionName.function(pos, null, "nth"), 2, null).dontGenerate();
	public static final FunctionDefinition item = new FunctionDefinition(FunctionName.function(pos, null, "item"), 2, null).dontGenerate();
	public static final FunctionDefinition take = new FunctionDefinition(FunctionName.function(pos, null, "take"), 2, null).dontGenerate();
	public static final FunctionDefinition drop = new FunctionDefinition(FunctionName.function(pos, null, "drop"), 2, null).dontGenerate();
	public static final FunctionDefinition append = new FunctionDefinition(FunctionName.function(pos, null, "append"), 2, null).dontGenerate();
	public static final FunctionDefinition assoc = new FunctionDefinition(FunctionName.function(pos, null, "assoc"), 2, null).dontGenerate();
	public static final FunctionDefinition strlen = new FunctionDefinition(FunctionName.function(pos, null, "strlen"), 1, null).dontGenerate();
	public static final FunctionDefinition concatLists = new FunctionDefinition(FunctionName.function(pos, null, "concatLists"), 1, null).dontGenerate();
	public static final FunctionDefinition dispatch = new FunctionDefinition(FunctionName.function(pos, null, "dispatch"), 1, null).dontGenerate();
	public static final FunctionDefinition show = new FunctionDefinition(FunctionName.function(pos, null, "show"), 1, null).dontGenerate();
	public static final FunctionDefinition expr = new FunctionDefinition(FunctionName.function(pos, null, "expr"), 1, null).dontGenerate();
	public static final FunctionDefinition seconds = new FunctionDefinition(FunctionName.function(pos, null, "seconds"), 1, null).dontGenerate();
	public static final FunctionDefinition parseUri = new FunctionDefinition(FunctionName.function(pos, null, "parseUri"), 1, null).dontGenerate();
	public static final FunctionDefinition parseJson = new FunctionDefinition(FunctionName.function(pos, null, "parseJson"), 1, null).dontGenerate();

	// Secret builtin functions for testing
	public static final UnresolvedVar probeState = new UnresolvedVar(pos, "_probe_state");
	public static final UnresolvedVar getUnderlying = new UnresolvedVar(pos, "_underlying");

	static {
		probeState.bind(new FunctionDefinition(FunctionName.function(pos, null, "_probe_state"), 2, null));
		getUnderlying.bind(new FunctionDefinition(FunctionName.function(pos, null, "_underlying"), 1, null));

		// specify function types
		{
			Type pa = new PolyType(pos, new SolidName(isEqual.name(), "A"));
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
		replace.bindType(new Apply(list, number, listA_A, list));
		nth.bindType(new Apply(number, list, listA_A));
		assoc.bindType(new Apply(hash, string, any));
		hashPair.bindType(new Apply(string, any, hashPairType));
		item.bindType(new Apply(number, list, assignItem));
		take.bindType(new Apply(number, list, list));
		drop.bindType(new Apply(number, list, list));
		append.bindType(new Apply(list, listA_A, list));
		strlen.bindType(new Apply(string, number));
		concat.bindType(new Apply(string, string, string));
		concatLists.bindType(new Apply(new PolyInstance(pos, list, Arrays.asList(new PolyInstance(pos, list, Arrays.asList(listA_A)))), new PolyInstance(pos, list, Arrays.asList(listA_A))));
		makeTuple.bindType(tuple);
		handleSend.bindType(new Apply(new Apply(contract, send), contract, send)); // TODO: "contract" arg (in both places) should be specifically "Handler" I think
		dispatch.bindType(new Apply(listMessages, listMessages));
		dispatch.restrict(new UTOnlyRestriction("dispatch"));
		show.bindType(new Apply(any, string));
		expr.bindType(new Apply(any, string));
		seconds.bindType(new Apply(number, interval));
		parseUri.bindType(new Apply(string, uri));
		parseJson.bindType(new Apply(string, hash));
	}
	
	public static void applyTo(ErrorReporter errors, Repository repository) {
		repository.addEntry(errors, any.name(), any);
		repository.addEntry(errors, new SolidName(null, "Entity"), entity);
		repository.addEntry(errors, contract.name(), contract);
		repository.newStruct(errors, error);
		repository.addEntry(errors, number.name(), number);
		repository.addEntry(errors, string.name(), string);
		repository.addEntry(errors, uri.name(), uri);
		repository.addEntry(errors, interval.name(), interval);
		repository.addEntry(errors, instant.name(), instant);
		repository.newStruct(errors, type);

		repository.addEntry(errors, falseT.name(), falseT);
		repository.addEntry(errors, trueT.name(), trueT);
		repository.addEntry(errors, bool.name(), bool);
		
		repository.addEntry(errors, new SolidName(null, "[]"), nil);
		repository.newStruct(errors, nil);
		repository.newStruct(errors, cons);
		repository.newUnion(errors, list);
		
		repository.newStruct(errors, hash);
		repository.addEntry(errors, new SolidName(null, "{}"), hash);
		
		repository.addEntry(errors, id.name(), id);	

		repository.newObject(errors, random);
		repository.newObjectMethod(errors, randomSeed);
		repository.newObjectMethod(errors, randomUnseeded);
		repository.newObjectAccessor(errors, randomNext);
		repository.newObjectMethod(errors, randomUsed);

		repository.newObject(errors, crobag);
		repository.newObjectMethod(errors, crobagNew);
		repository.newObjectMethod(errors, crobagInsert);
		repository.newObjectMethod(errors, crobagPut);
		repository.newObjectMethod(errors, crobagUpsert);
		repository.newObjectMethod(errors, crobagWindow);
		repository.newObjectAccessor(errors, crobagSize);

		repository.newContract(errors, crobagWindowHandler);
		repository.newContract(errors, crobagSlideWindow);
		
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
		repository.functionDefn(errors, assoc);
		repository.functionDefn(errors, hashPair);
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
		repository.functionDefn(errors, seconds);
		repository.functionDefn(errors, parseUri);
		repository.functionDefn(errors, parseJson);
	}
}
