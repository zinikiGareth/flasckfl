package org.flasck.flas.testrunner;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.Event;
import org.flasck.jvm.builtin.FLError;
import org.flasck.jvm.container.CardEnvironment;
import org.flasck.jvm.container.Dispatcher;
import org.flasck.jvm.container.FLEvalContextFactory;
import org.flasck.jvm.container.JvmDispatcher;
import org.flasck.jvm.container.MockAgent;
import org.flasck.jvm.container.MockCard;
import org.flasck.jvm.container.MockService;
import org.flasck.jvm.fl.ClientContext;
import org.flasck.jvm.fl.EventZone;
import org.flasck.jvm.fl.HandlerInfo;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ziniki.ziwsh.intf.EvalContext;
import org.ziniki.ziwsh.intf.EvalContextFactory;
import org.ziniki.ziwsh.intf.ZiwshBroker;
import org.ziniki.ziwsh.jvm.SimpleBroker;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner implements EvalContextFactory, FLEvalContextFactory, CardEnvironment {
	// TODO: I don't think this needs to be a special thing in the modern world
	private final ClassLoader loader;
	private Document document;
	private final JvmDispatcher dispatcher;
	private final ZiwshBroker broker = new SimpleBroker(this);
	private List<Throwable> runtimeErrors = new ArrayList<Throwable>();
	private int docId = 1;
	private final Map<String, String> templates;

	public JVMRunner(Configuration config, Repository repository, ClassLoader bcl, Map<String, String> templates) {
		super(config, repository);
		this.loader = bcl;
		this.templates = templates;
		this.dispatcher = new JvmDispatcher(this);
	}
	
	@Override
	public EvalContext newContext() {
		return create();
	}

	@Override
	public FLEvalContext create() {
		return new ClientContext(this);
	}

	@Override
	public ClassLoader getLoader() {
		return loader;
	}

	@Override
	public ZiwshBroker getBroker() {
		return broker;
	}

	@Override
	public Dispatcher getDispatcher() {
		return dispatcher;
	}

	public void clearBody(FLEvalContext cx) {
		this.document = Document.createShell("http://localhost");
	}
	
	@Override
	public String getTemplate(String template) {
		return templates.get(template);
	}

	@Override
	public String nextDocumentId() {
		return "flaselt_" + docId++;
	}

	@Override
	public String name() {
		return "jvm";
	}
	
	@Override
	public Element newdiv() {
		return document.createElement("div");
	}

	@Override
	public void attachToBody(Element newdiv) {
		document.body().appendChild(newdiv);
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
		FLEvalContext cx = new ClientContext();
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
		MockCard mc = null;
		Object reply;
		if (to instanceof MockAgent)
			reply = ((MockAgent)to).sendTo(cx, contract, meth, args);
		else if (to instanceof MockService)
			reply = ((MockService)to).sendTo(cx, contract, meth, args);
		else if (to instanceof MockCard) {
			mc = (MockCard)to;
			reply = mc.sendTo(cx, contract, meth, args);
		} else
			throw new NotImplementedException("cannot handle " + to.getClass());
		cx.handleMessages(reply);
		if (mc != null)
			mc.card()._updateDisplay(cx);
	}

	@Override
	public void event(FLEvalContext cx, Object card, Object zone, Object event) throws Exception {
		MockCard mc = (MockCard) card;
		@SuppressWarnings("unchecked")
		Element e = findDiv(cx, mc.card()._currentDiv(), (List<EventZone>) zone, 0);
		if (e == null)
			return; // nothing to do
		HandlerInfo hi = findBubbledHandler(mc.card()._eventHandlers().get(mc.card()._rootTemplate()), mc.card()._currentDiv(), e, event);
		if (hi == null)
			return; // could not find a handler
		cx.handleEvent(mc.card(), hi.handler, (Event)event);
	}

	private Element findDiv(FLEvalContext cx, Element div, List<EventZone> zone, int pos) {
		if (pos >= zone.size())
			return div;
		
		EventZone ez = zone.get(pos);
		Elements qs = div.select("[data-flas-"+ez.type + "='" + ez.name + "']");
		if (qs.size() == 0)
			return null;
		else
			return findDiv(cx, qs.first(), zone, pos+1);
	}

	private HandlerInfo findBubbledHandler(List<HandlerInfo> handlers, Element top, Element curr, Object event) {
		if (handlers == null)
			return null;
		
		String type = null, name = null;
		for (Attribute a : curr.attributes()) {
			if (a.getKey().equals("data-flas-content")) {
				type = "content";
				name = a.getValue();
				break;
			} else if (a.getKey().equals("data-flas-style")) {
				type = "style";
				name = a.getValue();
				break;
			}
		}
		if (type != null) {
			for (HandlerInfo hi : handlers) {
				if (hi.type.equals(type) && hi.slot.equals(name) && hi.event.equals(event.getClass().getSimpleName()))
					return hi;
			}
		}
		// stop if this is the root div of the card
		if (curr == top)
			return null;
		return findBubbledHandler(handlers, top, curr.parent(), event);
	}

	@Override
	public void match(FLEvalContext cx, Object target, String selector, boolean contains, String matches) throws NotMatched {
		if (target == null || !(target instanceof MockCard)) {
			throw new NotMatched(selector, "The card had no rendered content");
		}
		MockCard card = (MockCard) target;
		String content = card.getContent();
		if (content == null)
			throw new NotMatched(selector, "The card had no rendered content");
		content = content.trim().replace('\n', ' ').replaceAll(" +", " ");
		if (contains) {
			if (!content.contains(matches))
				throw new NotMatched(selector, matches, content);
		} else {
			if (!content.equals(matches))
				throw new NotMatched(selector, matches, content);
		}
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
