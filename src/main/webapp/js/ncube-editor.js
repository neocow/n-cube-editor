/**
 * NCube Editor
 *     IDE for building and editing NCubes
 *
 * @author John DeRegnaucourt
 */

$(function ()
{
    var _padding = ["", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000", "0000000000" ];
    var _cubeList = {};
    var _apps = [];
    var _statuses = ['RELEASE', 'SNAPSHOT'];
    var _versions = [];
    var _selectedCubeName = localStorage[SELECTED_CUBE];
    var _selectedApp = localStorage[SELECTED_APP];
    var _selectedVersion = localStorage[SELECTED_VERSION];
    var _selectedTab = localStorage[SELECTED_TAB];
    var _testSelectionAnchor = -1;
    var _testData = null;
    var _selectedStatus = "SNAPSHOT";
    var _axisName;
    var _errorId = null;
    var _colIds = -1;   // Negative and gets smaller (to differentiate on server side what is new)
    var _columnList = $('#editColumnsList');
    var _editor;
    var _activeTab = 'ncubeTab';
    var _jsonEditorMode = 'format';
    var _cellId = null;
    var _uiCellId = null;
    var _urlDropdown = $('#datatypes-url');
    var _valueDropdown = $('#datatypes-value');
    var _editCellCache = $('#editCellCache');
    var _editCellValue = $('#editCellValue');
    var _editCellRadioURL = $('#editCellRadioURL');
    var _clipboard = $('#cell-clipboard');

    //  modal dialogs
    var _editCellModal = $('#editCellModal');
    var _addParameterModal = $('#addParameterModal');
    var _deleteParameterModal = $('#deleteParameterModal');
    var _createNewTestModal = $('#createNewTestModal');
    var _renameTestModal = $('#renameTestModal');
    var _duplicateTestModal = $('#duplicateTestModal');
    var _deleteTestModal = $('#deleteTestmodal');

    //  locations
    var _testResultsDiv = $('#testResultsDiv');
    var _testListWarning = $('#testListWarning');
    var _focusedElement = null;

    initialize();

    function initialize()
    {
        loadAppNames();
        loadVersions();
        loadNCubes();
        loadAppListView();
        loadStatusListView();
        loadVersionListView();
        loadNCubeListView();
        loadCube();

        $.fn.selectRange = function (start, end)
        {
            if (!end)
            {
                end = start;
            }
            return this.each(function ()
            {
                if (this.setSelectionRange)
                {
                    this.focus();
                    this.setSelectionRange(start, end);
                }
                else if (this.createTextRange)
                {
                    var range = this.createTextRange();
                    range.collapse(true);
                    range.moveEnd('character', end);
                    range.moveStart('character', start);
                    range.select();
                }
            });
        };
        var west = $('#west');
        var appListDiv = $('#app-list-div');
        var appListPanel = appListDiv.find('> .panel-body');
        appListPanel.height(75);

        var statListDiv = $('#status-list-div');
        var statListPanel = statListDiv.find('> .panel-body');
        statListPanel.height(36);

        var verListDiv = $('#version-list-div');
        var verListPanel = verListDiv.find('> .panel-body');
        verListPanel.height(60);

        var ncubeListPanel = $('#ncube-list-div').find('> .panel-body');

        var hApp = appListDiv.height();
        var hStat = statListDiv.height();
        var hVer = verListDiv.height();

        initJsonEditor();
        addListeners();

        var myLayout;
        var secondaryLayout;

        secondaryLayout = $('div#tests').layout({
            name: "secondaryLayout"
            ,   closable:					true	// pane can open & close
            ,	resizable:					true	// when open, pane can be resized
            ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
            ,	livePaneResizing:			true
            ,	east__minSize:				170
            ,	spacing_open:			5  // ALL panes
            ,	spacing_closed:			5 // ALL panes
            //            ,	south__spacing_open:			5  // ALL panes
            //,	south__spacing_closed:			5 // ALL panes
            ,  east__onresize: function()
            {
                calculateTestPanelSize();
            }
        });

        myLayout = $('body').layout({
            name:   "BodyLayout"
            //	reference only - these options are NOT required because 'true' is the default
            ,   closable:					true	// pane can open & close
            ,	resizable:					true	// when open, pane can be resized
            ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
            ,	livePaneResizing:			true

            //	some resizing/toggling settings
            //            ,	north__slidable:			false	// OVERRIDE the pane-default of 'slidable=true'
            //            ,	north__togglerLength_closed: '100%'	// toggle-button is full-width of resizer-bar
            //            ,	north__spacing_closed:		20		// big resizer-bar when open (zero height)
            //            ,	south__resizable:			false	// OVERRIDE the pane-default of 'resizable=true'
            //            ,	south__spacing_open:		0		// no resizer-bar when open (zero height)
            //            ,	south__spacing_closed:		20		// big resizer-bar when open (zero height)

            //	some pane-size settings
            //,	west__minSize:				100
            //            ,	east__size:					300
            //            ,	east__minSize:				200
            //            ,	east__maxSize:				.5 // 50% of layout width
            // ,   center__paneSelector: "#center"
            // ,   west__paneSelector: "#west"

            //	some pane animation settings
            ,	west__animatePaneSizing:	false
            ,   west__fxName_open:          "none"
            ,	west__fxName_close:			"none"	// NO animation when closing west-pane
            ,   spacing_open:         5
            ,   spacing_closed:       5
            ,   west__resizeable:           true
            ,   west__size:                 "auto"
            ,   west__minSize:              150
            //	enable showOverflow on west-pane so CSS popups will overlap north pane
            ,	west__showOverflowOnHover:	true
            ,   center__triggerEventsOnLoad: true
            //	enable state management
            ,	stateManagement__enabled:	false // automatic cookie load & save enabled by default
            //           ,	showDebugMessages:			true // log and/or display messages from debugging & testing code
            //,  west__onresize:		"secondaryLayout.resizeAll" // resize ALL visible layouts nested inside
            ,  west__onresize: function()
            {
                ncubeListPanel.height(west.height() - hApp - hStat - hVer - 110);
                secondaryLayout.resizeAll();
                //                _editor.resize();
            }
        });

        ncubeListPanel.height(west.height() - hApp - hStat - hVer - 110);
        $(document).on( 'shown.bs.tab', 'a[data-toggle="tab"]', function (e)
        {
            secondaryLayout.resizeAll();
            calculateTestPanelSize();
        });

        $(document).keydown(function(e)
        {
            var isModalDisplayed = $('body').hasClass('modal-open');

            if (_activeTab == 'ncubeTab' && !isModalDisplayed)
            {
                if (e.metaKey || e.ctrlKey)
                {   // Control Key (command in the case of Mac)
                    if (e.keyCode == 88 || e.keyCode == 67)
                    {   // Ctrl-C or Ctrl-X
                        editCutCopy(e.keyCode == 88);
                    }
                    else if (e.keyCode == 86)
                    {   // Ctrl-V
                        editPaste();
                    }
                }
            }
        });

        myLayout.resizeAll();
        openGroupContainingLastSelectedNCube();
        buildNCubeEditMenu();
    }

    function editPaste()
    {
        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            return;
        }
        var firstCell = $('td.cell-selected');
        if (!firstCell || firstCell.length < 1)
        {
            return;
        }
        var lastCell = $(firstCell[firstCell.length - 1]);
        firstCell = $(firstCell[0]);

        var table = $(".table-ncube")[0];
        var tableRows = table.rows;

        // Location of first selected cell in 2D spreadsheet view.
        var row = getRow(firstCell);
        var col = getCol(firstCell) - countTH(tableRows[row].cells);

        // Location of the last selected cell in 2D spreadsheet view.
        var lastRow = getRow(lastCell);
        var lastCol = getCol(lastCell) - countTH(tableRows[lastRow].cells);

        var onlyOneCellSelected = row == lastRow && col == lastCol;

        // Point focus to hidden text area so that it will receive the pasted content
        _focusedElement = $(':focus');
        _clipboard.focusin();
        _clipboard.select();

        var content = _clipboard.val();
        if (!content || content == "")
        {
            return;
        }

        // Parse the clipboard content and build-up coordinates where this content will be pasted.
        var values = [];
        var coords = [];
        var firstRow = row;
        var lines = content.split('\n');

        for (var i = 0; i < lines.length; i++)
        {
            if (lines[i] && lines[i] != "")
            {
                var strValues = lines[i].split('\t');
                values.push(strValues);
                var rowCoords = [];
                for (var j = 0; j < strValues.length; j++)
                {
                    var numTH = countTH(tableRows[row].cells);
                    var colIdx = col + j + numTH;
                    if (colIdx < tableRows[row].cells.length)
                    {   // Do attempt to read past edge of 2D grid
                        var domCell = tableRows[row].cells[colIdx]; // This is a DOM "TD" element
                        var jqCell = $(domCell);                    // Now it's a jQuery object.
                        rowCoords[j] = getCellId(jqCell);
                    }
                }
                coords.push(rowCoords);
                row++;

                if (row >= tableRows.length)
                {   // Do not go past bottom of grid
                    break;
                }
            }
        }

        // If more than one cell is selected, create coords for all selected cells.
        // Server will repeat values, properly throughout the selected 'clip' region.
        if (!onlyOneCellSelected)
        {
            coords = [];
            row = firstRow;
            for (var r = firstRow; r <= lastRow; r++)
            {
                rowCoords = [];
                for (var c = col; c <= lastCol; c++)
                {
                    numTH = countTH(tableRows[row].cells);
                    domCell = tableRows[row].cells[c + numTH]; // This is a DOM "TD" element
                    jqCell = $(domCell);                    // Now it's a jQuery object.
                    rowCoords[c - col] = getCellId(jqCell);
                }
                coords.push(rowCoords);
                row++;
            }
        }

        // Paste cells from database
        var result = call("ncubeController.pasteCells", [_selectedApp, _selectedVersion, _selectedCubeName, values, coords]);

        if (result.status)
        {
            reloadCube();
        }
        else
        {
            clearError();
            _errorId = showNote('Error pasting cells:<hr class="hr-small"/>' + result.data);
        }
    }

    function reloadCube()
    {
        var doc = document.documentElement;
        var left = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0);
        var top = (window.pageYOffset || doc.scrollTop) - (doc.clientTop || 0);
        loadCube();
        window.scrollTo(left, top);
    }

    function getCellId(cell)
    {
        var cellId = cell.attr('data-id');
        if (cellId)
        {
            return cellId.split("_");
        }
        return null;
    }

    function editCutCopy(isCut)
    {
        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            return;
        }
        _focusedElement = $(':focus');
        var cells = [];
        var lastRow = -1;
        var clipData = "";

        $('td.cell-selected').each(function ()
        {   // Visit selected cells in spreadsheet
            var cell = $(this);
            var cellRow = getRow(cell);
            if (lastRow == cellRow)
            {
                clipData += '\t';
            }
            else
            {
                if (lastRow != -1) clipData += '\n';
                lastRow = cellRow;
            }
            clipData = clipData + cell.text();
            var cellId = getCellId(cell);
            if (cellId)
            {
                cells.push(cellId);
            }
            if (isCut)
            {
                cell.empty();
            }
        });
        clipData += '\n';
        _clipboard.val(clipData);
        _clipboard.focusin();
        _clipboard.select();

        // Clear cells from database
        if (isCut)
        {
            var result = call("ncubeController.clearCells", [_selectedApp, _selectedVersion, _selectedStatus, _selectedCubeName, cells]);
            if (!result.status)
            {
                _errorId = showNote('Error cutting cells:<hr class="hr-small"/>' + result.data);
            }
        }
    }

    function buildNCubeEditMenu()
    {
        $('#edit-cut-cells').click(function ()
        {
            editCutCopy();
        });
        $('#edit-copy-cells').click(function()
        {
            console.log('copy');
        });
        $('#edit-paste-cells').click(function()
        {
            console.log('paste');
        });
        $('#edit-set-cells').click(function()
        {
            console.log('set-cells');
        });
    }

    function calculateTestPanelSize()
    {
        var east = $('#testLayoutEast');
        var testList = $('#testList').find('> .panel-body');
        testList.height(east.height() - 47);
    }

    function openGroupContainingLastSelectedNCube()
    {
        if (!_selectedCubeName)
        {
            return;
        }
        // Re-open Group containing last-selected cube name
        var groupNode = $("[itemname='" + _selectedCubeName + "']");
        if (groupNode)
        {
            groupNode = groupNode.closest('div.accordion-body');
            if (groupNode)
            {
                groupNode.collapse(true);
            }
        }
    }

    function initJsonEditor()
    {
        // create the editor
        var container = document.getElementById('jsoneditor');
        // TODO: Uncomment 'modes:' below when you figure out how to detect when the drop-down changes, so that
        // you can add back the Save button and remove the 'PoweredBy'

        var options =
        {
            mode: 'code',
            //            modes:['code','tree','view','form','text'],
            change: function()
            {
                setDirtyStatus(true);
            }
        };

        // Create JSON Editor (http://jsoneditoronline.org/downloads/)
        _editor = new JSONEditor(container, options);
        var editCtrl = $('#jsoneditor');
        var menu = editCtrl.find('.menu');
        var save = $("<button/>").attr({id:'saveButton', style:'background-image:none;width:64px',title:'Save changes'});
        save.html('Save');
        menu.append(save);           // Add 'Save' button to toolbar
        menu.find('a').remove();     // Get rid of 'Powered by Ace' link
    }

    function setDirtyStatus(dirty)
    {
        var saveButton = $('#saveButton');
        saveButton.prop('disabled', !dirty);
        if (dirty === true)
        {
            saveButton.html('Save*');
        }
        else
        {
            saveButton.html('Save');
        }
    }

    function getDirtyStatus()
    {
        var saveButton = $('#saveButton');
        return !saveButton.prop('disabled');
    }

    function addListeners()
    {
        var editor = $('#jsoneditor');
        editor.find(".format").click(function()
        {
            _jsonEditorMode = 'format';
        });

        editor.find('.compact').click(function()
        {
            _jsonEditorMode = 'compact';
        });

        var saveButton = $('#saveButton');
        saveButton.click(function()
        {
            clearError();
            var result = call("ncubeController.saveJson", [_selectedCubeName, _selectedApp, _selectedVersion, _editor.getText()]);
            if (result.status === true)
            {
                setDirtyStatus(false);
                loadNCubes();
                loadNCubeListView();
                loadCube();
            }
            else
            {
                _errorId = showNote('Error saving JSON n-cube:<hr class="hr-small"/>' + result.data);
            }
        });

        $('#ncubeTab').click(function ()
        {
            clearError();
            $('#EditMenu').show();
            _activeTab = 'ncubeTab';
            loadCube();
        });

        $('#jsonTab').click(function ()
        {
            clearError();
            $('#EditMenu').hide();
            _activeTab = "jsonTab";
            loadCube();
        });

        $('#detailsTab').click(function ()
        {
            clearError();
            $('#EditMenu').hide();
            _activeTab = "detailsTab";
            loadCube();
        });

        $('#testTab').click(function()
        {
            clearError();
            $('#EditMenu').hide();
            _activeTab = "testTab";
            loadCube();
        });

        $('#picTab').click(function()
        {
            clearError();
            $('#EditMenu').hide();
            _activeTab = "picTab";
            loadCube();
        });

        $('#newCubeMenu').click(function ()
        {
            newCube()
        });
        $('#newCubeSave').click(function ()
        {
            newCubeSave()
        });
        $('#renameCubeMenu').click(function ()
        {
            renameCube();
        });
        $('#renameCubeOk').click(function ()
        {
            renameCubeOk();
        });
        $('#renameTestOk').click(function ()
        {
            renameTestOk();
        });
        $('#dupeCubeMenu').click(function ()
        {
            dupeCube()
        });
        $('#dupeCubeCopy').click(function ()
        {
            dupeCubeCopy()
        });
        $('#deleteCubeMenu').click(function ()
        {
            deleteCube()
        });
        $('#deleteCubeOk').click(function ()
        {
            deleteCubeOk()
        });
        $('#restoreCubeMenu').click(function ()
        {
            restoreCube()
        });
        $('#restoreCubeOk').click(function ()
        {
            restoreCubeOk()
        });
        $('#revisionHistoryMenu').click(function ()
        {
            revisionHistory()
        });
        $('#revisionHistoryOk').click(function ()
        {
            revisionHistoryOk()
        });
        $('#deleteParameterOk').click(function ()
        {
            deleteParameterOk();
        });
        $('#addParameterMenu').click(function ()
        {
            addParameterMenu();
        });
        $('#addAssertionMenu').click(function ()
        {
            addNewAssertion();
        });
        $('#addParameterOk').click(function ()
        {
            addParameterOk();
        });
        $('#showRefsToMenu').click(function ()
        {
            showRefsToCube()
        });
        $('#showRefsToClose').click(function ()
        {
            showRefsToCubeClose()
        });
        $('#showRefsFromMenu').click(function ()
        {
            showRefsFromCube()
        });
        $('#showRefsFromClose').click(function ()
        {
            showRefsFromCubeClose()
        });
        $('#showReqScopeMenu').click(function ()
        {
            showReqScope()
        });
        $('#showReqScopeClose').click(function ()
        {
            showReqScopeClose()
        });
        $('#releaseCubesMenu').click(function ()
        {
            releaseCubes()
        });
        $('#releaseCubesOk').click(function ()
        {
            releaseCubesOk()
        });
        $('#changeVerMenu').click(function ()
        {
            changeVersion()
        });
        $('#changeVerOk').click(function ()
        {
            changeVersionOk()
        });
        $('#addAxisOk').click(function ()
        {
            addAxisOk()
        });
        $('#deleteAxisOk').click(function ()
        {
            deleteAxisOk()
        });
        $('#updateAxisMenu').click(function ()
        {
            updateAxis()
        });
        $('#updateAxisOk').click(function ()
        {
            updateAxisOk()
        });
        $('#editColSelectAll').click(function ()
        {
            editColSelect(true)
        });
        $('#editColSelectNone').click(function ()
        {
            editColSelect(false)
        });
        $('#editColAdd').click(function ()
        {
            editColAdd()
        });
        $('#editColDelete').click(function ()
        {
            editColDelete()
        });
        $('#editColUp').click(function ()
        {
            editColUp()
        });
        $('#editColDown').click(function ()
        {
            editColDown()
        });
        $('#editColumnsCancel').click(function ()
        {
            editColCancel()
        });
        $('#editColumnsSave').click(function ()
        {
            editColSave()
        });
        $('#createTestsLink').click(function ()
        {
            createNewTestMenu();
        });
        $('#generateTestsLink').click(function ()
        {
            loadTestListView("ncubeController.generateTests")
        });
        $('#editCellClear').click(function()
        {
            editCellClear();
        });
        $('#editCellCancel').click(function()
        {
            editCellCancel()
        });
        $('#editCellOk').click(function()
        {
            editCellOK()
        });
        $('#dataChangeType').click(function()
        {
            changeType();
        });
        $('#clearCache').click(function()
        {
            clearCache();
        });

        //  Set focused field when dialog appears so user can just start typing.
        _addParameterModal.on('shown.bs.modal', function () {
            $('#addParameterField').focus();
        });

        _createNewTestModal.on('shown.bs.modal', function () {
            $('#createNewTestField').focus();
        });

        _renameTestModal.on('shown.bs.modal', function () {
            $('#renameTestNewName').focus();
        });

        _duplicateTestModal.on('shown.bs.modal', function () {
            $('#duplicateTestNameField').focus();
        });

        //  Set default button clicked when <enter> is hit.
        _addParameterModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#addParameterOk').click();
            }
        });

        _renameTestModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#renameTestOk').click();
            }
        });

        _deleteParameterModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#deleteParameterOk').click();
            }
        });

        _duplicateTestModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#duplicateCurrentTestOk').click();
            }
        });

        _deleteTestModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#deleteTestOk').click();
            }
        });

        _createNewTestModal.on( 'keypress', function( e ) {
            if( e.keyCode === 13 ) {
                e.preventDefault();
                $('#addParameterOk').click();
            }
        });

        $('#runAllTestsMenu').click(function ()
        {
            $(this).css({'cursor':'wait'});
            runAllTests();
            $(this).css({'cursor':'default'})
        });

        $('#runAllTestsButton').click(function ()
        {
            $(this).css({'cursor':'wait'});
            runAllTests();
            $(this).css({'cursor':'default'})
        });

        $('#runCurrentTestMenu').click(function ()
        {
            $(this).css({'cursor':'wait'});
            runCurrentTest();
            $(this).css({'cursor':'default'})
        });

        $('#runTestButton').click(function ()
        {
            $(this).css({'cursor':'wait'});
            runCurrentTest();
            $(this).css({'cursor':'default'})
        });

        $('#runTestMenu').click(function ()
        {
            $(this).css({'cursor':'wait'});
            runCurrentTest();
            $(this).css({'cursor':'default'})
        });

        $('#renameCurrentTestMenu').click(function ()
        {
            renameCurrentTestMenu();
        });

        $('#renameTestMenu').click(function ()
        {
            renameCurrentTestMenu();
        });

        $('#deleteCurrentTestMenu').click(function ()
        {
            deleteCurrentTestMenu();
        });

        $('#deleteTestMenu').click(function ()
        {
            deleteCurrentTestMenu();
        });

        $('#deleteTestOk').click(function ()
        {
            deleteTestOk();
        });

        $('#createNewTestMenu').click(function ()
        {
            createNewTestMenu();
        });

        $('#createNewTestOk').click(function ()
        {
            createNewTestOk();
        });

        $('#duplicateCurrentTestMenu').click(function ()
        {
            duplicateCurrentTestMenu();
        });

        $('#duplicateTestMenu').click(function ()
        {
            duplicateCurrentTestMenu();
        });

        $("#duplicateCurrentTestOk").click(function ()
        {
            duplicateCurrentTestOk();
        });

        $('#deleteAllTestsMenu').click(function ()
        {
            deleteAllTestsMenu();
        });

        $('#deleteAllTestsOk').click(function ()
        {
            deleteAllTestsOk();
        });

        _editCellRadioURL.change(function()
        {
            var isUrl = _editCellRadioURL.find('input').is(':checked');
            _urlDropdown.toggle(isUrl);
            _valueDropdown.toggle(!isUrl);
            showHideCacheCheckbox(isUrl);
        });
        _urlDropdown.change(function()
        {
            var isUrl = _editCellRadioURL.find('input').is(':checked');
            showHideCacheCheckbox(isUrl)
        });
    }

    function showHideCacheCheckbox(isUrl)
    {
        var selDataType = _urlDropdown.val();
        var isGroovy = selDataType == 'exp' || selDataType == 'method';
        _editCellCache.toggle(!isGroovy && isUrl);
    }

    function loadAppListView()
    {
        $('#appCount').html(_apps.length);
        var list = $('#app-list');
        list.empty();
        $.each(_apps, function (index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function ()
            {
                var appName = anchor.text();
                setListSelectedStatus(appName, '#app-list');
                localStorage[SELECTED_APP] = appName;
                _selectedApp = appName;
                loadVersions();
                loadVersionListView();
                loadNCubes();
                loadNCubeListView();
                loadCube();
            });
            anchor.html(value);
            if (value == _selectedApp)
            {
                anchor.attr('class', 'ncube-selected');
            }
            else
            {
                anchor.attr('class', 'ncube-notselected');
            }
            li.append(anchor);
            list.append(li);
        });
        setListSelectedStatus(_selectedApp, '#app-list');
    }

    function loadTestListView(funcName)
    {
        _testData = null;
        _testSelectionAnchor = -1;

        enableTestItems();

        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            _editor.setText('No n-cube to load');
            setDirtyStatus(false);
            return;
        }

        var testListResult = call(funcName, [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);

        if (testListResult.status === true)
        {
            _testData = testListResult.data;
            _testSelectionAnchor = 0;
            refreshTestList();
        }
        else {
            var msg = 'Error fetching test data for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + ')';
            if (testListResult.data != null) {
                msg += (':<hr class="hr-small"/>' + testListResult.data);
            }
            _errorId = showNote(msg);
        }
    }

    function refreshTestList() {

        var testListItems = $('#testListItems');
        testListItems.empty();

        if (_testData != null && _testData.length > 0) {
            _testListWarning.hide();
            $("#testCount").html(_testData.length);

            $.each(_testData, function (index, value) {
                var anchor = $("<a/>").attr({'class':'list-group-item'});
                anchor.html('<span class="glyphicon" style="vertical-align: -1px;"></span>&nbsp;&nbsp;' + value['name']);

                if (index == _testSelectionAnchor) {

                    anchor.toggleClass("selected");
                }

                anchor.click(function(e) {

                        var link = $(e.currentTarget);
                        _testSelectionAnchor = index;
                        clearTestSelection();
                        link.addClass("selected");
                        enableTestItems();
                        loadTestView();
                    }
                );
                testListItems.append(anchor);
            });
            $('#testList').fadeIn("fast");
        } else {
            $('#testList').hide();
            _testListWarning.fadeIn("fast");
        }
        loadTestView();
        enableTestItems();
    }

    function loadStatusListView()
    {
        var list = $('#status-list');
        list.empty();
        $.each(_statuses, function (index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function ()
            {
                var status = anchor.text();
                setListSelectedStatus(status, '#status-list');
                _selectedStatus = status;
                loadVersions();
                loadVersionListView();
                loadNCubes();
                loadNCubeListView();
                loadCube();
            });
            anchor.html(value);
            if (value == _selectedStatus)
            {
                anchor.attr('class', 'ncube-selected');
            }
            else
            {
                anchor.attr('class', 'ncube-notselected');
            }
            li.append(anchor);
            list.append(li);
        });
        setListSelectedStatus(_selectedStatus, '#status-list');
    }

    function loadVersionListView()
    {
        $('#verCount').html(_versions.length);
        var list = $('#version-list');
        list.empty();
        $.each(_versions, function (index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function ()
            {
                var version = anchor.text();
                setListSelectedStatus(version, '#version-list');
                localStorage[SELECTED_VERSION] = version;
                _selectedVersion = version;
                loadNCubes();
                loadNCubeListView();
                loadCube();
            });
            anchor.html(value);
            if (value == _selectedVersion)
            {
                anchor.attr('class', 'ncube-selected');
            }
            else
            {
                anchor.attr('class', 'ncube-notselected');
            }
            li.append(anchor);
            list.append(li);
        });
        setListSelectedStatus(_selectedVersion, '#version-list');
    }

    function clearTestSelection()
    {
        $("#testListItems").find("a.selected").each(function (index, elem)
        {
            $(elem).removeClass("selected");
        });
        enableTestItems();
    }

    function selectAllTests()
    {
        $("#testListItems").find("a").each(function (index, elem)
        {
            var a = $(elem);
            var span = a.find("span");
            a.addClass("selected");
        });
        enableTestItems();
    }

    function enableTestItems() {
        var count = $("#testListItems").find("a.selected").length;

        $("#renameCurrentTestMenu").parent().toggleClass('disabled', count != 1);
        $("#runCurrentTestMenu").parent().toggleClass('disabled', count != 1);
        $("#duplicateCurrentTestMenu").parent().toggleClass('disabled', count != 1);
        $("#deleteCurrentTestMenu").parent().toggleClass('disabled', count != 1);

        $("#renameTestMenu").parent().toggleClass('disabled', count != 1);
        $("#duplicateTestMenu").parent().toggleClass('disabled', count != 1);
        $("#runTestMenu").parent().toggleClass('disabled', count != 1);
        $("#deleteTestMenu").parent().toggleClass('disabled', count != 1);

        if (count != 1) {
            clearTestView();
        }
    }

    function findTestByName(name) {
        if (_testData == null) {
            return null;
        }

        for (var i=0; i<_testData.length; i++) {
            if (name == _testData[i]["name"]) {
                return _testData[i];
            }
        }
        return null;
    }

    function createNewTestMenu() {
        clearError();

        if ($('#createNewTestMenu').parent().hasClass('disabled')) {
            return;
        }

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube is currently selected. You cannot create a test.');
            return;
        }

        $('#createNewTestField').val('');
        _createNewTestModal.modal({
            keyboard: true
        });
    }

    function deleteAllTestsMenu() {
        clearError();

        if ($('#deleteAllTestsMenu').parent().hasClass('disabled')) {
            return;
        }

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube is currently selected. You cannot delete all tests.');
            return;
        }

        $('#deleteAllTestsModal').modal({
            keyboard: true
        });
    }

    function deleteAllTestsOk() {
        $('#deleteAllTestsModal').modal('hide');

        _testData = [];
        _testSelectionAnchor = -1;

        saveAllTests(true);
        refreshTestList();
    }

    function createNewTestOk() {
        var newName = $('#createNewTestField').val().trim();

        if (findTestByName(newName) != null)
        {
            _errorId = showNote('There is already a test named \'' + newName + '\'.  Please choose a new name.');
            return;
        }

        var result = call("ncubeController.createNewTest", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, newName]);

        if (result.status === true)
        {
            if (_testData == null) {
                _testData = [];
            }
            _testData.push(result.data);
            _testSelectionAnchor = _testData.length-1;

            refreshTestList();
            saveAllTests(true);

            _createNewTestModal.modal('hide');

            $('#testList div.panel-body').animate({
                scrollTop: $('#testListItems a.selected').offset().top
            }, 200);
        }
        else {
            var msg = 'Error creating new test for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + ')';
            if (result.data != null) {
                msg += (':<hr class="hr-small"/>' + result.data);
            }
            _errorId = showNote(msg);
        }
    }

    function duplicateCurrentTestMenu() {
        clearError();

        if ($('#duplicateCurrentTestMenu').parent().hasClass('disabled')) {
            return;
        }

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube is currently selected. There is no test to duplicate.');
            return;
        }

        var list = getSelectedTestList();

        if (list.length != 1)
        {
            if (list.length == 0) {
                _errorId = showNote('No test is currently selected. Select a test to duplicate.');
            } else {
                _errorId = showNote('More than one test is selected. There can only be one test selected to duplicate.');
            }
            return;
        }

        $('#duplicateTestTitle').html('Duplicate \'' + $(list[0]).text().trim() + '\' ?');
        $('#duplicateTestNameField').val('');
        $('#duplicateTestModal').modal();
    }

    function getSelectedTestList() {
        return $("#testListItems a.selected");
    }

    function getSelectedTestCount() {
        return getSelectedTestList().length;
    }

    function getSelectedTestFromModel() {
        var list = getSelectedTestList();
        return findTestByName($(list[0]).text().trim());
    }

    function duplicateCurrentTestOk() {
        // see if name already exists in tests?

        var newName = $('#duplicateTestNameField').val().trim();

        if (findTestByName(newName) != null)
        {
            _errorId = showNote('There is already a test named \'' + newName + '\'.  Please choose a new name.');
            return;
        }

        $('#duplicateTestModal').modal('hide');

        var newTest = duplicateTest(getSelectedTestFromModel(), newName);

        // scroll to item we just added.
        _testData.push(newTest);
        _testSelectionAnchor = _testData.length-1;
        refreshTestList();
        saveAllTests(true);

        $('#testList div.panel-body').animate({
            scrollTop: $('#testListItems a.selected').offset().top
        }, 200);
    }

    function duplicateTest(test, newTestName) {
        var newTest = {};
        newTest["@type"] = "com.cedarsoftware.ncube.NCubeTest";
        newTest["name"] = newTestName;

        var parameters = [];

        $.each(test["coord"], function(index, item) {
            var pair = {};

            pair["key"] = item["key"];

            var cell = item["value"];
            var newCell = {};

            if (!cell) {
                pair["value"] = null;
            } else {
                $.each(cell, function(key, value) {
                    newCell[key] = value;
                });
                pair["value"] = newCell;
            }

            parameters.push(pair);
        });

        newTest["coord"] = parameters;

        var result = [];

        $.each(test["expected"], function(index, item) {
            var newCell = {};

            if (!item) {
                newCell = null;
            } else {
                $.each(item, function (key, value) {
                    newCell[key] = value;
                });
            }

            result.push(newCell);
        });

        newTest["expected"] = result;

        return newTest;
    }

    function loadNCubeListView()
    {
        $('#ncubeCount').html(Object.keys(_cubeList).length);
        var groupList = $('#ncube-list');
        groupList.empty();
        $.each(_cubeList, function (key, value)
        {
            var groupName = value['group'];
            var groupNode = $('#ac-' + groupName);
            if (groupNode.length == 0)
            {
                groupNode = $('<div/>').attr({class: 'accordion-group', 'id': 'ac-' + groupName});
                var heading = $("<div/>").attr({class: 'accordion-heading'});
                groupNode.append(heading);
                var anchor = $('<a/>').attr({class: 'accordion-toggle icon-folder-open cube-list-font', 'data-toggle': 'collapse', 'data-parent': 'ncube-list', 'href': '#grp-' + groupName});
                anchor.html(groupName);
                heading.append(anchor);
                groupList.append(groupNode);
                var accordionBody = $('<div/>').attr({class:'accordion-body collapse', 'id':'grp-' + groupName});
                var bodyInner = $('<div/>').attr({class:'accordion-inner'});
                var ul1 = $('<ul/>').attr({class:'nav nav-list'});
                bodyInner.append(ul1);
                accordionBody.append(bodyInner);
                groupNode.append(accordionBody);
                groupList.append(groupNode);
            }

            var ul = groupNode.find('ul');
            var li = $('<li/>');
            var a = $('<a/>').attr({class:'ncube-notselected', 'href':'#','itemName':value['ncube'].name}).html(getSmallCubeName(value));
            a.click(function ()
            {
                clearError();
                var cubeName = a.attr('itemName');
                localStorage[SELECTED_CUBE] = cubeName;
                _selectedCubeName = cubeName;
                loadCube(); // load spreadsheet side
            });
            ul.append(li);
            li.append(a);
            if (value['ncube'].name == _selectedCubeName)
            {
                a.attr('class', 'ncube-selected');
            }
            else
            {
                a.attr('class', 'ncube-notselected');
            }
        });
    }

    function getSmallCubeName(cubeInfo)
    {
        var prefix = cubeInfo['prefix'];
        return cubeInfo['ncube'].name.substring(prefix.length);
    }

    function loadCubeHtml()
    {
        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            $('#ncube-content').html('No n-cubes to load');
            return;
        }
        var result = call("ncubeController.getHtml", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            $('#ncube-content').html(result.data);

            // Disallow any selecting within the table
            var table = $('table');
            table.addClass('noselect');
            table.find('th').addClass('noselect');
            table.find('tr').addClass('noselect');
            var td = table.find('td');
            td.addClass('noselect');

            $(".axis-menu").each(function ()
            {
                var element = $(this);
                var axisName = element.attr('data-id');
                var ul = $('<ul/>').prop({'class': 'dropdown-menu', 'role': 'menu'});
                var li = $('<li/>');
                var an = $('<a href="#">');
                an.html("Update Axis...");
                an.click(function ()
                {
                    updateAxis(axisName)
                });
                li.append(an);
                ul.append(li);
                li = $('<li/>');
                an = $('<a href="#">');
                an.html("Add Axis...");
                an.click(function ()
                {
                    addAxis();
                });
                li.append(an);
                ul.append(li);
                li = $('<li/>');
                an = $('<a href="#">');
                an.html("Delete Axis...");
                an.click(function ()
                {
                    deleteAxis(axisName)
                });
                li.append(an);
                ul.append(li);
                li = $('<div/>').prop({'class': 'divider'});
                ul.append(li);
                li = $('<li/>');
                an = $('<a href="#">');
                an.html("Edit " + axisName + " columns...");
                an.click(function ()
                {
                    editColumns(axisName)
                });
                li.append(an);
                ul.append(li);
                element.append(ul);
            });
        }
        else
        {
            $('#ncube-content').empty();
            _errorId = showNote('Unable to load ' + _selectedCubeName + ':<hr class="hr-small"/>' + result.data);
        }

        $('.column').each(function ()
        {
            $(this).dblclick(function()
            {   // On double click, bring up column value editor modal
                var col = $(this);
                editColumns(col.attr('data-axis'));
            });
        });

        $('.cmd-url').each(function()
        {
            var anchor = $(this);
            anchor.click(function()
            {
                clearError();
                var link = anchor.html();
                if (link.indexOf('http:') == 0 || link.indexOf('https:') == 0 || link.indexOf('file:') == 0)
                {
                    window.open(link);
                }
                else
                {
                    var result = call("ncubeController.resolveRelativeUrl", [_selectedApp, _selectedVersion, _selectedStatus, link]);
                    if (result.status === true && result.data)
                    {
                        link = result.data;
                        window.open(link);
                    }
                    else
                    {
                        var msg = result.data ? result.data : "Unable to resolve relative URL against entries sys.classpath";
                        _errorId = showNote('Unable to open ' + link + ':<hr class="hr-small"/>' + msg);
                    }
                }
            });
        });
        processCellClicks();
        buildCubeNameLinks();
    }

    function processCellClicks()
    {
        // Add ability for the user to double-click axis and pop-up the 'Update Axis' modal
        $('.ncube-head').each(function()
        {
            $(this).dblclick(function()
            {
                var div = $(this).find('div');
                var axisName = div.attr('data-id');
                updateAxis(axisName);
            });
        });

        // Add support for individual and shift-selection of cells within the table cell area
        $('td.cell').each(function ()
        {
            var cell = $(this);
            cell.click(function (event)
            {
                if (event.shiftKey || event.ctrlKey)
                {
                    var selectedCell = $('td.cell-selected');
                    if (!selectedCell || selectedCell.length == 0)
                    {
                        clearSelectedCells();
                        cell.addClass('cell-selected');
                        cell.children().addClass('cell-selected');
                    }
                    else
                    {
                        var table = $(".table-ncube")[0];
                        var minRow = 10000000000;
                        var minCol = 10000000000;
                        var maxRow = -1;
                        var maxCol = -1;
                        var tableRows = table.rows;

                        selectedCell.each(function()
                        {
                            var iCell = $(this);
                            var iRow = getRow(iCell);
                            var iCol = getCol(iCell) - countTH(tableRows[iRow].cells);
                            if (iRow < minRow) minRow = iRow;
                            if (iRow > maxRow) maxRow = iRow;
                            if (iCol < minCol) minCol = iCol;
                            if (iCol > maxCol) maxCol = iCol;
                        });
                        var aRow = getRow(cell);
                        var aCol = getCol(cell) - countTH(tableRows[aRow].cells);

                        // Ensure that the rectangle goes from top left to lower right
                        if (aCol > maxCol) maxCol = aCol;
                        if (aCol < minCol) minCol = aCol;
                        if (aRow > maxRow) maxRow = aRow;
                        if (aRow < minRow) minRow = aRow;

                        for (var column = minCol; column <= maxCol; column++)
                        {
                            for (var row = minRow; row <= maxRow; row++)
                            {
                                var numTH = countTH(tableRows[row].cells);
                                var domCell = tableRows[row].cells[column + numTH]; // This is a DOM "TD" element
                                var jqCell = $(domCell);                             // Now it's a jQuery object.
                                jqCell.addClass('cell-selected');
                                jqCell.children().addClass('cell-selected');
                            }
                        }
                    }
                }
                else
                {   // On a straight-up click, nuke any existing selection.
                    clearSelectedCells();
                    cell.addClass('cell-selected');
                    cell.children().addClass('cell-selected');
                }
            });

            // Add ability for user to double-click and get the edit cell modal
            cell.dblclick(function ()
            {   // On double click open Edit Cell modal
                _uiCellId = cell;
                _cellId = _uiCellId.attr('data-id').split("_");
                editCell();
            });
        });
    }

    function countTH(row)
    {
        var count = 0;
        for (var i=0; i < row.length; i++)
        {
            if (row[i].tagName == "TH")
            {
                count++;
            }
        }
        return count;
    }

    function getCol(cell)
    {
        var col = cell.parent().children().index(cell);
        return col;
    }

    function getRow(cell)
    {
        var row = cell.parent().parent().children().index(cell.parent());
        return row;
    }

    function clearSelectedCells()
    {
        $(".cell-selected").each(function()
        {
            $(this).removeClass('cell-selected');
        });
    }

    function buildCubeNameLinks()
    {
        // Build cube list names
        var cubeNames = [];
        $.each(_cubeList, function (key, value)
        {
            if (key.length > 1)
            {   // Only support n-cube names with 2 or more characters in them (too many false replaces will occur otherwise)
                cubeNames.push(key);
            }
        });

        // Sort n-cube names by longest to shortest (eliminates subset matching)
        cubeNames.sort(function (a, b)
        {
            return b.length - a.length;
        });

        // Create parallel map of cube name to HTML cube name
        var anchorCubeNames = {};
        $.each(cubeNames, function (key, cubeName)
        {
            anchorCubeNames[cubeName.toLowerCase()] = '<a class="ncube-anchor" href="#">' + cubeName + '</a>';
        });

        var failedCheck = {};
        // Add links to all n-cubes within columns and cells
        var regex = new RegExp(cubeNames.join("|"), "gi");
        $('.column, .cell').each(function ()
        {
            var cell = $(this);
            var html = cell.html();
            var found = false;

            html = html.replace(regex, function (matched)
            {
                found = true;
                return anchorCubeNames[matched.toLowerCase()];
            });

            if (found)
            {   // substitute new text with anchor tag
                cell.html(html);
            }
            else
            {
                var loHtml = html.toLowerCase();
                if (!failedCheck[html] && (anchorCubeNames['rpm.class.' + loHtml] || anchorCubeNames['rpm.enum.' + loHtml]))
                {
                    html = '<a class="ncube-anchor" href="#">' + html + '</a>';
                    cell.html(html);
                }
                else
                {
                    failedCheck[html] = true;
                }
            }
        });

        failedCheck = null;

        // Add click handler that opens clicked cube names
        $('.ncube-anchor').each(function ()
        {
            var link = $(this);
            link.click(function ()
            {
                var cubeName = link.html().toLowerCase();
                if (anchorCubeNames[cubeName])
                {
                    _selectedCubeName = link.html();
                }
                else
                {
                    if (anchorCubeNames['rpm.class.' + cubeName])
                    {
                        _selectedCubeName = 'rpm.class.' + link.html();
                    }
                    else if (anchorCubeNames['rpm.enum.' + cubeName])
                    {
                        _selectedCubeName = 'rpm.enum.' + link.html();
                    }
                }
                loadCube();
            });
        });
    }

    function loadCubeJson()
    {
        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            _editor.setText('No n-cube to load');
            setDirtyStatus(false);
            return;
        }
        var result = call("ncubeController.getJson", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            _editor.setText(result.data);
        }
        else
        {
            _editor.setText("Unable to load '" + _selectedCubeName + "'. " + result.data);
        }

        var editCtrl = $('#jsoneditor');
        if (_jsonEditorMode == 'format')
        {
            editCtrl.find('.format').click();
        }
        else
        {
            editCtrl.find('.compact').click();
        }
        setDirtyStatus(false);
    }

    function loadCubeDetails()
    {
        var cube = _cubeList[_selectedCubeName]['ncube'];
        if (!cube)
        {
            return;
        }
        $('#cube_id').val(cube.id);
        $('#cube_name').val(cube.name);
        $('#cube_revision').val(cube.revision);
        var date = '';
        if (cube.createDate != undefined)
        {
            date = new Date(cube.createDate.value).format('yyyy-mm-dd HH:MM:ss');
        }
        $('#cube_createDate').val(date);
        $('#cube_createHid').val(cube.createHid);
    }

    function clearTestView() {
        $('#testParametersDiv').hide();
        $('#testAssertionsDiv').hide();
        $('#testNameDiv').hide();
        $('#testButtonGroupDiv').hide();
        _testResultsDiv.hide();

        $('#testParameters').empty();
        $('#testAssertions').empty();
        $('#testResults').empty();
    }

    function loadTestView() {
        clearTestView();

        if (!_testData || _testData.length == 0 || _testSelectionAnchor >= _testData.length || _testSelectionAnchor < 0) {
            _testSelectionAnchor = -1;
            return;
        }

        var testData = _testData[_testSelectionAnchor];

        var testParameters = $('#testParameters');
        var testAssertions = $('#testAssertions');
        var testResult = $('#testResults');

        try {
            $('#selectedTestName').html(testData['name']);

            var coordinate = testData['coord'];

            if (coordinate != null  && coordinate) {
                $.each(coordinate, function (index, item) {
                    var key = item['key'];
                    if (key.substring(0, 1) != "@") {
                        var value = item['value'];

                        var isUrl = (value == null || value.isUrl == null) ? false : value.isUrl;
                        var v = (value == null || value.value == null) ? null : value.value;
                        var dataType = (value == null || value.dataType == null) ? null : value.dataType;
                        testParameters.append(buildParameter(key, dataType, isUrl, v, true, false, deleteParameter));
                    }
                });
            }

            var assertions = testData['expected'];
            if (assertions != null  && assertions) {
                $.each(assertions, function (index, value) {
                    var isUrl = value['isUrl'] == null ? null : value['isUrl'];
                    var v = value.value == null ? null : value.value;
                    var dataType = value.dataType == null ? null : value.dataType;
                    testAssertions.append(buildParameter(index + 1, "exp", isUrl, v, false, true, deleteAssertion));
                });
            }
        } catch (e) {
            console.log(e);
            _errorId = showNote('Unable to load test view:<hr class="hr-small"/>' + e.message);
        }

        $('#testParametersDiv').fadeIn('fast');
        $('#testAssertionsDiv').fadeIn('fast');
        $('#testNameDiv').fadeIn('fast');
        $('#testButtonGroupDiv').fadeIn('fast');
    }

    function createTypeSelector(typeStr, url) {
        if (typeStr == null) {
            typeStr = 'string';
        }
        var selector = $("<select/>").attr({'class': 'selectpicker show-tick show-menu-arrow', 'data-width':'auto', 'data-style': 'btn-default'});
        return fillTypeSelector(selector, typeStr, url);
    }

    function fillTypeSelector(selector, typeStr, url) {

        if (selector == null) {
            return;
        }
        selector.empty();

        var options = null;

        if (url) {
            options = $('#datatypes-url').find('option');
        } else {
            options = $('#datatypes-value').find('option');
        }

        $.each(options, function (i, value)
        {
            var item = $(value);
            var opt = $("<option/>").attr({'value': item.val()});
            opt.text(item.text());

            if (typeStr != null && typeStr == item.val()) {
                opt.prop({'selected' : true});
            }
            selector.append(opt);
        });

        return selector;
    }

    function leftPad (str, length) {
        var pad = length - str.length;

        if (pad < 1 || pad > 11) {
            return str;
        }

        return _padding[pad] + str;
    }

    // these are just temporary to use as an id
    function generateRandomId() {
        return leftPad(Math.random().toString(36).substring(7), 11);
    }

    function buildParameter(labelText, type, isUrl, value, hasSelector, isRenumberable, deleteFunc) {
        var ident = generateRandomId();

        var labelGroup = $("<div/>").attr({'class': 'form-group', 'parameter-id':labelText, 'id':ident});

        var cat = ident + "-value";
        var label = $("<label/>").attr({'for': cat, 'class': 'control-label'}).html(labelText);

        var deleteParamButton = $("<a/>").attr({'class':'red-item pull-right', 'font-size':'9px', 'style':'padding: 1px 3px; font-size: 12px; line-height: 1.5; border-radius: 3px;'});
        var glyph = $("<span/>").attr({'class':'glyphicon glyphicon-remove', 'style':'vertical-align: -1px;' });
        deleteParamButton.append(glyph);
        deleteParamButton.click(deleteFunc);


        labelGroup.append(label);
        labelGroup.append(deleteParamButton);


        var inputGroupAddon = $("<span/>").attr({'class':'input-group-btn'});
        var controls = $("<div/>").attr({'class': 'controls'});
        var inputGroup = $("<div/>").attr({'class':'input-group input-group-sm'});

        var urlButton = $("<button/>").attr({'type':'button', 'class':'btn btn-default', 'value':'url'});

        if (isUrl) {
            urlButton.html("&nbsp;URL&nbsp;");
        } else {
            urlButton.html("Value");
        }

        urlButton.click(function ()
        {
            var txt = urlButton.text();
            if (txt == "Value") {
                urlButton.html("&nbsp;URL&nbsp;");
            } else {
                urlButton.html("Value");
            }
        });


        inputGroupAddon.append(urlButton);
        inputGroup.append(inputGroupAddon);

        inputGroup.append($("<input/>").attr({'class': 'form-control', 'type': 'text', 'id': cat}).val(value));

        if (hasSelector) {
            var selector = createTypeSelector(type, isUrl);
            inputGroup.append(selector);
            selector.selectpicker();
        }

        controls.append(inputGroup);
        labelGroup.append(controls);

        return labelGroup;
    }

    function loadCube()
    {
        if (_activeTab == 'ncubeTab')
        {
            loadCubeHtml();
        }
        else if (_activeTab == 'jsonTab')
        {
            loadCubeJson();
        }
        else if (_activeTab == 'detailsTab')
        {
            loadCubeDetails();
        }
        else if (_activeTab == 'testTab')
        {
            loadTestListView("ncubeController.getTests");
        }
        else if (_activeTab == 'picTab')
        {
            // TODO: Load D3.js pictures
        }
        else
        {
            console.log('Unknown tab selected: ' + _activeTab);
        }
        openGroupContainingLastSelectedNCube();
        setListSelectedStatus(_selectedCubeName, '#ncube-list');
    }

    /**
     * Tweak the class name of the selected / non-selected items
     * to match what was selected.
     */
    function setListSelectedStatus(itemName, listId)
    {
        var items = $(listId).find('li a');
        var saveSelected = null;
        $.each(items, function (index, value)
        {
            var anchor = $(value);
            var text = anchor.html();
            var elemName = anchor.attr('itemName');
            if (itemName == text || itemName == elemName)
            {
                saveSelected = anchor;
                anchor.attr('class', 'ncube-selected');
            }
            else
            {
                anchor.attr('class', 'ncube-notselected');
            }
        });

        if (saveSelected)
        {
            saveSelected.scrollintoview();
        }
    }

    /**
     * Load NCube List from Database
     */
    function loadNCubes()
    {
        _cubeList = {};
        clearError();
        if (!_selectedApp)
        {
            _errorId = showNote('No App selected, cannot load n-cubes.');
            return;
        }
        if (!_selectedVersion)
        {
            _errorId = showNote('No Version selected, cannot load n-cubes.');
            return;
        }
        if (!_selectedStatus)
        {
            _errorId = showNote('No Status selected, cannot load n-cubes.');
            return;
        }
        var result = call("ncubeController.getCubeList", ['%', _selectedApp, _selectedVersion, _selectedStatus]);
        var first = null;
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var name = value['ncube'].name;
                _cubeList[name] = value;
                if (!first)
                {
                    first = name;
                }
            });
        }
        else
        {
            _errorId = showNote('Unable to load n-cubes:<hr class="hr-small"/>' + result.data);
        }
        if (!_selectedCubeName || !doesCubeExist())
        {
            _selectedCubeName = (_cubeList && first) ? _cubeList[first]['ncube'].name : null;
        }
    }

    function doesCubeExist()
    {
        var found = false;
        $.each(_cubeList, function(key, value)
        {
            if (key == _selectedCubeName)
            {
                found = true;
            }
        });
        return found;
    }

    function loadVersions()
    {
        _versions = [];
        clearError();
        if (!_selectedApp)
        {
            _errorId = showNote('Unable to load versions, no n-cube App selected.');
            return;
        }
        if (!_selectedStatus)
        {
            _errorId = showNote('Unable to load versions, no n-cube Status selected.');
            return;
        }
        var result = call("ncubeController.getAppVersions", [_selectedApp, _selectedStatus]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                _versions[index] = value;
            });
        }
        else
        {
            _errorId = showNote('Unable to load versions:<hr class="hr-small"/>' + result.data);
        }
        if (!_selectedVersion || !doesVersionExist(_selectedVersion))
        {
            _selectedVersion = (_versions && _versions.length > 0) ? _versions[_versions.length - 1] : null;
        }
    }

    function doesVersionExist(selVer)
    {
        for (var i=0; i < _versions.length; i++)
        {
            if (_versions[i] == selVer)
            {
                return true;
            }
        }
        return false;
    }

    function loadAppNames()
    {
        _apps = [];
        clearError();
        var result = call("ncubeController.getAppNames", []);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                _apps[index] = value;
            });
        }
        else
        {
            _errorId = showNote('Unable to load n-cube Apps:<hr class="hr-small"/>' + result.data);
        }
        if (!_selectedApp && _apps)
        {
            _selectedApp = _apps[0];
            localStorage[SELECTED_APP] = _selectedApp;
        }
        if (!_apps)
        {
            _selectedApp = localStorage[SELECTED_APP] = null;
        }
        else if (!doesItemExist(_selectedApp, _apps) && _apps.length > 0)
        {
            _selectedApp = _apps[0];
        }
    }

    function newCube()
    {
        clearError();
        $('#newCubeAppName').val(_selectedApp);
        $('#newCubeStatus').val('SNAPSHOT');
        $('#newCubeVersion').val(_selectedVersion);
        buildDropDown('#newCubeAppList', '#newCubeAppName', _apps, function (app)
        {
            var result = call("ncubeController.getAppVersions", [app, 'SNAPSHOT']);
            if (result.status === true)
            {
                buildDropDown('#existVersionList', '#newCubeVersion', result.data, function ()
                {
                });
            }
            else
            {
                _errorId = showNote('Failed to load App versions:<hr class="hr-small"/>' + result.data);
            }
        });
        buildDropDown('#existVersionList', '#newCubeVersion', _versions, function ()
        {
        });
        $('#newCubeModal').modal();
    }

    function newCubeSave()
    {
        $('#newCubeModal').modal('hide');
        var appName = $('#newCubeAppName').val();
        var cubeName = $('#newCubeName').val();
        var version = $('#newCubeVersion').val();
        var result = call("ncubeController.createCube", [cubeName, appName, version]);
        if (result.status === true)
        {
            _selectedCubeName = cubeName;
            loadAppNames();
            _selectedApp = appName;
            loadAppListView();
            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();
            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to create n-cube '" + cubeName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function renameAssertions()
    {
        $("#testAssertions .form-group").each(function (index, value)
        {
            var v = $(value);
            var count = index + 1;
            v.attr('parameter-id', count);
            v.find('label.control-label').html("" + count + ".");
            v.find('a[parameter-id]').attr('parameter-id', count);
        });
    }

    function deleteParameter(e)
    {
        var parent = $(e.currentTarget).parent("div.form-group");
        var param = parent.attr('parameter-id');
        var test = $('#selectedTestName').html();

        var title = "Delete Parameter?";
        var label = "Delete parameter '" + param + "' from '" + test + "'?";

        deleteTestItem(title, label, false, parent.attr('id'));
    }

    function deleteAssertion(e)
    {
        var parent = $(e.currentTarget).parent("div.form-group");
        var param = parent.attr('parameter-id');
        var test = $('#selectedTestName').html();
        var title = "Delete Assertion?";
        var label = "Delete assertion '" + param + "' from '" + test + "'?";

        deleteTestItem(title, label, true, parent.attr('id'));
    }

    function deleteTestItem(title, label, isRenumberable, paramId)
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Nothing to delete.');
            return;
        }

        $('#deleteParameterTitle').html(title);
        $('#deleteParameterLabel').html(label);
        $('#deleteParameterHiddenId').val(paramId);
        $('#deleteParameterOk').attr({'isRenumberable': isRenumberable});
        _deleteParameterModal.modal({
            keyboard: true
        });
    }

    function deleteParameterOk()
    {
        _deleteParameterModal.modal('hide');

        var id = $('#deleteParameterHiddenId').val();
        var isRenumberable = $('#deleteParameterOk').attr('isRenumberable')

        $('#' + id).remove();

        if (isRenumberable == "true") {
            renameAssertions();
        }

        saveAllTests(false);
    }

    function deleteCube()
    {
        if (!ensureModifiable('Cannot delete n-cube.'))
        {
            return;
        }

        $('#deleteCubeLabel').html('Delete \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ') ?');
        $('#deleteCubeModal').modal();
    }

    function deleteCubeOk()
    {
        $('#deleteCubeModal').modal('hide');
        var result = call("ncubeController.deleteCube", [_selectedCubeName, _selectedApp, _selectedVersion]);
        if (result.status === true)
        {
            loadAppNames();
            loadAppListView();
            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();
            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to delete n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function restoreCube()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No (inbound) references to show.');
            return;
        }
        var ul = $('#deletedCubeList');
        ul.empty();
        $('#restoreCubeLabel').html('Restore Cubes in ' + _selectedVersion + ', ' + _selectedStatus);
        var result = call("ncubeController.getDeletedCubeList", ["", _selectedApp, _selectedVersion]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var label = $('<label/>').prop({class: 'checkbox'}).text(value.name);
                var input = $('<input>').prop({class: 'restoreCheck', 'type': 'checkbox'});
                input.prependTo(label); // <=== create input without the closing tag
                ul.append(label);
            });
            $('#restoreCubeModal').modal();
        }
        else
        {
            _errorId = showNote('Error fetching deleted cubes (' + _selectedVersion + ', ' + _selectedStatus + '):<hr class="hr-small"/>' + result.data);
        }
    }

    function restoreCubeOk()
    {
        $('#restoreCubeModal').modal('hide');

        var input = $('.restoreCheck');
        var cubesToRestore = [];
        $.each(input, function (index, label)
        {
            if ($(this).is(':checked'))
            {
                cubesToRestore.push($(this).parent().text());
            }
        });

        var result = call("ncubeController.restoreCube", [_selectedApp, _selectedVersion, _selectedStatus, cubesToRestore]);
        if (result.status === true)
        {
            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to restore n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function revisionHistory()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No revision history to show.');
            return;
        }
        var ul = $('#revisionHistoryList');
        ul.empty();
        $('#revisionHistoryLabel').html('Revision History for ' + _selectedCubeName);
        $('#revisionHistoryModal').modal();
        var result = call("ncubeController.getRevisionHistory", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var li = $("<li/>").attr({'class': 'list-group-item'});
                var anchor = $('<a href="#"/>');
                var date = '';
                if (value.createDate != undefined)
                {
                    date = new Date(value.createDate.value).format('yyyy-mm-dd HH:MM:ss');
                }
                anchor.html('rev: ' + value.revision + '&nbsp;&nbsp;&nbsp;' + date + '&nbsp;&nbsp;&nbsp;' + value.createHid);
                //anchor.click(function ()
                //{
                //    showRefsToCubeClose();
                //    _selectedCubeName = value;
                //    loadCube();
                //});
                li.append(anchor);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching revision history (' + _selectedVersion + ', ' + _selectedStatus + '):<hr class="hr-small"/>' + result.data);
        }
    }

    function revisionHistoryOk()
    {
        $('#revisionHistoryModal').modal('hide');
        //var result = call("ncubeController.restoreCube", [_selectedCubeName, _selectedApp, _selectedVersion]);
        //if (result.status === true)
        //{
        //    loadAppNames();
        //    loadAppListView();
        //    var saveSelectedVersion = _selectedVersion;
        //    loadVersions();
        //    _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
        //    loadVersionListView();
        //    loadNCubes();
        //    loadNCubeListView();
        //    loadCube();
        //}
        //else
        //{
        //    _errorId = showNote("Unable to restore n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
        //}
    }

    function deleteCurrentTestMenu()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube is currently selected. There is nothing to rename.');
            return;
        }

        var list = $("#testListItems a.selected");

        if (list.length != 1)
        {
            if (list.length == 0) {
                _errorId = showNote('No test is currently selected. Select a test to delete.');
            } else {
                _errorId = showNote('More than one test is selected. There can only be one test selected to delete.');
            }
            return;
        }

        var test = _testData[_testSelectionAnchor]["name"];

        $('#deleteTestTitle').html("Delete Test");
        $('#deleteTestLabel').html("Are you sure you want to delete the test '" + test + "'?");
        $('#deleteTestModal').modal({
            keyboard: true
        });

    }

    function deleteTestOk()
    {
        clearError();

        var list = $("#testListItems a.selected");

        $('#deleteTestModal').modal('hide');

        _testData.splice(_testSelectionAnchor, 1);
        _testSelectionAnchor = -1;

        refreshTestList();
        saveAllTests(true);
    }

    function renameCurrentTestMenu()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube is currently selected. There is nothing to rename.');
            return;
        }

        var list = $("#testListItems a.selected");

        if (list.length != 1)
        {
            if (list.length == 0) {
                _errorId = showNote('No test is currently selected. Select a test to duplicate.');
            } else {
                _errorId = showNote('More than one test is selected. There can only be one test selected to duplicate.');
            }
            return;
        }

        var test = _testData[_testSelectionAnchor]["name"];


        $('#renameTestOldName').val(test);
        $('#renameTestNewName').val("");
        $('#renameTestLabel').html('Rename \'' + test + '\'?');

        _renameTestModal.modal({
            keyboard: true
        });

    }


    function renameCube()
    {
        if (!ensureModifiable('Unable to rename cube.'))
        {
            return;
        }

        $('#renameCubeAppName').val(_selectedApp);
        $('#renameCubeVersion').val(_selectedVersion);
        $('#renameCubeName').val(_selectedCubeName);
        $('#renameCubeLabel').html('Rename \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ') ?');
        $('#renameCubeModal').modal();
    }

    function renameCubeOk()
    {
        $('#renameCubeModal').modal('hide');
        var oldName = $('#renameCubeName').val();
        var newName = $('#renameNewCubeName').val();
        var newApp = $('#renameCubeAppName').val();
        var newVersion = $('#renameCubeVersion').val();
        var result = call("ncubeController.renameCube", [oldName, newName, newApp, newVersion]);
        if (result.status === true)
        {
            loadAppNames();
            _selectedApp = newApp;
            loadAppListView();
            _selectedStatus = 'SNAPSHOT';
            setListSelectedStatus('SNAPSHOT', '#status-list')
            loadVersions();
            _selectedVersion = newVersion;
            loadVersionListView();
            loadNCubes();
            _selectedCubeName = newName;
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to rename n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function testNameAlreadyExists(name) {

        if (_testData == null) {
            return false;
        }

        var found = false;
        $.each(_testData, function (index, value)
        {
            if (value['name'] == name) {
                found = true;
            }
        });

        return found;
    }

    function validateTestName(name) {
        var nameRegex = /^([A-Za-z0-9-.]{3,64})$/g;

        if (!nameRegex.test(name)) {
            _errorId = showNote("Test name is invalid. Test names can only contain letters, numbers, and, ., and -");
            return false;
        }

        if (testNameAlreadyExists(name)) {
            _errorId = showNote("There is already a test with that name.  Please rename and try again.");
            return false;
        }

        return true;
    }


    function renameTestOk()
    {
        clearError();
        var oldName = $('#renameTestOldName').val();
        var newName = $('#renameTestNewName').val();

        if (!validateTestName(newName)) {
            return;
        }

        _renameTestModal.modal('hide');

        // change name of selected test
        $('#selectedTestName').html(newName);

        // change currently selected model item
        var test = _testData[_testSelectionAnchor];
        test["name"] = newName;

        // change in our list.
        refreshTestList();
    }

    function dupeCube()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Nothing to duplicate.');
            return;
        }
        $('#dupeCubeAppName').val(_selectedApp);
        $('#dupeCubeVersion').val(_selectedVersion);
        $('#dupeCubeName').val(_selectedCubeName);
        $('#dupeCubeLabel').html('Duplicate \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ') ?');
        buildDropDown('#dupeCubeAppList', '#dupeCubeAppName', _apps, function (app)
        {
            var result = call("ncubeController.getAppVersions", [app, 'SNAPSHOT']);
            if (result.status === true)
            {
                buildDropDown('#dupeCubeVersionList', '#dupeCubeVersion', result.data, function ()
                {
                });
            }
            else
            {
                _errorId = showNote('Unable to load App versions:<hr class="hr-small"/>' + result.data);
            }
        });
        buildDropDown('#dupeCubeVersionList', '#dupeCubeVersion', _versions, function ()
        {
        });
        $('#dupeCubeModal').modal();
    }

    function dupeCubeCopy()
    {
        $('#dupeCubeModal').modal('hide');
        var newName = $('#dupeCubeName').val();
        var newApp = $('#dupeCubeAppName').val();
        var newVersion = $('#dupeCubeVersion').val();
        var result = call("ncubeController.duplicateCube", [newName, _selectedCubeName, newApp, _selectedApp, newVersion, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            loadAppNames();
            _selectedApp = newApp;
            loadAppListView();
            _selectedStatus = 'SNAPSHOT';
            setListSelectedStatus('SNAPSHOT', '#status-list');
            loadVersions();
            _selectedVersion = newVersion;
            loadVersionListView();
            loadNCubes();
            _selectedCubeName = newName;
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to duplicate n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function showRefsToCube()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No (inbound) references to show.');
            return;
        }
        $('#showRefsToLabel').html('Inbound refs to \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ')');
        var ul = $('#refsToCubeList');
        ul.empty();
        $('#showRefsToCubeModal').modal();
        var result = call("ncubeController.getReferencesTo", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var li = $("<li/>").attr({'class': 'list-group-item'});
                var anchor = $('<a href="#"/>');
                anchor.html(value);
                anchor.click(function ()
                {
                    showRefsToCubeClose();
                    _selectedCubeName = value;
                    loadCube();
                });
                li.append(anchor);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching inbound references to ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '):<hr class="hr-small"/>' + result.data);
        }
    }

    function showRefsToCubeClose()
    {
        $('#showRefsToCubeModal').modal('hide');
    }

    function showRefsFromCube()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No (outbound) references to show.');
            return;
        }
        $('#showRefsFromLabel').html('Outbound refs of \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ')');
        var ul = $('#refsFromCubeList');
        ul.empty();
        $('#showRefsFromCubeModal').modal();
        var result = call("ncubeController.getReferencesFrom", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var li = $("<li/>").attr({'class': 'list-group-item'});
                var anchor = $('<a href="#"/>');
                anchor.html(value);
                anchor.click(function ()
                {
                    showRefsFromCubeClose();
                    _selectedCubeName = value;
                    loadCube();
                });
                li.append(anchor);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching outbound references for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '):<hr class="hr-small"/>' + result.data);
        }
    }

    function showRefsFromCubeClose()
    {
        $('#showRefsFromCubeModal').modal('hide');
    }

    function showReqScope()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No required scope to show.');
            return;
        }
        $('#showReqScopeLabel').html('Scope for \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ')');
        var ul = $('#reqScopeList');
        ul.empty();
        $('#showReqScopeModal').modal();
        var result = call("ncubeController.getRequiredScope", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true)
        {
            $.each(result.data, function (index, value)
            {
                var li = $("<li/>").attr({'class': 'list-group-item'});
                li.html(value);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching required scope for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '):<hr class="hr-small"/>' + result.data);
        }
    }

    function showReqScopeClose()
    {
        $('#showReqScopeModal').modal('hide');
    }

    function releaseCubes()
    {
        if (!ensureModifiable('Release of SNAPSHOT cannot happen.'))
        {
            return;
        }

        $('#releaseCubesLabel').html('Release ' + _selectedApp + ' ' + _selectedVersion + ' SNAPSHOT ?');
        $('#releaseCubesAppName').val(_selectedApp);
        $('#releaseCubesModal').modal();
    }

    function releaseCubesOk()
    {
        $('#releaseCubesModal').modal('hide');
        var newSnapVer = $('#releaseCubesVersion').val();
        var result = call("ncubeController.releaseCubes", [_selectedApp, _selectedVersion, newSnapVer]);
        if (result.status === true)
        {
            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();
            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to release version '" + _selectedVersion + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function changeVersion()
    {
        if (!ensureModifiable('Version cannot be changed.'))
        {
            return;
        }

        $('#changeVerLabel').html('Change ' + _selectedApp + ' ' + _selectedVersion + ' ?');
        $('#changeVerModal').modal();
    }

    function changeVersionOk()
    {
        $('#changeVerModal').modal('hide');
        var newSnapVer = $('#changeVerValue').val();
        var result = call("ncubeController.changeVersionValue", [_selectedApp, _selectedVersion, newSnapVer]);
        if (result.status === true)
        {
            loadVersions();
            _selectedVersion = doesItemExist(newSnapVer, _versions) ? newSnapVer : _selectedVersion;
            loadVersionListView();
            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to change SNAPSHOT version to value '" + newSnapVer + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function addAxis()
    {
        if (!ensureModifiable('Axis cannot be added.'))
        {
            return;
        }

        var generalTypes = ['STRING', 'LONG', 'BIG_DECIMAL', 'DOUBLE', 'DATE', 'COMPARABLE'];
        var ruleTypes = ['EXPRESSION'];
        buildDropDown('#addAxisTypeList', '#addAxisTypeName', ['DISCRETE', 'RANGE', 'SET', 'NEAREST', 'RULE'], function (selected)
        {
            if ("RULE" == selected)
            {
                buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', ruleTypes, function () { });
                $('#addAxisValueTypeName').val('EXPRESSION');
            }
            else
            {
                buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', generalTypes, function () { });
                $('#addAxisValueTypeName').val('STRING');
            }
        });
        buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', generalTypes, function () { });
        $('#addAxisName').val('');
        $('#addAxisModal').modal();
    }

    function addAxisOk()
    {
        $('#addAxisModal').modal('hide');
        var axisName = $('#addAxisName').val();
        var axisType = $('#addAxisTypeName').val();
        var axisValueType = $('#addAxisValueTypeName').val();
        var result = call("ncubeController.addAxis", [_selectedCubeName, _selectedApp, _selectedVersion, axisName, axisType, axisValueType]);
        if (result.status === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to add axis '" + axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function deleteAxis(axisName)
    {
        if (!ensureModifiable('Axis cannot be deleted.'))
        {
            return;
        }

        $('#deleteAxisName').val(axisName);
        $('#deleteAxisModal').modal();
    }

    function deleteAxisOk()
    {
        $('#deleteAxisModal').modal('hide');
        var axisName = $('#deleteAxisName').val();
        var result = call("ncubeController.deleteAxis", [_selectedCubeName, _selectedApp, _selectedVersion, axisName]);
        if (result.status === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to delete axis '" + axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }


    function runAllTests() {
        _errorId = showNote('Functionality not implemented yet.');
    }

    function saveAllTests(modelIsUpToDate)
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Axis cannot be updated.');
            return;
        }

        if (_testData == null)
        {
            _errorId = showNote('No test selected.  There are no tests to save.');
            return;
        }

        if (!modelIsUpToDate) {
            var test = getActiveTest();

            //  If a test is currently selected
            if (test != null) {
                //  locate test in list to add it in...and add it before saving.
                _testData[_testSelectionAnchor] = test;
            }
        }

        var result = call("ncubeController.saveTests", [_selectedCubeName, _selectedApp, _selectedVersion, _testData]);

        if (!result.status)
        {
            _errorId = showNote("Unable to save TestData:<hr class=\"hr-small\"/>" + result.data);
        }

    }

    function getActiveTest()
    {
        var test = {};

        var name = $("#selectedTestName").html();

        if (name == null) {
            return null;
        }

        test["name"] = name;
        test["@type"] = "com.cedarsoftware.ncube.NCubeTest";
        test["coord"] = retrieveParameters();
        test["expected"] = retrieveAssertions();

        return test;
    }

    function retrieveParameters() {
        var parameters = $("#testParameters > div[parameter-id]");
        var coord = new Array(parameters.length);

        $.each(parameters, function (index, value)
        {
            var pair = {};
            var v = $(value);
            pair['key'] = v.attr("parameter-id");
            pair['value'] = retrieveCellInfo(v, true);
            coord[index] = pair;
        });

        return coord;
    }

    function retrieveAssertions() {
        var assertions = $('#testAssertions > div[parameter-id]');
        var array = new Array(assertions.length);

        $.each(assertions, function(index, value) {
            array[index] = retrieveCellInfo($(value), false);
        });

        return array;
    }

    function retrieveCellInfo(group, hasSelector) {
        var cell = {"@type":"com.cedarsoftware.ncube.CellInfo"};

        cell["value"] = group.find('input').val();
        cell["isUrl"] = group.find('button[value]').text() != "Value";

        if (hasSelector) {
            cell["dataType"] = group.find('select').val();
        } else {
            cell["dataType"] = "exp";
        }

        return cell;
    }

    function addParameterMenu() {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Nothing to delete.');
            return;
        }

        $('#addParameterField').val('');

        _addParameterModal.modal({
            keyboard: true
        });

    }

    function addParameterOk() {
        _addParameterModal.modal('hide');

        var id = $('#addParameterField').val();

        // check to see if parameter already exists in parameter-key of #testAssertions .form-group
        var param = buildParameter(id, "string", false, '', true, false, deleteParameter);

        if ($('#testParameters .form-group').length > 0) {
            param.insertAfter('#testParameters .form-group:last');
        } else {
            $('#testParameters').append(param);
        }

        saveAllTests(false);
    }

    function addNewAssertion() {
        var count = $('#testAssertions .form-group').length;
        var param = buildParameter(count+1, "exp", false, 'output.return', false, true, deleteAssertion);

        if (count > 0) {
            param.insertAfter('#testAssertions .form-group:last');
        } else {
            $('#testAssertions').append(param);
        }

        saveAllTests(false);
    }

    function runCurrentTest() {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus) {
            _errorId = showNote('No n-cube selected. Test cannot be run.');
            return;
        }

        if (_testData == null || _testSelectionAnchor == -1) {
            _errorId = showNote('No test selected.  Test cannot be run.');
            return;
        }

        try {
            var test = getActiveTest();
            _testData[_testSelectionAnchor] = test;

            var result = call("ncubeController.runTest", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, test]);
            saveAllTests(true);

            if (result.status != true) {
                showTestResult(false, "Could not run test:  " + result.data);

                $('#testLayoutCenter > .well').animate({
                    scrollTop: _testResultsDiv.offset().top
                }, 200);

                return;
            }

            showTestResult(result.data["_result"], result.data["_message"]);


            $('#testLayoutCenter > .well').animate({
                scrollTop: _testResultsDiv.offset().top
            }, 200);

        } catch (e) {
            _errorId = showNote("Could not run cube test:<hr class=\"hr-small\"/>" + e.message);
        }
    }

    function showTestResult(success, message)
    {
        _testResultsDiv.hide();

        var testResults = $('#testResults');
        testResults.empty();

        if (success)
        {
            _testResultsDiv.addClass("panel-success");
            _testResultsDiv.removeClass("panel-danger");
        }
        else
        {
            _testResultsDiv.addClass("panel-danger");
            _testResultsDiv.removeClass("panel-success");
        }

        testResults.html(message);
        _testResultsDiv.fadeIn("fast");
    }

    function updateAxis(axisName)
    {
        if (!ensureModifiable('Axis cannot be updated.'))
        {
            return false;
        }

        var result = call("ncubeController.getAxis", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, axisName]);
        var axis;
        if (result.status === true)
        {
            axis = result.data;
        }
        else
        {
            _errorId = showNote("Could not retrieve axes for ncube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        var isRule = axis.type.name == 'RULE';
        var isNearest = axis.type.name == 'NEAREST';
        $('#updateAxisLabel').html("Update Axis: " + axisName);
        $('#updateAxisName').val(axisName);
        $('#updateAxisTypeName').val(axis.type.name);
        $('#updateAxisValueTypeName').val(axis.valueType.name);
        $('#updateAxisDefaultCol').prop({'checked': axis.defaultCol != null});
        if (isRule)
        {
            hideAxisSortOption();
            showAxisDefaultColumnOption(axis);
            showAxisFireAllOption(axis);
        }
        else if (isNearest)
        {
            hideAxisSortOption();
            hideAxisDefaultColumnOption();
            hideAxisFireAllOption();
        }
        else
        {
            showAxisSortOption(axis);
            showAxisDefaultColumnOption(axis);
            hideAxisFireAllOption();
        }
        _axisName = axisName;
        $('#updateAxisModal').modal({
            keyboard: true
        });
    }

    function showAxisSortOption(axis)
    {
        $('#updateAxisSortOrderRow').show();
        $('#updateAxisSortOrder').prop({'checked': axis.preferredOrder == 0, 'disabled': false});
    }

    function hideAxisSortOption()
    {
        $('#updateAxisSortOrderRow').hide();
    }

    function showAxisDefaultColumnOption(axis)
    {
        $('#updateAxisDefaultColRow').show();
        $('#updateAxisDefaultCol').prop({'checked': axis.defaultCol != null, 'disabled': false});
    }

    function hideAxisDefaultColumnOption()
    {
        $('#updateAxisDefaultColRow').hide();
    }

    function showAxisFireAllOption(axis)
    {
        $('#updateAxisFireAllRow').show();
        $('#updateAxisFireAll').prop({'checked': axis.fireAll == true, 'disabled': false});
    }

    function hideAxisFireAllOption()
    {
        $('#updateAxisFireAllRow').hide();
    }

    function updateAxisOk()
    {
        $('#updateAxisModal').modal('hide');
        var axisName = $('#updateAxisName').val();
        var hasDefault = $('#updateAxisDefaultCol').prop('checked');
        var sortOrder = $('#updateAxisSortOrder').prop('checked');
        var fireAll = $('#updateAxisFireAll').prop('checked');
        var result = call("ncubeController.updateAxis", [_selectedCubeName, _selectedApp, _selectedVersion, _axisName, axisName, hasDefault, sortOrder, fireAll]);
        if (result.status === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to update axis '" + axisName + "':<hr class=\"hr-small\"/>" + result.data);
        }
    }

    function loadColumns(axis)
    {
        var axisList = axis.columns['@items'];
        _columnList.empty();
        _columnList.prop('model', axis);
        var displayOrder = 0;
        $.each(axis.columns["@items"], function (key, item)
        {
            if (!item.displayOrder || item.displayOrder < 2147483647)
            {   // Don't add default column in
                item.displayOrder = displayOrder++;
                var rowDiv = $('<div/>').prop({class: "row", "model": item});
                var div = $('<div/>').prop({class: "input-group"});
                var span = $('<span/>').prop({class: "input-group-addon"});
                var inputBtn = $('<input/>').prop({class: "editColCheckBox", "type": "checkbox"});
                if (item.checked === true)
                {
                    inputBtn[0].checked = true;
                }

                if (axis.type.name == 'RULE')
                {
                    if (!item.metaProps)
                    {
                        item.metaProps = {"name": "Condition " + displayOrder};
                    }
                    var inputName = $('<input/>').prop({class: "form-control", "type": "text"});
                    inputName.attr({"data-type": "name"});
                    inputName.blur(function ()
                    {
                        item.metaProps.name = inputName.val();
                    });
                    inputName.val(item.metaProps.name);
                }

                var inputText = $('<input/>').prop({class: "form-control", "type": "text"});
                inputText.attr({"data-type":"cond"});
                inputText.blur(function()
                {
                    item.value = inputText.val();
                });

                inputText.val(item.value);
                span.append(inputBtn);
                div.append(span);
                if (axis.type.name == 'RULE')
                {
                    div.append(inputName);
                }
                div.append(inputText);
                rowDiv.append(div);
                _columnList.append(rowDiv);
            }
        });
    }

    function ensureModifiable(operation)
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote(operation + ' No n-cube selected.');
            return false;
        }
        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote(operation + ' Only a SNAPSHOT version can be modified.');
            return false;
        }
        return true;
    }

    function editColumns(axisName)
    {
        if (!ensureModifiable('Columns cannot be edited.'))
        {
            return false;
        }

        var result = call("ncubeController.getAxis", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, axisName]);
        var axis;
        if (result.status === true)
        {
            axis = result.data;
            if (!axis.columns['@items'])
            {
                axis.columns['@items'] = [];
            }
            if (axis.defaultCol)
            {   // Remove actual Default Column object (not needed, we can infer it from Axis.defaultCol field being not null)
                axis.columns["@items"].splice(axis.columns["@items"].length - 1, 1);
            }
        }
        else
        {
            _errorId = showNote("Could not retrieve axes for n-cube '" + _selectedCubeName + "':<hr class=\"hr-small\"/>" + result.data);
            return;
        }
        sortColumns(axis);
        loadColumns(axis);
        var moveBtnAvail = axis.preferredOrder == 1;
        if (moveBtnAvail === true)
        {
            $('#editColUp').show();
            $('#editColDown').show();
        }
        else
        {
            $('#editColUp').hide();
            $('#editColDown').hide();
        }
        $('#editColumnsLabel').html('Edit ' + axisName);
        $('#editColumnsModal').modal();
    }

    function sortColumns(axis)
    {
        if (axis.preferredOrder == 1)
        {
            axis.columns['@items'].sort(function(a, b)
            {
                return a.displayOrder - b.displayOrder;
            });
        }
    }

    // Check all or uncheck all column check boxes
    function editColSelect(state)
    {
        var input = $('.editColCheckBox');
        $.each(input, function (index, btn)
        {
            $(this).prop('checked', state);
        });
    }

    function editColAdd()
    {
        var input = $('.editColCheckBox');
        var loc = -1;
        $.each(input, function (index, btn)
        {
            if ($(this).prop('checked'))
            {
                loc = index;
            }
        });
        var axis = _columnList.prop('model');
        var newCol = {
            '@type': 'com.cedarsoftware.ncube.Column',
            'value': 'newValue',
            'id': getUniqueId()
        };

        if (loc == -1 || axis.preferredOrder == 0)
        {
            axis.columns['@items'].push(newCol);
            loc = input.length - 1;
        }
        else
        {
            axis.columns['@items'].splice(loc + 1, 0, newCol);
        }
        loadColumns(axis);

        // Select newly added column name, so user can just type over it.
        input = _columnList.find('.form-control');
        input[loc + 1].select();
    }

    function editColDelete()
    {
        var axis = _columnList.prop('model');
        var input = $('.editColCheckBox');
        var cols = axis.columns['@items'];
        var colsToDelete = [];
        $.each(input, function (index, btn)
        {
            if ($(this).prop('checked'))
            {
                colsToDelete.push(index);
            }
        });

        // Walk through in reverse order, deleting from back to front so that
        // the correct elements are deleted.
        for (var i=colsToDelete.length - 1; i >= 0; i--)
        {
            cols.splice(colsToDelete[i], 1);
        }
        loadColumns(axis);
    }

    function editColUp()
    {
        var axis = _columnList.prop('model');
        var cols = axis.columns['@items'];
        var input = $('.editColCheckBox');

        if (cols && cols.length > 0 && input[0].checked)
        {   // Top one checked, cannot move any items up
            return;
        }

        for (var i=0; i < input.length - 1; i++)
        {
            var tag = input[i];
            cols[i].checked = tag.checked;
            if (!tag.checked)
            {
                var nextTag = input[i + 1];
                cols[i + 1].checked = nextTag.checked;
                if (nextTag.checked)
                {
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

    function editColDown()
    {
        var axis = _columnList.prop('model');
        var cols = axis.columns['@items'];
        var input = $('.editColCheckBox');

        if (cols && cols.length > 0 && input[cols.length - 1].checked)
        {   // Bottom one checked, cannot move any items down
            return;
        }

        for (var i=input.length - 1; i > 0; i--)
        {
            var tag = input[i];
            cols[i].checked = tag.checked;
            if (!tag.checked)
            {
                var nextTag = input[i - 1];
                cols[i - 1].checked = nextTag.checked;
                if (nextTag.checked)
                {
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

    function editColCancel()
    {
        $('#editColumnsModal').modal('hide');
    }

    function editColSave()
    {
        var axis = _columnList.prop('model');
        _columnList.find('input[data-type=cond]').each(function(index, elem)
        {
            axis.columns['@items'][index].value = elem.value;
        });
        _columnList.find('input[data-type=name]').each(function(index, elem)
        {
            var col = axis.columns['@items'][index];
            if (col.metaProp)
            {
                col.metaProp.name = elem.value;
            }
        });
        $('#editColumnsModal').modal('hide');
        axis.defaultCol = null;
        var result = call("ncubeController.updateAxisColumns", [_selectedCubeName, _selectedApp, _selectedVersion, axis]);

        if (result.status === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote("Unable to update columns for axis '" + axis.name + "':<hr class=\"hr-small\"/>" + result.data);
        }
        reloadCube();
    }

    function changeType()
    {
        if (!ensureModifiable('Cell types cannot be changed.'))
        {
            return;
        }

        // TODO: Turn off field value, then pop-up edit cell modal.
        // Set flag in modal so that it handles OK differently, has no clear.
        // In editCell() make sure to turn on appropriate fields.

        alert('Change type not yet implemented.');
    }

    function clearCache()
    {
        var result = call("ncubeController.clearCache", []);

        if (result.status === false)
        {
            _errorId = showNote('Unable to fetch the cell contents: ' + result.data);
        }
    }

    function editCell(value)
    {
        if (!ensureModifiable('Cell cannot be updated.'))
        {
            return;
        }

        var result = call("ncubeController.getCellNoExecute", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, _cellId]);

        if (result.status === false)
        {
            _errorId = showNote('Unable to fetch the cell contents: ' + result.data);
            return;
        }

        var cellInfo = result.data;
        // Set the cell value (String)
        _editCellValue.val(cellInfo.value ? cellInfo.value : "");
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
        _editCellRadioURL.find('input').prop('checked', cellInfo.isUrl);

        // Show/Hide the Cache check box
        _editCellCache.toggle(cellInfo.isUrl && cellInfo.dataType != "exp" && cellInfo.dataType != "model");

        // Set the Cache check box state
        _editCellCache.find('input').prop('checked', cellInfo.isCached);

        _editCellModal.modal('show');
    }

    function editCellClear()
    {
        _editCellModal.modal('hide');
        var result = call("ncubeController.updateCell", [_selectedCubeName, _selectedApp, _selectedVersion, _cellId, null]);

        if (result.status === false)
        {
            _cellId = null;
            _errorId = showNote('Unable to clear cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        _uiCellId.html('');
        _uiCellId.attr({'class':'cell'});
        _cellId = null;
    }

    function editCellCancel()
    {
        _cellId = null;
        _editCellModal.modal('hide');
    }

    function editCellOK()
    {
        var cellInfo = {'@type':'com.cedarsoftware.ncube.CellInfo'};
        cellInfo.isUrl = _editCellRadioURL.find('input').is(':checked');
        cellInfo.value = _editCellValue.val();
        cellInfo.dataType = cellInfo.isUrl ? _urlDropdown.val() : _valueDropdown.val();
        cellInfo.isCached = _editCellCache.find('input').is(':checked');
        _editCellModal.modal('hide');

        var result = call("ncubeController.updateCell", [_selectedCubeName, _selectedApp, _selectedVersion, _cellId, cellInfo]);

        if (result.status === false)
        {
            _cellId = null;
            _errorId = showNote('Unable to update cell:<hr class="hr-small"/>' + result.data);
            return;
        }

        if (cellInfo.isUrl)
        {
            _uiCellId.html(cellInfo.value);
            _uiCellId.attr({'class':'cell cell-url'});
        }
        else if (cellInfo.dataType == "exp" || cellInfo.dataType == "method")
        {
            var pre2 = $('<pre/>').attr({'class':'ncube-pre'});
            pre2.html(cellInfo.value);
            _uiCellId.html('');
            _uiCellId.append(pre2);
            _uiCellId.attr({'class':'cell cell-code'});
        }
        else
        {
            _uiCellId.html(cellInfo.value);
            _uiCellId.attr({'class':'cell'});
        }
        _cellId = null;
        reloadCube();
    }

    // --------------------------------------------------------------------------------------------
    function buildDropDown(listId, inputId, list, callback)
    {
        var ul = $(listId);
        ul.empty();
        $.each(list, function (key, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.html(value);
            anchor.click(function ()
            {   // User clicked on a dropdown entry, copy its text to input field
                $(inputId).val(anchor.html());
                callback(anchor.html());
            });
            li.append(anchor);
            ul.append(li);
        });
    }

    function doesItemExist(item, list)
    {
        var found = false;
        $.each(list, function (index, value)
        {
            if (item.toLowerCase() === value.toLowerCase())
            {
                found = true;
                return;
            }
        });
        return found;
    }

    function showNote(msg, title)
    {
        return $.gritter.add({
            title: (title || 'Note'),
            text: msg,
            image: './img/cube-logo.png',
            sticky: true
        });
    }

    function clearError()
    {
        if (_errorId)
        {
            $.gritter.remove(_errorId);
            _errorId = null;
        }
    }

    function saveScroll()
    {
        var div = $('#ncubeTabContent');
        var top = div.scrollTop();
        console.log("saveScroll()");
        console.log(top);
        console.log(typeof top);
        localStorage['top'] = top;
    }

    function loadScroll()
    {
        var div = $('#ncubeTabContent');
        console.log("loadScroll()");
        console.log(localStorage['top']);
        console.log(typeof localStorage['top']);
        div.scrollTop(localStorage['top']);
    }

    function getUniqueId()
    {
        return _colIds--;
    }
});
