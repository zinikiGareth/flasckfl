package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.EventHandler;
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
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.FieldParser;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.parser.MethodMessageParser;
import org.flasck.flas.parser.MethodParser;
import org.flasck.flas.parser.TemplateLineParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class FLASStory implements StoryProcessor {

	public class State {
		private String pkg;
		public final Scope scope;

		public State(Scope scope, String pkg) {
			this.scope = scope;
			this.pkg = pkg;
		}

		public String withPkg(String name) {
			if (pkg == null || pkg.length() == 0)
				return name;
			return pkg +"." + name;
		}
	}

	@Override
	public Object process(String pkg, List<Block> blocks) {
		State s = new State(new Scope(builtinScope()), pkg);
		ErrorResult er = new ErrorResult();
		return doScope(er, s, blocks);
	}
	
	private Object doScope(ErrorResult er, State s, List<Block> blocks) {
		if (blocks.isEmpty())
			return null;

		Scope ret = s.scope;
		List<Object> fndefns = new ArrayList<Object>();
		for (Block b : blocks) {
			if (b.isComment())
				continue;
			
			// TODO: if it's a "package", deal with that ... and all blocks must either be or not be packages
			Object o = new MultiParser(ret, IntroParser.class, FunctionParser.class).parse(b);
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
				if (!b.nested.isEmpty()) {
					doScope(er, new State(((ContainsScope)o).innerScope(), s.pkg +"." + ((FunctionCaseDefn)o).intro.name), b.nested);
				}
			} else if (o instanceof StructDefn) {
				StructDefn sd = (StructDefn)o;
				if (ret.contains(sd.typename))
					er.message(b, "duplicate definition for name " + sd.typename);
				else
					ret.define(sd.typename, s.withPkg(sd.typename), sd);
				doStructFields(er, sd, b.nested);
			} else if (o instanceof ContractDecl) {
				ContractDecl cd = (ContractDecl) o;
				if (ret.contains(cd.contractName))
					er.message(b, "duplicate definition for name " + cd.contractName);
				else
					ret.define(cd.contractName, s.withPkg(cd.contractName), cd);
				doContractMethods(er, cd, b.nested);
			} else if (o instanceof CardDefinition) {
				CardDefinition cd = (CardDefinition) o;
				if (ret.contains(cd.name))
					er.message(b, "duplicate definition for name " + cd.name);
				else
					ret.define(cd.name, s.withPkg(cd.name), cd);
				doCardDefinition(er, s, cd, b.nested);
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
					er.message((Block)null, "inconsistent numbers of arguments in definitions of " + cfn);
				groups.add(cfn, fcd);
			}
		}
		for (Entry<String, List<FunctionCaseDefn>> x : groups.entrySet()) {
			ret.define(x.getKey(), s.withPkg(x.getKey()), new FunctionDefinition(x.getValue().get(0).intro, x.getValue()));
		}
		
		if (er.hasErrors())
			return er;
		return ret;
	}

	public static Scope builtinScope() {
		Scope ret = new Scope(null);
		{ // core
			ret.define(".", "FLEval.field", 
				Type.function(Type.polyvar("A"), Type.simple("String"), Type.polyvar("B")));
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
			ret.define("List", "List", null);
			ret.define("Nil", "Nil",
				new StructDefn("Nil"));
			ret.define("Cons", "Cons",
				new StructDefn("Cons")
				.add("A")
				.addField(new StructField(new TypeReference(null, "A"), "head"))
				.addField(new StructField(new TypeReference("List").with(new TypeReference(null, "A")), "tail")));
		}
		{ // messaging
			ret.define("Message", "Message", null);
			ret.define("Assign", "Assign",
				new StructDefn("Assign")
				.add("A")
				.addField(new StructField(new TypeReference("String"), "slot"))
				.addField(new StructField(new TypeReference(null, "A"), "value")));
			ret.define("Send", "Send",
				new StructDefn("Send")
				.addField(new StructField(new TypeReference("Any"), "dest"))
				.addField(new StructField(new TypeReference("String"), "method"))
				.addField(new StructField(new TypeReference("List").with(new TypeReference("Any")), "args")));
			ret.define("JSNI", "JSNI", null);
		}
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

	private void doCardDefinition(ErrorResult er, State s, CardDefinition cd, List<Block> components) {
		IntroParser ip = new IntroParser(s.scope);
		List<FunctionCaseDefn> functions = new ArrayList<FunctionCaseDefn>();
		for (Block b : components) {
			if (b.isComment())
				continue;
			Tokenizable tkz = new Tokenizable(b);
			Object o = ip.tryParsing(tkz);
			if (o == null) {
				o = new FunctionParser(s.scope).tryParsing(new Tokenizable(b));
				if (o  == null) {
					er.message(tkz, "must have valid card component definition here");
					continue;
				}
			}
			if (o instanceof ErrorResult)
				er.merge((ErrorResult)o);
			else if (o instanceof String) {
				switch ((String)o) {
				case "state": {
					doCardState(er, s, cd, b.nested);
					break;
				}
				case "template": {
					if (cd.template != null)
						er.message(b, "duplicate template definition in card");
					else {
						List<TemplateLine> items = new ArrayList<TemplateLine>();
						doCardTemplate(er, b.nested, items, null);
						if (items.size() != 1)
							er.message(b, "top level template must be a div or list");
						else
							cd.template = items.get(0);
					}
					break;
				}
				default: {
					throw new UtilException("Cannot handle " + o);
				}
				}
			} else if (o instanceof ContractImplements) {
				cd.addContractImplementation((ContractImplements)o);
				doImplementation(s, er, (Implements)o, b.nested);
			} else if (o instanceof HandlerImplements) {
				cd.addHandlerImplementation((HandlerImplements)o);
				doImplementation(s, er, (Implements)o, b.nested);
			} else if (o instanceof FunctionCaseDefn) {
				functions.add((FunctionCaseDefn) o);
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
		System.out.println("Need to handle functions: " + functions);
	}	

	private void doCardState(ErrorResult er, State s, CardDefinition cd, List<Block> nested) {
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

	private List<TemplateLine> doCardTemplate(ErrorResult er, List<Block> nested, List<TemplateLine> ret, List<EventHandler> handlers) {
		TemplateLineParser tlp = new TemplateLineParser();
		for (Block b : nested) {
			if (b.isComment())
				continue;
			Object o = tlp.tryParsing(new Tokenizable(b));
			if (o == null)
				er.message(b, "syntax error");
			else if (o instanceof ErrorResult)
				er.merge((ErrorResult) o);
			else if (o instanceof TemplateLine) {
				TemplateLine tl = (TemplateLine)o;
				ret.add(tl);
				if (!b.nested.isEmpty()) {
					if (tl.isDiv()) { 
						doCardTemplate(er, b.nested, tl.nested, tl.handlers);
					} else if (tl.isList()) {
						doCardTemplate(er, b.nested, tl.nested, tl.handlers);
					} else {
						boolean nc = false;
						for (Block b1 : b.nested)
							nc |= !b1.isComment();
						if (nc)
							er.message(b, "this node cannot have nested content");
					}
				}
			} else if (o instanceof EventHandler)
				handlers.add((EventHandler)o);
			else
				er.message(b, "not a valid template line");
		}
		return ret;
	}

	private void doImplementation(State s, ErrorResult er, Implements impl, List<Block> nested) {
		FunctionParser fp = new FunctionParser(s.scope);
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
