package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.parser.MethodParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FLASStory implements StoryProcessor {

	@Override
	public Object process(List<Block> blocks) {
		if (blocks.isEmpty())
			return null;

		ErrorResult er = new ErrorResult();
		Scope ret = new Scope();
		List<Object> fndefns = new ArrayList<Object>();
		for (Block b : blocks) {
			if (b.isComment())
				continue;
			
			// TODO: if it's a "package", deal with that ... and all blocks must either be or not be packages
			Object o = new MultiParser(IntroParser.class, FunctionParser.class).parse(b);
			if (o == null) {
				System.out.println("Could not parse " + b.line.text());
				er.message(new Tokenizable(b.line.text()), "syntax error");
				continue;
			}
			else if (o instanceof ErrorResult) {
				er.merge((ErrorResult)o);
				continue;
			}
			if (o instanceof FunctionCaseDefn) {
				fndefns.add(o);
				if (!b.nested.isEmpty())
					throw new UtilException("Need to handle nested function defns");
			} else if (o instanceof ContractDecl) {
				ContractDecl cd = (ContractDecl) o;
				if (ret.contains(cd.contractName))
					er.message(b, "duplicate definition for name " + cd.contractName);
				ret.define(cd.contractName, cd);
				doContractMethods(er, cd, b.nested);
			} else
				throw new UtilException("Need to handle " + o.getClass());
		}
		if (er.hasErrors())
			return er;
		
		ListMap<String, FunctionCaseDefn> groups = new ListMap<String, FunctionCaseDefn>();
		String cfn = null;
		int pnargs = 0;
		for (Object o : fndefns) {
			if (o instanceof FunctionCaseDefn) {
				// group together all function defns for a given function
				FunctionCaseDefn fcd = (FunctionCaseDefn)o;
				String n = fcd.name;
				if (cfn == null || !cfn.equals(n)) {
					cfn = n;
					pnargs = fcd.args.size();
					if (groups.contains(cfn))
						er.message((Tokenizable)null, "split definition of function " + cfn);
					else if (ret.contains(cfn))
						er.message((Tokenizable)null, "duplicate definition of " + cfn);
				} else if (fcd.args.size() != pnargs)
					er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
				groups.add(cfn, fcd);
			}
		}
		for (Entry<String, List<FunctionCaseDefn>> x : groups.entrySet()) {
			ret.define(x.getKey(), new FunctionDefinition(cfn, x.getValue().get(0).args.size(), x.getValue()));
		}
		
		if (er.hasErrors())
			return er;
		return ret;
	}

	private void doContractMethods(ErrorResult er, ContractDecl cd, List<Block> methods) {
		MethodParser mp = new MethodParser();
		for (Block b : methods) {
			Tokenizable tkz = new Tokenizable(b.line.text());
			Object md = mp.tryParsing(tkz);
			if (md == null)
				er.message(tkz, "syntax error");
			else if (md instanceof ErrorResult)
				er.merge((ErrorResult) md);
			else
				cd.addMethod((ContractMethodDecl)md);
			assertNoNonCommentNestedLines(er, b);
		}
	}

	private void assertNoNonCommentNestedLines(ErrorResult er, Block b) {
		if (b.nested.isEmpty())
			return;
		for (Block q : b.nested)
			if (!q.isComment())
				er.message(q, "method declarations may not have inner blocks");
	}
}
