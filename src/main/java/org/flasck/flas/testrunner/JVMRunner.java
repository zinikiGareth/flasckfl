package org.flasck.flas.testrunner;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLError;
import org.flasck.jvm.container.ErrorCollector;
import org.flasck.jvm.container.FLEvalContextFactory;
import org.flasck.jvm.container.JvmDispatcher;
import org.flasck.jvm.container.MockAgent;
import org.flasck.jvm.container.MockCard;
import org.flasck.jvm.container.MockService;
import org.flasck.jvm.fl.LoaderContext;
import org.ziniki.ziwsh.intf.EvalContext;
import org.ziniki.ziwsh.intf.EvalContextFactory;
import org.ziniki.ziwsh.intf.ZiwshBroker;
import org.ziniki.ziwsh.jvm.SimpleBroker;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner /* implements ServiceProvider */ implements EvalContextFactory, FLEvalContextFactory, ErrorCollector {
//	private final EntityStore store;
//	private final JDKFlasckController controller;
	// TODO: I don't think this needs to be a special thing in the modern world
	private final ClassLoader loader;
//	private final Map<String, FlasckHandle> cards = new TreeMap<String, FlasckHandle>();
//	private Document document;
	private final JvmDispatcher dispatcher;
	private final ZiwshBroker broker = new SimpleBroker(this);
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl) {
		super(config, repository);
		this.loader = bcl;
		this.dispatcher = new JvmDispatcher(this);
//		this.store = null;
//		this.controller = null;
	}
	
	@Override
	public EvalContext newContext() {
		return create();
	}

	@Override
	public FLEvalContext create() {
		return new LoaderContext(loader, broker, this, dispatcher);
	}

	@Override
	public String name() {
		return "jvm";
	}

	@Override
	public void error(Throwable error) {
		runtimeErrors.add(error);
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
				Throwable e2 = WrappedException.unwrapThrowable(ex);
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

	public void assertSameValue(Object expected, Object actual) throws FlasTestException {
		FLEvalContext cx = new LoaderContext();
		expected = cx.full(expected);
		actual = cx.full(actual);
		if (!cx.compare(expected, actual))
			throw new AssertFailed(expected, actual);
	}

//	@Override
//	public void createCardAs(CardName cardType, String bindVar) {
////		if (cards.containsKey(bindVar))
////			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");
//
////		ScopeEntry se = compiledScope.get(cardType.cardName);
////		if (se == null)
////			throw new UtilException("There is no definition for card '" + cardType.cardName + "' in scope");
////		if (se.getValue() == null || !(se.getValue() instanceof CardDefinition))
////			throw new UtilException(cardType.cardName + " is not a card");
////		CardDefinition cd = (CardDefinition) se.getValue();
//		try {
//			Element body = document.select("body").get(0);
//			Element div = document.createElement("div");
//			body.appendChild(div);
//			
////			@SuppressWarnings("unchecked")
////			Class<? extends FlasckCard> clz = (Class<? extends FlasckCard>) loader.loadClass(cardType.javaName());
////			FlasckHandle handle = controller.createCard(clz, new JSoupWrapperElement(div));
////			List<Object> services = new ArrayList<>();
////			
////			for (ContractImplements ctr : cd.contracts) {
////				String fullName = fullName(ctr.name());
////				if (!fullName.equals("org.flasck.Init") && !fullName.equals("org.flasck.Render"))
////					cacheSvc(fullName);
////			}
//			
////			cdefns.put(bindVar, cd);
////			cards.put(bindVar, handle);
//		} catch (Exception ex) {
//			throw UtilException.wrap(ex);
//		}
//	}

	@Override
	public void invoke(FLEvalContext cx, Object expr) throws Exception {
		expr = cx.full(expr);
		dispatcher.invoke(cx, expr);
	}

	@Override
	public void send(FLEvalContext cx, Object to, String contract, String meth, Object... args) {
		Object reply;
		if (to instanceof MockAgent)
			reply = ((MockAgent)to).sendTo(cx, contract, meth, args);
		else if (to instanceof MockService)
			reply = ((MockService)to).sendTo(cx, contract, meth, args);
		else if (to instanceof MockCard)
			reply = ((MockCard)to).sendTo(cx, contract, meth, args);
		else
			throw new NotImplementedException("cannot handle " + to.getClass());
		reply = cx.full(reply);
		dispatcher.invoke(cx, reply);
	}

	@Override
	public void event(FLEvalContext cx, Object card, Object event) throws Exception {
		MockCard mc = (MockCard) card;
		Object reply = cx.handleEvent(mc.card(), event);
		reply = cx.full(reply);
		dispatcher.invoke(cx, reply);
	}

//	@Override
//	public void match(HTMLMatcher matcher, String selector) throws NotMatched {
////		matcher.match(selector, document.select(selector).stream().map(e -> new JSoupWrapperElement(e)).collect(Collectors.toList()));
//	}
//
//	@Override
//	public void click(String selector) {
//		Elements elts = document.select(selector);
//		if (elts.size() == 0)
//			throw new UtilException("No elements matched " + selector);
//		else if (elts.size() > 1)
//			throw new UtilException("Multiple elements matched " + selector);
//		Element e = elts.first();
//		if (!e.hasAttr("onclick"))
//			throw new UtilException("There is no 'onclick' attribute on " + e.outerHtml());
////		String[] ca = e.attr("onclick").split(":");
////		FlasckCard card = this.controller.getCard(ca[0]);
////		EventHandler handler = card.getAction(ca[1], "click");
////		// TODO: we really should create an event object here ...
////		Object ev = null;
////		this.controller.handleEventOn(card, handler, ev);
////		assertAllInvocationsCalled();
//	}
	
	public void testComplete() throws Throwable {
		if (!runtimeErrors.isEmpty())
			throw runtimeErrors .get(0);
	}
	// can we get rid of this and just use the other one directly?
//	public <T> T mockContract(Class<T> ctr) {
//		return cxt.mockContract(ctr);
//	}
}
