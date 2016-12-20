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
    var _typesToAddMap = null;
    var _selectedLevel = null;
    var _countNodesAtLevel = null;
    var _visualizerInfo = null;
    var _visualizerNetwork = null;
    var _visualizerContent = null;
    var TWO_LINE_BREAKS = '<BR><BR>';
    var _nodeDetailsTitle1 = null;
    var _nodeDetailsTitle2 = null;
    var _nodeCubeLink = null;
    var _nodeVisualizer = null;
    var _nodeTraits = null;
    var _nodeAddTypes = null;
    var _nodeDetails = null;
    var _layout = null;
    var _scopeInput = null;
    var STATUS_SUCCESS = 'success';
    var STATUS_MISSING_START_SCOPE = 'missingStartScope';
    var _scopeLastKeyTime = Date.now();
    var _scopeKeyPressed = false;
    var SCOPE_KEY_DELAY = 3000;
    var UNSPECIFIED = 'UNSPECIFIED';
    var _noteIdList = [];

    //Network layout parameters
    var _hierarchical = false;

    //Network physics
    var ZOOM = 0.02;
    var NODE_SCALING = 24;
    var NODE_SCALING_SPECIAL = 200;
    var DASH_LENGTH = 15;
    var EDGE_FONT = 20;
    var GRAVITATIONAL_CONSTANT = -300000;
    var MIN_VELOCITY = 5;
    var SPRING_CONSTANT = 0.3;
    var CENTRAL_GRAVITY = 3;
    
    var _networkOptionsButton = null;
    var _networkOptionsSection = null;
    var _stabilizeDataChange = false;
    var _stabilizeInitialNetwork = false;
    var _tuningNetwork = false;
    var _networkOptionsVis = {};
    var _networkOptionsBasic = null;
    var _networkOptionsDefaults = null;
    var _networkOptionsInput = null;
    var _networkOptionsInputHold = null;
    var _networkOptionsOverridesTuned = {
         nodes: {
            shadow: {
                enabled: true
            }
        },
        edges: {
            arrowStrikethrough: true,
            arrows: {
                to: {
                    enabled: true
                }
            },
            shadow: {
                enabled: true
            }
        }
    };

    var _networkOptionOverridesBasic = {
        interaction: {
            navigationButtons: true,
            keyboard: {
                enabled: false,
                speed: {
                    x: 5,
                    y: 5,
                    zoom: ZOOM}
            },
            zoomView: true
        },
        nodes: {
            value: NODE_SCALING,
            scaling: {
                min: NODE_SCALING,
                max: NODE_SCALING,
                label: {
                    enabled: true
                }
            },
            shadow: {
                enabled: false
            }
        },
        edges: {
            arrowStrikethrough: false,
            arrows: {
                to: {
                    enabled: false
                }
            },
            color: {
                color: 'gray'
            },
            smooth: {
                enabled: false
            },
            shadow: {
                enabled: false
            },
            hoverWidth: 3,
            selectionWidth: 2,
            font: {
                size: EDGE_FONT
            }
        },
        physics: {
            minVelocity: MIN_VELOCITY,
            barnesHut: {
                gravitationalConstant: GRAVITATIONAL_CONSTANT,
                springConstant: SPRING_CONSTANT,
                centralGravity: CENTRAL_GRAVITY
            }
        },
        layout: {
            hierarchical: {
                enabled: false
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

    var EAST_MIN_SIZE = 50;
    var EAST_MAX_SIZE = 1000;
    var EAST_SIZE = 250;
    var EAST_LENGTH_OPEN = 60;
    
    function init(info) {
        if (!_nce) {
            _nce = info;

            _layout = $('#visBody').layout({
                name: 'visLayout'
                ,	livePaneResizing:			true
                ,   east__minSize:              EAST_MIN_SIZE
                ,   east__maxSize:              EAST_MAX_SIZE
                ,   east__size:                 EAST_SIZE
                ,   east__closable:             true
                ,   east__resizeable:           true
                ,   east__initClosed:           true
                ,   east__slidable:             true
                ,   center__triggerEventsOnLoad: true
                ,   center__maskContents:       true
                ,   togglerLength_open:         EAST_LENGTH_OPEN
                ,   togglerLength_closed:       '100%'
                ,	spacing_open:			    5  // ALL panes
                ,	spacing_closed:			    5 // ALL panes
            });

            _visualizerContent = $('#visualizer-content');
            _visualizerInfo = $('#visualizer-info');
            _visualizerNetwork = $('#visualizer-network');
            _nodeDetailsTitle1 = $('#nodeDetailsTitle1');
            _nodeDetailsTitle2 = $('#nodeDetailsTitle2');
            _nodeCubeLink = $('#nodeCubeLink');
            _nodeTraits = $('#nodeTraits');
            _nodeAddTypes = $('#nodeAddTypes');
            _nodeVisualizer = $('#nodeVisualizer');
            _nodeDetails = $('#nodeDetails');
            _scopeInput = $('#scope');
            _networkOptionsSection = $('#networkOptionsSection');

            $(window).on('resize', function() {
                if (_network) {
                   _network.setSize('100%', getVisNetworkHeight());
                }
            });

             $('#selectedLevel-list').on('change', function () {
                _selectedLevel = Number($('#selectedLevel-list').val());
                saveToLocalStorage(_selectedLevel, SELECTED_LEVEL);
                reload();
            });

            $('#hierarchical').on('change', function () {
                //Hierarchical mode is disabled due to what appears to be a bug in vis.js or because the
                //visualizer.js code is missing something. Tis problem started when convering to use
                //visjs DataSets for network data.
                //The problem is that when visjs creates the network.body nodes from the network.body.data
                //nodes, it doesn't set the level on the network.body nodes. This causes an exception
                //if hierarchical mode is selected since that mode requires either all or no nodes to
                //have a defined level. Attempted short-term fix in setLevelOnNetworkNodes() method, but it's not enough.
                //TODO: Keep investigating, submit question and possibly a bug fix to visjs.
                $('#hierarchical').prop('checked', false);
                _nce.showNote('Hierarchical mode is currently not available');
                //_hierarchical = this.checked;
                //saveToLocalStorage(_hierarchical, HIERARCHICAL);
                //updateNetworkOptions();
            });

            _scopeInput.on('change', function () {
                _scopeKeyPressed = false;
                _scope = buildScopeFromText(_scopeInput.val());
                scopeChange();
            });
            
            _scopeInput.on('input', function () {
                _scopeKeyPressed = true;
                _scopeLastKeyTime = Date.now();
            });

            addSelectAllNoneGroupsListeners();
            addNetworkOptionsListeners();

            scopeKeyDelayLoop();
        }
    }

    function onNoteClick(e) {
        var target, id, scopeParts, key, value;
        target = e.target;
        if (target.className.indexOf('missingScope') > -1) {
            id = target.title;
            scopeParts = id.split(':');
            key = scopeParts[0];
            value = scopeParts[1].trim();
            _scope[key] = value;
            scopeChange();
        }
    };

    function addNetworkOptionsListeners() {
        $('#networkOptionsButton').click(function () {
            var button = $('#networkOptionsButton');
            button.toggleClass('active');
            _networkOptionsButton = button.hasClass('active');
            saveToLocalStorage(_networkOptionsButton, NETWORK_OPTIONS_DISPLAY);
            _networkOptionsSection.toggle();
            loadNetworkOptionsSectionView();
        });

        $('#networkOptionsChangeSection').change(function (e) {
            var target = e.target;
            $('.networkOption').each(function () {
                var id, keys;
                id = $(this).attr('id');
                keys = id.split('.');
                setNetworkOption($(this), _networkOptionsInput, keys);
            });
            updateNetworkOptions();
        });
    }
    
    function addSelectAllNoneGroupsListeners() {
        $('#selectAll').on('click', function (e) {
            e.preventDefault();
            $('#groups').find('button').addClass('active');
            Array.prototype.push.apply(_selectedGroups, _availableGroupsAllLevels);
            saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
            reload();
        });

        $('#selectNone').on('click', function (e) {
            e.preventDefault();
            $('#groups').find('button').removeClass('active');
            _selectedGroups = [];
            saveToLocalStorage(_selectedGroups, SELECTED_GROUPS);
            reload();
        });
    }

    function setNetworkOption(inputOption, options, keys) {
        var key, value, errorMessage;
        if (keys) {
            key = keys[0];
            value = options[key];
            if (typeof value === OBJECT)
            {
                keys.splice(key, 1);
                setNetworkOption(inputOption, value, keys)
            }
            else if (BOOLEAN === typeof value)
            {
                options[key] = inputOption.prop('checked');
            }
            else if (NUMBER === typeof value)
            {
                options[key] = Number(inputOption.val());
            }
            else if (FUNCTION === typeof value)
            {
                //Not currently supporting updating of netork options that are functions (only one).
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

    function initNetworkOptions(container) {
        var emptyDataSet, emptyNetwork, copy;
        _tuningNetwork = false;
        _stabilizeDataChange = false;
        _stabilizeInitialNetwork  = true;
        _networkOptionsSection.hide();
        _networkOptionOverridesBasic.height = getVisNetworkHeight();
        emptyDataSet = new vis.DataSet({});
        emptyNetwork = new vis.Network(container, {nodes: emptyDataSet, edges: emptyDataSet}, {});

        _networkOptionsVis.physics = emptyNetwork.physics.defaultOptions;
        _networkOptionsVis.layout = emptyNetwork.layoutEngine.defaultOptions;
        _networkOptionsVis.nodes = emptyNetwork.nodesHandler.defaultOptions;
        _networkOptionsVis.edges = emptyNetwork.edgesHandler.defaultOptions;
        _networkOptionsVis.interaction = emptyNetwork.interactionHandler.defaultOptions;
        _networkOptionsVis.manipulation = emptyNetwork.manipulation.defaultOptions;
        _networkOptionsVis.groups = emptyNetwork.groups.defaultOptions;

        //TODO: Figure out why these keys throw "unknown" exception in vis when set on the network despite originating
        //TODO: from vis. Removing keys for now.
        delete _networkOptionsVis.physics.barnesHut['theta'];
        delete _networkOptionsVis.physics.forceAtlas2Based['theta'];
        delete _networkOptionsVis.physics.repulsion['avoidOverlap'];

        copy = $.extend(true, {}, _networkOptionsVis);
        _networkOptionsBasic = $.extend(true, copy, _networkOptionOverridesBasic);
        _networkOptionsInput = $.extend(true, {}, _networkOptionsBasic);
        emptyNetwork.destroy();
    }

    function loadNetworkOptionsSectionView()
    {
        var button = $('#networkOptionsButton');
        var section =  $('#networkOptionsChangeSection');
        section.empty();
        button.prop('checked', _networkOptionsButton);
        if (_networkOptionsButton)
        {
            button.addClass('active');
            _networkOptionsSection.show();
            $("#stabilizationStatus").val(_stabilizationStatus);
            $("#iterationsToStabilize").val(_iterationsToStabilize);
            buildNetworkOptionsChangeSection( section, null, _networkOptionsInput, _networkOptionsDefaults, _networkOptionsVis);
        }
        else
        {
            button.removeClass('active');
            _networkOptionsSection.hide();
        }
    }

    function buildNetworkOptionsChangeSection(section, parentKey, networkOptions, networkOptionsDefaults, networkOptionsVis)
    {
        var rowBordersDiv, col1Div, col2Div, col3Div, col4Div, col5Div, col2Span, col2Input, col4Input, col5Input, fullKey,
            value, keyVis, valueVis, highlightedClass, readOnly, readOnlyClass, functionTitle;

        $.each(networkOptionsDefaults, function (key, defaultValue)
        {
            value = networkOptions[key];
            if (networkOptionsVis)
            {
                keyVis =  key in networkOptionsVis ? key : null;
                valueVis = networkOptionsVis[key];
            }
            else
            {
                keyVis = null;
                valueVis = null;
            }

            fullKey = null === parentKey ? key : parentKey + '.' + key;

            if (typeof defaultValue === OBJECT)
            {
                buildNetworkOptionsChangeSection(section, fullKey, value, defaultValue, valueVis);
            }
            else
            {
                rowBordersDiv = $('<div/>').prop({class: "row borders"});
                col1Div = $('<div/>').prop({class: "col-md-4", align: "right"});
                col2Div = $('<div/>').prop({class: "col-md-1"});
                col3Div = $('<div/>').prop({class: "col-md-1"});
                col4Div = $('<div/>').prop({class: "col-md-2"});
                col5Div = $('<div/>').prop({class: "col-md-4"});

                highlightedClass = value === defaultValue ? '' : ' highlighted';
                if (typeof defaultValue === BOOLEAN) {
                    col2Span = $('<span/>').prop({class: highlightedClass});
                    col2Input = $('<input/>').prop({class: 'networkOption', id: fullKey, type: "checkbox" });
                    col2Input[0].checked = value;
                    col4Input = $('<input/>').prop({class: 'readOnly', id: fullKey + 'Default', type: "checkbox", readOnly: "true"});
                    col4Input[0].checked = defaultValue;
                }
                else {
                    readOnly = FUNCTION === typeof value ? true: false;
                    readOnlyClass = readOnly ? ' readOnly' : '';
                    functionTitle = "Not currently supporting update of network options that are functions.";
                    col2Span = $('<span/>');
                    col2Input = $('<input/>').prop({class: 'networkOption' + highlightedClass + readOnlyClass, id: fullKey, type: "text", readOnly: readOnly, title: functionTitle});
                    col2Input[0].value = value;
                    col4Input = $('<input/>').prop({class: 'readOnly', id: fullKey + 'Default', type: "text", readOnly: "true"});
                    col4Input[0].value = defaultValue;
                }
                col1Div[0].innerHTML = fullKey;
                col2Span.append(col2Input);
                col2Div.append(col2Span);
                col3Div[0].innerHTML = NBSP;
                col4Div.append(col4Input);

                if (keyVis)
                {
                    if (valueVis === defaultValue)
                    {
                        col5Div[0].innerHTML = NBSP;
                    }
                    else if (BOOLEAN === typeof valueVis )
                    {
                        col5Input = $('<input/>').prop({class: 'readOnly', id: fullKey, type: "checkbox", readOnly: "true"});
                        col5Input[0].checked = valueVis;
                        col5Div.append(col5Input);
                    }
                    else
                    {
                        col5Input = $('<input/>').prop({class: 'readOnly', id: fullKey, type: "text", readOnly: "true"});
                        col5Input[0].value = valueVis;
                        col5Div.append(col5Input);
                    }
                }
                else
                {
                    col5Div[0].innerHTML = 'Parameter does not exist in Vis'
                }
                rowBordersDiv.append(col1Div);
                rowBordersDiv.append(col2Div);
                rowBordersDiv.append(col3Div);
                rowBordersDiv.append(col4Div);
                rowBordersDiv.append(col5Div);
            }
            section.append(rowBordersDiv);
        });
    }

    function scopeKeyDelayLoop() {
        var now;
        setInterval(function() {
            now = Date.now();
            if (now - _scopeLastKeyTime > SCOPE_KEY_DELAY && _scopeKeyPressed) {
                _scopeKeyPressed = false;
                _scope = buildScopeFromText(_scopeInput.val());
                scopeChange();
            }
       }, SCOPE_KEY_DELAY);
    }
    
    function scopeChange()
    {
        saveToLocalStorage(_scope, SCOPE_MAP);
        load();
    }

  
    function buildScopeFromText(scopeString) {
        var parts, part, key, value, i, iLen;
        var newScope = {};
        if (scopeString) {
            parts = scopeString.split(',');
            for ( i = 0, iLen = parts.length; i < iLen; i++) {
                part = parts[i].split(':');
                key = part[0].trim();
                value = part[1];
                if (value) {
                    newScope[key] = value.trim();
                }
            }
         }
        return newScope;
    }

    function reload() {
         setTimeout(function () {reloadNetwork();}, PROGRESS_DELAY);
        _nce.showNote('Updating network...');
    }

    function reloadNetwork() {
        updateNetworkData();
        loadSelectedLevelListView();
        loadGroupsView();
        loadCountsView();
        _visualizerInfo.show();
        _visualizerNetwork.show();
        _nce.clearNote();
     }

    function loadTraits(node) {
        _nce.clearNotes(_noteIdList);
        setTimeout(function () {loadTraitsFromServer(node);}, PROGRESS_DELAY);
        _nce.showNote(node.loadTraits ? 'Loading traits...' : 'Removing traits...');
    }

    function loadTraitsFromServer(node)
    {
         var message, options, result, json, visInfo;
         options = {
            node: node,
            scope: _scope,
            availableScopeKeys: _availableScopeKeys,
            availableScopeValues: _availableScopeValues,
            typesToAddMap: _typesToAddMap
        };
        
        result = _nce.call('ncubeController.getVisualizerTraits', [_nce.getSelectedTabAppId(), options]);
        _nce.clearNote();
        if (false === result.status) {
            _nce.showNote('Failed to load traits: ' + TWO_LINE_BREAKS + result.data);
            return node;
        }

        json = result.data;

        if (STATUS_SUCCESS === json.status) {
            if (null !== json.message) {
                _noteIdList.push(_nce.showNote(json.message));
             }
            visInfo = json.visInfo;
            node = visInfo.nodes['@items'][0];
            _scope = visInfo.scope;
            delete _scope['@type'];
            delete _scope['@id'];
            saveAllToLocalStorage();
            _availableScopeValues = visInfo.availableScopeValues;
            _availableScopeKeys = visInfo.availableScopeKeys['@items'].sort();
            _typesToAddMap = visInfo.typesToAddMap;
            replaceNode(_nodes, node);
             _nodeDetails[0].innerHTML = node.details;
            _nodeTraits = $('#nodeTraits');
            _nodeAddTypes = $('#nodeAddTypes');
            _nodeTraits[0].innerHTML = '';
            _nodeTraits.append(createTraitsLink(node));
         }
        else {
            message = json.message;
            if (null !== json.stackTrace) {
                message = message + TWO_LINE_BREAKS + json.stackTrace
            }
            _nce.showNote('Failed to load traits: ' + TWO_LINE_BREAKS + message);
        }
        return node;
    }

    function load() {
        _nce.clearNotes(_noteIdList);
        setTimeout(function () {loadFromServer();}, PROGRESS_DELAY);
        _noteIdList.push(_nce.showNote('Loading visualizer...'));
    }

    function loadFromServer() {
        var options, result, json, message;
        clearVisLayoutEast();
        destroyNetwork();

        if (!_nce.getSelectedCubeName()) {
             _visualizerContent.hide();
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + 'No cube selected.');
            return;
        }

        //TODO: The .replace is temporary until figured out why nce.getSelectedCubeName()
        //TODO: occasionally contains a cube name with "_" instead of "." (e.g. rpm_class_product instead of
        //TODO: rpm.class.product) after a page refresh.
        _selectedCubeName = _nce.getSelectedCubeName().replace(/_/g, '.');

        if (_keepCurrentScope) {
            _keepCurrentScope = false;
        }
        else{
            _scope = getFromLocalStorage(SCOPE_MAP, null);
        }

        _selectedLevel = getFromLocalStorage(SELECTED_LEVEL, null);
        _selectedGroups = getFromLocalStorage(SELECTED_GROUPS, null);
        _hierarchical = getFromLocalStorage(HIERARCHICAL, false);
        _networkOptionsButton = getFromLocalStorage(NETWORK_OPTIONS_DISPLAY, false);

        if ((_selectedCubeName !== _loadedCubeName) ||
            (_loadedAppId && !appIdMatch(_loadedAppId, _nce.getSelectedTabAppId()))) {
            _availableScopeKeys = null;
            _availableScopeValues = null;
        }

        options = {
            selectedLevel: _selectedLevel,
            startCubeName: _selectedCubeName,
            scope: _scope,
            selectedGroups: _selectedGroups,
            availableScopeKeys: _availableScopeKeys,
            availableScopeValues: _availableScopeValues,
            loadTraits: _loadTraits,
            typesToAddMap: _typesToAddMap
        };


        result = _nce.call('ncubeController.getVisualizerJson', [_nce.getSelectedTabAppId(), options]);
        _nce.clearNotes(_noteIdList);
        if (!result.status) {
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + result.data);
             _visualizerContent.hide();
            return;
        }

        json = result.data;

        if (json.status === STATUS_SUCCESS) {
            if (null !== json.message) {
                _noteIdList.push(_nce.showNote(json.message));
            }
            loadData(json.visInfo, json.status);
            initNetwork();
            loadSelectedLevelListView();
            saveAllToLocalStorage();
            loadScopeView();
            loadHierarchicalView();
            loadGroupsView();
            loadCountsView();
            _visualizerContent.show();
            _visualizerInfo.show();
            _visualizerNetwork.show();
        }
        else if (STATUS_MISSING_START_SCOPE === json.status) {
            _noteIdList.push(_nce.showNote(json.message));
            loadData(json.visInfo, json.status);
            initNetwork();
            saveAllToLocalStorage();
            loadScopeView();
            _visualizerContent.show();
            _visualizerInfo.hide();
            _visualizerNetwork.hide();
            _networkOptionsSection.hide();
        }
        else {
             _visualizerContent.hide();
            message = json.message;
            if (null !== json.stackTrace) {
                message = message + TWO_LINE_BREAKS + json.stackTrace
            }
            _nce.showNote('Failed to load visualizer: ' + TWO_LINE_BREAKS + message);
        }
    }

    function appIdMatch(appIdA, appIdB)
    {
        return appIdA.appId === appIdB.appId &&
            appIdA.version === appIdB.version &&
            appIdA.status ===  appIdB.status &&
            appIdA.branch === appIdB.branch;
    }

    function clearVisLayoutEast(){
        _nodeDetailsTitle1[0].innerHTML = '';
        _nodeDetailsTitle2[0].innerHTML = '';
        _nodeCubeLink[0].innerHTML = '';
        _nodeVisualizer[0].innerHTML = '';
        _nodeTraits[0].innerHTML = '';
        _nodeAddTypes.innerHTML = '';
        _nodeDetails[0].innerHTML = '';
        _layout.close('east');
    }

    function loadHierarchicalView() {
        $('#hierarchical').prop('checked', _hierarchical);
    }

    function loadScopeView() {
        var scope = getScopeString();
        _scopeInput.val(scope);
        _scopeInput.prop('title', scope);
    }

    function loadGroupsView() {
        var groupName, id, available, label, title, input, selected, button, groups,
            background, fontMap, color, groupMap, boxShadow, j, jLen, k, kLen, divGroups;

        divGroups = $('#groups');
        divGroups.empty();
        groups = _networkOptionsInput.groups;

        _availableGroupsAllLevels.sort();
        for (j = 0, jLen = _availableGroupsAllLevels.length; j < jLen; j++) {
            color = null;
            boxShadow = '';
            groupName = _availableGroupsAllLevels[j];
            id = groupName + _groupSuffix;
            available = groupCurrentlyAvailable(id);
            label = _allGroups[groupName];
            title = available ? "Show/hide " + label + " in the graph" : "Increase level to enable show/hide of " + label + " in the graph";

            selected = false;
            for (k = 0, kLen = _selectedGroups.length; k < kLen; k++) {
                if (groupIdsEqual(id, _selectedGroups[k])) {
                    selected = true;
                    break;
                }
            }

            groupMap = groups.hasOwnProperty(groupName) ? groups[groupName] : options.groups[UNSPECIFIED];
            background = groupMap.color;
            fontMap = groupMap.font;
            if (fontMap) {
                color = fontMap.hasOwnProperty('buttonColor') ? fontMap.buttonColor : fontMap.color;
            }
            if (!color) {
                color = '#000000';
            }

            button = $('<button/>').addClass('btn');
            if (selected) {
                button.addClass('active btn-primary-group');
                boxShadow = 'box-shadow: 0 4px #575757;';
            } else if (available) {
                button.addClass('btn-default-group');
            } else {
                button.addClass('btn-default-group');
                button.attr({disabled: 'disabled'});
            }
            button.attr({style:'background: ' +  background + '; color: ' + color + ';font-size: 12px; padding: 3px; margin-right: 3px; margin-bottom: 7px;border-radius: 15px;' + boxShadow});
            button.attr({'data-cat': label, 'data-toggle': "buttons", 'id': id, 'title': title});
            input = $('<input>').attr({type: 'checkbox', autocomplete: 'off'});
            button.append(input);
            button.append(label);
            button.click(function (e) {
                e.preventDefault();
                groupChangeEvent(this.id);
            });
            divGroups.append(button);
        }
    }

    function getScopeString(){
        var scopeLen, key, i, len;
        var scopeString = '';
        var keys = Object.keys(_scope);
        for (i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            scopeString += key + ': ' + _scope[key] + ', ';
        }
        scopeLen = scopeString.length;
        if (1 < scopeLen) {
            scopeString = scopeString.substring(0, scopeLen - 2);
        }
        return scopeString;
    }

    function groupChangeEvent(groupId)
    {
        var button, i, len, k, kLen;

        $('#groups').find('button').each(function()
        {
            button = $(this);
            if(button.attr('id') === groupId){

                if  (button.hasClass('active'))
                {
                    for (i = 0, len = _selectedGroups.length; i < len; i++) {
                        if (groupIdsEqual(groupId, _selectedGroups[i])) {
                            _selectedGroups.splice(i, 1);
                            button.removeClass('active');
                            break;
                        }
                    }
                }
                else
                {
                    for (k = 0, kLen = _availableGroupsAllLevels.length; k < kLen; k++)
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
             }
        });
    }

    function groupCurrentlyAvailable(groupId)
    {
        var l;
        for ( l = 0; l < _availableGroupsAtLevel.length; l++)
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
        var maxLevelLabel = 1 === _maxLevel ? 'level' : 'levels';
        var nodeCountLabel = 1 === totalNodeCount ? 'node' : 'nodes';

        var nodesDisplayingAtLevelCount = _network.body.data.nodes.length;
        var nodesAtLevelLabel = 1 === nodesDisplayingAtLevelCount ? 'node' : 'nodes';

        $('#levelCounts')[0].textContent = nodesDisplayingAtLevelCount  + ' ' + nodesAtLevelLabel + ' of ' + _countNodesAtLevel + ' displaying at current level';
        $('#totalCounts')[0].textContent = totalNodeCount + ' ' + nodeCountLabel + ' total over ' +  _maxLevel + ' ' + maxLevelLabel ;
    }

    function loadSelectedLevelListView()
    {
        var option, j;
        var select = $('#selectedLevel-list');
        select.empty();

        for (j = 1; j <= _maxLevel; j++)
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
        var j, jLen;
        if (_selectedGroups) {
            for (j = 0, jLen = _selectedGroups.length; j < jLen; j++) {
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
     }


    function updateNetworkDataNodes()
    {
        var node, selectedGroup, groupNamePrefix, i, iLen;
        var networkDataNodes =  _network.body.data.nodes;
        var nodeIdsToRemove = [];
        var nodesToAddBack = [];
        var level = _selectedLevel;
        var selectedGroups = [];
        var availableGroupsAtLevel = [];
        _countNodesAtLevel = 0;

        //given the selected level, determine nodes to exclude, nodes to add back, selected groups and available groups
        for (i = 0, iLen = _nodes.length; i < iLen; i++)
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
        var edge, k, kLen;
        var networkDataEdges =  _network.body.data.edges;
        var edgeIdsToRemove = [];
        var edgesToAddBack = [];

        //given the selected level, determine edges to exclude and edges to add back
        if (_edges) {
            for (k = 0, kLen = _edges.length; k < kLen; k++) {
                edge = _edges[k];

                if (parseInt(edge.level) > _selectedLevel)
                {
                    edgeIdsToRemove.push(edge.id);
                }
                else if (!networkDataEdges || !networkDataEdges.get(edge.id))
                {
                    edgesToAddBack.push(edge);
                }
            }
        }
        networkDataEdges.remove(edgeIdsToRemove);
        networkDataEdges.add(edgesToAddBack);
    }

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
            edges = visInfo.edges['@items'];
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
        _typesToAddMap  = visInfo.typesToAddMap;
     }

    function handleCubeSelected() {
        load();
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
        var node = _nodes[0];
        if (node) {
            if ('1' !== node.id) {
                throw new Error('Expected node id of 1 for first node in list.')
            }
            node.shapeProperties = {borderDashes: [DASH_LENGTH, 5]};
            node.borderWidth = 3;
            node.scaling = {min: NODE_SCALING_SPECIAL, max: NODE_SCALING_SPECIAL, label: {enabled: true}};
        }
    }

    function initNetwork()
    {
        var container, nodeDataSet, edgeDataSet;
        if (_network)
        {
            updateNetworkData();
        }
        else
        {
            container = document.getElementById('network');
            initNetworkOptions(container);
            nodeDataSet = new vis.DataSet({});
            markTopNodeSpecial();
            nodeDataSet.add(_nodes);
            edgeDataSet = new vis.DataSet({});
            edgeDataSet.add(_edges);
            _network = new vis.Network(container, {nodes:nodeDataSet, edges:edgeDataSet}, _networkOptionsInput);
            updateNetworkData();

            _network.on('select', function(params) {
                networkSelectEvent(params);
            });

            _network.on('startStabilizing', function () {
                networkStartStabilizingEvent();
             });

             _network.on('stabilizationIterationsDone', function () {
                 networkStabilizationIterationsDoneEvent();
            });

            _network.on('stabilized', function (params) {
                networkStabilizedEvent(params);
            });

            nodeDataSet.on('add', function () {
                dataSetChangeEvent()
            });

            nodeDataSet.on('remove', function () {
                dataSetChangeEvent()
            });

            edgeDataSet.on('add', function () {
                dataSetChangeEvent()
            });

            edgeDataSet.on('remove', function () {
                dataSetChangeEvent()
            });
        }
    }

    function networkStartStabilizingEvent(){
        if (_tuningNetwork) {
            _nce.clearNote();
            _noteIdList.push(_nce.showNote('Tuning network...'));
            _stabilizationStatus = 'tuning started';
            _tuningNetwork = false;
        }
        else{
            _noteIdList.push(_nce.showNote('Stabilizing network...'));
            _stabilizationStatus = 'stabilization started';
            _iterationsToStabilize = 'iterating...';
        }
        $("#stabilizationStatus").val(_stabilizationStatus);
        $("#iterationsToStabilize").val(_iterationsToStabilize);
    }

    function networkStabilizationIterationsDoneEvent(){
        _stabilizationStatus = 'hidden stabilization complete';
        $("#stabilizationStatus").val(_stabilizationStatus);
        //Clear note 'Stabilizing network...' here when hidden stabilization is complete.
        //The full stabilization is not done until the stabilized event has fired, but the network is
        //showing to the user and the user can work the page at this point.
        _nce.clearNote();
    }

    function networkStabilizedEvent(params){
        var copy, iterations;
        if (_stabilizeInitialNetwork || _stabilizeDataChange) {
            if (_stabilizeInitialNetwork) {
                copy = $.extend(true, {}, _networkOptionsBasic);
                _networkOptionsDefaults = $.extend(true, copy, _networkOptionsOverridesTuned);
                _networkOptionsInput = $.extend(true, {}, _networkOptionsDefaults);
                _stabilizeInitialNetwork = false;
            }
            else if (_stabilizeDataChange) {
                _networkOptionsInput = $.extend(true, {}, _networkOptionsInputHold);
                _networkOptionsInputHold = {};
                _stabilizeDataChange = false;
            }
            _tuningNetwork = true;
            _iterationsToStabilize = params.iterations;
            $("#iterationsToStabilize").val(_iterationsToStabilize);
            _stabilizationStatus = 'basic stabilization complete';
            updateNetworkOptions();
        }
        else{
            _stabilizationStatus = 'stabilization complete';
            if (typeof _iterationsToStabilize === NUMBER){
                iterations = Number(_iterationsToStabilize) + Number(params.iterations);
            }
            else{
                iterations = Number(params.iterations);
            }
            _iterationsToStabilize = iterations + ' iterations to stabilize';
            _nce.clearNote();
        }

        $("#stabilizationStatus").val(_stabilizationStatus);
        $("#iterationsToStabilize").val(_iterationsToStabilize);
    }

    function dataSetChangeEvent(){
        _tuningNetwork = false;
        if (!_stabilizeInitialNetwork && !_stabilizeDataChange) {
            _stabilizeDataChange = true;
            _networkOptionsInputHold = $.extend(true, {}, _networkOptionsInput);
            _networkOptionsInput = $.extend(true, {}, _networkOptionsBasic);
            updateNetworkOptions();
        }
    }

    function networkSelectEvent(params)
    {
        var nodeId, node, cubeName, appId, typesToAdd, title2;
        nodeId = params.nodes[0];
        node = getNodeById(_nodes, nodeId );
        if (node) {
            cubeName = node.cubeName;
            appId =_nce.getSelectedTabAppId();

            _nodeDetailsTitle1[0].innerHTML = node.detailsTitle1;
            title2 = node.detailsTitle2;
            if (typeof title2 === STRING){
                _nodeDetailsTitle2[0].innerHTML = title2;
                _nodeDetailsTitle1.addClass('title1');
            }
            else{
                _nodeDetailsTitle1.removeClass('title1');
                _nodeDetailsTitle2[0].innerHTML = '';
            }
            
            _nodeVisualizer[0].innerHTML = '';
            _nodeVisualizer.append(createVisualizeFromHereLink(appId, cubeName, node));

            _nodeCubeLink[0].innerHTML = '';
            _nodeCubeLink.append(createCubeLink(cubeName, appId));

            if (node.hasFields) {
                _nodeTraits[0].innerHTML = '';
                _nodeTraits.append(createTraitsLink(node));
            }

            _nodeAddTypes[0].innerHTML = '';
            if (typeof node.typesToAdd === OBJECT) {
                typesToAdd = node.typesToAdd['@items'];
                if (typeof typesToAdd === OBJECT && typesToAdd.length > 0) {
                    createAddTypesDropdown(typesToAdd, node.label);
                }
            }

            _nodeDetails[0].innerHTML = node.details;
            addNodeDetailsListeners();
            _layout.open('east');
        }
    }

    function addNodeDetailsListeners()
    {
        if (!_nodeDetails.hasClass(HAS_CLICK_EVENT))
        {
            _nodeDetails.click(function (e) {
                e.preventDefault();
                onNoteClick(e);
            });
            _nodeDetails.addClass(HAS_CLICK_EVENT)
        }
    }

    function updateNetworkOptions()
    {
        loadNetworkOptionsSectionView();
       _network.setOptions(_networkOptionsInput);
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

    function createAddTypesDropdown(typesToAdd, label) {
        var li, addLink, a, ul, i, len;

        addLink = $('<a/>');
        addLink.prop({class: 'nc-anc', id:"addLink" });
        addLink.attr("data-toggle", "dropdown");
        addLink.html('Add to ' + label);

        ul = $('<ul/>');
        ul.addClass('dropdown-menu');

        for (i = 0, len = typesToAdd.length; i < len; i++) {
            li = $('<li/>');
            a = $('<a/>');
            a.prop({href: '#' });
            a.html(typesToAdd[i]);
            a.click(function (e) {
                e.preventDefault();
                _nce.showNote('Add ' + this.innerHTML + ' is not yet implemented.');
            });
            li.append(a);
            ul.append(li);
        }

        _nodeAddTypes.append(addLink);
        _nodeAddTypes.append(ul);
    }

    function replaceNode(nodes, newNode) {
        var node, i, len;
        for (i = 0, len = nodes.length; i < len; i++) {
            node = nodes[i];
            if (node.id === newNode.id) {
                nodes.splice(i, 1);
                nodes.push(newNode);
                return;
            }
        }
    }

    function getNodeById(nodes, nodeId) {
        var node, i, len;
        for (i = 0, len = nodes.length; i < len; i++) {
            node = nodes[i];
            if (node.id === nodeId) {
                return node;
            }
        }
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
        saveToLocalStorage(_networkOptionsButton, NETWORK_OPTIONS_DISPLAY);
    }

    function saveToLocalStorage(value, key) {
        saveOrDeleteValue(value, getStorageKey(_nce, key));
    }
    
// Let parent (main frame) know that the child window has loaded.
// The loading of all of the Javascript (deeply) is continuous on the main thread.
// Therefore, the setTimeout(, 1) ensures that the main window (parent frame)
// is called after all Javascript has been loaded.
    setTimeout(function() { window.parent.frameLoaded(); }, 1);

    return {
        init: init,
        handleCubeSelected: handleCubeSelected,
        load: load,
        onNoteClick: onNoteClick
    };

})(jQuery);

function tabActivated(info) {
    Visualizer.init(info);
    Visualizer.load();
}

function cubeSelected() {
    Visualizer.handleCubeSelected();
}

function onNoteClick(e, element) {
    Visualizer.onNoteClick(e, element);
}
