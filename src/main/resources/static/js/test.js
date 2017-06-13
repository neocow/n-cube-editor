/**
 * Test Panel for N-Cube Editor
 *
 * @author Ken Partlow (kpartlow@gmail.com) / John DeRegnaucourt (jdereg@gmail.com)
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

var TestEditor = (function ($) {
    var nce = null;
    var _testData = null;
    var _testSelectionAnchor = -1;
    var _duplicateTestModal = null;
    var _deleteTestModal = null;
    var _deleteParameterModal = null;
    var _createNewTestModal = null;
    var _renameTestModal = null;
    var _selectedTestName = null;
    var _testResultsDiv = null;
    var _testList = null;
    var _testListWarning = null;
    var _testListItems = null;
    var _testLayoutCenter = null;
    var _testParameters = null;
    var _testAssertions = null;
    var _testResults = null;

    var _testParametersDiv = null;
    var _testAssertionsDiv = null;
    var _testNameDiv = null;
    var _testButtonGroupDiv = null;

    var _padding = ["", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000", "0000000000" ];

    function init(info) {
        var secondaryLayout;
        if (!nce) {   // One-time initialization code.
            nce = info;

            secondaryLayout = $('#testBody').layout({
                name: "secondaryLayout"
                ,   closable:					true	// pane can open & close
                ,	resizable:					true	// when open, pane can be resized
                ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
                ,	livePaneResizing:			true
                ,	east__minSize:				110
                ,   east__resizeable:           true
                ,   east__size:                 140
                ,   east__triggerEventsOnLoad:  true
                ,   east__maskContents:         true
                ,   center__triggerEventsOnLoad: true
                ,   center__maskContents:       true
                ,   togglerLength_open:         60
                ,   togglerLength_closed:       "100%"
                ,	spacing_open:			5  // ALL panes
                ,	spacing_closed:			5 // ALL panes
            });

            _duplicateTestModal = $('#duplicateTestModal');
            _deleteTestModal = $('#deleteTestmodal');
            _deleteParameterModal = $('#deleteParameterModal');
            _createNewTestModal = $('#createNewTestModal');
            _renameTestModal = $('#renameTestModal');
            _testResultsDiv = $('#testResultsDiv');
            _testList = $('#testList');
            _testListWarning = $('#testListWarning');
            _testListItems = $('#testListItems');
            _testLayoutCenter = $('#testLayoutCenter');
            _testParameters = $('#testParameters');
            _testAssertions = $('#testAssertions');
            _selectedTestName = $('#selectedTestName');
            _testResults = $('#testResults');

            _testParametersDiv = $('#testParametersDiv');
            _testAssertionsDiv = $('#testAssertionsDiv');
            _testNameDiv = $('#testNameDiv');
            _testButtonGroupDiv = $('#testButtonGroupDiv');

            secondaryLayout.resizeAll();

            $('#renameTestOk').click(function (e)
            {
                e.preventDefault();
                renameTestOk();
            });
            $('#deleteParameterOk').click(function (e)
            {
                e.preventDefault();
                deleteParameterOk();
            });
            $('#addParameter').click(function (e)
            {
                addNewParameter();
            });
            $('#addAssertion').click(function (e)
            {
                addNewAssertion();
            });
            $('#createTestsLink').click(function (e)
            {
                e.preventDefault();
                createNewTestMenu();
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

            _renameTestModal.on( 'keypress', function( e ) {
                if( e.keyCode === KEY_CODES.ENTER ) {
                    e.preventDefault();
                    $('#renameTestOk').click();
                }
            });

            _deleteParameterModal.on( 'keypress', function( e ) {
                if( e.keyCode === KEY_CODES.ENTER ) {
                    e.preventDefault();
                    $('#deleteParameterOk').click();
                }
            });

            _duplicateTestModal.on( 'keypress', function( e ) {
                if( e.keyCode === KEY_CODES.ENTER ) {
                    e.preventDefault();
                    $('#duplicateCurrentTestOk').click();
                }
            });

            _deleteTestModal.on( 'keypress', function( e ) {
                if( e.keyCode === KEY_CODES.ENTER ) {
                    e.preventDefault();
                    $('#deleteTestOk').click();
                }
            });

            _createNewTestModal.on( 'keypress', function( e ) {
                if( e.keyCode === KEY_CODES.ENTER ) {
                    e.preventDefault();
                    $('#addParameterOk').click();
                }
            });

            $('#runTestButton').click(function (e) {
                e.preventDefault();
                runCurrentTest();
            });

            $('#deleteTestOk').click(function (e) {
                deleteTestOk();
                e.preventDefault();
            });

            $('#createNewTestMenu').click(function (e) {
                e.preventDefault();
                createNewTestMenu();
            });

            $('#createNewTestOk').click(function (e) {
                e.preventDefault();
                createNewTestOk();
            });

            $("#duplicateCurrentTestOk").click(function (e) {
                e.preventDefault();
                duplicateCurrentTestOk();
            });

            $('#deleteAllTestsMenu').click(function (e) {
                e.preventDefault();
                deleteAllTestsMenu();
            });

            $('#deleteAllTestsOk').click(function (e) {
                e.preventDefault();
                deleteAllTestsOk();
            });

            _selectedTestName.on('click', function(e) {

            });
        }
    }

    var loadTestListView = function(funcName, genTests)
    {
        if (!nce.getSelectedCubeName())
        {
            _selectedTestName[0].innerHTML = 'No n-cube selected.';
            return false;
        }

        _testData = null;
        _testSelectionAnchor = -1;

        enableTestItems();

        var testListResult = nce.call(funcName, [nce.getSelectedTabAppId(), nce.getSelectedCubeName()]);

        if (testListResult.status === true)
        {
            if (testListResult.data && testListResult.data.length > 0)
            {
                _testData = testListResult.data;
                _testSelectionAnchor = 0;
            }
            else if (genTests)
            {
                nce.showNote('The cube is empty, no test input generated.  Create a single test.');
            }
            refreshTestList();
        }
        else
        {
            var msg = 'Error fetching test data for: ' + nce.getSelectedCubeName();
            if (testListResult.data != null)
            {
                msg += (':<hr class="hr-small"/>' + testListResult.data);
            }
            nce.showNote(msg);
        }
    };

    function hasTests() {
        return _testData && _testData.length;
    }

    var refreshTestList = function()
    {
        _testListItems.empty();

        if (hasTests())
        {
            $("#testCount")[0].textContent = _testData.length;
            $.each(_testData, function (index, value)
            {
                var anchor = $('<a/>');
                anchor[0].textContent = value['name'];

                if (index == _testSelectionAnchor)
                {
                    anchor.toggleClass("selected");
                }

                anchor.click(function (e)
                {
                    var link = $(e.currentTarget);
                    _testSelectionAnchor = index;
                    clearTestSelection();
                    link.addClass("selected");
                    enableTestItems();
                    loadTestView();
                });

                var li = $("<li/>");
                li.append(anchor);
                _testListItems.append(li);
            });
        }

        enableTestItems();
        loadTestView();

        if (hasTests())
        {
            _testListWarning.attr('hidden', true);
            _testList.removeAttr('hidden');
        }
        else
        {
            _testListWarning.removeAttr('hidden');
            _testList.attr('hidden', true);
        }
    };

    function clearTestView() {
        _testParametersDiv
            .add(_testAssertionsDiv)
            .add(_testNameDiv)
            .add(_testButtonGroupDiv)
            .add(_testResultsDiv).hide();

        _testParameters
            .add(_testAssertions)
            .add(_testResults).empty();
    }

    var loadTestView = function()
    {
        clearTestView();

        if (!_testData || !_testData.length || _testSelectionAnchor >= _testData.length || _testSelectionAnchor < 0)
        {
            _testSelectionAnchor = -1;
            return;
        }

        var testData = _testData[_testSelectionAnchor];
        var testParameters = _testParameters;

        try
        {
            _selectedTestName[0].textContent = testData['name'];
            loadTestOutput();
            var coordinate = testData['coord'];

            if (coordinate)
            {
                $.each(coordinate, function (key, value)
                {
                    if (key.substring(0, 1) != "@")
                    {
                        var isUrl = (value == null || value.isUrl == null) ? false : value.isUrl;
                        var v = (value == null || value.value == null) ? null : value.value;
                        var dataType = (value == null || value.dataType == null) ? null : value.dataType;
                        testParameters.append(buildParameter(key, dataType, isUrl, v, true, false, deleteParameter));
                    }
                });
            }

            var assertions = testData['expected'];
            if (assertions)
            {
                $.each(assertions, function (index, value)
                {
                    var isUrl = value['isUrl'] == null ? null : value['isUrl'];
                    var v = value.value == null ? null : value.value;
                    _testAssertions.append(buildParameter(index + 1, "exp", isUrl, v, false, true, deleteAssertion));
                });
            }
        }
        catch (e)
        {
            console.log(e);
            nce.showNote('Unable to load test view:<hr class="hr-small"/>' + e.message);
        }

        _testParametersDiv
            .add(_testAssertionsDiv)
            .add(_testNameDiv)
            .add(_testButtonGroupDiv).fadeIn('fast');
    };

    var buildParameter = function(labelText, type, isUrl, value, hasSelector, isRenumberable, deleteFunc)
    {
        var ident = generateRandomId();
        var labelGroup = $("<div/>").attr({'class': 'form-group', 'parameter-id':labelText, 'id':ident});
        var cat = ident + '-value';
        var label = $('<span/>').addClass('control-label').html(labelText);
        var deleteParamButton = $('<a/>').attr({'class':'red-item pull-right', 'font-size':'9px', 'style':'padding: 1px 3px; font-size: 12px; line-height: 1.5; border-radius: 3px;'});
        var glyph = $('<span/>').attr({'class':'glyphicon glyphicon-remove', 'style':'vertical-align: -1px;'});

        label.click(function() {
            if (label.find('input').length === 0) {
                var updateParameterName = function() {
                    var newVal = textBox.val();
                    if (newVal !== '') {
                        label.html(newVal);
                        labelGroup.attr('parameter-id', newVal);
                        saveAllTests(false);
                    }
                };
                var textBox = $('<input/>')
                    .prop('type', 'text')
                    .val(label[0].innerHTML)
                    .blur(function () {
                        updateParameterName();
                    })
                    .keyup(function(e) {
                        if (e.keyCode === KEY_CODES.ENTER) {
                            updateParameterName();
                        }
                    });
                label.empty();
                label.append(textBox);
                textBox.focus();
                textBox.select();
            }
        });

        deleteParamButton.append(glyph);
        deleteParamButton.click(deleteFunc);

        labelGroup.append(label);
        labelGroup.append(deleteParamButton);

        var inputGroupAddon = $("<span/>").attr({'class':'input-group-btn'});
        var controls = $("<div/>").attr({'class': 'controls'});
        var inputGroup = $("<div/>").attr({'class':'input-group input-group-sm'});
        var urlButton = $("<button/>").attr({'type':'button', 'class':'btn btn-default', 'value':'url'});

        if (isUrl)
        {
            urlButton[0].innerHTML = '&nbsp;URL&nbsp;';
        }
        else
        {
            urlButton[0].textContent = 'Value';
        }

        urlButton.click(function ()
        {
            var isValue;
            var txt = urlButton.text();
            if (txt == "Value")
            {
                urlButton[0].innerHTML = '&nbsp;URL&nbsp;';
                isValue = false;
            }
            else
            {
                urlButton.html("Value");
                isValue = true;
            }

            if (hasSelector)
            {
                var selector = createTypeSelector(type, !isValue);
                inputGroup.find("select").remove();
                inputGroup.find("div").remove();
                inputGroup.append(selector);
                selector.selectpicker();
            }
        });

        inputGroupAddon.append(urlButton);
        inputGroup.append(inputGroupAddon);
        inputGroup.append($("<input/>").attr({'class': 'form-control', 'type': 'text', 'id': cat}).val(value));

        if (hasSelector)
        {
            var selector = createTypeSelector(type, isUrl);
            inputGroup.append(selector);
            selector.selectpicker();
        }

        controls.append(inputGroup);
        labelGroup.append(controls);

        return labelGroup;
    };

    var renameAssertions = function()
    {
        $("#testAssertions").find(".form-group").each(function (index, value)
        {
            var v = $(value);
            var count = index + 1;
            v.attr('parameter-id', count);
            v.find('label.control-label').html("" + count + ".");
            v.find('a[parameter-id]').attr('parameter-id', count);
        });
    };

    var deleteParameter = function(e)
    {
        var parent = $(e.currentTarget).parent("div.form-group");
        var param = parent.attr('parameter-id');
        var test = getSelectedTestName();

        var title = "Delete Parameter?";
        var label = "Delete parameter '" + param + "' from '" + test + "'?";

        deleteTestItem(title, label, false, parent.attr('id'));
    };

    var deleteAssertion = function(e)
    {
        var parent = $(e.currentTarget).parent("div.form-group");
        var param = parent.attr('parameter-id');
        var test = getSelectedTestName();
        var title = "Delete Assertion?";
        var label = "Delete assertion '" + param + "' from '" + test + "'?";

        deleteTestItem(title, label, true, parent.attr('id'));
    };

    var deleteTestItem = function(title, label, isRenumberable, paramId)
    {
        nce.clearNote();
        if (!nce.getSelectedCubeName())
        {
            nce.showNote('No n-cube selected. Nothing to delete.');
            return;
        }

        $('#deleteParameterTitle').html(title);
        $('#deleteParameterLabel').html(label);
        $('#deleteParameterHiddenId').val(paramId);
        $('#deleteParameterOk').attr({'isRenumberable': isRenumberable});
        _deleteParameterModal.modal({
            keyboard: true
        });
    };

    var deleteParameterOk = function()
    {
        _deleteParameterModal.modal('hide');

        var id = $('#deleteParameterHiddenId').val();
        var isRenumberable = $('#deleteParameterOk').attr('isRenumberable');

        $('#' + id).remove();

        if (isRenumberable == "true") {
            renameAssertions();
        }

        saveAllTests(false);
    };

    function enableTestItems() {
        var enabled = getSelectedTestList().length === 1;

        enableDisableMenuButton($("#renameCurrentTestMenu"), enabled, renameCurrentTestMenu);
        enableDisableMenuButton($("#runCurrentTestMenu"), enabled, runCurrentTest);
        enableDisableMenuButton($("#duplicateCurrentTestMenu"), enabled, duplicateCurrentTestMenu);
        enableDisableMenuButton($("#deleteCurrentTestMenu"), enabled, deleteCurrentTestMenu);

        if (enabled) {
            clearTestView();
        }
    }

    function deleteCurrentTestMenu() {
        if (!verifyTestSelected()) {
            return;
        }

        $('#deleteTestTitle').html("Delete Test");
        $('#deleteTestLabel').html("Are you sure you want to delete the test '" + _testData[_testSelectionAnchor].name + "'?");
        $('#deleteTestModal').modal({
            keyboard: true
        });
    }

    var deleteTestOk = function()
    {
        nce.clearNote();

        $('#deleteTestModal').modal('hide');

        _testData.splice(_testSelectionAnchor, 1);
        _testSelectionAnchor = -1;

        refreshTestList();
        saveAllTests(true);
    };

    function renameCurrentTestMenu() {
        var test;
        if (!verifyTestSelected()) {
            return;
        }

        test = _testData[_testSelectionAnchor].name;

        $('#renameTestOldName').val(test);
        $('#renameTestNewName').val('');
        $('#renameTestLabel').html('Rename \'' + test + '\'?');

        _renameTestModal.modal({
            keyboard: true
        });
    }

    function testNameAlreadyExists(name) {
        var i, len;
        if (hasTests()) {
            for (i = 0, len = _testData.length; i < len; i++) {
                if (_testData[i].name === name) {
                    return true;
                }
            }
        }
        return false;
    }

    function validateTestName(name) {
        var nameRegex = /^([A-Za-z0-9.-]{3,64})$/g;
        nce.clearNote();

        if (!nameRegex.test(name)) {
            nce.showNote('Test name is invalid. Test names can only contain letters, numbers, ., and -.');
            return false;
        }

        if (testNameAlreadyExists(name)) {
            nce.showNote('There is already a test with that name.');
            return false;
        }

        return true;
    }

    var renameTestOk = function() {
        var newName = $('#renameTestNewName').val().trim();

        if (!validateTestName(newName)) {
            return;
        }

        _renameTestModal.modal('hide');

        // change name of selected test
        $('#testListItems').find('li').find('a.selected').html(newName);
        _selectedTestName.html(newName);

        // change currently selected model item
        _testData[_testSelectionAnchor].name = newName;

        saveAllTests(false);
    };

    function addNewParameter() {
        // check to see if parameter already exists in parameter-key of #testAssertions .form-group
        var param = buildParameter('newInput', 'string', false, '', true, false, deleteParameter);
        _testParameters.append(param);
        saveAllTests(false);
        param.find('.control-label').click();
    }

    function addNewAssertion()
    {
        var count = _testAssertions.find('.form-group').length;
        var param = buildParameter(count+1, "exp", false, 'output.return', false, true, deleteAssertion);
        _testAssertions.append(param);
        saveAllTests(false);
    }

    var runCurrentTest = function()
    {
        nce.clearNote();
        if (!nce.getSelectedCubeName())
        {
            nce.showNote('No n-cube selected. Test cannot be run.');
            return;
        }

        if (_testData == null || _testSelectionAnchor == -1)
        {
            nce.showNote('No test selected.  Test cannot be run.');
            return;
        }

        try
        {
            $('#testResults').html('Running test...');
            setOutputHeaderColor(null);

            setTimeout(function()
            {
                var test = getActiveTest();
                _testData[_testSelectionAnchor] = test;

                var result = nce.call("ncubeController.runTest", [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), test]);
                saveAllTests(true);

                if (result.status != true)
                {
                    showTestResult(false, "Could not run test:  " + result.data);

                    _testLayoutCenter.find('> .well').animate({
                        scrollTop: _testResultsDiv.offset().top
                    }, 200);

                    return;
                }

                showTestResult(result.data["_result"], result.data["_message"]);

                _testLayoutCenter.find('> .well').animate({
                    scrollTop: _testResultsDiv.offset().top
                }, 200);

                // Only uncomment when running tests that create n-cubes and you want the list to auto-refresh.
                //nce.loadNCubes();
                //nce.loadNCubeListView();
                //nce.runSearch();
            }, 1);
        }
        catch (e)
        {
            nce.showNote("Could not run cube test:<hr class=\"hr-small\"/>" + e.message);
        }
    };

    var saveAllTests = function(modelIsUpToDate) {
        var test, result;
        if (!nce.ensureModifiable("Unable to save all tests.")) {
            return;
        }

        if (!_testData) {
            nce.showNote('No test selected.  There are no tests to save.');
            return;
        }

        if (!modelIsUpToDate) {
            test = getActiveTest();
            if (test) { //  If a test is currently selected
                //  locate test in list to add it in...and add it before saving.
                _testData[_testSelectionAnchor] = test;
            }
        }

        result = nce.call(CONTROLLER + CONTROLLER_METHOD.SAVE_TESTS, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), _testData]);
        if (!result.status) {
            nce.showNote("Unable to save tests:<hr class=\"hr-small\"/>" + result.data);
        }
    };

    function getActiveTest() {
        var name = getSelectedTestName();
        return name ? {
            '@type': 'com.cedarsoftware.ncube.NCubeTest',
            name: name,
            coord: retrieveParameters(),
            expected: retrieveAssertions()
        } : null;
    }

    function retrieveParameters() {
        var i, len, v;
        var parameters = _testParameters.find("> div[parameter-id]");
        var coord = {};

        for (i = 0, len = parameters.length; i < len; i++) {
            v = $(parameters[i]);
            coord[v.attr("parameter-id")] = retrieveCellInfo(v, true);
        }
        return coord;
    }

    function retrieveAssertions() {
        var i, len;
        var assertions = _testAssertions.find('> div[parameter-id]');
        var array = [];

        for (i = 0, len = assertions.length; i < len; i++) {
            array.push(retrieveCellInfo($(assertions[i]), false));
        }
        return array;
    }

    function retrieveCellInfo(group, hasSelector) {
        return {
            '@type': 'com.cedarsoftware.ncube.CellInfo',
            value: group.find('input').val(),
            isUrl: group.find('button[value]').text() !== 'Value',
            dataType: hasSelector ? group.find('select').val() : 'exp'
        };
    }

    function showTestResult(success, message) {
        _testResultsDiv.hide();
        setOutputHeaderColor(success);
        _testResults.html(message);
        _testResultsDiv.fadeIn('fast');
        storeTestOutput();
    }

    function setOutputHeaderColor(success) {
        var suc = 'panel-success';
        var dang = 'panel-danger';
        if (success) {
            _testResultsDiv.addClass(suc).removeClass(dang);
        } else if (success === false) {
            _testResultsDiv.addClass(dang).removeClass(suc);
        } else {
            _testResultsDiv.removeClass(suc + ' ' + dang);
        }
    }

    function getSelectedTestName() {
        return _selectedTestName.html();
    }

    function getTestKey() {
        return nce.getSelectedCubeInfoKey() + TAB_SEPARATOR + getSelectedTestName();
    }

    function loadTestOutput() {
        var testOutput, html;
        var testOutputStr = sessionStorage[TEST_RESULTS];
        if (hasTests() && testOutputStr) {
            _testResultsDiv.fadeIn('fast');
            testOutput = JSON.parse(testOutputStr);
            html = testOutput[getTestKey()];
            setOutputHeaderColor(html ? html.indexOf('Could not run test:') === -1 : null);
            _testResultsDiv[0].innerHTML = html ? html : 'No prior test results';
        }
    }

    function storeTestOutput() {
        var s = sessionStorage[TEST_RESULTS];
        var testOutput = s ? JSON.parse(s) : {};
        testOutput[getTestKey()] = _testResultsDiv[0].innerHTML;
        sessionStorage[TEST_RESULTS] = JSON.stringify(testOutput);
    }

    function clearTestSelection() {
        getSelectedTestList().removeClass('selected');
        enableTestItems();
    }

    function findTestByName(name) {
        var i, len, test;
        if (_testData) {
            for (i = 0, len = _testData.length; i < len; i++) {
                test = _testData[i];
                if (test.name === name) {
                    return test;
                }
            }
        }
    }

    var createNewTestMenu = function()
    {
        if (!nce.ensureModifiable("Unable to create a test."))
        {
            return;
        }

        if ($('#createNewTestMenu').parent().hasClass('disabled'))
        {
            return;
        }

        $('#createNewTestField').val('');
        _createNewTestModal.modal({
            keyboard: true
        });
    };

    var deleteAllTestsMenu = function()
    {
        nce.clearNote();

        if ($('#deleteAllTestsMenu').parent().hasClass('disabled'))
        {
            return;
        }

        if (!nce.getSelectedCubeName())
        {
            nce.showNote('No n-cube is currently selected. You cannot delete all tests.');
            return;
        }

        $('#deleteAllTestsModal').modal({
            keyboard: true
        });
    };

    var deleteAllTestsOk = function()
    {
        $('#deleteAllTestsModal').modal('hide');

        _testData = [];
        _testSelectionAnchor = -1;

        saveAllTests(true);
        refreshTestList();
    };

    function createNewTestOk() {
        var result;
        var newName = $('#createNewTestField').val().trim();
        if (!validateTestName(newName)) {
            return;
        }

        result = nce.call(CONTROLLER + CONTROLLER_METHOD.CREATE_NEW_TEST, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), newName]);
        if (result.status) {
            addTestToList(result.data);
        } else {
            nce.showNote('Error creating new test for ' + nce.getSelectedCubeName() + ':<hr class="hr-small"/>' + result.data);
        }
    }

    function addTestToList(test) {
        if (!_testData) {
            _testData = [];
        }
        _testData.push(test);
        _testSelectionAnchor = _testData.length - 1;

        refreshTestList();
        saveAllTests(true);

        _createNewTestModal.modal('hide');

        _testList.find('div.panel-body').animate({
            scrollTop: getSelectedTestList().offset().top
        }, 200);
    }

    function duplicateCurrentTestMenu() {
        if (!verifyTestSelected()) {
            return;
        }

        $('#duplicateTestTitle').html('Duplicate \'' + $(getSelectedTestList()[0]).text().trim() + '\' ?');
        $('#duplicateTestNameField').val('');
        $('#duplicateTestModal').modal();
    }

    function verifyTestSelected() {
        nce.clearNote();

        if (!nce.getSelectedCubeName()) {
            nce.showNote('No n-cube is currently selected.');
            return false;
        }

        list = getSelectedTestList();
        if (list.length !== 1) {
            if (list.length) {
                nce.showNote('More than one test is selected.');
            } else {
                nce.showNote('No test is currently selected.');
            }
            return false;
        }

        return true;
    }

    function getSelectedTestList() {
        return _testListItems.find('a.selected');
    }

    var getSelectedTestFromModel = function()
    {
        var list = getSelectedTestList();
        return findTestByName($(list[0]).text().trim());
    };

    var duplicateCurrentTestOk = function() {
        var newName = $('#duplicateTestNameField').val().trim();
        if (validateTestName(newName)) {
            $('#duplicateTestModal').modal('hide');
            addTestToList(duplicateTest(getSelectedTestFromModel(), newName));
        }
    };

    function duplicateTest(test, newTestName) {
        return {
            '@type': 'com.cedarsoftware.ncube.NCubeTest',
            name: newTestName,
            coord: $.extend(true, {}, test['coord']),
            expected: $.extend(true, [], test['expected'])
        };
    }

    function createTypeSelector(typeStr, url) {
        var selector = $("<select/>").attr({'class': 'selectpicker show-tick show-menu-arrow', 'data-width':'auto', 'data-style': 'btn-default'});
        return fillTypeSelector(selector, typeStr || 'string', url);
    }

    function fillTypeSelector(selector, typeStr, url) {
        var options, i, len, item, opt;
        if (!selector) {
            return;
        }
        selector.empty();

        options = $('#datatypes-' + (url ? 'url' : 'value')).find('option');
        for (i = 0, len =  options.length; i < len; i++) {
            item = $(options[i]);
            opt = $('<option/>').val(item.val());
            opt.text = item.text;
            selector.append(opt);
        }

        selector.val(typeStr);
        return selector;
    }

    var leftPad = function(str, length)
    {
        var pad = length - str.length;

        if (pad < 1 || pad > 11) {
            return str;
        }

        return _padding[pad] + str;
    };

    // these are just temporary to use as an id
    var generateRandomId = function()
    {
        return leftPad(Math.random().toString(36).substring(7), 11);
    };

    var load = function()
    {
        var info = nce.getInfoDto();
        if (!info)
        {
            return;
        }

        loadTestListView("ncubeController.getTests", false);
    };

    var handleCubeSelected = function()
    {
        load();
    };

    // Let parent (main frame) know that the child window has loaded.
    // The loading of all of the Javascript (deeply) is continuous on the main thread.
    // Therefore, the setTimeout(, 1) ensures that the main window (parent frame)
    // is called after all Javascript has been loaded.
    if (window.parent.frameLoaded) {
        setTimeout(function () {
            window.parent.frameLoaded(document);
        }, 1);
    }

    return {
        init: init,
        load: load,
        handleCubeSelected: handleCubeSelected
    };

})(jQuery);

var tabActivated = function tabActivated(info)
{
    TestEditor.init(info);
    TestEditor.load();
};

var cubeSelected = function cubeSelected()
{
    TestEditor.handleCubeSelected();
};

function onNoteEvent(e, element){}

function closeChildMenu() {
    $('.open').removeClass('open');
    $('div.dropdown-backdrop').hide();
}