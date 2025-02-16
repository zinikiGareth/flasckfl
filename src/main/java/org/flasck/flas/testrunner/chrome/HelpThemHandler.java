package org.flasck.flas.testrunner.chrome;

import java.io.File;
import java.io.StringReader;

import org.ziniki.servlet.basic.InputStreamResponder;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.servlet.tda.Responder;
import org.ziniki.ziwsh.intf.Param;
import org.zinutils.utils.FileUtils;

public class HelpThemHandler extends InputStreamResponder implements RequestProcessor{
	private final File basedir;
	private final String rooturl;

	public HelpThemHandler(@Param("basedir") File basedir, @Param("rooturl") String rooturl) {
		this.basedir = basedir;
		this.rooturl = rooturl;
	}
	
	@Override
	public void process(Responder r) throws Exception {
		String html = figureHTML();
		sendText(r, new StringReader(html), "text/html", html.getBytes().length);
		r.done();
	}

	private String figureHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>\n");
		sb.append("<html>\n");
		sb.append("<body>\n");
		sb.append("<ul>\n");
		for (File f : FileUtils.findFilesUnderMatching(basedir, "*.html")) {
			sb.append("<li> <a href='" + rooturl + "/test/" + f + "'>" + f.getName() + "</a>");
		}
		sb.append("</ul>\n");
		sb.append("</body>\n");
		sb.append("</html>\n");
		return sb.toString();
	}

}
