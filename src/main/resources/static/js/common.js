/**
 * NCube Editor
 *     Common Javascript utilities for all tabs to use.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */

/**
 * Convert strings containing DOS-style '*' or '?' to a regex String.
 */
function wildcardToRegexString(wildcard) {
    var i, len, c;
    var s = '';

    for (i = 0, len = wildcard.length; i < len; i++) {
        c = wildcard.charAt(i);
        switch (c) {
            case '*':
                s += '.*?';
                break;

            case '?':
                s += '.';
                break;

            // escape special regexp-characters
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
            case '.':
            case '{':
            case '}':
            case '|':
            case '\\':
                s += '\\';
                s += c;
                break;

            default:
                s += c;
                break;
        }
    }
    return s;
}

/**
 * Escape regex characters in source String.  For example, period (.) becomes \.
 */
function escapeRegExp(string) {
    return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}

/**
 * Check all the inputs in a list.
 */
function checkAll(state, queryStr) {
    $(queryStr).filter(':visible').not('.exclude').prop('checked', state).change();
}

function keyCount(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) {
            size++;
        }
    }
    return size;
}

/**
 * SheetClip - Spreadsheet Clipboard Parser
 * version 0.2
 *
 * This tiny library transforms JavaScript arrays to strings that are pasteable by LibreOffice, OpenOffice,
 * Google Docs and Microsoft Excel.
 *
 * Copyright 2012, Marcin Warpechowski
 * Licensed under the MIT license.
 * http://github.com/warpech/sheetclip/
 */

function countQuotes(str) {
    return str.split('"').length - 1;
}

function parseExcelClipboard(str) {
    var r, rlen, rows, arr = [], a = 0, c, clen, multiline, last;
    rows = str.split('\n');
    if (rows.length > 1 && rows[rows.length - 1] === '') {
        rows.pop();
    }

    for (r = 0, rlen = rows.length; r < rlen; r += 1) {
        rows[r] = rows[r].split('\t');
        for (c = 0, clen = rows[r].length; c < clen; c += 1) {
            if (!arr[a]) {
                arr[a] = [];
            }

            if (multiline && c === 0) {
                last = arr[a].length - 1;
                arr[a][last] = arr[a][last] + '\n' + rows[r][0];
                if (multiline && (countQuotes(rows[r][0]) & 1)) { //& 1 is a bitwise way of performing mod 2
                    multiline = false;
                    arr[a][last] = arr[a][last].substring(0, arr[a][last].length - 1).replace(/""/g, '"');
                }
            } else {
                if (c === clen - 1 && rows[r][c].indexOf('"') === 0 && (countQuotes(rows[r][c]) & 1)) {
                    arr[a].push(rows[r][c].substring(1).replace(/""/g, '"'));
                    multiline = true;
                } else {
                    arr[a].push(rows[r][c].replace(/""/g, '"'));
                    multiline = false;
                }
            }
        }
        if (!multiline) {
            a += 1;
        }
    }

    return arr;
}

var delay = (function(){
    var timer = 0;
    return function(callback, ms){
        clearTimeout(timer);
        timer = setTimeout(callback, ms);
    };
})();

function selectAll() {
    checkAll(true, 'input[type="checkbox"]');
}

function selectNone() {
    checkAll(false, 'input[type="checkbox"]');
}

function addSelectAllNoneListeners() {
    $('.select-all').on('click', function() {
        selectAll();
    });
    $('.select-none').on('click', function() {
        selectNone();
    });
}

function addModalFilters() {
    $('.modal-filter').each(function() {
        var items = [];
        var checkBoxes = [];
        var checkedItems = [];
        var contentDiv = $(this);
        var countSpan = $('<span/>');
        var div = $('<div/>');
        var input = $('<input/>');

        function refreshItems() {
            input.val('');
            input.focus();
            items = contentDiv.find('.modal-body').find('li,tr');
            if (items.find('input[type="checkbox"]').length) {
                items = items.has('input[type="checkbox"]:not(".exclude")');
            }
            items.on('remove', function() {
                delay(function() {
                    refreshItems();
                }, 50);
            });
            checkBoxes = items.find('input[type="checkbox"]');
            refreshCount();
        }

        function refreshCount() {
            checkedItems = checkBoxes.filter(function() {
                return $(this)[0].checked;
            });
            countSpan[0].innerHTML = checkedItems.length + ' of ' + items.length + ' Selected';
        }

        countSpan.addClass('pull-left selected-count');
        contentDiv.find('.btn.pull-left:last').after(countSpan);
        
        contentDiv.click(function() {
            refreshCount();
        });
        
        input.addClass('modal-filter-input');
        input.prop({'type':'text','placeholder':'Filter...'});
        input.css({'width':'100%'});
        input.on('keyup', function(e) {
            delay(function() {
                var query = input.val().toLowerCase();
                if (query === '') {
                    items.show();
                } else {
                    items.hide();
                    items.filter(function () {
                        var el, cb, tds, td;
                        var item = $(this);
                        if (item.is('li')) {
                            el = item;
                            cb = item.find('input[type="checkbox"]');
                            if (cb.length) {
                                el = cb.parent();
                            }
                            return el[0].textContent.toLowerCase().indexOf(query) > -1;
                        }
                        if (item.is('tr')) {
                            tds = item.find('td').filter(function() {
                                td = $(this);
                                if (td.find('input[type="checkbox"]').length) {
                                    return false;
                                }
                                return td[0].textContent.toLowerCase().indexOf(query) > -1;
                            });
                            return tds.length;
                        }
                    }).show();
                }
            }, e.keyCode === KEY_CODES.ENTER ? 0 : PROGRESS_DELAY);
        });

        contentDiv.find('.modal-header').after(div);
        div.append(input);

        contentDiv.parent().parent().on('shown.bs.modal', function(){
            refreshItems();
        });
        contentDiv.on('show', function() {
            refreshItems();
        });
    });
}

function modalsDraggable(shouldBeDraggable) {
    $('.modal').each(function() {
        makeModalDraggable($(this), shouldBeDraggable);
    });
}

function makeModalDraggable(modal, shouldBeDraggable) {
    var maxX = 600;
    var maxY = 400;
    var prevX = 0;
    var prevY = 0;
    modal.draggable({
        handle: '.modal-header',
        drag: function(e) {
            var offset = modal.offset();
            var posX = offset.left;
            var posY = offset.top;
            var tooFarLeft = posX < MODAL_TOO_FAR_LEFT && posX < prevX;
            var tooFarRight = posX > maxX && posX > prevX;
            var tooFarUp = posY < 0 && posY < prevY;
            var tooFarDown = posY > maxY && posY > prevY;
            if (tooFarLeft || tooFarRight || tooFarUp || tooFarDown) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
            prevX = posX;
            prevY = posY;
        }
    });
    modal.draggable(shouldBeDraggable ? 'enable' : 'disable');
}


function getStorageKey(nce, prefix) {
    return prefix + ':' + nce.getSelectedTabAppId().app.toLowerCase() + ':' + nce.getSelectedCubeName().toLowerCase();
}

function saveOrDeleteValue(obj, storageKey) {
    if (obj && (typeof obj !== OBJECT || Object.keys(obj).length)) {
        localStorage[storageKey] = JSON.stringify(obj);
    } else {
        delete localStorage[storageKey];
    }
}

function appIdFrom(app, version, status, branch) {
    return {
        app: app,
        version: version,
        status: status,
        branch: branch
    };
}

function populateSelectFromMap(sel, map, keepPrevVal, defVal) {
    var i, len, options, keys;
    var prevVal = sel.val();
    sel.empty();

    options = '<option></option>';
    keys = Object.keys(map).sort(function(a, b) {
        var aArr, bArr;
        if (a.indexOf('/') > -1 && b.indexOf('/') > -1) {
            aArr = a.split('/');
            bArr = b.split('/');
            return bArr[2] - aArr[2]
                || bArr[0] - aArr[0]
                || bArr[1] - aArr[1];
        }
        return a.toLowerCase().localeCompare(b.toLowerCase());
    });
    for (i = 0, len = keys.length; i < len; i++) {
        options += '<option>' + keys[i] + '</option>';
    }

    sel.append(options);
    if (keepPrevVal) {
        sel.val(prevVal);
    } else if (defVal !== undefined) {
        sel.val(defVal);
    }
}

function getDefaultSearchOptions() {
    var opts = {};
    opts[SEARCH_OPTIONS.SEARCH_ACTIVE_RECORDS_ONLY] = true;
    return opts;
}

function getDeletedRecordsSearchOptions() {
    var opts = {};
    opts[SEARCH_OPTIONS.SEARCH_DELETED_RECORDS_ONLY] = true;
    return opts;
}

/*
 * Options:
 *  value
 *  onSave
 */
function popoutAceEditor(opts) {
    var saveFunc = function(editor) {
        opts.onSave(editor.getValue());
        w.close();
    };
    var w = window.open();
    w.document.write('<html><head><title>NCE Text Editor</title>');
    $.ajax({
        async: false,
        type: 'GET',
        url: 'css/bootstrap.min.css',
        success: function(data) {
            w.document.write('<style id="popout-bootstrap">' + data + '</style>');
        }
    });
    $.ajax({
        async: false,
        type: 'GET',
        url: 'css/ace-popout.css',
        success: function(data) {
            w.document.write('<style id="popout-ace-popout">' + data + '</style>');
        }
    });
    w.document.write('</head><body>');
    $.ajax({
        async: false,
        type: 'GET',
        url: 'html/ace-popout.html',
        success: function(data) {
            w.document.write(data);
        }
    });
    w.document.write('</body></html>');
    delay(function() {
        var modeSelect, selectChangeEvent;
        if (opts.readonly) {
            w.aceEditor.setReadOnly(true);
        }

        if (opts.mode) {
            modeSelect = w.document.getElementById('mode');
            modeSelect.value = opts.mode;
            selectChangeEvent = document.createEvent('HTMLEvents');
            selectChangeEvent.initEvent('change', true, true);
            modeSelect.dispatchEvent(selectChangeEvent);
        }

        w.aceEditor.setValue(opts.value);
        w.aceEditor.clearSelection();

        w.aceEditor.commands.addCommand({
            name: 'saveCommand',
            bindKey: { win: 'Ctrl-S', mac: 'Command-S' },
            exec: function(editor) {
                saveFunc(editor);
            }
        });

        $(w.document.body).find('#save').on('click', function() {
            saveFunc(w.aceEditor);
        });
    }, 250);
    return w;
}

function appIdsEqual(id1, id2) {
    return id1 && id2
        && id1.app     === id2.app
        && id1.version === id2.version
        && id1.status  === id2.status
        && id1.branch  === id2.branch;
}

function getNextVersion(currentVersion, partChanged) {
    var i, len;
    var version = currentVersion.split('.');
    version[partChanged] = parseInt(version[partChanged]) + 1;
    for (i = partChanged + 1, len = version.length; i < len; i++) {
        version[i] = 0;
    }
    return version.join('.')
}

function enableDisableMenuButton(el, enable, onClick) {
    el.off('click');
    if (enable) {
        el.parent().removeClass('disabled');
        el.on('click', function() { onClick(); });
    } else {
        el.parent().addClass('disabled');
        el.on('click', function(e) {
            e.preventDefault();
            e.stopImmediatePropagation();
        });
    }
}

(function($) {
    $.fn.hasScrollBar = function() {
        return this.get(0).scrollWidth > this.width();
    };

    $.fn.canvasMeasureWidth = function (font) {
        var canvas;
        if (!jQuery._cachedCanvas) {
            canvas = document.createElement('canvas');
            jQuery._cachedCanvas = canvas.getContext('2d');
        }
        jQuery._cachedCanvas.font = font;
        return jQuery._cachedCanvas.measureText(this[0].innerText).width;
    };
})(jQuery);