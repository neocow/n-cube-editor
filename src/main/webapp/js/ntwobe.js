var NCubeEditor2 = (function ($)
{
    var headerAxisNames = ['trait','traits','businessDivisionCode','bu','month','months','col','column','cols','columns'];
    var nce = null;
    var hot = null;
    var colNums = null;
    var rowNums = null;
    var cubeName = null;
    var axes = null;
    var colOffset = null;
    var data = null;
    var cache = null;

    var init = function(info) {
        if (!nce) {
            nce = info;

            $(window).resize(function () {
                hot.updateSettings({
                    height: $(this).height() - $('#hot-container').offset().top,
                    width: $(this).width()
                });

                NCubeEditor2.render();

                $('#coordinate-bar').width($(this).width());
            });

        }
    };

    var load = function() {
        if (hot) {
            hot.destroy();
            hot = null;
            document.getElementById('coordinate-bar').innerHTML = 'Axis Coordinates: [ ]';
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

        var result = nce.call("ncubeController.getJson", [nce.getAppId(), nce.getSelectedCubeName(), {mode:'json-index'}], {noResolveRefs:true});
        if (result.status === false) {
            return;
        }

        handleCubeData(JSON.parse(result.data));

        hot = new Handsontable(document.getElementById('hot-container'), getHotSettings());
        hot.render();
    };

    var getColumnLength = function(axis) {
        var cachedColumnLength = getCacheValue('columnLength', {axis:axis.name});
        if (cachedColumnLength != null) {
            return cachedColumnLength;
        }

        var colLen = Object.keys(axis.columns).length;
        if (axis.hasDefault) {
            colLen++;
        }
        setCacheValue('columnLength', {axis:axis.name}, colLen);
        return colLen;
    };

    var getAxisColumn = function(axis, colNum) {
        var cachedAxisColumn = getCacheValue('axisColumn', {axis:axis.name, col:colNum});
        if (cachedAxisColumn != null) {
            return cachedAxisColumn;
        }

        var objColumns = axis.columns;
        var key = Object.keys(objColumns)[colNum];
        var obj = JSON.parse(JSON.stringify(objColumns[key]));
        obj.id = key;

        setCacheValue('axisColumn', {axis:axis.name, col:colNum}, obj);
        return obj;
    };

    var getColumnHeader = function(col) {
        var cachedColumnHeader = getCacheValue('columnHeader', {col:col});
        if (cachedColumnHeader != null) {
            return cachedColumnHeader;
        }

        var axis = axes[colOffset];
        var colNum = col - colOffset;
        var column = colNum < Object.keys(axis.columns).length ? getAxisColumn(axis, colNum) : {id:getDefaultId(axis), value:'Default'};

        setCacheValue('columnHeader', {col:col}, column);
        return column;
    };

    var getColumnHeaderValue = function(col) {
        return getColumnHeader(col).value;
    };

    var getColumnHeaderId = function(col) {
        return getColumnHeader(col).id;
    };

    var getDefaultId = function(axis) {
        return Object.keys(data.axes).indexOf(axis.name.toLowerCase()) + 1 + '002147483647';
    };

    var getRowHeader = function(row, col) {
        var cachedRowHeader = getCacheValue('rowHeader', {row:row, col:col});
        if (cachedRowHeader) {
            return cachedRowHeader;
        }

        var result;
        var rowNum = row - 2;
        if (rowNum < 0 || col < 0 || col > axes.length) {
            return;
        }

        var axis = axes[col];
        var colLen = getColumnLength(axis);

        var repeatRowCount = (rowNums.length - 2) / colLen;
        for (var axisNum = 0; axisNum < col; axisNum++) {
            var tempAxis = axes[axisNum];
            var colCount = getColumnLength(tempAxis);
            repeatRowCount /= colCount;
        }

        var columnNumberToGet = Math.floor(rowNum / repeatRowCount) % colLen;
        result = (axis.hasDefault && columnNumberToGet === colLen - 1)
            ? {id:getDefaultId(axis), value:'Default'} : getAxisColumn(axis, columnNumberToGet);

        setCacheValue('rowHeader', {row:row, col:col}, result);
        return result;
    };

    var getRowHeaderValue = function(row, col) {
        return getRowHeader(row, col).value;
    };

    var getRowHeaderId = function(row, col) {
        return getRowHeader(row, col).id;
    };

    var getCacheValue = function(valType, vars) {
        if (!cache.hasOwnProperty(valType)) {
            return;
        }
        var coord;
        switch (valType) {
            case 'rowHeader':
            case 'cellData':
                coord = vars.row + '_' + vars.col;
                break;
            case 'columnLength':
                coord = vars.axis;
                break;
            case 'axisColumn':
                coord = vars.axis + '_' + vars.col;
                break;
            case 'columnHeader':
                coord = vars.col;
                break;
        }

        return cache[valType][coord];
    };

    var setCacheValue = function(valType, vars, value) {
        if (!cache.hasOwnProperty(valType)) {
            cache[valType] = {};
        }
        var coord;
        switch (valType) {
            case 'rowHeader':
            case 'cellData':
                coord = vars.row + '_' + vars.col;
                break;
            case 'columnLength':
                coord = vars.axis;
                break;
            case 'axisColumn':
                coord = vars.axis + '_' + vars.col;
                break;
            case 'columnHeader':
                coord = vars.col;
                break;
        }

        cache[valType][coord] = value;
    };

    var getCellData = function(row, col) {
        var cachedCellData = getCacheValue('cellData', {row:row, col:col});
        if (cachedCellData != null) {
            return cachedCellData;
        }

        var cellId = '';
        var headerInfo = [];

        if (axes.length > 1) {
            for (var i = 0; i < colOffset; i++) {
                headerInfo.push(getRowHeaderId(row, i));
            }
            headerInfo.push(getColumnHeaderId(col));

            headerInfo.sort(function(a, b) {
                return a - b;
            });

            for (var i = 0; i < headerInfo.length; i++) {
                cellId += headerInfo[i] + '_';
            }
            cellId = cellId.slice(0, -1);
        } else {
            cellId = getRowHeaderId(row, 0);
        }
        var cell = data.cells[cellId];

        setCacheValue('cellData', {row:row, col:col}, cell);
        return cell;
    };

    var handleCubeData = function(cubeData) {

        var determineAxesOrder = function (cubeAxes) {
            axes = [];
            var keys = Object.keys(cubeAxes);
            for (var i = 0; i < keys.length; i++) {
                axes.push(cubeAxes[keys[i]]);
            }

            var horizontal;
            axes.sort(function (a, b) {
                return getColumnLength(b) - getColumnLength(a);
            });

            var delta;
            var smallestDelta = Number.MAX_VALUE;
            for (var i = 0; i < axes.length; i++) {
                var axis = axes[i];
                if (headerAxisNames.indexOf(axis.name) > -1) {
                    horizontal = i;
                    break;
                }
                delta = Math.abs(getColumnLength(axis) - 12);
                if (delta < smallestDelta) {
                    smallestDelta = delta;
                    horizontal = i;
                }
            }

            horizontal = axes.splice(horizontal, 1);
            axes.push(horizontal[0]);
        };

        var setUpDataTable = function() {
            var getColumnString = function(index) {
                var dividend = index;
                var modulo;
                var colName = '';

                while (dividend > 0) {
                    modulo = (dividend - 1) % 26;
                    colName = String.fromCharCode(65 + modulo) + colName;
                    dividend = Math.floor((dividend - modulo) / 26);
                }

                return colName;
            };

            cubeName = cubeData.ncube;
            rowNums = ['',''];
            colNums = [''];

            var totalRows = 1;
            var numAxes = axes.length;
            colOffset = axes.length - 1;

            if (numAxes > 1) {
                var horizAxis = axes[colOffset];

                for (var i = 1; i < colOffset; i++) {
                    colNums.push('');
                }

                var colLen = getColumnLength(horizAxis);
                for (var columnIndex = 1; columnIndex < colLen + 1; columnIndex++) {
                    var columnLetterNum = getColumnString(columnIndex);
                    colNums.push(columnLetterNum);
                }

                for (var axisNum = 0; axisNum < colOffset; axisNum++) {
                    var axis = axes[axisNum];
                    var numCols = getColumnLength(axis);
                    totalRows *= numCols;
                }

                for (var rowNum = 1; rowNum <= totalRows; rowNum++) {
                    rowNums.push(rowNum.toString());
                }
            } else {
                var axis = axes[0];
                colNums.push('A');
                var colLen = getColumnLength(axis);

                for (var columnNum = 1; columnNum < colLen + 1; columnNum++) {
                    rowNums.push(columnNum.toString());
                }
            }
        };

        cache = {};
        data = cubeData;
        determineAxesOrder();
        setUpDataTable();
    };

    var getHotSettings = function() {
        return {
            autoColumnSize: true,
            enterBeginsEditing: false,
            enterMoves: {row: 1, col: 0},
            tabMoves : {row: 0, col: 1},
            colHeaders: colNums,
            rowHeaders: rowNums,
            startCols: colNums.length,
            startRows: rowNums.length,
            contextMenu: false,
            manualColumnResize: true,
            manualRowResize: true,
            fixedColumnsLeft: axes.length - 1,
            fixedRowsTop: 2,
            currentRowClassName: 'handsonCurrentRow',
            currentColClassName: 'handsonCurrentRow',
            outsideClickDeselects: false,
            autoColumnSize: true,
            height: function() {
                return $(window).height() - $("#hot-container").offset().top;
            },
            width: function() {
                return $(window).width();
            },
            cells: function (row, col, prop) {
                return {renderer:categoryRenderer};
            },
            afterSelectionEnd: function(r, c, r2, c2) {
                var display = '';
                if (c >= colOffset) {
                    if (r === 1) {
                        display = '<strong>Axis</strong>: ' + axes[colOffset].name + ', <strong>Column</strong>:' + getColumnHeaderValue(c);
                    } else if (r > 1) {
                        display = '<strong>Axis Coordinates</strong>: [ ';
                        for (var axisNum = 0; axisNum < colOffset; axisNum++) {
                            var axisName = axes[axisNum].name;
                            var axisVal = getRowHeaderValue(r, axisNum);
                            display += '<strong>' + axisName + '</strong>: ' + axisVal + ', ';
                        }
                        if (axes.length > 1) {
                            var axisName = axes[colOffset].name;
                            var axisVal = getColumnHeaderValue(c);
                        } else {
                            var axisName = axes[0].name;
                            var axisVal = getRowHeaderValue(r, 0);
                        }
                        display += '<strong>' + axisName + '</strong>: ' + axisVal + ' ]';
                    }
                }
                else if (r > 1) {
                    display = '<strong>Axis</strong>: ' + axes[c].name + ', <strong>Column</strong>:' + getRowHeaderValue(r, c);
                }
                document.getElementById('coordinate-bar').innerHTML = display;
            }
        };
    };

    var categoryRenderer = function(instance, td, row, col, prop, value, cellProperties) {
        Handsontable.renderers.TextRenderer.apply(this, arguments);

        // cube name
        if (row === 0 && (col < colOffset || col === 0)) {
            if (col === 0) {
                td.innerHTML = cubeName;
            }
            td.style.background = '#6495ED';
            td.style.color = 'white';
            td.style.opacity = '1';
            cellProperties.readOnly = true;
            if (col < axes.length - 2) {
                td.style.borderRight = 'none';
            }
        }

        // horizontal axis metadata area
        else if (row === 0) {
            if (axes.length > 1 && col === colOffset) {
                td.innerHTML = axes[colOffset].name;
            }
            td.style.background = '#4D4D4D';
            td.style.color = 'white';
            td.style.opacity = '1';
            cellProperties.readOnly = true;

            // rest of the top row
            if (col < colNums.length - 1) {
                td.style.borderRight = 'none';
            }
        }

        // vertical axes metadata area
        else if (row === 1 && (col < colOffset || col === 0)) {
            td.innerHTML = axes[col].name;
            td.style.background = '#4D4D4D';
            td.style.color = 'white';
            td.style.opacity = '1';
            cellProperties.readOnly = true;
        }

        // column headers
        else if (row === 1) {
            if (axes.length > 1) {
                td.innerHTML = getColumnHeaderValue(col);
            }
            td.className = 'handsonTableHeader';
            td.style.background = '#929292';
            td.style.color = 'white';
        }

        // row headaers
        else if (col === 0 || col < colOffset) {
            var val = getRowHeaderValue(row, col);
            if (row > 2 && val === getRowHeaderValue(row - 1, col) && col != colOffset - 1) {
                td.style.borderTop = 'none';
            } else {
                td.innerHTML = val;
            }
            td.className = 'handsonTableHeader';
            td.style.background = '#929292';
            td.style.color = 'white';
        }

        // otherwise in cell data
        else {
            var cellData = getCellData(row, col);
            if (cellData) {
                td.innerHTML = cellData.value || cellData.url;
            }

            // odd row style
            if (row % 2 != 0) {
                td.style.background = '#e0e0e0';
            }
        }
    };

    // =============================================== End Cell Editing ================================================

    var render = function() {
        if (!hot) {
            return;
        }
        hot.render();
    };

    var handleCubeSelected = function() {
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

var tabActivated = function tabActivated(info) {
    NCubeEditor2.init(info);
    NCubeEditor2.load();
};

var cubeSelected = function cubeSelected() {
    NCubeEditor2.handleCubeSelected();
};