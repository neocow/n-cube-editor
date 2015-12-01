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
    var traits = null;
    var rowNums = null;
    var data = null;

    var init = function(info) {
        if (!nce) {
            nce = info;

            $(window).resize(function () {
                NCubeEditor2.render();
            });

        }
    };

    var load = function() {
        if (hot) {
            hot.destroy();
            hot = null;
        }

        if (!nce.getCubeMap() || !nce.doesCubeExist()) {
            $('#ncube-content')[0].innerHTML = '';
            return;
        }

        var info = nce.getCubeMap()[(nce.getSelectedCubeName() + '').toLowerCase()];
        if (!info) {
            $('#ncube-content')[0].innerHTML = '';
            return;
        }

        var result = nce.call("ncubeController.getJson", [nce.getAppId(), nce.getSelectedCubeName()], {noResolveRefs:true});
        if (result.status === false) {
            return;
        }

        handleCubeData(JSON.parse(result.data));

        hot = new Handsontable(document.getElementById('ncube-content'), getHotSettings());
        hot.render();
    };

    var handleCubeData = function(cubeData) {
        var handleCellData = function(cells) {
            for (var i = 0; i < cells.length; i++) {
                var cell = cells[i];
                var cellLocationInfo = getLogicalNamesFromCoordinates(cell);

                var dataRow = findRowFromHeaders(cellLocationInfo.row);
                if (!dataRow) {
                    dataRow = initRow(cellLocationInfo.row);
                    data.push(dataRow);
                }

                dataRow[cellLocationInfo.trait] = cell.value;
            }
        };

        var findRowFromHeaders = function(rowHeader) {
            for (var r = 0; r < data.length; r++) {
                if (data[r].rowId === rowHeader) {
                    return data[r];
                }
            }
            return null;
        };

        var getLogicalNamesFromCoordinates = function(cell) {
            var rowHeader;
            var traitHeader;
            for (var i = 0; i < cell.id.length; i++) {
                var curId = cell.id[i];
                if (traitMap.hasOwnProperty(curId)) {
                    traitHeader = traitMap[curId];
                } else if (rowMap.hasOwnProperty(curId)) {
                    rowHeader = rowMap[curId];
                }
            }

            return {row:rowHeader, trait:traitHeader};
        };

        // creates a new row with blank columns
        var initRow = function(rowId) {
            var row = {rowId:rowId};
            for (var i = 0; i < traits.length; i++) {
                row[traits[i]] = '';
            }
            return row;
        };

        var cubeName = cubeData.ncube;
        var axes = cubeData.axes;
        var numAxes = axes.length - 1;

        traits = [];
        fieldNames = [];
        rowNums = [];
        data = [];

        var traitMap = {};
        var rowMap = {};
        var row = 0;

        for (var i = 0; i < axes.length; i++) {
            var axis = axes[i];
            if (axis.name.toLowerCase() === 'trait') {
                var traitCols = axis.columns;
                for (var x = 0; x < traitCols.length; x++) {
                    var trait = traitCols[x];
                    traits.push(trait.value);
                    traitMap[trait.id] = trait.value;
                }
            } else {
                // TODO - change this for n-dimensional cubes
                traits.unshift(''); // blank for row header(s)
                var columns = axis.columns;
                for (var x = 0; x < columns.length; x++) {
                    var column = columns[x];
                    fieldNames.push(column.value);
                    rowNums.push(++row);
                    rowMap[column.id] = column.value;
                }
            }
        }

        handleCellData(cubeData.cells);
    };

    var getHotSettings = function() {
        return {
            data: data,
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
            height:500,
            cells: function (row, col, prop) {
                return {renderer:categoryRenderer};
            }
        };
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