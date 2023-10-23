package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ServiceLoader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.modules.ProvideBuiltins;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FieldAccessor;
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
	public static final InputPosition pos = new InputPosition("BuiltIn", 1, 0, null, "<<builtin>>");

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
		error.addField(new StructField(pos, error, true, true, stringTR, "message"));
	}

	//   -> any
	public static final Primitive any = new Primitive(pos, "Any");
	public static final TypeReference anyTR = new TypeReference(pos, "Any");
	static {
		anyTR.bind(any);
		any.accept(ms -> { return true; });
	}
	
	/* TODO: I think we want subclasses of Any called "Entity", "Deal", "Offer", etc
	 * Not quite sure what etc. includes because I don't think "Primitive" and "Struct" hold any value
	 * Entity is obviously useful because we use it in Data Contracts
	 * Deal & Offer feel like they would come up in conversations about Commerce
	 * Countables, Currency and ValueStore need to go here too
	 */
	public static final Primitive entity = new Primitive(pos, "Entity");
	public static final TypeReference entityTR = new TypeReference(pos, "Entity");
	public static final FieldAccessor idAccessor = new EntityIdAccessor();

	static {
		entityTR.bind(entity);
		// TODO: this should be more precise in what it accepts
		entity.accept(ms -> { return true; });
	}
	
	public static final Primitive co = new Primitive(pos, "ContentObject");
	public static final TypeReference coTR = new TypeReference(pos, "ContentObject");
	static {
		coTR.bind(co);
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
		StructField head = new StructField(pos, cons, true, true, consATR_A, "head");
		head.fullName(new VarName(pos, cons.name(), "head"));
		cons.addField(head);
		PolyInstance pil = new PolyInstance(pos, list, Arrays.asList(cta));
		TypeReference piltr = new TypeReference(pos, "List", consATR_A);
		piltr.bind(pil);
		StructField tail = new StructField(pos, cons, true, true, piltr, "tail");
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
		StructField aihead = new StructField(pos, assignItem, true, true, aitr, "head");
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
		debug.addField(new StructField(pos, debug, true, true, stringTR, "message"));
		message.addCase(debugTR);
	}
	
	//   -> Send
	public static final TypeReference sendTR = new TypeReference(pos, "Send");
	public static final StructDefn send = new StructDefn(pos, FieldsType.STRUCT, null, "Send", false);
	static {
		sendTR.bind(send);
		send.addField(new StructField(pos, send, false, true, contractTR, "sendto"));
		send.addField(new StructField(pos, send, false, true, stringTR, "meth"));
		send.addField(new StructField(pos, send, false, true, listAnyTR, "args"));
		message.addCase(sendTR);
	}
	
	//   -> Assign
	public static final TypeReference assignTR = new TypeReference(pos, "Assign");
	public static final StructDefn assign = new StructDefn(pos, FieldsType.STRUCT, null, "Assign", false);
	static {
		assignTR.bind(assign);
		assign.addField(new StructField(pos, assign, false, true, anyTR, "on"));
		assign.addField(new StructField(pos, assign, false, true, stringTR, "fld"));
		assign.addField(new StructField(pos, assign, false, true, anyTR, "value"));
		message.addCase(assignTR);
	}
	
	//   -> AssignCons (this and assignitem seem somewhat duplicative)
	public static final StructDefn assignCons = new StructDefn(pos, FieldsType.STRUCT, null, "AssignCons", false);
	static {
		assignCons.addField(new StructField(pos, assignCons, false, true, anyTR, "on"));
		assignCons.addField(new StructField(pos, assignCons, false, true, anyTR, "value"));
		// why is this not added to message?
	}
	
	//   -> UpdateDisplay
	public static final TypeReference updateDisplayTR = new TypeReference(pos, "UpdateDisplay");
	public static final StructDefn updateDisplay = new StructDefn(pos, FieldsType.STRUCT, null, "UpdateDisplay", false);
	static {
		updateDisplayTR.bind(updateDisplay);
		message.addCase(updateDisplayTR);
	}

	/* Card is a primitive */

	public static final TypeReference cardTR = new TypeReference(pos, "Card");
	public static final Primitive card = new Primitive(pos, "Card");
	static {
		cardTR.bind(card);
	}

	/* Events */

	//   -> Event (the union)
	public static final UnionTypeDefn event = new UnionTypeDefn(pos, false, new SolidName(null, "Event"));
	
	//   -> ClickEvent
	public static final TypeReference clickEventTR = new TypeReference(pos, "ClickEvent");
	public static final StructDefn clickEvent = new StructDefn(pos, FieldsType.STRUCT, null, "ClickEvent", false);
	static {
		clickEventTR.bind(clickEvent);
		final StructField source = new StructField(pos, pos, clickEvent, true, true, anyTR, "source", new CurrentContainer(pos, clickEvent));
		clickEvent.addField(source);
		source.fullName(new VarName(pos, clickEvent.name(), "source"));
		event.addCase(clickEventTR);
	}

	//   -> ScrollTo
	public static final TypeReference scrollToTR = new TypeReference(pos, "ScrollTo");
	public static final StructDefn scrollTo = new StructDefn(pos, FieldsType.STRUCT, null, "ScrollTo", false);
	static {
		scrollToTR.bind(scrollTo);
		{
			final StructField whereTo = new StructField(pos, pos, scrollTo, true, true, anyTR, "to", null);
			scrollTo.addField(whereTo);
			whereTo.fullName(new VarName(pos, scrollTo.name(), "to"));
		}
		{
			final StructField source = new StructField(pos, pos, scrollTo, true, true, anyTR, "source", new CurrentContainer(pos, scrollTo));
			scrollTo.addField(source);
			source.fullName(new VarName(pos, scrollTo.name(), "source"));
		}
		event.addCase(scrollToTR);
	}

	//   -> Link
	public static final TypeReference linkTR = new TypeReference(pos, "Link");
	public static final StructDefn link = new StructDefn(pos, FieldsType.STRUCT, null, "Link", false);
	static {
		linkTR.bind(link);
		
		final StructField uri = new StructField(pos, pos, link, true, true, uriTR, "uri", null);
		link.addField(uri);
		uri.fullName(new VarName(pos, link.name(), "uri"));

		final StructField label = new StructField(pos, pos, link, true, true, stringTR, "label", null);
		link.addField(label);
		label.fullName(new VarName(pos, link.name(), "label"));
	}

	//   -> hlml
	public static final TypeReference hlmlTR = new TypeReference(pos, "hlml");
	public static final StructDefn hlml = new StructDefn(pos, FieldsType.STRUCT, null, "hlml", false);
	static {
		hlmlTR.bind(hlml);
		
		final StructField uri = new StructField(pos, pos, hlml, true, true, uriTR, "uri", null);
		hlml.addField(uri);
		uri.fullName(new VarName(pos, hlml.name(), "uri"));

		final StructField label = new StructField(pos, pos, hlml, true, true, stringTR, "label", null);
		hlml.addField(label);
		label.fullName(new VarName(pos, hlml.name(), "label"));
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

	// Image

	public static final TypeReference imageTR = new TypeReference(pos, "Image");
	public static final ObjectDefn image = new ObjectDefn(pos, pos, new ObjectName(null, "Image"), false, new ArrayList<>());
	static {
		imageTR.bind(image);
	}
	
	//   -> ctor Image.from
	private static ObjectCtor imageFrom;
	static {
		FunctionName ctorFrom = FunctionName.objectCtor(pos, image.name(), "from");
		imageFrom = new ObjectCtor(pos, image, ctorFrom, Arrays.asList(new TypedPattern(pos, coTR, new VarName(pos, ctorFrom, "co"))));
		imageFrom.dontGenerate();
		imageFrom.bindType(new Apply(co, image));
		image.addConstructor(imageFrom);
	}

	//   -> ctor Image.asset
	private static ObjectCtor imageAsset;
	static {
		FunctionName ctorAsset = FunctionName.objectCtor(pos, image.name(), "asset");
		imageAsset = new ObjectCtor(pos, image, ctorAsset, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, ctorAsset, "asset"))));
		imageAsset.dontGenerate();
		imageAsset.bindType(new Apply(string, image));
		image.addConstructor(imageAsset);
	}

	//   -> ctor Image.uri
	private static ObjectCtor imageUri;
	static {
		FunctionName ctorUri = FunctionName.objectCtor(pos, image.name(), "uri");
		imageUri = new ObjectCtor(pos, image, ctorUri, Arrays.asList(new TypedPattern(pos, uriTR, new VarName(pos, ctorUri, "uri"))));
		imageUri.dontGenerate();
		imageUri.bindType(new Apply(uri, image));
		image.addConstructor(imageUri);
	}

	// HTML
	public static final TypeReference htmlTR = new TypeReference(pos, "Html");
	public static final ObjectDefn html = new ObjectDefn(pos, pos, new ObjectName(null, "Html"), false, new ArrayList<>());
	static {
		htmlTR.bind(html);
	}
	

	//   -> ctor Html.from
	private static ObjectCtor htmlFrom;
	static {
		FunctionName ctorFrom = FunctionName.objectCtor(pos, html.name(), "from");
		htmlFrom = new ObjectCtor(pos, html, ctorFrom, Arrays.asList(new TypedPattern(pos, new TypeReference(pos, "AjaxMessage").bindDynamically(), new VarName(pos, ctorFrom, "msg"))));
		htmlFrom.dontGenerate();
		htmlFrom.dynamicallyType();
		// We cannot bind the type here because it depends on the stdlib
		// See afterFLIM below
		html.addConstructor(htmlFrom);
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
	
	//   -> method Crobag.insert
	private static ObjectMethod crobagInsert;
	static {
		FunctionName insert = FunctionName.objectMethod(pos, crobag.name(), "insert");
		crobagInsert = new ObjectMethod(pos, insert, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, insert, "key")), new TypedPattern(pos, entityTR, new VarName(pos, insert, "value"))), null, crobag);
		crobagInsert.dontGenerate();
		crobagInsert.bindType(new Apply(string, entity, listMessages));
		crobag.addMethod(crobagInsert);
	}

	//   -> method Crobag.put
	private static ObjectMethod crobagPut;
	static {
		FunctionName put = FunctionName.objectMethod(pos, crobag.name(), "put");
		crobagPut = new ObjectMethod(pos, put, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, put, "key")), new TypedPattern(pos, entityTR, new VarName(pos, put, "value"))), null, crobag);
		crobagPut.dontGenerate();
		crobagPut.bindType(new Apply(string, entity, listMessages));
		crobag.addMethod(crobagPut);
	}

	//   -> method Crobag.upsert
	private static ObjectMethod crobagUpsert;
	static {
		FunctionName upsert = FunctionName.objectMethod(pos, crobag.name(), "upsert");
		crobagUpsert = new ObjectMethod(pos, upsert, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, upsert, "key")), new TypedPattern(pos, entityTR, new VarName(pos, upsert, "value"))), null, crobag);
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

	
	// Calendar
	public static final TypeReference calendarTR = new TypeReference(pos, "Calendar");
	public static final ObjectDefn calendar = new ObjectDefn(pos, pos, new ObjectName(null, "Calendar"), false, new ArrayList<>());
	static {
		calendarTR.bind(calendar);
	}
	
	//   -> ctor Calendar.gregorian
	private static ObjectCtor calendarGregorian;
	static {
		FunctionName gregorian = FunctionName.objectCtor(pos, calendar.name(), "gregorian");
		calendarGregorian = new ObjectCtor(pos, calendar, gregorian, new ArrayList<>());
		calendarGregorian.dontGenerate();
		calendarGregorian.bindType(calendar);
		calendar.addConstructor(calendarGregorian);
	}
	
	//   -> method Calendar.timezone
	private static ObjectMethod calendarTimezone;
	static {
		FunctionName timezone = FunctionName.objectMethod(pos, calendar.name(), "timezone");
		calendarTimezone = new ObjectMethod(pos, timezone, Arrays.asList(new TypedPattern(pos, stringTR, new VarName(pos, timezone, "tz"))), null, calendar);
		calendarTimezone.dontGenerate();
		calendarTimezone.bindType(new Apply(string, listMessages));
		calendar.addMethod(calendarTimezone);
	}

	//   -> acor Calendar.isoDateTime
	private static ObjectAccessor calendarIsoDateTime;
	static {
		FunctionName idt = FunctionName.objectMethod(pos, calendar.name(), "isoDateTime");
		FunctionDefinition isodatetime = new FunctionDefinition(idt, 1, calendar);
		calendarIsoDateTime = new ObjectAccessor(calendar, isodatetime);
		calendarIsoDateTime.dontGenerate();
		isodatetime.bindType(new Apply(instant, string));
		calendar.addAccessor(calendarIsoDateTime);
	}
	//   -> acor Calendar.parseIsoDateTime
	private static ObjectAccessor calendarParseIsoDateTime;
	static {
		FunctionName pidt = FunctionName.objectMethod(pos, calendar.name(), "parseIsoDateTime");
		FunctionDefinition pidtFn = new FunctionDefinition(pidt, 1, null);
		calendarParseIsoDateTime = new ObjectAccessor(calendar, pidtFn);
		calendarParseIsoDateTime.dontGenerate();
		pidtFn.bindType(new Apply(string, instant));
		calendar.addAccessor(calendarParseIsoDateTime);
	}
	

	// Types
	public static final TypeReference typeTR = new TypeReference(pos, "Type");
	public static StructDefn type = new StructDefn(pos, FieldsType.STRUCT, null, "Type", false);

	static {
		typeTR.bind(type);
		type.addField(new StructField(pos, type, false, true, stringTR, "type"));
	}

	
	/* Functions */
	
	// Builtin operators

	// test for null/undefined
	public static final FunctionDefinition isNull = new FunctionDefinition(FunctionName.function(pos, null, "isNull"), 1, null).dontGenerate();
	// TODO: test for the more nebulous concept of Falsy?

	//   -> comparisons
	public static final FunctionDefinition isEqual = new FunctionDefinition(FunctionName.function(pos, null, "=="), 2, null).dontGenerate();
	public static final FunctionDefinition isNotEqual = new FunctionDefinition(FunctionName.function(pos, null, "<>"), 2, null).dontGenerate();
	public static final FunctionDefinition isGE = new FunctionDefinition(FunctionName.function(pos, null, ">="), 2, null).dontGenerate();
	public static final FunctionDefinition isGT = new FunctionDefinition(FunctionName.function(pos, null, ">"), 2, null).dontGenerate();
	public static final FunctionDefinition isLE = new FunctionDefinition(FunctionName.function(pos, null, "<="), 2, null).dontGenerate();
	public static final FunctionDefinition isLT = new FunctionDefinition(FunctionName.function(pos, null, "<"), 2, null).dontGenerate();
	static {
		isNull.bindType(new Apply(any, bool));
		{
			Type pa = new PolyType(pos, new SolidName(isEqual.name(), "A"));
			isEqual.bindType(new Apply(pa, pa, bool));
		}
		isNotEqual.bindType(new Apply(number, number, bool));
		isGE.bindType(new Apply(number, number, bool));
		isGT.bindType(new Apply(number, number, bool));
		isLE.bindType(new Apply(number, number, bool));
		isLT.bindType(new Apply(number, number, bool));
	}

	//   -> arithmetic
	public static final FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, null).dontGenerate();
	public static final FunctionDefinition unaryMinus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 1, null).dontGenerate();
	public static final FunctionDefinition minus = new FunctionDefinition(FunctionName.function(pos, null, "-"), 2, null).dontGenerate();
	public static final FunctionDefinition mul = new FunctionDefinition(FunctionName.function(pos, null, "*"), 2, null).dontGenerate();
	public static final FunctionDefinition div = new FunctionDefinition(FunctionName.function(pos, null, "/"), 2, null).dontGenerate();
	public static final FunctionDefinition mod = new FunctionDefinition(FunctionName.function(pos, null, "%"), 2, null).dontGenerate();
	public static final FunctionDefinition round = new FunctionDefinition(FunctionName.function(pos, null, "round"), 1, null).dontGenerate();
	static {
		plus.bindType(new Apply(number, number, number));
		unaryMinus.bindType(new Apply(number, number));
		minus.bindType(new Apply(number, number, number));
		mul.bindType(new Apply(number, number, number));
		div.bindType(new Apply(number, number, number));
		mod.bindType(new Apply(number, number, number));
		round.bindType(new Apply(number, number));
	}	
	
	//   -> boolean
	public static final FunctionDefinition not = new FunctionDefinition(FunctionName.function(pos, null, "!"), 1, null).dontGenerate();
	public static final FunctionDefinition and = new FunctionDefinition(FunctionName.function(pos, null, "&&"), 2, null).dontGenerate();
	public static final FunctionDefinition or = new FunctionDefinition(FunctionName.function(pos, null, "||"), 2, null).dontGenerate();
	static {
		not.bindType(new Apply(bool, bool));
		and.bindType(new Apply(bool, bool, bool));
		or.bindType(new Apply(bool, bool, bool));
	}	
	
	//   -> string concat
	public static final FunctionDefinition concat = new FunctionDefinition(FunctionName.function(pos, null, "++"), 2, null).dontGenerate();
	static {
		concat.bindType(new Apply(string, string, string));
	}	

	//   -> syntax support
	public static final FunctionDefinition makeTuple = new FunctionDefinition(FunctionName.function(pos, null, "()"), -1, null).dontGenerate();
	public static final FunctionDefinition handleSend = new FunctionDefinition(FunctionName.function(pos, null, "->"), 2, null).dontGenerate();
	static {
		makeTuple.bindType(tuple);
		handleSend.bindType(new Apply(new Apply(contract, send), contract, send)); // TODO: "contract" arg (in both places) should be specifically "Handler" I think
	}	

	//   -> internal functions
	public static final FunctionDefinition isType = new FunctionDefinition(FunctionName.function(pos, null, "istype"), 2, null).dontGenerate();
	public static final FunctionDefinition dispatch = new FunctionDefinition(FunctionName.function(pos, null, "dispatch"), 1, null).dontGenerate();
	public static final FunctionDefinition show = new FunctionDefinition(FunctionName.function(pos, null, "show"), 1, null).dontGenerate();
	public static final FunctionDefinition expr = new FunctionDefinition(FunctionName.function(pos, null, "expr"), 1, null).dontGenerate();
	static {
		isType.bindType(new Apply(type, any, bool));
		dispatch.bindType(new Apply(listMessages, listMessages));
		dispatch.restrict(new UTOnlyRestriction("dispatch"));
		show.bindType(new Apply(any, string));
		expr.bindType(new Apply(any, string));
	}	
	
	//   -> list functions
	public static final FunctionDefinition length = new FunctionDefinition(FunctionName.function(pos, null, "length"), 1, null).dontGenerate();
	public static final FunctionDefinition replace = new FunctionDefinition(FunctionName.function(pos, null, "replace"), 3, null).dontGenerate();
	public static final FunctionDefinition nth = new FunctionDefinition(FunctionName.function(pos, null, "nth"), 2, null).dontGenerate();
	public static final FunctionDefinition item = new FunctionDefinition(FunctionName.function(pos, null, "item"), 2, null).dontGenerate();
	public static final FunctionDefinition take = new FunctionDefinition(FunctionName.function(pos, null, "take"), 2, null).dontGenerate();
	public static final FunctionDefinition drop = new FunctionDefinition(FunctionName.function(pos, null, "drop"), 2, null).dontGenerate();
	public static final FunctionDefinition append = new FunctionDefinition(FunctionName.function(pos, null, "append"), 2, null).dontGenerate();
	public static final FunctionDefinition concatLists = new FunctionDefinition(FunctionName.function(pos, null, "concatLists"), 1, null).dontGenerate();
	static {
		length.bindType(new Apply(list, number));
		replace.bindType(new Apply(list, number, listA_A, list));
		nth.bindType(new Apply(number, list, listA_A));
		item.bindType(new Apply(number, list, assignItem));
		take.bindType(new Apply(number, list, list));
		drop.bindType(new Apply(number, list, list));
		append.bindType(new Apply(list, listA_A, list));
		concatLists.bindType(new Apply(new PolyInstance(pos, list, Arrays.asList(new PolyInstance(pos, list, Arrays.asList(listA_A)))), new PolyInstance(pos, list, Arrays.asList(listA_A))));
	}	
	
	
	//   -> hash functions
	public static final FunctionDefinition assoc = new FunctionDefinition(FunctionName.function(pos, null, "assoc"), 2, null).dontGenerate();
	public static final FunctionDefinition hashPair = new FunctionDefinition(FunctionName.function(pos, null, ":"), 2, null).dontGenerate();
	static {
		assoc.bindType(new Apply(hash, string, any));
		hashPair.bindType(new Apply(string, any, hashPairType));
	}	

	
	//   -> string functions
	public static final FunctionDefinition strlen = new FunctionDefinition(FunctionName.function(pos, null, "strlen"), 1, null).dontGenerate();
	static {
		strlen.bindType(new Apply(string, number));
	}

	
	//   -> date & time functions
	public static final FunctionDefinition seconds = new FunctionDefinition(FunctionName.function(pos, null, "seconds"), 1, null).dontGenerate();
	public static final FunctionDefinition milliseconds = new FunctionDefinition(FunctionName.function(pos, null, "milliseconds"), 1, null).dontGenerate();
	public static final FunctionDefinition unixdate = new FunctionDefinition(FunctionName.function(pos, null, "unixdate"), 1, null).dontGenerate();
	public static final FunctionDefinition fromunixdate = new FunctionDefinition(FunctionName.function(pos, null, "fromunixdate"), 1, null).dontGenerate();
	static {
		seconds.bindType(new Apply(number, interval));
		milliseconds.bindType(new Apply(number, interval));
		unixdate.bindType(new Apply(instant, number));
		fromunixdate.bindType(new Apply(number, instant));
	}	
	
	
	//   -> URI & JSON functions
	public static final FunctionDefinition parseUri = new FunctionDefinition(FunctionName.function(pos, null, "parseUri"), 1, null).dontGenerate();
	public static final FunctionDefinition parseJson = new FunctionDefinition(FunctionName.function(pos, null, "parseJson"), 1, null).dontGenerate();
	static {
		parseUri.bindType(new Apply(string, uri));
		parseJson.bindType(new Apply(string, hash));
	}	

	
	//   -> secret builtin functions for testing
	public static final UnresolvedVar probeState = new UnresolvedVar(pos, "_probe_state");
	public static final UnresolvedVar getUnderlying = new UnresolvedVar(pos, "_underlying");
	static {
		probeState.bind(new FunctionDefinition(FunctionName.function(pos, null, "_probe_state"), 2, null));
		getUnderlying.bind(new FunctionDefinition(FunctionName.function(pos, null, "_underlying"), 1, null));
	}

	public static void applyTo(ErrorReporter errors, Repository repository) {
		repository.addEntry(errors, any.name(), any);
		repository.addEntry(errors, new SolidName(null, "Entity"), entity);
		repository.addEntry(errors, contract.name(), contract);
		repository.newStruct(errors, error);
		repository.addEntry(errors, co.name(), co);
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

		repository.newObject(errors, image);
		repository.newObjectMethod(errors, imageFrom);
		repository.newObjectMethod(errors, imageAsset);
		repository.newObjectMethod(errors, imageUri);

		repository.newObject(errors, html);
		repository.newObjectMethod(errors, htmlFrom);

		repository.newObject(errors, crobag);
		repository.newObjectMethod(errors, crobagNew);
		repository.newObjectMethod(errors, crobagInsert);
		repository.newObjectMethod(errors, crobagPut);
		repository.newObjectMethod(errors, crobagUpsert);
		repository.newObjectMethod(errors, crobagWindow);
		repository.newObjectAccessor(errors, crobagSize);

		repository.newObject(errors, calendar);
		repository.newObjectMethod(errors, calendarGregorian);
		repository.newObjectAccessor(errors, calendarIsoDateTime);
		repository.newObjectAccessor(errors, calendarParseIsoDateTime);

		repository.newContract(errors, crobagWindowHandler);
		repository.newContract(errors, crobagSlideWindow);
		
		repository.newStruct(errors, debug);
		repository.newStruct(errors, send);
		repository.newStruct(errors, assign);
		repository.newStruct(errors, updateDisplay);
		repository.newUnion(errors, message);

		repository.addEntry(errors, card.name(), card);

		repository.newStruct(errors, clickEvent);
		repository.newStruct(errors, scrollTo);
		repository.newUnion(errors, event);

		repository.newStruct(errors, link);

		repository.functionDefn(errors, isNull);
		repository.functionDefn(errors, isEqual);
		repository.functionDefn(errors, isNotEqual);
		repository.functionDefn(errors, isGE);
		repository.functionDefn(errors, isGT);
		repository.functionDefn(errors, isLE);
		repository.functionDefn(errors, isLT);

		repository.functionDefn(errors, plus);
		// why not unaryMinus? because same symbol?
		repository.functionDefn(errors, minus);
		repository.functionDefn(errors, mul);
		repository.functionDefn(errors, div);
		repository.functionDefn(errors, mod);
		repository.functionDefn(errors, round);

		repository.functionDefn(errors, not);
		repository.functionDefn(errors, and);
		repository.functionDefn(errors, or);
		
		repository.functionDefn(errors, concat);

		repository.functionDefn(errors, makeTuple);
		repository.functionDefn(errors, handleSend);

		repository.functionDefn(errors, isType);
		repository.functionDefn(errors, dispatch);
		repository.functionDefn(errors, show);
		repository.functionDefn(errors, expr);

		repository.functionDefn(errors, length);
		repository.functionDefn(errors, replace);
		repository.functionDefn(errors, nth);
		repository.functionDefn(errors, item);
		repository.functionDefn(errors, drop);
		repository.functionDefn(errors, take);
		repository.functionDefn(errors, append);
		repository.functionDefn(errors, concatLists);

		repository.functionDefn(errors, assoc);
		repository.functionDefn(errors, hashPair);

		repository.functionDefn(errors, strlen);
		
		repository.functionDefn(errors, seconds);
		repository.functionDefn(errors, milliseconds);
		repository.functionDefn(errors, unixdate);
		repository.functionDefn(errors, fromunixdate);
		
		repository.functionDefn(errors, parseUri);
		repository.functionDefn(errors, parseJson);
		
		// not probe_state
		// not underlying
		
		// Look at any modules that might exist
		ServiceLoader<ProvideBuiltins> modules = ServiceLoader.load(ProvideBuiltins.class);
		for (ProvideBuiltins pb : modules)
			pb.applyTo(errors, repository);
	}
}
