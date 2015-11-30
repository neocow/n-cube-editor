/**
 * Main NCube editor
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

var NCubeEditor2 = (function ($)
{
    var nce = null;
    var hot = null;
    var fieldNames = null;

    var init = function(info)
    {
        if (!nce)
        {
            nce = info;

            $(window).resize(function ()
            {
                NCubeEditor2.render();
            });

        }
    };

    var load = function()
    {
        if (hot)
        {
            hot.destroy();
            hot = null;
        }

        if (!nce.getCubeMap() || !nce.doesCubeExist())
        {
            $('#ncube-content')[0].innerHTML = '';
            return;
        }

        var info = nce.getCubeMap()[(nce.getSelectedCubeName() + '').toLowerCase()];
        if (!info)
        {
            $('#ncube-content')[0].innerHTML = '';
            return;
        }

        var result = nce.call("ncubeController2.getJson", [nce.getAppId(), nce.getSelectedCubeName()], {noResolveRefs:true});
        if (result.status === false) {
            return;
        }
        var cubeData = JSON.parse(result.data);
        var cubeName = cubeData.ncube;
        var axes = cubeData.axes;

        var traits = [];
        fieldNames = [];
        var rowNums = [];
        var row = 0;

        // TODO - remove this for n-dimensional cubes
        var columns = axes[0].columns;
        for (var i = 0; i < columns.length; i++) {
            fieldNames.push(columns[i].value);
            rowNums.push(++row);
        }

        for (var i = 0; i < axes.length; i++) {
            var axis = axes[i];
            if (axis.name.toLowerCase() === 'trait') {
                var traitCols = axis.columns;
                for (var x = 0; x < traitCols.length; x++) {
                    traits.push(traitCols[x].value);
                }
            } else {
                //TODO -- handle n-dimensional cubes here
            }
        }

        var container = document.getElementById('ncube-content'),
            settings = {
                autoColumnSize: true,
                enterBeginsEditing: false,
                enterMoves: {row: 1, col: 0},
                tabMoves : {row: 0, col: 1},
                //colHeaders: traitTable.names,
                colHeaders: traits,
                //rowHeaders: rowNums,
                //startCols: traitTable.names.length,
                startCols: traits.length,
                startRows: rowNums.length,
                contextMenu: true,
                manualColumnResize: true,
                manualRowResize: true,
                fixedColumnsLeft: 1,
                //height:500,
                cells: function (row, col, prop)
                {
                    return {renderer:categoryRenderer};
                }
            };

        hot = new Handsontable(container, settings);
        hot.render();
    };

    var categoryRenderer = function(instance, td, row, col, prop, value, cellProperties)
    {
        Handsontable.renderers.TextRenderer.apply(this, arguments);
        if (col == 0) {
            td.innerHTML = fieldNames[row];
        }

        //$.each(traitTable, function(item)
        //{
        //    if (item.startsWith('css:') && traitTable[item][col])
        //    {
        //        var style = item.replace(/^css:/, '');
        //        td.style[style] = traitTable[item][col];
        //    }
        //});
    };

    // =============================================== End Cell Editing ================================================

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

    // Let parent (main frame) know that the child window has loaded.
    // The loading of all of the Javascript (deeply) is continuous on the main thread.
    // Therefore, the setTimeout(, 1) ensures that the main window (parent frame)
    // is called after all Javascript has been loaded.
    setTimeout(function() { window.parent.frameLoaded(); }, 1);

    // API
    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load,
        render: render
    };

})(jQuery);

var tabActivated = function tabActivated(info)
{
    NCubeEditor2.init(info);
    NCubeEditor2.load();
};

var cubeSelected = function cubeSelected()
{
    NCubeEditor2.handleCubeSelected();
};