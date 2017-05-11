var NCubeJsonEditor = (function ($) {
    var nce = null;
    var _editor = null;
    var _saveBtn = null;

    function init(info) {
        var JsonMode;
        // Create JSON Editor (http://jsoneditoronline.org/downloads/)
        if (!nce) {
            nce = info;
            JsonMode = ace.require('ace/mode/json').Mode;
            _editor = ace.edit('editor');
            _editor.$blockScrolling = Infinity;
            _editor.renderer.setShowPrintMargin(false);
            _editor.session.setMode(new JsonMode);

            _editor.on('change', function() {
                updateDirtyStatus();
            });

            _editor.session.on('changeScrollTop', function(scrollTop) {
                saveViewPosition(scrollTop);
            });

            _editor.commands.addCommand({
                name: 'saveCommand',
                bindKey: { win: 'Ctrl-S', mac: 'Command-S' },
                exec: function(editor) {
                    save();
                }
            });
        }
        _saveBtn = $('#saveButton');
        _saveBtn.on('click', function () {
            save();
        });
    }

    function scrollToSavedPosition() {
        var pos = nce.getViewPosition() || 0;
        _editor.session.setScrollTop(pos);
    }

    function saveViewPosition(scrollInfo) {
        nce.saveViewPosition(scrollInfo);
    }

    function save() {
        var result;
        var selectedTabAppId = nce.getSelectedTabAppId();
        nce.clearNote();
        if (selectedTabAppId.branch === 'HEAD') {
            nce.selectBranch();
            return;
        }

        clearDirtyStatus();
        result = nce.call(CONTROLLER + CONTROLLER_METHOD.SAVE_JSON, [selectedTabAppId, _editor.getValue()]);
        if (result.status) {
            nce.updateCubeLeftHandChangedStatus(nce.getSelectedCubeName(), CHANGETYPE.UPDATED);
        } else {
            nce.showNote('Error saving JSON n-cube:<hr class="hr-small"/>' + result.data);
        }
    }

    function isDirty() {
        return _editor ? !_editor.getSession().getUndoManager().isClean() : false;
    }

    function clearDirtyStatus() {
        var undoMgr;
        if (_editor) {
            undoMgr = _editor.getSession().getUndoManager();
            undoMgr.reset();
            undoMgr.markClean();
        }
        updateDirtyStatus();
    }

    function updateDirtyStatus() {
        var dirty, text;
        dirty = isDirty();
        _saveBtn.prop('disabled', !dirty);
        _saveBtn.html(dirty ? 'Save*' : 'Save');
    }

    function load() {
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.GET_JSON, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), {mode:JSON_MODE.PRETTY}], {noResolveRefs:true});
        if (result.status) {
            try {
                _editor.setValue(result.data);
                _editor.clearSelection();
                scrollToSavedPosition();
            } catch (e) {
                nce.showNote('JSON content too large for the JSON Text Editor.  Capture the JSON from the "Revision History" option.');
                console.log(e);
            }
        } else {
            _editor.setValue("Unable to load '" + nce.getSelectedCubeName() + "'. " + result.data);
        }

        clearDirtyStatus();
    }

    function handleCubeSelected() {
        load();
    }

    // Let parent (main frame) know that the child window has loaded.
    // The loading of all of the Javascript (deeply) is continuous on the main thread.
    // Therefore, the setTimeout(, 1) ensures that the main window (parent frame)
    // is called after all Javascript has been loaded.
    if (window.parent.frameLoaded) {
        setTimeout(function () {
            window.parent.frameLoaded(document);
        }, 1);
    }

    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load
    };

})(jQuery);

function tabActivated(info) {
    NCubeJsonEditor.init(info);
    NCubeJsonEditor.load();
}

function cubeSelected() {
    NCubeJsonEditor.handleCubeSelected();
}

var onNoteEvent = function onNoteEvent(e, element){};