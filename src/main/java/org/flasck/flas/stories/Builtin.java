package org.flasck.flas.stories;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.typechecker.Type;

public class Builtin {

	public static Scope builtinScope() {
		Scope ret = new Scope((ScopeEntry)null);
		InputPosition posn = new InputPosition("builtin", 0, 0, "builtin");
		{ // core
			/* PackageDefn fleval = */new PackageDefn(posn, ret, "FLEval");
			ret.define(".", "FLEval.field", 
				null); // special case handling
			ret.define("()", "FLEval.tuple", 
				null); // special case handling
			ret.define("if", "if",
				Type.function(posn, Type.simple(posn, "Boolean"), Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.polyvar(posn, "A")));
			ret.define("let", "let",
				null);
			ret.define("Any", "Any",
				Type.simple(posn, "Any"));
		}
		{ // text
			ret.define("String", "String",
				Type.simple(posn, "String"));
			ret.define("concat", "concat",
				Type.function(posn, Type.simple(posn, "List", Type.simple(posn, "String")), Type.simple(posn, "String")));
			ret.define("join", "join",
				Type.function(posn, Type.simple(posn, "List", Type.simple(posn, "String")), Type.simple(posn, "String"), Type.simple(posn, "String")));
			ret.define("++", "append",
				Type.function(posn, Type.simple(posn, "String"), Type.simple(posn, "String"), Type.simple(posn, "String")));
		}
		{ // boolean logic
			ret.define("Boolean", "Boolean",
				Type.simple(posn, "Boolean"));
			ret.define("==", "FLEval.compeq",
				Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.simple(posn, "Boolean"))); // Any -> Any -> Boolean
		}
		{ // math
			ret.define("Number", "Number",
				Type.simple(posn, "Number"));
			ret.define("+", "FLEval.plus", 
				Type.function(posn, Type.simple(posn, "Number"), Type.simple(posn, "Number"), Type.simple(posn, "Number")));
			ret.define("-", "FLEval.minus",
				Type.function(posn, Type.simple(posn, "Number"), Type.simple(posn, "Number"), Type.simple(posn, "Number")));
			ret.define("*", "FLEval.mul",
				Type.function(posn, Type.simple(posn, "Number"), Type.simple(posn, "Number"), Type.simple(posn, "Number")));
			ret.define("/", "FLEval.div",
				Type.function(posn, Type.simple(posn, "Number"), Type.simple(posn, "Number"), Type.simple(posn, "Number")));
			ret.define("^", "FLEval.exp",
				Type.function(posn, Type.simple(posn, "Number"), Type.simple(posn, "Number"), Type.simple(posn, "Number")));
		}
		{ // lists
			ret.define("List", "List",
				new TypeDefn(posn, false, new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")))
				.addCase(new TypeReference(null, "Nil", null))
				.addCase(new TypeReference(null, "Cons", null).with(new TypeReference(null, null, "A"))));
			ret.define("Nil", "Nil",
				new StructDefn(posn, "Nil", false));
			ret.define("Cons", "Cons",
				new StructDefn(posn, "Cons", false)
				.add("A")
				.addField(new StructField(new TypeReference(null, null, "A"), "head"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")), "tail")));
			ret.define("map", "map",
				Type.function(posn, Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "B")), Type.simple(posn, "List", Type.polyvar(posn, "A")), Type.simple(posn, "List", Type.polyvar(posn, "B"))));
		}
		{ // maps
			ret.define("Map", "Map",
				new TypeDefn(posn, false, new TypeReference(null, "Map", null).with(new TypeReference(null, null, "A")).with(new TypeReference(null, null, "B")))
				.addCase(new TypeReference(null, "NilMap", null))
				.addCase(new TypeReference(null, "Assoc", null).with(new TypeReference(null, null, "A")).with(new TypeReference(null, null, "B"))));
			ret.define("NilMap", "NilMap",
				new StructDefn(posn, "NilMap", false));
			ret.define("Assoc", "Assoc",
				new StructDefn(posn, "Assoc", false)
				.add("A")
				.add("B")
				.addField(new StructField(new TypeReference(null, null, "A"), "key"))
				.addField(new StructField(new TypeReference(null, null, "B"), "value"))
				.addField(new StructField(new TypeReference(null, "Map", null).with(new TypeReference(null, null, "A")).with(new TypeReference(null, null, "B")), "rest")));
		}
		{ // crosets
			ret.define("Croset", "Croset",
				new StructDefn(posn, "Croset", false).add("A")
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")), "list")));
		}
		{ // d3
			ret.define("D3Element", "D3Element",
				new StructDefn(posn, "D3Element", false).add("A")
				.addField(new StructField(new TypeReference(null, null, "A"), "data"))
				.addField(new StructField(new TypeReference(null, "Number", null), "idx")));
		}
		{ // messaging
			ret.define("Message", "Message", null);
			ret.define("Assign", "Assign",
				new StructDefn(posn, "Assign", false)
				.add("A")
				.addField(new StructField(new TypeReference(null, "String", null), "slot"))
				.addField(new StructField(new TypeReference(null, null, "A"), "value")));
			ret.define("Send", "Send",
				new StructDefn(posn, "Send", false)
				.addField(new StructField(new TypeReference(null, "Any", null), "dest"))
				.addField(new StructField(new TypeReference(null, "String", null), "method"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, "Any", null)), "args")));
			ret.define("CreateCard", "CreateCard",
				new StructDefn(posn, "CreateCard", false)
				.addField(new StructField(new TypeReference(null, "Any", null), "explicitCard")) // type should probably be String|Card where Card is some kind of "interface" type thing
				.addField(new StructField(new TypeReference(null, "Any", null), "value"))
				.addField(new StructField(
						new TypeReference(null, "Map", null).
						with(new TypeReference(null, "String", null)).
						with(new TypeReference(null, "Any", null)), "opts"))
				.addField(new StructField(
						new TypeReference(null, "List", null)
							.with(new TypeReference(null, "()", null)
								.with(new TypeReference(null, "String", null))
								.with(new TypeReference(null, "CardHandle", null))), "contracts")));
			ret.define("JSNI", "JSNI", null);
			ret.define("D3Action", "D3Action",
				new StructDefn(posn, "D3Action", false)
				.addField(new StructField(new TypeReference(null, "String", null), "action"))
				.addField(new StructField(new TypeReference(null, "List", null), "args")));
		}
		{ // DOM
			PackageDefn dom = new PackageDefn(posn, ret, "DOM");
			dom.innerScope().define("Element", "DOM.Element",
				new StructDefn(posn, "DOM.Element", false)
				.addField(new StructField(new TypeReference(null, "String", null), "tag"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")), "attrs"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, "DOM.Element", null)), "content"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "B")), "handlers")));
		}
		{ // Ziniki
//			PackageDefn dom = new PackageDefn(ret, "org");
//			PackageDefn ziniki = new PackageDefn(dom.innerScope(), "ziniki");
			// TODO: I think this should be in org.cardstack
//			ziniki.innerScope().define("Init", "org.ziniki.Init",
//				new ContractDecl(null, "org.ziniki.Init"));
//			ziniki.innerScope().define("KeyValue", "org.ziniki.KeyValue",
//					new ContractDecl(null, "org.ziniki.KeyValue"));
//			ContractDecl creds = new ContractDecl(null, "org.ziniki.Credentials");
//			creds.methods.add(new ContractMethodDecl("up", "logout", new ArrayList<Object>()));
//			ziniki.innerScope().define("Credentials", "org.ziniki.Credentials", creds);
//			ContractDecl qc = new ContractDecl(null, "org.ziniki.Query");
//			qc.methods.add(new ContractMethodDecl("up", "scan", new ArrayList<Object>()));
//			ziniki.innerScope().define("Query", "org.ziniki.Query",	qc);
//			ziniki.innerScope().define("QueryHandler", "org.ziniki.QueryHandler",
//					new ContractDecl(null, "org.ziniki.QueryHandler"));
		}
		return ret;
	}

}
