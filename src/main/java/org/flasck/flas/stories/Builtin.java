package org.flasck.flas.stories;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.typechecker.Type;

public class Builtin {

	public static Scope builtinScope() {
		Scope ret = new Scope((ScopeEntry)null);
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
			ret.define("concat", "concat",		Type.function(posn, list.instance(posn, string), string));
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
		{ // lists
			StructDefn nil = new StructDefn(posn, "Nil", false);
			StructDefn cons = new StructDefn(posn, "Cons", false, varA);
			cons.addField(new StructField(varA, "head"));
			cons.addField(new StructField(list, "tail"));
			list.addCase(nil);
			list.addCase(cons);
			ret.define("List", "List",			list);
			ret.define("Nil", "Nil",			nil);
			ret.define("Cons", "Cons",			cons);
			ret.define("map", "map",			Type.function(posn, Type.function(posn, varA, varB), list.instance(posn, varA), list.instance(posn, varB)));
		}
		UnionTypeDefn map = new UnionTypeDefn(posn, false, "Map", varA, varB);
		{ // maps
			StructDefn nilMap = new StructDefn(posn, "NilMap", false);
			StructDefn assoc = new StructDefn(posn, "Assoc", false);
			map.addCase(nilMap);
			map.addCase(assoc);
			ret.define("Map", "Map", map);
			ret.define("NilMap", "NilMap", nilMap);
			ret.define("Assoc", "Assoc", assoc);
			assoc.addField(new StructField(varA, "key"));
			assoc.addField(new StructField(varB, "value"));
			assoc.addField(new StructField(map, "rest"));
		}
		{ // d3
			StructDefn d3 = new StructDefn(posn, "D3Element", false, varA);
			ret.define("D3Element", "D3Element", d3);
			d3.addField(new StructField(varA, "data"));
			d3.addField(new StructField(number, "idx"));
		}
		StructDefn send = new StructDefn(posn, "Send", false);
		{ // messaging
			UnionTypeDefn message = new UnionTypeDefn(posn, false, "Message");
			StructDefn assign = new StructDefn(posn, "Assign", false);
			StructDefn crCard = new StructDefn(posn, "CreateCard", false);
			StructDefn d3 = new StructDefn(posn, "D3Action", false);
			message.addCase(assign);
			message.addCase(send);
			message.addCase(crCard);
			assign.addField(new StructField(any, "into"));
			assign.addField(new StructField(string, "slot"));
			assign.addField(new StructField(any, "value"));
				
			send.addField(new StructField(any, "dest"));
			send.addField(new StructField(string, "method"));
			send.addField(new StructField(list.instance(posn, any), "args"));
			crCard.addField(new StructField(any, "explicitCard")); // type should probably be String|Card where Card is some kind of "interface" type thing
			crCard.addField(new StructField(any, "value"));
			crCard.addField(new StructField(map.instance(posn, string, any), "opts"));
			crCard.addField(new StructField(list.instance(posn, any), "contracts")); // maybe List[(String, CardHandle)] ?  what is CardHandle?  This is what I had "before"
			ret.define("Assign", "Assign", assign);
			ret.define("Send", "Send", send);
			ret.define("CreateCard", "CreateCard", crCard);
			ret.define("D3Action", "D3Action", d3);
			ret.define("Message", "Message", message);
//			ret.define("JSNI", "JSNI", null);
				
			d3.addField(new StructField(string, "action"));
			d3.addField(new StructField(list.instance(posn, any), "args"));
		}
		{ // DOM
			PackageDefn domPkg = new PackageDefn(posn, ret, "DOM");
			StructDefn elt = new StructDefn(posn, "DOM.Element", false, varA, varB);
			domPkg.innerScope().define("Element", "DOM.Element", elt);
			elt.addField(new StructField(string, "tag"));
			elt.addField(new StructField(list, "attrs"));
			elt.addField(new StructField(list.instance(posn, elt), "content"));
			elt.addField(new StructField(list.instance(posn, varB), "handlers"));
		}
		{ // crosets
			ObjectDefn croset = new ObjectDefn(posn, "Croset", false, varA);
			ret.define("Croset", "Croset", croset);
			croset.addMethod(new ObjectMethod(Type.function(posn, any, send), "put"));
			croset.addMethod(new ObjectMethod(Type.function(posn, list.instance(posn,  any), send), "mergeAppend"));
			croset.addMethod(new ObjectMethod(Type.function(posn, string, send), "delete"));
		}
		return ret;
	}
}
