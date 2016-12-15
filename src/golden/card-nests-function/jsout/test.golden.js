if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._NestFunction = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.NestFunction';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.NestFunction = function(v0) {
  "use strict";
  return new test.golden._NestFunction(v0);
}

test.golden._NestFunction.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._NestFunction.B1(new CardArea(parent, wrapper, this));
}

test.golden._NestFunction.B1 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello');
  this._onAssign(this._card, 'sayHello', test.golden._NestFunction.B1.prototype._setVariableFormats);
  test.golden._NestFunction.B1.prototype._setVariableFormats.call(this);
}

test.golden._NestFunction.B1.prototype = new TextArea();

test.golden._NestFunction.B1.prototype.constructor = test.golden._NestFunction.B1;

test.golden._NestFunction.B1.prototype._setVariableFormats = function() {
  this._mydiv.setAttribute('class', join(FLEval.full(this.formats_0()), ' '));
}

test.golden.NestFunction.prototype.styleIf = function(v0, v1) {
  "use strict";
  if (v1) {
    return v0;
  }
  return '';
}

test.golden._NestFunction.B1.prototype.formats_0 = function() {
  "use strict";
  var v2 = FLEval.oclosure(this._card, FLEval.curry, test.golden.NestFunction.prototype.styleIf, 2);
  var v0 = FLEval.closure(v2, 'show', this._card.sayHello);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden;
