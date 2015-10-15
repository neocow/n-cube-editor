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
