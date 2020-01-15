package org.flasck.flas.testrunner;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLError;
import org.flasck.jvm.container.FLEvalContextFactory;
import org.flasck.jvm.container.JvmDispatcher;
import org.flasck.jvm.fl.FLMockEvalContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ziniki.cbstore.json.FLConstructorServer;
import org.ziniki.ziwsh.model.InternalHandle;
import org.zinutils.exceptions.UtilException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner /* implements ServiceProvider */ implements FLEvalContextFactory {
//	private final EntityStore store;
//	private final JDKFlasckController controller;
	// TODO: I don't think this needs to be a special thing in the modern world
	private final ClassLoader loader;
//	private final Map<String, FlasckHandle> cards = new TreeMap<String, FlasckHandle>();
	private Document document;
	private final JvmDispatcher dispatcher;

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl) {
		super(config, repository);
		this.loader = bcl;
		this.dispatcher = new JvmDispatcher(this);
//		this.store = null;
//		this.controller = null;
	}
	
	@Override
	public FLEvalContext create() {
		return new FLConstructorServer(loader);
	}

	@Override
	public String name() {
		return "jvm";
	}

	
	@Override
	public void preparePackage(PrintWriter pw, UnitTestPackage e) {
		
	}

	@Override
	public void runit(PrintWriter pw, UnitTestCase utc) {
		try {
			FLEvalContext cxt = create();
			Class<?> tc = Class.forName(utc.name.javaName(), false, loader);
			try {
				Object result = Reflection.callStatic(tc, "dotest", this, cxt);
				if (result instanceof FLError)
					throw (Throwable)result;
				if (cxt.getError() != null)
					throw cxt.getError();
				pw.println("JVM PASS " + utc.description);
			} catch (WrappedException ex) {
				Throwable e2 = ex.unwrap();
				if (e2 instanceof AssertFailed) {
					AssertFailed af = (AssertFailed) e2;
					pw.println("JVM FAIL " + utc.description);
					pw.println("  expected: " + af.expected);
					pw.println("  actual:   " + af.actual);
				} else {
					pw.println("JVM ERROR " + utc.description);
					e2.printStackTrace(pw);
				}
			} catch (Throwable t) {
				pw.println("ERROR " + utc.description);
				t.printStackTrace(pw);
			}
		} catch (ClassNotFoundException e) {
			pw.println("NOTFOUND " + utc.description);
			config.errors.message(((InputPosition)null), "cannot find test class " + utc.name.javaName());
		}
		pw.flush();
	}

//	public void considerResource(File file) {
//		loader.addClassesFrom(file);
//	}

	// Compile this to JVM bytecodes using the regular compiler
	// - should only have access to exported things
	// - make the generated classes available to the loader
//	public void prepareScript(String scriptPkg, Scope scope) {
//		CompileResult tcr = null;
//		try {
//			try {
//				tcr = compiler.createJVM(testPkg, compiledPkg, compiledScope, scope);
//			} catch (ErrorResultException ex) {
//				((ErrorResult)ex.errors).showTo(new PrintWriter(System.err), 0);
//				fail("Errors compiling test script");
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			throw new UtilException("Failed", ex);
//		}
		// 3. Load the class(es) into memory
//		loader.add(tcr.bce);
//	}

//	@Override
//	public void prepareCase() {
//		document = Jsoup.parse("<html><head></head><body></body></html>");
//		cards.clear();
//		errors.clear();
//	}

	public void assertSameValue(Object expected, Object actual) throws FlasTestException {
		FLEvalContext cx = new FLMockEvalContext();
		expected = cx.full(expected);
		actual = cx.full(actual);
		if (!cx.compare(expected, actual))
			throw new AssertFailed(expected, actual);
	}

	@Override
	@Deprecated
	public void assertCorrectValue(int exprId) throws Exception {
//		List<Class<?>> toRun = new ArrayList<>();
//		toRun.add(Class.forName(testPkg + ".PACKAGEFUNCTIONS$expr" + exprId, false, loader));
//		toRun.add(Class.forName(testPkg + ".PACKAGEFUNCTIONS$value" + exprId, false, loader));
//
////		FLASRuntimeCache edc = new FLASTransactionContext(cxt, this.loader, this.store);
//		Map<String, Object> evals = new TreeMap<String, Object>();
//		for (Class<?> clz : toRun) {
//			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
//			try {
//				Object o = Reflection.callStatic(clz, "eval", new Object[] { cxt, new Object[] {} });
//				o = Reflection.callStatic(FLEval.class, "full", cxt, o);
//				evals.put(key, o);
//			} catch (Throwable ex) {
//				System.out.println("Error evaluating " + key);
//				ex.printStackTrace();
//				throw ex;
//			}
//		}
//		
//		Object expected = evals.get("value" + exprId);
//		Object actual = evals.get("expr" + exprId);
//		try {
//			assertEquals(expected, actual);
//		} catch (AssertionError ex) {
//			throw new AssertFailed(expected, actual);
//		}
	}

	@Override
	public void createCardAs(CardName cardType, String bindVar) {
//		if (cards.containsKey(bindVar))
//			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");

//		ScopeEntry se = compiledScope.get(cardType.cardName);
//		if (se == null)
//			throw new UtilException("There is no definition for card '" + cardType.cardName + "' in scope");
//		if (se.getValue() == null || !(se.getValue() instanceof CardDefinition))
//			throw new UtilException(cardType.cardName + " is not a card");
//		CardDefinition cd = (CardDefinition) se.getValue();
		try {
			Element body = document.select("body").get(0);
			Element div = document.createElement("div");
			body.appendChild(div);
			
//			@SuppressWarnings("unchecked")
//			Class<? extends FlasckCard> clz = (Class<? extends FlasckCard>) loader.loadClass(cardType.javaName());
//			FlasckHandle handle = controller.createCard(clz, new JSoupWrapperElement(div));
//			List<Object> services = new ArrayList<>();
//			
//			for (ContractImplements ctr : cd.contracts) {
//				String fullName = fullName(ctr.name());
//				if (!fullName.equals("org.flasck.Init") && !fullName.equals("org.flasck.Render"))
//					cacheSvc(fullName);
//			}
			
//			cdefns.put(bindVar, cd);
//			cards.put(bindVar, handle);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void invoke(FLEvalContext cx, Object expr) throws Exception {
		expr = cx.full(expr);
		dispatcher.invoke(cx, expr);
	}

	@Override
	public void send(FLEvalContext cxt, InternalHandle ih, String cardVar, String contractName, String methodName, List<Integer> args) throws Exception {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

//		String ctrName = getFullContractNameForCard(cardVar, contractName, methodName);
//		FlasckHandle card = cards.get(cardVar);
		Object[] argVals;
		if (args == null || args.isEmpty())
			argVals = new Object[0];
		else {
			argVals = new Object[args.size()];
			int cnt = 0;
			for (int i : args) {
				Class<?> clz = Class.forName(testPkg + ".PACKAGEFUNCTIONS$arg" + i, false, loader);
				Object o = Reflection.callStatic(clz, "eval", cxt, new Object[] { new Object[] {} });
				o = Reflection.call(cxt, "full", o);
				argVals[cnt++] = o;
			}
		}
//		card.send(ih, ctrName, methodName, argVals);
//		controller.processPostboxes();
		assertAllInvocationsCalled();
	}

	@Override
	public void event(String cardVar, String methodName) throws Exception {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");
//		FlasckHandle card = cards.get(cardVar);
//		card.event(controller.createContext(), cardVar, methodName);
	}

	@Override
	public void match(HTMLMatcher matcher, String selector) throws NotMatched {
//		matcher.match(selector, document.select(selector).stream().map(e -> new JSoupWrapperElement(e)).collect(Collectors.toList()));
	}

	@Override
	public void click(String selector) {
		Elements elts = document.select(selector);
		if (elts.size() == 0)
			throw new UtilException("No elements matched " + selector);
		else if (elts.size() > 1)
			throw new UtilException("Multiple elements matched " + selector);
		Element e = elts.first();
		if (!e.hasAttr("onclick"))
			throw new UtilException("There is no 'onclick' attribute on " + e.outerHtml());
//		String[] ca = e.attr("onclick").split(":");
//		FlasckCard card = this.controller.getCard(ca[0]);
//		EventHandler handler = card.getAction(ca[1], "click");
//		// TODO: we really should create an event object here ...
//		Object ev = null;
//		this.controller.handleEventOn(card, handler, ev);
//		assertAllInvocationsCalled();
	}
	
	// can we get rid of this and just use the other one directly?
//	public <T> T mockContract(Class<T> ctr) {
//		return cxt.mockContract(ctr);
//	}
}
