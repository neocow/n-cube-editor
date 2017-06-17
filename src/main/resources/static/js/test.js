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
    var _selectedTestName = null;
    var _testResultsDiv = null;
    var _testList = null;
    var _testListWarning = null;
    var _testListItems = null;
    var _testLayoutCenter = null;
    var _testParameters = null;
    var _testAssertions = null;
    var _testResults = null;
    var _testCount = null;

    var _testParametersDiv = null;
    var _testAssertionsDiv = null;
    var _testNameDiv = null;
    var _testButtonGroupDiv = null;

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

            _testResultsDiv = $('#testResultsDiv');
            _testList = $('#testList');
            _testListWarning = $('#testListWarning');
            _testListItems = $('#testListItems');
            _testLayoutCenter = $('#testLayoutCenter');
            _testParameters = $('#testParameters');
            _testAssertions = $('#testAssertions');
            _selectedTestName = $('#selectedTestName');
            _testResults = $('#testResults');
            _testCount = $('#testCount');

            _testParametersDiv = $('#testParametersDiv');
            _testAssertionsDiv = $('#testAssertionsDiv');
            _testNameDiv = $('#testNameDiv');
            _testButtonGroupDiv = $('#testButtonGroupDiv');

            secondaryLayout.resizeAll();

            addListeners();
        }
    }

    function addListeners() {
        $('#addParameter').on('click', function() {
            addNewParameter();
        });
        $('#addAssertion').on('click', function () {
            addNewAssertion();
        });
        $('#createTestsLink, #createNewTestMenu').on('click', function (e) {
            e.preventDefault();
            createNewTest();
        });
        $('#runTestButton').on('click', function (e) {
            e.preventDefault();
            runCurrentTest();
        });
        $('#runAllTestsMenu').on('click', function (e) {
            e.preventDefault();
            runAllTests();
        });
        $('#deleteAllTestsMenu').on('click', function (e) {
            e.preventDefault();
            deleteAllTestsMenu();
        });
        _selectedTestName.on('click', function() {
            onLabelClick(this, renameTest);
        });
    }

    function loadTestListView(funcName, genTests) {
        var result;
        if (!nce.getSelectedCubeName()) {
            _selectedTestName[0].innerHTML = 'No n-cube selected.';
            return;
        }

        _testData = null;
        _testSelectionAnchor = -1;
        enableTestItems();

        result = nce.call(funcName, [nce.getSelectedTabAppId(), nce.getSelectedCubeName()]);
        if (result.status) {
            if (result.data && result.data.length) {
                _testData = result.data;
                _testSelectionAnchor = 0;
            } else if (genTests) {
                nce.showNote('The cube is empty, no test input generated.  Create a single test.');
            }
            refreshTestList();
        } else {
            nce.showNote('Error fetching test data for: ' + nce.getSelectedCubeName() + (result.data ? (':<hr class="hr-small"/>' + result.data) : ''));
        }
    }

    function hasTests() {
        return _testData && _testData.length;
    }

    function refreshTestList() {
        var i, len, test, html;
        _testListItems.empty();
        if (hasTests()) {
            len = _testData.length;
            _testCount[0].textContent = len;
            for (i = 0; i < len; i++) {
                test = _testData[i];
                html = '<li><a class="' + (i === _testSelectionAnchor ? 'selected' : '') + '" data-index="' + i + '">' + test.name + '</a></li>';
                _testListItems.append(html);
            }

            _testListItems.find('a').on('click', function(e) {
                var link = $(e.currentTarget);
                _testSelectionAnchor = link.data('index');
                clearTestSelection();
                link.addClass('selected');
                enableTestItems();
                loadTestView();
            });
        }

        enableTestItems();
        loadTestView();

        if (hasTests()) {
            _testListWarning.attr('hidden', true);
            _testList.removeAttr('hidden');
        } else {
            _testListWarning.removeAttr('hidden');
            _testList.attr('hidden', true);
        }
    }

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

    function loadTestView() {
        var test;
        clearTestView();

        if (!_testData || !_testData.length || _testSelectionAnchor >= _testData.length || _testSelectionAnchor < 0) {
            _testSelectionAnchor = -1;
            return;
        }

        test = _testData[_testSelectionAnchor];
        _selectedTestName[0].textContent = test.name;
        loadTestOutput();
        loadCoordinates(test);
        loadAssertions(test);

        _testParametersDiv
            .add(_testAssertionsDiv)
            .add(_testNameDiv)
            .add(_testButtonGroupDiv).fadeIn('fast');
    }

    function loadCoordinates(test) {
        var i, len, keys, key, value, isUrl, v, dataType;
        var coordinates = test.coord;
        if (coordinates) {
            keys = Object.keys(coordinates);
            for (i = 0, len = keys.length; i < len; i++) {
                key = keys[i];
                if (key.substring(0, 1) !== '@') {
                    value = coordinates[key];
                    isUrl = value && value.isUrl;
                    v = (value && value.value !== null) ? value.value : '';
                    dataType = (value && value.dataType) ? value.dataType : null;
                    _testParameters.append(buildParameter({labelText:key, type:dataType, isUrl:isUrl, value:v, hasSelector:true, isRenumerable:false}));
                }
            }
        }
    }

    function loadAssertions(test) {
        var i, len, value, isUrl, v;
        var assertions = test.expected;
        if (assertions) {
            for (i = 0, len = assertions.length; i < len; i++) {
                value = assertions[i];
                isUrl = value && value.isUrl;
                v = (value && value.value !== null) ? value.value : '';
                _testAssertions.append(buildParameter({labelText:i + 1, type:'exp', isUrl:isUrl, value:v, isRenumerable:true}));
            }
        }
    }

    function onLabelClick(lab, updateFunc) {
        var textBox;
        var label = $(lab);
        var oldVal = lab.innerHTML;
        if (!label.find('input').length) {
            textBox = $('<input type="text" value="' + oldVal + '"/>')
                .blur(function () {
                    updateFunc(this, oldVal);
                })
                .keyup(function(e) {
                    if (e.keyCode === KEY_CODES.ENTER) {
                        updateFunc(this, oldVal);
                    }
                });
            label.empty();
            label.append(textBox);
            textBox.focus();
            textBox.select();
        }
    }

    function updateParameterName(textBox, oldVal) {
        var newVal = textBox.value;
        var label = $(textBox).parent();
        if (newVal !== '') {
            label.html(newVal);
            if (newVal !== oldVal) {
                label.parent().data('parameter-id', newVal);
                saveAllTests(false);
            }
        }
    }

    function buildParameter(opts) {
        function getUrlButtonText(isUrl) {
            return isUrl ? '&nbsp;URL&nbsp;' : 'Value';
        }

        function onUrlButtonClick(button) {
            var selector;
            var inputGroup = $(button).closest('div');
            var oldEls = inputGroup.find('div, select');
            var isUrl = button.innerHTML === 'Value';
            button.innerHTML = getUrlButtonText(isUrl);

            if (oldEls.length) {
                selector = createTypeSelector(inputGroup.data('type'), isUrl);
                oldEls.remove();
                inputGroup.append(selector);
                selector.selectpicker();
            }
        }

        var selector;
        var id = generateRandomId();
        var valId = id + '-value';
        var html = '<div class="form-group" data-renumerable="' + opts.isRenumerable + '" data-parameter-id="' + opts.labelText + '" id="' + id + '">'
                 + '<span class="control-label renameable">' + opts.labelText + '</span>'
                 + '<span class="pull-right glyphicon glyphicon-remove"></span>'
                 + '<div class="input-group input-group-sm" data-type="' + opts.type + '">'
                 + '<span class="input-group-btn"><button type="button" class="btn btn-default param">' + getUrlButtonText(opts.isUrl) + '</button></span>'
                 + '<input class="form-control" type="text" id="' + valId + '" value="' + opts.value + '"/>'
                 + '</div></div>';
        var labelGroup = $(html);

        if (opts.hasSelector) {
            selector = createTypeSelector(opts.type, opts.isUrl);
            labelGroup.find('div.input-group').append(selector);
            selector.selectpicker();
        }

        labelGroup.find('span.glyphicon-remove').on('click', function() {
            deleteParameter($(this).parent());
        });
        labelGroup.find('button.param').on('click', function () {
            onUrlButtonClick(this);
        });

        labelGroup.find('span.control-label').on('click', function() {
            onLabelClick(this, updateParameterName);
        });

        return labelGroup;
    }

    function renameAssertions() {
        var i, len, group, count;
        var groups = _testAssertions.find('.form-group');
        for (i = 0, len = groups.length; i < len; i++) {
            count = i + 1;
            group = $(groups[i]);
            group.data('parameter-id', count);
            group.find('label.control-label').html("" + count + ".");
            group.find('a[data-parameter-id]').data('parameter-id', count);
        }
    }

    function deleteParameter(formGroup) {
        var isRenumerable = formGroup.data('renumerable');
        formGroup.remove();
        if (isRenumerable) {
            renameAssertions();
        }
        saveAllTests(false)
    }

    function enableTestItems() {
        var enabled = getSelectedTestList().length === 1;

        enableDisableMenuButton($("#renameCurrentTestMenu"), enabled, renameTest);
        enableDisableMenuButton($("#runCurrentTestMenu"), enabled, runCurrentTest);
        enableDisableMenuButton($("#duplicateCurrentTestMenu"), enabled, duplicateCurrentTestMenu);
        enableDisableMenuButton($("#deleteCurrentTestMenu"), enabled, deleteCurrentTestMenu);

        if (enabled) {
            clearTestView();
        }
    }

    function deleteCurrentTestMenu() {
        if (verifyTestSelected()) {
            _testData.splice(_testSelectionAnchor, 1);
            _testSelectionAnchor = -1;
            refreshTestList();
            saveAllTests(true);
        }
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

    function renameTest(input, oldName) {
        var textbox = $(input);
        var newName = textbox.val();
        if (newName === oldName || !validateTestName(newName)) {
            _selectedTestName.html(oldName);
            return;
        }

        getSelectedTestList().html(newName);
        _selectedTestName.html(newName);
        _testData[_testSelectionAnchor].name = newName;
        saveAllTests(false);
    }

    function addNewParameter() {
        var param = buildParameter({labelText:'newInput', type:'string', value:'', hasSelector:true, isRenumerable:false});
        _testParameters.append(param);
        saveAllTests(false);
        param.find('.control-label').click();
    }

    function addNewAssertion() {
        var count = _testAssertions.find('.form-group').length;
        var param = buildParameter({labelText:count+1, type:'exp', value:'output.return', isRenumerable:true});
        _testAssertions.append(param);
        saveAllTests(false);
    }

    function preRunTest() {
        nce.clearNote();
        if (!nce.getSelectedCubeName()) {
            nce.showNote('No n-cube selected. Test cannot be run.');
            return false;
        }

        if (!_testData || _testSelectionAnchor === -1) {
            nce.showNote('No test selected.  Test cannot be run.');
            return false;
        }
        _testResults.html('Running test...');
        setOutputHeaderColor(null);
        return true;
    }

    function runCurrentTest() {
        if (!preRunTest()) {
            return;
        }
        try {
            setTimeout(function() {
                callRunTests(false);
            }, 1);
        } catch (e) {
            nce.showNote("Could not run cube test:<hr class=\"hr-small\"/>" + e.message);
        }
    }

    function runAllTests() {
        if (!preRunTest()) {
            return;
        }
        try {
            setTimeout(function() {
                callRunTests(true);
            }, 1);
        } catch (e) {
            nce.showNote("Could not run cube test:<hr class=\"hr-small\"/>" + e.message);
        }
    }

    function callRunTests(runAllTests) {
        var result, testResult;
        var test = getActiveTest();
        var testToSend = runAllTests ? _testData : test;
        var method = runAllTests ? CONTROLLER_METHOD.RUN_TESTS : CONTROLLER_METHOD.RUN_TEST;
        _testData[_testSelectionAnchor] = test;

        result = nce.call(CONTROLLER + method, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), testToSend]);
        saveAllTests(true);

        if (result.status) {
            testResult = runAllTests ? result.data[test.name] : result.data;
            showTestResult(testResult['_result'], testResult['_message']);
        } else {
            showTestResult(false, 'Could not run test: ' + result.data);
        }

        _testLayoutCenter.find('> .well').animate({
            scrollTop: _testResultsDiv.offset().top
        }, 200);
    }

    function saveAllTests(modelIsUpToDate) {
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
    }

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
        var parameters = _testParameters.find('> div[data-parameter-id]');
        var coord = {};

        for (i = 0, len = parameters.length; i < len; i++) {
            v = $(parameters[i]);
            coord[v.data('parameter-id')] = retrieveCellInfo(v);
        }
        return coord;
    }

    function retrieveAssertions() {
        var i, len;
        var assertions = _testAssertions.find('> div[data-parameter-id]');
        var array = [];

        for (i = 0, len = assertions.length; i < len; i++) {
            array.push(retrieveCellInfo($(assertions[i])));
        }
        return array;
    }

    function retrieveCellInfo(group) {
        return {
            '@type': 'com.cedarsoftware.ncube.CellInfo',
            value: group.find('input').val(),
            isUrl: group.find('button.param').text() !== 'Value',
            dataType: group.find('select').val() || 'exp'
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
            _testResults[0].innerHTML = html ? html : 'No prior test results';
        }
    }

    function storeTestOutput() {
        var s = sessionStorage[TEST_RESULTS];
        var testOutput = s ? JSON.parse(s) : {};
        testOutput[getTestKey()] = _testResults[0].innerHTML;
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

    function deleteAllTestsMenu() {
        nce.clearNote();
        if (!nce.getSelectedCubeName()) {
            nce.showNote('No n-cube is currently selected. You cannot delete all tests.');
            return;
        }
        FormBuilder.openBuilderModal(NCEBuilderOptions.deleteAllTests({ afterSave: deleteAllTestsOk }));
    }

    function deleteAllTestsOk() {
        _testData = [];
        _testSelectionAnchor = -1;
        saveAllTests(true);
        refreshTestList();
    }

    function createNewTest() {
        var result;
        if (!nce.ensureModifiable('Unable to create a test.')) {
            return;
        }
        result = nce.call(CONTROLLER + CONTROLLER_METHOD.CREATE_NEW_TEST, [nce.getSelectedTabAppId(), nce.getSelectedCubeName(), 'newTest']);
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

        _testList.find('div.panel-body').animate({
            scrollTop: getSelectedTestList().offset().top
        }, 200);
        _selectedTestName.click();
    }

    function duplicateCurrentTestMenu() {
        var test;
        if (verifyTestSelected()) {
            test = getSelectedTestFromModel();
            addTestToList(duplicateTest(test, test.name + '-Duplicate'));
        }
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

    function getSelectedTestFromModel() {
        return findTestByName(getSelectedTestList().first().text());
    }

    function duplicateTest(test, newTestName) {
        return {
            '@type': 'com.cedarsoftware.ncube.NCubeTest',
            name: newTestName,
            coord: $.extend(true, {}, test['coord']),
            expected: $.extend(true, [], test['expected'])
        };
    }

    function createTypeSelector(typeStr, url) {
        var selector = $('<select class="selectpicker show-tick show-menu-arrow" data-width="auto" data-style="btn-default"/>');
        return fillTypeSelector(selector, typeStr || 'string', url);
    }

    function fillTypeSelector(selector, typeStr, url) {
        var keys, i, len, key;
        if (!selector) {
            return;
        }
        selector.empty();

        keys = Object.keys(DATA_TYPES);
        for (i = 0, len =  keys.length; i < len; i++) {
            key = keys[i];
            if (!url || URL_ENABLED_LIST.indexOf(key) > -1) {
                selector.append($('<option>' + DATA_TYPES[key] + '</option>').val(key));
            }
        }

        selector.val(typeStr);
        return selector;
    }

    function leftPad(str, length) {
        var pad = length - str.length;
        return (pad < 1 || pad > 11) ? str : NUMBER_PADDING[pad] + str;
    }

    // these are just temporary to use as an id
    function generateRandomId() {
        return leftPad(Math.random().toString(36).substring(7), 11);
    }

    function load() {
        if (nce.getInfoDto()) {
            loadTestListView(CONTROLLER + CONTROLLER_METHOD.GET_TESTS, false);
        }
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
            window.parent.frameLoaded(document);
        }, 1);
    }

    return {
        init: init,
        load: load,
        handleCubeSelected: handleCubeSelected
    };

})(jQuery);

function tabActivated(info) {
    TestEditor.init(info);
    TestEditor.load();
}

function cubeSelected() {
    TestEditor.handleCubeSelected();
}

function onNoteEvent(e, element){}

function closeChildMenu() {
    $('.open').removeClass('open');
    $('div.dropdown-backdrop').hide();
}
