package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.servlet.basic.InputStreamResponder;
import org.ziniki.servlet.tda.ParameterSource;
import org.ziniki.servlet.tda.RequestPathParameters;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;

public class RunTestHandler extends InputStreamResponder implements RequestProcessor, RequestPathParameters {
	static final Logger logger = LoggerFactory.getLogger("JSRunner");
	private final File basePath;
	private final File flasck;
	private String test;

	public RunTestHandler(@Param("path") File basePath, @Param("flasck") File flasck) {
		this.basePath = new File(basePath.getPath().replace("%20", " "));
		this.flasck = flasck;
	}

	@Override
	public void stringValue(String name, String value, ParameterSource source) {
		if ("*".equals(name)) {
			this.test = value;
		}
	}

	@Override
	public void process(Responder r) throws Exception {
		File f;
		if (test.startsWith("/html/flasck"))
			f = new File(flasck, test.substring(5));
		else
			f = new File(basePath, test);
		logger.info("requested file " + f + " exists: " + f.exists());
		String contentType = null;
		if (f.getName().endsWith(".html"))
			contentType = "text/html";
		else if (f.getName().endsWith(".js"))
			contentType = "text/javascript";
		else if (f.getName().endsWith(".css"))
			contentType = "text/css";
		if (test == null) {
			r.setStatus(400);
			r.write("cannot run test without a valid path", null);
			r.done();
		} else if (contentType == null) {
			r.setStatus(400);
			r.write("cannot find content type for " + test, null);
			r.done();
		} else {
			sendBinary(r, new FileInputStream(f), contentType, f.length());
		}
	}

	@Override
	public void hasSegment(String segment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void integerValue(String name, int value, ParameterSource source) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doubleValue(String name, double value, ParameterSource source) {
		// TODO Auto-generated method stub

	}

}
