package org.flasck.flas.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWObjectMethod;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.CollectionUtils;

public class Builtin {

	public static List<ImportPackage> builtinScope() {
		List<ImportPackage> ret = new ArrayList<ImportPackage>();
		ImportPackage root = new ImportPackage(null);
		ImportPackage fleval = new ImportPackage("FLEval");
		ImportPackage stdlib = new ImportPackage("StdLib");
		ImportPackage dom = new ImportPackage("DOM");
		ret.add(root);
		ret.add(fleval);
		ret.add(stdlib);
		ret.add(dom);
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = Type.polyvar(posn, "A");
		Type varB = Type.polyvar(posn, "B");
		Type bool = Type.builtin(posn, "Boolean");
		Type number = Type.builtin(posn, "Number");
		Type string = Type.builtin(posn, "String");
		RWUnionTypeDefn any = new RWUnionTypeDefn(posn, false, "Any", null);
		{ // core
			/* PackageDefn fleval = */new PackageDefn(posn, ret, "FLEval");
			ret.define(".", "FLEval.field",		null); // special case handling
			ret.define("()", "FLEval.tuple",	null); // special case handling
			ret.define("if", "if",				Type.function(posn, bool, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.polyvar(posn, "A")));
			ret.define("let", "let", 			null);
			ret.define("Any", "Any", 			any);
		}
		RWUnionTypeDefn list = new RWUnionTypeDefn(posn, false, "List", CollectionUtils.listOf(Type.polyvar(posn, "A")));
		{ // text
			ret.define("String", "String",		string);
			ret.define("concat", "StdLib.concat",		Type.function(posn, list.instance(posn, string), string));
			ret.define("join", "join",			Type.function(posn, list.instance(posn, string), string, string));
			ret.define("++", "append",			Type.function(posn, string, string, string));
			ret.define("string", "asString",	Type.function(posn, any, string));
		}
		{ // boolean logic
			ret.define("Boolean", "Boolean",	bool);
			ret.define("==", "FLEval.compeq",	Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), bool)); // Any -> Any -> Boolean
		}
		{ // math
			ret.define("Number", "Number",		number);
			ret.define("+", "FLEval.plus",		Type.function(posn, number, number, number));
			ret.define("-", "FLEval.minus",		Type.function(posn, number, number, number));
			ret.define("*", "FLEval.mul",		Type.function(posn, number, number, number));
			ret.define("/", "FLEval.div",		Type.function(posn, number, number, number));
			ret.define("^", "FLEval.exp",		Type.function(posn, number, number, number));
		}
		RWStructDefn nil = new RWStructDefn(posn, "Nil", false);
		list.addCase(nil);
		{ // lists
			RWStructDefn cons = new RWStructDefn(posn, "Cons", false, varA);
			cons.addField(new RWStructField(posn, false, varA, "head"));
			cons.addField(new RWStructField(posn, false, list, "tail"));
			list.addCase(cons);
			ret.define("List", "List",			list);
			ret.define("Nil", "Nil",			nil);
			ret.define("Cons", "Cons",			cons);
			ret.define("map", "map",			Type.function(posn, Type.function(posn, varA, varB), list.instance(posn, varA), list.instance(posn, varB)));
		}
		{ // stacks
			RWUnionTypeDefn stack = new RWUnionTypeDefn(posn, false, "Stack", CollectionUtils.listOf(Type.polyvar(posn, "A")));
			RWStructDefn push = new RWStructDefn(posn, "StackPush", false, varA);
			push.addField(new RWStructField(posn, false, varA, "head"));
			push.addField(new RWStructField(posn, false, stack, "tail"));
			stack.addCase(nil);
			stack.addCase(push);
			ret.define("Stack", "Stack",			stack);
			ret.define("StackPush", "StackPush",	push);
		}
		RWUnionTypeDefn map = new RWUnionTypeDefn(posn, false, "Map", CollectionUtils.listOf(varA));
		{ // maps
			RWStructDefn nilMap = new RWStructDefn(posn, "NilMap", false);
			RWStructDefn assoc = new RWStructDefn(posn, "Assoc", false, varA);
			map.addCase(nilMap);
			map.addCase(assoc);
			ret.define("Map", "Map", map);
			ret.define("NilMap", "NilMap", nilMap);
			ret.define("Assoc", "Assoc", assoc);
			assoc.addField(new RWStructField(posn, false, string, "key"));
			assoc.addField(new RWStructField(posn, false, varA, "value"));
			assoc.addField(new RWStructField(posn, false, map, "rest"));
			ret.define("assoc", "StdLib.assoc",		Type.function(posn, map.instance(posn, varA), string, varA));
		}
		{ // d3
			RWStructDefn d3 = new RWStructDefn(posn, "D3Element", false, varA);
			ret.define("D3Element", "D3Element", d3);
			d3.addField(new RWStructField(posn, false, varA, "data"));
			d3.addField(new RWStructField(posn, false, number, "idx"));
		}
		{
			RWObjectDefn card = new RWObjectDefn(posn, "Card", false);
			card.constructorArg(posn, string, "explicit");
			card.constructorArg(posn, string, "loadId");
		}
		RWStructDefn send = new RWStructDefn(posn, "Send", false);
		{ // messaging
			RWUnionTypeDefn message = new RWUnionTypeDefn(posn, false, "Message", null);
			RWStructDefn assign = new RWStructDefn(posn, "Assign", false);
			RWStructDefn crCard = new RWStructDefn(posn, "CreateCard", false);
			RWStructDefn d3 = new RWStructDefn(posn, "D3Action", false);
			RWStructDefn debug = new RWStructDefn(posn, "Debug", false);
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

			ret.define("Assign", "Assign", assign);
			ret.define("Send", "Send", send);
			ret.define("CreateCard", "CreateCard", crCard);
			ret.define("D3Action", "D3Action", d3);
			ret.define("Debug", "Debug", debug);
			ret.define("Message", "Message", message);
//			ret.define("JSNI", "JSNI", null);
			
			Type polyT = Type.polyvar(posn, "T");
			RWStructDefn mw = new RWStructDefn(posn, "MessageWrapper", false, polyT);
			mw.addField(new RWStructField(posn, false, polyT, "value"));
			mw.addField(new RWStructField(posn, false, list.instance(posn, message), "msgs"));
			ret.define("MessageWrapper", "MessageWrapper", mw);

		}
		{ // DOM
			PackageDefn domPkg = new PackageDefn(posn, ret, "DOM");
			RWStructDefn elt = new RWStructDefn(posn, "DOM.Element", false, varA, varB);
			domPkg.innerScope().define("Element", "DOM.Element", elt);
			elt.addField(new RWStructField(posn, false, string, "tag"));
			elt.addField(new RWStructField(posn, false, list, "attrs"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, elt), "content"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, varB), "handlers"));
		}
		{ // crosets
			RWStructDefn crokey = new RWStructDefn(posn, "Crokey", false);
			ret.define("Crokey", "Crokey", crokey);
			crokey.addField(new RWStructField(posn, false, string, "key"));
			crokey.addField(new RWStructField(posn, false, string, "id"));

			// It is not abundantly clear to me that we want to project this level of detail into the API
			// This comes from having two separate classes down in the JS layer
			// At some level, we DO need to distinguish between them, but I'm not sure we should put it on the user
			RWStructDefn ncrokey = new RWStructDefn(posn, "NaturalCrokey", false);
			ret.define("NaturalCrokey", "NaturalCrokey", ncrokey);
			ncrokey.addField(new RWStructField(posn, false, string, "key"));
			ncrokey.addField(new RWStructField(posn, false, string, "id"));
			
			RWStructDefn crokeys = new RWStructDefn(posn, "Crokeys", false);
			ret.define("Crokeys", "Crokeys", crokeys);
			crokeys.addField(new RWStructField(posn, false, string, "id"));
			crokeys.addField(new RWStructField(posn, false, string, "keytype"));
			crokeys.addField(new RWStructField(posn, false, list.instance(posn,  crokey), "keys"));

			RWObjectDefn croset = new RWObjectDefn(posn, "Croset", false, varA);
			ret.define("Croset", "Croset", croset);
			croset.constructorArg(posn, crokeys, "init");
			
			// These are actually accessors ...
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, any), "item"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, any, any), "member")); // crokey, natural crokey or string as input

			// These are real methods
			croset.addMethod(new RWObjectMethod(Type.function(posn, any, send), "put"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, list.instance(posn,  any), send), "mergeAppend"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, send), "delete"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, crokeys, send), "deleteSet"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, string, any, send), "insert"));
			croset.addMethod(new RWObjectMethod(Type.function(posn, send), "clear"));
		}
		return ret;
	}
}
