if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
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

test.golden._Simple = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Simple';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this.hello = "hello, world";
  this.hello = FLEval.full(this.hello);
  this._services = {};
  this._services['test.golden.Offer'] = test.golden.Simple._S0.apply(this);
  this._contracts = {};
  this._contracts['test.golden.Init'] = test.golden.Simple._C0.apply(this);
  this._contracts['test.golden.DataStore'] = test.golden.Simple._C1.apply(this);
  this.ds = this._contracts['test.golden.DataStore'];
}

test.golden.Simple = function(v0) {
  "use strict";
  return new test.golden._Simple(v0);
}

test.golden.Simple.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Simple._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.Init';
}

test.golden.Simple._C0 = function() {
  "use strict";
  return new test.golden.Simple.__C0(this);
}

test.golden.Simple.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Simple._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.DataStore';
}

test.golden.Simple._C1 = function() {
  "use strict";
  return new test.golden.Simple.__C1(this);
}

test.golden.Simple.__S0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Simple._S0';
  this._card = v0;
  this._special = 'service';
  this._contract = 'test.golden.Offer';
}

test.golden.Simple._S0 = function() {
  "use strict";
  return new test.golden.Simple.__S0(this);
}

if (typeof test.golden.Simple === 'undefined') {
  test.golden.Simple = function() {
  }
}

if (typeof test.golden.Simple.FooHandler === 'undefined') {
  test.golden.Simple.FooHandler = function() {
  }
}

test.golden.Simple._FooHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'test.golden.Simple.FooHandler';
  this._card = v0;
  this._special = 'handler';
  this._contract = 'test.golden.Handler';
  this.k = v1;
}

test.golden.Simple.FooHandler = function(v0) {
  "use strict";
  return new test.golden.Simple._FooHandler(this, v0);
}

test.golden._Simple.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Simple.B1(new CardArea(parent, wrapper, this));
}

test.golden._Simple.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._Simple.B2(this);
}

test.golden._Simple.B1.prototype = new DivArea();

test.golden._Simple.B1.prototype.constructor = test.golden._Simple.B1;

test.golden._Simple.B2 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'hello', test.golden._Simple.B2.prototype._contentExpr);
  test.golden._Simple.B2.prototype._contentExpr.call(this);
}

test.golden._Simple.B2.prototype = new TextArea();

test.golden._Simple.B2.prototype.constructor = test.golden._Simple.B2;

test.golden._Simple.B2.prototype._contentExpr = function() {
  var str = this._card.hello;
  this._assignToText(str);
}

test.golden.Simple._FooHandler.prototype.reply = function(v0) {
  "use strict";
  var v1 = FLEval.closure(append, this.k, v0);
  var v2 = FLEval.closure(Assign, this._card, 'hello', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden.Simple.__C0.prototype.ready = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.Thing')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'x');
    var v2 = FLEval.oclosure(this._card, test.golden.Simple.FooHandler, 'yo');
    var v3 = FLEval.closure(Cons, v2, Nil);
    var v4 = FLEval.closure(Cons, v1, v3);
    var v5 = FLEval.closure(Send, this._card.ds, 'get', v4);
    return FLEval.closure(Cons, v5, Nil);
  }
  return FLEval.error("test.golden.Simple._C0.ready: case not handled");
}

test.golden.Simple.__S0.prototype.get = function(v0, v1) {
  "use strict";
  var v2 = FLEval.closure(Cons, v0, Nil);
  var v3 = FLEval.closure(Send, v1, 'reply', v2);
  return FLEval.closure(Cons, v3, Nil);
}

test.golden;
