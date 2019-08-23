
var exec = require('cordova/exec');

var PLUGIN_NAME = 'MiPlugin';

var MiPlugin = {
  saludo: function (name, successCallback, errorCallback){
        exec(successCallback, errorCallback, PLUGIN_NAME, "saludar", [name]);
  },
  list: function (fnSuccess, fnError, name) {
    exec(fnSuccess, fnError, PLUGIN_NAME, "list", [name]);
},

};

module.exports = MiPlugin;
