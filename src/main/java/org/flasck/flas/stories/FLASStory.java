package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.SingleLineFunctionCase;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FLASStory implements StoryProcessor {

	@Override
	public Object process(List<Block> blocks) {
		if (blocks.isEmpty())
			return null;

		Scope ret = new Scope();
		List<Object> fndefns = new ArrayList<Object>();
		for (Block b : blocks) {
			// if it's a "package", deal with that ... and all blocks must either be or not be packages
			Object o = new SingleLineFunctionCase().tryParsing(new Tokenizable(b.line.text()));
			if (o != null)
				fndefns.add(o);
			else
				; // handle error
			if (!b.nested.isEmpty())
				throw new UtilException("Need to handle nested defns");
		}
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
						return null; // duplicate name
					else if (ret.contains(cfn))
						return null; // duplicate name
				} else if (fcd.args.size() != pnargs)
					return null; // different numbers of args
				groups.add(cfn, fcd);
			}
		}
		for (Entry<String, List<FunctionCaseDefn>> x : groups.entrySet()) {
			FunctionDefinition hsie = new FunctionDefinition(cfn, x.getValue().get(0).args.size(), x.getValue());
			if (hsie != null)
				ret.define(x.getKey(), hsie);
			// else handle errors
		}
		return ret;
	}
	
}
