package org.flasck.flas.flim;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWObjectMethod;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.collections.CollectionUtils;

public class Builtin {

	public static ImportPackage builtinScope() {
//		List<ImportPackage> ret = new ArrayList<ImportPackage>();
		ImportPackage root = new ImportPackage(null);
//		ImportPackage fleval = new ImportPackage("FLEval");
//		ImportPackage stdlib = new ImportPackage("StdLib");
//		ImportPackage dom = new ImportPackage("DOM");
//		ret.add(root);
//		ret.add(fleval);
//		ret.add(stdlib);
//		ret.add(dom);
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = Type.polyvar(posn, "A");
		Type varB = Type.polyvar(posn, "B");
		Type bool = Type.builtin(posn, "Boolean");
		Type number = Type.builtin(posn, "Number");
		Type string = Type.builtin(posn, "String");
		RWUnionTypeDefn any = new RWUnionTypeDefn(posn, false, "Any", null);
		{ // core
//			/* PackageDefn fleval = */new PackageDefn(posn, ret, "FLEval");
//			root.define(".", "FLEval.field",		null); // special case handling
//			root.define("()", "FLEval.tuple",	null); // special case handling
			root.define("if", "if",				fnhelper("if", varA, varA, varA));
//			root.define("let", "let", 			null);
			root.define("Any", "Any", 			any);
		}
		RWUnionTypeDefn list = new RWUnionTypeDefn(posn, false, "List", CollectionUtils.listOf(Type.polyvar(posn, "A")));
		{ // text
			root.define("String", "String",		string);
			root.define("concat", "StdLib.concat",		fnhelper("concat", list.instance(posn, string), string));
			root.define("join", "join",			fnhelper("join", list.instance(posn, string), string, string));
			root.define("++", "append",			fnhelper("++", string, string, string));
			root.define("string", "asString",	fnhelper("string", any, string));
		}
		{ // boolean logic
			root.define("Boolean", "Boolean",	bool);
			root.define("==", "FLEval.compeq",	fnhelper("==", Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), bool)); // Any -> Any -> Boolean
		}
		{ // math
			root.define("Number", "Number",		number);
			root.define("+", "FLEval.plus",		fnhelper("+", number, number, number));
			root.define("-", "FLEval.minus",	fnhelper("-", number, number, number));
			root.define("*", "FLEval.mul",		fnhelper("*", number, number, number));
			root.define("/", "FLEval.div",		fnhelper("/", number, number, number));
			root.define("^", "FLEval.exp",		fnhelper("^", number, number, number));
		}
		RWStructDefn nil = new RWStructDefn(posn, "Nil", false);
		list.addCase(nil);
		{ // lists
			RWStructDefn cons = new RWStructDefn(posn, "Cons", false, varA);
			cons.addField(new RWStructField(posn, false, varA, "head"));
			cons.addField(new RWStructField(posn, false, list, "tail"));
			list.addCase(cons);
			root.define("List", "List",			list);
			root.define("Nil", "Nil",			nil);
			root.define("Cons", "Cons",			cons);
			root.define("map", "map",			fnhelper("map", Type.function(posn, varA, varB), list.instance(posn, varA), list.instance(posn, varB)));
		}
		{ // stacks
			RWUnionTypeDefn stack = new RWUnionTypeDefn(posn, false, "Stack", CollectionUtils.listOf(Type.polyvar(posn, "A")));
			RWStructDefn push = new RWStructDefn(posn, "StackPush", false, varA);
			push.addField(new RWStructField(posn, false, varA, "head"));
			push.addField(new RWStructField(posn, false, stack, "tail"));
			stack.addCase(nil);
			stack.addCase(push);
			root.define("Stack", "Stack",			stack);
			root.define("StackPush", "StackPush",	push);
		}
		RWUnionTypeDefn map = new RWUnionTypeDefn(posn, false, "Map", CollectionUtils.listOf(varA));
		{ // maps
			RWStructDefn nilMap = new RWStructDefn(posn, "NilMap", false);
			RWStructDefn assoc = new RWStructDefn(posn, "Assoc", false, varA);
			map.addCase(nilMap);
			map.addCase(assoc);
			root.define("Map", "Map", map);
			root.define("NilMap", "NilMap", nilMap);
			root.define("Assoc", "Assoc", assoc);
			assoc.addField(new RWStructField(posn, false, string, "key"));
			assoc.addField(new RWStructField(posn, false, varA, "value"));
			assoc.addField(new RWStructField(posn, false, map, "rest"));
			root.define("assoc", "StdLib.assoc",		fnhelper("assoc", map.instance(posn, varA), string, varA));
		}
		{ // d3
			RWStructDefn d3 = new RWStructDefn(posn, "D3Element", false, varA);
			root.define("D3Element", "D3Element", d3);
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

			root.define("Assign", "Assign", assign);
			root.define("Send", "Send", send);
			root.define("CreateCard", "CreateCard", crCard);
			root.define("D3Action", "D3Action", d3);
			root.define("Debug", "Debug", debug);
			root.define("Message", "Message", message);
//			root.define("JSNI", "JSNI", null);
			
			Type polyT = Type.polyvar(posn, "T");
			RWStructDefn mw = new RWStructDefn(posn, "MessageWrapper", false, polyT);
			mw.addField(new RWStructField(posn, false, polyT, "value"));
			mw.addField(new RWStructField(posn, false, list.instance(posn, message), "msgs"));
			root.define("MessageWrapper", "MessageWrapper", mw);

		}
		{ // crosets
			RWStructDefn crokey = new RWStructDefn(posn, "Crokey", false);
			root.define("Crokey", "Crokey", crokey);
			crokey.addField(new RWStructField(posn, false, string, "key"));
			crokey.addField(new RWStructField(posn, false, string, "id"));

			// It is not abundantly clear to me that we want to project this level of detail into the API
			// This comes from having two separate classes down in the JS layer
			// At some level, we DO need to distinguish between them, but I'm not sure we should put it on the user
			RWStructDefn ncrokey = new RWStructDefn(posn, "NaturalCrokey", false);
			root.define("NaturalCrokey", "NaturalCrokey", ncrokey);
			ncrokey.addField(new RWStructField(posn, false, string, "key"));
			ncrokey.addField(new RWStructField(posn, false, string, "id"));
			
			RWStructDefn crokeys = new RWStructDefn(posn, "Crokeys", false);
			root.define("Crokeys", "Crokeys", crokeys);
			crokeys.addField(new RWStructField(posn, false, string, "id"));
			crokeys.addField(new RWStructField(posn, false, string, "keytype"));
			crokeys.addField(new RWStructField(posn, false, list.instance(posn,  crokey), "keys"));

			RWObjectDefn croset = new RWObjectDefn(posn, "Croset", false, varA);
			root.define("Croset", "Croset", croset);
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
		return root;
	}

	protected static RWFunctionDefinition fnhelper(String name, Object... args) {
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		return new RWFunctionDefinition(posn, CodeType.FUNCTION, new RWFunctionIntro(posn, name, CollectionUtils.listOf(args), null), null, false);
	}
	
	public ImportPackage domScope(ImportPackage root) {
		ImportPackage ret = new ImportPackage("DOM");
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = Type.polyvar(posn, "A");
		Type varB = Type.polyvar(posn, "B");
		Type string = (Type) root.get("String");
		Type list = (Type) root.get("List");
		{ // DOM
//			PackageDefn domPkg = new PackageDefn(posn, ret, "DOM");
			RWStructDefn elt = new RWStructDefn(posn, "DOM.Element", false, varA, varB);
//			domPkg.innerScope().define("Element", "DOM.Element", elt);
			elt.addField(new RWStructField(posn, false, string, "tag"));
			elt.addField(new RWStructField(posn, false, list, "attrs"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, elt), "content"));
			elt.addField(new RWStructField(posn, false, list.instance(posn, varB), "handlers"));
		}
		return ret;
	}
}
