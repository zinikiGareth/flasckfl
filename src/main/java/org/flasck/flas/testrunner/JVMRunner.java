package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.flasck.android.FlasckActivity;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.jdk.post.JDKPostbox;
import org.flasck.jvm.Despatcher;
import org.flasck.jvm.FlasckService;
import org.flasck.jvm.post.DeliveryAddress;
import org.flasck.jvm.post.SendOver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner {
	public class InitService extends FlasckService {
		public void ready(DeliveryAddress from, Map<String, DeliveryAddress> contracts) {
			DeliveryAddress myaddr = postbox.addressOf(initSvc);
			Map<String, DeliveryAddress> servicesForCard = new TreeMap<String, DeliveryAddress>();
			for (Entry<String, DeliveryAddress> s : contracts.entrySet()) {
				if (s.getKey().equals("org.ziniki.Init"))
					continue;
				// I suspect I want to stash the value somewhere so that I know how to contact this card
				// But which card?  Do I need that from the "from" address
				servicesForCard.put(s.getKey(), postbox.addressOf(cacheSvc(s.getKey()).chan));
			}
			// need to find da's for all the contracts named in contracts and wire things up
			postbox.send(myaddr, from, "services", servicesForCard);
			postbox.send(myaddr, from, "state");
		}
	}

	private final BCEClassLoader loader;
	private final Document document;
	private final Map<String, FlasckActivity> cards = new TreeMap<String, FlasckActivity>();
	private final JDKPostbox postbox = new JDKPostbox("container", null, -1);
	private final Despatcher despatcher;
	private final Map<String, CachedService> services = new TreeMap<>();
	private final int initSvc;

	public JVMRunner(CompileResult prior) {
		super(prior);
		loader = new BCEClassLoader(prior.bce);
		document = Jsoup.parse("<html><head></head><body></body></html>");
		despatcher = new Despatcher(false);
		despatcher.bindPostbox(postbox);
		postbox.bindDespatcher(null, despatcher);
		initSvc = cacheSvc("org.ziniki.Init", new InitService()).chan;
	}

	public void considerResource(File file) {
		loader.addClassesFrom(file);
	}

	// Compile this to JVM bytecodes using the regular compiler
	// - should only have access to exported things
	// - make the generated classes available to the loader
	public void prepareScript(ScriptCompiler compiler, Scope scope) {
		CompileResult tcr = null;
		try {
			try {
				tcr = compiler.createJVM(spkg, prior, scope);
				spkg = tcr.getPackage().uniqueName();
			} catch (ErrorResultException ex) {
				ex.errors.showTo(new PrintWriter(System.err), 0);
				fail("Errors compiling test script");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new UtilException("Failed", ex);
		}
		// 3. Load the class(es) into memory
		loader.add(tcr.bce);
	}

	@Override
	public void assertCorrectValue(int exprId) throws Exception {
		List<Class<?>> toRun = new ArrayList<>();
		toRun.add(Class.forName(spkg + ".PACKAGEFUNCTIONS$expr" + exprId, false, loader));
		toRun.add(Class.forName(spkg + ".PACKAGEFUNCTIONS$value" + exprId, false, loader));

		Class<?> fleval = Class.forName("org.flasck.jvm.FLEval", false, loader);
		Map<String, Object> evals = new TreeMap<String, Object>();
		for (Class<?> clz : toRun) {
			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
			Object o = Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
			o = Reflection.callStatic(fleval, "full", o);
			evals.put(key, o);
		}
		
		Object expected = evals.get("value" + exprId);
		Object actual = evals.get("expr" + exprId);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

	@Override
	public void createCardAs(CardName cardType, String bindVar) {
		if (cards.containsKey(bindVar))
			throw new UtilException("Duplicate card assignment to '" + bindVar + "'");

		ScopeEntry se = prior.getScope().get(cardType.cardName);
		if (se == null)
			throw new UtilException("There is no definition for card '" + cardType.cardName + "' in scope");
		if (se.getValue() == null || !(se.getValue() instanceof CardDefinition))
			throw new UtilException(cardType.cardName + " is not a card");
		CardDefinition cd = (CardDefinition) se.getValue();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends FlasckActivity> clz = (Class<? extends FlasckActivity>) loader.loadClass(cardType.javaName());
			List<Object> services = new ArrayList<>();
			
			for (ContractImplements ctr : cd.contracts) {
				String fullName = fullName(ctr.name());
				if (!fullName.equals("org.ziniki.Init"))
					cacheSvc(fullName);
			}
			
			Element body = document.select("body").get(0);
			Element div = document.createElement("div");
			body.appendChild(div);
			
			FlasckActivity card = Reflection.create(clz);
			card.init(postbox, initSvc, despatcher.nextChan(), div, clz);
			processQueues();
			cdefns.put(bindVar, cd);
			cards.put(bindVar, card);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	protected CachedService cacheSvc(String name) {
		if (services.containsKey(name))
			return services.get(name);
		MockService service = new MockService(name);
		return cacheSvc(name, service);
	}

	protected CachedService cacheSvc(String name, FlasckService service) {
		int chan = despatcher.registerService(name, service);
		CachedService ret = new CachedService(chan, service);
		services.put(name, ret);
		return ret;
	}

	@Override
	public void send(String cardVar, String contractName, String methodName) {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

		String fullName = getFullContractNameForCard(cardVar, contractName, methodName);
		FlasckActivity card = cards.get(cardVar);
//		card.call("send", fullName, methodName); // TODO: should also allow args
	}

	@Override
	public void match(WhatToMatch what, String selector, String contents) throws NotMatched {
		System.out.println(document.outerHtml());
		what.match(selector, contents, document.select(selector).stream().map(e -> new JVMWrapperElement(e)).collect(Collectors.toList()));
	}

	// Because everything is single-threaded in this environment,
	// we can just keep calling the execute methods on all the outstanding
	// items in all the queues in all the postboxes, until we're done
	private void processQueues() {
		boolean done = false;
		while (!done) {
			done = true;
			done &= !dispatch(postbox);
			for (JDKPostbox pb : postbox.connectedTo.values())
				done &= !dispatch(pb);
		}
	}

	private boolean dispatch(JDKPostbox pb) {
		SendOver next = pb.next(1);
		if (next == null)
			return false;
		
		Object ret = pb.despatch(next);
		if (ret != null)
			throw new UtilException("We need to do something with this");
		return true;
	}
}
