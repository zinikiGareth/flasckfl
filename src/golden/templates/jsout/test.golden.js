if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Item = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Item';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
    if (v0.title) {
      this.title = v0.title;
    }
    if (v0.format) {
      this.format = v0.format;
    }
  }
  else {
  }
}

test.golden.Item = function(v0, v1, v2) {
  "use strict";
  return new test.golden._Item({id: v0, title: v1, format: v2});
}

test.golden._Templates = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Templates';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.Templates = function(v0) {
  "use strict";
  return new test.golden._Templates(v0);
}

test.golden._Templates.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Templates.B1(new CardArea(parent, wrapper, this));
}

test.golden._Templates.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._Templates.B2(this);
  var b6 = new test.golden._Templates.B6(this);
  var b7 = new test.golden._Templates.B7(this);
}

test.golden._Templates.B1.prototype = new DivArea();

test.golden._Templates.B1.prototype.constructor = test.golden._Templates.B1;

test.golden._Templates.B2 = function(parent) {
  ListArea.call(this, parent);
  if (!parent) return;
  test.golden._Templates.B2.prototype._assignToVar.call(this);
  this._onAssign(this._card, 'data', test.golden._Templates.B2.prototype._assignToVar);
}

test.golden._Templates.B2.prototype = new ListArea();

test.golden._Templates.B2.prototype.constructor = test.golden._Templates.B2;

test.golden._Templates.B2.prototype._newChild = function() {
  return new test.golden._Templates.B3(this);
}

test.golden._Templates.B3 = function(parent) {
  DivArea.call(this, parent, 'li');
  if (!parent) return;
  this._src_e = this;
  var b4 = new test.golden._Templates.B4(this);
  var b5 = new test.golden._Templates.B5(this);
}

test.golden._Templates.B3.prototype = new DivArea();

test.golden._Templates.B3.prototype.constructor = test.golden._Templates.B3;

test.golden._Templates.B3.prototype._assignToVar = function(obj) {
  if (this. e == obj) return;
  if (this.e) {
     this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this.e = obj;
  if (this.e) {
    this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this._fireInterests();
}

test.golden._Templates.B4 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._src_e = parent._src_e;
  this._src_e._interested(this, test.golden._Templates.B4.prototype._contentExpr);
  this._onAssign(this._src_e.e, 'title', test.golden._Templates.B4.prototype._contentExpr);
  test.golden._Templates.B4.prototype._contentExpr.call(this);
  this._src_e._interested(this, test.golden._Templates.B4.prototype._setVariableFormats);
  this._onAssign(this._src_e.e, 'format', test.golden._Templates.B4.prototype._setVariableFormats);
  test.golden._Templates.B4.prototype._setVariableFormats.call(this);
}

test.golden._Templates.B4.prototype = new TextArea();

test.golden._Templates.B4.prototype.constructor = test.golden._Templates.B4;

test.golden._Templates.B4.prototype._contentExpr = function() {
  this._assignToText(this.contents_1());
}

test.golden._Templates.B4.prototype._setVariableFormats = function() {
  this._mydiv.setAttribute('class', join(FLEval.full(this.formats_2()), ' '));
}

test.golden._Templates.B5 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._src_e = parent._src_e;
  this._src_e._interested(this, test.golden._Templates.B5.prototype._contentExpr);
  this._onAssign(this._src_e.e, 'id', test.golden._Templates.B5.prototype._contentExpr);
  this._onAssign(this.ctsAssign_4(), 'id', test.golden._Templates.B5.prototype._contentExpr);
  test.golden._Templates.B5.prototype._contentExpr.call(this);
}

test.golden._Templates.B5.prototype = new TextArea();

test.golden._Templates.B5.prototype.constructor = test.golden._Templates.B5;

test.golden._Templates.B5.prototype._contentExpr = function() {
  this._assignToText(this.contents_3());
}

test.golden._Templates.B2.prototype._assignToVar = function() {
  var lv = FLEval.full(this.lvs_0());
  ListArea.prototype._assignToVar.call(this, lv);
}

test.golden._Templates.B6 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'header', test.golden._Templates.B6.prototype._contentExpr);
  this._onAssign(this._card.header, 'title', test.golden._Templates.B6.prototype._contentExpr);
  test.golden._Templates.B6.prototype._contentExpr.call(this);
}

test.golden._Templates.B6.prototype = new TextArea();

test.golden._Templates.B6.prototype.constructor = test.golden._Templates.B6;

test.golden._Templates.B6.prototype._contentExpr = function() {
  this._assignToText(this.contents_5());
}

test.golden._Templates.B7 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'header', test.golden._Templates.B7.prototype._contentExpr);
  this._onAssign(this._card.header, 'id', test.golden._Templates.B7.prototype._contentExpr);
  this._onAssign(this.ctsAssign_7(), 'id', test.golden._Templates.B7.prototype._contentExpr);
  test.golden._Templates.B7.prototype._contentExpr.call(this);
}

test.golden._Templates.B7.prototype = new TextArea();

test.golden._Templates.B7.prototype.constructor = test.golden._Templates.B7;

test.golden._Templates.B7.prototype._contentExpr = function() {
  this._assignToText(this.contents_6());
}

test.golden._Templates.B2.prototype.lvs_0 = function() {
  "use strict";
  return this._card.data;
}

test.golden._Templates.B4.prototype.contents_1 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._src_e.e, 'title');
}

test.golden._Templates.B4.prototype.formats_2 = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.field, this._src_e.e, 'format');
  return FLEval.closure(Cons, v0, Nil);
}

test.golden._Templates.B5.prototype.contents_3 = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.field, this._src_e.e, 'id');
  return FLEval.closure(FLEval.field, v0, 'id');
}

test.golden._Templates.B5.prototype.ctsAssign_4 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._src_e.e, 'id');
}

test.golden._Templates.B6.prototype.contents_5 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._card.header, 'title');
}

test.golden._Templates.B7.prototype.contents_6 = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.field, this._card.header, 'id');
  return FLEval.closure(FLEval.field, v0, 'id');
}

test.golden._Templates.B7.prototype.ctsAssign_7 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._card.header, 'id');
}

test.golden;
