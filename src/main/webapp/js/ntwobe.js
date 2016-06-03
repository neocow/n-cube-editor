var NCubeEditor2 = (function ($) {

    var headerAxisNames = ['trait','traits','businessDivisionCode','bu','month','months','col','column','cols','columns'];
    var nce = null;
    var hot = null;
    var numColumns = 0;
    var numRows = 0;
    var cubeName = null;
    var axes = null;
    var _axisIdsInOrder = null;
    var colOffset = null;
    var data = null;
    var prefixes = null;
    var cubeMap = null;
    var cubeMapRegex = null;
    var axisColumnMap = null;
    var _columnWidths = [];
    var _rowHeights = [];
    var _cellId = null;
    var _tableCellId = null;
    var _columnList = null;
    var _hideColumnsList = null;
    var _hiddenColumns = {};
    var _editCellModal = null;
    var _editCellValue = null;
    var _editCellCache = null;
    var _editCellCancel = null;
    var _editCellClear = null;
    var _editCellRadioURL = null;
    var _editColumnModal = null;
    var _valueDropdown = null;
    var _urlDropdown = null;
    var _colIds = -1;   // Negative and gets smaller (to differentiate on server side what is new)
    var _clipboard = null;
    var _editColClipboard = null;
    var _clipFormat = CLIP_NCE;
    var _searchField = null;
    var _searchInfo = null;
    var _searchCoords = null;
    var _currentSearchResultIndex = null;
    var _ncubeContent = null;
    var _ncubeHtmlError = null;
    var _bufferText = null;
    var _firstRenderedCol = null;
    var _defaultCellText = null;
    var _axisName = null;
    var _isRefAxis = null;
    var _hasRefFilter = null;
    var _addAxisName = null;
    var _addAxisTypeName = null;
    var _addAxisValueTypeName = null;
    var _refAxisGroup = null;
    var _refAxisBranch = null;
    var _refAxisStatus = null;
    var _refAxisApp = null;
    var _refAxisVersion = null;
    var _refAxisCube = null;
    var _refAxisAxis = null;
    var _refFilterGroup = null;
    var _refFilterBranch = null;
    var _refFilterStatus = null;
    var _refFilterApp = null;
    var _refFilterVersion = null;
    var _refFilterCube = null;
    var _refFilterMethod = null;
    var _isRefAxisUpdate = null;
    var _hasRefFilterUpdate = null;
    var _updateAxisModal = null;
    var _updateAxisName = null;
    var _updateAxisTypeName = null;
    var _updateAxisValueTypeName = null;
    var _updateAxisSortOrder = null;
    var _refAxisGroupUpdate = null;
    var _refAxisBranchUpdate = null;
    var _refAxisStatusUpdate = null;
    var _refAxisAppUpdate = null;
    var _refAxisVersionUpdate = null;
    var _refAxisCubeUpdate = null;
    var _refAxisAxisUpdate = null;
    var _refFilterGroupUpdate = null;
    var _refFilterBranchUpdate = null;
    var _refFilterStatusUpdate = null;
    var _refFilterAppUpdate = null;
    var _refFilterVersionUpdate = null;
    var _refFilterCubeUpdate = null;
    var _refFilterMethodUpdate = null;
    var _topAxisBtn = null;
    var _filterModal = null;
    var _filters = null;
    var _filterTable = null;
    var _columnIdCombinationsToShow = null;
    var _searchText = null;
    var _hotContainer = null;

    function init(info) {
        if (!nce) {
            nce = info;

            _columnList = $('#editColumnsList');
            _hideColumnsList = $('#hideColumnsList');
            _editCellModal = $('#editCellModal');
            _editCellValue = $('#editCellValue');
            _editCellCache = $('#editCellCache');
            _editCellCancel = $('#editCellCancel');
            _editCellClear = $('#editCellClear');
            _editCellRadioURL = $('#editCellRadioURL');
            _editColumnModal = $('#editColumnsModal');
            _valueDropdown = $('#datatypes-value');
            _urlDropdown = $('#datatypes-url');
            _clipboard = $('#cell-clipboard');
            _editColClipboard = $('#edit-columns-clipboard');
            _searchField = document.getElementById('search-field');
            _searchInfo = $('#search-info');
            _ncubeContent = $('#ncube-content');
            _ncubeHtmlError = $('#ncube-error');
            _isRefAxis = $('#isRefAxis');
            _hasRefFilter = $('#hasRefFilter');
            _addAxisName = $('#addAxisName');
            _addAxisTypeName = $('#addAxisTypeName');
            _addAxisValueTypeName = $('#addAxisValueTypeName');
            _refAxisGroup = $('#refAxisGroup');
            _refAxisBranch = $('#refAxisBranch');
            _refAxisStatus = $('#refAxisStatus');
            _refAxisApp = $('#refAxisApp');
            _refAxisVersion = $('#refAxisVersion');
            _refAxisCube = $('#refAxisCube');
            _refAxisAxis = $('#refAxisAxis');
            _refFilterGroup = $('#refFilterGroup');
            _refFilterBranch = $('#refFilterBranch');
            _refFilterStatus = $('#refFilterStatus');
            _refFilterApp = $('#refFilterApp');
            _refFilterVersion = $('#refFilterVersion');
            _refFilterCube = $('#refFilterCube');
            _refFilterMethod = $('#refFilterMethod');
            _isRefAxisUpdate = $('#isRefAxisUpdate');
            _hasRefFilterUpdate = $('#hasRefFilterUpdate');
            _updateAxisModal = $('#updateAxisModal');
            _updateAxisName = $('#updateAxisName');
            _updateAxisTypeName = $('#updateAxisTypeName');
            _updateAxisValueTypeName = $('#updateAxisValueTypeName');
            _updateAxisSortOrder = $('#updateAxisSortOrder');
            _refAxisGroupUpdate = $('#refAxisGroupUpdate');
            _refAxisBranchUpdate = $('#refAxisBranchUpdate');
            _refAxisStatusUpdate = $('#refAxisStatusUpdate');
            _refAxisAppUpdate = $('#refAxisAppUpdate');
            _refAxisVersionUpdate = $('#refAxisVersionUpdate');
            _refAxisCubeUpdate = $('#refAxisCubeUpdate');
            _refAxisAxisUpdate = $('#refAxisAxisUpdate');
            _refFilterGroupUpdate = $('#refFilterGroupUpdate');
            _refFilterBranchUpdate = $('#refFilterBranchUpdate');
            _refFilterStatusUpdate = $('#refFilterStatusUpdate');
            _refFilterAppUpdate = $('#refFilterAppUpdate');
            _refFilterVersionUpdate = $('#refFilterVersionUpdate');
            _refFilterCubeUpdate = $('#refFilterCubeUpdate');
            _refFilterMethodUpdate = $('#refFilterMethodUpdate');
            _topAxisBtn = $('#topAxisBtn');
            _filterModal = $('#filterModal');
            _filterTable = $('#filterTable');
            _hotContainer = $('#hot-container');

            addSelectAllNoneListeners();
            addAxisEditListeners();
            addColumnEditListeners();
            addColumnHideListeners();
            addEditCellListeners();
            addSearchListeners();
            addModalFilters();
            modalsDraggable(true);

            _editCellRadioURL.on('change', onEditCellRadioUrlChange);
            _urlDropdown.on('change', enabledDisableCheckBoxes);
            _valueDropdown.on('change', enabledDisableCheckBoxes);
            $('#addAxisCancel').on('click', addAxisClose);
            $('#addAxisOk').on('click', addAxisOk);
            $('#deleteAxisCancel').on('click', deleteAxisClose);
            $('#deleteAxisOk').on('click', deleteAxisOk);
            $('#updateAxisMenu').on('click', updateAxis);
            $('#updateAxisOk').on('click', updateAxisOk);
            _editCellModal.on('shown.bs.modal', onEditCellModalShown);

            $(document).on('keydown', onWindowKeyDown);

            $(window).on('resize', function () {
                delay(hotResize, PROGRESS_DELAY);
            });
        }

        setCoordinateBarListeners();
        buildCubeMap();
        setUtilityBarDisplay();
    }
    
    function onWindowKeyDown(e) {
        var isModalDisplayed = $('body').hasClass('modal-open');
        var focus = $(':focus');

        if (!isModalDisplayed && focus && ['cube-search','cube-search-content','search-field'].indexOf(focus.attr('id')) < 0) {
            var keyCode = e.keyCode;
            if (e.metaKey || e.ctrlKey) {
                // Control Key (command in the case of Mac)
                if (keyCode === KEY_CODES.F) {
                    e.preventDefault();
                    _searchField.focus();
                }
                else if (keyCode == KEY_CODES.X) {
                    if (CLIP_NCE == _clipFormat) {
                        editCutCopy(true);  // true = isCut
                    } else {
                        excelCutCopy(true);
                    }
                }
                else if (keyCode == KEY_CODES.C)
                {
                    if (CLIP_NCE == _clipFormat) {
                        editCutCopy(false); // false = copy
                    } else {
                        excelCutCopy(false);
                    }
                } else if (keyCode == KEY_CODES.K) {
                    toggleClipFormat(e);
                } else if (keyCode == KEY_CODES.V) {
                    // Ctrl-V
                    // Point focus to hidden text area so that it will receive the pasted content
                    editPaste();
                }
            } else {
                if (keyCode === KEY_CODES.DELETE) {
                    nceCutCopyData(getSelectedCellRange(), true);
                    reload();
                }
            }
        }
    }

    function hotResize() {
        var winWidth = $(this).width();
        var winHeight = $(this).height();
        if (hot) {
            destroyEditor();
            hot.updateSettings({
                height: winHeight - _hotContainer.offset().top,
                width: winWidth
            });
        }
        
        render();
        setUtilityBarDisplay();
    }

    function getNumFrozenCols() {
        var savedNum = nce.getNumFrozenCols();
        if (savedNum !== null) {
            return parseInt(savedNum);
        }
        return colOffset || 1;
    }

    function saveNumFrozenCols(num) {
        nce.saveNumFrozenCols(num);
    }

    function toggleClipFormat(event) {
        if (event) {
            event.preventDefault();
        }
        _clipFormat = CLIP_NCE === _clipFormat ? CLIP_EXCEL : CLIP_NCE;
        render();
    }

    function setUtilityBarDisplay() {
        var moveRightBtn = $('#coordinate-bar-move-right');
        var btnWidth = moveRightBtn.outerWidth();

        var windowWidth = $(this).width();
        var search = $('#search-container');
        var searchWidth = search.width();
        var coordWidth = windowWidth - searchWidth;
        $(getDomCoordinateBar()).width(coordWidth - btnWidth * 2); // coord bar text
        moveRightBtn.css({left: coordWidth - btnWidth}); // keep the right button to the end
        $('#util-container-bar').width(coordWidth); // coord bar container for background
        search.css({left: windowWidth - searchWidth});
    }

    function buildCubeMap() {
        // Step 1: Build giant cube list names string for pattern matching
        var s = "";
        prefixes = ['rpm.class.', 'rpm.enum.', ''];
        cubeMap = null;
        cubeMap = nce.getCubeMap();

        $.each(cubeMap, function (key)
        {
            if (key.length > 2)
            {   // 1. Only support n-cube names with 3 or more characters in them (too many false replaces will occur otherwise)
                // 2. Chop off accepted prefixes.
                for (var i=0; i < prefixes.length; i++)
                {
                    if (key.indexOf(prefixes[i]) == 0)
                    {
                        key = key.replace(prefixes[i], '');
                        break;
                    }
                }
                // 3. Reverse the cube list order (comes from server alphabetically case-insensitively sorted) to match
                // longer strings before shorter strings.
                // 4. Replace '.' with '\.' so that they are only matched against dots (period), not any character.
                s = escapeRegExp(key) + '|' + s;
            }
        });

        if (s.length > 0) {
            s = s.substring(0, s.length - 1);
        }
        s = '\\b(' + s + ')\\b';
        cubeMapRegex = null;
        cubeMapRegex = new RegExp(s, 'gi');
    }

    function showHtmlError(text) {
        _ncubeContent.hide();
        _ncubeHtmlError.show();
        _ncubeHtmlError[0].innerHTML = text;
        buildTopAxisMenu();
    }

    function clearHtmlError() {
        _ncubeContent.show();
        _ncubeHtmlError.hide();
    }
    
    function killHotEditor() {
        var editor, keys, i, len, key;
        editor = hot.getActiveEditor();
        if (editor) {
            keys = Object.keys(editor);
            for (i = 0, len = keys.length; i < len; i++) {
                key = keys[i];
                if (['eventManager', 'eventListeners'].indexOf(key) < 0) {
                    delete editor[key];
                }
            }
            editor = null;
        }
    }
    
    function load(keepTable) {
        resetCoordinateBar();
        if (hot && !keepTable) {
            killHotEditor();
            removeClipFormatToggleListener();
            removeButtonDropdownLocationListeners();
            removeCellListeners();
            hot.destroy();
            hot = null;
        }

        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.GET_JSON, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), {mode:'json-index'}], {noResolveRefs:true});
        if (result.status === false) {
            showHtmlError('Failed to load JSON for cube, error: ' + result.data);
            return;
        }

        clearHtmlError();

        handleCubeData(JSON.parse(result.data));
        if (!hot) {
            hot = new Handsontable(_hotContainer[0], getHotSettings());
        } else {
            render();
        }
        selectSavedOrDefaultCell();
        setClipFormatToggleListener();
        _searchField.value = nce.getSearchQuery() || '';
        _searchText = '';
        runSearch();
        searchDown();
    }

    function setSearchHelperText() {
        var query = _searchField.value;
        if (query !== null && query !== '') {
            var len = _searchCoords.length;
            var idx = _currentSearchResultIndex + 1;
            _searchInfo[0].innerHTML = len > 0 ? idx + ' of ' + len : 'not found';
        } else {
            _searchInfo[0].innerHTML = '';
        }
    }

    function addSearchListeners() {
        $(_searchField).on('focus', addHotBeforeKeyDown);

        $(_searchField).on('blur', removeHotBeforeKeyDown);

        $(_searchField).on('keyup', function (e) {
            var keyCode = e.keyCode;
            if (keyCode === KEY_CODES.ENTER) {
                runSearch();
                searchDown();
            } else if (keyCode === KEY_CODES.ARROW_DOWN) {
                searchDown();
            } else if (keyCode === KEY_CODES.ARROW_UP) {
                searchUp();
            } else {
                delay(function() {
                    runSearch();
                    searchDown();
                }, 500);
            }
        });

        $('#search-btn-down').on('click', function() {
            searchDown();
            $(this).blur();
        });

        $('#search-btn-up').on('click', function() {
            searchUp();
            $(this).blur();
        });

        $('#search-btn-remove').on('click', function() {
            searchClear();
            $(this).blur();
        });

        _searchInfo.on('click', searchClick);
    }

    function addHotBeforeKeyDown() {
        hot.addHook('beforeKeyDown', onBeforeKeyDown);
    }

    function removeHotBeforeKeyDown() {
        hot.removeHook('beforeKeyDown', onBeforeKeyDown);
    }

    function searchClear() {
        _searchField.value = '';
        clearSearchMatches();
        setSearchHelperText();
        render();
    }

    function searchClick() {
        _searchField.focus();
    }

    function runSearch() {
        var query = _searchField.value;
        if (_searchText !== query) {
            if (query && query.length > 0) {
                searchCubeData(query);
            } else {
                clearSearchMatches();
            }
            setSearchHelperText();
            render();
            _searchText = query;
            nce.saveSearchQuery(query);
        }
    }

    function getSearchResult(idx) {
        var val = _searchCoords[idx].split('_');
        return {row:parseInt(val[0]), col:parseInt(val[1])};
    }

    function highlightSearchResult(idx) {
        var result = getSearchResult(idx);
        hot.selectCell(result.row, result.col);
        setSearchHelperText();
    }

    function searchDown() {
        var searchResultsLen = _searchCoords.length;
        if (searchResultsLen > 0) {
            var idx = _currentSearchResultIndex === searchResultsLen - 1 ? searchResultsLen - 1 : ++_currentSearchResultIndex;
            highlightSearchResult(idx);
        }
    }

    function searchUp() {
        if (_searchCoords.length > 0) {
            var idx = _currentSearchResultIndex > 0 ? --_currentSearchResultIndex : 0;
            highlightSearchResult(idx);
        }
    }

    function clearSearchMatches() {
        for (var axisNum = 0, axisLen = axes.length; axisNum < axisLen; axisNum++) {
            var axis = axes[axisNum];
            var cols = axis.columns;
            var colKeys = Object.keys(cols);
            for (var colNum = 0, colLen = colKeys.length; colNum < colLen; colNum++) {
                cols[colKeys[colNum]].isSearchResult = false;
            }
        }

        var cells = data.cells;
        var cellKeys = Object.keys(cells);
        for (var i = 0, len = cellKeys.length; i < len; i++) {
            cells[cellKeys[i]].isSearchResult = false;
        }

        _searchCoords = null;
        _searchCoords = [];
        _currentSearchResultIndex = -1;
        nce.saveSearchQuery(null);
    }

    function getAxesOrderedId(numericallyOrderedString) {
        var arr = numericallyOrderedString.split('_');
        arr.sort(function(x, y) {
            var xId = getAxisIdFromString(x);
            var yId = getAxisIdFromString(y);
            return _axisIdsInOrder.indexOf(xId) - _axisIdsInOrder.indexOf(yId);
        });
        return arr.join('_');
    }

    function searchCubeData(query) {
        _searchCoords = null;
        _searchCoords = [];
        _currentSearchResultIndex = -1;

        var queryLower = query.toLowerCase();
        var multiplier = 1;
        var rowSpacing = numRows - 2;
        var axisNum, colNum, axisLen, colLen;
        var rowSpacingHelper = [];

        var isHorizAxis = function(axisNum) {
            return axisLen > 1 && axisNum === colOffset;
        };

        var getColumnTableCoords = function() {
            if (isHorizAxis(axisNum)) {
                addToSearchCoords(1, colOffset + colNum);
                return;
            }
            if (getAppliedFilters().length === 0) {
                var rowIdx = colNum * rowSpacing;
                for (var m = 0; m < multiplier; m++) {
                    addToSearchCoords(rowIdx + 2, axisNum);
                    rowIdx += rowSpacing * colLen;
                }
                return;
            }
            for (var cIdx = 0, cLen = _columnIdCombinationsToShow.length; cIdx < cLen; cIdx++) {
                var combo = _columnIdCombinationsToShow[cIdx];
                var colId = axisColumnMap[axes[axisNum].name][colNum];
                if (combo.indexOf(colId) > -1) {
                    var add = cIdx === 0;
                    if (!add) {
                        var prevCombo = _columnIdCombinationsToShow[cIdx - 1];
                        if (prevCombo.indexOf(colId) === -1) {
                            add = true;
                        } else {
                            var curStr = getAxesOrderedId(combo);
                            var prevStr = getAxesOrderedId(prevCombo);
                            var curSubStr = curStr.substring(0, curStr.indexOf(colId));
                            var prevSubStr = prevStr.substring(0, prevStr.indexOf(colId));
                            add = curSubStr !== prevSubStr;
                        }
                    }
                    if (add) {
                        addToSearchCoords(cIdx + 2, axisNum);
                    }
                }
            }
        };

        // search all axes
        for (axisNum = 0, axisLen = axes.length; axisNum < axisLen; axisNum++) {
            var axis = axes[axisNum];
            var cols = axis.columns;
            var colKeys = Object.keys(cols);
            colLen = colKeys.length;
            rowSpacing /= colLen;

            rowSpacingHelper.push({axisName:axis.name, order:getAxisId(axis), rowSpacing:rowSpacing, horizAxis:isHorizAxis(axisNum)});

            // search all columns for an axis
            for (colNum = 0; colNum < colLen; colNum++)
            {
                var col = cols[colKeys[colNum]];
                var colVal = getRowHeaderPlainTextForWidth(axis, col);
                col.isSearchResult = false;
                if (colVal.toLowerCase().indexOf(queryLower) > -1) {
                    col.isSearchResult = true;
                    getColumnTableCoords();
                }
            }
            multiplier *= colLen;
        }
        rowSpacingHelper.sort(function(a, b) {
            return a.order - b.order;
        });

        // search cells
        var cells = data.cells;
        var cellKeys = Object.keys(cells);
        for (var cellNum = 0, len = cellKeys.length; cellNum < len; cellNum++)
        {
            var cellId = cellKeys[cellNum];
            var cell = cells[cellId];
            var cellVal = getTextCellValue(cell);
            cell.isSearchResult = cellVal.toLowerCase().indexOf(queryLower) > -1;

            if (cell.isSearchResult) {
                var colIds = cellId.split('_');
                var r = 0;
                var c = 0;
                if (getAppliedFilters().length === 0) {
                    for (var colIdNum = 0, colIdLen = colIds.length; colIdNum < colIdLen; colIdNum++) {
                        var curColId = colIds[colIdNum];
                        var curHelperObj = rowSpacingHelper[colIdNum];
                        var curColNum = axisColumnMap[curHelperObj.axisName].indexOf(curColId);

                        if (curHelperObj.horizAxis) {
                            c = curColNum;
                        } else {
                            r += curColNum * curHelperObj.rowSpacing;
                        }
                    }
                    addToSearchCoords(r + 2, c + (colOffset || 1));
                } else {
                    var topAxis = axes[colOffset];
                    var topAxisId = getAxisId(topAxis);
                    for (var cellIdIdx = 0, cellIdLen = colIds.length; cellIdIdx < cellIdLen; cellIdIdx++) {
                        if (getAxisIdFromString(colIds[cellIdIdx]) === topAxisId) {
                            var topColId = colIds.splice(cellIdIdx, 1)[0];
                            c = axisColumnMap[topAxis.name].indexOf(topColId);
                            var colIdCombo = colIds.join('_');
                            r = _columnIdCombinationsToShow.indexOf(colIdCombo);
                            if (r > -1) {
                                addToSearchCoords(r + 2, c + (colOffset || 1));
                            }
                            break;
                        }
                    }
                }
            }
        }

        _searchCoords.sort(function(a, b) {
            var aa = a.split('_');
            var ab = b.split('_');
            var rowA = parseInt(aa[0]);
            var rowB = parseInt(ab[0]);
            if (rowA === rowB) {
                var colA = parseInt(aa[1]);
                var colB = parseInt(ab[1]);
                return colA - colB;
            } else {
                return rowA - rowB;
            }
        });
    }

    function colorAxisButtons() {
        $('button.axis-btn')
            .filter(function() {
                var buttonAxisName = $(this)[0].textContent.trim().toLowerCase();
                return _hiddenColumns.hasOwnProperty(buttonAxisName);
            })
            .removeClass('btn-primary')
            .addClass('btn-warning');
    }

    function getColumnLength(axis) {
        if (axis.hasOwnProperty('columnLength')) {
            return axis.columnLength;
        }

        var colLen = Object.keys(axis.columns).length;
        if (axis.hasDefault) {
            colLen++;
        }
        axis.columnLength = colLen;
        return colLen;
    }

    function getAxisId(axis) {
        if (axis.hasOwnProperty('id')) {
            return axis.id;
        }

        var col = Object.keys(axis.columns)[0];
        if (!col) {
            return;
        }
        var id = getAxisIdFromString(col);
        axis.id = id;
        return id;
    }

    function getAxisIdFromString(id) {
        return id.substring(0, id.length - AXIS_DEFAULT.length);
    }

    function getAxisColumn(axis, colNum) {
        if (axis.columnLength === 0 || colNum < 0) {
            return;
        }

        var key = axisColumnMap[axis.name][colNum];
        var obj = axis.columns[key];
        obj.id = key;

        return obj;
    }

    function getColumnHeader(col) {
        var colHeader = getAxisColumn(axes[colOffset], col - colOffset);
        return colHeader ? colHeader : {id:'', value:''};
    }

    function getColumnHeaderValue(col) {
        return getRowHeaderValue(axes[colOffset], getColumnHeader(col));
    }

    function getColumnHeaderId(col) {
        return getColumnHeader(col).id;
    }

    function getAxisIndex(axis) {
        return Object.keys(data.axes).indexOf(axis.name.toLowerCase()) + 1;
    }

    function getAxisDefault(axis) {
        var defaultId = getAxisIndex(axis) + AXIS_DEFAULT;
        return {id:defaultId, value:DEFAULT_TEXT};
    }

    function getRowHeader(row, col) {
        var result;
        var rowNum = row - 2;
        if (rowNum < 0 || col < 0 || col > axes.length) {
            return;
        }

        var axis = axes[col];
        if (getAppliedFilters().length === 0) {
            var colLen = getColumnLength(axis);
            var repeatRowCount = (numRows - 2) / colLen;

            for (var axisNum = 0; axisNum < col; axisNum++) {
                var tempAxis = axes[axisNum];
                var colCount = getColumnLength(tempAxis);
                repeatRowCount /= colCount;
            }
            var columnNumberToGet = Math.floor(rowNum / repeatRowCount) % colLen;
            result = getAxisColumn(axis, columnNumberToGet);
        } else {
            var ids = _columnIdCombinationsToShow[rowNum];
            if (ids) {
                ids = ids.split('_');
                for (var idx = 0, idLen = ids.length; idx < idLen; idx++) {
                    var curId = ids[idx];
                    var column = axis.columns[curId];
                    if (column !== undefined) {
                        result = column;
                        result.id = curId;
                        break;
                    }
                }
            }
        }

        return result;
    }

    function getRowHeaderValue(axis, rowHeader) {
        var getDateRangeString = function(range) {
            var v1 = range[0].substring(0, range[0].indexOf('T'));
            var v2 = range[1].substring(0, range[1].indexOf('T'));
            return '[' + v1 + ' - ' + v2 + ')';
        };

        var rule = '';
        var val = '';

        var type = axis.type.toLowerCase();
        var valueType = axis.valueType.toLowerCase();

        if (type === 'rule' && rowHeader.name !== undefined) {
            rule = '<span class="rule-name">' + rowHeader.name + '</span><hr class="hr-rule"/>';
        }

        val = rowHeader.value;
        if (valueType === 'date' && val !== DEFAULT_TEXT) {
            if (typeof val === 'object') {
                if (typeof val[0] === 'object') {
                    var temp = '';
                    for (var i = 0, len = val.length; i < len; i++) {
                        temp += getDateRangeString(val[i]) + ', ';
                    }
                    val = temp.slice(0, temp.lastIndexOf(', '));
                } else {
                    val = getDateRangeString(val);
                }
            } else {
                val = val.substring(0, val.indexOf('T'));
            }
        }
        if (val === undefined) {
            val = '<a class="nc-anc">' + rowHeader.url + '</a>';
        }
        if (rule !== '') {
            val = rule + '<span class="code">' + val + '</span>';
        }
        return '' + val;
    }

    function getRowHeaderPlainTextForWidth(axis, rowHeader) {
        var regexAnyTag = /(<([^>]+)>)/ig;
        var regexHr = /(<hr([^>]+)>)/ig;
        var val = getRowHeaderValue(axis, rowHeader);
        return val.replace(regexHr, '\n').replace(regexAnyTag, '');
    }

    function getRowHeaderPlainText(row, col) {
        var regexAnyTag = /(<([^>]+)>)/ig;
        var regexHr = /(<hr([^>]+)>)/ig;
        var val = getRowHeaderValue(axes[col], getRowHeader(row, col));
        return val.replace(regexHr, ' - ').replace(regexAnyTag, '');
    }

    function getRowHeaderId(row, col) {
        var header = getRowHeader(row, col);
        if (header) {
            return header.id;
        }
    }

    function getCellId(row, col) {
        var cellId = '';
        var headerInfo = [];
        var i;

        if (axes.length > 1) {
            for (i = 0; i < colOffset; i++) {
                headerInfo.push(getRowHeaderId(row, i));
            }
            headerInfo.push(getColumnHeaderId(col));

            headerInfo.sort(function(a, b) {
                return a - b;
            });

            cellId = headerInfo.join('_');
        } else {
            cellId = getRowHeaderId(row, 0);
        }

        headerInfo = null;
        return cellId;
    }

    function getCellData(row, col) {
        return data.cells[getCellId(row, col)];
    }

    function hasCustomAxisOrder() {
        return localStorage.hasOwnProperty(getStorageKey(nce, AXIS_ORDER));
    }

    function getAppliedFilters() {
        var appliedFilters = [];
        for (var i = 0, len = _filters.length; i < len; i++) {
            var curFilter = _filters[i];
            if (curFilter.isApplied) {
                appliedFilters.push(curFilter);
            }
        }

        return appliedFilters;
    }

    function getCellsByColumnId(colId) {
        var allCells = data.cells;
        var cellKeys = Object.keys(allCells);
        var columnCells = {};
        for (var i = 0, len = cellKeys.length; i < len; i++) {
            var key = cellKeys[i];
            if (key.indexOf(colId) > -1) {
                var cell = allCells[key];
                columnCells[key] = cell;
            }
        }

        allCells = null;
        cellKeys = null;
        return columnCells;
    }

    function doesCellMatchFilterExpression(cell, filter) {
        if (!filter.isIncludeAll && cell === undefined) {
            return false;
        }
        var cellVal = getTextCellValue(cell);
        cell = null;
        var expVal = filter.expressionValue;
        switch (filter.comparator) {
            case '=':
                return cellVal === expVal;
            case '!=':
                return cellVal !== expVal;
            case '>':
                if (isNaN(cellVal) || isNaN(expVal)) {
                    return cellVal > expVal;
                }
                return parseInt(cellVal) > parseInt(expVal);
            case '<':
                if (isNaN(cellVal) || isNaN(expVal)) {
                    return cellVal < expVal;
                }
                return parseInt(cellVal) < parseInt(expVal);
            case 'contains':
                return cellVal.toLowerCase().indexOf(expVal.toLowerCase()) > -1;
            case 'excludes':
                return cellVal.toLowerCase().indexOf(expVal.toLowerCase()) === -1;
        }
        return false;
    }

    function getFirstFilteredCellsFromData() {
        var combos = [];
        var colIdReplaceIdx;
        var filter = getAppliedFilters()[0];
        if (filter.isIncludeAll) {
            var dAxes = data.axes;
            var dKeys = Object.keys(dAxes);
            for (var a = 0, aLen = dKeys.length; a < aLen; a++) {
                var axis = dAxes[dKeys[a]];
                if (axis.columns[filter.column]) {
                    colIdReplaceIdx = combos.length === 0 ? 0 : combos[0].length;
                } else {
                    var tempCombos = [];
                    var columns = axis.columns;
                    var colKeys = Object.keys(columns);
                    for (var colIdx = 0, colLen = colKeys.length; colIdx < colLen; colIdx++) {
                        var colId = colKeys[colIdx];
                        var comboLen = combos.length;
                        if (comboLen === 0) {
                            tempCombos.push(colId);
                        } else {
                            for (var comboIdx = 0; comboIdx < comboLen; comboIdx++) {
                                tempCombos.push(combos[comboIdx] + '_' + colId);
                            }
                        }
                    }
                    combos = tempCombos;
                    combos.sort();
                }
            }
        } else {
            var colCells = getCellsByColumnId(filter.column);
            var colCellKeys = Object.keys(colCells);
            for (var c = 0, cLen = colCellKeys.length; c < cLen; c++) {
                var colCellKey = colCellKeys[c];
                var colCell = colCells[colCellKey];
                if (doesCellMatchFilterExpression(colCell, filter)) {
                    colIdReplaceIdx = colCellKey.indexOf(filter.column);
                    var axisColId = colCellKey.replace(filter.column,'').replace('__','');
                    if (axisColId.indexOf('_') === 0) {
                        axisColId = axisColId.substring(1);
                    } else if (axisColId.substring(axisColId.length - 1) === '_') {
                        axisColId = axisColId.substring(0, axisColId.length - 1);
                    }
                    combos.push(axisColId);
                }
            }
        }

        return {idCombinations:combos, idReplaceIdx:colIdReplaceIdx};
    }

    function handleCubeData(cubeData) {

        var determineAxesOrder = function (cubeAxes) {
            axes = [];
            _axisIdsInOrder = [];
            var i, len, axis;
            if (hasCustomAxisOrder())
            {
                var order = JSON.parse(localStorage[getStorageKey(nce, AXIS_ORDER)]);
                for (i = 0, len = order.length; i < len; i++)
                {
                    axis = cubeAxes[order[i]];
                    getColumnLength(axis);
                    axes.push(axis);
                    _axisIdsInOrder.push(getAxisId(axis));
                }
            }
            else
            {
                var keys = Object.keys(cubeAxes);
                for (i = 0, len = keys.length; i < len; i++) {
                    axes.push(cubeAxes[keys[i]]);
                }

                var horizontal;
                axes.sort(function (a, b) {
                    return getColumnLength(b) - getColumnLength(a);
                });

                var delta;
                var smallestDelta = Number.MAX_VALUE;
                for (i = 0, len = axes.length; i < len; i++) {
                    axis = axes[i];
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

                for (i = 0, len = axes.length; i < len; i++) {
                    _axisIdsInOrder.push(getAxisId(axes[i]));
                }
            }
        };

        var setUpDataTable = function() {
            cubeName = cubeData.ncube;
            var totalRows = 1;
            colOffset = axes.length - 1;
            _filters = getSavedFilters();
            var appliedFilters = getAppliedFilters();

            if (axes.length > 1) {
                var horizAxis = axes[colOffset];

                numColumns = colOffset;

                var colLen = getColumnLength(horizAxis);
                if (colLen === 0) {
                    numColumns++;
                }

                numColumns += colLen;

                if (appliedFilters.length === 0) {
                    for (var axisNum = 0; axisNum < colOffset; axisNum++) {
                        totalRows *= getColumnLength(axes[axisNum]);
                    }
                    numRows = totalRows + 2;
                } else {
                    var resultFromFirstFilter = getFirstFilteredCellsFromData();
                    var colIdReplaceIdx = resultFromFirstFilter.idReplaceIdx;
                    _columnIdCombinationsToShow = resultFromFirstFilter.idCombinations;

                    for (var f = appliedFilters[0].isIncludeAll ? 0 : 1, fLen = appliedFilters.length; f < fLen; f++) {
                        var filter = appliedFilters[f];
                        var addIn = colIdReplaceIdx === 0 ? filter.column + '_' : '_' + filter.column;

                        for (var comboIdx = 0, comboLen = _columnIdCombinationsToShow.length; comboIdx < comboLen; comboIdx++) {
                            var combo = _columnIdCombinationsToShow[comboIdx];
                            var cellId = [combo.slice(0, colIdReplaceIdx), addIn, combo.slice(colIdReplaceIdx)].join('');
                            var cell = data.cells[cellId];
                            if (!doesCellMatchFilterExpression(cell, filter)) {
                                _columnIdCombinationsToShow.splice(comboIdx, 1);
                                comboIdx--;
                                comboLen--;
                            }
                        }
                    }
                    numRows = _columnIdCombinationsToShow.length + 2;
                }
            }
            else
            {
                var axis = axes[0];
                numColumns = 2;
                numRows = getColumnLength(axis) + 2;
            }
        };

        var setUpAxisColumnMap = function() {
            axisColumnMap = null;
            axisColumnMap = {};
            for (var axisNum = 0, axisLen = axes.length; axisNum < axisLen; axisNum++) {
                var axis = axes[axisNum];
                var axisName = axis.name;
                axisColumnMap[axisName] = [];
                var colArray = axisColumnMap[axisName];
                var colKeys = Object.keys(axis.columns);
                for (var colNum = 0, colLen = colKeys.length; colNum < colLen; colNum++) {
                    colArray.push(colKeys[colNum]);
                }

                if (axis.hasDefault) {
                    var defaultColumn = getAxisDefault(axis);
                    var defId = defaultColumn.id;
                    colArray.push(defId);
                    axis.columns[defId] = defaultColumn;
                }
            }
        };

        var hideColumns = function()
        {
            _hiddenColumns = null;
            _hiddenColumns = {};
            var storageKey = getStorageKey(nce, HIDDEN_COLUMNS);
            if (localStorage.hasOwnProperty(storageKey))
            {
                _hiddenColumns = JSON.parse(localStorage[storageKey]);
            }
            for (var i = 0, axisLength=axes.length; i < axisLength; i++)
            {
                var axis = axes[i];
                var lowerAxisName = axis.name.toLowerCase();
                if (_hiddenColumns.hasOwnProperty(lowerAxisName))
                {
                    var hiddenAxis = _hiddenColumns[lowerAxisName];
                    var keys = Object.keys(hiddenAxis);
                    for (var j = 0, len = keys.length; j < len; j++)
                    {
                        axis.columnLength--;
                        var columnId = keys[j];
                        delete axis.columns[columnId];
                    }
                }
            }
        };

        _searchCoords = null;
        _searchCoords = [];
        _currentSearchResultIndex = 0;
        _searchField.value = '';
        setSearchHelperText();
        _defaultCellText = null;

        data = cubeData;
        determineAxesOrder(data.axes);
        hideColumns();
        setUpDataTable();
        setUpAxisColumnMap();
        setUpColumnWidths();
    }

    function buildTopAxisMenu() {
        if (_ncubeHtmlError.is(':visible')) {
            _topAxisBtn.hide();
            return;
        }
        _topAxisBtn.show();
        if (axes.length < 2) {
            _topAxisBtn.empty();
        } else {
            buildAxisMenu(axes[colOffset], _topAxisBtn);
            var frozen = getNumFrozenCols();
            var idx = colOffset > frozen ? colOffset : frozen;
            idx += 2;
            var offset = _hotContainer.find('div.ht_clone_top.handsontable > div > div > div > table > tbody > tr:nth-child(1) > td:nth-child(' + idx + ')').offset();
            _topAxisBtn.css({top: offset.top + 1, left: offset.left + 1});
        }
    }

    function setUpColumnWidths() {
        var getStringWidth = function(str, font) {
            return $('<p>' + str + '</p>').canvasMeasureWidth(font);
        };

        var calcDomDims = function (value, type) {
            if (textDims.hasOwnProperty(value)) {
                return textDims[value];
            }
            var font = CODE_CELL_TYPE_LIST.indexOf(type) > -1 ? FONT_CODE : FONT_CELL;
            var width = 0;
            var lineWidths = [];
            var idx = value.indexOf('\n');
            if (idx < 0) {
                width = getStringWidth(value, font);
                lineWidths.push(width);
                width = findWidth(0, width);
            } else {
                var temp2;
                var temp1 = value;
                var tempWidth;
                while (idx > -1) {
                    temp2 = value.substring(0, idx);
                    tempWidth = getStringWidth(temp2, font);
                    lineWidths.push(tempWidth);
                    width = findWidth(width, tempWidth);
                    temp1 = temp1.substring(idx + 1);
                    idx = temp1.indexOf('\n');
                }
                tempWidth = getStringWidth(temp1, font);
                lineWidths.push(tempWidth);
                width = findWidth(width, tempWidth);
            }

            var ret = {width:width, lineWidths:lineWidths};
            textDims[value] = ret;
            return ret;
        };

        var findWidth = function(oldWidth, newWidth)
        {
            if (oldWidth == undefined) oldWidth = 0;
            if (newWidth == undefined) newWidth = 0;
            if (oldWidth > MAX_COL_WIDTH || newWidth > MAX_COL_WIDTH)
            {
                return MAX_COL_WIDTH
            }
            else
            {
                return Math.max(MIN_COL_WIDTH, oldWidth, newWidth);
            }
        };

        var setupTopWidths = function()
        {
            var savedWidths = getSavedColumnWidths();
            var axis = axes[colOffset];
            var axisName = axis.name;
            var columns = axis.columns;
            var columnKeys = axisColumnMap[axisName];
            var width;
            if (savedWidths.hasOwnProperty(0 + colOffset)) {
                width = savedWidths[0 + colOffset];
            } else {
                var modifier = axis.isRef ? CALC_WIDTH_REF_AX_BUTTON_MOD : CALC_WIDTH_AXIS_BUTTON_MOD;
                var buttonWidth = calcDomDims(axisName, null).width + modifier;
                width = findWidth(0, buttonWidth);
            }
            topWidths[columnKeys[0]] = width;

            for (var colIndex = 0, colLength = columnKeys.length; colIndex < colLength; colIndex++)
            {
                var colKey = columnKeys[colIndex];
                if (savedWidths.hasOwnProperty(colIndex + colOffset)) {
                    width = savedWidths[colIndex + colOffset];
                } else {
                    var column = columns[colKey];
                    var columnText = getRowHeaderPlainTextForWidth(axis, column);
                    var firstWidth = calcDomDims(columnText, column.type).width + CALC_WIDTH_BASE_MOD;
                    width = findWidth(topWidths[colKey], firstWidth);
                }
                topWidths[colKey] = width;
            }

            if (columnKeys.length == 0)
            {
                _columnWidths.push(MIN_COL_WIDTH);
            }
            else
            {
                var firstColId = columnKeys[0];
                var colPrefix = firstColId.slice(0,-10);
                var regex = new RegExp(colPrefix + "(?:\\d{10})");
                var cells = data.cells;
                var cellKeys = Object.keys(cells);
                var topKeys = Object.keys(topWidths);
                for (var keyIndex = 0, len = cellKeys.length; keyIndex < len; keyIndex++) {
                    var cellKey = cellKeys[keyIndex];
                    var colId = regex.exec(cellKey)[0];
                    var hotColIdx = topKeys.indexOf(colId) + colOffset;
                    if (savedWidths.hasOwnProperty(hotColIdx)) {
                        width = savedWidths[hotColIdx];
                    } else {
                        var cell = cells[cellKey];
                        var value = getTextCellValue(cell);
                        var dims = calcDomDims(value, cell.type);
                        width = dims.width + CALC_WIDTH_BASE_MOD;
                        width = findWidth(topWidths[colId], width);
                        setHeightForCellRow(cellKey, width, dims);
                    }

                    topWidths[colId] = width;
                }
            }
        };

        var buildWidthArray = function()
        {
            var savedWidths = getSavedColumnWidths();
            for (var hotCol = 0; hotCol < numColumns; hotCol++)
            {
                if (hotCol < colOffset)
                {
                    var width;
                    if (savedWidths.hasOwnProperty(hotCol)) {
                        width = savedWidths[hotCol];
                    } else {
                        width = findWidestColumn(axes[hotCol]);
                    }
                    _columnWidths.push(width);
                }
                else
                {
                    var columnHeaderId = getColumnHeaderId(hotCol);
                    _columnWidths.push(topWidths[columnHeaderId]);
                }
            }
        };

        var buildSingleAxisWidthArray = function()
        {
            var savedWidths = getSavedColumnWidths();

            // header width
            if (savedWidths.hasOwnProperty(0)) {
                _columnWidths.push(savedWidths[0]);
            } else {
                _columnWidths.push(findWidestColumn(axes[0]));
            }

            // cell width
            if (savedWidths.hasOwnProperty(1)) {
                _columnWidths.push(savedWidths[1]);
            } else {
                var correctWidth;
                var oldWidth = 0;
                var cells = data.cells;
                var cellKeys = Object.keys(cells);
                for (var keyIndex = 0, len = cellKeys.length; keyIndex < len; keyIndex++) {
                    var cellKey = cellKeys[keyIndex];
                    var cell = cells[cellKey];
                    var value = getTextCellValue(cell);
                    var dims = calcDomDims(value, cell.type);
                    var width = dims.width + CALC_WIDTH_BASE_MOD;
                    correctWidth = findWidth(oldWidth, width);
                    setHeightForCellRow(cellKey, correctWidth, dims);
                    oldWidth = correctWidth;
                }
                _columnWidths.push(correctWidth);
            }
        };

        var findWidestColumn = function (axis)
        {
            var axisName = axis.name;
            var modifier = axis.isRef ? CALC_WIDTH_REF_AX_BUTTON_MOD : CALC_WIDTH_AXIS_BUTTON_MOD;
            var buttonWidth = calcDomDims(axisName, null).width + modifier;
            var oldWidth = findWidth(0, buttonWidth);
            var correctWidth = oldWidth;
            var axisColumns = axisColumnMap[axisName];
            for (var axisCol = 0, axisColLength = axisColumns.length; axisCol < axisColLength; axisCol++)
            {
                var columnId = axisColumns[axisCol];
                var column = axis.columns[columnId];
                var columnText = getRowHeaderPlainTextForWidth(axis, column);
                var colWidth = calcDomDims(columnText, column.type).width + CALC_WIDTH_BASE_MOD;
                correctWidth = findWidth(oldWidth, colWidth);
                oldWidth = correctWidth;
            }
            return correctWidth;
        };

        var setHeightForCellRow = function(cellId, colWidth, dims) {
            var cellIdArray = cellId.split('_');
            for (var i = 0, len = cellIdArray.length; i < len; i++) {
                var id = cellIdArray[i];
                if (getAxisIdFromString(id).indexOf(horizAxisId) > -1) {
                    cellIdArray.splice(i, 1);
                    break;
                }
            }
            var rowId = cellIdArray.join('_');
            var lineWidths = dims.lineWidths;
            var totalLines = 0;
            for (var lineIdx = 0, numLines = lineWidths.length; lineIdx < numLines; lineIdx++) {
                totalLines += Math.ceil(lineWidths[lineIdx] / colWidth);
            }

            var newHeight = totalLines * FONT_HEIGHT + 1;
            var prevHeight = heights[rowId] || MIN_ROW_HEIGHT;
            if (newHeight > prevHeight) {
                prevHeight = newHeight;
            }
            heights[rowId] = prevHeight;
        };

        var buildHeightArray = function() {
            var savedHeights = getSavedRowHeights();
            for (var r = 0; r < numRows; r++) {
                var saved = savedHeights[r];
                if (saved) {
                    _rowHeights.push(saved);
                } else if (r < 2) {
                    _rowHeights.push(33);
                } else {
                    var headerInfo = [];
                    for (var i = 0; i < colOffset; i++) {
                        headerInfo.push(getRowHeaderId(r, i));
                    }
                    headerInfo.sort(function(a, b) {
                        return a - b;
                    });
                    var rowId = headerInfo.join('_');
                    _rowHeights.push(heights[rowId] || MIN_ROW_HEIGHT);
                }
            }
        };

        _columnWidths = null;
        _columnWidths = [];
        _rowHeights = null;
        _rowHeights = [];
        var topWidths = {};
        var heights = {};
        var textDims = {};
        var horizAxisId = getAxisId(axes[colOffset]);
        if (axes.length == 1) {
            buildSingleAxisWidthArray();
        } else {
            setupTopWidths();
            buildWidthArray();
        }
        buildHeightArray();
        textDims = null; // free up memory
    }

    function deleteSavedColumnWidths() {
        saveOrDeleteValue(null, COLUMN_WIDTHS);
        saveOrDeleteValue(null, ROW_HEIGHTS);
    }

    function getSavedColumnWidths() {
        var localWidthVar = localStorage[getStorageKey(nce, COLUMN_WIDTHS)];
        return localWidthVar ? JSON.parse(localWidthVar) : {};
    }

    function getSavedRowHeights() {
        var localHeightVar = localStorage[getStorageKey(nce, ROW_HEIGHTS)];
        return localHeightVar ? JSON.parse(localHeightVar) : {};
    }

    function saveColumnWidth(col, newVal) {
        var saved = getSavedColumnWidths();
        saved[col] = newVal;
        saveOrDeleteValue(saved, getStorageKey(nce, COLUMN_WIDTHS));
    }

    function saveRowHeight(row, newVal) {
        var saved = getSavedRowHeights();
        saved[row] = newVal;
        saveOrDeleteValue(saved, getStorageKey(nce, ROW_HEIGHTS));
    }

    function calcColumnHeader(index) {
        if (axes.length == 1) {
            if (index == 0) {
                return '';
            }
            return 'A';
        }

        if (index < colOffset) {
            return '';
        }
        var dividend = (index + 1) - colOffset;
        var modulo;
        var colName = '';

        while (dividend > 0) {
            modulo = (dividend - 1) % 26;
            colName = String.fromCharCode(65 + modulo) + colName;
            dividend = Math.floor((dividend - modulo) / 26);
        }

        return colName;
    }

    function calcRowHeader(index) {
        if (index < 1) {
            return '';
        }
        var text = _clipFormat === CLIP_NCE ? NBSP + 'NCE' + NBSP : 'Excel';
        if (index === 1) {
            return '<span class="glyphicon glyphicon-copy" style="font-size:13px"></span>' + NBSP + text;
        }
        return index - 1;
    }

    function setFrozenColumns(numFixed) {
        saveNumFrozenCols(numFixed);
        hot.updateSettings({fixedColumnsLeft:numFixed});
    }

    function getHotSettings() {
        return {
            copyPaste: false,
            fillHandle: {
                autoInsertRow: false
            },
            colWidths: _columnWidths,
            rowHeights: _rowHeights,
            autoColumnSize: false,
            autoRowSize: false,
            enterBeginsEditing: false,
            enterMoves: {row: 1, col: 0},
            tabMoves : {row: 0, col: 1},
            colHeaders: calcColumnHeader,
            rowHeaders: calcRowHeader,
            startCols: numColumns,
            startRows: numRows,
            maxCols: numColumns,
            maxRows: numRows,
            contextMenu: false,
            manualColumnResize: true,
            manualRowResize: true,
            fixedColumnsLeft: getNumFrozenCols(),
            fixedRowsTop: 2,
            currentRowClassName: CLASS_HANDSON_CURRENT_ROW,
            currentColClassName: CLASS_HANDSON_CURRENT_ROW,
            outsideClickDeselects: false,
            height: function() {
                return $(window).height() - _hotContainer.offset().top;
            },
            width: function() {
                return $(window).width();
            },
            cells: function (row, col, prop) {
                return {renderer:categoryRenderer};
            },
            beforeRender: function() {
                _firstRenderedCol = null;
                removeButtonDropdownLocationListeners();
                removeCellListeners();
            },
            afterRender: function() {
                buildTopAxisMenu();
                moveTopAxisMenu();
                setButtonDropdownLocations();
                colorAxisButtons();
            },
            afterColumnResize: function(currentColumn, newSize, isDoubleClick) {
                saveColumnWidth(currentColumn, newSize);
            },
            afterRowResize: function(currentRow, newSize, isDoubleClick) {
                saveRowHeight(currentRow, newSize);
            },
            afterSelection: function(r, c, r2, c2) {
                var display = '';
                var axisName;
                var axisVal;
                if (c >= colOffset) {
                    if (r === 1) {
                        display = '<strong>Axis</strong>: ' + axes[colOffset].name + ', <strong>Column</strong>:' + getColumnHeaderValue(c);
                    } else if (r > 1) {
                        display = '&nbsp;';
                        for (var axisNum = 0; axisNum < colOffset; axisNum++) {
                            axisName = axes[axisNum].name;
                            axisVal = getRowHeaderPlainText(r, axisNum);
                            display += '<strong>' + axisName + '</strong>: ' + axisVal + ', ';
                        }
                        if (axes.length > 1) {
                            var axis = axes[colOffset];
                            axisName = axis.name;
                            axisVal = getRowHeaderPlainTextForWidth(axis, getColumnHeader(c));
                        } else {
                            axisName = axes[0].name;
                            axisVal = getRowHeaderPlainText(r, 0);
                        }
                        display += '<strong>' + axisName + '</strong>: ' + axisVal;
                    }
                }
                else if (r > 1) {
                    display = '<strong>Axis</strong>: ' + axes[c].name + ', <strong>Column</strong>:' + getRowHeaderPlainText(r, c);
                }
                
                resetCoordinateBar(display);
                delay(saveViewPosition(r, c), PROGRESS_DELAY);
            },
            afterScrollHorizontally: function() {
                moveTopAxisMenu();
                delay(saveViewPosition, PROGRESS_DELAY);
            },
            afterScrollVertically: function() {
                delay(saveViewPosition, PROGRESS_DELAY);
            },
            beforeAutofill: function(start, end, data) {
                autoFillNce(start, end);
            }
        };
    }

    function saveViewPosition(row, col) {
        var wth = $('.wtHolder');
        var r;
        var c;
        var saved = nce.getViewPosition();
        if (row !== undefined && col !== undefined) {
            r = row;
            c = col;
        } else {
            r = saved.row;
            c = saved.c;
        }
        nce.saveViewPosition({row:r, col:c, left:wth.scrollLeft(), top:wth.scrollTop()});
        wth = null;
    }

    function moveTopAxisMenu() {
        var numFixed = getNumFrozenCols();
        var tr = _hotContainer.find('div.ht_clone_top.handsontable > div > div > div > table > tbody > tr:nth-child(1)');
        var scrollAmt = $('.ht_master .wtHolder').scrollLeft();
        var thWidth = tr.find('th').outerWidth();
        var frozenWidth = thWidth;
        var startingWidth = thWidth;
        var newWidth;
        
        if (_firstRenderedCol === 0) {
            for (var i = 0; i < colOffset; i++) {
                var curWidth = tr.find('td').eq(i).outerWidth();
                startingWidth += curWidth;
                if (i < numFixed) {
                    frozenWidth += curWidth;
                }
            }
        } else if (numFixed > 0) {
            newWidth = $('.ht_clone_left.handsontable').width();
        }
        
        if (!newWidth) {
            if (scrollAmt < (startingWidth - frozenWidth)) {
                newWidth = startingWidth - scrollAmt;
            } else {
                var afterSubtract = frozenWidth || (startingWidth - scrollAmt);
                newWidth = afterSubtract < thWidth ? thWidth : afterSubtract;
            }
        }
        _topAxisBtn.css({left: newWidth});
    }

    function setClipFormatToggleListener() {
        _hotContainer.find('div.ht_clone_top_left_corner.handsontable > div > div > div > table > tbody > tr:nth-child(2) > th').on('click', function(e) {
            toggleClipFormat(e);
        });
    }
    
    function removeClipFormatToggleListener() {
        _hotContainer.find('div.ht_clone_top_left_corner.handsontable > div > div > div > table > tbody > tr:nth-child(2) > th').off('click');
    }

    function setButtonDropdownLocations() {
        $('.dropdown-toggle').on('click', function() {
            var button = $(this);
            var offset = button.offset();
            var dropDownTop = offset.top + button.outerHeight();
            var dropDownLeft = offset.left;
            
            var modal = button.closest('.modal-content');
            if (modal.length > 0) {
                var modalOffset = modal.offset();
                dropDownTop -= modalOffset.top;
                dropDownLeft -= modalOffset.left;
            }
            
            button.parent().find('ul').css({top: dropDownTop, left: dropDownLeft});
            button = null;
            modal = null;
        });
    }

    function removeButtonDropdownLocationListeners() {
        $('.dropdown-toggle').off('click');
        $('.axis-menu').remove();
    }
    
    function removeCellListeners() {
        _hotContainer.find('a.nc-anc').parent().empty();
    }

    function categoryRenderer(instance, td, row, col, prop, value, cellProperties) {
        Handsontable.renderers.TextRenderer.apply(this, arguments);
        cellProperties.editor = NcubeBaseEditor;
        td.className = '';
        if (_firstRenderedCol === null) {
            _firstRenderedCol = col;
        }

        // cube name
        if (row === 0 && (col < colOffset || col === 0)) {
            if (col === 0) {
                td.innerHTML = cubeName;
            }
            td.className += CLASS_HANDSON_CELL_CUBE_NAME;
            td.style.background = BACKGROUND_CUBE_NAME;
            td.style.color = COLOR_WHITE;
            td.colSpan = 1;
            cellProperties.editor = CubeEditor;
            if (col < axes.length - 2) {
                td.style.borderRight = NONE;
                td.style.overflow = 'visible';
            }
        }

        // horizontal axis metadata area
        else if (row === 0) {
            if (axes.length > 1 && (col === colOffset || (_firstRenderedCol > 0 && col === _firstRenderedCol))) {
                td.style.overflow = 'visible';
                td.colSpan = axes[colOffset].columnLength - _firstRenderedCol;
            }
            td.style.background = BACKGROUND_AXIS_INFO;
            td.style.color = COLOR_WHITE;
            cellProperties.readOnly = true;
        }

        // vertical axes metadata area
        else if (row === 1 && (col < colOffset || col === 0)) {
            td.style.background = BACKGROUND_AXIS_INFO;
            td.style.color = COLOR_WHITE;
            td.style.overflow = 'visible';
            cellProperties.readOnly = true;
            buildAxisMenu(axes[col], td);
        }

        // column headers
        else if (row === 1) {
            if (axes.length > 1) {
                var column = getColumnHeader(col);
                td.innerHTML = getRowHeaderValue(axes[colOffset], column);
                if (column.isSearchResult) {
                    td.className += CLASS_HANDSON_SEARCH_RESULT;
                }
            }
            td.className += CLASS_HANDSON_TABLE_HEADER;
            cellProperties.editor = ColumnEditor;
        }

        // row headaers
        else if (col === 0 || col < colOffset) {
            var rowHeader = getRowHeader(row, col);
            var axis = axes[col];
            if (row > 2 && getColumnLength(axis) > 1  && col < colOffset - 1 && rowHeader.id === getRowHeader(row - 1, col).id && (col === 0 || (col > 0 && getRowHeader(row, col - 1) === getRowHeader(row - 1, col - 1)))) {
                td.style.borderTop = NONE;
            } else {
                td.innerHTML = getRowHeaderValue(axis, rowHeader);
            }
            td.className += CLASS_HANDSON_TABLE_HEADER;
            if (getRowHeader(row, col).isSearchResult) {
                td.className += CLASS_HANDSON_SEARCH_RESULT;
            }

            cellProperties.editor = ColumnEditor;
            if (axis.isRef) {
                cellProperties.readOnly = true;
            }
        }

        // otherwise in cell data
        else {
            var cellData = getCellData(row, col);
            td.className += CLASS_HANDSON_CELL_BASIC;
            if (cellData) {
                if (cellData.isSearchResult) {
                    td.className += CLASS_HANDSON_SEARCH_RESULT;
                }

                if (cellData.url !== undefined) {
                    td.className += CLASS_HANDSON_CELL_URL;
                    td.innerHTML = '<a class="nc-anc">' + cellData.url + '</a>';
                    buildUrlLink(td);
                } else if (cellData.value !== undefined && CODE_CELL_TYPE_LIST.indexOf(cellData.type) > -1) {
                    td.className += CLASS_HANDSON_CELL_CODE;
                    td.innerHTML = buildExpressionLink('' + cellData.value, 'groovy');
                    activateLinks(td);
                } else if ('date' === cellData.type) {
                    var val = cellData.value;
                    td.innerHTML = val.substring(0, val.indexOf('T'));
                } else {
                    td.innerHTML = buildExpressionLink('' + cellData.value);
                    activateLinks(td);
                }
            } else if (data.defaultCellValue !== null && data.defaultCellValue !== undefined) {
                td.className += CLASS_HANDSON_CELL_DEFAULT;
                if (_defaultCellText === null) {
                    _defaultCellText = buildExpressionLink('' + data.defaultCellValue, NONE);
                }
                td.innerHTML = _defaultCellText;
                activateLinks(td);
            } else if (data.defaultCellValueUrl !== null && data.defaultCellValueUrl !== undefined) {
                td.className += CLASS_HANDSON_CELL_DEFAULT;
                td.innerHTML = '<a class="nc-anc">' + data.defaultCellValueUrl + '</a>';
                buildUrlLink(td);
            }

            if (row % 2 !== 0) {
                td.className += CLASS_HANDSON_CELL_ODD_ROW;
            }

            cellProperties.editor = CellEditor;
        }
    }

    function addToSearchCoords (row, col) {
        var val = row + '_' + col;
        if (_searchCoords.indexOf(val) === -1) {
            _searchCoords.push(val);
        }
    }

    function getTableTextCellValue(row, col) {
        return getTextCellValue(getCellData(row, col));
    }

    function getTextCellValue(cellData) {
        var val = '';

        if (cellData) {
            if (cellData.url !== undefined) {
                val = cellData.url;
            } else if ('date' === cellData.type) {
                val = cellData.value;
                val = val.substring(0, val.indexOf('T'));
            } else {
                val = cellData.value;
            }
        }
        else if (data.defaultCellValue !== null && data.defaultCellValue !== undefined) {
            val = data.defaultCellValue;
        }
        return '' + val;
    }

    function buildUrlLink(element) {
        // Add click handler that opens clicked cube names
        $(element).find('a').on('click', function (e) {
            nce.clearError();
            var link = this.innerHTML;
            if (link.indexOf('http:') === 0 || link.indexOf('https:') === 0 || link.indexOf('file:') === 0) {
                window.open(link);
            } else {
                var result = nce.call(CONTROLLER + CONTROLLER_METHOD.RESOLVE_RELATIVE_URL, [nce.getSelectedTabAppId(), link], {noResolveRefs:true});
                if (result.status && result.data) {
                    link = result.data;
                    window.open(link);
                } else {
                    var msg = result.data ? result.data : 'Unable to resolve relative URL against entries in sys.classpath';
                    nce.showNote('Unable to open ' + link + ':<hr class="hr-small"/>' + msg);
                }
            }
            e.preventDefault();
        });
    }

    function buildExpressionLink(url, highlightLanguage) {
        if (url && url.length > 2) {
            var found = false;
            var val;
            var shouldHighlight = highlightLanguage !== NONE;
        
            url = url.replace(cubeMapRegex, function (matched) {
                found = true;
                return '<a class="nc-anc">' + matched + '</a>';
            });
            val = url;
        
            if (found) {
                if (shouldHighlight) {
                    //highlight in between links
                    var highlighted = '';
                    var tempHighlight;
                    var top;
                    var ancIdx;
                    while ((ancIdx = url.indexOf('<a class="nc-anc">')) > -1) {
                        var text = url.substring(0, ancIdx);
                        var endIdx = url.indexOf('</a>') + 4;
                        tempHighlight = highlightLanguage ? hljs.highlight(highlightLanguage, text, true, top) : hljs.highlightAuto(text);
                        if (highlightLanguage) {
                            top = hljs.highlight(highlightLanguage, url.substring(0, endIdx), true, top).top;
                        }
                        highlighted += tempHighlight.value;
                        highlighted += url.substring(ancIdx, endIdx);
                        url = url.substring(endIdx);
                    }
                    tempHighlight = highlightLanguage ? hljs.highlight(highlightLanguage, url, true, top) : hljs.highlightAuto(url);
                    highlighted += tempHighlight.value;
                    val = highlighted;
                }
            } else {
                if (shouldHighlight) {
                    var highlighted = highlightLanguage ? hljs.highlight(highlightLanguage, url, true) : hljs.highlightAuto(url);
                    val = highlighted.value;
                }
            }
        } else {
            val = url;
        }
        return val;
    }

    function activateLinks(element) {
        // Add click handler that opens clicked cube names
        $(element).find('a').on('click', function () {
            var cubeName = this.textContent.toLowerCase();
            for (var i = 0, len = prefixes.length; i < len; i++) {
                var fullName = prefixes[i] + cubeName;
                if (cubeMap[fullName]) {
                    nce.selectCubeByName(nce.getProperCubeName(fullName));
                    break;
                }
            }
        });
    }

    function closeAxisMenu() {
        $('.axis-menu.open').removeClass('open');
        $('div.dropdown-backdrop').hide();
        destroyEditor();
    }

    function buildAxisMenu(axis, element) {
        var el = $(element);
        var axisName = axis.name;
        var axisIndex = findIndexOfAxisName(axisName);
        var isRef = axis.isRef;
        var div = $('<div/>').addClass('btn-group axis-menu');

        el.empty();
        el.append(div);
        div.append(buildAxisMenuHtml(axis));
        div.find('a.anc-axis-source').on('click', function(e) {
            e.preventDefault();
            closeAxisMenu();
            onAxisSourceClick(axis);
        });
        div.find('a.anc-break-reference').on('click', function(e) {
            e.preventDefault();
            e.stopImmediatePropagation();
            onBreakReferenceClick(axis, $(this));
        });
        div.find('a.anc-update-axis').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            updateAxis(axisName);
        });
        div.find('a.anc-add-axis').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            addAxis();
        });
        div.find('a.anc-delete-axis').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            deleteAxis(axisName);
        });
        div.find('a.anc-edit-axis-metadata').on('click', function(e) {
            e.preventDefault();
            if (isRef) {
                e.stopImmediatePropagation();
                return;
            }
            closeAxisMenu();
            editAxisMetadata(axis);
        });
        div.find('a.anc-edit-columns').on('click', function (e) {
            e.preventDefault();
            if (isRef) {
                e.stopImmediatePropagation();
                return;
            }
            closeAxisMenu();
            editColumns(axisName);
        });
        div.find('a.anc-filter-data').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            filterOpen();
        });
        div.find('a.anc-clear-filters').on('click', function (e) {
            e.preventDefault();
            if (_filters.length === 0) {
                e.stopImmediatePropagation();
                return;
            }
            closeAxisMenu();
            clearFilters();
            reload();
        });
        div.find('a.anc-hide-columns').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            hideColumns(axisName);
        });
        div.find('a.anc-show-all-columns').on('click', function (e) {
            var axisLower = axisName.toLowerCase();
            e.preventDefault();
            if (!_hiddenColumns.hasOwnProperty(axisLower)) {
                e.stopImmediatePropagation();
            }
            closeAxisMenu();
            delete _hiddenColumns[axisLower];
            storeHiddenColumns();
            reload();
        });
        div.find('a.anc-revert-sizing').on('click', function (e) {
            var columnWidthStorageKey = getStorageKey(nce, COLUMN_WIDTHS);
            e.preventDefault();
            if (!localStorage[columnWidthStorageKey]) {
                e.stopImmediatePropagation();
            }
            closeAxisMenu();
            saveOrDeleteValue(null, columnWidthStorageKey);
            saveOrDeleteValue(null, getStorageKey(nce, ROW_HEIGHTS));
            reload();
        });
        div.find('a.anc-frozen-cols').on('click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
        });
        div.find('input.frozen-cols-input').on('click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
        }).on('keyup', function(e) {
            if ([KEY_CODES.ENTER,KEY_CODES.TAB].indexOf(e.keyCode) > -1) {
                closeAxisMenu();
            }
        }).on('change', function() {
            var val = $(this).val();
            if (!isNaN(val)) {
                setFrozenColumns(parseInt(val));
            }
        });
        div.find('button.move-left').on('click', function (e) {
            e.preventDefault();
            if (axisIndex === 0) {
                e.stopImmediatePropagation();
            }
            closeAxisMenu();
            moveAxis(axisIndex, axisIndex - 1);
        });
        div.find('button.move-up').on('click', function (e) {
            e.preventDefault();
            closeAxisMenu();
            clearFilters();
            moveAxis(axisIndex, colOffset);
        });
        div.find('button.move-right').on('click', function (e) {
            e.preventDefault();
            if (axisIndex === axes.length - 2) {
                e.stopImmediatePropagation();
            }
            closeAxisMenu();
            moveAxis(axisIndex, axisIndex + 1);
        });
        div.find('a.anc-revert-axis-order').on('click', function (e) {
            e.preventDefault();
            if (!hasCustomAxisOrder()) {
                e.stopImmediatePropagation();
            }
            closeAxisMenu();
            delete localStorage[getStorageKey(nce, AXIS_ORDER)];
            reload();
        });
    }

    function editAxisMetadata(axis) {
        var axisName = axis.name;
        var metaPropertyOptions = {
            objectType: METAPROPERTIES.OBJECT_TYPES.AXIS,
            objectName: axisName,
            axis: axis,
            readonly: !nce.checkPermissions(nce.getSelectedTabAppId(), cubeName + '/' + axisName, PERMISSION_ACTION.UPDATE)
        };
        openMetaPropertiesBuilder(metaPropertyOptions);
    }

    function onAxisSourceClick(axis) {
        var appId = appIdFrom(axis.referenceApp, axis.referenceVersion, axis.referenceStatus, axis.referenceBranch);
        nce.selectCubeByName(axis.referenceCubeName, appId);
    }

    function onBreakReferenceClick(axis, an) {
        var btnsHtml;
        an.parent().find('li').not(an.parent()).find('button').remove();
        if (an.find('button').length === 0) {
            btnsHtml = '<button class="btn btn-danger btn-xs pull-right axis-menu-button">Cancel</button>';
            btnsHtml += '<button class="btn btn-primary btn-xs pull-right axis-menu-button confirm">Confirm</button>';
            an.append(btnsHtml);
            an.find('button.confirm').on('click', function () {
                $(this).parent().parent().parent().parent().removeClass('open');
                $(this).parent().parent().find('button').remove();
                $('div.dropdown-backdrop').hide();
                var result = nce.call(CONTROLLER + CONTROLLER_METHOD.BREAK_AXIS_REFERENCE, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), axisName]);
                if (result.status) {
                    closeAxisMenu();
                    reload();
                } else {
                    nce.showNote('Error breaking reference for axis ' + axis.name + ':<hr class="hr-small"/>' + result.data);
                }
            });
        } else {
            an.find('button').remove();
        }
    }

    function buildAxisMenuHtml(axis) {
        var axisName = axis.name;
        var isRef = axis.isRef;
        var axisIndex = findIndexOfAxisName(axisName);
        var isTopAxis = axisIndex === colOffset;
        var html = '';

        html += '<button type="button" class="btn-sm btn-primary dropdown-toggle axis-btn" data-toggle="dropdown"><span>';
        html += isRef ? axisName + NBSP + '<span class="glyphicon glyphicon-share-alt"></span>' : axisName;
        html += '</span><span class="caret"/></button>';

        html += '<ul class="dropdown-menu" role="menu">';

        if (isRef) {
            html += '<li><a href="#" class="anc-axis-source">Go to Axis Source</a></li>';
            html += '<li><a href="#" class="anc-break-reference">Break Reference</a></li>';
            html += '<li class="divider"/>';
        }

        html += '<li><a href="#" class="anc-update-axis">Update Axis...</a></li>';
        html += '<li><a href="#" class="anc-add-axis">Add Axis...</a></li>';
        html += '<li><a href="#" class="anc-delete-axis">Delete Axis...</a></li>';
        html += '<li';
        if (isRef) {
            html += ' class="disabled"';
        }
        html += '><a href="#" class="anc-edit-axis-metadata">Edit Axis Metadata...</a></li>';
        html += '<li class="divider"/>';

        html += '<li';
        if (isRef) {
            html += ' class="disabled"';
        }
        html += '><a href="#" class="anc-edit-columns">Edit Columns...</a></li>';
        html += '<li class="divider"/>';

        if (isTopAxis) {
            html += '<li><a href="#" class="anc-filter-data">Filter Data...</a></li>';
            html += '<li';
            if (_filters.length === 0) {
                html += ' class="disabled"';
            }
            html += '><a href="#" class="anc-clear-filters">Clear Filters</a></li>';
            html += '<li class="divider"/>';
        }

        html += '<li><a href="#" class="anc-hide-columns">Hide Columns...</a></li>';
        html += '<li';
        if (!_hiddenColumns.hasOwnProperty(axisName.toLowerCase())) {
            html += ' class="disabled"';
        }
        html += '><a href="#" class="anc-show-all-columns">Show All Columns</a></li>';
        html += '<li';
        if (!localStorage[getStorageKey(nce, COLUMN_WIDTHS)]) {
            html += ' class="disabled"';
        }
        html += '><a href="#" class="anc-revert-sizing">Revert Column / Row Sizing</a></li>';
        html += '<li class="divider"/>';

        html += '<li><a href="#" class="anc-frozen-columns"># Frozen Columns:';
        html += '<input type="text" class="form-control frozen-cols-input" value="' + getNumFrozenCols() + '"/>'
        html += '</a></li>';

        if (!isTopAxis) {
            html += '<li class="divider"/>';
            html += '<li><a href="#" class="dropdown-header">Move Axis</a></li>';
            html += '<li><div role="group" class="btn-group btn-group-sm indent-axis-buttons">';

            html += '<button type="button" class="btn btn-default move-left" aria-label="Move Left"';
            if (axisIndex === 0) {
                html += ' disabled'
            }
            html += '><span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span></button>';

            html += '<button type="button" class="btn btn-default move-up" aria-label="Move Up"';
            html += '><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span></button>';

            html += '<button type="button" class="btn btn-default move-right" aria-label="Move Right"';
            if (axisIndex === axes.length - 2) {
                html += ' disabled'
            }
            html += '><span class="glyphicon glyphicon-arrow-right" aria-hidden="true"></span></button>';

            html += '</div></li>';

            html += '<li';
            if (!hasCustomAxisOrder()) {
                html += ' class="disabled"';
            }
            html += '><a href="#" class="anc-revert-axis-order">Revert Axis Order</a></li>';
        }

        html += '</ul>';
        return html;
    }

    //====================================== coordinate bar functions ==================================================

    function getDomCoordinateBar() {
        return document.getElementById('coordinate-bar-text');
    }

    function getDomCoordinateBarLeftButton() {
        return document.getElementById('coordinate-bar-move-left');
    }

    function getDomCoordinateBarRightButton() {
        return document.getElementById('coordinate-bar-move-right');
    }

    function resetCoordinateBar(displayText) {
        var bar = getDomCoordinateBar();
        var left = getDomCoordinateBarLeftButton();
        var right = getDomCoordinateBarRightButton();

        bar.scrollLeft = 0;
        bar.innerHTML = displayText || '';

        if ($(bar).hasScrollBar()) {
            $(left).show();
            $(right).show();
        } else {
            $(left).hide();
            $(right).hide();
        }
        bar = null;
        left = null;
        right = null;
    }

    function setCoordinateBarListeners() {
        $(getDomCoordinateBarLeftButton()).on('click', function() {
            getDomCoordinateBar().scrollLeft = getDomCoordinateBar().scrollLeft - COORDINATE_BAR_SCROLL_AMOUNT;
            $(this).blur();
        });

        $(getDomCoordinateBarRightButton()).on('click', function() {
            getDomCoordinateBar().scrollLeft = getDomCoordinateBar().scrollLeft + COORDINATE_BAR_SCROLL_AMOUNT;
            $(this).blur();
        });
    }

    // ==================================== Begin Custom HOT Editors ===================================================

    function destroyEditor() {
        if (hot.getActiveEditor()) {
            hot.getActiveEditor().finishEditing(null, null, null, true);
        }
        var searchQuery = _searchField.value;
        if (searchQuery !== null && searchQuery.length > 0) {
            var curCell = getSelectedCellRange();
            if (curCell) {
                var curRow = curCell.startRow;
                var curCol = curCell.startCol;
                var prevIdx = _currentSearchResultIndex;
                searchCubeData(searchQuery);
                _currentSearchResultIndex = prevIdx;

                for (var i = 0, len = _searchCoords.length; i < len; i++) {
                    var curSearchCoord = getSearchResult(i);
                    if (curRow === curSearchCoord.row && curCol === curSearchCoord.col) {
                        _currentSearchResultIndex = i;
                        break;
                    }
                }
            }

            setSearchHelperText();
            render();
        }
    }

    function onBeforeKeyDown(event) {
        event.isImmediatePropagationEnabled = false;
        event.cancelBubble = true;
    }

    function keepKeyDownText(e) {
        var keycode = e.keyCode;
        var validNotNumPad =
            (keycode > 47 && keycode < 58)   || // number keys
            keycode == 32 || keycode == 13   || // spacebar & return key(s) (if you want to allow carriage returns)
            (keycode > 64 && keycode < 91)   || // letter keys
            (keycode > 185 && keycode < 193) || // ;=,-./` (in order)
            (keycode > 218 && keycode < 223);   // [\]' (in order)

        if (validNotNumPad) {
            var addChar = String.fromCharCode(keycode);
            _bufferText += e.shiftKey ? addChar : addChar.toLowerCase();
        } else if (keycode > 95 && keycode < 112) { //numpad
            _bufferText += String.fromCharCode(keycode - 48);
        }
    }

    var NcubeBaseEditor = Handsontable.editors.BaseEditor.prototype.extend();
    NcubeBaseEditor.prototype.prepare = function(row, col, prop, td, cellProperties) {
        Handsontable.editors.BaseEditor.prototype.prepare.apply(this, arguments);
        _bufferText = null;
        _bufferText = '';
        this.instance.addHook('beforeKeyDown', keepKeyDownText);
    };
    NcubeBaseEditor.prototype.open = function() {
        this.instance.addHook('beforeKeyDown', onBeforeKeyDown);
    };
    NcubeBaseEditor.prototype.finishEditing = function(restoreOriginalValue, ctrlDown, callback, forceClose) {
        if (!this.isOpened()) {
            if (forceClose) {
                this.state = Handsontable.EditorState.EDITING; // needed to override finish editing
            }
            Handsontable.editors.BaseEditor.prototype.finishEditing.apply(this, arguments);
        }
    };
    NcubeBaseEditor.prototype.close = function() {
        this.instance.removeHook('beforeKeyDown', onBeforeKeyDown);
        this.instance.removeHook('beforeKeyDown', keepKeyDownText);
    };
    NcubeBaseEditor.prototype.getValue = function() { };
    NcubeBaseEditor.prototype.setValue = function() { };
    NcubeBaseEditor.prototype.init = function() { };
    NcubeBaseEditor.prototype.focus = function() { };

    var CellEditor = NcubeBaseEditor.prototype.extend();
    CellEditor.prototype.prepare = function(row, col, prop, td, cellProperties) {
        NcubeBaseEditor.prototype.prepare.apply(this, arguments);
        _tableCellId = getCellId(this.row, this.col);
        _cellId = _tableCellId.split('_');
    };
    CellEditor.prototype.open = function() {
        NcubeBaseEditor.prototype.open.apply(this, arguments);
        editCell();
    };
    CellEditor.prototype.isOpened = function() {
        return _editCellModal.hasClass('in');
    };

    var ColumnEditor = NcubeBaseEditor.prototype.extend();
    ColumnEditor.prototype.open = function() {
        NcubeBaseEditor.prototype.open.apply(this, arguments);

        var row = this.row;
        var col = this.col;
        var axis;
        var column;
        var columnName;

        if (row === 1) {
            if (colOffset < 1) {
                return;
            }
            axis = axes[colOffset];
            column = getColumnHeader(col);
            columnName = getColumnHeaderValue(col);
        } else {
            axis = axes[this.col];
            column = getRowHeader(row, col);
            columnName = getRowHeaderPlainText(row, col);
        }

        var metaPropertyOptions = {
            objectType: METAPROPERTIES.OBJECT_TYPES.COLUMN,
            objectName: columnName,
            axis: axis,
            column: column,
            readonly: !nce.checkPermissions(nce.getSelectedTabAppId(), cubeName + '/' + axis.name, PERMISSION_ACTION.UPDATE)
        };
        openMetaPropertiesBuilder(metaPropertyOptions);
    };

    var CubeEditor = NcubeBaseEditor.prototype.extend();
    CubeEditor.prototype.open = function() {
        NcubeBaseEditor.prototype.open.apply(this, arguments);

        var metaPropertyOptions = {
            objectType: METAPROPERTIES.OBJECT_TYPES.CUBE,
            objectName: cubeName,
            readonly: !nce.checkPermissions(nce.getSelectedTabAppId(), cubeName, PERMISSION_ACTION.UPDATE)
        };
        openMetaPropertiesBuilder(metaPropertyOptions);
    };

    // ==================================== End Custom HOT Editors =====================================================

    // ==================================== Begin Edit Metaproperties ==================================================

    function getMetaPropertiesControllerInfo(metaPropertyOptions) {
        var getMethod;
        var setMethod;
        var params = [nce.getSelectedTabAppId(), nce.getSelectedCubeName()];
        var objectType = metaPropertyOptions.objectType;
        if (objectType === METAPROPERTIES.OBJECT_TYPES.COLUMN) {
            getMethod = CONTROLLER_METHOD.GET_COLUMN_METAPROPERTIES;
            setMethod = CONTROLLER_METHOD.UPDATE_COLUMN_METAPROPERTIES;
            params.push(metaPropertyOptions.axis.name);
            params.push(metaPropertyOptions.column.id);
        } else if (objectType === METAPROPERTIES.OBJECT_TYPES.AXIS) {
            getMethod = CONTROLLER_METHOD.GET_AXIS_METAPROPERTIES;
            setMethod = CONTROLLER_METHOD.UPDATE_AXIS_METAPROPERTIES;
            params.push(metaPropertyOptions.axis.name);
        } else {
            getMethod = CONTROLLER_METHOD.GET_CUBE_METAPROPERTIES;
            setMethod = CONTROLLER_METHOD.UPDATE_CUBE_METAPROPERTIES;
        }

        return {
            getter: getMethod,
            setter: setMethod,
            params: params
        };
    }

    function getMetaProperties(metaPropertyOptions) {
        var controllerInfo = getMetaPropertiesControllerInfo(metaPropertyOptions);
        var result = nce.call(CONTROLLER + controllerInfo.getter, controllerInfo.params);
        if (result.status !== true) {
            nce.showNote("Unable to fetch metaproperties for " + metaPropertyOptions.objectType + " '" + metaPropertyOptions.objectName + "':<hr class=\"hr-small\"/>" + result.data);
            return null;
        }
        return result.data;
    }

    function updateMetaProperties(metaPropertyOptions, metaProperties) {
        var mpMap = {};
        for (var mpIdx = 0, mpLen = metaProperties.length; mpIdx < mpLen; mpIdx++) {
            var prop = metaProperties[mpIdx];
            mpMap[prop.key] = prop.value;
        }

        var controllerInfo = getMetaPropertiesControllerInfo(metaPropertyOptions);
        controllerInfo.params.push(mpMap);
        var result = nce.call(CONTROLLER + controllerInfo.setter, controllerInfo.params);
        if (result.status !== true) {
            nce.showNote("Unable to update metaproperties for " + metaProperties.objectType + " '" + metaPropertyOptions.objectName + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        reload();
    }

    function openMetaPropertiesBuilder(metaPropertyOptions) {
        var mpData = getMetaProperties(metaPropertyOptions);
        if (mpData === null) {
            return;
        }
        delete mpData['@type'];
        var metaKeys = Object.keys(mpData);
        var metaProperties = [];
        for (var i = 0, len = metaKeys.length; i < len; i++) {
            var key = metaKeys[i];
            metaProperties.push({key:key, value:mpData[key]});
        }

        var builderOptions = {
            title: 'Metaproperties - ' + metaPropertyOptions.objectName,
            instructionsTitle: 'Instructions - Metaproperties',
            instructionsText: 'Add custom properties for this ' + metaPropertyOptions.objectType + '.',
            columns: {
                key: {
                    heading: 'Key',
                    type: PropertyBuilder.COLUMN_TYPES.TEXT
                },
                value: {
                    heading: 'Value',
                    type: PropertyBuilder.COLUMN_TYPES.TEXT
                }
            },
            readonly: metaPropertyOptions.readonly,
            afterSave: function() {
                updateMetaProperties(metaPropertyOptions, metaProperties);
                removeHotBeforeKeyDown();
            }
        };

        addHotBeforeKeyDown();
        PropertyBuilder.openBuilderModal(builderOptions, metaProperties);
    }

    // ===================================== End Edit Metaproperties ===================================================


    // ==================================== Begin Copy / Paste =========================================================

    function getSelectedCellRange() {
        var cellRange = hot.getSelected(); // index of the currently selected cells as an array [startRow, startCol, endRow, endCol]
        if (!cellRange) {
            return null;
        }
        var row1 = cellRange[0];
        var row2 = cellRange[2];
        var col1 = cellRange[1];
        var col2 = cellRange[3];
        return {
            startRow: Math.min(row1, row2),
            startCol: Math.min(col1, col2),
            endRow: Math.max(row1, row2),
            endCol: Math.max(col1, col2)
        };
    }

    function excelCutCopy(isCut) {
        if (isCut && !nce.ensureModifiable('Cannot cut / copy cells.')) {
            return;
        }

        var clipData = '';
        var range = getSelectedCellRange();
        var cells = [];

        for (var row = range.startRow; row <= range.endRow; row++)
        {
            for (var col = range.startCol; col <= range.endCol; col++)
            {
                var content = getTableTextCellValue(row, col);
                var cellId = getCellId(row, col);
                if (cellId) {
                    cells.push(cellId.split('_'));
                }

                if (content.indexOf('\n') > -1) {
                    // Must quote if newline (and double any quotes inside)
                    clipData += '"' + content.replace(/"/g, '""') + '"';
                }
                else
                {
                    clipData += content;
                }

                if (col < range.endCol)
                {   // Only add tab on last - 1 (otherwise paste will overwrite data in next column)
                    clipData += '\t';
                }
            }
            cells.push(null);
            clipData += '\n';
        }
        cells.splice(cells.length - 1, 1);

        if (isCut) {
            var result = nce.call("ncubeController.copyCells", [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), cells, true]);
            if (!result.status) {
                nce.showNote('Error copying/cutting cells:<hr class="hr-small"/>' + result.data);
                return;
            }
            reload();
        }

        _clipboard.val(clipData);
        _clipboard.focusin();
        _clipboard.select();
    }

    function editCutCopy(isCut) {
        if (isCut && !nce.ensureModifiable('Cannot cut / copy cells.')) {
            return;
        }

        var clipData = nceCutCopyData(getSelectedCellRange(), isCut);
        if (!clipData) {
            return;
        }

        if (isCut) {
            reload();
        }
        _clipboard.val(clipData);
        _clipboard.focusin();
        _clipboard.select();
    }

    function nceCutCopyData(range, isCut) {
        var cells = [];
        for (var row = range.startRow; row <= range.endRow; row++) {
            for (var col = range.startCol; col <= range.endCol; col++) {
                var cellId = getCellId(row, col);
                if (cellId) {
                    cells.push(cellId.split('_'));
                }
            }
            cells.push(null);
        }
        cells.splice(cells.length - 1, 1);

        // Get clipboard ready string + optionally clear cells from database
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.COPY_CELLS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), cells, isCut]);
        if (!result.status) {
            nce.showNote('Error copying/cutting cells:<hr class="hr-small"/>' + result.data);
            return null;
        }
        return CLIP_NCE + result.data;
    }

    function editPaste() {
        if (!nce.ensureModifiable('Cannot paste cells.')) {
            return;
        }

        _clipboard.val('');
        _clipboard.focus();

        var range = getSelectedCellRange();
        if (!range || range.length < 1) {
            return;
        }

        setTimeout(function() {
            var content = _clipboard.val();
            if (!content || content == "") {
                return;
            }
            pasteData(content, range);
        }, 100);
    }

    function pasteData(content, range) {
        // Location of first selected cell in 2D spreadsheet view.
        var firstRow = range.startRow;
        var firstCol = range.startCol;

        // Location of the last selected cell in 2D spreadsheet view.
        var lastRow = range.endRow;
        var lastCol = range.endCol;

        var numTableRows = hot.countRows();
        var numTableCols = hot.countCols();

        var onlyOneCellSelected = firstRow == lastRow && firstCol == lastCol;

        // Parse the clipboard content and build-up coordinates where this content will be pasted.
        var result;
        var colNum = firstCol;
        var rowNum = firstRow;

        if (content.indexOf(CLIP_NCE) == 0) {
            // NCE clipboard data (allows us to handle all cell types)
            content = content.slice(CLIP_NCE.length);
            var clipboard = JSON.parse(content);

            if (onlyOneCellSelected) {
                // Paste full clipboard data to cube (if it fits, otherwise clip to edges)
                for (var lineNum = 0, len = clipboard.length; lineNum < len; lineNum++) {
                    var cellInfo = clipboard[lineNum];
                    if (cellInfo == null) {
                        rowNum++;
                        colNum = firstCol;
                    } else {
                        if (colNum < numTableCols) {
                            // Do attempt to read past edge of 2D grid
                            var cellId = getCellId(rowNum, colNum);
                            cellInfo.push(cellId.split('_'));
                        }
                        colNum++;
                    }
                    if (rowNum >= numTableRows) {
                        // Do not go past bottom of grid
                        break;
                    }
                }
            } else {
                // Repeat / Clip case: multiple cells are selected when PASTE invoked, clip pasting to selected
                // range. This is the 'fill-mode' of paste (repeating clipboard data to fill selected rectangle)
                var clipRect = [[]];  // 2D array
                var clipCol = 0;
                var clipRow = 0;

                // Refashion linear clipboard (with nulls as column boundaries) to 2D
                for (var k = 0, len = clipboard.length; k < len; k++) {
                    if (clipboard[k]) {
                        clipRect[clipRow][clipCol] = clipboard[k];
                        clipCol++;
                    } else {
                        clipCol = 0;
                        clipRow++;
                        clipRect[clipRow] = [];
                    }
                }
                clipRow++;  // count of rows (not 0 based), e.g. 4 for rows 0-3 (needed for modulo below)

                rowNum = firstRow;
                var clipboard2 = [];

                for (var r = firstRow; r <= lastRow; r++) {
                    for (var c = firstCol; c <= lastCol; c++) {
                        var info = clipRect[(r - firstRow) % clipRow][(c - firstCol) % clipCol];
                        var cloneCellInfo = info.slice(0);
                        var cellId = getCellId(r, c);
                        cloneCellInfo.push(cellId.split('_'));
                        clipboard2.push(cloneCellInfo);
                    }
                    rowNum++;
                }
                clipboard = clipboard2;
            }
            // Paste cells from database
            result = nce.call("ncubeController.pasteCellsNce", [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), clipboard]);
        } else {
            // Normal clipboard data, from Excel, for example
            var lines = parseExcelClipboard(content);
            var coords = [];
            var rowCoords = [];
            var values = [];

            // If more than one cell is selected, create coords for all selected cells.
            // Server will repeat values, properly throughout the selected 'clip' region.
            for (var i = 0; i < lines.length; i++) {
                rowCoords = null;
                rowCoords = [];
                values.push(lines[i]);  // push a whole line of values at once.
                colNum = firstCol;

                for (var j = 0, len = lines[i].length; j < len; j++) {
                    if (colNum < numTableCols) {
                        // Do attempt to read past edge of 2D grid
                        var cellId = getCellId(rowNum, colNum);
                        rowCoords.push(cellId.split('_'));
                    }
                    colNum++;
                }
                coords.push(rowCoords);
                rowNum++;

                if (rowNum > numTableRows) {
                    // Do not go past bottom of grid
                    break;
                }
            }

            if (!onlyOneCellSelected) {
                // Multiple cells are selected when PASTE invoked, clip pasting to selected range.
                coords = null;
                coords = [];
                var addHotIds = [];
                for (r = firstRow; r <= lastRow; r++) {
                    rowCoords = [];
                    for (c = firstCol; c <= lastCol; c++) {
                        var cellId = getCellId(r, c);
                        rowCoords.push(cellId.split('_'));
                        addHotIds.push(cellId);
                    }
                    coords.push(rowCoords);
                }
            }

            // Paste cells from database
            result = nce.call(CONTROLLER + CONTROLLER_METHOD.PASTE_CELLS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), values, coords]);
        }

        if (result.status) {
            reload(true);
        } else {
            nce.clearError();
            nce.showNote('Error pasting cells:<hr class="hr-small"/>' + result.data);
        }
    }

    function autoFillNce(start, end) {
        var copyRange = getSelectedCellRange();
        var pasteRange = {
            startRow: start.row,
            endRow: end.row,
            startCol: start.col,
            endCol: end.col
        };

        if (copyRange.startRow < pasteRange.startRow) { //fill down
            copyRange.endRow = pasteRange.startRow - 1;
        } else if (copyRange.endRow > pasteRange.endRow) { //fill up
            copyRange.startRow = pasteRange.endRow + 1;
        } else if (copyRange.startCol < pasteRange.startCol) { //fill right
            copyRange.endCol = pasteRange.startCol - 1;
        } else if (copyRange.endCol > pasteRange.endCol) { //fill left
            copyRange.startCol = pasteRange.endCol + 1;
        }

        var content = nceCutCopyData(copyRange, false);
        if (content) {
            pasteData(content, pasteRange);
        }
    }

    // ==================================== End Copy / Paste ===========================================================

    // ==================================== Everything to do with Cell Editing =========================================

    function addEditCellListeners() {
        _editCellClear.on('click', editCellClear);
        _editCellCancel.on('click', editCellClose);
        $('#editCellOk').on('click', editCellOK);
        _editCellValue.on('keydown', function(e) {
            if (e.keyCode === KEY_CODES.TAB) {
                e.preventDefault();
                var start = this.selectionStart;
                var end = this.selectionEnd;

                // set textarea value to insert tab
                var oldVal = $(this).val();
                $(this).val(oldVal.substring(0, start) + '\t' + oldVal.substring(end));

                // put cursor to correct position
                this.selectionStart = this.selectionEnd = start + 1;
            }
        });
    }

    function editCell() {
        var appId = nce.getSelectedTabAppId();
        var modifiable = nce.checkPermissions(appId, cubeName, PERMISSION_ACTION.UPDATE);

        var result = nce.call("ncubeController.getCellNoExecute", [appId, cubeName, _cellId]);
        if (result.status === false) {
            nce.showNote('Unable to fetch the cell contents: ' + result.data);
            return;
        }

        var cellInfo = result.data;
        var value = null;
        var dataType = null;
        var isUrl = false;
        var isCached = false;
        var isDefault = false;
        if (cellInfo.value !== null || cellInfo.dataType !== null || cellInfo.isUrl || cellInfo.isCached) {
            value = cellInfo.value;
            dataType = cellInfo.dataType;
            isUrl = cellInfo.isUrl;
            isCached = cellInfo.isCached;
        } else { // use cube defaults if exist
            isDefault = true;
            isUrl = data.defaultCellValueUrl !== undefined;
            value = isUrl ? data.defaultCellValueUrl : data.defaultCellValue;
            dataType = data.defaultCellValueType;
            isCached = data.defaultCellValueCache;
        }
        // Set the cell value (String)
        var cellValue = value !== null && value !== undefined ? value : '';
        _editCellValue.val(cellValue);
        if (dataType === null || !dataType) {
            dataType = 'string';
        }

        // Set the correct entry in the drop-down
        if (isUrl) {
            _urlDropdown.val(dataType);
        } else {
            _valueDropdown.val(dataType);
        }

        // Choose the correct data type drop-down (show/hide the other)
        _urlDropdown.toggle(isUrl);
        _valueDropdown.toggle(!isUrl);

        // Set the URL check box
        _editCellRadioURL.find('input').prop('checked', isUrl);

        // Set the Cache check box state
        _editCellCache.find('input').prop('checked', isCached);

        enabledDisableCheckBoxes(); // reset for form
        _editCellModal.find('input,textarea,select').attr('disabled', !modifiable);
        if (modifiable) {
            _editCellCancel.show();
            _editCellClear.show();

            _editCellModal.one('shown.bs.modal', function () {
                if (_bufferText.trim() !== '') {
                    _editCellValue.val(isDefault ? _bufferText : (cellValue + _bufferText));
                } else if (isDefault) {
                    _editCellValue.select();
                }
            });
        } else {
            _editCellCancel.hide();
            _editCellClear.hide();
        }
        _editCellModal.modal('show');
    }

    function editCellClear() {
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.UPDATE_CELL, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), _cellId, null]);
        if (!result.status) {
            _cellId = null;
            nce.showNote('Unable to clear cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        delete data.cells[_tableCellId];
        editCellClose();
    }

    function editCellClose() {
        _cellId = null;
        _editCellModal.modal('hide');
        destroyEditor();
    }

    function editCellOK() {
        var appId = nce.getSelectedTabAppId();
        var modifiable = nce.checkPermissions(appId, cubeName, PERMISSION_ACTION.UPDATE);
        if (!modifiable) {
            editCellClose();
            return;
        }
        var cellInfo = {'@type':'com.cedarsoftware.ncube.CellInfo'};
        cellInfo.isUrl = _editCellRadioURL.find('input').prop('checked');
        cellInfo.value = _editCellValue.val();
        cellInfo.dataType = cellInfo.isUrl ? _urlDropdown.val() : _valueDropdown.val();
        cellInfo.isCached = _editCellCache.find('input').prop('checked');

        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.UPDATE_CELL, [appId, cubeName, _cellId, cellInfo]);
        if (!result.status) {
            nce.showNote('Unable to update cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        data.cells[_tableCellId] = {value:cellInfo.value};
        editCellClose();
        reload();
    }

    function onEditCellModalShown() {
        $('#editCellValue').focus();
    }

    function onEditCellRadioUrlChange() {
        var isUrl = _editCellRadioURL.find('input').is(':checked');
        _urlDropdown.toggle(isUrl);
        _valueDropdown.toggle(!isUrl);
    }

    function enabledDisableCheckBoxes() {
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
        _editCellCache.find('input').prop('disabled', !cacheEnabled);

        if (cacheEnabled) {
            _editCellCache.removeClass('disabled');
        } else {
            _editCellCache.addClass('disabled');
        }
    }

    // =============================================== End Cell Editing ================================================

    // ============================================== Begin Filtering ==================================================

    function filterSave() {
        saveFilters();
        reload();
    }

    function filterOpen() {
        var columnSelectList = [];
        var columns = axes[colOffset].columns;
        var columnKeys = Object.keys(columns);
        for (var i = 0, len = columnKeys.length; i < len; i++) {
            var colId = columnKeys[i];
            columnSelectList.push({key:colId, value:columns[colId].value});
        }

        var builderOptions = {
            title: 'Filter Data',
            instructionsTitle: 'Instructions - Filter Data',
            instructionsText: 'Select filters to apply to cell data for ncube.',
            columns: {
                isApplied: {
                    heading: 'Apply',
                    type: PropertyBuilder.COLUMN_TYPES.CHECKBOX,
                    default: true
                },
                column: {
                    heading: 'Column',
                    type: PropertyBuilder.COLUMN_TYPES.SELECT,
                    selectOptions: columnSelectList
                },
                comparator: {
                    heading: 'Comparator',
                    type: PropertyBuilder.COLUMN_TYPES.SELECT,
                    selectOptions: FILTER_COMPARATOR_LIST,
                    default: FILTER_COMPARATOR_LIST[0]
                },
                expressionValue: {
                    heading: 'Comparison Value',
                    type: PropertyBuilder.COLUMN_TYPES.TEXT
                },
                isIncludeAll: {
                    heading: 'Include Empty Cells',
                    type: PropertyBuilder.COLUMN_TYPES.CHECKBOX,
                    default: true
                }
            },
            afterSave: function() {
                filterSave();
                removeHotBeforeKeyDown();
            }
        };

        addHotBeforeKeyDown();
        PropertyBuilder.openBuilderModal(builderOptions, _filters);
    }

    // =============================================== End Filtering ===================================================

    // ============================================== Column Editing ===================================================

    function focusModal() {
        var el = $(document.activeElement);
        if (el.is('input')) {
            el.blur();
            el.closest('.modal').focus();
        }
        el = null;
    }

    function selectNextInput(relativeIdx) {
        var el = document.activeElement;
        var inputs = $(el).closest('.modal').find('input[type="text"]');
        var curIdx = inputs.index(el);
        var newIdx = curIdx + relativeIdx;
        if ((relativeIdx > 0 && newIdx < inputs.length) || (relativeIdx < 0 && newIdx >= 0)) {
            var newInput = $(inputs[newIdx]);
            newInput.focusin();
            newInput.select();
            newInput = null;
        }
        el = null;
        inputs = null;
    }

    function addColumnEditListeners() {
        _editColumnModal.on('keydown', function(e) {
            var keyCode = e.keyCode;
            var isTextInputTarget = $(e.target).is('input[type="text"]');
            if (e.metaKey || e.ctrlKey) {
                if (keyCode === KEY_CODES.V && !isTextInputTarget) {
                    editColPaste();
                }
                return;
            }
            if (keyCode === KEY_CODES.ENTER) {
                var el = document.activeElement;
                var inputs = $(el).closest('.modal').find('input[type="text"]');
                var idx = inputs.index(el);
                $(el).blur();
                editColAdd(null, idx);
                el = null;
                inputs = null;
                return;
            }
            if (isTextInputTarget) {
                if (keyCode === KEY_CODES.ARROW_UP) {
                    selectNextInput(-1);
                } else if (keyCode === KEY_CODES.ARROW_DOWN) {
                    selectNextInput(1);
                }
                return;
            }
            if (keyCode === KEY_CODES.DELETE) {
                focusModal();
                editColDelete();
            } else if (keyCode === KEY_CODES.ARROW_UP) {
                focusModal();
                editColUp();
            } else if (keyCode === KEY_CODES.ARROW_DOWN) {
                focusModal();
                editColDown();
            }
        });

        $('#editColAdd').on('click', editColAdd);
        $('#editColDelete').on('click', editColDelete);
        $('#editColUp').on('click', editColUp);
        $('#editColDown').on('click', editColDown);
        $('#editColumnsCancel').on('click', editColCancel);
        $('#editColumnsSave').on('click', editColSave);
    }

    function editColPaste() {
        _editColClipboard.val('');
        _editColClipboard.focus();

        setTimeout(function() {
            var content = _editColClipboard.val();
            if (content === null || content === '') {
                return;
            }
            var vals = content.split(/\t|\n/);
            for (var i = 0, len = vals.length; i < len; i++) {
                var addedColVal = vals[i];
                if (addedColVal !== '') {
                    editColAdd(addedColVal);
                }
            }
        },100);
    }

    function editColumns(axisName) {
        var appId = nce.getSelectedTabAppId();
        var modifiable = nce.checkPermissions(appId, cubeName + '/' + axisName, PERMISSION_ACTION.UPDATE);
        if (!modifiable) {
            nce.showNote('Columns cannot be edited.');
            return false;
        }

        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.GET_AXIS, [appId, cubeName, axisName]);
        var axis;
        if (result.status === true) {
            axis = result.data;
            if (!axis.columns) {
                axis.columns = [];
            }
            if (axis.defaultCol) {
                // Remove actual Default Column object (not needed, we can infer it from Axis.defaultCol field being not null)
                axis.columns.splice(axis.columns.length - 1, 1);
            }
        } else {
            nce.showNote("Could not retrieve axis: " + axisName + " for n-cube '" + nce.getSelectedCubeName() + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        sortColumns(axis);
        loadColumns(axis);
        if (axis.preferredOrder === 1) {
            $('#editColUp').show();
            $('#editColDown').show();
        } else {
            $('#editColUp').hide();
            $('#editColDown').hide();
        }
        $('#editColumnsLabel')[0].innerHTML = 'Edit ' + axisName;
        addHotBeforeKeyDown();
        _editColumnModal.modal();
    }

    function sortColumns(axis) {
        if (axis.preferredOrder === 1) {
            axis.columns.sort(function(a, b) {
                return a.displayOrder - b.displayOrder;
            });
        }
    }

    function getUniqueId() {
        return _colIds--;
    }

    function editColAdd(addedColVal, addedAtIndex) {
        var input = $('.editColCheckBox');
        var loc = addedAtIndex;
        if (loc === undefined || loc === null) {
            loc = -1;
            $.each(input, function (index, btn) {
                if ($(this).prop('checked')) {
                    loc = index;
                }
            });
        }
        var axis = _columnList.prop('model');
        var newCol = {
            '@type': 'com.cedarsoftware.ncube.Column',
            'value': addedColVal === undefined || addedColVal === null ? 'newValue' : addedColVal,
            'id': getUniqueId()
        };

        if (loc == -1 || axis.preferredOrder === 0) {
            axis.columns.push(newCol);
            loc = input.length - 1;
        } else {
            axis.columns.splice(loc + 1, 0, newCol);
        }
        loadColumns(axis);

        // Select newly added column name, so user can just type over it.
        input = _columnList.find('.form-control');
        input[loc + 1].select();
        input = null;
        axis = null;
        newCol = null;
    }

    function editColDelete() {
        var axis = _columnList.prop('model');
        var cols = axis.columns;
        var colsToDelete = [];
        $.each($('.editColCheckBox'), function (index, btn) {
            if ($(this).prop('checked')) {
                colsToDelete.push(index);
            }
        });

        // Walk through in reverse order, deleting from back to front so that
        // the correct elements are deleted.
        for (var i=colsToDelete.length - 1; i >= 0; i--) {
            cols.splice(colsToDelete[i], 1);
        }
        loadColumns(axis);
        axis = null;
        cols = null;
        colsToDelete = null;
    }

    function editColUp() {
        var axis = _columnList.prop('model');
        var cols = axis.columns;
        var input = $('.editColCheckBox');

        if (cols && cols.length > 0 && input[0].checked) {
            // Top one checked, cannot move any items up
            return;
        }

        for (var i=0; i < input.length - 1; i++) {
            var tag = input[i];
            cols[i].checked = tag.checked;
            if (!tag.checked) {
                var nextTag = input[i + 1];
                cols[i + 1].checked = nextTag.checked;
                if (nextTag.checked) {
                    tag.checked = true;
                    nextTag.checked = false;

                    var temp = cols[i];
                    cols[i] = cols[i + 1];
                    cols[i + 1] = temp;

                    cols[i].checked = true;
                    cols[i + 1].checked = false;
                }
            }
        }

        loadColumns(axis);
    }

    function editColDown() {
        var axis = _columnList.prop('model');
        var cols = axis.columns;
        var input = $('.editColCheckBox');

        if (cols && cols.length > 0 && input[cols.length - 1].checked) {
            // Bottom one checked, cannot move any items down
            return;
        }

        for (var i=input.length - 1; i > 0; i--) {
            var tag = input[i];
            cols[i].checked = tag.checked;
            if (!tag.checked) {
                var nextTag = input[i - 1];
                cols[i - 1].checked = nextTag.checked;
                if (nextTag.checked) {
                    tag.checked = true;
                    nextTag.checked = false;

                    var temp = cols[i];
                    cols[i] = cols[i - 1];
                    cols[i - 1] = temp;

                    cols[i].checked = true;
                    cols[i - 1].checked = false;
                }
            }
        }

        loadColumns(axis);
    }

    function editColCancel() {
        _editColumnModal.modal('hide');
        removeHotBeforeKeyDown();
        destroyEditor();
    }

    function editColSave() {
        var axis = _columnList.prop('model');
        _columnList.find('input[data-type=cond]').each(function(index, elem) {
            axis.columns[index].value = elem.value;
        });
        _columnList.find('input[data-type=name]').each(function(index, elem) {
            var col = axis.columns[index];
            if (col.metaProp) {
                col.metaProp.name = elem.value;
            }
        });
        axis.defaultCol = null;
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.UPDATE_AXIS_COLUMNS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), axis.name, axis.columns]);
        
        if (!result.status) {
            nce.showNote("Unable to update columns for axis '" + axis.name + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        var lowerAxisName = axis.name.toLowerCase();
        if (_hiddenColumns.hasOwnProperty(lowerAxisName)) {
            nce.showNote('Hidden column selections for axis ' + axis.name + ' removed.', 'Note', 2000);
            delete _hiddenColumns[lowerAxisName];
            storeHiddenColumns();
        }
        deleteSavedColumnWidths();
        editColCancel();
        reload();
    }

    function loadColumns(axis) {
        var insTitle = $('#editColInstTitle');
        var inst = $('#editColInstructions');
        if ('DISCRETE' == axis.type.name) {
            insTitle[0].textContent = 'Instructions - Discrete Column';
            inst[0].innerHTML = "<i>Discrete</i> column has a single value per column. Values are matched with '='. \
            Strings are matched case-sensitively.  Look ups are indexed and run \
            in <a href=\"http://en.wikipedia.org/wiki/Time_complexity\" target=\"_blank\">O(log n)</a>. \
        <ul><li>Examples: \
        <ul> \
        <li>Enter string values as is, no quotes: <code>OH</code></li> \
        <li>Valid number: <code>42</code></li> \
        <li>Valid date: <code>2015/02/14</code> (or <code>14 Feb 2015</code>, <code>Feb 14, 2015</code>, <code>February 14th, 2015</code>, <code>2015-02-14</code>)</li> \
        <li>Do not use mm/dd/yyyy or dd/mm/yyyy. \
        </li></ul></li></ul>";
        }
        else if ('RANGE' == axis.type.name) {
            insTitle[0].textContent = 'Instructions - Range Column';
            inst[0].innerHTML = "A <i>Range</i> column contains a <i>low</i> and <i>high</i> value.  It matches when \
            <i>value</i> is within the range: value >= <i>low</i> and value < <i>high</i>. Look ups are indexed \
            and run in <a href=\"http://en.wikipedia.org/wiki/Time_complexity\" target=\"_blank\">O(log n)</a>.\
        <ul><li>Enter low value, high value. Treated [inclusive, exclusive).</li> \
        <li>Examples: \
        <ul> \
        <li><i>Number range</i>: <code>25, 75</code> (meaning x >= 25 AND x < 75)</li> \
        <li><i>Number range</i>: <code>[100, 1000]</code> (brackets optional)</li> \
        <li><i>Date range</i>: <code>2015/01/01, 2017-01-01</code> (date >= 2015-01-01 AND date < 2017-01-01) \
        </li></ul></li></ul>";
        }
        else if ('SET' == axis.type.name) {
            insTitle[0].textContent = 'Instructions - Set Column';
            inst[0].innerHTML = "A <i>Set</i> column can contain unlimited discrete values and ranges. Discrete values \
            match with '=' and ranges match when value is within the range [inclusive, exclusive).  Overlapping\
            ranges and values are <b>not</b> allowed.  If you need that capability, use a <i>Rule</i> axis.\
            Look ups are indexed and run in <a href=\"http://en.wikipedia.org/wiki/Time_complexity\" target=\"_blank\">O(log n)</a>.\
        <ul><li>Examples: \
        <ul> \
        <li><i>Numbers</i>: <code>6, 10, [20, 30], 45</code></li> \
        <li><i>Strings</i>: <code>\"TX\", \"OH\", \"GA\"</code></li> \
        <li><i>Dates</i>: <code>\"2016-01-01\"</code></li> \
        <li><i>Date range</i>: <code>[\"2010/01/01\", \"2012/12/31\"]</code></li> \
        <li><i>Date ranges</i>: <code>[\"2015-01-01\", \"2016-12-31\"], [\"2019/01/01\", \"2020/12/31\"]</code> \
        </li></ul></li></ul>";
        }
        else if ('NEAREST' == axis.type.name) {
            insTitle[0].textContent = 'Instructions - Nearest Column';
            inst[0].innerHTML = "A <i>Nearest</i> column has a single value per column.  The <i>closest</i> column on the \
            axis to the passed in value is matched.  Strings are compared similar to spell-check \
            (See <a href=\"http://en.wikipedia.org/wiki/Levenshtein_distance\" target=\"_blank\">Levenshtein</a> algorithm). \
            Lat/Lon's column values are compared using earth curvature in distance calculation \
            (See <a href=\"http://en.wikipedia.org/wiki/Haversine_formula\" target=\"_blank\">Haversine</a> forumla). \
            Numbers compared using abs(column - value).  Look ups scan all columns and run in \
            <a href=\"http://en.wikipedia.org/wiki/Time_complexity\" target=\"_blank\">O(n)</a>. \
        <ul><li>Examples: \
        <ul> \
        <li>With columns <code>Alpha, Bravo, Charlie</code>, <i>value</i> <code>alfa</code> will match column <code>Alpha</code>.  It has the closest 'edit' distance.</li> \
        <li>With columns <code>1, 10, 100, 1000</code>, <i>value</i> <code>400</code> will match column <code>100</code>. (Distance of 300 is smallest).</li> \
        <li>Dates are entered in the same formats in Discrete column instructions (many formats supported).</li> \
        <li>Do not use mm/dd/yyyy or dd/mm/yyyy for dates.</li></ul></li></ul>";
        }
        else if ('RULE' == axis.type.name) {
            insTitle[0].textContent = 'Instructions - Rule Column';
            inst[0].innerHTML = "A <i>Rule condition</i> column is entered as a rule name and condition.  All rule conditions \
            that evaluate to <i>true</i> have their associated statement cells executed.  By default all <i>true</i> \
            conditions will fire. (See our definition of <a href=\"http://groovy.codehaus.org/Groovy+Truth\" target=\"_blank\">true</a>). \
            The Rule axis can be set so that only the first <i>true</i> condition fires.  When running a rule-cube, \
            if the name of a rule is bound to the rule axis, execution will start on the named rule.  A rule axis can \
            have a <i>Default</i> column. Just like all other axis types, at least one condition on a rule axis must fire, \
            otherwise a CoordinateNotFound exception will be thrown.  Look ups scan all columns (except when fire once is indicated) \
            and run in <a href=\"http://en.wikipedia.org/wiki/Time_complexity\" target=\"_blank\">O(n)</a>. \
        <ul><li>Notes: \
        <ul> \
        <li>Enter the [optional] rule name in the top line (no quotes).</li> \
        <li>Enter <i>condition</i> in <a href=\"http://groovy.codehaus.org/\" target=\"_blank\">Groovy</a> on the second line.</li> \
        <li>The <i>input</i> and <i>output</i> Maps and <i>ncube</i> are available in the condition and statements (cells).</li> \
        <li><i>Example condition</i>: <code>input.state == 'OH'</code></li> \
        </ul></li></ul>";
        }
        else {
            insTitle[0].textContent = 'Instructions';
            inst[0].innerHTML = 'Unknown axis type';
        }

        var axisList = axis.columns;
        _columnList.empty();
        _columnList.prop('model', axis);
        var displayOrder = 0;
        $.each(axisList, function (key, item) {
            if (!item.displayOrder || item.displayOrder < 2147483647) {   // Don't add default column in
                item.displayOrder = displayOrder++;
                var rowDiv = $('<div/>').prop({class: "row", "model": item});
                var div = $('<div/>').prop({class: "input-group"});
                var span = $('<span/>').prop({class: "input-group-addon"});
                var inputBtn = $('<input/>').prop({class: "editColCheckBox", "type": "checkbox"});
                if (item.checked === true) {
                    inputBtn[0].checked = true;
                }

                // For rules with URL to conditions, support URL: (or url:) in front of URL - then store as URL
                if (axis.type.name == 'RULE') {
                    if (!item.metaProps) {
                        item.metaProps = {"name": "Condition " + displayOrder};
                    }
                    var inputName = $('<input/>').prop({class: "form-control", "type": "text"});
                    inputName.attr({"data-type": "name"});
                    inputName.on('blur', function () {
                        item.metaProps.name = inputName.val();
                    });
                    inputName.val(item.metaProps.name);
                }

                var inputText = $('<input/>').prop({class: "form-control", "type": "text"});
                inputText.attr({"data-type":"cond"});
                inputText.on('blur', function() {
                    item.value = inputText.val();
                });

                var prefix = '';
                if (item.isUrl) {
                    prefix += 'url|';
                }
                if (item.isCached) {
                    prefix += 'cache|';
                }

                inputText.val(prefix + item.value);
                _columnList.append(rowDiv);
                rowDiv.append(div);
                div.append(span);
                span.append(inputBtn);
                if (axis.type.name == 'RULE') {
                    div.append(inputName);
                }
                div.append(inputText);
            }
        });
    }

    // =============================================== End Column Editing ==============================================

    // =============================================== Begin Column Hiding ==========================================

    function addColumnHideListeners() {
        $('#hideColumnsCancel').on('click', hideColCancel);
        $('#hideColumnsSave').on('click', hideColSave);
    }

    function hideColumns(axisName) {
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.GET_AXIS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), axisName]);
        var axis;
        if (result.status === true) {
            axis = result.data;
            if (!axis.columns) {
                axis.columns = [];
            }
            if (axis.defaultCol) {   // Remove actual Default Column object (not needed, we can infer it from Axis.defaultCol field being not null)
                axis.columns.splice(axis.columns.length - 1, 1);
            }
        } else {
            nce.showNote("Could not retrieve axis: " + axisName + " for n-cube '" + nce.getSelectedCubeName() + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        sortColumns(axis);
        loadHiddenColumns(axis);
        $('#hideColumnsLabel')[0].innerHTML = 'Hide ' + axisName + ' columns';
        addHotBeforeKeyDown();
        $('#hideColumnsModal').modal();
    }

    function loadHiddenColumns(axis) {
        var insTitle = $('#hideColInstTitle');
        var inst = $('#hideColInstructions');
        insTitle[0].textContent = 'Instructions - Hide Column';
        inst[0].innerHTML = "Select columns to show. Deselect columns to hide.";

        var axisList = axis.columns;
        var lowerAxisName = axis.name.toLowerCase();
        var defaultCol = axis.defaultCol;
        if (defaultCol !=null) {
            axisList.push(axis.defaultCol);
        }
        _hideColumnsList.empty();
        _hideColumnsList.prop('model', axis);
        var displayOrder = 0;
        $.each(axisList, function (key, item) {
            item.displayOrder = displayOrder++;
            var itemId = item.id;
            var listItem = $('<li/>').prop({class: "list-group-item skinny-lr no-margins"});
            var rowDiv = $('<div/>').prop({class: "container-fluid"});
            var rowLabel = $('<label/>').prop({class: "checkbox no-margins col-xs-10"});
            var ruleLabel = '';
            if (item.metaProps !=null) {
                ruleLabel = item.metaProps.name + ': ';
            }
            var labelText = item.value != null ? item.value : DEFAULT_TEXT;
            var label = ruleLabel + labelText;
            var inputBtn = $('<input/>').prop({class: "commitCheck", type: "checkbox"});
            inputBtn.attr("data-id", itemId);
            inputBtn[0].checked = !_hiddenColumns[lowerAxisName] || !_hiddenColumns[lowerAxisName][itemId];
            _hideColumnsList.append(listItem);
            listItem.append(rowDiv);
            rowDiv.append(rowLabel);
            rowLabel.append(inputBtn);
            rowLabel.append(label);
        });
    }

    function hideColCancel() {
        $('#hideColumnsModal').modal('hide');
        removeHotBeforeKeyDown();
    }

    function hideColSave() {
        var axis = _hideColumnsList.prop('model');
        var lowerAxisName = axis.name.toLowerCase();
        var columnIds = [];
        $('.commitCheck:not(:checked)').each(function () {
            var id = $(this).attr('data-id');
            columnIds.push(id);
        });
        if (columnIds.length == axis.columns.length)
        {
            nce.showNote('Please select at least one column to show.', 'Note', 2000);
            return;
        }
        delete _hiddenColumns[lowerAxisName];
        if (columnIds.length > 0)
        {
            _hiddenColumns[lowerAxisName] = {};
            for (var i = 0, len = columnIds.length; i < len; i++)
            {
                var columnId = columnIds[i];
                _hiddenColumns[lowerAxisName][columnId] = true;
            }
        }
        storeHiddenColumns();
        $('#hideColumnsModal').modal('hide');
        removeHotBeforeKeyDown();
        destroyEditor();
        reload();
    }

    function getSavedFilters() {
        var filters = localStorage[getStorageKey(nce, FILTERS)];
        return filters ? JSON.parse(filters) : [];
    }

    function clearFilters() {
        _filters = [];
        saveFilters();
    }

    function saveFilters() {
        saveOrDeleteValue(_filters, getStorageKey(nce, FILTERS));
    }

    function storeHiddenColumns() {
        saveOrDeleteValue(_hiddenColumns, getStorageKey(nce, HIDDEN_COLUMNS));
    }

    // =============================================== End Column Hiding ===============================================

    // =============================================== Begin Axis Ordering =============================================

    function moveAxis(fromIndex, toIndex) {
        axes.splice(toIndex, 0, axes.splice(fromIndex, 1)[0]);
        storeAxisOrder();
        deleteSavedColumnWidths();
        destroyEditor();
        reload();
    }

    function findIndexOfAxisName(axisName) {
        for (var i = 0, len = axes.length; i < len; i++) {
            if (axes[i].name.toLowerCase() === axisName.toLowerCase()) {
                return i;
            }
        }
        return -1;
    }

    function storeAxisOrder() {
        var order = [];
        for (var i = 0, len = axes.length; i < len; i++) {
            order.push(axes[i].name.toLowerCase());
        }
        localStorage[getStorageKey(nce, AXIS_ORDER)] = JSON.stringify(order);
    }

    // =============================================== End Axis Ordering ===============================================

    // =============================================== Begin Axis Editing ==============================================

    function addAxisEditListeners() {
        var axisTypes = {};

        _isRefAxis.on('change', function() {
            _refAxisGroup.toggle();

            var toggleVal = $(this)[0].checked;
            _addAxisTypeName.parent().find('button').prop('disabled', toggleVal);
            _addAxisValueTypeName.parent().find('button').prop('disabled', toggleVal);

            populateSelect(nce, _refAxisApp, CONTROLLER_METHOD.GET_APP_NAMES, [] , null, toggleVal);
            _refAxisAxis.empty();
        });
        _hasRefFilter.on('change', function() {
            _refFilterGroup.toggle();

            var toggleVal = $(this)[0].checked;
            populateSelect(nce, _refFilterApp, CONTROLLER_METHOD.GET_APP_NAMES, [], null, toggleVal);
            _refFilterMethod.empty();
        });

        _refAxisApp.on('change', function() {
            _refAxisVersion.empty();
            _refAxisCube.empty();
            _refAxisAxis.empty();
            populateSelect(nce, _refAxisVersion, CONTROLLER_METHOD.GET_APP_VERSIONS, [$(this).val(), _refAxisStatus.val()], null, true, true);
        });
        _refFilterApp.on('change', function() {
            _refFilterVersion.empty();
            _refFilterCube.empty();
            _refFilterMethod.empty();
            populateSelect(nce, _refFilterVersion, CONTROLLER_METHOD.GET_APP_VERSIONS, [$(this).val(), _refFilterStatus.val()], null, true, true);
        });

        _refAxisVersion.on('change', function() {
            _refAxisCube.empty();
            _refAxisAxis.empty();
            populateSelect(nce, _refAxisCube, CONTROLLER_METHOD.SEARCH, [appIdFrom(_refAxisApp.val(), $(this).val(), _refAxisStatus.val(), _refAxisBranch.val()), '*', null, true]);
        });
        _refFilterVersion.on('change', function() {
            _refFilterCube.empty();
            _refFilterMethod.empty();
            populateSelect(nce, _refFilterCube, CONTROLLER_METHOD.SEARCH, [appIdFrom(_refFilterApp.val(), $(this).val(), _refFilterStatus.val(), _refFilterBranch.val()), '*', null, true]);
        });

        _refAxisCube.on('change', function() {
            axisTypes = populateSelectFromCube(nce, _refAxisAxis, [appIdFrom(_refAxisApp.val(), _refAxisVersion.val(), _refAxisStatus.val(), _refAxisBranch.val()), _refAxisCube.val(), {mode:'json'}], POPULATE_SELECT_FROM_CUBE.AXIS);
        });
        _refFilterCube.on('change', function() {
            populateSelectFromCube(nce, _refFilterMethod, [appIdFrom(_refFilterApp.val(), _refFilterVersion.val(), _refFilterStatus.val(), _refFilterBranch.val()), _refFilterCube.val(), {mode:'json'}], POPULATE_SELECT_FROM_CUBE.METHOD);
        });

        _refAxisAxis.on('change', function() {
            var val = axisTypes[$(this).val()];
            $('#addAxisTypeName').val(val.axisType);
            $('#addAxisValueTypeName').val(val.valueType);
        });
    }

    function addAxis() {
        var modifiable = nce.checkPermissions(nce.getSelectedTabAppId(), cubeName + '/*', PERMISSION_ACTION.UPDATE);
        if (!modifiable) {
            nce.showNote('Axis cannot be added.');
            return;
        }

        buildDropDown('#addAxisTypeList', '#addAxisTypeName', AXIS_TYPE_LIST.TYPE, onAxisTypeChange);
        buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', AXIS_TYPE_LIST.GENERAL_SUBTYPE, function () { });
        _addAxisName.val('');
        $('#addAxisModal').modal();
        addHotBeforeKeyDown();
    }

    function onAxisTypeChange(selected) {
        if ('RULE' === selected) {
            buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', AXIS_TYPE_LIST.RULE_SUBTYPE, function () { });
            _addAxisValueTypeName.val(AXIS_SUBTYPES.EXPRESSION);
        } else {
            buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', AXIS_TYPE_LIST.GENERAL_SUBTYPE, function () { });
            _addAxisValueTypeName.val(AXIS_SUBTYPES.STRING);
        }
    }
    
    function addAxisClose() {
        $('#addAxisModal').modal('hide');
        removeHotBeforeKeyDown();
        destroyEditor();
    }

    function addAxisOk() {
        addAxisClose();
        var axisName = _addAxisName.val();
        var appId = nce.getSelectedTabAppId();
        var modifiable = nce.checkPermissions(appId, cubeName + '/' + axisName, PERMISSION_ACTION.UPDATE);
        if (!modifiable) {
            nce.showNote('Cannot add axis ' + axisName);
            return;
        }
        var params;
        if (_isRefAxis[0].checked) {
            var refAppId = appIdFrom(_refAxisApp.val(), _refAxisVersion.val(), _refAxisStatus.val(), _refAxisBranch.val());
            var refCubeName = _refAxisCube.val();
            var refAxisName = _refAxisAxis.val();
            var filterAppId = null;
            var filterCubeName = null;
            var filterMethodName = null;
            if (_hasRefFilter[0].checked) {
                filterAppId = appIdFrom(_refFilterApp.val(), _refFilterVersion.val(), _refFilterStatus.val(), _refFilterBranch.val());
                filterCubeName = _refFilterCube.val();
                filterMethodName = _refFilterMethod.val();
            }
            params = [refAppId, refCubeName, refAxisName, filterAppId, filterCubeName, filterMethodName];
        } else {
            var axisType = _addAxisTypeName.val();
            var axisValueType = _addAxisValueTypeName.val();
            params = [axisType, axisValueType];
        }
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.ADD_AXIS, [appId, cubeName, axisName].concat(params));
        if (result.status === true) {
            if (hasCustomAxisOrder()) {
                axes.splice(colOffset, 0, {name:axisName});
                storeAxisOrder();
            }
            deleteSavedColumnWidths();
            clearFilters();
            nce.loadCube();
        } else {
            nce.showNote("Unable to add axis '" + axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function deleteAxis(axisName) {
        var modifiable = nce.checkPermissions(nce.getSelectedTabAppId(), cubeName + '/' + axisName, PERMISSION_ACTION.UPDATE);
        if (!modifiable) {
            nce.showNote('Axis cannot be deleted.');
            return;
        }

        $('#deleteAxisName').val(axisName);
        $('#deleteAxisModal').modal();
        addHotBeforeKeyDown();
    }
    
    function deleteAxisClose() {
        $('#deleteAxisModal').modal('hide');
        removeHotBeforeKeyDown();
        destroyEditor();
    }

    function deleteAxisOk() {
        var axisName = $('#deleteAxisName').val();
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.DELETE_AXIS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), axisName]);
        if (result.status) {
            var lowerAxisName = axisName.toLowerCase();
            if (_hiddenColumns.hasOwnProperty(lowerAxisName))
            {
                nce.showNote('Hidden column selections for axis ' + axisName + ' removed.', 'Note', 2000);
                delete _hiddenColumns[lowerAxisName];
                storeHiddenColumns();
            }
            if (hasCustomAxisOrder()) {
                var order = JSON.parse(localStorage[getStorageKey(nce, AXIS_ORDER)]);
                axes.splice(order.indexOf(lowerAxisName), 1);
                storeAxisOrder();
            }
            clearFilters();
            deleteSavedColumnWidths();
            nce.loadCube();
        } else {
            nce.showNote("Unable to delete axis '" + axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
        deleteAxisClose();
    }

    function updateAxis(axisName) {
        var appId = nce.getSelectedTabAppId();
        var modifiable = nce.checkPermissions(appId, cubeName + '/' + axisName, PERMISSION_ACTION.UPDATE);
        _updateAxisModal.find('input').attr('disabled', !modifiable);
        if (modifiable) {
            $('#updateAxisOk').show();
        } else {
            $('#updateAxisOk').hide();
        }

        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.GET_AXIS, [appId, cubeName, axisName]);
        var axis;
        if (result.status === true) {
            axis = result.data;
        } else {
            nce.showNote("Could not retrieve axis: " + axisName + " for n-cube '" + nce.getSelectedCubeName() + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        var isRule = axis.type.name === 'RULE';
        var isNearest = axis.type.name === 'NEAREST';
        _updateAxisName.val(axisName);
        _updateAxisTypeName.val(axis.type.name);
        _updateAxisValueTypeName.val(axis.valueType.name);
        $('#updateAxisDefaultCol').prop({'checked': axis.defaultCol !== null});
        if (isRule) {
            hideAxisSortOption();
            showAxisDefaultColumnOption(axis);
            showAxisFireAllOption(axis);
        }
        else if (isNearest) {
            hideAxisSortOption();
            hideAxisDefaultColumnOption();
            hideAxisFireAllOption();
        }
        else {
            showAxisSortOption(axis);
            showAxisDefaultColumnOption(axis);
            hideAxisFireAllOption();
        }

        if (axis.isRef) {
            _updateAxisSortOrder.prop('disabled', true);
            var metaProps = axis.metaProps;
            _refAxisGroupUpdate.show();
            _isRefAxisUpdate[0].checked = true;
            if (metaProps.transformApp) {
                _refFilterGroupUpdate.show();
                _hasRefFilterUpdate[0].checked = true;
            } else {
                _refFilterGroupUpdate.hide();
                _hasRefFilterUpdate[0].checked = false;
                _refFilterGroupUpdate.find('input').val('');
            }
            _refAxisBranchUpdate.val(metaProps.referenceBranch);
            _refAxisStatusUpdate.val(metaProps.referenceStatus);
            _refAxisAppUpdate.val(metaProps.referenceApp);
            _refAxisVersionUpdate.val(metaProps.referenceVersion);
            _refAxisCubeUpdate.val(metaProps.referenceCubeName);
            _refAxisAxisUpdate.val(metaProps.referenceAxisName);
            _refFilterBranchUpdate.val(metaProps.transformBranch);
            _refFilterStatusUpdate.val(metaProps.transformStatus);
            _refFilterAppUpdate.val(metaProps.transformApp);
            _refFilterVersionUpdate.val(metaProps.transformVersion);
            _refFilterCubeUpdate.val(metaProps.transformCubeName);
            _refFilterMethodUpdate.val(metaProps.transformMethodName);
        } else {
            _refAxisGroupUpdate.hide();
            _refFilterGroupUpdate.hide();
            _isRefAxisUpdate[0].checked = false;
            _hasRefFilterUpdate[0].checked = false;
            _refAxisGroupUpdate.find('input').val('');
            _refFilterGroupUpdate.find('input').val('');
        }

        _axisName = axisName;
        _updateAxisModal.modal({
            keyboard: true
        });
    }

    function showAxisSortOption(axis) {
        $('#updateAxisSortOrderRow').show();
        _updateAxisSortOrder.prop({'checked': axis.preferredOrder === 0});
    }

    function hideAxisSortOption() {
        $('#updateAxisSortOrderRow').hide();
    }

    function showAxisDefaultColumnOption(axis) {
        $('#updateAxisDefaultColRow').show();
        $('#updateAxisDefaultCol').prop({'checked': axis.defaultCol !== null});
    }

    function hideAxisDefaultColumnOption() {
        $('#updateAxisDefaultColRow').hide();
    }

    function showAxisFireAllOption(axis) {
        $('#updateAxisFireAllRow').show();
        $('#updateAxisFireAll').prop({'checked': axis.fireAll === true});
    }

    function hideAxisFireAllOption() {
        $('#updateAxisFireAllRow').hide();
    }

    function updateAxisOk() {
        _updateAxisModal.modal('hide');
        var axisName = $('#updateAxisName').val();
        var hasDefault = $('#updateAxisDefaultCol').prop('checked');
        var sortOrder = _updateAxisSortOrder.prop('checked');
        var fireAll = $('#updateAxisFireAll').prop('checked');
        var result = nce.call(CONTROLLER + CONTROLLER_METHOD.UPDATE_AXIS, [nce.getSelectedTabAppId(), cubeName, _axisName, axisName, hasDefault, sortOrder, fireAll]);
        if (result.status === true) {
            var oldName = _axisName.toLowerCase();
            var newName = axisName.toLowerCase();
            if (oldName != newName)
            {
                _hiddenColumns[newName] = _hiddenColumns[oldName];
                delete _hiddenColumns[oldName];
                storeHiddenColumns();

                if (hasCustomAxisOrder()) {
                    var order = JSON.parse(localStorage[getStorageKey(nce, AXIS_ORDER)]);
                    axes[order.indexOf(oldName)].name = newName;
                    storeAxisOrder();
                }
            }
            nce.loadCube();
        } else {
            nce.showNote("Unable to update axis '" + _axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    // =============================================== End Axis Editing ================================================


    function render() {
        if (!hot) {
            return;
        }
        hot.render();
    }

    function reload(keepTable) {
        if (hot) {
            var selection = getSelectedCellRange();
            load(keepTable);

            if (selection) {
                hot.selectCell(selection.startRow, selection.startCol, selection.endRow, selection.endCol, true);
            } else {
                selectSavedOrDefaultCell();
            }
        }
    }

    function selectSavedOrDefaultCell() {
        if (!hot) {
            return;
        }
        
        var pos = nce.getViewPosition();
        var row, col, left, top;
        if (typeof pos === 'object') {
            row = pos.row;
            col = pos.col;
            left = pos.left;
            top = pos.top;
        } else {
            row = 2;
            col = axes.length === 1 ? 1 : colOffset;
        }
        
        hot.selectCell(row, col);
        var wth = $('.wtHolder')[0];
        wth.scrollLeft = left || 0;
        wth.scrollTop = top || 0;
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
            window.parent.frameLoaded();
        }, 1);
    }

    // API
    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load,
        reload: reload,
        render: render
    };

})(jQuery);

function tabActivated(info) {
    NCubeEditor2.init(info);
    NCubeEditor2.load();
}

function cubeSelected() {
    NCubeEditor2.handleCubeSelected();
}