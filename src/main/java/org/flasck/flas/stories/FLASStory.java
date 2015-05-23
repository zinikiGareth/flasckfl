package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parser.FieldParser;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.parser.MethodMessageParser;
import org.flasck.flas.parser.MethodParser;
import org.flasck.flas.parser.TemplateLineParser;
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
				er.message(new Tokenizable(b), "syntax error");
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
			} else if (o instanceof CardDefinition) {
				CardDefinition cd = (CardDefinition) o;
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
				String n = fcd.intro.name;
				if (cfn == null || !cfn.equals(n)) {
					cfn = n;
					pnargs = fcd.intro.args.size();
					if (groups.contains(cfn))
						er.message((Tokenizable)null, "split definition of function " + cfn);
					else if (ret.contains(cfn))
						er.message((Tokenizable)null, "duplicate definition of " + cfn);
				} else if (fcd.intro.args.size() != pnargs)
					er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
				groups.add(cfn, fcd);
			}
		}
		for (Entry<String, List<FunctionCaseDefn>> x : groups.entrySet()) {
			ret.define(x.getKey(), new FunctionDefinition(cfn, x.getValue().get(0).intro.args.size(), x.getValue()));
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
			Tokenizable tkz = new Tokenizable(b);
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
			Tokenizable tkz = new Tokenizable(b);
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

	private void doCardDefinition(ErrorResult er, CardDefinition cd, List<Block> components) {
		IntroParser ip = new IntroParser();
		for (Block b : components) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
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
					List<TemplateLine> items = doCardTemplate(er, b.nested);
					if (cd.template != null)
						er.message((Block)null, "duplicate template definition in card");
					cd.template = items;
					break;
				}
				default: {
					throw new UtilException("Cannot handle " + o);
				}
				}
			} else if (o instanceof ContractImplements) {
				cd.addContractImplementation((ContractImplements)o);
				doImplementation(er, (Implements)o, b.nested);
			} else if (o instanceof HandlerImplements) {
				cd.addHandlerImplementation((HandlerImplements)o);
				doImplementation(er, (Implements)o, b.nested);
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
	}	

	private void doCardState(ErrorResult er, CardDefinition cd, List<Block> nested) {
		if (cd.state != null)
			er.message((Block)null, "duplicate state definition in card");
		cd.state = new StateDefinition();
		FieldParser fp = new FieldParser();
		for (Block q : nested)
			if (!q.isComment()) {
				Object o = fp.tryParsing(new Tokenizable(q));
				if (o == null)
					er.message(q, "syntax error");
				else if (o instanceof ErrorResult)
					er.merge((ErrorResult) o);
				else if (o instanceof StructField)
					cd.state.addField((StructField)o);
				else
					er.message(q, "cannot handle " + o.getClass());
			}
				
	}

	private List<TemplateLine> doCardTemplate(ErrorResult er, List<Block> nested) {
		TemplateLineParser tlp = new TemplateLineParser();
		List<TemplateLine> ret = new ArrayList<TemplateLine>();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof TemplateLine)
				ret.add((TemplateLine)o);
			else
				er.message(b, "invalid type");
		}
		return ret;
	}

	private void doImplementation(ErrorResult er, Implements impl, List<Block> nested) {
		FunctionParser fp = new FunctionParser();
		List<MethodCaseDefn> cases = new ArrayList<MethodCaseDefn>();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = fp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof FunctionIntro) {
				FunctionIntro meth = (FunctionIntro)o;
				MethodCaseDefn mcd = new MethodCaseDefn(meth);
				cases.add(mcd);
				assertSomeNonCommentNestedLines(er, b);
				handleMessageMethods(er, mcd, b.nested);
			} else
				er.message(b, "cannot handle " + o.getClass());
		}

		ListMap<String, MethodCaseDefn> groups = new ListMap<String, MethodCaseDefn>();
		String cfn = null;
		int pnargs = 0;
		for (MethodCaseDefn fcd : cases) {
			String n = fcd.intro.name;
			if (cfn == null || !cfn.equals(n)) {
				cfn = n;
				pnargs = fcd.intro.args.size();
				if (groups.contains(cfn))
					er.message((Tokenizable)null, "split definition of function " + cfn);
			} else if (fcd.intro.args.size() != pnargs)
				er.message((Tokenizable)null, "inconsistent numbers of arguments in definitions of " + cfn);
			groups.add(cfn, fcd);
		}
		for (Entry<String, List<MethodCaseDefn>> x : groups.entrySet()) {
			impl.addMethod(new MethodDefinition(x.getValue().get(0).intro, x.getValue()));
		}

		// TODO: somewhere (possibly not here) they need to be rewritten to functions and thus to HSIE
		// Oh, and we probably want to do that in a phase that converts templates to functions too
	}

	private void handleMessageMethods(ErrorResult er, MethodCaseDefn mcd, List<Block> nested) {
		MethodMessageParser mm = new MethodMessageParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = mm.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (o instanceof MethodMessage)
				mcd.addMessage((MethodMessage) o);
			else
				throw new UtilException("What is " + o + "?");
		}
	}

	private void assertNoNonCommentNestedLines(ErrorResult er, Block b) {
		for (Block q : b.nested)
			if (!q.isComment())
				er.message(q, "nested declarations prohibited");
	}

	private void assertSomeNonCommentNestedLines(ErrorResult er, Block b) {
		for (Block q : b.nested)
			if (!q.isComment())
				return;
		er.message(b, "nested declarations required");
	}
}
