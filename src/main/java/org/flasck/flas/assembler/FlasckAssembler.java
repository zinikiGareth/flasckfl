package org.flasck.flas.assembler;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.deployment.concepts.Activatable;
import org.ziniki.deployment.concepts.ApplicationAssembly;
import org.ziniki.deployment.concepts.CardInitializer;
import org.ziniki.deployment.concepts.VisitingPackageInfo;
import org.ziniki.deployment.concepts.WaitAMo;
import org.ziniki.interfaces.ContentObject;

public abstract class FlasckAssembler implements VisitingPackageInfo {
	protected final static Logger logger = LoggerFactory.getLogger("main");
	private final List<String> stdlib =
		Arrays.asList(
			"js/ziwsh.js",
			"js/flas-runtime.js",
			"js/flas-container.js"
		);
	private final String flasckpath;
	protected Activatable traverser;
	private int stdlibpos = 0;
	private boolean useCachebuster = true;

	public FlasckAssembler(String stdlib) {
		this.flasckpath = stdlib;
	}

	public void assemble(ApplicationAssembly source) throws Exception {
		this.traverser = source.visitAsync(this);
		this.traverser.activate();
	}

	@Override
	public WaitAMo begin() {
		writeHTML("<html>\n  <head>\n    <title>");
		return WaitAMo.CONTINUE;
	}

	@Override
	public WaitAMo title(String title) {
		return writeHTML(title);
	}

	@Override
	public WaitAMo afterTitle() {
		return writeHTML("</title>\n");
	}

	@Override
	public WaitAMo templates(ContentObject co) {
		return writeHTML(co.asString());
	}

	@Override
	public WaitAMo beginCss() {
		return WaitAMo.CONTINUE;
	}

	@Override
	public WaitAMo css(String includeCss) {
		String file = includeCss; // TODO: make this a content object we get a link from
		return writeHTML("    <link rel='stylesheet' type='text/css' href='" + file + "'>\n");
	}

	@Override
	public WaitAMo endCss() {
		return WaitAMo.CONTINUE;
	}

	@Override
	public WaitAMo beginJs() {
		while (stdlibpos < stdlib.size()) {
			WaitAMo ret = javascript(flasckpath + "/" + stdlib.get(stdlibpos++));
			if (!ret.pleaseContinue())
				return ret;
		}
		// TODO: if application, write container library (probably wants to be a separate call)
		return WaitAMo.CONTINUE;
	}

	@Override
	public WaitAMo javascript(String path) {
		if (useCachebuster)
			path += "?cachebuster=" + System.currentTimeMillis();

		return writeHTML("    <script src='" + path + "'></script>\n");
	}

	@Override
	public WaitAMo endJs() {
		return writeHTML("  </head>\n");
	}

	@Override
	public WaitAMo beginInit() {
		return writeHTML("  <body>\n");
	}

	@Override
	public WaitAMo initializer(CardInitializer init) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<div id='flaselt_root'></div>\n");
		buffer.append("<script>\n");
		buffer.append("const env = new JSEnv();\n");
		buffer.append("const cx = env.newContext();\n");
		for (String pkg : init.packages())
			buffer.append(pkg + "._init(cx);\n");
		buffer.append("window.maincard = cx.localCard(" + init.mainCard() + ", 'flaselt_root');\n");
		buffer.append("</script>");
		
		return writeHTML(buffer.toString());
	}

	@Override
	public WaitAMo endInit() {
		return WaitAMo.CONTINUE;
	}

	@Override
	public WaitAMo end() {
		return writeHTML("\n  </body>\n</html>");
	}

	protected abstract WaitAMo writeHTML(String html);
}
