/**
 * NCube Editor
 *     IDE for building and editing NCubes
 *     
 * @author John DeRegnaucourt
 */

$(function() 
{
	var _cubeList = new Object();
    var _apps = [];
    var _statuses = ['RELEASE', 'SNAPSHOT'];
    var _versions = [];
	var _selectedCubeName = null;
    var _selectedApp = localStorage[SELECTED_APP];
    var _selectedVersion = null;
    var _selectedStatus = "SNAPSHOT";
    var _axisName;
    var _errorId = null;
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

        setListSelectedStatus(_selectedApp, '#app-list');
        setListSelectedStatus(_selectedStatus, '#status-list');
        setListSelectedStatus(_selectedVersion, '#version-list');
        setListSelectedStatus(_selectedCubeName, '#ncube-list');

        loadCube();
		
		var west = $('#west');
		var tabs = $('#tabs');
		var tabDiv = $('#ncubeTabContent');
		
		$('body').layout(
		{
			center__paneSelector: "#center",
			west__paneSelector: "#west",
			center__onresize: function()
			{
		    	tabDiv.height(west.height() - tabs.height());
			}
		});		
		tabDiv.height(west.height() - tabs.height());
        addListeners();
	}

    function addListeners()
    {
        $('#aboutMenu').click(function() { about() });

        $('#newCubeMenu').click(function() { newCube() });
        $('#newCubeSave').click(function() { newCubeSave() });

        $('#dupeCubeMenu').click(function() { dupeCube() });
        $('#dupeCubeCopy').click(function() { dupeCubeCopy() });

        $('#deleteCubeMenu').click(function() { deleteCube() });
        $('#deleteCubeOk').click(function() { deleteCubeOk() });

        $('#showRefsToMenu').click(function() { showRefsToCube() });
        $('#showRefsToClose').click(function() { showRefsToCubeClose() });

        $('#showRefsFromMenu').click(function() { showRefsFromCube() });
        $('#showRefsFromClose').click(function() { showRefsFromCubeClose() });

        $('#showReqScopeMenu').click(function() { showReqScope() });
        $('#showReqScopeClose').click(function() { showReqScopeClose() });

        $('#releaseCubesMenu').click(function() { releaseCubes() });
        $('#releaseCubesOk').click(function() { releaseCubesOk() });

        $('#changeVerMenu').click(function() { changeVersion() });
        $('#changeVerOk').click(function() { changeVersionOk() });

        $('#addAxisMenu').click(function() { addAxis() });
        $('#addAxisOk').click(function() { addAxisOk() });

        $('#deleteAxisMenu').click(function() { deleteAxis() });
        $('#deleteAxisOk').click(function() { deleteAxisOk() });

        $('#updateAxisMenu').click(function() { updateAxis() });
        $('#updateAxisOk').click(function() { updateAxisOk() });
    }

	function loadNCubeListView()
	{
        $('#ncubeCount').html(Object.keys(_cubeList).length);
        var list = $('#ncube-list');
		list.empty();
		
		$.each(_cubeList, function(key, value)
		{
			var li = $("<li/>");
			var anchor = $('<a href="#"/>');
			anchor.click(function()
			{
				var cubeName = anchor.text();
				setListSelectedStatus(cubeName, '#ncube-list');
                _selectedCubeName = cubeName;
				loadCube(); // load spreadsheet side
			});
			anchor.html(key);
			if (value.name == _selectedCubeName)
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
	}

    function loadAppListView()
    {
        $('#appCount').html(_apps.length);
        var list = $('#app-list');
        list.empty();

        $.each(_apps, function(index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function()
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
    }

    function loadStatusListView()
    {
        var list = $('#status-list');
        list.empty();

        $.each(_statuses, function(index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function()
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
    }

    function loadVersionListView()
    {
        $('#verCount').html(_versions.length);
        var list = $('#version-list');
        list.empty();

        $.each(_versions, function(index, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.click(function()
            {
                var version = anchor.text();
                setListSelectedStatus(version, '#version-list');
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
    }

	function loadCubeHtml()
	{
        if (!_selectedCubeName || !_selectedApp || !_selectedVersion || !_selectedStatus)
        {
            $("#ncube-content").html('No n-cubes to load');
            return;
        }
		var result = call("ncubeController.getHtml",[_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
		var htmlContent;

		if (result.status === true)
		{
			htmlContent = result.data;
		}
		else
		{
			htmlContent = "Unable to load " + _selectedCubeName + '. ' + result.data;
		}
		$("#ncube-content").html(htmlContent);
        $('.btn-sm').click(function()
        {
            updateAxis($(this).html());
        });
	}

	function loadCubeDetails()
	{
		var cube = _cubeList[_selectedCubeName];
        if (!cube)
        {
            return;
        }
		$('#cube_id').val(cube.id);
		$('#cube_app').val(cube.app);
		$('#cube_name').val(cube.name);
		$('#cube_version').val(cube.version);
		$('#cube_status').val(cube.status);
		
		var date = '';
		if (cube.createDate != undefined)
		{
			date = new Date(cube.createDate.value).format('yyyy-mm-dd HH:MM:ss');
		}
		$('#cube_createDate').val(date);
		
		date = '';
		if (cube.updateDate != undefined)
		{
			date = new Date(cube.updateDate.value).format('yyyy-mm-dd HH:MM:ss');			
		}
		$('#cube_updateDate').val(date);	
		
		$('#cube_createHid').val(cube.createHid);		
		$('#cube_updateHid').val(cube.updateHid);
		
		var date = '';
		if (cube.sysEffDate != undefined)
		{
			date = new Date(cube.sysEffDate.value).format('yyyy-mm-dd HH:MM:ss');
		}
		$('#cube_sysEffDate').val(date);
		
		date = '';
		if (cube.sysEndDate != undefined)
		{
			date = new Date(cube.sysEndDate.value).format('yyyy-mm-dd HH:MM:ss');			
		}
		$('#cube_sysEndDate').val(date);	
		
		var date = '';
		if (cube.bizEffDate != undefined)
		{
			date = new Date(cube.bizEffDate.value).format('yyyy-mm-dd HH:MM:ss');
		}
		$('#cube_bizEffDate').val(date);
		
		date = '';
		if (cube.bizEndDate != undefined)
		{
			date = new Date(cube.bizEndDate.value).format('yyyy-mm-dd HH:MM:ss');			
		}
		$('#cube_bizEndDate').val(date);	
	}
	
	function loadCube()
	{
		loadCubeHtml();
		loadCubeDetails();
        // TODO: Load Tests
        // TODO: Load D3.js pictures
	}
	
	/**
	 * Tweak the class name of the selected / non-selected items
	 * to match what was selected.
	 */
	function setListSelectedStatus(itemName, listId)
	{
		var list = $(listId);
        var items = list.find('li a');
		$.each(items, function(index, value)
		{
			var anchor = $(value);
			var text = anchor.html();
			if (text == itemName)
			{
				anchor.attr('class', 'ncube-selected');
			}
			else
			{
				anchor.attr('class', 'ncube-notselected');
			}
		});
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
            _selectedCubeName = null;
            _errorId = showNote('No App selected, cannot load n-cubes.');
            return;
        }
        if (!_selectedVersion)
        {
            _selectedCubeName = null;
            _errorId = showNote('No Version selected, cannot load n-cubes.');
            return;
        }
        if (!_selectedStatus)
        {
            _selectedCubeName = null;
            _errorId = showNote('No Status selected, cannot load n-cubes.');
            return;
        }

		var result = call("ncubeController.getCubeList", ['%', _selectedApp, _selectedVersion, _selectedStatus]);
		var first = null;

		if (result.status === true)
		{
			$.each(result.data, function(index, value)
			{
				_cubeList[value.name] = value;
                if (!first)
                {
                    first = value.name;
                }
			});
		}
        else
        {
            _errorId = showNote('Unable to load n-cubes. ' + result.data);
        }

        _selectedCubeName = (_cubeList && first) ? _cubeList[first].name : null;
	}

    function loadVersions()
    {
        _versions = [];
        clearError();

        if (!_selectedApp)
        {
            _selectedVersion = null;
            _errorId = showNote('Unable to load versions, no n-cube App selected.');
            return;
        }
        if (!_selectedStatus)
        {
            _selectedVersion = null;
            _errorId = showNote('Unable to load versions, no n-cube Status selected.');
            return;
        }
        var result = call("ncubeController.getAppVersions", [_selectedApp, _selectedStatus]);

        if (result.status === true)
        {
            $.each(result.data, function(index, value)
            {
                _versions[index] = value;
            });
        }
        else
        {
            _errorIdLoadVersions = showNote('Unable to load versions. ' + result.data);
        }

        _selectedVersion = (_versions) ? _versions[_versions.length - 1] : null;
    }

    function loadAppNames()
    {
        _apps = [];
        if (_errorId)
        {
            $.gritter.remove(_errorId);
            _errorId = null;
        }
        var result = call("ncubeController.getAppNames", []);
        if (result.status === true)
        {
            $.each(result.data, function(index, value)
            {
                _apps[index] = value;
            });
        }
        else
        {
            _errorId = showNote('Unable to load n-cube Apps. ' + result.data);
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
        buildDropDown('#newCubeAppList', '#newCubeAppName', _apps, function(app)
        {
            var result = call("ncubeController.getAppVersions", [app, 'SNAPSHOT']);
            if (result.status === true)
            {
                buildDropDown('#existVersionList', '#newCubeVersion', result.data, function() { });
            }
        });
        buildDropDown('#existVersionList', '#newCubeVersion', _versions, function() { });
        $('#newCubeModal').modal();
    }

    function newCubeSave()
    {
        $('#newCubeModal').modal('hide');
        var appName = $('#newCubeAppName').val();
        var cubeName = $('#newCubeName').val();
        var version = $('#newCubeVersion').val();
        var result = call("ncubeController.createCube",[cubeName, appName, version]);

        if (result.status === true && result.data === true)
        {
            loadAppNames();
            _selectedApp = appName;
            loadAppListView();
            setListSelectedStatus(_selectedApp, '#app-list')

            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();
            setListSelectedStatus(_selectedVersion, '#version-list');

            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to create n-cube: ' + cubeName + '. ' + result.data);
        }
    }

    function deleteCube()
    {
        clearError();

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Nothing to delete.');
            return;
        }

        $('#deleteCubeLabel').html('Delete \'' + _selectedCubeName + '\' (' + _selectedVersion + ', ' + _selectedStatus + ') ?');
        $('#deleteCubeModal').modal();
    }

    function deleteCubeOk()
    {
        $('#deleteCubeModal').modal('hide');
        var result = call("ncubeController.deleteCube",[_selectedCubeName, _selectedApp, _selectedVersion]);
        if (result.status === true && result.data === true)
        {
            loadAppNames();
            loadAppListView();
            setListSelectedStatus(_selectedApp, '#app-list')

            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();
            setListSelectedStatus(_selectedVersion, '#version-list');

            loadNCubes();
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to delete n-cube: ' + _selectedCubeName + '. ' + result.data);
        }
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
        buildDropDown('#dupeCubeAppList', '#dupeCubeAppName', _apps, function(app)
        {
            var result = call("ncubeController.getAppVersions", [app, 'SNAPSHOT']);
            if (result.status === true)
            {
                buildDropDown('#dupeCubeVersionList', '#dupeCubeVersion', result.data, function() { });
            }
        });
        buildDropDown('#dupeCubeVersionList', '#dupeCubeVersion', _versions, function() { });

        $('#dupeCubeModal').modal();
    }

    function dupeCubeCopy()
    {
        $('#dupeCubeModal').modal('hide');
        var newName = $('#dupeCubeName').val();
        var newApp = $('#dupeCubeAppName').val();
        var newVersion = $('#dupeCubeVersion').val();
        var result = call("ncubeController.duplicateCube",[newName, _selectedCubeName, newApp, _selectedApp, newVersion, _selectedVersion, _selectedStatus]);

        if (result.status === true && result.data === true)
        {
            loadAppNames();
            _selectedApp = newApp;
            loadAppListView();

            _selectedStatus = 'SNAPSHOT';
            setListSelectedStatus('SNAPSHOT', '#status-list')
            loadVersions();
            _selectedVersion = newVersion;
            loadVersionListView();
            setListSelectedStatus(_selectedVersion, '#version-list');

            loadNCubes();
            _selectedCubeName = newName;
            loadNCubeListView();
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to duplicate n-cube: ' + _selectedCubeName + '. ' + result.data);
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

        var result = call("ncubeController.getReferencesTo",[_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true && typeof result.data !== "string")
        {
            $.each(result.data, function(index, value)
            {
                var li = $("<li/>").attr({'class':'list-group-item'});
                var anchor = $('<a href="#"/>');
                anchor.html(value);
                anchor.click(function()
                {
                    showRefsToCubeClose();
                    _selectedCubeName = value;
                    setListSelectedStatus(_selectedCubeName, '#ncube-list');
                    loadCube();
                });
                li.append(anchor);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching inbound references to ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '). ' + result.data);
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

        var result = call("ncubeController.getReferencesFrom",[_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true && typeof result.data !== "string")
        {
            $.each(result.data, function(index, value)
            {
                var li = $("<li/>").attr({'class':'list-group-item'});
                var anchor = $('<a href="#"/>');
                anchor.html(value);
                anchor.click(function()
                {
                    showRefsFromCubeClose();
                    _selectedCubeName = value;
                    setListSelectedStatus(_selectedCubeName, '#ncube-list');
                    loadCube();
                });
                li.append(anchor);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching outbound references for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '). ' + result.data);
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

        var result = call("ncubeController.getRequiredScope",[_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        if (result.status === true && typeof result.data !== "string")
        {
            $.each(result.data, function(index, value)
            {
                var li = $("<li/>").attr({'class':'list-group-item'});
                li.html(value);
                ul.append(li);
            });
        }
        else
        {
            _errorId = showNote('Error fetching required scope for ' + _selectedCubeName + ' (' + _selectedVersion + ', ' + _selectedStatus + '). ' + result.data);
        }
    }

    function showReqScopeClose()
    {
        $('#showReqScopeModal').modal('hide');
    }

    function releaseCubes()
    {
        clearError();

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No version to release.');
            return;
        }

        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote('Only a SNAPSHOT version can be released.');
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
        var result = call("ncubeController.releaseCubes",[_selectedApp, _selectedVersion, newSnapVer]);

        if (result.status === true && result.data === true)
        {
            var saveSelectedVersion = _selectedVersion;
            loadVersions();
            _selectedVersion = doesItemExist(saveSelectedVersion, _versions) ? saveSelectedVersion : _selectedVersion;
            loadVersionListView();

            loadNCubes();
            loadNCubeListView();
            setListSelectedStatus(_selectedCubeName, '#ncube-list');
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to release version: ' + _selectedVersion + '. ' + result.data);
        }
    }

    function changeVersion()
    {
        clearError();

        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. No SNAPSHOT version can be changed.');
            return;
        }

        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote('Only a SNAPSHOT version can be changed.');
            return;
        }

        $('#changeVerLabel').html('Change ' + _selectedApp + ' ' + _selectedVersion + ' ?');
        $('#changeVerModal').modal();
    }

    function changeVersionOk()
    {
        $('#changeVerModal').modal('hide');
        var newSnapVer = $('#changeVerValue').val();
        var result = call("ncubeController.changeVersionValue",[_selectedApp, _selectedVersion, newSnapVer]);

        if (result.status === true && result.data === true)
        {
            loadVersions();
            _selectedVersion = doesItemExist(newSnapVer, _versions) ? newSnapVer : _selectedVersion;
            loadVersionListView();

            loadNCubes();
            loadNCubeListView();
            setListSelectedStatus(_selectedCubeName, '#ncube-list');
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to change SNAPSHOT version to value: ' + newSnapVer + '. ' + result.data);
        }
    }

    function addAxis()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Axis cannot be added.');
            return;
        }

        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote('Only a SNAPSHOT version can be modified.');
            return;
        }

        var generalTypes= ['STRING','LONG', 'BIG_DECIMAL', 'DOUBLE', 'DATE', 'COMPARABLE'];
        var ruleTypes = ['EXPRESSION'];
        buildDropDown('#addAxisTypeList', '#addAxisTypeName', ['DISCRETE','RANGE', 'SET', 'NEAREST', 'RULE'], function(selected)
        {
            if ("RULE" == selected)
            {
                buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', ruleTypes, function() { });
                $('#addAxisValueTypeName').val('EXPRESSION');
            }
            else
            {
                buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', generalTypes, function() { });
                $('#addAxisValueTypeName').val('STRING');
            }
        });
        buildDropDown('#addAxisValueTypeList', '#addAxisValueTypeName', generalTypes, function() { });
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
        if (result.status === true && result.data === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to add axis: ' + axisName + '. ' + result.data);
        }
    }

    function deleteAxis()
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Axis cannot be deleted.');
            return;
        }

        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote('Only a SNAPSHOT version can be modified.');
            return;
        }

        var result = call("ncubeController.getAxes", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus]);
        var axes = [];
        if (result.status === true && typeof result.data !== "string")
        {
            axes = result.data;
            $.each(result.data, function(index, value)
            {
                axes[index] = value.name;
            });
        }
        else
        {
            _errorId = showNote('Could not retrieve axes for ncube: ' + _selectedCubeName);
            return;
        }
        buildDropDown('#deleteAxisList', '#deleteAxisName', axes, function() { });

        $('#deleteAxisName').val('');
        $('#deleteAxisModal').modal();
    }

    function deleteAxisOk()
    {
        $('#deleteAxisModal').modal('hide');
        var axisName = $('#deleteAxisName').val();
        var result = call("ncubeController.deleteAxis", [_selectedCubeName, _selectedApp, _selectedVersion, axisName]);
        if (result.status === true && result.data === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to delete axis: ' + axisName + '. ' + result.data);
        }
    }


    function updateAxis(axisName)
    {
        clearError();
        if (!_selectedApp || !_selectedVersion || !_selectedCubeName || !_selectedStatus)
        {
            _errorId = showNote('No n-cube selected. Axis cannot be updated.');
            return;
        }

        if (_selectedStatus == "RELEASE")
        {
            _errorId = showNote('Only a SNAPSHOT version can be modified.');
            return;
        }

        var result = call("ncubeController.getAxis", [_selectedCubeName, _selectedApp, _selectedVersion, _selectedStatus, axisName]);
        var axis;
        if (result.status === true && typeof result.data !== "string")
        {
            axis = result.data;
        }
        else
        {
            _errorId = showNote('Could not retrieve axes for ncube: ' + _selectedCubeName);
            return;
        }

        var forceState = axis.type.name == 'RULE';

        $('#updateAxisLabel').html("Update Axis: " + axisName);
        $('#updateAxisName').val(axisName);
        $('#updateAxisTypeName').val(axis.type.name);
        $('#updateAxisValueTypeName').val(axis.valueType.name);
        $('#updateAxisDefaultCol').prop({'checked':axis.defaultCol != null});
        if (forceState)
        {
            $('#updateAxisSortOrderRow').hide();
            $('#updateAxisMultiMatchRow').hide();
        }
        else
        {
            $('#updateAxisSortOrderRow').show();
            $('#updateAxisMultiMatchRow').show();
            $('#updateAxisSortOrder').prop({'checked':axis.preferredOrder == 0,'disabled':false});
            $('#updateAxisMultiMatch').prop({'checked':axis.multiMatch === true, 'disabled':false});
        }
        _axisName = axisName;
        $('#updateAxisModal').modal();
    }

    function updateAxisOk()
    {
        $('#updateAxisModal').modal('hide');
        var axisName = $('#updateAxisName').val();
        var hasDefault = $('#updateAxisDefaultCol').prop('checked');
        var sortOrder = $('#updateAxisSortOrder').prop('checked');
        var multiMatch = $('#updateAxisMultiMatch').prop('checked');
        var result = call("ncubeController.updateAxis", [_selectedCubeName, _selectedApp, _selectedVersion, _axisName, axisName, hasDefault, sortOrder, multiMatch]);
        if (result.status === true && result.data === true)
        {
            loadCube();
        }
        else
        {
            _errorId = showNote('Unable to update axis: ' + axisName + '. ' + result.data);
        }
    }

    // --------------------------------------------------------------------------------------------

    function buildDropDown(listId, inputId, list, callback)
    {
        var ul = $(listId);
        ul.empty();
        $.each(list, function(key, value)
        {
            var li = $("<li/>");
            var anchor = $('<a href="#"/>');
            anchor.html(value);
            anchor.click(function()
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
        $.each(list, function(index, value)
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

    function about()
    {
        clearError();
        $('#aboutModal').modal();
    }
});
