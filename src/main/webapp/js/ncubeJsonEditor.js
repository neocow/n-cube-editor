/**
 * JSON Editor
 *     Edit n-cubes in JSON format
 *
 * @author John DeRegnaucourt
 */

var NCubeJsonEditor = (function ($)
{
    var nce = null;
    var _editor = null;
    var _vScroll = null;
    var _hScroll = null;

    var init = function(info)
    {
        // Create JSON Editor (http://jsoneditoronline.org/downloads/)
        if (!nce)
        {
            nce = info;
            var container = document.getElementById('jsoneditor');
            var options =
            {
                mode: 'code',
                change: function ()
                {
                    updateDirtyStatus();
                }
            };

            _editor = new JSONEditor(container, options);
            addSaveButton();
        }
        _vScroll = $('.ace_scrollbar-v');
        _hScroll = $('.ace_scrollbar-h');

        _vScroll.scroll(function() {
            clearTimeout($.data(this, 'scrollVTimer'));
            $.data(this, 'scrollVTimer', setTimeout(function() {
                saveViewPosition({scrollTop: _vScroll.scrollTop()});
            }, 250));
        });

        _hScroll.scroll(function() {
            clearTimeout($.data(this, 'scrollHTimer'));
            $.data(this, 'scrollHTimer', setTimeout(function() {
                saveViewPosition({scrollLeft: _hScroll.scrollLeft()});
            }, 250));
        });

        // have to use this hack to get around lack of exposed API in json editor for the underlying ace editor
        MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
        var observer = new MutationObserver(function(mutations, observer) {
            scrollToSavedPosition();
        }).observe(_vScroll[0], {
            subtree: true,
            attributes: true,
            attributeFilter: ['style']
        });
    };

    var getSavedScrollPosition = function() {
        var pos = nce.getViewPosition();
        if (typeof pos !== 'object') {
            pos = {scrollTop:0, scrollLeft:0};
        }
        return pos;
    };

    var scrollToSavedPosition = function() {
        var pos = getSavedScrollPosition();
        _vScroll.scrollTop(pos.scrollTop);
        _hScroll.scrollLeft(pos.scrollLeft);
    };

    var saveViewPosition = function (scrollInfo) {
        var pos = getSavedScrollPosition();
        $.extend(pos, scrollInfo);
        nce.saveViewPosition(pos);
    };

    var addSaveButton = function()
    {
        var editCtrl = $('#jsoneditor');
        var menu = editCtrl.find('.menu');
        var save = $("<button/>").attr(
        {
            id: 'saveButton',
            style: 'background-image:none;width:64px',
            title: 'Save changes'
        });
        menu.append(save);           // Add 'Save' button to toolbar
        menu.find('a').remove();

        // Attach listener
        $('#saveButton').click(function ()
        {
            nce.clearError();
            if (nce.isHeadSelected())
            {
                nce.selectBranch();
                return;
            }

            clearDirtyStatus();
            updateDirtyStatus();
            var result = nce.call("ncubeController.saveJson", [nce.getAppId(), nce.getSelectedCubeName(), _editor.getText()]);
            if (result.status !== true)
            {
                nce.showNote('Error saving JSON n-cube:<hr class="hr-small"/>' + result.data);
            }
        });
    };

    var isDirty = function()
    {
        if (_editor.editor)
        {
            return !_editor.editor.getSession().getUndoManager().isClean();
        }
        return false;
    };

    var clearDirtyStatus = function()
    {
        if (_editor.editor)
        {
            var undoMgr = _editor.editor.getSession().getUndoManager();
            undoMgr.reset();
            undoMgr.markClean();
        }
    };

    var updateDirtyStatus = function()
    {
        var saveButton = $('#saveButton');
        if (saveButton.length == 0)
        {
            return;
        }
        var dirty = isDirty();
        saveButton.prop('disabled', !dirty);
        var text = 'Save';
        if (dirty)
        {
            text += '*';
        }
        saveButton.html(text);
    };

    var load = function()
    {
        if (!nce.getSelectedCubeName() || !nce.getSelectedApp() || !nce.getSelectedVersion() || !nce.getSelectedStatus())
        {
            _editor.setText('No n-cube to load');
            clearDirtyStatus();
            updateDirtyStatus();
            return;
        }
        var result = nce.call("ncubeController.getJson", [nce.getAppId(), nce.getSelectedCubeName(), {mode:"json"}], {noResolveRefs:true});
        if (result.status === true)
        {
            try {
                _editor.setText(result.data);
                _editor.format();
            }
            catch (e)
            {
                nce.showNote('JSON content too large for the JSON Text Editor.  Capture the JSON from the "Revision History" option.');
                console.log(e);
            }
        }
        else
        {
            _editor.setText("Unable to load '" + nce.getSelectedCubeName() + "'. " + result.data);
        }

        clearDirtyStatus();
        updateDirtyStatus();
    };

    var handleCubeSelected = function()
    {
        load();
    };

    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load
    };

})(jQuery);

var tabActivated = function tabActivated(info)
{
    NCubeJsonEditor.init(info);
    NCubeJsonEditor.load();
};

var cubeSelected = function cubeSelected()
{
    NCubeJsonEditor.handleCubeSelected();
};
