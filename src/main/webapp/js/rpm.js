/**
 * RPM Editor
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

var RpmEditor = (function($)
{
    var hot = null;
    var nce = null;

    var init = function(info)
    {
        if (!nce)
        {
            nce = info.fn;

            $(window).resize(function ()
            {
                RpmEditor.render();
            });

            buildTraitButtons();
        }
    };

    var buildTraitButtons = function()
    {
        var div = $('#traitFilter');
        div.empty();
        var result = nce.exec('RpmController.getTraitCategories', [nce.getAppId(), {}]);
        if (result.status === false)
        {
            showRpmError('Unable to fetch rpm.meta.traits n-cube.');
            return;
        }
        var traitList = result.data.return['@items'];
        var i = 0;
        $.each(traitList, function(index, item)
        {
            console.log(item);
            var label = $('<label/>').attr({class:'btn'});
            if (i == 0)
            {
                label.addClass('active');
            }
            label.attr({style:'border-color:#999;color:' + item.metaProps['css:color'] + ';background-color:' + item.metaProps['css:background-color'] + ' !important'});
            var input = $('<input>').attr({type:'checkbox', autocomplete:'off'});
            i++;
            label.append(input);
            var btnText = item.metaProps['shortDesc'] || item.value;
            label.append(' ' + btnText);
            div.append(label);
        });

        $('#selectAll').click(function(e)
        {
            e.preventDefault();
            console.log('all');
        });

        $('#selectNone').click(function(e)
        {
            e.preventDefault();
            console.log('none');
        });
    };

    var showRpmError = function(msg)
    {
        $('#rpmErrorPanel').removeAttr('hidden');
        $('#viewRpmInfo').attr('hidden', true);
        $('#rpmErrorMsg').html(msg);
    };

    var clearRpmError = function()
    {
        $('#rpmErrorPanel').attr('hidden', true);
        $('#viewRpmInfo').removeAttr('hidden');
    };

    var load = function()
    {
        if (!nce.getCubeMap() || !nce.doesCubeExist())
        {
            showRpmError('No cubes available.');
            return;
        }

        var info = nce.getCubeMap()[(nce.getSelectedCubeName() + '').toLowerCase()];
        if (!info)
        {
            showRpmError('No cube selected.');
            return;
        }

        var loName = nce.getSelectedCubeName().toLowerCase();

        if (loName.indexOf('rpm.class') != 0 && loName.indexOf('rpm.enum') != 0)
        {
            showRpmError('<b>' + info.name + '</b> is not an RPM Class.');
            return;
        }

        clearRpmError();

        //var input = {'method':'getTraits'};
        //var result = nce.exec('RpmController.getTraits', [nce.getAppId(), input]);
        //console.log(result);
        // 1. Call server to fetch Rpm Class data in JSON format.
        // 2. Build out table
        // 3. Plop in links
        var colHeaders = ['a', 'b', 'c'];
        var fieldNames = [1, 2, 3];
        var container = document.getElementById('rpmTable'),
            settings = {
                //data: data1,
                colHeaders: colHeaders,
                rowHeaders: fieldNames,
                //contextMenu: true,
                manualColumnResize: true,
                height:500
            };

        hot = new Handsontable(container, settings);
        hot.render();
    };

    var render = function()
    {
        if (!hot)
        {
            return;
        }
        hot.render();
    };

    var handleCubeSelected = function()
    {
        load();
    };

    return {
        init: init,
        load: load,
        render: render,
        handleCubeSelected: handleCubeSelected
    };

})(jQuery);


// Event handlers for events from NCE Frame
var tabActivated = function tabActivated(info)
{
    RpmEditor.init(info);
    RpmEditor.load();
};

var cubeSelected = function cubeSelected()
{
    RpmEditor.handleCubeSelected();
};