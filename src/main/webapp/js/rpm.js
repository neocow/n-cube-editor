/**
 * RPM Editor
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

var RpmEditor = (function($)
{
    var hot = null;
    var nce = null;
    var traitMetaInfo = null;
    var traitTable = null;
    var fieldNames = null;

    var init = function(info)
    {
        if (!nce)
        {
            nce = info.fn;

            $(window).resize(function ()
            {
                RpmEditor.render();
            });

            loadTraitMetaInfo();
            buildTraitButtons();
        }
    };

    var loadTraitMetaInfo = function()
    {
        var result = nce.exec('RpmController.getTraitCategoriesDeep', [nce.getAppId(), {}]);
        if (result.status === false || !result.data.return)
        {
            showRpmError('Unable to fetch trait categories for all traits.  The rpm.meta.traits cubes are not loading:<hr class="hr-small"/>' + result.data);
            return;
        }

        traitMetaInfo = result.data.return;
    };

    var buildTraitButtons = function()
    {
        var div = $('#traitFilter');
        div.empty();
        var result = nce.exec('RpmController.getTraitCategories', [nce.getAppId(), {}]);
        if (result.status === false)
        {
            showRpmError('Unable to fetch rpm.meta.traits n-cube.');
            return;
        }
        var traitList = result.data.return['@items'];
        var i = 0;
        $.each(traitList, function(index, item)
        {
            var label = $('<label/>');
            label.attr({class:'btn'});
            if (i == 0)
            {
                label.addClass('active');
            }
            label.attr({style:'border-color:#999;color:' + item.metaProps['css:color'] + ';background-color:' + item.metaProps['css:background-color'] + ' !important'});
            label.attr({'data-cat': item.value, 'data-toggle': "buttons"});
            var input = $('<input>').attr({type:'checkbox', autocomplete:'off'});
            i++;
            label.append(input);
            var btnText = item.metaProps['shortDesc'] || item.value;
            label.append(' ' + btnText);
            label.click(function(e)
            {
                e.preventDefault();
                label.toggleClass('active');
                load();
            });
            div.append(label);
        });

        $('#selectAll').click(function(e)
        {
            e.preventDefault();
            $('#traitFilter').find('label').each(function()
            {
                $(this).addClass('active');
            });
            load();
        });

        $('#selectNone').click(function(e)
        {
            e.preventDefault();
            $('#traitFilter').find('label').each(function()
            {
                $(this).removeClass('active');
            });
            load();
        });
    };

    var showRpmError = function(msg)
    {
        $('#rpmErrorPanel').removeAttr('hidden');
        $('#viewRpmInfo').attr('hidden', true);
        $('#rpmErrorMsg').html(msg);
    };

    var clearRpmError = function()
    {
        $('#rpmErrorPanel').attr('hidden', true);
        $('#viewRpmInfo').removeAttr('hidden');
    };

    var buildTraitTable = function()
    {
        traitTable = {names:[], back:[], fore:[]};
        var i = 0;
        traitTable.names[i] = 'Traits >';
        traitTable.back[i] = '#777';
        traitTable.fore[i] = '#fff';
        i++;
        $('#traitFilter').find('label').each(function(index, element)
        {
            var elem = $(element);

            if (elem.hasClass('active'))
            {
                var category = elem.attr('data-cat');
                var catInfo = traitMetaInfo[category];

                $.each(catInfo.traits['@items'], function(key1, val1)
                {
                    traitTable.names[i] = val1.value;
                    traitTable.fore[i] = '#111';
                    traitTable.back[i] = '#fff';

                    if (catInfo.info.metaProps)
                    {
                        var fore = catInfo.info.metaProps['css:color'];
                        var back = catInfo.info.metaProps['css:background-color'];
                        if (fore)
                        {
                            traitTable.fore[i] = fore;
                        }
                        if (back)
                        {
                            traitTable.back[i] = back;
                        }
                    }
                    i++;
                });
            }
        });
    };

    var load = function()
    {
        if (hot)
        {
            hot.destroy();
            hot = null;
        }

        if (!nce.getCubeMap() || !nce.doesCubeExist())
        {
            showRpmError('No cubes available.');
            return;
        }

        var info = nce.getCubeMap()[(nce.getSelectedCubeName() + '').toLowerCase()];
        if (!info)
        {
            showRpmError('No cube selected.');
            return;
        }

        var loName = nce.getSelectedCubeName().toLowerCase();

        if (loName.indexOf('rpm.class') != 0 && loName.indexOf('rpm.enum') != 0)
        {
            showRpmError('<b>' + info.name + '</b> is not an RPM Class.');
            return;
        }

        var result = nce.exec('RpmController.getFields', [nce.getAppId(), {'cube':nce.getSelectedCubeName()}]);
        if (result.status === false || !result.data.return || !result.data.return['@items'])
        {
            showRpmError('<b>' + info.name + '</b> is not an RPM Class.');
            return;
        }

        var rpmClassName;
        if (nce.getSelectedCubeName().startsWith('rpm.class'))
        {
            rpmClassName = nce.getSelectedCubeName().replace(/^rpm.class./, '');
        }
        else if (nce.getSelectedCubeName().startsWith('rpm.enum'))
        {
            rpmClassName = nce.getSelectedCubeName().replace(/^rpm.enum./, '');
        }
        else
        {
            showRpmError('<b>' + info.name + '</b> is not an RPM Class.');
            return;
        }
        buildTraitTable();

        $('#rpmClassName').html(rpmClassName);
        var rpmClass = result.data.return['@items'];
        fieldNames = [];
        var rowNums = [];
        var row = 0;
        $.each(rpmClass, function(index, col)
        {
            fieldNames.push(col.value);
            rowNums.push(row++);
        });

        clearRpmError();

        var container = document.getElementById('rpmTable'),
            settings = {
                autoColumnSize: true,
                enterBeginsEditing: false,
                enterMoves: {row: 1, col: 0},
                tabMoves : {row: 0, col: 1},
                colHeaders: traitTable.names,
                rowHeaders: rowNums,
                startCols: traitTable.names.length,
                startRows: rowNums.length,
                contextMenu: true,
                manualColumnResize: true,
                manualRowResize: true,
                fixedColumnsLeft: 1,
                height:500,
                cells: function (row, col, prop) {
                    var cellProperties = {};
                    cellProperties.renderer = categoryRenderer;
                    return cellProperties;
                }
            };

        hot = new Handsontable(container, settings);
        hot.render();
    };

    var categoryRenderer = function(instance, td, row, col, prop, value, cellProperties)
    {
        Handsontable.renderers.TextRenderer.apply(this, arguments);
        if (col == 0)
        {
            td.style.fontWeight = 'normal';
            td.style.color = traitTable.fore[col];
            td.style.background = traitTable.back[col];
            td.innerHTML = '<html>' + fieldNames[row] + '</html>';
        }
        else
        {
            td.style.fontWeight = 'normal';
            td.style.color = traitTable.fore[col];
            td.style.background = traitTable.back[col];
        }
    };

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

    return {
        init: init,
        load: load,
        render: render,
        handleCubeSelected: handleCubeSelected
    };

})(jQuery);


// Event handlers for events from NCE Frame
var tabActivated = function tabActivated(info)
{
    RpmEditor.init(info);
    RpmEditor.load();
};

var cubeSelected = function cubeSelected()
{
    RpmEditor.handleCubeSelected();
};