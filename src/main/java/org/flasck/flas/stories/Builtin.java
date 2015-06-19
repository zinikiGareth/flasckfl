package org.flasck.flas.stories;

import java.util.ArrayList;

import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
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
		{ // core
			/* PackageDefn fleval = */new PackageDefn(ret, "FLEval");
			ret.define(".", "FLEval.field", 
				Type.function(Type.polyvar("A"), Type.simple("String"), Type.polyvar("B")));
			ret.define("()", "FLEval.tuple", 
					null);
			ret.define("if", "if",
				Type.function(Type.simple("Boolean"), Type.polyvar("A"), Type.polyvar("A"), Type.polyvar("A")));
			ret.define("let", "let",
					null);
		}
		{ // text
			ret.define("String", "String", null);
			ret.define("concat", "concat",
				Type.function(Type.simple("List", Type.simple("String")), Type.simple("String")));
			ret.define("join", "join",
					Type.function(Type.simple("List", Type.simple("String")), Type.simple("String"), Type.simple("String")));
		}
		{ // boolean logic
			ret.define("Boolean", "Boolean", null);
			ret.define("==", "FLEval.compeq",
				Type.function(Type.polyvar("A"), Type.polyvar("A"), Type.simple("Boolean"))); // Any -> Any -> Boolean
		}
		{ // math
			ret.define("Number", "Number", null);
			ret.define("+", "FLEval.plus", 
				Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
			ret.define("-", "FLEval.minus", null);
			ret.define("*", "FLEval.mul", null);
			ret.define("/", "FLEval.div", null);
			ret.define("^", "FLEval.exp", null);
		}
		{ // lists
			ret.define("List", "List",
				new TypeDefn(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")))
				.addCase(new TypeReference(null, "Nil", null))
				.addCase(new TypeReference(null, "Cons", null).with(new TypeReference(null, null, "A"))));
			ret.define("Nil", "Nil",
				new StructDefn("Nil", false));
			ret.define("Cons", "Cons",
				new StructDefn("Cons", false)
				.add("A")
				.addField(new StructField(new TypeReference(null, null, "A"), "head"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")), "tail")));
		}
		{ // messaging
			ret.define("Message", "Message", null);
			ret.define("Assign", "Assign",
				new StructDefn("Assign", false)
				.add("A")
				.addField(new StructField(new TypeReference(null, "String", null), "slot"))
				.addField(new StructField(new TypeReference(null, null, "A"), "value")));
			ret.define("Send", "Send",
				new StructDefn("Send", false)
				.addField(new StructField(new TypeReference(null, "Any", null), "dest"))
				.addField(new StructField(new TypeReference(null, "String", null), "method"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, "Any", null)), "args")));
			ret.define("CreateCard", "CreateCard",
				new StructDefn("CreateCard", false)
				.addField(new StructField(new TypeReference(null, "Any", null), "explicitCard")) // type should probably be String|Card where Card is some kind of "interface" type thing
				.addField(new StructField(new TypeReference(null, "Any", null), "value"))
				.addField(new StructField(
						new TypeReference(null, "List", null)
							.with(new TypeReference(null, "()", null)
								.with(new TypeReference(null, "String", null))
								.with(new TypeReference(null, "CardHandle", null))), "contracts")));
			ret.define("JSNI", "JSNI", null);
		}
		{ // DOM
			PackageDefn dom = new PackageDefn(ret, "DOM");
			dom.innerScope().define("Element", "DOM.Element",
				new StructDefn("DOM.Element", false)
				.addField(new StructField(new TypeReference(null, "String", null), "tag"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "A")), "attrs"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, "DOM.Element", null)), "content"))
				.addField(new StructField(new TypeReference(null, "List", null).with(new TypeReference(null, null, "B")), "handlers")));
		}
		{ // Ziniki
			PackageDefn dom = new PackageDefn(ret, "org");
			PackageDefn ziniki = new PackageDefn(dom.innerScope(), "ziniki");
			// TODO: I think this should be in org.cardstack
			ziniki.innerScope().define("Init", "org.ziniki.Init",
				new ContractDecl("org.ziniki.Init"));
			ziniki.innerScope().define("KeyValue", "org.ziniki.KeyValue",
					new ContractDecl("org.ziniki.KeyValue"));
			ContractDecl qc = new ContractDecl("org.ziniki.Query");
			qc.methods.add(new ContractMethodDecl("up", "scan", new ArrayList<Object>()));
			ziniki.innerScope().define("Query", "org.ziniki.Query",
					qc);
			ziniki.innerScope().define("QueryHandler", "org.ziniki.QueryHandler",
					new ContractDecl("org.ziniki.QueryHandler"));
		}
		return ret;
	}

}
