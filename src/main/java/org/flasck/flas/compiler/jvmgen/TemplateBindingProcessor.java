package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.ziniki.splitter.FieldType;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.NotImplementedException;

public class TemplateBindingProcessor extends LeafAdapter implements ResultAware {
	enum Mode {
		COND, EXPR
	}
	public class JVMBinding {
		public IExpr cond;
		public List<IExpr> trueBlock = new ArrayList<IExpr>();
		public IExpr du;
	}
	private final FunctionState fs;
	private final StackVisitor sv;
	private final ByteCodeSink templateClass;
	private final AtomicInteger containerIdx;
	private final Template t;
	private final IExpr source;
	private final TemplateField assignsTo;
	private final List<JVMStyleIf> styles = new ArrayList<>();
	private final List<IExpr> cexpr = new ArrayList<>();

	private final List<JVMBinding> bindings = new ArrayList<>();
	private Mode mode;
	private JVMBinding curr;
	private List<IExpr> bindingBlock;
	private TemplateBindingOption currentTBO;
	private int option = 0;

	public TemplateBindingProcessor(FunctionState fs, StackVisitor sv, ByteCodeSink templateClass, AtomicInteger containerIdx, Template t, IExpr source, TemplateBinding b) {
		this.fs = fs;
		this.sv = sv;
		this.templateClass = templateClass;
		this.containerIdx = containerIdx;
		this.t = t;
		this.source = source;
		this.bindingBlock = new ArrayList<IExpr>();
		assignsTo = b.assignsTo;
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		curr = new JVMBinding();
		bindings.add(0, curr);
		currentTBO = option;
		this.option++;
	}
	
	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		mode = Mode.COND;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		mode = Mode.EXPR;
		new ExprGenerator(fs, sv, bindingBlock, false);
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		new TemplateStyling(fs, sv, bindingBlock, tso);
	}

	@Override
	public void result(Object r) {
		if (r instanceof JVMStyleIf) {
			JVMStyleIf si = (JVMStyleIf)r;
			if (si.cond != null)
				styles.add(si);
			else
				cexpr.add(si.style);
		} else {
			if (mode == Mode.COND) {
				curr.cond = (IExpr) r;
				curr.trueBlock = new ArrayList<>();
			} else {
				IExpr expr = (IExpr) r;
				if (currentTBO.sendsTo != null) {
					IExpr tc;
					if (fs.templateObj == null)
						tc = fs.meth.arrayOf(J.OBJECT);
					else {
						ArrayList<IExpr> wanted = new ArrayList<>();
						for (int i : currentTBO.sendsTo.contextPosns()) {
							wanted.add(CollectionUtils.nth(fs.templateObj.values(), i));
						}
						tc = fs.meth.arrayOf(J.OBJECT, wanted);
					}
					boolean isOtherObject = (currentTBO.expr instanceof UnresolvedVar) &&
						((UnresolvedVar)currentTBO.expr).defn() instanceof StructField &&
						((StructField)((UnresolvedVar)currentTBO.expr).defn()).type.defn() instanceof ObjectDefn;

					IExpr invokeOn;
					IExpr send;
					if (isOtherObject) {
						invokeOn = fs.meth.as(expr, J.TEMPLATE_HOLDER);
						send = fs.meth.makeNew(J.OBJECT);
					} else {
						invokeOn = fs.meth.as(fs.container, J.TEMPLATE_HOLDER);
						send = fs.meth.as(expr, J.OBJECT);
					}
					
					curr.du = fs.meth.callInterface("void", invokeOn, "_updateTemplate",
						fs.fcx, fs.renderTree(), 
						fs.meth.stringConst(assignsTo.type().toString().toLowerCase()), fs.meth.stringConst(assignsTo.text),
						fs.meth.intConst(currentTBO.sendsTo.template().position()),
						fs.meth.stringConst(currentTBO.sendsTo.defn().id()),
						send,
						tc);
					if (isOtherObject) {
						curr.du = fs.meth.ifNotNull(invokeOn, curr.du, null);
					}
				} else if (currentTBO.assignsTo.type() == FieldType.CONTAINER) {
					Map<StructDefn, Template> mapping = currentTBO.mapping();
					if (mapping == null)
						throw new NotImplementedException("No mapping");
					int ucidx = containerIdx.getAndIncrement();
					{
						GenericAnnotator gen = GenericAnnotator.newMethod(templateClass, false, "_updateContainer" + ucidx);
						PendingVar fcx = gen.argument(J.FLEVALCONTEXT, "_cxt");
						PendingVar rt = gen.argument(J.RENDERTREE, "_renderTree");
						PendingVar parent = gen.argument(J.ELEMENT, "parent");
						PendingVar e = gen.argument(J.OBJECT, "e");
						gen.returns("void");
						MethodDefiner uc = gen.done();
						IExpr ret = null;
						for (Entry<StructDefn, Template> m : mapping.entrySet()) {
							IExpr curr = templateMember(uc, fcx.getVar(), rt.getVar(), parent.getVar(), m.getValue(), e.getVar());
							if (ret == null)
								ret = curr;
							else {
								IExpr isIt = uc.callInterface("boolean", fcx.getVar(), "isA", e.getVar(), uc.stringConst(m.getKey().name().uniqueName()));
								ret = uc.ifBoolean(isIt, curr, ret);
							}
						}
						ret.flush();
						uc.returnVoid().flush();
					}
					curr.du = fs.meth.callVirtual("void", fs.container, "_updateContainer", fs.fcx, fs.renderTree(), fs.meth.stringConst(assignsTo.text), fs.meth.as(expr, J.OBJECT), fs.meth.intConst(ucidx));
				} else
					curr.du = fs.meth.callVirtual("void", fs.container, "_updateContent", fs.fcx, fs.renderTree(), fs.meth.stringConst(t.webinfo().id()), fs.meth.stringConst(assignsTo.text), fs.meth.intConst(this.option), this.source, fs.meth.as(expr, J.OBJECT));
			}
			this.bindingBlock = curr.trueBlock;
		}
	}
	
	private IExpr templateMember(MethodDefiner uc, Var cx, Var rt, Var parent, Template t, Var e) {
		ArrayList<IExpr> wanted = new ArrayList<>();
		// TODO: the context needs to be considered properly
		IExpr tc = fs.meth.arrayOf(J.OBJECT, wanted);
		
		return uc.callVirtual("void", uc.myThis(), "_addItemWithName", cx, rt, parent, uc.stringConst(t.webinfo().id()), uc.intConst(t.position()), e, tc);
	}

	@Override
	public void leaveTemplateCustomization(TemplateCustomization tc) {
		applyStyles(fs, bindingBlock, t.webinfo().id(), assignsTo, option, source, styles, cexpr, !tc.events.isEmpty());
	}

	static void applyStyles(FunctionState fs, List<IExpr> bindingBlock, String templateName, TemplateField field, int option, IExpr source, List<JVMStyleIf> styles, List<IExpr> cexpr, boolean hasStylingEvents) {
		IExpr ty;
		IExpr tx;
		if (field == null) {
			ty = fs.meth.as(fs.meth.aNull(), J.STRING);
			tx = fs.meth.as(fs.meth.aNull(), J.STRING);
		} else {
			ty = fs.meth.stringConst(field.type().toString().toLowerCase());
			tx = fs.meth.stringConst(field.text);
		}
		if (!styles.isEmpty() || !cexpr.isEmpty()) {
			IExpr ce;
			if (cexpr.isEmpty())
				ce = fs.meth.as(fs.meth.aNull(), J.STRING);
			else if (cexpr.size() == 1)
				ce = cexpr.get(0);
			else
				ce = fs.meth.callStatic(J.BUILTINPKG+".PACKAGEFUNCTIONS", J.STRING, "concatMany", fs.fcx, fs.meth.arrayOf(J.OBJECT, cexpr));
			
			List<IExpr> arr = new ArrayList<>();
			for (JVMStyleIf si : styles) {
				arr.add(si.cond);
				arr.add(si.style);
			}
			
			IExpr doUpdate = fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.renderTree(), fs.meth.stringConst(templateName), ty, tx, fs.meth.intConst(option), source, ce, fs.meth.arrayOf(J.OBJECT, arr));
			bindingBlock.add(doUpdate);
			styles.clear();
			cexpr.clear();
		} else if (hasStylingEvents) {
			IExpr doUpdate = fs.meth.callVirtual("void", fs.container, "_updateStyles", fs.fcx, fs.renderTree(), fs.meth.stringConst(templateName), ty, tx, fs.meth.intConst(option), source, fs.meth.as(fs.meth.aNull(), J.STRING), fs.meth.arrayOf(J.OBJECT));
			bindingBlock.add(doUpdate);
		}
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		currentTBO = null;
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding tb) {
		IExpr ret = null;
		if (bindings.isEmpty() && !bindingBlock.isEmpty())
			ret = JVMGenerator.makeBlock(fs.meth, bindingBlock);
		for (JVMBinding b : bindings) {
			b.trueBlock.add(b.du);
			IExpr truth = JVMGenerator.makeBlock(fs.meth, b.trueBlock);
			if (b.cond == null)
				ret = truth;
			else
				ret = fs.meth.ifBoolean(
					fs.meth.callInterface(JavaType.boolean_.toString(), fs.fcx, "isTruthy", fs.meth.as(b.cond, J.OBJECT)),
					truth,
					ret);
		}
		if (ret != null)
			ret.flush();
		sv.result(null);
	}
}
