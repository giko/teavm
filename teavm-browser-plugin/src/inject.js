__TeaVM_remote__ = (function() {
    function callMethod() {
        var instance = arguments[0];
        var method = arguments[1];
        var params = [];
        for (var i = 2; i < arguments.length; ++i) {
            params.push(arguments[i]);
        }
        if (!instance.method) {
            throw "Method not found: " + method;
        }
        return instance[method].apply(instance, params);
    }
    function getProperty(instance, property) {
        return instance[property];
    }
    function setProperty(instance, property, value) {
        instance[property] = value;
    }
    function createProxy(classId) {
    }
    return { createProxy : createProxy, callMethod : callMethod, getProperty : getProperty,
            setProperty : setProperty };
})();
