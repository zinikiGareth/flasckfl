package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.server.tda.WSReceiver;
import org.ziniki.ziwsh.intf.JsonSender;
import org.ziniki.ziwsh.intf.WSResponder;
import org.zinutils.exceptions.InvalidUsageException;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;

public class BrowserJSJavaBridge implements JSJavaBridge, WSReceiver {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected static Logger debugLogger = LoggerFactory.getLogger("DebugLog");
	protected final List<String> errors = new ArrayList<>();
//	private final Map<Class<?>, Object> modules = new HashMap<>();
	private final LockingCounter counter;
	private Map<String, Object> conns = new TreeMap<>();
	private int next = 1;
	private JSRunner controller;
	private WSResponder responder;

	BrowserJSJavaBridge(JSRunner controller, LockingCounter counter) {
		this.controller = controller;
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
				controller.ready();
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
			case "error": {
				controller.error(jo.getString("error"));
				break;
			}
//			case "step": {
//				Thread.sleep(100); // we aren't properly synced, so wait to see if anything happens ...
//				counter.waitForZero(2500);
//				responder.send(new JSONObject().put("action", "stepdone").toString());
//				break;
//			}
			case "lock": {
				lock();
				break;
			}
			case "unlock": {
				unlock();
				break;
			}
			case "module": {
				String name = jo.getString("name");
				Object ret = module(null, name);
				String conn = "conn" + (next++);
				conns.put(conn, ret);
				responder.send(new JSONObject().put("action", "haveModule").put("name", name).put("clz", "ZinTestModule").put("conn", conn).toString());
				break;
			}
			default:
				throw new InvalidUsageException("there is no action '" + action + "'");
			}
		} catch (Throwable ex) {
			logger.error("error processing " + text, ex);
		}
	}

	public void prepareTest(String test) throws JSONException {
		responder.send(new JSONObject().put("action", "prepareTest").put("testname", test).toString());
	}

	public void runStep(String step) throws JSONException {
		responder.send(new JSONObject().put("action", "runStep").put("step", step).toString());
	}

	public void checkContextSatisfied() throws JSONException {
		responder.send(new JSONObject().put("action", "assertSatisfied").toString());
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
		/*
		try {
			Class<?> clz = Class.forName(s);
			if (!modules.containsKey(clz)) {
				ServerEnvironment env = new ServerEnvironment();
				FLASBroker broker = new FLASBroker(env);
				env.provideBroker(broker);
				env.provideWebSocketFinder(new GrizzlyConnectionStore(broker));
				modules.put(clz, Reflection.callStatic(clz, "createChrome", this, classloader, root, env));
			}
			return modules.get(clz);
		} catch (IllegalArgumentException | ClassNotFoundException e) {
			throw WrappedException.wrap(e);
		}
		*/
		
		return null;
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
		throw new NotImplementedException();
//		if (Platform.isFxApplicationThread()) {
//			doSend(json);
//		} else {
//			uiThread(cdl -> {
//				doSend(json);
//				cdl.countDown();
//			});
//		}
	}

//	private void doSend(String json) {
//		JSObject utrunner = (JSObject) page.executeScript("window.utrunner");
//		try {
//			utcall("deliver", json);
//		} catch (JSException ex) {
//			JSlogger.error("JSException " + ex);
//		}
//	}
	
	public void lock() {
		counter.lock("lock");
	}
	
	public void unlock() {
		counter.release("unlock");
	}

	public LockingCounter getTestCounter() {
		return counter;
	}
}