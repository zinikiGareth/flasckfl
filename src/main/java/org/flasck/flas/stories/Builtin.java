package org.flasck.flas.stories;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.typechecker.Type;

public class Builtin {

	public static Scope builtinScope() {
		Scope ret = new Scope(null, null);
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		Type varA = Type.polyvar(posn, "A");
		Type varB = Type.polyvar(posn, "B");
		Type bool = Type.builtin(posn, "Boolean");
		Type number = Type.builtin(posn, "Number");
		Type string = Type.builtin(posn, "String");
		UnionTypeDefn any = new UnionTypeDefn(posn, false, "Any");
		{ // core
			/* PackageDefn fleval = */new PackageDefn(posn, ret, "FLEval");
			ret.define(".", "FLEval.field",		null); // special case handling
			ret.define("()", "FLEval.tuple",	null); // special case handling
			ret.define("if", "if",				Type.function(posn, bool, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.polyvar(posn, "A")));
			ret.define("let", "let", 			null);
			ret.define("Any", "Any", 			any);
		}
		UnionTypeDefn list = new UnionTypeDefn(posn, false, "List", Type.polyvar(posn, "A"));
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
		StructDefn nil = new StructDefn(posn, "Nil", false);
		{ // lists
			StructDefn cons = new StructDefn(posn, "Cons", false, varA);
			cons.addField(new StructField(false, varA, "head"));
			cons.addField(new StructField(false, list, "tail"));
			list.addCase(nil);
			list.addCase(cons);
			ret.define("List", "List",			list);
			ret.define("Nil", "Nil",			nil);
			ret.define("Cons", "Cons",			cons);
			ret.define("map", "map",			Type.function(posn, Type.function(posn, varA, varB), list.instance(posn, varA), list.instance(posn, varB)));
		}
		{ // stacks
			UnionTypeDefn stack = new UnionTypeDefn(posn, false, "Stack", Type.polyvar(posn, "A"));
			StructDefn push = new StructDefn(posn, "StackPush", false, varA);
			push.addField(new StructField(false, varA, "head"));
			push.addField(new StructField(false, stack, "tail"));
			stack.addCase(nil);
			stack.addCase(push);
			ret.define("Stack", "Stack",			stack);
			ret.define("StackPush", "StackPush",	push);
		}
		UnionTypeDefn map = new UnionTypeDefn(posn, false, "Map", varA);
		{ // maps
			StructDefn nilMap = new StructDefn(posn, "NilMap", false);
			StructDefn assoc = new StructDefn(posn, "Assoc", false, varA);
			map.addCase(nilMap);
			map.addCase(assoc);
			ret.define("Map", "Map", map);
			ret.define("NilMap", "NilMap", nilMap);
			ret.define("Assoc", "Assoc", assoc);
			assoc.addField(new StructField(false, string, "key"));
			assoc.addField(new StructField(false, varA, "value"));
			assoc.addField(new StructField(false, map, "rest"));
			ret.define("assoc", "StdLib.assoc",		Type.function(posn, map.instance(posn, varA), string, varA));
		}
		{ // d3
			StructDefn d3 = new StructDefn(posn, "D3Element", false, varA);
			ret.define("D3Element", "D3Element", d3);
			d3.addField(new StructField(false, varA, "data"));
			d3.addField(new StructField(false, number, "idx"));
		}
		StructDefn send = new StructDefn(posn, "Send", false);
		{ // messaging
			UnionTypeDefn message = new UnionTypeDefn(posn, false, "Message");
			StructDefn assign = new StructDefn(posn, "Assign", false);
			StructDefn crCard = new StructDefn(posn, "CreateCard", false);
			StructDefn d3 = new StructDefn(posn, "D3Action", false);
			StructDefn debug = new StructDefn(posn, "Debug", false);
			message.addCase(assign);
			message.addCase(send);
			message.addCase(crCard);
			message.addCase(debug);
			assign.addField(new StructField(false, any, "into"));
			assign.addField(new StructField(false, string, "slot"));
			assign.addField(new StructField(false, any, "value"));
				
			send.addField(new StructField(false, any, "dest"));
			send.addField(new StructField(false, string, "method"));
			send.addField(new StructField(false, list.instance(posn, any), "args"));

			crCard.addField(new StructField(false, map.instance(posn, string, any), "opts"));
			crCard.addField(new StructField(false, list.instance(posn, any), "contracts")); // maybe List[(String, CardHandle)] ?  what is CardHandle?  This is what I had "before"
			
			d3.addField(new StructField(false, string, "action"));
			d3.addField(new StructField(false, list.instance(posn, any), "args"));

			debug.addField(new StructField(false, any, "value"));

			ret.define("Assign", "Assign", assign);
			ret.define("Send", "Send", send);
			ret.define("CreateCard", "CreateCard", crCard);
			ret.define("D3Action", "D3Action", d3);
			ret.define("Debug", "Debug", debug);
			ret.define("Message", "Message", message);
//			ret.define("JSNI", "JSNI", null);
			
			Type polyT = Type.polyvar(posn, "T");
			StructDefn mw = new StructDefn(posn, "MessageWrapper", false, polyT);
			mw.addField(new StructField(false, polyT, "value"));
			mw.addField(new StructField(false, list.instance(posn, message), "msgs"));
			ret.define("MessageWrapper", "MessageWrapper", mw);

		}
		{ // DOM
			PackageDefn domPkg = new PackageDefn(posn, ret, "DOM");
			StructDefn elt = new StructDefn(posn, "DOM.Element", false, varA, varB);
			domPkg.innerScope().define("Element", "DOM.Element", elt);
			elt.addField(new StructField(false, string, "tag"));
			elt.addField(new StructField(false, list, "attrs"));
			elt.addField(new StructField(false, list.instance(posn, elt), "content"));
			elt.addField(new StructField(false, list.instance(posn, varB), "handlers"));
		}
		{ // crosets
			StructDefn crokey = new StructDefn(posn, "Crokey", false);
			ret.define("Crokey", "Crokey", crokey);
			crokey.addField(new StructField(false, string, "key"));
			crokey.addField(new StructField(false, string, "id"));

			// It is not abundantly clear to me that we want to project this level of detail into the API
			// This comes from having two separate classes down in the JS layer
			// At some level, we DO need to distinguish between them, but I'm not sure we should put it on the user
			StructDefn ncrokey = new StructDefn(posn, "NaturalCrokey", false);
			ret.define("NaturalCrokey", "NaturalCrokey", ncrokey);
			ncrokey.addField(new StructField(false, string, "key"));
			ncrokey.addField(new StructField(false, string, "id"));
			
			StructDefn crokeys = new StructDefn(posn, "Crokeys", false);
			ret.define("Crokeys", "Crokeys", crokeys);
			crokeys.addField(new StructField(false, string, "id"));
			crokeys.addField(new StructField(false, string, "keytype"));
			crokeys.addField(new StructField(false, list.instance(posn,  crokey), "keys"));

			ObjectDefn croset = new ObjectDefn(posn, ret, "Croset", false, varA);
			croset.constructorArg(crokeys, "init");
//			ret.define("Croset", "Croset", croset); // handled automatically by the constructor ...
			
			// These are actually accessors ...
			croset.addMethod(new ObjectMethod(Type.function(posn, string, any), "item"));
			croset.addMethod(new ObjectMethod(Type.function(posn, any, any), "member")); // crokey, natural crokey or string as input

			// These are real methods
			croset.addMethod(new ObjectMethod(Type.function(posn, any, send), "put"));
			croset.addMethod(new ObjectMethod(Type.function(posn, list.instance(posn,  any), send), "mergeAppend"));
			croset.addMethod(new ObjectMethod(Type.function(posn, string, send), "delete"));
			croset.addMethod(new ObjectMethod(Type.function(posn, string, any, send), "insert"));
			croset.addMethod(new ObjectMethod(Type.function(posn, send), "clear"));
		}
		return ret;
	}
}
