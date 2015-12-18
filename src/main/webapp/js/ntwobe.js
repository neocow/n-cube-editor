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
    var _cellId = null;
    var _tableCellId = null;
    var _columnList = null;
    var _editCellModal = null;
    var _editCellValue = null;
    var _editCellCache = null;
    var _editCellRadioURL = null;
    var _valueDropdown = null;
    var _urlDropdown = null;
    var _clipboard = null;
    var _clipFormat = CLIP_NCE;

    var init = function(info) {
        if (!nce) {
            nce = info;

            _columnList = $('#editColumnsList');
            _editCellModal = $('#editCellModal');
            _editCellValue = $('#editCellValue');
            _editCellCache = $('#editCellCache');
            _editCellRadioURL = $('#editCellRadioURL');
            _valueDropdown = $('#datatypes-value');
            _urlDropdown = $('#datatypes-url');
            _clipboard = $('#cell-clipboard');

            addEditCellListeners();

            _editCellRadioURL.change(function() {
                var isUrl = _editCellRadioURL.find('input').is(':checked');
                _urlDropdown.toggle(isUrl);
                _valueDropdown.toggle(!isUrl);
            });

            _urlDropdown.change(function() {
                enabledDisableCheckBoxes()
            });

            _valueDropdown.change(function() {
                enabledDisableCheckBoxes()
            });
            $('#addAxisOk').click(function () {
                addAxisOk()
            });
            $('#deleteAxisOk').click(function () {
                deleteAxisOk()
            });
            $('#updateAxisMenu').click(function () {
                updateAxis()
            });
            $('#updateAxisOk').click(function () {
                updateAxisOk()
            });
            _editCellModal.on('shown.bs.modal', function () {
                $('#editCellValue').focus();
            });

            $(window).resize(function () {
                if (hot) {
                    hot.updateSettings({
                        height: $(this).height() - $('#hot-container').offset().top,
                        width: $(this).width()
                    });
                }

                NCubeEditor2.render();

                $('#coordinate-bar').width($(this).width());
            });

        }

        setCoordinateBarListeners();
    };

    var load = function() {
        resetCoordinateBar();
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

        var result = nce.call("ncubeController.getJson", [nce.getAppId(), nce.getSelectedCubeName(), {mode:'json-index'}], {noResolveRefs:true});
        if (result.status === false) {
            return;
        }

        handleCubeData(JSON.parse(result.data));

        hot = new Handsontable(document.getElementById('hot-container'), getHotSettings());
        hot.render();
    };

    var getColumnLength = function(axis) {
        if (axis.hasOwnProperty('columnLength')) {
            return axis.columnLength;
        }

        var colLen = Object.keys(axis.columns).length;
        if (axis.hasDefault) {
            colLen++;
        }
        axis.columnLength = colLen;
        return colLen;
    };

    var copyColumn = function(column) {
        var newCol = {};
        var keys = Object.keys(column);
        for (var i = 0, len = keys.length; i < len; i++) {
            var key = keys[i];
            newCol[key] = column[key];
        }
        return newCol;
    };

    var getAxisColumn = function(axis, colNum) {
        var objColumns = axis.columns;
        var keys = Object.keys(objColumns);
        var obj;

        if (colNum >= keys.length) {
            obj = getAxisDefault(axis);
        } else {
            var key = keys[colNum];
            var obj = copyColumn(objColumns[key]);
            obj.id = key;
        }

        return obj;
    };

    var getColumnHeader = function(col) {
        return getAxisColumn(axes[colOffset], col - colOffset);
    };

    var getColumnHeaderValue = function(col) {
        return getColumnHeader(col).value;
    };

    var getColumnHeaderId = function(col) {
        return getColumnHeader(col).id;
    };

    var getAxisDefault = function(axis) {
        var defaultId = Object.keys(data.axes).indexOf(axis.name.toLowerCase()) + 1 + AXIS_DEFAULT;
        return {id:defaultId, value:DEFAULT_TEXT};
    };

    var getRowHeader = function(row, col) {
        var cachedRowHeader = getCacheValue(CACHE_ROW_HEADER, {row:row, col:col});
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
            ? getAxisDefault(axis) : getAxisColumn(axis, columnNumberToGet);

        setCacheValue(CACHE_ROW_HEADER, {row:row, col:col}, result);
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
            case CACHE_ROW_HEADER:
                coord = vars.row + '_' + vars.col;
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
            case CACHE_ROW_HEADER:
                coord = vars.row + '_' + vars.col;
                break;
        }

        cache[valType][coord] = value;
    };

    var getCellId = function(row, col) {
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

            for (var i = 0, len = headerInfo.length; i < len; i++) {
                cellId += headerInfo[i] + '_';
            }
            cellId = cellId.slice(0, -1);
        } else {
            cellId = getRowHeaderId(row, 0);
        }

        return cellId;
    };

    var getCellData = function(row, col) {
        return data.cells[getCellId(row, col)];
    };

    var handleCubeData = function(cubeData) {

        var determineAxesOrder = function (cubeAxes) {
            axes = [];
            var keys = Object.keys(cubeAxes);
            for (var i = 0, len = keys.length; i < len; i++) {
                axes.push(cubeAxes[keys[i]]);
            }

            var horizontal;
            axes.sort(function (a, b) {
                return getColumnLength(b) - getColumnLength(a);
            });

            var delta;
            var smallestDelta = Number.MAX_VALUE;
            for (var i = 0, len = axes.length; i < len; i++) {
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
                for (var columnIndex = 1; columnIndex <= colLen; columnIndex++) {
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

                for (var columnNum = 1; columnNum <= colLen; columnNum++) {
                    rowNums.push(columnNum.toString());
                }
            }
        };

        cache = {};
        data = cubeData;
        determineAxesOrder(data.axes);
        setUpDataTable();
    };

    var getHotSettings = function() {
        return {
            autoColumnSize: true,
            autoRowSize: false,
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
            currentRowClassName: CLASS_HANDSON_CURRENT_ROW,
            currentColClassName: CLASS_HANDSON_CURRENT_ROW,
            outsideClickDeselects: false,
            height: function() {
                return $(window).height() - $("#hot-container").offset().top;
            },
            width: function() {
                return $(window).width();
            },
            cells: function (row, col, prop) {
                return {renderer:categoryRenderer};
            },
            afterSelection: function(r, c, r2, c2) {
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

                resetCoordinateBar(display);
            }
        };
    };

    var categoryRenderer = function(instance, td, row, col, prop, value, cellProperties) {
        Handsontable.renderers.TextRenderer.apply(this, arguments);

        // cube name
        if (row === 0 && (col < colOffset || col === 0)) {
            if (col === 0) {
                td.innerHTML = cubeName;
                td.style.overflow = 'visible';
            }
            td.style.background = BACKGROUND_CUBE_NAME;
            td.style.color = COLOR_WHITE;
            td.style.opacity = '1';
            cellProperties.readOnly = true;
            if (col < axes.length - 2) {
                td.style.borderRight = NONE;
            }
        }

        // horizontal axis metadata area
        else if (row === 0) {
            if (axes.length > 1 && col === colOffset) {
                td.innerHTML = axes[colOffset].name;
                td.style.overflow = 'visible';
            }
            td.style.background = BACKGROUND_AXIS_INFO;
            td.style.color = COLOR_WHITE;
            td.style.opacity = '1';
            cellProperties.readOnly = true;

            // rest of the top row
            if (col < colNums.length - 1) {
                td.style.borderRight = NONE;
            }
        }

        // vertical axes metadata area
        else if (row === 1 && (col < colOffset || col === 0)) {
            td.innerHTML = axes[col].name;
            td.style.background = BACKGROUND_AXIS_INFO;
            td.style.color = COLOR_WHITE;
            td.style.opacity = '1';
            cellProperties.readOnly = true;
        }

        // column headers
        else if (row === 1) {
            if (axes.length > 1) {
                td.innerHTML = getColumnHeaderValue(col);
            }
            td.className = CLASS_HANDSON_TABLE_HEADER;
            td.style.background = BACKGROUND_COLUMN_HEADER;
            td.style.color = COLOR_WHITE;
        }

        // row headaers
        else if (col === 0 || col < colOffset) {
            var val = getRowHeaderValue(row, col);
            if (row > 2 && getColumnLength(axes[col]) > 1 && val === getRowHeaderValue(row - 1, col)) {
                td.style.borderTop = NONE;
            } else {
                td.innerHTML = val;
            }
            td.className = CLASS_HANDSON_TABLE_HEADER;
            td.style.background = BACKGROUND_COLUMN_HEADER;
            td.style.color = COLOR_WHITE;
        }

        // otherwise in cell data
        else {
            var cellData = getCellData(row, col);
            if (cellData) {
                td.innerHTML = cellData.value == null ? cellData.url : cellData.value;
            } else if (data.defaultCellValue) {
                td.innerHTML = data.defaultCellValue;
                td.className = 'tableDefault';
            }

            cellProperties.editor = CellEditor;

            // odd row style
            if (row % 2 != 0) {
                td.style.background = BACKGROUND_ODD_ROW;
            }
        }
    };

    var getDomCoordinateBar = function() {
        return document.getElementById('coordinate-bar');
    };

    var curDown = false;
    var curPos = 0;
    var resetCoordinateBar = function(displayText) {
        var bar = getDomCoordinateBar();
        curDown = false;
        curPos = 0;
        bar.scrollLeft = 0;
        bar.innerHTML = displayText || '<strong>Axis Coordinates</strong>: [ ]';
    };

    var setCoordinateBarListeners = function() {
        var coordBar = getDomCoordinateBar();

        coordBar.addEventListener('mousedown', function (e) {
            curDown = true;
            curPos = e.pageX;
        });

        coordBar.addEventListener('mouseup', function(e) {
            curDown = false;
        });

        coordBar.addEventListener('mousemove', function(e) {
            if (curDown) {
                coordBar.scrollLeft = coordBar.scrollLeft + curPos - e.pageX;
            }
        });

        coordBar.addEventListener('mouseout', function(e) {
            curDown = false;
        });
    };

    // ==================================== Everything to do with Cell Editing =========================================

    var onBeforeKeyDown = function(event) {
        event.isImmediatePropagationEnabled = false;
        event.cancelBubble = true;
    };

    var CellEditor = Handsontable.editors.TextEditor.prototype.extend();
    CellEditor.prototype.prepare = function(row, col, prop, td, originalValue, cellProperties){
        Handsontable.editors.BaseEditor.prototype.prepare.apply(this, arguments);
    };
    CellEditor.prototype.open = function() {
        this.instance.addHook('beforeKeyDown', onBeforeKeyDown);

        _tableCellId = getCellId(this.row, this.col);
        _cellId = _tableCellId.split('_');
        editCell();
    };
    CellEditor.prototype.isOpened = function() {
        return $(_editCellModal).hasClass('in');
    };
    CellEditor.prototype.finishEditing = function(restoreOriginalValue, ctrlDown, callback, forceClose) {
        if (!this.isOpened()) {
            if (forceClose) {
                this.state = Handsontable.EditorState.EDITING; // needed to override finish editing
            }
            Handsontable.editors.BaseEditor.prototype.finishEditing.apply(this, arguments);
        }
    };
    CellEditor.prototype.close = function() {
        this.instance.removeHook('beforeKeyDown', onBeforeKeyDown);
    };

    var destroyEditor = function() {
        hot.getActiveEditor().finishEditing(null, null, null, true);
    };

    var addEditCellListeners = function() {
        $('#editCellClear').click(function() {
            editCellClear();
        });
        $('#editCellCancel').click(function() {
            editCellCancel();
        });
        $('#editCellOk').click(function() {
            editCellOK();
        });
    };

    var editCell = function() {
        if (!nce.ensureModifiable('Cell cannot be updated.')) {
            return;
        }

        var result = nce.call("ncubeController.getCellNoExecute", [nce.getAppId(), nce.getSelectedCubeName(), _cellId]);

        if (result.status === false) {
            nce.showNote('Unable to fetch the cell contents: ' + result.data);
            return;
        }

        var cellInfo = result.data;
        // Set the cell value (String)
        _editCellValue.val(cellInfo.value ? cellInfo.value : "");
        if (cellInfo.dataType == "null" || !cellInfo.dataType) {
            cellInfo.dataType = "string";
        }

        // Set the correct entry in the drop-down
        if (cellInfo.isUrl) {
            _urlDropdown.val(cellInfo.dataType);
        } else {
            _valueDropdown.val(cellInfo.dataType);
        }

        // Choose the correct data type drop-down (show/hide the other)
        _urlDropdown.toggle(cellInfo.isUrl);
        _valueDropdown.toggle(!cellInfo.isUrl);

        // Set the URL check box
        _editCellRadioURL.find('input').prop('checked', cellInfo.isUrl);

        // Set the Cache check box state
        _editCellCache.find('input').prop('checked', cellInfo.isCached);

        enabledDisableCheckBoxes(); // reset for form
        _editCellModal.modal('show');
    };

    var editCellClear = function() {
        _editCellModal.modal('hide');
        var result = nce.call("ncubeController.updateCell", [nce.getAppId(), nce.getSelectedCubeName(), _cellId, null]);

        if (result.status === false) {
            _cellId = null;
            nce.showNote('Unable to clear cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        delete data.cells[_tableCellId];
        _cellId = null;
        destroyEditor();
    };

    var editCellCancel = function() {
        _cellId = null;
        _editCellModal.modal('hide');
        destroyEditor();
    };

    var editCellOK = function() {
        var cellInfo = {'@type':'com.cedarsoftware.ncube.CellInfo'};
        cellInfo.isUrl = _editCellRadioURL.find('input').prop('checked');
        cellInfo.value = _editCellValue.val();
        cellInfo.dataType = cellInfo.isUrl ? _urlDropdown.val() : _valueDropdown.val();
        cellInfo.isCached = _editCellCache.find('input').prop('checked');
        _editCellModal.modal('hide');

        var result = nce.call("ncubeController.updateCell", [nce.getAppId(), nce.getSelectedCubeName(), _cellId, cellInfo]);

        if (result.status === false) {
            _cellId = null;
            nce.showNote('Unable to update cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        data.cells[_tableCellId] = {value:cellInfo.value};
        _cellId = null;
        destroyEditor();
    };

    var enabledDisableCheckBoxes = function() {
        var isUrl = _editCellRadioURL.find('input').is(':checked');
        var selDataType = isUrl ? _urlDropdown.val() : _valueDropdown.val();
        var urlEnabled = URL_ENABLED_LIST.indexOf(selDataType) > -1;
        var cacheEnabled = CACHE_ENABLED_LIST.indexOf(selDataType) > -1;

        // Enable / Disable [x] URL
        _editCellRadioURL.find('input').prop("disabled", !urlEnabled);

        if (urlEnabled) {
            _editCellRadioURL.removeClass('disabled');
        } else {
            _editCellRadioURL.addClass('disabled');
        }

        // Enable / Disable [x] Cache
        _editCellCache.find('input').prop("disabled", !cacheEnabled);

        if (cacheEnabled) {
            _editCellCache.removeClass('disabled');
        } else {
            _editCellCache.addClass('disabled');
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