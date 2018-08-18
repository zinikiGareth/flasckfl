if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Container = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Container';
  if (v0) {
    if (v0.title) {
      this.title = v0.title;
    }
  }
  else {
  }
}

test.golden.Container = function(v0) {
  "use strict";
  return new test.golden._Container({title: v0});
}

test.golden._MyCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.MyCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden._MyCard.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  return msgs.curr;
}

test.golden.MyCard = function(v0) {
  "use strict";
  return new test.golden._MyCard(v0);
}

test.golden._MyCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._MyCard.B1(new CardArea(parent, wrapper, this));
}

test.golden._MyCard.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._MyCard.B2(this);
  var b3 = new test.golden._MyCard.B3(this);
}

test.golden._MyCard.B1.prototype = new DivArea();

test.golden._MyCard.B1.prototype.constructor = test.golden._MyCard.B1;

test.golden._MyCard.B2 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'c', test.golden._MyCard.B2.prototype._contentExpr);
  this._onAssign(this._card.c, 'title', test.golden._MyCard.B2.prototype._contentExpr);
  test.golden._MyCard.B2.prototype._contentExpr.call(this);
  this._editable(test.golden._MyCard.B2._rules);
  this._mydiv.className = 'flasck-editable';
}

test.golden._MyCard.B2.prototype = new TextArea();

test.golden._MyCard.B2.prototype.constructor = test.golden._MyCard.B2;

test.golden._MyCard.B2.prototype._contentExpr = function() {
  this._assignToText(this.contents_0());
}

test.golden._MyCard.B2._rules = {
  save: function(wrapper, text) {
    var containingObject = test.golden._MyCard.B2.editcontainer_1();
    containingObject.title = text;
    wrapper.saveObject(containingObject);
  }
}

test.golden._MyCard.B3 = function(parent) {
  ListArea.call(this, parent);
  if (!parent) return;
  test.golden._MyCard.B3.prototype._assignToVar.call(this);
  this._onAssign(this._card, 'set', test.golden._MyCard.B3.prototype._assignToVar);
}

test.golden._MyCard.B3.prototype = new ListArea();

test.golden._MyCard.B3.prototype.constructor = test.golden._MyCard.B3;

test.golden._MyCard.B3.prototype._newChild = function() {
  return new test.golden._MyCard.B4(this);
}

test.golden._MyCard.B4 = function(parent) {
  DivArea.call(this, parent, 'li');
  if (!parent) return;
  this._src_q = this;
  var b5 = new test.golden._MyCard.B5(this);
}

test.golden._MyCard.B4.prototype = new DivArea();

test.golden._MyCard.B4.prototype.constructor = test.golden._MyCard.B4;

test.golden._MyCard.B4.prototype._assignToVar = function(obj) {
  if (this. q == obj) return;
  if (this.q) {
     this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this.q = obj;
  if (this.q) {
    this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this);
  }
  this._fireInterests();
}

test.golden._MyCard.B5 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._src_q = parent._src_q;
  this._src_q._interested(this, test.golden._MyCard.B5.prototype._contentExpr);
  this._onAssign(this._src_q.q, 'title', test.golden._MyCard.B5.prototype._contentExpr);
  test.golden._MyCard.B5.prototype._contentExpr.call(this);
  this._editable(test.golden._MyCard.B5._rules);
  this._mydiv.className = 'flasck-editable';
}

test.golden._MyCard.B5.prototype = new TextArea();

test.golden._MyCard.B5.prototype.constructor = test.golden._MyCard.B5;

test.golden._MyCard.B5.prototype._contentExpr = function() {
  this._assignToText(this.contents_3());
}

test.golden._MyCard.B5._rules = {
  save: function(wrapper, text) {
    var containingObject = test.golden._MyCard.B5.editcontainer_4();
    containingObject.title = text;
    wrapper.saveObject(containingObject);
  }
}

test.golden._MyCard.B3.prototype._assignToVar = function() {
  var lv = FLEval.full(this.lvs_2());
  ListArea.prototype._assignToVar.call(this, lv);
}

test.golden._MyCard.B2.prototype.contents_0 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._card.c, 'title');
}

test.golden._MyCard.B2.prototype.editcontainer_1 = function() {
  "use strict";
  return this._card.c;
}

test.golden._MyCard.B3.prototype.lvs_2 = function() {
  "use strict";
  return this._card.set;
}

test.golden._MyCard.B5.prototype.contents_3 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._src_q.q, 'title');
}

test.golden._MyCard.B5.prototype.editcontainer_4 = function() {
  "use strict";
  return this._src_q.q;
}

test.golden;
