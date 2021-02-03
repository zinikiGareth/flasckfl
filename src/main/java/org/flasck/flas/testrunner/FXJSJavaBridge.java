package org.flasck.flas.testrunner;

import org.ziniki.ziwsh.intf.JsonSender;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.reflection.Reflection;
import org.zinutils.sync.LockingCounter;

import javafx.application.Platform;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class FXJSJavaBridge implements JSJavaBridge {
	private final JSRunner runner;

	FXJSJavaBridge(JSRunner runner) {
		this.runner = runner;
	}

	@Override
	public void error(String s) {
		runner.errors.add(s);
	}
	
	@Override
	public void log(String s) {
		JSRunner.logger.info(s);
	}
	
	@Override
	public Object module(String s) {
		try {
			Class<?> clz = Class.forName(s);
			if (!runner.modules.containsKey(clz)) {
				runner.modules.put(clz, Reflection.callStatic(clz, "createJS", this, runner.classloader, runner.config.root));
			}
			return runner.modules.get(clz);
		} catch (IllegalArgumentException | ClassNotFoundException e) {
			throw WrappedException.wrap(e);
		}
	}

	@Override
	public void transport(JsonSender toZiniki) {
		if (Platform.isFxApplicationThread()) {
			JSObject utrunner = (JSObject) runner.page.executeScript("window.utrunner");
			utrunner.call("transport", toZiniki);
		} else
			throw new RuntimeException("Could not pass transport to JS: not in FX thread");
	}

	@Override
	public void sendJson(String json) {
		if (Platform.isFxApplicationThread()) {
			doSend(json);
		} else {
			runner.uiThread(cdl -> {
				doSend(json);
				cdl.countDown();
			});
		}
	}

	private void doSend(String json) {
		JSObject utrunner = (JSObject) runner.page.executeScript("window.utrunner");
		try {
			utrunner.call("deliver", json);
		} catch (JSException ex) {
			JSRunner.logger.error("JSException " + ex);
		}
	}
	
	public void lock() {
		runner.counter.lock("lock");
	}
	
	public void unlock() {
		runner.counter.release("unlock");
	}

	@Override
	public LockingCounter getTestCounter() {
		return runner.counter;
	}
}