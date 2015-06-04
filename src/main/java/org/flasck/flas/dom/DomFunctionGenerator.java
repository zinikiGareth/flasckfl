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
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
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
		FunctionDefinition f = generateOne(template);
		functions.put(f.name, f);
		for (TemplateLine tl : template.nested)
			generate(tl);
	}
	
	// TODO: this shouldn't take both a "name" and a "line"
	// I think there should be a name-generator in this class
	// We should then iterate over the "contents" array (if not a div/list)
	public FunctionDefinition generateOne(TemplateLine tl) {
		String name = nextName();
		// This is standard for all cases
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		List<Object> args = new ArrayList<Object>();
		args.add(new VarPattern("card"));

		// This is the case for a simple (variable) content item
		// TODO: it goes with a "type: content" entry in the tree
		for (Object x : tl.contents) {
			if (x instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken)x;
				if (tt.type == TemplateToken.IDENTIFIER)
					cases.add(new FunctionCaseDefn(scope, name, args,
						new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.PUNC, ".")),
						new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "card")),
						new ItemExpr(new ExprToken(ExprToken.STRING, tt.text)))));
				else if (tt.type == TemplateToken.STRING)
					cases.add(new FunctionCaseDefn(scope, name, args,
						new ItemExpr(new ExprToken(ExprToken.STRING, tt.text))));
				else
					throw new UtilException("template token case not handled: " + tt.type);
			} else
				throw new UtilException("Non TT not handled");
		}
		
		// This is standard
		return new FunctionDefinition(name, 1, cases);
	}

	private String nextName() {
		return "_templateNode_"+(++node);
	}
}
