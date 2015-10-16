function buildDropDown(listId, inputId, list, callback)
{
    var ul = $(listId);
    ul.empty();
    $.each(list, function (key, value)
    {
        var li = $('<li/>');
        var anchor = $('<a href="#"/>');
        anchor.html(value);
        anchor.click(function (e)
        {   // User clicked on a dropdown entry, copy its text to input field
            e.preventDefault();
            $(inputId).val(anchor.html());
            callback(anchor.html());
        });
        li.append(anchor);
        ul.append(li);
    });
}

/**
 * Convert strings containing DOS-style '*' or '?' to a regex String.
 */
function wildcardToRegexString(wildcard)
{
    var s = '';

    for (var i = 0, is = wildcard.length; i < is; i++)
    {
        var c = wildcard.charAt(i);
        switch (c)
        {
            case '*':
                s += '.*?';
                break;

            case '?':
                s += '.';
                break;

            // escape special regexp-characters
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
            case '.':
            case '{':
            case '}':
            case '|':
            case '\\':
                s += '\\';
                s += c;
                break;

            default:
                s += c;
                break;
        }
    }
    return s;
}
