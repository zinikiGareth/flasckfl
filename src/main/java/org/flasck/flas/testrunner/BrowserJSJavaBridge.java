package org.flasck.flas.testrunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.server.tda.WSReceiver;
import org.ziniki.ziwsh.intf.JsonSender;
import org.ziniki.ziwsh.intf.WSResponder;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;

public class BrowserJSJavaBridge implements JSJavaBridge, WSReceiver {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected static Logger debugLogger = LoggerFactory.getLogger("DebugLog");
	private final JSRunner controller;
	private final ClassLoader classloader;
	private final File root;
	private final LockingCounter counter;
	protected final List<String> errors = new ArrayList<>();
	private final Map<Class<?>, Object> modules = new HashMap<>();
	private Map<String, Object> conns = new TreeMap<>();
	private int next = 1;
	private WSResponder responder;
	private boolean readyWhenZero = false;

	BrowserJSJavaBridge(JSRunner controller, ClassLoader classloader, File root, LockingCounter counter) {
		this.controller = controller;
		this.classloader = classloader;
		this.root = root;
		this.counter = counter;
	}

	@Override
	public void open(WSResponder responder) {
		this.responder = responder;
		logger.info("opened chrome ws");
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
				responder.send(resp.toString());
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
				lock(jo.getString("msg"));
				break;
			}
			case "unlock": {
				unlock(jo.getString("msg"));
				break;
			}
			case "module": {
				String name = jo.getString("name");
				Object ret = module(null, name);
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

	public void runStep(String step) throws JSONException {
		sendJson(new JSONObject().put("action", "runStep").put("step", step).toString());
	}

	public void checkContextSatisfied() throws JSONException {
		sendJson(new JSONObject().put("action", "assertSatisfied").toString());
	}

	@Override
	public void error() {
		logger.error("error on chrome ws");
	}

	@Override
	public void close(Object t) {
		logger.info("closed chrome ws");
		responder = null;
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
	public Object module(Object runner, String s) {
		try {
			Class<?> clz = Class.forName(s);
			if (!modules.containsKey(clz)) {
				modules.put(clz, Reflection.callStatic(clz, "createChrome", this, classloader, root));
			}
			return modules.get(clz);
		} catch (IllegalArgumentException | ClassNotFoundException e) {
			throw WrappedException.wrap(e);
		}
	}

	@Override
	public void transport(JsonSender toZiniki) {
//		if (Platform.isFxApplicationThread()) {
//			JSObject utrunner = (JSObject) page.executeScript("window.utrunner");
//			utcall("transport", toZiniki);
//		} else
//			throw new RuntimeException("Could not pass transport to JS: not in FX thread");
	}

	@Override
	public void sendJson(String json) {
		logger.info("sending " + json);
		responder.send(json);
	}

//	private void doSend(String json) {
//		JSObject utrunner = (JSObject) page.executeScript("window.utrunner");
//		try {
//			utcall("deliver", json);
//		} catch (JSException ex) {
//			JSlogger.error("JSException " + ex);
//		}
//	}
	
	public void lock(String msg) {
		counter.lock("lock " + msg);
		logger.info("lock " + msg + ": counter = " + counter.getCount());
	}
	
	public void unlock(String msg) {
		counter.release("unlock " + msg);
		logger.info("unlock " + msg + ": counter = " + counter.getCount());
		if (counter.isZero() && readyWhenZero)
			controller.ready();
	}

	public LockingCounter getTestCounter() {
		return counter;
	}
}