org = function() {
}

org.ziniki = function() {
}

org.ziniki._ID = function(v0) {
  "use strict";
  this._ctor = 'org.ziniki.ID';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
  }
  else {
  }
}

org.ziniki.ID = function(v0) {
  "use strict";
  return new org.ziniki._ID({id: v0});
}

org.ziniki;
