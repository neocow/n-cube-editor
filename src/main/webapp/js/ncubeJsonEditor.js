/**
 * JSON Editor
 *     Edit n-cubes in JSON format
 *
 * @author John DeRegnaucourt
 */

$(function ()
{
    $.info = {};
    var _editor;

    initJsonEditor();

    function initJsonEditor()
    {
        var container = document.getElementById('jsoneditor');
        var options =
        {
            mode: 'code',
            change: function ()
            {
                updateDirtyStatus();
            }
        };

        // Create JSON Editor (http://jsoneditoronline.org/downloads/)
        _editor = new JSONEditor(container, options);
        addSaveButton();
    }

    function addSaveButton()
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
            nce().clearError();
            if (nce().isHeadSelected())
            {
                nce().selectBranch();
                return;
            }

            clearDirtyStatus();
            updateDirtyStatus();
            var result = nce().call("ncubeController.saveJson", [nce().getAppId(), nce().getSelectedCubeName(), _editor.getText()]);
            if (result.status !== true)
            {
                nce().showNote('Error saving JSON n-cube:<hr class="hr-small"/>' + result.data);
            }
        });
    }

    function isDirty()
    {
        if (_editor.editor)
        {
            return !_editor.editor.getSession().getUndoManager().isClean();
        }
        return false;
    }

    function clearDirtyStatus()
    {
        if (_editor.editor)
        {
            var undoMgr = _editor.editor.getSession().getUndoManager();
            undoMgr.reset();
            undoMgr.markClean();
        }
    }

    function updateDirtyStatus()
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
    }

    $.loadCubeJson = function()
    {
        if (!nce().getSelectedCubeName() || !nce().getSelectedApp() || !nce().getSelectedVersion() || !nce().getSelectedStatus())
        {
            _editor.setText('No n-cube to load');
            clearDirtyStatus();
            updateDirtyStatus();
            return;
        }
        var result = nce().call("ncubeController.getJson", [nce().getAppId(), nce().getSelectedCubeName()]);
        if (result.status === true)
        {
            _editor.setText(result.data);
            _editor.format();
        }
        else
        {
            _editor.setText("Unable to load '" + nce().getSelectedCubeName() + "'. " + result.data);
        }

        clearDirtyStatus();
        updateDirtyStatus();
    }
});

function nce()
{
    return $.info.fn;
}

function tabActivated(info)
{
    try
    {
        $.info = info;
        $.loadCubeJson();
    }
    catch (e)
    {
        console.log(e);
    }
}
