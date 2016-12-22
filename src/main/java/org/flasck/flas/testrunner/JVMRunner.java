package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.jdk.FlasckHandle;
import org.flasck.jdk.JDKFlasckController;
import org.flasck.jdk.ServiceProvider;
import org.flasck.jvm.cards.FlasckCard;
import org.flasck.jvm.container.FlasckService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

public class JVMRunner extends CommonTestRunner implements ServiceProvider {
	private final BCEClassLoader loader;
	private final Document document;
	private final Map<String, FlasckHandle> cards = new TreeMap<String, FlasckHandle>();
	private final JDKFlasckController controller;

	public JVMRunner(CompileResult prior) {
		super(prior);
		loader = new BCEClassLoader(prior.bce);
		document = Jsoup.parse("<html><head></head><body></body></html>");
		controller = new JDKFlasckController(this);
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
	public void prepareCase() {
		
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
			Element body = document.select("body").get(0);
			Element div = document.createElement("div");
			body.appendChild(div);
			
			@SuppressWarnings("unchecked")
			Class<? extends FlasckCard> clz = (Class<? extends FlasckCard>) loader.loadClass(cardType.javaName());
			FlasckHandle handle = controller.createCard(clz, div);
//			List<Object> services = new ArrayList<>();
//			
//			for (ContractImplements ctr : cd.contracts) {
//				String fullName = fullName(ctr.name());
//				if (!fullName.equals("org.ziniki.Init"))
//					cacheSvc(fullName);
//			}
			
			cdefns.put(bindVar, cd);
			cards.put(bindVar, handle);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void send(String cardVar, String contractName, String methodName) {
		if (!cdefns.containsKey(cardVar))
			throw new UtilException("there is no card '" + cardVar + "'");

		String ctrName = getFullContractNameForCard(cardVar, contractName, methodName);
		FlasckHandle card = cards.get(cardVar);
		card.send(ctrName, methodName); // TODO: should also allow args
		controller.processPostboxes();
	}

	@Override
	public void match(WhatToMatch what, String selector, String contents) throws NotMatched {
		System.out.println(document.outerHtml());
		what.match(selector, contents, document.select(selector).stream().map(e -> new JVMWrapperElement(e)).collect(Collectors.toList()));
	}

	@Override
	public FlasckService getService(String name) {
		return new MockService(name);
	}
}
