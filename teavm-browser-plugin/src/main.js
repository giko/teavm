EventTypes = {
    INVOKE_METHOD: 5
};

var connection = null;

chrome.browserAction.onClicked.addListener(function() {
    connection = new WebSocket("ws://localhost:8888/");
    webSocket.binaryType = "arraybuffer";
    webSocket.onmessage = function(event) {
        receiveMessage(event.data);
    };
});

function receiveMessage(data) {
    var input = new InputStream(new Int8Array(data));
    switch (input.readByte()) {
        case EventTypes.INVOKE_METHOD:
            break;
    }
};

function InputStream(data) {
    this.data = data;
    this.index = 0;
};
InputStream.prototype.readByte = function() {
    return this.data[this.index++];
};
InputStream.prototype.readShort = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var r = ((a & 0xFF) << 8) | (b & 0xFF);
    return (r << 16) >> 16;
};
InputStream.prototype.readInt = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var c = this.data[this.index++];
    var d = this.data[this.index++];
    return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((c & 0xFF) << 8) | (d & 0xFF);
};
InputStream.prototype.readUTF8 = function() {
    
};

function ValueReader(reader) {
    this.reader = reader;
};
ValueReader.prototype.read = function() {
    switch (this.reader.readByte()) {
        
    }
};

function ObjectRepository() {
    this.map = {};
}