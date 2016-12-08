if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Card = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Card';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['test.golden.Ctr'] = test.golden.Card._C0.apply(this);
}

test.golden.Card = function(v0) {
  "use strict";
  return new test.golden._Card(v0);
}

test.golden.Card.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Card._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.Ctr';
}

test.golden.Card._C0 = function() {
  "use strict";
  return new test.golden.Card.__C0(this);
}

test.golden._Card.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Card.__C0.prototype.go = function() {
  "use strict";
  var v0 = FLEval.closure(Cons, 42, Nil);
  var v1 = FLEval.closure(Send, this._card.c, 'put', v0);
  var v2 = FLEval.closure(Send, this._card.c, 'clear', Nil);
  var v3 = FLEval.closure(Cons, v2, Nil);
  return FLEval.closure(Cons, v1, v3);
}

test.golden;