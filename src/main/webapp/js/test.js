/**
 * Test Panel for N-Cube Editor
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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

$(function ()
{
    $.info = {};

    $.loadData = function()
    {
        if (!nce().getCubeMap() || !nce().doesCubeExist())
        {
            return;
        }

        var info = nce().getCubeMap()[(nce().getSelectedCubeName() + '').toLowerCase()];
        if (!info)
        {
            return;
        }

        var secondaryLayout = $('#testBody').layout({
            name: "secondaryLayout"
            ,   closable:					true	// pane can open & close
            ,	resizable:					true	// when open, pane can be resized
            ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
            ,	livePaneResizing:			true
            ,	east__minSize:				170
            ,   togglerLength_open:         60
            ,   togglerLength_closed:       "100%"
            ,	spacing_open:			5  // ALL panes
            ,	spacing_closed:			5 // ALL panes
            //            ,	south__spacing_open:			5  // ALL panes
            //,	south__spacing_closed:			5 // ALL panes
            ,  east__onresize: function()
            {
                calculateTestPanelSize();
            }
        });

        secondaryLayout.resizeAll();
        calculateTestPanelSize();
    };

    function calculateTestPanelSize()
    {
        var east = $('#testLayoutEast');
        var testList = $('#testList').find('> .panel-body');
        testList.height(east.height() - 47);
    }
});

function nce()
{
    return $.info.fn;
}

function tabActivated(info)
{
    try
    {
        $.info = info;
        $.loadData();
    }
    catch (e)
    {
        console.log(e);
    }
}
