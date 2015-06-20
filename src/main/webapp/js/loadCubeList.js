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
    var cubeList = args[0];
    var filter = args[1];
    var content = args[2];
    var appId = args[3];
    var hasFilter = filter && filter.length > 0;
    var cubes = hasFilter ? {} : cubeList;
    if (hasFilter)
    {
        filter = filter.toLowerCase();

        // Step 1. Filter all cubes and store into a Map keyed by match location (int) to list of cubes that matched starting at this location.
        var matches = {};
        var count = 0;
        for (var key in cubeList)
        {
            if (cubeList.hasOwnProperty(key))
            {
                var infoDto = cubeList[key];
                infoDto.pos = null;
                var idx = infoDto.name.toLowerCase().indexOf(filter);
                if (idx >= 0)
                {
                    if (!matches[idx])
                    {
                        matches[idx] = [];
                    }
                    matches[idx].push(infoDto);
                    count++;
                    if (count >= 100)
                    {
                        break;
                    }
                }
            }
        }

        // Step 2. Make one big list from lists above, recording start position in InfoDto
        for (var pos in matches)
        {
            if (matches.hasOwnProperty(pos))
            {
                var list = matches[pos];
                for (var i=0; i < list.length; i++)
                {
                    var infoDto1 = list[i];
                    cubes[infoDto1.name] = infoDto1;
                    infoDto1.pos = parseInt(pos);
                }
            }
        }
    }

    var req = new XMLHttpRequest();
    // http://localhost:8080/nce/cmd/ncubeController/search?json=[{%22app%22:%22riskrum%22,%22version%22:%221.0.0%22,%22status%22:%22SNAPSHOT%22,%22branch%22:%22john%22},%20%22a%22,%20%22fat%22]

    var appIdString = JSON.stringify(appId);
    console.log(appIdString);
    req.open("GET",'http://localhost:8080/nce/cmd/ncubeController/search?json=[' + appIdString + ',"' + filter + '","' + content + '"]', false);
    req.send();
    console.log(req.response);
    if (req.response)
    {
        var xyz = JSON.parse(req.response);
    }
    postMessage(cubes);
}
