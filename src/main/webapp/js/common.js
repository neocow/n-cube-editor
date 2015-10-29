/**
 * NCube Editor
 *     Common Javascript utilities for all tabs to use.
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

/**
 * return number of 'own' keys in object
 */
function countKeys(object)
{
    var count = 0;
    for (var key in object)
    {
        if (object.hasOwnProperty(key))
        {
            count++;
        }
    }
    return count;
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

/**
 * Escape regex characters in source String.  For example, period (.) becomes \.
 */
function escapeRegExp(string)
{
    return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}

/**
 * Check all the inputs in a list.
 */
function checkAll(state, queryStr)
{
    var input = $(queryStr);
    $.each(input, function (index, btn)
    {
        $(this).prop('checked', state);
    });
}

function diffFile()
{
    var outputDiv = $('#diffOutput');
    outputDiv.empty();
    var base = [
        'The quick brown fox jumps over the lazy dog.',
        'This one is the same',
        'This one also is the same',
        'This one also again is the same',
        'This one was deleted',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line'
    ];
    var newtxt = [
        'The kiwk brown focks jumps over the lazy dawg.',
        'This one is the same',
        'This line was added',
        'This one also is the same',
        'This one also again is the same',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line',
        'Another same line'
    ];

    // create a SequenceMatcher instance that diffs the two sets of lines
    var sm = new difflib.SequenceMatcher(base, newtxt);

    // get the opcodes from the SequenceMatcher instance
    // opcodes is a list of 3-tuples describing what changes should be made to the base text
    // in order to yield the new text
    var opcodes = sm.get_opcodes();
    var contextSize = $("contextSize").value;
    contextSize = contextSize ? contextSize : null;

    // build the diff view and add it to the current DOM
    outputDiv[0].appendChild(diffview.buildView({
        baseTextLines: base,
        newTextLines: newtxt,
        opcodes: opcodes,
        // set the display titles for each resource
        baseTextName: "Base Text",
        newTextName: "New Text",
        contextSize: contextSize,
        viewType: 0
    }));
    $('#diffOutput .author').remove();
    $('#diffOutputModal').css('display', 'block');

    // TODO:
    // 1. Trap keyboard (escape = close)
    // 2. Set title bar to cube name
    // 3. Set 'HEAD' on left and 'branch name' on right
}

function keyCount(obj)
{
    var size = 0, key;
    for (key in obj)
    {
        if (obj.hasOwnProperty(key))
        {
            size++;
        }
    }
    return size;
}

/**
 * Fill the list identified by listId, with items from the list 'list',
 * where the list is an array of Strings.  A click listener will be
 * added to each item, so that when the user clicks on an itemin the list,
 * the input identified by inputId, will be filled with the selected text,
 * and the passed in callback function will be called on the click (selection).
 */
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