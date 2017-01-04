package org.flasck.flas.flim;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.BooleanLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWObjectMethod;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.zinutils.collections.CollectionUtils;

public class Builtin {

	public static ImportPackage builtins() {
		ImportPackage root = new ImportPackage(null);
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = new PolyVar(posn, "A");
		Type varB = new PolyVar(posn, "B");
		Type bool = new PrimitiveType(posn, new StructName(null, "Boolean"));
		Type number = new PrimitiveType(posn, new StructName(null, "Number"));
		Type string = new PrimitiveType(posn, new StructName(null, "String"));
		RWUnionTypeDefn any = new RWUnionTypeDefn(posn, false, new StructName(null, "Any"), null);
		{ // core
			root.define("if", fnhelper("if", varA, varA, varA));
//			root.define("let", "let", 			null);
			root.define("Any", any);
		}
		RWUnionTypeDefn list = new RWUnionTypeDefn(posn, false, new StructName(null, "List"), CollectionUtils.listOf(new PolyVar(posn, "A")));
		{ // text
			root.define("String", string);
			root.define("concat", fnhelper("concat", list.instance(posn, string), string));
			root.define("join", fnhelper("join", list.instance(posn, string), string, string));
			root.define("++", fnhelper("++", string, string, string));
			root.define("string", fnhelper("string", any, string));
		}
		{ // boolean logic
			root.define("Boolean", bool);
			root.define("==", fnhelper("==", new PolyVar(posn, "A"), new PolyVar(posn, "A"), bool)); // Any -> Any -> Boolean
			root.define("true", new BooleanLiteral(posn, true));
			root.define("false", new BooleanLiteral(posn, false));
		}
		{ // math
			root.define("Number", number);
			root.define("+", fnhelper("+", number, number, number));
			root.define("-", fnhelper("-", number, number, number));
			root.define("*", fnhelper("*", number, number, number));
			root.define("/", fnhelper("/", number, number, number));
			root.define("^", fnhelper("^", number, number, number));
		}
		RWStructDefn nil = new RWStructDefn(posn, new StructName(null, "Nil"), false);
		list.addCase(nil);
		{ // lists
			RWStructDefn cons = new RWStructDefn(posn, new StructName(null, "Cons"), false, varA);
			cons.addField(new RWStructField(posn, false, varA, "head"));
			cons.addField(new RWStructField(posn, false, list, "tail"));
			list.addCase(cons);
			root.define("List", list);
			root.define("Nil", nil);
			root.define("Cons", cons);
			root.define("map", fnhelper("map", Type.function(posn, varA, varB), list.instance(posn, varA), list.instance(posn, varB)));
		}
		{ // stacks
			RWUnionTypeDefn stack = new RWUnionTypeDefn(posn, false, new StructName(null, "Stack"), CollectionUtils.listOf(new PolyVar(posn, "A")));
			RWStructDefn push = new RWStructDefn(posn, new StructName(null, "StackPush"), false, varA);
			push.addField(new RWStructField(posn, false, varA, "head"));
			push.addField(new RWStructField(posn, false, stack, "tail"));
			stack.addCase(nil);
			stack.addCase(push);
			root.define("Stack", stack);
			root.define("StackPush", push);
		}
		RWUnionTypeDefn map = new RWUnionTypeDefn(posn, false, new StructName(null, "Map"), CollectionUtils.listOf(varA));
		{ // maps
			RWStructDefn nilMap = new RWStructDefn(posn, new StructName(null, "NilMap"), false);
			RWStructDefn assoc = new RWStructDefn(posn, new StructName(null, "Assoc"), false, varA);
			map.addCase(nilMap);
			map.addCase(assoc);
			root.define("Map", map);
			root.define("NilMap", nilMap);
			root.define("Assoc", assoc);
			assoc.addField(new RWStructField(posn, false, string, "key"));
			assoc.addField(new RWStructField(posn, false, varA, "value"));
			assoc.addField(new RWStructField(posn, false, map, "rest"));
			root.define("assoc", fnhelper("assoc", map.instance(posn, varA), string, varA));
		}
		{ // d3
			RWStructDefn d3 = new RWStructDefn(posn, new StructName(null, "D3Element"), false, varA);
			root.define("D3Element", d3);
			d3.addField(new RWStructField(posn, false, varA, "data"));
			d3.addField(new RWStructField(posn, false, number, "idx"));
		}
		{
			RWObjectDefn card = new RWObjectDefn(posn, new StructName(null, "Card"), false);
			root.define("Card", card);
			card.constructorArg(posn, string, "explicit");
			card.constructorArg(posn, string, "loadId");
		}
		RWStructDefn send = new RWStructDefn(posn, new StructName(null, "Send"), false);
		{ // messaging
			RWUnionTypeDefn message = new RWUnionTypeDefn(posn, false, new StructName(null, "Message"), null);
			RWStructDefn assign = new RWStructDefn(posn, new StructName(null, "Assign"), false);
			RWStructDefn crCard = new RWStructDefn(posn, new StructName(null, "CreateCard"), false);
			RWStructDefn d3 = new RWStructDefn(posn, new StructName(null, "D3Action"), false);
			RWStructDefn debug = new RWStructDefn(posn, new StructName(null, "Debug"), false);
			message.addCase(assign);
			message.addCase(send);
			message.addCase(crCard);
			message.addCase(debug);
			assign.addField(new RWStructField(posn, false, any, "into"));
			assign.addField(new RWStructField(posn, false, string, "slot"));
			assign.addField(new RWStructField(posn, false, any, "value"));
				
			send.addField(new RWStructField(posn, false, any, "dest"));
			send.addField(new RWStructField(posn, false, string, "method"));
			send.addField(new RWStructField(posn, false, list.instance(posn, any), "args"));

			crCard.addField(new RWStructField(posn, false, map.instance(posn, string, any), "opts"));
			crCard.addField(new RWStructField(posn, false, list.instance(posn, any), "contracts")); // maybe List[(String, CardHandle)] ?  what is CardHandle?  This is what I had "before"
			
			d3.addField(new RWStructField(posn, false, string, "action"));
			d3.addField(new RWStructField(posn, false, list.instance(posn, any), "args"));

			debug.addField(new RWStructField(posn, false, any, "value"));

			root.define("Assign", assign);
			root.define("Send", send);
			root.define("CreateCard", crCard);
			root.define("D3Action", d3);
			root.define("Debug", debug);
			root.define("Message", message);
//			root.define("JSNI", "JSNI", null);
			
			Type polyT = new PolyVar(posn, "T");
			RWStructDefn mw = new RWStructDefn(posn, new StructName(null, "MessageWrapper"), false, polyT);
			mw.addField(new RWStructField(posn, false, polyT, "value"));
			mw.addField(new RWStructField(posn, false, list.instance(posn, message), "msgs"));
			root.define("MessageWrapper", mw);

		}
		{ // crosets
			RWStructDefn crokey = new RWStructDefn(posn, new StructName(null, "Crokey"), false);
			root.define("Crokey", crokey);
			crokey.addField(new RWStructField(posn, false, string, "key"));
			crokey.addField(new RWStructField(posn, false, string, "id"));

			// It is not abundantly clear to me that we want to project this level of detail into the API
			// This comes from having two separate classes down in the JS layer
			// At some level, we DO need to distinguish between them, but I'm not sure we should put it on the user
			RWStructDefn ncrokey = new RWStructDefn(posn, new StructName(null, "NaturalCrokey"), false);
			root.define("NaturalCrokey", ncrokey);
			ncrokey.addField(new RWStructField(posn, false, string, "key"));
			ncrokey.addField(new RWStructField(posn, false, string, "id"));
			
			RWStructDefn crokeys = new RWStructDefn(posn, new StructName(null, "Crokeys"), false);
			root.define("Crokeys", crokeys);
			crokeys.addField(new RWStructField(posn, false, string, "id"));
			crokeys.addField(new RWStructField(posn, false, string, "keytype"));
			crokeys.addField(new RWStructField(posn, false, list.instance(posn,  crokey), "keys"));

			RWObjectDefn croset = new RWObjectDefn(posn, new StructName(null, "Croset"), false, varA);
			root.define("Croset", croset);
			croset.constructorArg(posn, crokeys, "init");
			
			// These are actually accessors ...
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, varA), "item"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, any, varA), "member")); // crokey, natural crokey or string as input

			// These are real methods
			croset.addMethod(new RWObjectMethod(Type.function(posn, any, send), "put"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, list.instance(posn,  any), send), "mergeAppend"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, send), "delete"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, crokeys, send), "deleteSet"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, any, send), "insert"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, send), "clear"));
		}
		return root;
	}

	protected static RWFunctionDefinition fnhelper(String name, Type... args) {
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		RWFunctionDefinition ret = new RWFunctionDefinition(FunctionName.function(posn, null, name), args.length-1, false);
		ret.setType(Type.function(posn, args));
		return ret;
	}
	
	public ImportPackage domScope(ImportPackage root) {
		ImportPackage ret = new ImportPackage("DOM");
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = new PolyVar(posn, "A");
		Type varB = new PolyVar(posn, "B");
		Type string = (Type) root.get("String");
		Type list = (Type) root.get("List");
		{ // DOM
//			PackageDefn domPkg = new PackageDefn(posn, ret, "DOM");
			RWStructDefn elt = new RWStructDefn(posn, new StructName(null, "DOM.Element"), false, varA, varB);
//			domPkg.innerScope().define("Element", "DOM.Element", elt);
			elt.addField(new RWStructField(posn, false, string, "tag"));
			elt.addField(new RWStructField(posn, false, list, "attrs"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, elt), "content"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, varB), "handlers"));
		}
		return ret;
	}
}
