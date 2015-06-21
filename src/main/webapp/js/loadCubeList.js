/**
 * Web Worker thread
 *
 *     This Javascript background thread is used to filter the list of cubes
 * based on the passed in filter string.  The data passed into onmessage(),
 * which comes from a postMessage(), is an array [] of two (2) elements.
 * [0] = NCE's cube list, which is a Map (object) 'cubeName' ==> infoDto
 * [1] = filter string.  This will be used to filter the above list.
 *
 * @return a stable list of cubes, filtered by the passed in string.
 * @author John DeRegnaucourt
 */

onmessage = function(e)
{
    var args = e.data;
    var filter = args[0];
    var content = args[1];
    var appId = args[2];
    var hasFilter = filter && filter.length > 0;
    var appIdString = JSON.stringify(appId);
    var req = new XMLHttpRequest();

    var regexp = /\/([^\/]+)\//g;
    var match = regexp.exec(location.pathname);
    var url;
    if (match == null || match.length != 2)
    {
        url = location.protocol + '//' + location.hostname + ":" + location.port + "/cmd/ncubeController/search";
    }
    else
    {
        var ctx = match[1];
        url = location.protocol + '//' + location.hostname + ":" + location.port + "/" + ctx + "/cmd/ncubeController/search";
    }

    console.log(url);
    req.open("GET", url + '?json=[' + appIdString + ',"' + filter + '","' + content + '"]', false);
    req.send();

    if (req.response)
    {
        var searchResults = JSON.parse(req.response);
        if (searchResults.status === true)
        {
            var cubes = {};
            var results = searchResults.data;
            var len = results.length;
            for (var i = 0; i < len; ++i)
            {
                var infoDto = results[i];
                cubes[infoDto.name] = infoDto;
                if (hasFilter)
                {
                    var idx = infoDto.name.toLowerCase().indexOf(filter);
                    if (idx >= 0)
                    {   // record starting location of cube name filter, so UI can display highlighted matching text
                        infoDto.pos = idx;
                    }
                }
            }

            postMessage(cubes);
        }
    }
}
