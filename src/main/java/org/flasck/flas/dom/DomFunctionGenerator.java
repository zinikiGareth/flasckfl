package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.exceptions.UtilException;

public class DomFunctionGenerator {
	private final Map<String, FunctionDefinition> functions;
	private final Scope scope;
	private final StateDefinition state;
	private int node = 0;

	public DomFunctionGenerator(Map<String, FunctionDefinition> functions, Scope scope, StateDefinition state) {
		this.functions = functions;
		this.scope = scope;
		this.state = state;
	}

	// Steps:
	//  we need to break down the overall thing into lines & (content) nodes
	//  we need to generate appropriate functions
	//  we need to create the maps of overall tree and dependencies

	public void generate(TemplateLine template) {
		generateOne(template);
		for (TemplateLine tl : template.nested)
			generate(tl);
	}
	
	// TODO: this shouldn't take both a "name" and a "line"
	// I think there should be a name-generator in this class
	// We should then iterate over the "contents" array (if not a div/list)
	public void generateOne(TemplateLine tl) {
		if (tl.contents.isEmpty())
			function(div(tl));
		else {
			for (Object x : tl.contents) {
				if (x instanceof TemplateToken) {
					TemplateToken tt = (TemplateToken)x;
					// This is the case for a simple (variable) content item
					// TODO: it goes with a "type: content" entry in the tree
					if (tt.type == TemplateToken.IDENTIFIER) {
						// TODO: distinguish between state vars and functions to call
						// TODO: check that functions are defined on the card and not global
						function(ItemExpr.str(tt.text));
					} else if (tt.type == TemplateToken.STRING)
						function(ItemExpr.str(tt.text));
					else if (tt.type == TemplateToken.DIV)
						function(div(tl));
					else
						throw new UtilException("template token case not handled: " + tt.type);
				} else if (x instanceof ApplyExpr) {
					// in this case, this is an expression which should return an HTML structure or text value
					// anyway, it can be directly inserted into the DOM
					// But, it is effectively curried on the card, so lift that
					function(x);
				} else
					throw new UtilException("Non TT not handled: " + x.getClass());
			}
		}
	}

	private Object div(TemplateLine tl) {
		Object tag;
		if (tl.customTagVar != null)
			tag = ItemExpr.id(tl.customTagVar);
		else {
			if (tl.customTag != null)
				tag = ItemExpr.str(tl.customTag);
			else
				tag = ItemExpr.str("div");
		}
			
		// TODO: handle attributes (including from vars)
		// TODO: handle formats? (or just put them in the tree? because they are "common" to all classes?)
		// TODO: generate tree state
		return new ApplyExpr(ItemExpr.id("DOM.Element"), tag, ItemExpr.id("Nil"), ItemExpr.id("Nil"), ItemExpr.id("Nil"));
	}

	private void function(Object expr) {
		String name = "_templateNode_"+(++node);
		
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		List<Object> args = new ArrayList<Object>();
		cases.add(new FunctionCaseDefn(scope, name, args, expr));
		functions.put(name, new FunctionDefinition(name, 0, cases));
	}
}
