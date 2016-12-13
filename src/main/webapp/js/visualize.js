/**
 * Graphical representation of n-cubes
 *
 * @author Beata Heekin (bbheekin@gmail.com)
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

var Visualizer = (function ($) {

    var _network = null;
    var _nce = null;
    var _loadedCubeName = null;
    var _loadedAppId = null;
    var _nodes = [];
    var _edges = [];
    var _scope = null;
    var _scopeChange = false;
    var _keepCurrentScope = false;
    var _availableScopeKeys = [];
    var _selectedGroups = null;
    var _availableGroupsAtLevel = null;
    var _availableGroupsAllLevels = null;
    var _availableScopeValues = {};
    var _allGroups = null;
    var _maxLevel = null;
    var _groupSuffix = null;
    var _selectedCubeName = null;
    var _loadTraits = false;
    var _selectedLevel = null;
    var _countNodesAtLevel = null;
    var _visualizerInfo = null;
    var _visualizerNetwork = null;
    var _visualizerContent = null;
    var _visualizerHtmlError = null;
    var TWO_LINE_BREAKS = '<BR><BR>';
    var _nodeDetailsTitle = null;
    var _nodeCubeLink = null;
    var _nodeVisualizer = null;
    var _nodeTraits = null;
    var _nodeDetails = null;
    var _layout = null;
    var _scopeBuilderTable = null;
    var _scopeBuilderModal = null;
    var _scopeBuilderScope = [];
    var _scopeInput = null;
    var _scopeBuilderListenersAdded = false;
    var _selectAllNoneListenersAdded = false;
    var STATUS_SUCCESS = 'success';
    var STATUS_MISSING_START_SCOPE = 'missingStartScope';
    var _scopeLastKeyTime = Date.now();
    var _scopeKeyPressed = false;
    var SCOPE_KEY_DELAY = 3000;
    var UNSPECIFIED = 'UNSPECIFIED';
    var _clickTimer = null;
    var _clicks = 0;

    //Network layout parameters
    var _hierarchical = false;

    //Network physics
    var _networkOptionsButton = null;
    var _networkOptionsSection = null;
    var _networkOptionsChangeSection = null;
    var _networkDefaultsVis = {};
    var _networkDefaults = null;
    var _networkOptions = null;
    var _overriddenNetworkOptions =
    {
        height: getVisNetworkHeight(),
        interaction: {
            navigationButtons: true,
            keyboard: {
                enabled: false,
                speed: {x: 5, y: 5, zoom: 0.02}
            },
            zoomView: true
        },
        nodes: {
            value: 24,
            scaling: {
                min: 24,
                max: 24,
                label: {
                    enabled: true
                }
            }
        },
        edges: {
            arrows: {
                to: {
                    enabled: true
                }
            },
            color: {
                color: 'gray'
            },
            smooth: {
                enabled: true
            },
            hoverWidth: 3,
            font: {
                size: 20
            }
        },
        physics: {
            barnesHut: {
                gravitationalConstant: -30000
            }
        },
        layout: {
            hierarchical: {
                enabled: _hierarchical
            },
            improvedLayout : true,
            randomSeed:2
        },
        groups: {
            PRODUCT: {
                shape: 'box',
                color: '#DAE4FA'
            },
            RISK: {
                shape: 'box',
                color: '#759BEC'
            },
            COVERAGE: {
                shape: 'box',
                color: '#113275',
                font: {
                    color: '#D8D8D8'
                }
            },
            CONTAINER: {
                shape: 'star',
                color: "#731d1d",
                font: {
                    buttonColor: '#D8D8D8'
                }
            },
            LIMIT: {
                shape: 'ellipse',
                color: '#FFFF99'
            },
            DEDUCTIBLE: {
                shape: 'ellipse',
                color: '#FFFF99'
            },
            PREMIUM: {
                shape: 'ellipse',
                color: '#0B930B',
                font: {
                    color: '#D8D8D8'
                }
            },
            RATE: {
                shape: 'ellipse',
                color: '#EAC259'
            },
            ROLE: {
                shape: 'box',
                color: '#F59D56'
            },
            ROLEPLAYER: {
                shape: 'box',
                color: '#F2F2F2'
            },
            RATEFACTOR: {
                shape: 'ellipse',
                color: '#EAC259'
            },
            PARTY: {
                shape: 'box',
                color: '#004000',
                font: {
                    color: '#D8D8D8'
                }
            },
            PLACE: {
                shape: 'box',
                color: '#481849',
                font: {
                    color: '#D8D8D8'
                }
            },
            UNSPECIFIED: {
                shape: 'box',
                color: '#7ac5cd'
            },
            FORM: {
                shape: 'box',
                color: '#d2691e'
            },
            PRODUCT_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            RISK_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            COVERAGE_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            LIMIT_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            PREMIUM_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            RATE_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            RATEFACTOR_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            ROLE_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            ROLEPLAYER_ENUM : {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            CONTAINER_ENUM: {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            DEDUCTIBLE_ENUM: {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            PARTY_ENUM: {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            PLACE_ENUM: {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            },
            UNSPECIFIED_ENUM: {
                shape: 'dot',
                scaling: {min: 5, max: 5},
                color: 'gray'
            }
        }
    };



    //Set by network stabilize events
    var _stabilizationStatus = null;
    var _iterationsToStabilize = null;
    
    var init = function (info) {
        if (!_nce) {
            _nce = info;

            _layout = $('#visBody').layout({
                name: 'visLayout'
                ,	livePaneResizing:			true
                ,   east__minSize:              50
                ,   east__maxSize:              1000
                ,   east__size:                 250
                ,   east__closable:             true
                ,   east__resizeable:           true
                ,   east__initClosed:           true
                ,   east__slidable:             true
                ,   center__triggerEventsOnLoad: true
                ,   center__maskContents:       true
                ,   togglerLength_open:         60
                ,   togglerLength_closed:       '100%'
                ,	spacing_open:			    5  // ALL panes
                ,	spacing_closed:			    5 // ALL panes
            });

            _visualizerContent = $('#visualizer-content');
            _visualizerHtmlError = $('#visualizer-error');
            _visualizerInfo = $('#visualizer-info');
            _visualizerNetwork = $('#visualizer-network');
            _nodeDetailsTitle = $('#nodeDetailsTitle');
            _nodeCubeLink = $('#nodeCubeLink');
            _nodeTraits = $('#nodeTraits');
            _nodeVisualizer = $('#nodeVisualizer');
            _nodeDetails = $('#nodeDetails');
            _scopeBuilderTable = $('#scopeBuilderTable');
            _scopeBuilderModal = $('#scopeBuilderModal');
            _scopeInput = $('#scope');
            _networkOptionsSection = $('#networkOptionsSection');
            _networkOptionsChangeSection = $('#networkOptionsChangeSection');

            $(window).resize(function() {
                if (_network) {
                    _network.setSize('100%', getVisNetworkHeight());
                }
            });

             $('#selectedLevel-list').change(function () {
                _selectedLevel = Number($('#selectedLevel-list').val())
                saveToLocalStorage(_selectedLevel, SELECTED_LEVEL);
                reload();
            });

            $('#hierarchical').change(function () {
                //Hierarchical mode is disabled due to what appears to be a bug in vis.js or because the
                //visualizer.js code is missing something. Tis problem started when convering to use
                //visjs DataSets for network data.
                //The problem is that when visjs creates the network.body nodes from the network.body.data
                //nodes, it doesn't set the level on the network.body nodes. This causes an exception
                //if hierarchical mode is selected since that mode requires either all or no nodes to
                //have a defined level. Attempted short-term fix in setLevelOnNetworkNodes() method, but it's not enough.
                //TODO: Keep investigating, submit question and possibly a bug fix to visjs.
               // $('#hierarchical').prop('checked', false);
               // _nce.showNote('Hierarchical mode is currently not available');
                _hierarchical = this.checked;
                saveToLocalStorage(_hierarchical, HIERARCHICAL);
                updateNetworkOptions();
            });

            _scopeInput.on('change', function () {
                _scopeKeyPressed = false;
                scopeChange();
            });
            
            _scopeInput.on('input', function () {
                _scopeKeyPressed = true;
                _scopeLastKeyTime = Date.now();
            });

            scopeKeyDelayLoop();

            initNetworkOptions();

            //Network options parameters
            _networkOptionsSection.hide();
            $('#networkOptionsButton').click(function () {
                $('#networkOptionsButton').toggleClass('active');
                _networkOptionsButton = $('#networkOptionsButton').hasClass('active');
                saveToLocalStorage(_networkOptionsButton, NETWORK_OPTIONS_DISPLAY);
                _networkOptionsSection.toggle();
            });

            $('#networkOptionsChangeSection').change(function ()
            {
                _networkOptionsChangeSection = $('#networkOptionsChangeSection');
                $('.networkOption').each(function(){
                    var id, keys;
                    id = $(this).attr('id');
                    keys = id.split('.');
                    setNetworkOption($(this), _networkOptions, keys);
                });
                destroyNetwork();
                initNetwork();
            });
        }
    };

    function setNetworkOption(inputOption, options, keys)
    {
        var key, value, errorMessage;
        if (keys) {
            key = keys[0];
            value = options[key];
            if (typeof value === OBJECT)
            {
                keys.splice(key, 1);
                setNetworkOption(inputOption, value, keys)
            }
            else if (typeof value === BOOLEAN)
            {
                options[key] = inputOption.prop('checked');
            }
            else if (typeof value === NUMBER)
            {
                options[key] = Number(inputOption.val());
            }
            else if (typeof value === 'function')
            {
                //TODO: add this back in 
                //options[key] = Function(inputOption.val());
            }
            else
            {
                options[key] = inputOption.val();
            }
        }
        else
        {
            errorMessage = 'Invalid state encountered while updating network options for parameter ' + inputOption.attr('id') + '.';
            _nce.showNote(errorMessage);
            throw new Error(errorMessage);
        }
    }

     function initNetworkOptions()
    {
        var emptyDataSet, emptyNetwork, container;
        container = document.getElementById('network');
        emptyDataSet = new vis.DataSet({});
        emptyNetwork = new vis.Network(container, {nodes:emptyDataSet, edges:emptyDataSet}, {});

        _networkDefaultsVis.physics = emptyNetwork.physics.defaultOptions;
        _networkDefaultsVis.physics = emptyNetwork.physics.defaultOptions;
        _networkDefaultsVis.layout = emptyNetwork.layoutEngine.defaultOptions;
        _networkDefaultsVis.nodes = emptyNetwork.nodesHandler.defaultOptions;
        _networkDefaultsVis.edges = emptyNetwork.edgesHandler.defaultOptions;
        _networkDefaultsVis.groups = emptyNetwork.groups.defaultOptions;
        _networkDefaultsVis.interaction = emptyNetwork.interactionHandler.defaultOptions;
        _networkDefaultsVis.manipulation = emptyNetwork.manipulation.defaultOptions;

        //TODO: Figure out why key throws "unknown" exception in visjs when set on the network. Removing for now.
        delete _networkDefaultsVis.physics.barnesHut['theta'];  
        delete _networkDefaultsVis.physics.forceAtlas2Based['theta'];  
        delete _networkDefaultsVis.physics.repulsion['avoidOverlap'];

        _networkDefaults = $.extend(true, {}, _networkDefaultsVis);
        _networkOptions = $.extend(true, _networkDefaults, _overriddenNetworkOptions);

        emptyNetwork.destroy();
    }

    function loadNetworkOptionsSectionView()
    {
        $('#networkOptionsButton').prop('checked', _networkOptionsButton);
        if (_networkOptionsButton)
        {
            $('#networkOptionsButton').addClass('active');
            _networkOptionsSection.show();
        }
        else
        {
            $('#networkOptionsButton').removeClass('active');
            _networkOptionsSection.hide();
        }
        
        $("#stabilizationStatus").val(_stabilizationStatus);
        $("#iterationsToStabilize").val(_iterationsToStabilize);

        $('#networkOptionsChangeSection').empty();
        buildNetworkOptionsChangeSection( $('#networkOptionsChangeSection'), null, _networkOptions, _networkDefaults, _networkDefaultsVis);
    }

    function buildNetworkOptionsChangeSection(section, parentKey, networkOptions, networkDefaults, networkDefaultsVis)
    {
        $.each(networkDefaults, function (key, defaultValue) {
            var rowBordersDiv, value, defaultKeyVis, defaultValueVis;
            value = networkOptions[key];
            if (networkDefaultsVis)
            {
                defaultKeyVis =  key in networkDefaultsVis ? key : null;
                defaultValueVis = networkDefaultsVis[key];
            }
            else
            {
                defaultKeyVis = null;
                defaultValueVis = null;
            }

            rowBordersDiv = buildRowBordersDiv(section, parentKey, key, value, defaultValue, defaultKeyVis, defaultValueVis);
            section.append(rowBordersDiv);
        });
    }

    function buildRowBordersDiv(section, parentKey, key, value, defaultValue, defaultKeyVis, defaultValueVis)
    {
        var rowBordersDiv, col1Div, col2Div, col3Div, col4Div, col5Div, col2Input, col4Input, col5Input, fullKey, differentVisDefaultValue;

        fullKey = parentKey === null ? key : parentKey + '.' + key;

        rowBordersDiv = $('<div/>').prop({class: "row borders"});
        col1Div = $('<div/>').prop({class: "col-md-3"});
        col2Div = $('<div/>').prop({class: "col-md-1", title:"Value used for network"});
        col3Div = $('<div/>').prop({class: "col-md-1"});
        col4Div = $('<div/>').prop({class: "col-md-2", title:"Default value for network"});
        col5Div = $('<div/>').prop({class: "col-md-5", title:"Vis default value, if different"});

        if (typeof defaultValue === OBJECT)
        {
            buildNetworkOptionsChangeSection(section, fullKey, value, defaultValue, defaultValueVis);
        }
        else
        {
            if (typeof defaultValue === BOOLEAN) {
                col2Input = $('<input/>').prop({class: 'networkOption', id: fullKey, type: "checkbox"});
                col2Input[0].checked = value;
                col4Input = $('<input/>').prop({id: fullKey + 'Default', type: "checkbox", disabled: "true"});
                col4Input[0].checked = defaultValue;
            }
            else {
                col2Input = $('<input/>').prop({class: 'networkOption', id: fullKey, type: "text"});
                col2Input[0].value = value;
                col4Input = $('<input/>').prop({id: fullKey + 'Default', type: "text", disabled: "true"});
                col4Input[0].value = defaultValue;
            }
            
            col1Div[0].innerHTML = fullKey
            col2Div.append(col2Input);
            col3Div[0].innerHTML = NBSP;
            col4Div.append(col4Input);

            if (defaultKeyVis)
            {
                differentVisDefaultValue = defaultValueVis === defaultValue ? null : defaultValueVis;
                if (typeof differentVisDefaultValue === BOOLEAN) {
                    col5Input = $('<input/>').prop({class: 'networkOption', id: fullKey, type: "checkbox", disabled: "true"});
                    col5Input[0].checked = differentVisDefaultValue;
                    }
                else  if (differentVisDefaultValue !== null)
                {
                    col5Input = $('<input/>').prop({class: 'networkOption', id: fullKey, type: "text", disabled: "true"});
                    col5Input[0].value = differentVisDefaultValue;
                 }
                col5Div.append(col5Input);
            }
            else {
                col5Div[0].innerHTML = 'Parameter does not exist in Vis'
            }

            rowBordersDiv.append(col1Div);
            rowBordersDiv.append(col2Div);
            rowBordersDiv.append(col3Div);
            rowBordersDiv.append(col4Div);
            rowBordersDiv.append(col5Div);
        }
        return rowBordersDiv;
    }

    function scopeKeyDelayLoop() {
        var now;
        setInterval(function() {
            now = Date.now();
            if (now - _scopeLastKeyTime > SCOPE_KEY_DELAY && _scopeKeyPressed) {
                _scopeKeyPressed = false;
                scopeChange();
            }
       }, SCOPE_KEY_DELAY);
    }
    
    function scopeChange()
    {
        _scope = buildScopeFromText(_scopeInput.val());
        _scopeChange = true;
        saveToLocalStorage(_scope, SCOPE_MAP);
        updateScopeBuilderScope();
        load();
    }

    function updateScopeBuilderScope()
    {
        var key, value, shouldInsertNewExpression, expression;
        var keys = Object.keys(_scope);
        for (var i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            value = _scope[key];
            shouldInsertNewExpression = true;
            for (var j = 0, jLen = _scopeBuilderScope.length; j < jLen; j++) {
                expression = _scopeBuilderScope[j];
                if (expression.isApplied && expression.key === key) {
                    expression.value = value;
                    shouldInsertNewExpression = false;
                    break;
                }
            }
            if (shouldInsertNewExpression) {
                _scopeBuilderScope.push({'isApplied': true, 'key': key, 'value': value});
            }
        }
    }

    function buildScopeFromText(scopeString) {
        var tuple, key, value;
        var newScope = {};
        if (scopeString) {
            var tuples = scopeString.split(',');
            for (var i = 0, iLen = tuples.length; i < iLen; i++) {
                tuple = tuples[i].split(':');
                key = tuple[0].trim();
                value = tuple[1];
                if (value) {
                    newScope[key] = value.trim();
                }
            }
         }
        return newScope;
    }

    var reload = function () {
        _nce.clearAllErrors();
        setTimeout(function () {reloadNetwork();}, PROGRESS_DELAY);
        _nce.showNote('Updating network...');
    }

    var reloadNetwork = function () {
        _nce.clearError();
        updateNetworkOptions();
        updateNetworkData();
        loadSelectedLevelListView();
        loadGroupsView();
        loadCountsView();
        _visualizerInfo.show();
        _visualizerNetwork.show();
     };

    var loadTraits = function (node) {
        _nce.clearAllErrors();
        setTimeout(function () {loadTraitsFromServer(node);}, PROGRESS_DELAY);
        node.loadTraits ? _nce.showNote('Loading traits...') :  _nce.showNote('Removing traits...');
    }

    var loadTraitsFromServer = function(node)
    {
         var message, options, result, json, visInfo;
        _nce.clearError();
        options =
        {
            node: node,
            scope: _scope,
            availableScopeKeys: _availableScopeKeys,
            availableScopeValues: _availableScopeValues
        };
        
        result = _nce.call('ncubeController.getVisualizerTraits', [_nce.getSelectedTabAppId(), options]);
        if (result.status === false) {
            _nce.showNote('Failed to load traits: ' + TWO_LINE_BREAKS + result.data);
            return node;
        }

        json = result.data;

        if (json.status === STATUS_SUCCESS) {
            if (json.message !== null) {
                _nce.showNote(json.message);
            }
            visInfo = json.visInfo
            node = visInfo.nodes['@items'][0];
            _scope = visInfo.scope;
            delete _scope['@type'];
            delete _scope['@id'];
            saveAllToLocalStorage();
            updateScopeBuilderScope();
            _availableScopeValues = visInfo.availableScopeValues;
            _availableScopeKeys = visInfo.availableScopeKeys['@items'].sort();
            replaceNode(_nodes, node);
             _nodeDetails[0].innerHTML = node.details;
            _nodeTraits = $('#nodeTraits');
            _nodeTraits[0].innerHTML = '';
            _nodeTraits.append(createTraitsLink(node));
         }
        else {
            message = json.message;
            if (json.stackTrace != null) {
                message = message + TWO_LINE_BREAKS + json.stackTrace
            }
            _nce.showNote('Failed to load traits: ' + TWO_LINE_BREAKS + message);
        }
        return node;
    }

    var load = function () {
        _nce.clearAllErrors();
        setTimeout(function () {loadFromServer();}, PROGRESS_DELAY);
        _nce.showNote('Loading visualizer...');
    }

    var loadFromServer = function ()
    {
        var options, result, json, message;
        _nce.clearError();
        clearVisLayoutEast()

        if (!_nce.getSelectedCubeName()) {
            destroyNetwork();
            _visualizerContent.hide();
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + 'No cube selected.');
            return;
        }

        //TODO: The .replace is temporary until figured out why nce.getSelectedCubeName()
        //TODO: occasionally contains a cube name with "_" instead of "." (e.g. rpm_class_product instead of
        //TODO: rpm.class.product) after a page refresh.
        _selectedCubeName = _nce.getSelectedCubeName().replace(/_/g, '.')

        if (_keepCurrentScope)
        {
            _keepCurrentScope = false;
        }
        else{
            _scope = getFromLocalStorage(SCOPE_MAP, null);
        }
        _selectedLevel = getFromLocalStorage(SELECTED_LEVEL, null);
        _selectedGroups = getFromLocalStorage(SELECTED_GROUPS, null);
        _hierarchical = getFromLocalStorage(HIERARCHICAL, false);
        _networkOptionsButton = getFromLocalStorage(NETWORK_OPTIONS_DISPLAY, null);

        if (_loadedAppId && !appIdMatch(_loadedAppId, _nce.getSelectedTabAppId()))
        {
            _availableScopeKeys = null;
            _availableScopeValues = null;
        }

        if (_selectedCubeName !== _loadedCubeName)
        {
            destroyNetwork();
        }
        else if (_scopeChange)
        {
            _scopeChange = false;
            destroyNetwork();
        }
  
        options =
        {
            selectedLevel: _selectedLevel,
            startCubeName: _selectedCubeName,
            scope: _scope,
            selectedGroups: _selectedGroups,
            availableScopeKeys: _availableScopeKeys,
            availableScopeValues: _availableScopeValues,
            loadTraits: _loadTraits
        };


        result = _nce.call('ncubeController.getVisualizerJson', [_nce.getSelectedTabAppId(), options]);
        if (result.status === false) {
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + result.data);
            destroyNetwork();
            _visualizerContent.hide();
            _visualizerInfo.hide();
            _visualizerNetwork.hide();
            return;
        }

        json = result.data;

        if (json.status === STATUS_SUCCESS) {
            if (json.message !== null) {
                _nce.showNote(json.message);
            }
            loadData(json.visInfo, json.status);
            initNetwork();
            loadSelectedLevelListView();
            saveAllToLocalStorage();
            updateScopeBuilderScope();
            loadScopeView();
            loadHierarchicalView();
            loadNetworkOptionsSectionView();
            loadGroupsView();
            loadCountsView();
            _visualizerContent.show();
            _visualizerInfo.show();
            _visualizerNetwork.show();
        }
        else if (json.status === STATUS_MISSING_START_SCOPE) {
            _nce.showNote(json.message);
            loadData(json.visInfo, json.status);
            initNetwork();
            saveAllToLocalStorage();
            updateScopeBuilderScope();
            loadScopeView();
            _visualizerContent.show();
            _visualizerInfo.hide();
            _visualizerNetwork.hide();
        }
        else {
            destroyNetwork();
            _visualizerContent.hide();
             message = json.message;
            if (json.stackTrace != null) {
                message = message + TWO_LINE_BREAKS + json.stackTrace
            }
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + message);
        }
 
         if (_scopeBuilderListenersAdded === false){
            availableScopeKeys = _availableScopeKeys;
            availableScopeValues = _availableScopeValues;
            addScopeBuilderListeners();
            _scopeBuilderListenersAdded = true;
        }
    };

    function appIdMatch(appIdA, appIdB)
    {
        return appIdA.appId === appIdB.appId &&
            appIdA.version === appIdB.version &&
            appIdA.status ===  appIdB.status &&
            appIdA.branch === appIdB.branch;
    }

    function clearVisLayoutEast(){
        _nodeDetailsTitle[0].innerHTML = '';
        _nodeCubeLink[0].innerHTML = '';
        _nodeVisualizer[0].innerHTML = '';
        _nodeTraits[0].innerHTML = '';
        _nodeDetails[0].innerHTML = '';
        _layout.close('east');
    }

    function loadHierarchicalView() {
        $('#hierarchical').prop('checked', _hierarchical);
    }

    function loadScopeView() {
        _scopeInput.val(getScopeString());
    }

    var loadGroupsView = function()
    {
        var groupName, id, available, label, title, input, selected, button, groups,
            background, fontMap, color, groupMap, boxShadow;
        var divGroups = $('#groups');

        divGroups.empty();
        groups = _networkOptions.groups;

        _availableGroupsAllLevels.sort();
        for (var j = 0; j < _availableGroupsAllLevels.length; j++) {
            color = null;
            boxShadow = ''
            groupName = _availableGroupsAllLevels[j];
            id = groupName + _groupSuffix;
            available = groupCurrentlyAvailable(id);
            label = _allGroups[groupName];
            title = available ? "Show/hide " + label + " in the graph" : "Increase level to enable show/hide of " + label + " in the graph";

            selected = false;
            for (var k = 0; k < _selectedGroups.length; k++) {
                if (groupIdsEqual(id, _selectedGroups[k])) {
                    selected = true;
                    break;
                }
            }

            groupMap = groups[groupName];
            groupMap = groupMap ? groupMap : options.groups[UNSPECIFIED];
            background = groupMap.color;
            fontMap = groupMap.font;
            color = null;
            if (fontMap)
            {
                color = fontMap.buttonColor ? fontMap.buttonColor : fontMap.color;
            }
            color = color ? color : '#000000';

            button = $('<button/>');
            if (selected) {
                button.attr({class: 'btn active btn-primary-group'});
                boxShadow = 'box-shadow: 0 4px #575757;';
            }
            else if (available) {
                button.attr({class: 'btn btn-default-group'});
             }
            else{
                button.attr({class: 'btn btn-default-group'});
                button.attr({disabled: 'disabled'});
            }
            button.attr({style:'background: ' +  background + '; color: ' + color + ';font-size: 12px; padding: 3px; margin-right: 3px; margin-bottom: 7px;border-radius: 15px;' + boxShadow});
            button.attr({'data-cat': label, 'data-toggle': "buttons", 'id': id, 'title': title});
            var input = $('<input>').attr({type: 'checkbox', autocomplete: 'off'});
            button.append(input);
            button.append(label);
            button.click(function (e) {
                e.preventDefault();
                groupChangeEvent(this.id);
            });
            divGroups.append(button);
        }

        if (_selectAllNoneListenersAdded === false) {
            $('#selectAll').click(function (e) {
                e.preventDefault();
                $('#groups').find('button').each(function () {
                    $(this).addClass('active');
                });

                Array.prototype.push.apply(_selectedGroups, _availableGroupsAllLevels);
                saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
                reload();
            });

            $('#selectNone').click(function (e) {
                e.preventDefault();
                $('#groups').find('button').each(function () {
                    $(this).removeClass('active');
                });
                _selectedGroups = [];
                saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
                reload();
            });
            _selectAllNoneListenersAdded = true;
        }
    };

    function getScopeString(){
        var scopeLen, key;
        var scopeString = '';
        var keys = Object.keys(_scope);
        for (var i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            scopeString += key + ': ' + _scope[key] + ', ';
        }
        scopeLen = scopeString.length;
        if (scopeLen > 1) {
            scopeString = scopeString.substring(0, scopeLen - 2);
        }
        return scopeString;
    }

    function groupChangeEvent(groupId)
    {
        var button;

        $('#groups').find('button').each(function()
        {
            button = $(this);
            if(button.attr('id') === groupId){

                if  (button.hasClass('active'))
                {
                    for (var i = 0, len = _selectedGroups.length; i < len; i++) {
                        if (groupIdsEqual(groupId, _selectedGroups[i])) {
                            _selectedGroups.splice(i, 1);
                            button.removeClass('active');
                            break;
                        }
                    }
                }
                else
                {
                    for (var k = 0, kLen = _availableGroupsAllLevels.length; k < kLen; k++)
                    {
                        if (groupIdsEqual(groupId, _availableGroupsAllLevels[k])) {
                            _selectedGroups.push(_availableGroupsAllLevels[k]);
                            button.addClass('active');
                            break;
                        }
                    }
                }
                saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
                reload();
                return;
            }
        });
    }

    function groupCurrentlyAvailable(groupId)
    {
        for (var l = 0; l < _availableGroupsAtLevel.length; l++)
        {
            if (groupIdsEqual(groupId, _availableGroupsAtLevel[l]))
            {
                return true;
            }
        }
        return false
    }

    function groupIdsEqual(groupId1, groupId2)
    {
        var groupId1Prefix = groupId1.split(_groupSuffix)[0];
        var groupId2Prefix = groupId2.split(_groupSuffix)[0];
        return groupId1Prefix === groupId2Prefix;
    }

    function loadCountsView()
    {
        var totalNodeCount = _nodes.length;
        var maxLevelLabel = _maxLevel === 1 ? 'level' : 'levels';
        var nodeCountLabel = totalNodeCount === 1 ? 'node' : 'nodes';

        var nodesDisplayingAtLevelCount = _network.body.data.nodes.length;
        var nodesAtLevelLabel = nodesDisplayingAtLevelCount === 1 ? 'node' : 'nodes';

        $('#levelCounts')[0].textContent = nodesDisplayingAtLevelCount  + ' ' + nodesAtLevelLabel + ' of ' + _countNodesAtLevel + ' displaying at current level';
        $('#totalCounts')[0].textContent = totalNodeCount + ' ' + nodeCountLabel + ' total over ' +  _maxLevel + ' ' + maxLevelLabel ;
    }

    function loadSelectedLevelListView()
    {
        var option;
        var select = $('#selectedLevel-list');
        select.empty();

        for (var j = 1; j <= _maxLevel; j++)
        {
            option = $('<option/>');
            option[0].textContent = j.toString();
            select.append(option);
        }

        select.val('' + _selectedLevel);
    }

    function getVisNetworkHeight() {
        return  '' + ($(this).height() - $('#network').offset().top);
    }
    
    function isSelectedGroup(node)
    {
        if (_selectedGroups) {
            for (var j = 0, jLen = _selectedGroups.length; j < jLen; j++) {
                if (groupIdsEqual(node.group, _selectedGroups[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    function updateNetworkData()
    {
        updateNetworkDataNodes();
        updateNetworkDataEdges();
        _network.clusteredNodeIds = [];
    }


    function updateNetworkDataNodes()
    {
        var node, selectedGroup, groupNamePrefix;
        var networkDataNodes =  _network.body.data.nodes;
        var nodeIdsToRemove = [];
        var nodesToAddBack = [];
        var level = _selectedLevel;
        var selectedGroups = [];
        var availableGroupsAtLevel = [];
        _countNodesAtLevel = 0;

        //given the selected level, determine nodes to exclude, nodes to add back, selected groups and available groups
        for (var i = 0, iLen = _nodes.length; i < iLen; i++)
        {
            node  = _nodes[i];

            selectedGroup = isSelectedGroup(node);

            if (parseInt(node.level) > level)
            {
                nodeIdsToRemove.push(node.id);
            }
            else {
                if (selectedGroup) {
                    //collect selected groups at level
                    groupNamePrefix = node.group.replace(_groupSuffix, '');
                    if (_selectedGroups.indexOf(groupNamePrefix) > -1 && selectedGroups.indexOf(groupNamePrefix) === -1) {
                        selectedGroups.push(groupNamePrefix);
                    }
                    if (!networkDataNodes || !networkDataNodes.get(node.id)){
                        nodesToAddBack.push(node);
                    }
                }
                else{
                    nodeIdsToRemove.push(node.id);
                }
                //collect available groups at level
                groupNamePrefix = node.group.replace(_groupSuffix, '');
                if (availableGroupsAtLevel.indexOf(groupNamePrefix) === -1)
                {
                    availableGroupsAtLevel.push(groupNamePrefix)
                }
                _countNodesAtLevel++;
            }
        }
        networkDataNodes.remove(nodeIdsToRemove);
        networkDataNodes.add(nodesToAddBack);
        _selectedGroups = selectedGroups;
        _availableGroupsAtLevel = availableGroupsAtLevel;
    }

    function updateNetworkDataEdges()
    {
        var edge;
        var networkDataEdges =  _network.body.data.edges;
        var edgeIdsToRemove = [];
        var edgesToAddBack = [];
        var level = _selectedLevel;

        //given the selected level, determine edges to exclude and edges to add back
        if (_edges) {
            for (var k = 0, kLen = _edges.length; k < kLen; k++) {
                edge = _edges[k];

                if (parseInt(edge.level) > level) {
                    edgeIdsToRemove.push(edge.id);
                }
                else if (!networkDataEdges || !networkDataEdges.get(edge.id)){
                    {
                        edgesToAddBack.push(edge);
                    }
                }
            }
        }
        networkDataEdges.remove(edgeIdsToRemove);
        networkDataEdges.add(edgesToAddBack);
    }

    /*
     //TODO: Attempted short-term fix for issue with hierarchical mode, but it's not enough.
     //TODO: Keep investigating, submit question and possibly a bug fix to vis.
    function setLevelOnNetworkNodes()
    {
        var level, node, supportNode, edge, id;
        var networkNodes =  _network.body.nodes;
        var networkDataNodes = _network.body.data.nodes;
        var networkEdges =  _network.body.edges;
        var networkDataEdges = _network.body.data.edges;


        for (var i = 1, iLen = networkDataNodes.length; i < iLen; i++) {
            node = networkNodes[i];
            supportNode = networkNodes["edgeId:" + i];
            id = node.parentEdgeId ? node.parentEdgeId : node.id;
            level = networkDataNodes.get(id).level;
            node.level = level;
            supportNode.level = level;
        }

        for (var i = 1, iLen = networkDataEdges.length; i < iLen; i++) {
            edge = networkEdges[i];
            level = networkDataEdges.get(edge.id).level;
            edge.level = level;
        }
    }*/

    function loadData(visInfo, status)
    {
        var nodes, edges;

        if (status === STATUS_SUCCESS) {
            _allGroups = visInfo.allGroups;
            _availableGroupsAllLevels = visInfo.availableGroupsAllLevels['@items'];
            _selectedGroups = visInfo.selectedGroups['@items'];
            _selectedLevel = visInfo.selectedLevel;
            _groupSuffix = visInfo.groupSuffix;
            _maxLevel = visInfo.maxLevel;
            nodes = visInfo.nodes['@items'];
            edges = visInfo.edges['@items']
            _nodes =  nodes ? nodes : [];
            _edges = edges ? edges : [];
        }
        _scope = visInfo.scope;
        delete _scope['@type'];
        delete _scope['@id'];
        _loadedCubeName = _selectedCubeName;
        _loadedAppId = _nce.getSelectedTabAppId();
        _availableScopeValues = visInfo.availableScopeValues;
        _availableScopeKeys = visInfo.availableScopeKeys['@items'].sort();
     }

    var handleCubeSelected = function() {
        load();
    };

    function clusterDescendantsBySelectedNode(nodeId, immediateDescendantsOnly) {
        _network.clusteredNodeIds.push(nodeId);
        clusterDescendants(immediateDescendantsOnly)
    }

    function clusterDescendants(immediateDescendantsOnly) {
        var id;
        for (var i = 0; i < _network.clusteredNodeIds.length; i++) {
            id = _network.clusteredNodeIds[i];
            clusterDescendantsByNodeId(id, immediateDescendantsOnly);
        }
    }

    function clusterDescendantsByNodeId(nodeId, immediateDescendantsOnly) {
        var clusterOptionsByData = getClusterOptionsByNodeId(nodeId);
        _network.clusterDescendants(nodeId, immediateDescendantsOnly, clusterOptionsByData, true)
    }

    function getClusterOptionsByNodeId(nodeId) {
        var clusterOptionsByData, node, clusterLabel;
        return clusterOptionsByData = {
            processProperties: function (clusterOptions, childNodes) {
                node = getNodeById(childNodes, nodeId);
                clusterLabel = node.label + ' cluster';
                clusterOptions.label = clusterLabel;
                clusterOptions.title = clusterLabel;
                return clusterOptions;
            }
        };
    }

    function openClusterByClusterNodeId(clusterNodeId)  //TEMP: gets called when a clustered node is clicked
    {
        var node, indexNode;
        var nodesInCluster = _network.getNodesInCluster(clusterNodeId);
        for (var i = 0; i < nodesInCluster.length; i++)
        {
            node = nodesInCluster[i];
            indexNode = _network.clusteredNodeIds.indexOf(node);
            if (indexNode !== -1)
            {
                _network.clusteredNodeIds.splice(indexNode, 1);
            }
        }
        _network.openCluster(clusterNodeId)
    }

    function destroyNetwork()
    {
        if (_network) {
            _network.destroy();
            _network = null;
        }
    }

    function markTopNodeSpecial()
    {
        var node = _nodes[0]
        if (node) {
            if (node.id !== '1') {
                throw new Error('Expected node id of 1 for first node in list.')
            }
            node.shapeProperties = {borderDashes: [15, 5]};
            node.borderWidth = 3;
            node.scaling = {min: 200, max: 200, label: {enabled: true}};
        }
    }

    function initNetwork()
    {
        var container, nodeDataSet, edgeDataSet;
        if (_network)
        {
            updateNetworkOptions();
            updateNetworkData();
        }
        else
        {
            container = document.getElementById('network');
            nodeDataSet = new vis.DataSet({});
            markTopNodeSpecial();
            nodeDataSet.add(_nodes)
            edgeDataSet = new vis.DataSet({});
             edgeDataSet.add(_edges)
            _network = new vis.Network(container, {nodes:nodeDataSet, edges:edgeDataSet}, _networkOptions);
            updateNetworkData();
            customizeNetworkForNce(_network);

            _network.on('select', function(params) {
                _clicks++;
                if(_clicks === 1)
                {
                    _clickTimer = setTimeout(function()
                    {
                        networkSingleClick(params)
                        _clickTimer = 0;

                    }, PROGRESS_DELAY);

                } else
                {
                    clearTimeout(_clickTimer);
                    networkDoubleClick(params)
                    _clicks = 0;
                }
            });

            _network.on('doubleClick', function (params) {
                networkDoubleClick(params)
             });

            _network.on('startStabilizing', function () {
                _nce.showNote('Stabilizing network...');
                _iterationsToStabilize = 'iterating...';
                _stabilizationStatus = 'stabilization started';
                 $("#stabilizationStatus").val(_stabilizationStatus);
                $("#iterationsToStabilize").val(_iterationsToStabilize);
             });

            _network.on('stabilizationProgress', function (params) {
                 _stabilizationStatus = 'stabilization in progress';
                 $("#stabilizationStatus").val(_stabilizationStatus);
             });

            _network.on('stabilizationIterationsDone', function () {
                _stabilizationStatus = 'hidden stabilization complete';
                $("#stabilizationStatus").val(_stabilizationStatus);
                //Clear note 'Stabilizing network...' here when hidden stabilization is complete.
                //The full stabilization is not done until the stabilized event has fired, but the network is
                //showing to the user and the user can work the page at this point.
                _nce.clearError();
            });

            _network.on('stabilized', function (params) {
                _stabilizationStatus = 'stabilization complete';
                _iterationsToStabilize = params.iterations + ' iterations to stabilize';
                $("#stabilizationStatus").val(_stabilizationStatus);
                $("#iterationsToStabilize").val(_iterationsToStabilize);
                _nce.clearError();
            });
        }
    }

    function networkDoubleClick(params)
    {
        if (params.nodes.length === 1) {
            if (_network.isCluster(params.nodes[0])) {
                openClusterByClusterNodeId(params.nodes[0]);
            } else {
                clusterDescendantsBySelectedNode(params.nodes[0], false);
            }
        }
    }

    function networkSingleClick(params)
    {
        var nodeId, node, cubeName, appId
        nodeId = params.nodes[0];
        node = getNodeById(_nodes, nodeId );
        if (node) {
            cubeName = node.cubeName;
            appId =_nce.getSelectedTabAppId();

            _nodeDetailsTitle[0].innerHTML = node.detailsTitle;

            _nodeVisualizer[0].innerHTML = '';
            _nodeVisualizer.append(createVisualizeFromHereLink(appId, cubeName, node));

            if (node.hasFields) {
                _nodeTraits[0].innerHTML = '';
                _nodeTraits.append(createTraitsLink(node));
            }

            _nodeCubeLink[0].innerHTML = '';
            _nodeCubeLink.append(createCubeLink(cubeName, appId));
            _nodeCubeLink.append(TWO_LINE_BREAKS);

            _nodeDetails[0].innerHTML = node.details;
            _layout.open('east');
        }
    }

     function updateNetworkOptions()
    {
        _network.setOptions(_networkOptions);
    }

    function createVisualizeFromHereLink(appId, cubeName, node)
    {
        var visualizerLink = $('<a/>');
        visualizerLink.addClass('nc-anc');
        visualizerLink.html('New visual from here');
        visualizerLink.click(function (e) {
            e.preventDefault();
            _keepCurrentScope = true;
            _scope = node.scope;
            _nce.selectCubeByName(cubeName, appId, TAB_VIEW_TYPE_VISUALIZER + PAGE_ID);
        });
        return visualizerLink;
    }

    function createCubeLink(cubeName, appId)
    {
        var cubeLink = $('<a/>');
        cubeLink.addClass('nc-anc');
        cubeLink.html('Open n-cube');
        cubeLink.click(function (e) {
            e.preventDefault();
            _nce.selectCubeByName(cubeName, appId, TAB_VIEW_TYPE_NCUBE + PAGE_ID);
        });
        return cubeLink;
    }

    function createTraitsLink(node) {
        var traitsLink = $('<a/>');
        traitsLink.addClass('nc-anc');
        if (node.loadTraits) {
            traitsLink.html('Hide traits');
        }
        else {
            traitsLink.html('Show traits');
        }
        traitsLink.click(function (e) {
            e.preventDefault();
            node.loadTraits = !node.loadTraits;
            loadTraits(node);
        });
        return traitsLink;
    }

    function replaceNode(nodes, newNode) {
        var node;
        for (var i = 0, len = nodes.length; i < len; i++) {
            node = nodes[i];
            if (node.id === newNode.id) {
                nodes.splice(i, 1);
                nodes.push(newNode)
                return;
            }
        }
    }

    function getNodeById(nodes, nodeId) {
        var node;
        for (var i = 0, len = nodes.length; i < len; i++) {
            node = nodes[i];
            if (node.id === nodeId) {
                return node;
            }
        }
    }

    function customizeNetworkForNce(network)
    {
        var edge, childNodeId, childClonedOptions, refreshData, node, childNodesObj, childEdgesObj, parentNodeId, parentClonedOptions;

        //TODO: Consider submitting pull request with these enhancements to vis
        network.clustering.clusterDescendants = function(nodeId, immediateDescendantsOnly, options) {
            var collectDescendants = function(node, parentNodeId, childEdgesObj, childNodesObj, immediateDescendantsOnly, options, parentClonedOptions, _this) {

                // collect the nodes that will be in the cluster
                for (var i = 0; i < node.edges.length; i++) {
                    edge = node.edges[i];
                    //if (edge.hiddenByCluster !== true) {  //BBH:: commented this line
                    if (edge.hiddenByCluster !== true && edge.toId != parentNodeId) { //BBH: added this line
                        childNodeId = _this._getConnectedId(edge, parentNodeId);

                        // if the child node is not in a cluster (may not be needed now with the edge.hiddenByCluster check)
                        if (_this.clusteredNodes[childNodeId] === undefined) {
                            if (childNodeId !== parentNodeId) {
                                if (options.joinCondition === undefined) {
                                    childEdgesObj[edge.id] = edge;
                                    childNodesObj[childNodeId] = _this.body.nodes[childNodeId];
                                    if (immediateDescendantsOnly === false) {
                                        collectDescendants(_this.body.nodes[childNodeId], childNodeId, childEdgesObj, childNodesObj, immediateDescendantsOnly, options, parentClonedOptions, _this); //BBH: added this line
                                    }
                                } else {
                                    // clone the options and insert some additional parameters that could be interesting.
                                    childClonedOptions = _this._cloneOptions(this.body.nodes[childNodeId]);
                                    if (options.joinCondition(parentClonedOptions, childClonedOptions) === true) {
                                        childEdgesObj[edge.id] = edge;
                                        childNodesObj[childNodeId] = _this.body.nodes[childNodeId];
                                        if (immediateDescendantsOnly === false) {
                                            collectDescendants(_this.body.nodes[childNodeId], childNodeId, childEdgesObj, childNodesObj, immediateDescendantsOnly, options, parentClonedOptions, _this); //BBH: added this line
                                        }
                                    }
                                }
                            } else {
                                // swallow the edge if it is self-referencing.
                                childEdgesObj[edge.id] = edge;
                            }
                        }
                    }
                }
            };

            refreshData = arguments.length <= 3 || arguments[3] === undefined ? true : arguments[3];

            // kill conditions
            if (nodeId === undefined) {
                throw new Error('No nodeId supplied to clusterDescendants!');
            }
            if (this.body.nodes[nodeId] === undefined) {
                throw new Error('The nodeId given to clusterDescendants does not exist!');
            }

            node = this.body.nodes[nodeId];
            options = this._checkOptions(options, node);
            if (options.clusterNodeProperties.x === undefined) {
                options.clusterNodeProperties.x = node.x;
            }
            if (options.clusterNodeProperties.y === undefined) {
                options.clusterNodeProperties.y = node.y;
            }
            if (options.clusterNodeProperties.fixed === undefined) {
                options.clusterNodeProperties.fixed = {};
                options.clusterNodeProperties.fixed.x = node.options.fixed.x;
                options.clusterNodeProperties.fixed.y = node.options.fixed.y;
            }

            childNodesObj = {};
            childEdgesObj = {};
            parentNodeId = node.id;
            parentClonedOptions = this._cloneOptions(node);
            childNodesObj[parentNodeId] = node;

            collectDescendants(node, parentNodeId, childEdgesObj, childNodesObj, immediateDescendantsOnly, options, parentClonedOptions, this);

            this._cluster(childNodesObj, childEdgesObj, options, refreshData);
        };

        network.clustering._cloneOptions = function(item, type) {
            var clonedOptions = {};
            var util = vis.util;
            if (type === undefined || type === 'node') {
                util.deepExtend(clonedOptions, item.options, true);
                clonedOptions.x = item.x;
                clonedOptions.y = item.y;
                clonedOptions.amountOfConnections = item.edges.length;
            } else {
                util.deepExtend(clonedOptions, item.options, true);
            }
            return clonedOptions;
        };

        network.clusterDescendants = function () {
            return this.clustering.clusterDescendants.apply(this.clustering, arguments);
        };
    }

    function getFromLocalStorage(key, defaultValue) {
        var local = localStorage[getStorageKey(_nce, key)];
        return local ? JSON.parse(local) : defaultValue;
    }

    //TODO: Temporarily override this function in index.js until figured out why nce.getSelectedCubeName()
    //TODO: occasionally contains a cube name with "_" instead of "." (e.g. rpm_class_product instead of
    //TODO: rpm.class.product) after a page refresh.
    function getStorageKey(nce, prefix) {
        //return prefix + ':' + nce.getSelectedTabAppId().app.toLowerCase() + ':' + nce.getSelectedCubeName().toLowerCase();
        return prefix + ':' + nce.getSelectedTabAppId().app.toLowerCase() + ':' + _selectedCubeName.toLowerCase();
    }

    function saveAllToLocalStorage() {
        saveToLocalStorage(_scope, SCOPE_MAP);
        saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
        saveToLocalStorage(_selectedLevel, SELECTED_LEVEL);
        saveToLocalStorage(_hierarchical, HIERARCHICAL);
    }

    function saveToLocalStorage(value, key) {
        saveOrDeleteValue(value, getStorageKey(_nce, key));
    }


    /*================================= BEGIN SCOPE BUILDER ==========================================================*/
    var availableScopeKeys = []
    var availableScopeValues = {}

    //TODO  1. The key in the scope picker is case sensitive, which doesn’t play well with the case insensitive scope
    //TODO     that comes across from the server (Product vs. product, etc.).
    //TODO  2. Check the availableScopeValues map when the user selects a scope key. If the map contains
    //TODO     the selected key, make the scope value field a dropdown that contains the available scope values.
    function addScopeBuilderListeners() {
        var builderOptions = {
            title: 'Scope Builder',
            instructionsTitle: 'Instructions - Scope Builder',
            instructionsText: 'Add scoping for visualization.',
            availableScopeValues: availableScopeValues,
            columns: {
                isApplied: {
                    heading: 'Apply',
                    default: true,
                    type: PropertyBuilder.COLUMN_TYPES.CHECKBOX
                },
                key: {
                    heading: 'Key',
                    type: PropertyBuilder.COLUMN_TYPES.SELECT,
                    selectOptions: availableScopeKeys
                },
                value: {
                    heading: 'Value',
                    type: PropertyBuilder.COLUMN_TYPES.TEXT
                }
            },
            afterSave: function() {
                scopeBuilderSave();
            }
        };

        $('#scopeBuilderOpen').click(function() {
            PropertyBuilder.openBuilderModal(builderOptions, _scopeBuilderScope);
        });
    }

    function scopeBuilderSave() {
         var newScope = getScopeBuilderScopeText();
        _scopeInput.val(newScope);
        _scope = buildScopeFromText(newScope)
        saveToLocalStorage(_scope, SCOPE_MAP);
     }

    function getScopeBuilderScopeText() {
        var expression,i, len;
        var scopeText = '';
        for (i = 0, len = _scopeBuilderScope.length; i < len; i++) {
            expression = _scopeBuilderScope[i];
            if (expression.isApplied) {
                scopeText += expression.key + ':' + expression.value + ', ';
            }
        }
        scopeText = scopeText.substring(0, scopeText.length - 2);
        return scopeText;
    }

    /*================================= END SCOPE BUILDER ============================================================*/

// Let parent (main frame) know that the child window has loaded.
// The loading of all of the Javascript (deeply) is continuous on the main thread.
// Therefore, the setTimeout(, 1) ensures that the main window (parent frame)
// is called after all Javascript has been loaded.
    setTimeout(function() { window.parent.frameLoaded(); }, 1);

    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load
    };

})(jQuery);

var tabActivated = function tabActivated(info)
{
    Visualizer.init(info);
    Visualizer.load();
};

var cubeSelected = function cubeSelected()
{
    Visualizer.handleCubeSelected();
};
