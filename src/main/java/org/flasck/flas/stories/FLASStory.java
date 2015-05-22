package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.CardDefiniton;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parser.FieldParser;
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
			} else if (o instanceof StructDefn) {
				StructDefn sd = (StructDefn)o;
				if (ret.contains(sd.typename))
					er.message(b, "duplicate definition for name " + sd.typename);
				else
					ret.define(sd.typename, sd);
				doStructFields(er, sd, b.nested);
			} else if (o instanceof ContractDecl) {
				ContractDecl cd = (ContractDecl) o;
				if (ret.contains(cd.contractName))
					er.message(b, "duplicate definition for name " + cd.contractName);
				else
					ret.define(cd.contractName, cd);
				doContractMethods(er, cd, b.nested);
			} else if (o instanceof CardDefiniton) {
				CardDefiniton cd = (CardDefiniton) o;
				if (ret.contains(cd.name))
					er.message(b, "duplicate definition for name " + cd.name);
				else
					ret.define(cd.name, cd);
				doCardDefinition(er, cd, b.nested);
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

	private void doStructFields(ErrorResult er, StructDefn sd, List<Block> fields) {
		FieldParser fp = new FieldParser();
		for (Block b : fields) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b.line.text());
			Object sf = fp.tryParsing(tkz);
			if (sf == null)
				er.message(tkz, "syntax error");
			else if (sf instanceof ErrorResult)
				er.merge((ErrorResult) sf);
			else
				sd.addField((StructField)sf);
			assertNoNonCommentNestedLines(er, b);
		}
	}

	private void doContractMethods(ErrorResult er, ContractDecl cd, List<Block> methods) {
		MethodParser mp = new MethodParser();
		for (Block b : methods) {
			if (b.isComment())
				continue;
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

	private void doCardDefinition(ErrorResult er, CardDefiniton cd, List<Block> components) {
		IntroParser ip = new IntroParser();
		for (Block b : components) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b.line.text());
			Object o = ip.tryParsing(tkz);
			if (o == null)
				er.message(tkz, "must have valid card component definition here");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (o instanceof String) {
				switch ((String)o) {
				case "state": {
					doCardState(er, cd, b.nested);
					break;
				}
				case "template": {
					doCardTemplate(er, cd, b.nested);
					break;
				}
				default: {
					throw new UtilException("Cannot handle " + o);
				}
				}
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
	}	

	private void doCardState(ErrorResult er, CardDefiniton cd, List<Block> nested) {
		// TODO Auto-generated method stub
		
	}

	private void doCardTemplate(ErrorResult er, CardDefiniton cd, List<Block> nested) {
		// TODO Auto-generated method stub
		
	}

	private void assertNoNonCommentNestedLines(ErrorResult er, Block b) {
		if (b.nested.isEmpty())
			return;
		for (Block q : b.nested)
			if (!q.isComment())
				er.message(q, "method declarations may not have inner blocks");
	}
}
