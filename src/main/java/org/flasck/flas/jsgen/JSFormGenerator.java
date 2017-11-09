package org.flasck.flas.jsgen;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSFormGenerator {
	private JSTarget target;
	private HSIEForm form;

	public JSFormGenerator(JSTarget target, HSIEForm form) {
		this.target = target;
		this.form = form;
	}

	public void generate() {
		form.dump(LoggerFactory.getLogger("Generator"));
		String jsname = form.funcName.jsName();
		if (form.isMethod()) { // TODO: we should have jsPName() or something ...
			int idx = jsname.lastIndexOf(".");
			jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
			if (form.mytype == CodeType.HANDLER) {
				idx = jsname.lastIndexOf('.', idx-1);
			} else if (form.mytype == CodeType.AREA) {
				idx = -1;
			} else {
				idx = jsname.lastIndexOf("._C");
				if (idx == -1) idx = jsname.lastIndexOf("._S");
			}
			if (idx != -1) jsname = jsname.substring(0, idx+1) + "_" + jsname.substring(idx+1);
		} else if (form.mytype == CodeType.EVENT) {
			int idx = jsname.lastIndexOf(".");
			jsname = jsname.substring(0, idx);
		}
		JSForm ret = JSForm.function(jsname, form.vars, form.scoped, form.nformal);
		generateBlock(form.funcName, form, ret, form);
		target.add(ret);
	}

	private void generateBlock(FunctionName fn, HSIEForm form, JSForm into, HSIEBlock input) {
		for (HSIEBlock h : input.nestedCommands()) {
			if (h instanceof Head) {
				into.addAll(JSForm.head(((Head)h).v));
			} else if (h instanceof Switch) {
				Switch s = (Switch)h;
				JSForm sw = JSForm.switchOn(s.ctor, s.var);
				generateBlock(fn, form, sw, s);
				into.add(sw);
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				List<JSForm> bs = JSForm.ifCmd(form, c);
				into.addAll(bs);
				generateBlock(fn, form, bs.get(bs.size()-1), c);
			} else if (h instanceof BindCmd) {
				into.add(JSForm.bind((BindCmd) h));
			} else if (h instanceof PushReturn) {
				PushReturn r = (PushReturn) h;
				into.addAll(JSForm.ret(form, r));
			} else if (h instanceof ErrorCmd) {
				into.add(JSForm.error(fn));
			} else {
				System.out.println("Cannot generate block:");
				h.dumpOne((Logger)null, 0);
			}
		}
	}

}
