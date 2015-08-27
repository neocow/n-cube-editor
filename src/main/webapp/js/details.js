/**
 * Details display / Default cube-level cell editor
 *
 * @author John DeRegnaucourt
 */

$(function ()
{
    $.info = {};
    var _urlDropdown = $('#datatypes-url');
    var _valueDropdown = $('#datatypes-value');
    var _isUrl = $('#isURL');
    var _isCached = $('#isCached');
    var _defCellValue = $('#cube_defValue');

    _isUrl.change(function()
    {
        var isUrl = isUrlChecked();
        _urlDropdown.toggle(isUrl);
        _valueDropdown.toggle(!isUrl);
        enableDisableCheckboxes();
    });

    _urlDropdown.change(function()
    {
        enableDisableCheckboxes();
    });

    _valueDropdown.change(function()
    {
        enableDisableCheckboxes();
    });

    $('#defaultCellClear').click(function()
    {
        var result = nce().call("ncubeController.clearDefaultCell", [nce().getAppId(), nce().getSelectedCubeName()]);
        if (result.status === true)
        {
            $('#cube_defValue').val('');
            _isUrl.find('input').attr('checked', false);
            _isCached.find('input').attr('checked', false);
            _urlDropdown.toggle(false);
            _valueDropdown.toggle(true);
            nce().showNote('Default cell cleared.');
        }
        else
        {
            nce().showNote('Unable to clear default cell:<hr class="hr-small"/>' + result.data);
        }
    });

    $('#defaultCellUpdate').click(function()
    {
        var cellInfo = {'@type':'com.cedarsoftware.ncube.CellInfo'};
        cellInfo.isUrl = _isUrl.find('input').is(':checked');
        cellInfo.value = _defCellValue.val();
        cellInfo.dataType = cellInfo.isUrl ? _urlDropdown.val() : _valueDropdown.val();
        cellInfo.isCached = _isCached.find('input').is(':checked');

        var result = nce().call("ncubeController.updateDefaultCell", [nce().getAppId(), nce().getSelectedCubeName(), cellInfo]);

        if (result.status === true)
        {
            nce().showNote('Default cell updated successfully.');
        }
        else
        {
            nce().showNote('Unable to update default cell:<hr class="hr-small"/>' + result.data);
        }
    });

    function isUrlChecked()
    {
        return _isUrl.find('input').is(':checked');
    }

    function enableDisableCheckboxes()
    {
        var selDataType = isUrlChecked() ? _urlDropdown.val() : _valueDropdown.val();
        var urlEnabled = selDataType == 'string' || selDataType == 'binary' || selDataType == 'exp' || selDataType == 'method' || selDataType == 'template';
        var cacheEnabled = selDataType == 'string' || selDataType == 'binary' || selDataType == 'exp' || selDataType == 'method' || selDataType == 'template';

        // Enable / Disable [x] URL
        _isUrl.find('input').prop("disabled", !urlEnabled);

        if (urlEnabled)
        {
            _isUrl.removeClass('disabled');
        }
        else
        {
            _isUrl.addClass('disabled');
        }

        // Enable / Disable [x] Cache
        _isCached.find('input').prop("disabled", !cacheEnabled);

        if (cacheEnabled)
        {
            _isCached.removeClass('disabled');
        }
        else
        {
            _isCached.addClass('disabled');
        }
    }

    $.loadDetails = function()
    {
        if (!nce().getCubeMap() || !nce().doesCubeExist())
        {
            return;
        }

        var info = nce().getCubeMap()[(nce().getSelectedCubeName() + '').toLowerCase()];
        if (!info)
        {
            return;
        }

        $('#cube_name').val(info.name);
        $('#cube_revision').val(info.revision);
        var date = '';
        if (info.createDate != undefined)
        {
            date = new Date(info.createDate).format('yyyy-mm-dd HH:MM:ss');
        }
        $('#cube_createDate').val(date);
        $('#cube_createHid').val(info.createHid);
        $('#cube_notes').val(info.notes);
        $('#cube_id').val(info.id);
        $('#cube_sha1').val(info.sha1);
        $('#cube_headSha1').val(info.headSha1);

        var result = nce().call("ncubeController.getDefaultCell", [nce().getAppId(), nce().getSelectedCubeName()]);

        if (result.status === false)
        {
            nce().showNote('Unable to fetch default cell information: ' + result.data);
            return;
        }

        var cellInfo = result.data;

        _defCellValue.val(cellInfo.value ? cellInfo.value : "");
        if (cellInfo.dataType == "null" || !cellInfo.dataType)
        {
            cellInfo.dataType = "string";
        }

        // Set the correct entry in the drop-down
        if (cellInfo.isUrl)
        {
            _urlDropdown.val(cellInfo.dataType);
        }
        else
        {
            _valueDropdown.val(cellInfo.dataType);
        }

        // Choose the correct data type drop-down (show/hide the other)
        _urlDropdown.toggle(cellInfo.isUrl);
        _valueDropdown.toggle(!cellInfo.isUrl);

        // Set the URL check box
        _isUrl.find('input').prop('checked', cellInfo.isUrl);

        // Set the Cache check box state
        _isCached.find('input').prop('checked', cellInfo.isCached);
    };
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
        $.loadDetails();
    }
    catch (e)
    {
        console.log(e);
    }
}
