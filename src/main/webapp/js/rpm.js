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

RpmEditor = (function($)
{
    $.info = {};
    var hot = null;

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
    };

    return {
        init: function()
        {
            var fieldNames = [];
            for (var i=0; i < 50; i++)
            {
                fieldNames.push('field' + (i + 1));
            }
            var data1 = [
                    ['', 'Kia', 'Nissan', 'Toyota', 'Honda', 'Mazda', 'Ford','Kia', 'Nissan', 'Toyota', 'Honda', 'Mazda', 'Ford','Kia', 'Nissan', 'Toyota', 'Honda', 'Mazda', 'Ford','Kia', 'Nissan', 'Toyota', 'Honda', 'Mazda', 'Ford'],
                    ['2012', 10, 11, 12, 13, 15, 16],
                    ['2013', 10, 11, 12, 13, 15, 16],
                    ['2014', 10, 11, 12, 13, 15, 16],
                    ['2015', 10, 11, 12, 13, 15, 16],
                    ['2016', 10, 11, 12, 13, 15, 16],
                    ['2017', 10, 11, 12, 13, 15, 16],
                    ['2018', 10, 11, 12, 13, 15, 16],
                    ['2019', 10, 11, 12, 13, 15, 16],
                    ['2020', 10, 11, 12, 13, 15, 16],
                    ['2021', 10, 11, 12, 13, 15, 16],
                    ['2022', 10, 11, 12, 13, 15, 16],
                    ['2023', 10, 11, 12, 13, 15, 16],
                    ['2024', 10, 11, 12, 13, 15, 16],
                    ['2025', 10, 11, 12, 13, 15, 16],
                    ['2026', 10, 11, 12, 13, 15, 16],
                    ['2027', 10, 11, 12, 13, 15, 16],
                    ['2028', 10, 11, 12, 13, 15, 16],
                    ['2029', 10, 11, 12, 13, 15, 16],
                    ['2030', 10, 11, 12, 13, 15, 16],
                    ['2031', 10, 11, 12, 13, 15, 16],
                    ['2032', 10, 11, 12, 13, 15, 16],
                    ['2033', 10, 11, 12, 13, 15, 16],
                    ['2034', 10, 11, 12, 13, 15, 16],
                    ['2035', 10, 11, 12, 13, 15, 16],
                    ['2036', 10, 11, 12, 13, 15, 16],
                    ['2037', 10, 11, 12, 13, 15, 16],
                    ['2038', 10, 11, 12, 13, 15, 16],
                    ['2039', 10, 11, 12, 13, 15, 16],
                    ['2040', 10, 11, 12, 13, 15, 16],
                    ['2041', 10, 11, 12, 13, 15, 16],
                    ['2042', 10, 11, 12, 13, 15, 16],
                    ['2043', 10, 11, 12, 13, 15, 16],
                    ['2044', 10, 11, 12, 13, 15, 16],
                    ['2045', 10, 11, 12, 13, 15, 16],
                    ['2046', 10, 11, 12, 13, 15, 16],
                    ['2047', 10, 11, 12, 13, 15, 16],
                    ['2048', 10, 11, 12, 13, 15, 16],
                    ['2049', 10, 11, 12, 13, 15, 16],
                    ['2050', 10, 11, 12, 13, 15, 16],
                    ['2051', 10, 11, 12, 13, 15, 16],
                    ['2052', 10, 11, 12, 13, 15, 16],
                    ['2053', 10, 11, 12, 13, 15, 16],
                    ['2054', 10, 11, 12, 13, 15, 16],
                    ['2055', 10, 11, 12, 13, 15, 16],
                    ['2056', 10, 11, 12, 13, 15, 16],
                    ['2057', 10, 11, 12, 13, 15, 16],
                    ['2058', 10, 11, 12, 13, 15, 16],
                    ['2059', 10, 11, 12, 13, 15, 16]
                ],
                container = document.getElementById('example'),
                settings = {
                    data: data1,
                    colHeaders: ['name', 'type', 'default value'],
                    rowHeaders: fieldNames
                };

            hot = new Handsontable(container, settings);
            hot.render();
        }
    };
})(jQuery);

$(function()
{
    RpmEditor.init();
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
