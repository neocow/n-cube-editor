/**
 * options:
 *      title               - title of modal dialog
 *      instructionsTitle   - title of instructions area (omit for no instructions box)
 *      instructionsText    - text of instructions area (omit for no instructions box)
 *      afterSave           - callback fires upon clicking save button
 *      readonly            - should the user be able to edit or only view; default FALSE
 *      displayType         - type of display (table, list, form); use constant DISPLAY_TYPE
 *      draggable           - should modal be draggable; default TRUE
 *      size                - size of modal; use constant MODAL_SIZE; default LARGE
 *      hasSelectAllNone    - does modal use select all and select none; default FALSE
 *      hasFilter           - does modal use filter; list view only; default FALSE
 *      columns             - columns to show on modal for either table or list view
 *          heading         - heading for column
 *          type            - type of input for column; use constant INPUT_TYPE
 *          default         - used if column has a default value (such as true for checkbox) for new rows
 *          selectOptions   - if type is SELECT, use for list of options for select input
 *          hasFilter       - can filter on column; default FALSE
 *          sortable        - can sort on column; default FALSE
 *      formInputs          - input values to use in form view
 *          type            - type of input; use constant INPUT_TYPE; default TEXT
 *          default         - if input has a desired default value
 *          label           - label for input field
 *          layout          - layout for input field; use constant INPUT_LAYOUT; default NEW_LINE
 *          readonly        - should be disabled
 *          hidden          - should be hidden
 *          listeners       - listeners to attach to the field
 *              action      - listener action to attach
 *              func        - listener result
 */

var FormBuilder = (function ($) {

    // constants
    var INPUT_TYPE = {
        CHECKBOX: 'checkbox',
        READONLY: 'readonly',
        SECTION: 'section',
        SELECT: 'select',
        TEXT: 'text'
    };
    var DISPLAY_TYPE = {
        FORM: 'form',
        LIST: 'list',
        TABLE: 'table'
    };
    var INPUT_LAYOUT = {
        INLINE: 'inline',
        NEW_LINE: 'new-line',
        TABLE: 'table'
    };
    var MODAL_SIZE = {
        MEDIUM: '',
        LARGE: 'modal-lg',
        XL: 'modal-xl'
    };
    //noinspection MagicNumberJS
    var TD_CSS = {
        'padding-right': 25,
        'text-align':'center'
    };
    var TR_CLASS = 'builder-data-row';

    // elements;
    var _modal = null;
    var _table = null;
    var _list = null;

    // external variables
    var _data = null;
    var _options = null;

    // public
    function openBuilderModal(options, data) {
        var container;
        _data = data || {};
        _options = options;
        _modal = buildModal();
        container = _modal.find('.modal-content');
        container.append(buildModalHeader());
        container.append(buildModalContent());
        container.append(buildModalFooter());

        if (_options.displayType !== DISPLAY_TYPE.FORM) {
            container.find('input,textarea,select').attr('disabled', _options.readonly);
        }

        _modal.modal();
    }

    function buildModal() {
        var sizeClass = _options.size || MODAL_SIZE.MEDIUM;
        var filterClass = _options.hasFilter ? 'modal-filter' : '';

        var html = '<div tabindex="-1" data-role="dialog" data-backdrop="static" class="modal fade">'
                 + '<div class="modal-dialog ' + sizeClass + '">'
                 + '<div class="modal-content ' + filterClass + '"></div>'
                 + '</div></div>';

        var modal = $(html).on('shown.bs.modal', function() {
            makeModalDraggable();
        }).on('hidden.bs.modal', function() {
            $(this).remove();
        });

        if (!_options.readonly) {
            modal.keyup(function (e) {
                if (_options.displayType === DISPLAY_TYPE.TABLE) {
                    if (e.keyCode === KEY_CODES.ENTER) {
                        addTableRow();
                    }
                }
            });
        }

        return modal;
    }

    function buildModalHeader() {
        var html = '<div class="modal-header"><button class="close">&times;</button>'
                 + '<h3 class="modal-title">' + _options.title + '</h3>'
                 + '</div>';
        var header = $(html);
        header.find('button').on('click', function() { closeBuilderModal() });
        return header;
    }

    function closeBuilderModal(shouldSave) {
        if (shouldSave) {
            switch (_options.displayType) {
                case DISPLAY_TYPE.TABLE:
                    copyTableDataToModel();
                    break;
                case DISPLAY_TYPE.FORM:
                    copyFormDataToModel(_options.formInputs);
                    break;
            }
            if (_options.hasOwnProperty('afterSave')) {
                _options.afterSave(_data);
            }
        }
        _modal.modal('hide');
    }

    function buildModalFooter() {
        var html, footer;
        var buttons = {
            add: '',
            cancel: '<button class="btn btn-default btn-sm form-builder-cancel">Cancel</button>',
            clear: '',
            save: '',
            selectAll: '',
            selectNone: ''
        };

        if (!_options.readonly) {
            buttons.save = '<button class="btn btn-primary btn-sm form-builder-save">Save</button>';
            if (_options.displayType !== DISPLAY_TYPE.FORM) {
                if (_options.hasSelectAllNone) {
                    buttons.selectAll = '<button class="btn btn-info btn-sm pull-left form-builder-select-all" aria-hidden="true">Select All</button>';
                    buttons.selectNone = '<button class="btn btn-info btn-sm pull-left form-builder-select-none" aria-hidden="true">Select None</button>';
                }
                buttons.add = '<button class="btn btn-success btn-sm form-builder-add">Add New</button>';
                buttons.clear = '<button class="btn btn-danger btn-sm form-builder-clear">Clear</button>';
            }
        }

        html = '<div class="modal-footer">'
             + buttons.selectAll + buttons.selectNone
             + buttons.add + buttons.clear + buttons.cancel + buttons.save
             + '</div>';

        footer = $(html);
        footer.find('.form-builder-select-all').on('click', function() { checkAll(true, 'input[type="checkbox"]'); });
        footer.find('.form-builder-select-none').on('click', function() { checkAll(false, 'input[type="checkbox"]'); });
        footer.find('.form-builder-add').on('click', function() { addTableRow() });
        footer.find('.form-builder-clear').on('click', function() { clearTableRows() });
        footer.find('.form-builder-cancel').on('click', function() { closeBuilderModal() });
        footer.find('.form-builder-save').on('click', function() { closeBuilderModal(true) });

        return footer;
    }

    function buildModalContent() {
        var body;
        var html = '<div class="modal-body" style="overflow-y:auto;min-height:450px;">';

        if (_options.hasOwnProperty('instructionsTitle') && _options.hasOwnProperty('instructionsText')) {
            html += '<div class="panel-group"><div class="panel panel-default">'
                  + '<div class="panel-heading"><h4 class="panel-title">'
                  + '<a data-toggle="collapse" href="#form-builder-inst-collapse">' + _options.instructionsTitle + '</a></h4></div>'
                  + '<div id="form-builder-inst-collapse" class="panel-collapse collapse in">'
                  + '<div class="panel-body">' + _options.instructionsText + '</div></div></div></div>';
        }

        body = $(html);
        switch (_options.displayType) {
            case DISPLAY_TYPE.TABLE:
                body.append(buildTable());
                break;
            case DISPLAY_TYPE.FORM:
                body.append(buildForm());
                break;
        }
        return body;
    }

    function buildForm() {
        var form = $('<form role="form"/>');
        form.append(buildFormSection(_options.formInputs, _options.readonly));
        return form;
    }

    function buildFormSection(formInputs, readonly) {
        var i, len, formInput, key, subSection;
        var section = $('<div style="clear:both;"/>');
        var keys = Object.keys(formInputs);
        for (i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            formInput = formInputs[key];
            formInput.name = key;
            if (!formInput.hidden) {
                if (formInput.type === INPUT_TYPE.SECTION) {
                    subSection = $('<div class="form-builder-section" style="padding:10px;border:1px solid #CCC;margin-bottom:10px;">'
                        + '<h4 style="float:left;margin-top:-18px;background-color:white;">&nbsp;'
                        + formInput.label + '&nbsp;</h4></div>');
                    subSection.append(buildFormSection(formInput.formInputs, readonly || formInput.readonly));
                    section.append(subSection);
                } else {
                    section.append(buildFormInput(formInput, readonly));
                }
            }
        }
        return section;
    }

    function buildFormInput(formInput, readonlyOverride) {
        var inputGroup;
        var inputId = 'form-builder-input-' + formInput.name;
        var readonly = readonlyOverride || formInput.readonly;
        var control = $('<div class="form-group"/>');
        var initVal = '';

        if (formInput.data !== undefined && formInput.data !== null) {
            initVal = formInput.data;
        } else if (formInput.hasOwnProperty('default')) {
            initVal = formInput.default;
        }

        if (formInput.layout === INPUT_LAYOUT.TABLE) {
            switch (formInput.type) {
                default:
                    inputGroup = $('<label for="' + inputId + '" style="width:20%;">' + formInput.label + ':</label>'
                        + '<input id="' + inputId + '" type="text" class="form-control" style="display:inline-block;width:77%;"'
                        + (readonly ? 'readonly' : '') + '>');
                    inputGroup.last().val(initVal);
                    break;
            }
        } else {
            switch (formInput.type) {
                case INPUT_TYPE.CHECKBOX:
                    inputGroup = $('<div class="checkbox"><label><input id="' + inputId + '" type="checkbox"'
                          + (readonly ? 'disabled' : '') + '>' + formInput.label + '</label></div>');
                    inputGroup.find('input')[0].checked = initVal;
                    break;
                default:
                    inputGroup = $('<label for="' + inputId + '">' + formInput.label + '</label>'
                          + '<input id="' + inputId + '" type="text" class="form-control"'
                          + (readonly ? 'readonly' : '') + '>');
                    inputGroup.last().val(initVal);
                    break;
            }
        }

        control.append(inputGroup);
        if (formInput.hasOwnProperty('listeners')) {
            addListenersToControl(control, formInput.listeners);
        }
        return control;
    }

    function addListenersToControl(control, listeners) {
        // TODO - implement, duh
    }

    function buildTable() {
        var columns, columnKeys, headingRow, c, cLen, column, header;
        _table = $('<table/>').css({margin: '0 auto'});

        columns = _options.columns;
        columnKeys = Object.keys(columns);
        _options.columnKeys = columnKeys;
        headingRow = $('<tr/>');
        _table.append(headingRow);
        for (c = 0, cLen = columnKeys.length; c < cLen; c++) {
            column = columns[columnKeys[c]];
            header = $('<th/>').html(column.heading).css(TD_CSS);
            if (column.sortable) {
                header.on('click', function() {
                    sortTable(this);
                });
            }
            headingRow.append(header);
        }
        createTableRows();
        return _table;
    }

    function sortTable(header) {
        var idx = _table.find('th').index(header);
        var key = _options.columnKeys[idx];
        _data.sort(function(a, b) {
            return a[key] - b[key];
        });
    }

    function createTableRows() {
        var d, dLen;
        dLen = _data.length;
        if (dLen) {
            for (d = 0; d < dLen; d++) {
                addTableRow(_data[d]);
            }
        } else {
            addTableRow(); //start with empty row
        }
    }

    function addTableRow(dataRow) {
        var c, cLen, key, column, dataVal, inputElement, closeBtn;
        var tr = $('<tr/>').addClass(TR_CLASS);
        var columnKeys = _options.columnKeys;
        var columns = _options.columns;
        for (c = 0, cLen = columnKeys.length; c < cLen; c++) {
            key = columnKeys[c];
            column = null;
            column = columns[key];
            if (dataRow) {
                dataVal = dataRow[key];
            } else if (column.hasOwnProperty('default')) {
                dataVal = column.default;
            } else {
                dataVal = null;
            }

            inputElement = null;
            inputElement = getDataRowInput(column, dataVal);
            inputElement.addClass(key);
            tr.append($('<td/>').append(inputElement).css(TD_CSS));
        }

        if (!_options.readonly) {
            closeBtn = $('<span/>').addClass('glyphicon glyphicon-remove tab-close-icon');
            closeBtn.click(function () {
                tr.remove();
            });
        }
        _table.append(tr);
        tr.append($('<td/>').append(closeBtn));
        tr.find('input').first().focus();
    }

    function getDataRowInput(column, dataVal) {
        var inputElement, selectOptions, o, oLen, optEl, curOption;
        switch (column.type) {
            case INPUT_TYPE.CHECKBOX:
                inputElement = $('<input/>').prop({type:'checkbox', checked:dataVal});
                break;
            case INPUT_TYPE.READONLY:
                inputElement = $('<span/>').html(dataVal);
                break;
            case INPUT_TYPE.SELECT:
                inputElement = $('<select/>');
                selectOptions = column.selectOptions;
                for (o = 0, oLen = selectOptions.length; o < oLen; o++) {
                    optEl = $('<option/>');
                    curOption = selectOptions[o];

                    if (typeof curOption === 'object') {
                        optEl.val(curOption.key);
                        optEl.text(curOption.value);
                    } else {
                        optEl.text(selectOptions[o]);
                    }

                    inputElement.append(optEl);
                }
                inputElement.val(dataVal);
                break;
            case INPUT_TYPE.TEXT:
                inputElement = $('<input/>').prop('type','text').val(dataVal);
                break;
        }
        return inputElement;
    }

    function clearTableRows() {
        findTableRows().remove();
    }

    function findTableRows() {
        return _table.find('tr.' + TR_CLASS);
    }

    function copyTableDataToModel() {
        var trs, columnKeys, columns, trIdx, trLen, tr;
        _data.splice(0, _data.length);
        trs = findTableRows();
        columnKeys = _options.columnKeys;
        columns = _options.columns;
        for (trIdx = 0, trLen = trs.length; trIdx < trLen; trIdx++) {
            tr = null;
            tr = $(trs[trIdx]);
            _data.push(getDataRow(columns, columnKeys, tr));
        }
    }

    function copyFormDataToModel(formInputs) {
        var i, len, key, input, inputOpts, val;
        var keys = Object.keys(formInputs);
        for (i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            input = $('#form-builder-input-' + key);
            inputOpts = formInputs[key];
            if (!inputOpts.hidden) {
                switch (inputOpts.type) {
                    case INPUT_TYPE.SECTION:
                        copyFormDataToModel(inputOpts.formInputs);
                        break;
                    case INPUT_TYPE.CHECKBOX:
                        val = input.prop('checked');
                        break;
                    default:
                        val = input.val();
                        break;
                }
                _data[key] = val || inputOpts.default;
            }
        }
    }

    function getDataRow(columns, columnKeys, tr) {
        var c, cLen, key, inputElement, dataVal;
        var dataRow = {};
        for (c = 0, cLen = columnKeys.length; c < cLen; c++) {
            key = columnKeys[c];
            inputElement = null;
            inputElement = tr.find('.' + key);

            switch (columns[key].type) {
                case INPUT_TYPE.CHECKBOX:
                    dataVal = inputElement[0].checked;
                    break;
                case INPUT_TYPE.READONLY:
                    dataVal = inputElement[0].innerHTML;
                    break;
                case INPUT_TYPE.SELECT:
                case INPUT_TYPE.TEXT:
                    dataVal = inputElement.val();
                    break;
            }
            dataRow[key] = dataVal;
        }
        return dataRow;
    }

    // I know this code is also in common.js, but I wanted FormBuilder to be independent
    function makeModalDraggable() {
        var realPosX, realPosY;
        var shouldBeDraggable = !_options.hasOwnProperty('draggable') || _options.draggable;
        var $doc = $(document);
        var docWidth = $doc.width();
        var docHeight = $doc.height();
        var content = _modal.find('.modal-content');
        var modalWidth = content.outerWidth();
        var modalHeight = content.outerHeight();
        var marginTop = _modal.find('.modal-dialog').css('margin-top');
        marginTop = marginTop.substring(0, marginTop.indexOf('px'));
        marginTop = parseInt(marginTop);
        realPosX = docWidth - modalWidth;
        realPosY = docHeight - modalHeight - marginTop * 2;
        _modal.draggable({
            handle: '.modal-header',
            containment: [-realPosX, -marginTop, realPosX, realPosY]
        });
        _modal.draggable(shouldBeDraggable && docHeight > modalHeight ? 'enable' : 'disable');
    }

    function checkAll(state, queryStr) {
        $(queryStr).filter(':visible').not('.exclude').prop('checked', state).change();
    }

    return {
        openBuilderModal: openBuilderModal,
        DISPLAY_TYPE: DISPLAY_TYPE,
        INPUT_LAYOUT: INPUT_LAYOUT,
        INPUT_TYPE: INPUT_TYPE
    };

})(jQuery);