if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._ClickIt = function(v0) {
  "use strict";
  this._ctor = 'test.golden.ClickIt';
}

test.golden.ClickIt = function() {
  "use strict";
  return new test.golden._ClickIt({});
}

test.golden._Polyed = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Polyed';
  if (v0) {
    if (v0.x) {
      this.x = v0.x;
    }
    if (v0.y) {
      this.y = v0.y;
    }
    if (v0.z) {
      this.z = v0.z;
    }
  }
  else {
  }
}

test.golden.Polyed = function(v0, v1, v2) {
  "use strict";
  return new test.golden._Polyed({x: v0, y: v1, z: v2});
}

test.golden._Thing = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Thing';
  if (v0) {
    if (v0.x) {
      this.x = v0.x;
    }
  }
  else {
  }
}

test.golden.Thing = function(v0) {
  "use strict";
  return new test.golden._Thing({x: v0});
}

test.golden._Complex = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Complex';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this.yoyo = undefined;
  this.yoyo = FLEval.full(this.yoyo);
  this.format = "basic";
  this.format = FLEval.full(this.format);
  this.mapper = undefined;
  this.mapper = FLEval.full(this.mapper);
  this.hello = "hello, world";
  this.hello = FLEval.full(this.hello);
  this.list = undefined;
  this.list = FLEval.full(this.list);
  this._services = {};
  this._services['test.golden.Offer'] = test.golden.Complex._S0.apply(this);
  this._contracts = {};
  this._contracts['test.golden.Init'] = test.golden.Complex._C0.apply(this);
  this._contracts['test.golden.DataStore'] = test.golden.Complex._C1.apply(this);
  this.ds = this._contracts['test.golden.DataStore'];
}

test.golden.Complex = function(v0) {
  "use strict";
  return new test.golden._Complex(v0);
}

test.golden._SubCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.SubCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.SubCard = function(v0) {
  "use strict";
  return new test.golden._SubCard(v0);
}

test.golden.Complex.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Complex._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.Init';
}

test.golden.Complex._C0 = function() {
  "use strict";
  return new test.golden.Complex.__C0(this);
}

test.golden.Complex.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Complex._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.DataStore';
}

test.golden.Complex._C1 = function() {
  "use strict";
  return new test.golden.Complex.__C1(this);
}

test.golden.Complex.__S0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Complex._S0';
  this._card = v0;
  this._special = 'service';
  this._contract = 'test.golden.Offer';
}

test.golden.Complex._S0 = function() {
  "use strict";
  return new test.golden.Complex.__S0(this);
}

if (typeof test.golden.Complex === 'undefined') {
  test.golden.Complex = function() {
  }
}

if (typeof test.golden.Complex.FooHandler === 'undefined') {
  test.golden.Complex.FooHandler = function() {
  }
}

test.golden.Complex._FooHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'test.golden.Complex.FooHandler';
  this._card = v0;
  this._special = 'handler';
  this._contract = 'test.golden.Handler';
  this.k = v1;
}

test.golden.Complex.FooHandler = function(v0) {
  "use strict";
  return new test.golden.Complex._FooHandler(this, v0);
}

test.golden._SubCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden._Complex.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Complex.B1(new CardArea(parent, wrapper, this));
}

test.golden._Complex.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'hello', test.golden._Complex.B1.prototype._setAttr_1);
  test.golden._Complex.B1.prototype._setAttr_1.call(this);
  var b2 = new test.golden._Complex.B2(this);
  var b3 = new test.golden._Complex.B3(this);
  var b6 = new test.golden._Complex.B6(this);
  var b9 = new test.golden._Complex.B9(this);
}

test.golden._Complex.B1.prototype = new DivArea();

test.golden._Complex.B1.prototype.constructor = test.golden._Complex.B1;

test.golden._Complex.B1.prototype._setAttr_1 = function() {
  var attr = this._card.hello;
  attr = FLEval.full(attr);
  if (attr && !(attr instanceof FLError)) {
    this._mydiv.setAttribute('title', attr);
  }
}

test.golden._Complex.B2 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'hello', test.golden._Complex.B2.prototype._contentExpr);
  test.golden._Complex.B2.prototype._contentExpr.call(this);
  this._onAssign(this._card, 'format', test.golden._Complex.B2.prototype._setVariableFormats);
  test.golden._Complex.B2.prototype._setVariableFormats.call(this);
}

test.golden._Complex.B2.prototype = new TextArea();

test.golden._Complex.B2.prototype.constructor = test.golden._Complex.B2;

test.golden._Complex.B2.prototype._contentExpr = function() {
  var str = this._card.hello;
  this._assignToText(str);
}

test.golden._Complex.B2.prototype._setVariableFormats = function() {
  var attr = FLEval.closure(Cons, this._card.format, Nil);
  attr = FLEval.full(attr);
  this._mydiv.setAttribute('class', join(FLEval.full(attr), ' '));
}

test.golden._Complex.B3 = function(parent) {
  ListArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'list', test.golden._Complex.B3.prototype._assignToVar);
  test.golden._Complex.B3.prototype._assignToVar.call(this);
}

test.golden._Complex.B3.prototype = new ListArea();

test.golden._Complex.B3.prototype.constructor = test.golden._Complex.B3;

test.golden._Complex.B3.prototype._newChild = function() {
  return new test.golden._Complex.B4(this);
}

test.golden._Complex.B4 = function(parent) {
  DivArea.call(this, parent, 'li');
  if (!parent) return;
  this._src_lv = this;
  var b5 = new test.golden._Complex.B5(this);
}

test.golden._Complex.B4.prototype = new DivArea();

test.golden._Complex.B4.prototype.constructor = test.golden._Complex.B4;

test.golden._Complex.B4.prototype._assignToVar = function(obj) {
  if (this. lv == obj) return;
  if (this.lv) {
     this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this.lv = obj;
  if (this.lv) {
    this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this._fireInterests();
}

test.golden._Complex.B5 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._src_lv = parent._src_lv;
  this._src_lv._interested(this, test.golden._Complex.B5.prototype._contentExpr);
  test.golden._Complex.B5.prototype._contentExpr.call(this);
}

test.golden._Complex.B5.prototype = new TextArea();

test.golden._Complex.B5.prototype.constructor = test.golden._Complex.B5;

test.golden._Complex.B5.prototype._contentExpr = function() {
  var str = this._src_lv.lv;
  this._assignToText(str);
}

test.golden._Complex.B3.prototype._assignToVar = function() {
  var lv = this._card.list;
  lv = FLEval.full(lv);
  ListArea.prototype._assignToVar.call(this, lv);
}

test.golden._Complex.B6 = function(parent) {
  ListArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'list', test.golden._Complex.B6.prototype._assignToVar);
  test.golden._Complex.B6.prototype._assignToVar.call(this);
}

test.golden._Complex.B6.prototype = new ListArea();

test.golden._Complex.B6.prototype.constructor = test.golden._Complex.B6;

test.golden._Complex.B6.prototype._newChild = function() {
  return new test.golden._Complex.B7(this);
}

test.golden._Complex.B7 = function(parent) {
  DivArea.call(this, parent, 'li');
  if (!parent) return;
  var b8 = new test.golden._Complex.B8(this);
}

test.golden._Complex.B7.prototype = new DivArea();

test.golden._Complex.B7.prototype.constructor = test.golden._Complex.B7;

test.golden._Complex.B8 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello');
  test.golden._Complex.B8.prototype._add_handlers.call(this);
}

test.golden._Complex.B8.prototype = new TextArea();

test.golden._Complex.B8.prototype.constructor = test.golden._Complex.B8;

test.golden._Complex.B8.prototype._add_handlers = function() {
  var ehclick = test.golden.Complex.prototype.sayHello;
  this._mydiv['onclick'] = function(event) {
    this._area._wrapper.dispatchEvent(ehclick, event);
  }
}

test.golden._Complex.B6.prototype._assignToVar = function() {
  var lv = this._card.list;
  lv = FLEval.full(lv);
  ListArea.prototype._assignToVar.call(this, lv);
}

test.golden._Complex.B9 = function(parent) {
  CasesArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'hello', test.golden._Complex.B9.prototype._chooseCase);
  test.golden._Complex.B9.prototype._chooseCase.call(this);
}

test.golden._Complex.B9.prototype = new CasesArea();

test.golden._Complex.B9.prototype.constructor = test.golden._Complex.B9;

test.golden._Complex.B9.prototype._chooseCase = function(parent) {
  "use strict";
  var cond;
  cond = FLEval.closure(FLEval.compeq, this._card.hello, 'hello');
  if (FLEval.full(cond)) {
    this._setTo(test.golden._Complex.B10);
    return;
  }
  this._setTo(test.golden._Complex.B11);
  return;
}

test.golden._Complex.B10 = function(parent) {
  CardSlotArea.call(this, parent, { explicit: test.golden.SubCard});
  if (!parent) return;
}

test.golden._Complex.B10.prototype = new CardSlotArea();

test.golden._Complex.B10.prototype.constructor = test.golden._Complex.B10;

test.golden._Complex.B11 = function(parent) {
  CardSlotArea.call(this, parent, undefined);
  if (!parent) return;
  this._onAssign(this._card, 'yoyo', test.golden._Complex.B11.prototype._yoyoExpr);
  test.golden._Complex.B11.prototype._yoyoExpr.call(this);
}

test.golden._Complex.B11.prototype = new CardSlotArea();

test.golden._Complex.B11.prototype.constructor = test.golden._Complex.B11;

test.golden._Complex.B11.prototype._yoyoExpr = function() {
  var card = this._card.yoyo;
  this._updateToCard(card);
}

test.golden.Complex._FooHandler.prototype.reply = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (typeof v0 === 'string') {
    var v1 = FLEval.closure(append, this.k, v0);
    var v2 = FLEval.closure(Assign, this._card, 'hello', v1);
    return FLEval.closure(Cons, v2, Nil);
  }
  return FLEval.error("test.golden.Complex.FooHandler.reply: case not handled");
}

test.golden.Complex.__C0.prototype.ready = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.Thing')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'x');
    var v2 = FLEval.oclosure(this._card, test.golden.Complex.FooHandler, 'yo');
    var v3 = FLEval.closure(Cons, v2, Nil);
    var v4 = FLEval.closure(Cons, v1, v3);
    var v5 = FLEval.closure(Send, this._card.ds, 'get', v4);
    return FLEval.closure(Cons, v5, Nil);
  }
  return FLEval.error("test.golden.Complex._C0.ready: case not handled");
}

test.golden.Complex.__S0.prototype.get = function(v0, v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isA(v1, 'test.golden.Handler')) {
    v0 = FLEval.head(v0);
    if (v0 instanceof FLError) {
      return v0;
    }
    if (typeof v0 === 'string') {
      var v2 = FLEval.closure(Cons, v0, Nil);
      var v3 = FLEval.closure(Send, v1, 'reply', v2);
      return FLEval.closure(Cons, v3, Nil);
    }
  }
  return FLEval.error("test.golden.Complex._S0.get: case not handled");
}

test.golden.Complex.contents_1 = function() {
  "use strict";
  return this._card.hello;
}

test.golden.Complex.contents_4 = function() {
  "use strict";
  return this._src_lv.lv;
}

test.golden.Complex.formats_2 = function() {
  "use strict";
  return FLEval.closure(Cons, this._card.format, Nil);
}

test.golden.Complex.handlers_6 = function() {
  "use strict";
  return test.golden.Complex.prototype.sayHello;
}

test.golden.Complex.lvs_3 = function() {
  "use strict";
  return this._card.list;
}

test.golden.Complex.lvs_5 = function() {
  "use strict";
  return this._card.list;
}

test.golden.Complex.ors_7 = function() {
  "use strict";
  return FLEval.closure(FLEval.compeq, this._card.hello, 'hello');
}

test.golden.Complex.prototype.sayHello = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.ClickIt')) {
    var v1 = FLEval.closure(Assign, this, 'hello', 'goodbye');
    return FLEval.closure(Cons, v1, Nil);
  }
  return FLEval.error("test.golden.Complex.sayHello: case not handled");
}

test.golden.Complex.teas_0 = function() {
  "use strict";
  return this._card.hello;
}

test.golden.Complex.yoyos_9 = function() {
  "use strict";
  return this._card.yoyo;
}

test.golden;
