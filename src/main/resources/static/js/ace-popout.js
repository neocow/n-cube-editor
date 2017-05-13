var i, len, modeEl, modeOpt;
var modeList = require("ace/ext/modelist");
var modesByName = modeList.modesByName;
var modes = modeList.modes;
window.aceEditor = ace.edit('editor');
window.aceEditor.$blockScrolling = Infinity;
window.aceEditor.renderer.setShowPrintMargin(false);
window.aceEditor.session.setMode(modesByName['groovy'].mode);
window.aceEditor.session.modeName = 'groovy';

modeEl = document.getElementById('mode');
for (i = 0, len = modes.length; i < len; i++) {
    modeOpt = document.createElement('option');
    modeOpt.text = modes[i].caption;
    modeOpt.value = modes[i].name;
    modeEl.add(modeOpt);
}
modeEl.value = 'groovy';

modeEl.addEventListener('change', function(val) {
    var value = val.target.value;
    window.aceEditor.session.setMode(modesByName[value].mode || modesByName.text.mode);
    window.aceEditor.session.modeName = value;
});

document.getElementById('btn-increase').addEventListener('click', function(e) {
    e.preventDefault();
    increaseFontSize(1);
});

document.getElementById('btn-decrease').addEventListener('click', function(e) {
    e.preventDefault();
    increaseFontSize(-1);
});

function increaseFontSize(amount) {
    var oldSize = window.aceEditor.getFontSize();
    var newSize = oldSize + amount;
    if (newSize > 4 && newSize < 72) {
        window.aceEditor.setFontSize(newSize);
    }
}