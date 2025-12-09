package org.flasck.flas.testrunner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.jvm.fl.JVMTestPlugin;
import org.flasck.jvm.fl.TestModuleLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.server.tda.WSReceiver;
import org.ziniki.ziwsh.intf.WSResponder;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;

public class BrowserJSJavaBridge implements JSJavaBridge, WSReceiver, TestModuleLoader {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected static Logger debugLogger = LoggerFactory.getLogger("DebugLog");
	static String patienceChild = System.getProperty("org.flasck.patience.child");
	boolean wantTimeout = patienceChild == null || !patienceChild.equals("true");
	private final JSTestController controller;
	private final ClassLoader classloader;
	private final File root;
	private final LockingCounter counter;
	protected final List<String> errors = new ArrayList<>();
	private final Map<String, Object> modules = new HashMap<>();
	private Map<String, Object> conns = new TreeMap<>();
	private int next = 1;
	private WSResponder responder;
	private boolean readyWhenZero = false;
	private CountDownLatch shutdownCounter = new CountDownLatch(1);
	private Iterable<JVMTestPlugin> plugins;

	public BrowserJSJavaBridge(JSTestController controller, ClassLoader classloader, File root, LockingCounter counter) {
		this.controller = controller;
		this.classloader = classloader;
		this.root = root;
		this.counter = counter;
		plugins = ServiceLoader.load(JVMTestPlugin.class);
		for (JVMTestPlugin p : plugins) {
			p.ready(this, classloader);
		}
		if (classloader instanceof BCEClassLoader) {
			for (JVMTestPlugin p : ((BCEClassLoader)classloader).services(JVMTestPlugin.class)) {
				p.ready(this, classloader);
			}
		}
	}

	@Override
	public void open(WSResponder responder) {
		this.responder = responder;
		logger.info("opened bridge " + this + " with " + responder);
	}

	@Override
	public void onText(WSResponder responder, String text) {
		logger.info("received " + text);
		try {
			JSONObject jo = new JSONObject(text);
			String action = jo.getString("action");
			if (jo.has("conn")) {
				String c = jo.getString("conn");
				int reqId = jo.getInt("requestId");
				Object module = conns.get(c);
				JSONArray ja = jo.getJSONArray("args");
				Object[] args = new Object[ja.length()];
				for (int i=0;i<ja.length();i++)
					args[i] = ja.get(i);
				Object resp = Reflection.call(module, action, args);
				if (resp == null || !(resp instanceof JSONObject))
					resp = new JSONObject();
				((JSONObject) resp).put("action", "response").put("respondingTo", reqId);
				sendJson(resp.toString());
				return;
			}
			switch (action) {
			case "ready": {
				if (counter.isZero()) {
					controller.ready();
				} else {
					readyWhenZero  = true;
				}
				break;
			}
			case "steps": {
				JSONArray arr = jo.getJSONArray("steps");
				List<String> steps = new ArrayList<>();
				for (int i=0;i<arr.length();i++) {
					steps.add(arr.getString(i));
				}
				controller.stepsForTest(steps);
				break;
			}
			case "systemTestPrepared": {
				controller.systemTestPrepared();
				break;
			}
			case "error": {
				controller.error(jo.getString("error"));
				break;
			}
			case "log": 
			case "debugmsg": {
				logger.warn(jo.getString("message"));
				break;
			}
			case "lock": {
				int reqId = jo.getInt("reqId");
				lock(reqId, jo.getString("msg"));
				break;
			}
			case "unlock": {
				int reqId = jo.getInt("reqId");
				unlock(reqId, jo.getString("msg"));
				break;
			}
			case "module": {
				String name = jo.getString("name");
				Object ret = module(name);
				String conn = "conn" + (next++);
				conns.put(conn, ret);
				sendJson(new JSONObject().put("action", "haveModule").put("name", name).put("clz", "ZinTestModule").put("conn", conn).toString());
				break;
			}
			default:
				throw new InvalidUsageException("there is no action '" + action + "'");
			}
		} catch (Throwable ex) {
			logger.error("error processing " + text, ex);
		}
	}

	public void prepareUnitTest(NameOfThing pkg, String test) throws JSONException {
		sendJson(new JSONObject().put("action", "prepareUnitTest").put("wrapper", pkg.uniqueName()).put("testname", test).toString());
	}

	public void prepareSystemTest(NameOfThing pkg) throws JSONException {
		sendJson(new JSONObject().put("action", "prepareSystemTest").put("testclz", pkg.uniqueName()).toString());
	}

	public void prepareStage(String baseName) throws JSONException {
		sendJson(new JSONObject().put("action", "prepareStage").put("stage", baseName).toString());
	}

	public void runStep(int reqId, String step) throws JSONException {
		sendJson(new JSONObject().put("action", "runStep").put("reqId", reqId).put("step", step).toString());
	}

	public void checkContextSatisfied(int reqId) throws JSONException {
		sendJson(new JSONObject().put("action", "assertSatisfied").put("reqId", reqId).toString());
	}

	@Override
	public void error() {
		logger.error("error on chrome ws");
	}

	@Override
	public void error(String s) {
		errors.add(s);
	}
	
	@Override
	public void log(String s) {
		logger.info(s);
	}
	
	@Override
	public void debugmsg(String s) {
		debugLogger.info(s);
	}
	
	
	@Override
//	@SuppressWarnings("unchecked")
	public void register(String ctr, String impl) {
		// I think the relevant things need to happen in JS here ...
		/*
		try {
			@SuppressWarnings("rawtypes")
			Class cc = Class.forName(ctr, false, classloader);
			@SuppressWarnings("rawtypes")
			Class ci = Class.forName(impl, false, classloader);
			Object svc = ci.getDeclaredConstructor().newInstance();
			if (svc instanceof InterestedInBroker)
				((InterestedInBroker)svc).broker(this.broker);
			controller.broker.register(cc, svc);
		} catch (ClassNotFoundException ex) {
			// these things happen in development ...
			logger.info("could not find " + ex.getMessage());
		} catch (Exception ex) {
			throw WrappedException.wrap(ex);
		}
		*/
	}

	@Override
	public void configureModule(String name, Class<?> clz) {
		try {
			this.modules.put(name, Reflection.callStatic(clz, "createChrome", this, classloader, root));
		} catch (IllegalArgumentException | SecurityException e) {
			throw WrappedException.wrap(e);
		}
	}

	@Override
	public Object module(String name) {
		if (!modules.containsKey(name)) {
			throw new InvalidUsageException("there is no loaded module '" + name + "'");
		}
		return modules.get(name);
	}

	@Override
	public void sendJson(String json) {
		logger.info("sending " + json);
		responder.send(json);
	}

	public void lock(int reqId, String msg) {
		counter.lock(reqId, "lock " + msg);
		logger.info("lock " + msg + ": counter = " + counter.getCount());
	}
	
	public void unlock(int reqId, String msg) {
		counter.release(reqId, "unlock " + msg);
		logger.info("unlock " + msg + ": counter = " + counter.getCount());
		if (counter.isZero() && readyWhenZero) {
			controller.ready();
			readyWhenZero = false;
		}
	}

	public LockingCounter getTestCounter() {
		return counter;
	}

	@Override
	public void close(Object t) {
		logger.info("closed bridge " + this + " with " + responder);
		for (Object m : conns.values()) {
			logger.info("need to clean up module " + m);
			if (m instanceof Closeable) {
				try {
					((Closeable)m).close();
				} catch (IOException ex) {
					logger.error("error closing module " + m, ex);
				}
			}
		}
		responder = null;
		shutdownCounter.countDown();
	}

	public void waitForShutdown() {
		try {
			if (wantTimeout) {
				if (!shutdownCounter.await(15, TimeUnit.SECONDS))
					logger.error("timed out waiting for bridge to be closed");
			} else
				shutdownCounter.await();
		} catch (InterruptedException ex) {
			logger.error("interrupted waiting for bridge to be closed");
		}
	}
}