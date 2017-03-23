/**
 * options:
 *      title               - title of modal dialog
 *      instructionsTitle   - title of instructions area (omit for no instructions box)
 *      instructionsText    - text of instructions area (omit for no instructions box)
 *      saveButtonText      - text of save button; default 'Save'
 *      closeButtonText     - text of close button; default 'Cancel'
 *      afterSave           - callback fires upon clicking save button
 *      onClose             - callback fires whenever modal closes (save or otherwise)
 *      closeAfterSave      - should the modal close or stay open after saving; default TRUE
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
 *          css             - optional css for column
 *      formInputs          - input values to use in form view
 *          collapsible     - used to collapse sections; default FALSE
 *          sectionType     - bootstrap type style of section; use constant BOOTSTRAP_TYPE
 *          type            - type of input; use constant INPUT_TYPE; default TEXT
 *          default         - if input has a desired default value
 *          selectOptions   - if type is SELECT, use for list of options for select input
 *          label           - label for input field
 *          layout          - layout for input field; use constant INPUT_LAYOUT; default NEW_LINE
 *          readonly        - should be disabled
 *          hidden          - should be hidden
 *          listeners       - listeners to attach to the field
 *              action:func - listener action to attach
 */

var FormBuilder = (function ($) {

    // constants
    var INPUT_TYPE = {
        CHECKBOX: 'checkbox',
        READONLY: 'readonly',
        SECTION: 'section',
        SELECT: 'select',
        TABLE: 'table',
        TEXT: 'text',
        TEXT_SELECT: 'text-select'
    };
    var BOOTSTRAP_TYPE = {
        DANGER: 'danger',
        DEFAULT: 'default',
        INFO: 'info',
        PRIMARY: 'primary',
        SUCCESS: 'success',
        WARNING: 'warning'
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
        SMALL: 'modal-sm',
        MEDIUM: '',
        LARGE: 'modal-lg',
        XL: 'modal-xl'
    };
    //noinspection MagicNumberJS
    var TD_CSS = {
        'padding-right': 25,
        'text-align':'center'
    };
    var TR_CLASS = 'form-builder-data-row';

    var ID_PREFIX = {
        COLLAPSE: 'form-builder-collapse-',
        INPUT: 'form-builder-input-',
        SECTION: 'form-builder-section-'
    };

    // elements;
    var _modal = null;

    // external variables
    var _data = null;
    var _options = null;

    // public
    function openBuilderModal(options, data) {
        var container;
        var buildNew = !_modal;
        _data = data || {};
        _options = options;
        setDefaultOptions();

        _modal = _modal || buildModal();
        container = _modal.find('.modal-content');
        if (!buildNew) {
            container.empty();
        }
        container.append(buildModalHeader());
        container.append(buildModalContent());
        container.append(buildModalFooter());

        if (_options.displayType !== DISPLAY_TYPE.FORM) {
            container.find('input,textarea,select').attr('disabled', _options.readonly);
        }

        if (buildNew) {
            _modal.modal();
        }
    }

    function setDefaultOptions() {
        if (!_options.hasOwnProperty('size')) {
            _options.size = MODAL_SIZE.MEDIUM;
        }
        if (!_options.hasOwnProperty('saveButtonText')) {
            _options.saveButtonText = 'Save';
        }
        if (!_options.hasOwnProperty('closeButtonText')) {
            _options.closeButtonText = 'Cancel';
        }
        if (!_options.hasOwnProperty('draggable')) {
            _options.draggable = true;
        }
        if (!_options.hasOwnProperty('closeAfterSave')) {
            _options.closeAfterSave = true;
        }
    }

    function buildModal() {
        var sizeClass = _options.size;
        var filterClass = _options.hasFilter ? 'modal-filter' : '';

        var html = '<div tabindex="-1" data-role="dialog" data-backdrop="static" class="modal fade">'
                 + '<div class="modal-dialog ' + sizeClass + '">'
                 + '<div class="modal-content ' + filterClass + '"></div>'
                 + '</div></div>';

        var modal = $(html).on('shown.bs.modal', function() {
            makeModalDraggable();
            positionDropdownMenus();
        }).on('hidden.bs.modal', function() {
            closeModal();
        });

        if (!_options.readonly) {
            modal.keyup(function (e) {
                if (_options.displayType === DISPLAY_TYPE.TABLE) {
                    if (e.keyCode === KEY_CODES.ENTER) {
                        addTableRowForTableTypeBuilder();
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
        var shouldClose = _options.closeAfterSave;
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
        if (!shouldSave || (shouldSave && shouldClose)) {
            _modal.modal('hide');
        }
    }

    function buildModalFooter() {
        var html, footer;
        var buttons = {
            add: '',
            cancel: '<button class="btn btn-default btn-sm form-builder-cancel">' + _options.closeButtonText + '</button>',
            clear: '',
            save: '',
            selectAll: '',
            selectNone: ''
        };

        if (!_options.readonly) {
            buttons.save = '<button class="btn btn-primary btn-sm form-builder-save">' + _options.saveButtonText + '</button>';
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
        footer.find('.form-builder-add').on('click', function() { addTableRowForTableTypeBuilder(); });
        footer.find('.form-builder-clear').on('click', function() { clearTableRows() });
        footer.find('.form-builder-cancel').on('click', function() { closeBuilderModal() });
        footer.find('.form-builder-save').on('click', function() { closeBuilderModal(true) });

        return footer;
    }

    function buildModalContent() {
        var style = _options.size === MODAL_SIZE.SMALL ? '' : 'overflow-y:auto;min-height:450px;';
        var body = $('<div class="modal-body" style="'+ style + '">');

        if (_options.hasOwnProperty('instructionsTitle') && _options.hasOwnProperty('instructionsText')) {
            body.append(initSubSection('inst', _options.instructionsTitle, _options.instructionsText, BOOTSTRAP_TYPE.DEFAULT, true));
        }

        switch (_options.displayType) {
            case DISPLAY_TYPE.TABLE:
                body.append(buildTable(_data, _options));
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
        var i, len, formInput, key, subSection, control, sectionReadonly;
        var section = $('<div style="clear:both;"/>');
        var keys = Object.keys(formInputs);
        for (i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            formInput = formInputs[key];
            formInput.name = key;
            if (!formInput.hidden) {
                if (formInput.type === INPUT_TYPE.SECTION) {
                    subSection = initSubSectionFromFormInput(formInput);
                    sectionReadonly = readonly || formInput.readonly;
                    control = buildFormSection(formInput.formInputs, sectionReadonly);
                    addToSubSection(subSection, control);
                    section.append(subSection);
                } else {
                    control = buildFormControl(formInput, readonly);
                    section.append(control);
                }
            }
        }
        return section;
    }

    function initSubSection(id, title, body, type, collapsible) {
        var sectionId = ID_PREFIX.SECTION + id;
        var collapseId = ID_PREFIX.COLLAPSE + id;
        var panelType = type || BOOTSTRAP_TYPE.DEFAULT;
        var html = '<div id="' + sectionId + '" class="panel-group">'
            + '<div class="panel panel-' + panelType + '">'
            + '<div class="panel-heading"><h4 class="panel-title">'
            + (collapsible ? ('<a data-toggle="collapse" href="#' + collapseId + '">' + title + '</a>') : title)
            + '</h4></div><div id="' + collapseId + '" class="panel-collapse collapse in">'
            + '<div class="panel-body">' + (body || '') + '</div></div></div></div>';
        return $(html);
    }
    
    function initSubSectionFromFormInput(formInput) {
        return initSubSection(formInput.name, formInput.label, formInput.data, formInput.sectionType, formInput.collapsible);
    }
    
    function addToSubSection(section, toAdd) {
        section.find('.panel-body').append(toAdd);
    }

    function buildFormControl(formInput, readonlyOverride) {
        var control = $('<div class="form-group"/>');
        control.append(buildFormInput(formInput, readonlyOverride));
        if (formInput.hasOwnProperty('listeners')) {
            addListenersToControl(control, formInput.listeners);
            if (formInput.listeners.hasOwnProperty('populate')) {
                control.find('input,select').trigger('populate');
            }
        }
        return control;
    }

    function toggle(formInputKey, forceState) {
        var control, nearest;
        var formInputInfo = findFormInputByKey(_options.formInputs, formInputKey);
        var formInput = formInputInfo.current;
        formInput.hidden = forceState !== undefined && forceState !== null ? !forceState : !formInput.hidden;

        if (formInput.hidden) {
            findInputGroup(formInput).remove();
        } else {
            if (formInput.type === INPUT_TYPE.SECTION) {
                control = initSubSectionFromFormInput(formInput);
                addToSubSection(control, buildFormSection(formInput.formInputs));
            } else {
                control = buildFormControl(formInput);
            }

            if (formInputInfo.hasOwnProperty('before')) {
                nearest = formInputInfo.before;
                findInputGroup(nearest).after(control);
            } else if (formInputInfo.hasOwnProperty('after')) {
                nearest = formInputInfo.after;
                findInputGroup(nearest).before(control);
            } else {
                _modal.find('form').append(control);
            }
        }
    }

    function findInputGroup(formInput) {
        var isSection = formInput.type === INPUT_TYPE.SECTION;
        var prefix = isSection ? ID_PREFIX.SECTION : ID_PREFIX.INPUT;
        var selector = isSection ? '.form-builder-section' : '.form-group';
        return _modal.find('#' + prefix + formInput.name).closest(selector);
    }

    function findFormInputByKey(formInputs, searchKey) {
        var i, len, formInput, lowerRet;
        var ret = {};
        var keys = Object.keys(formInputs);
        for (i = 0, len = keys.length; i < len; i++) {
            formInput = formInputs[keys[i]];
            if (formInput.name === searchKey) {
                ret.current = formInput;
                if (ret.hasOwnProperty('before')) {
                    return ret;
                }
            } else if (!formInput.hidden) {
                if (formInput.type === INPUT_TYPE.SECTION) {
                    lowerRet = findFormInputByKey(formInput.formInputs, searchKey);
                    if (lowerRet.hasOwnProperty('current')) {
                        return lowerRet;
                    }
                }
                if (ret.hasOwnProperty('current')) {
                    ret.after = formInput;
                    return ret;
                }
                ret.before = formInput;
            }
        }
        return {};
    }

    function buildFormInput(formInput, readonlyOverride) {
        var id = ID_PREFIX.INPUT + formInput.name;
        var label = formInput.label;
        var readonly = readonlyOverride || formInput.readonly;
        var selectOptions = formInput.selectOptions;
        var initVal = '';

        if (formInput.data !== undefined && formInput.data !== null) {
            initVal = formInput.data;
        } else if (formInput.hasOwnProperty('default')) {
            initVal = formInput.default;
        }

        if (formInput.layout === INPUT_LAYOUT.TABLE) {
            switch (formInput.type) {
                case INPUT_TYPE.SELECT:
                    return createFormTableDisplayDefaultSelectInput(id, label, selectOptions, readonly, initVal);
                default:
                    return createFormTableDisplayTextInput(id, label, readonly, initVal);
            }
        } else {
            switch (formInput.type) {
                case INPUT_TYPE.CHECKBOX:
                    return createFormCheckboxInput(id, label, readonly, initVal);
                case INPUT_TYPE.SELECT:
                    return createFormDefaultDisplaySelectInput(id, label, selectOptions, readonly, initVal);
                case INPUT_TYPE.TABLE:
                    return $('<div id="' + id + '" style="max-height:300px; overflow-y:auto;"/>').append(buildTable(initVal, formInput));
                case INPUT_TYPE.TEXT_SELECT:
                    return createFormTextSelectInput(id, label, selectOptions, readonly, initVal);
                case INPUT_TYPE.READONLY:
                    return $('<span id="' + id + '">' + label + '</span>');
                default:
                    return createFormDefaultTextInput(id, label, readonly, initVal);
            }
        }
    }

    function createFormTableDisplayDefaultSelectInput(id, label, opts, readonly, initVal) {
        return createFormDefaultSelectInput(id, label, opts, readonly, initVal, {label:'width:20%;', input:'display:inline-block;width:77%;'});
    }

    function createFormDefaultDisplaySelectInput(id, label, opts, readonly, initVal) {
        return createFormDefaultSelectInput(id, label, opts, readonly, initVal, {label:'', input:''});
    }

    function createFormDefaultSelectInput(id, label, opts, readonly, initVal, styles) {
        var inputGroup = $('<label for="' + id + '" style="' + styles.label + '">' + label + '</label>'
            + '<select id="' + id + '" class="form-control input-sm" style="' + styles.input + '"'
            + (readonly ? ' disabled' : '') + '>');
        populateSelect(inputGroup.last(), opts, initVal);
        return inputGroup;
    }

    function createFormTextSelectInput(id, label, opts, readonly, initVal) {
        var inputGroup = $('<label for="' + id + '">' + label + '</label>'
            + '<div class="input-group"><div class="input-group-btn">'
            + '<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">'
            + 'Choose<span class="caret"></span></button><ul class="dropdown-menu"></ul></div>'
            + '<input id="' + id + '" type="text" class="form-control"'
            + (readonly ? ' readonly' : '') + '></div>');
        populateTextSelect(inputGroup, opts);
        inputGroup.find('input').val(initVal);
        return inputGroup;
    }

    function createFormCheckboxInput(id, label, readonly, initVal) {
        var inputGroup = $('<div class="checkbox"><label><input id="' + id + '" type="checkbox"'
            + (readonly ? ' disabled' : '') + '>' + label + '</label></div>');
        inputGroup.find('input')[0].checked = initVal;
        return inputGroup;
    }

    function createFormTableDisplayTextInput(id, label, readonly, initVal) {
        return createFormTextInput(id, label, readonly, initVal, {label:'width:20%;', input:'display:inline-block;width:77%;'});
    }

    function createFormDefaultTextInput(id, label, readonly, initVal) {
        return createFormTextInput(id, label, readonly, initVal, {label:'', input:''});
    }

    // req styles for label and input
    function createFormTextInput(id, label, readonly, initVal, styles) {
        var inputGroup = $('<label for="' + id + '" style="' + styles.label + '">' + label + ':</label>'
            + '<input id="' + id + '" type="text" class="form-control" style="' + styles.input + '"'
            + (readonly ? ' readonly' : '') + '>');
        inputGroup.last().val(initVal);
        return inputGroup;
    }

    function addListenersToControl(control, listeners) {
        var i, len, key;
        var keys = Object.keys(listeners);
        var input = control.find('input,select');
        for (i = 0, len = keys.length; i < len; i++) {
            key = keys[i];
            input.on(key, listeners[key]);
        }
    }

    function populateSelect(el, opts, init) {
        var i, len, optEl, opt;
        el.empty();
        if (!opts) {
            return;
        }
        for (i = 0, len = opts.length; i < len; i++) {
            optEl = $('<option/>');
            opt = opts[i];

            if (typeof opt === 'object') {
                optEl.val(opt.key);
                optEl.text(opt.value);
            } else {
                optEl.text(opts[i]);
            }

            el.append(optEl);
        }
        el.val(init);
    }

    function populateTextSelect(inputGroup, opts) {
        var i, len, html, ul, input;
        ul = inputGroup.find('ul');
        ul.empty();
        if (!opts) {
            return;
        }
        html = '';
        input = inputGroup.find('input');
        for (i = 0, len = opts.length; i < len; i++) {
            html += '<li><a href="#">' + opts[i] + '</a></li>';
        }
        ul.append(html);
        ul.find('a').on('click', function(e) {
            e.preventDefault();
            input.val(this.innerHTML);
            input.trigger('change');
        });
    }

    function buildTable(data, tableOpts) {
        var columns, columnKeys, headingRow, c, cLen, column, header;
        var style = tableOpts.css || "margin:0 auto;";
        var table = $('<table/>').css(style);

        columns = tableOpts.columns;
        columnKeys = Object.keys(columns);
        tableOpts.columnKeys = columnKeys;
        headingRow = $('<tr/>');
        table.append(headingRow);
        for (c = 0, cLen = columnKeys.length; c < cLen; c++) {
            column = columns[columnKeys[c]];
            header = $('<th/>').html(column.heading).css(column.css || TD_CSS);
            if (column.sortable) {
                header.on('click', function() {
                    sortTable($(this).closest('table'), this, tableOpts);
                });
            }
            headingRow.append(header);
        }
        createTableRows(table, data, tableOpts);
        return table;
    }

    function sortTable(table, header, tableOpts) {
        // TODO - NEEDS FLESHED OUT
        // var idx = table.find('th').index(header);
        // var key = tableOpts.columnKeys[idx];
        // _data.sort(function(a, b) {
        //     return a[key] - b[key];
        // });
    }

    function createTableRows(table, data, tableOpts) {
        var d, dLen;
        dLen = data.length;
        if (dLen) {
            for (d = 0; d < dLen; d++) {
                addTableRow(table, data[d], tableOpts);
            }
        } else {
            addTableRow(table, null, tableOpts); //start with empty row
        }
    }

    function addTableRowForTableTypeBuilder(dataRow) {
        addTableRow(_modal.find('table'), dataRow, _options);
    }

    function addTableRow(table, dataRow, tableOpts) {
        var c, cLen, key, column, dataVal, inputElement, closeBtn;
        var tr = $('<tr/>').addClass(TR_CLASS);
        var columnKeys = tableOpts.columnKeys;
        var columns = tableOpts.columns;
        for (c = 0, cLen = columnKeys.length; c < cLen; c++) {
            key = columnKeys[c];
            column = columns[key];
            if (dataRow) {
                dataVal = typeof dataRow === OBJECT ? dataRow[key] : dataRow;
            } else if (column.hasOwnProperty('default')) {
                dataVal = column.default;
            } else {
                dataVal = null;
            }

            inputElement = getDataRowInput(column, dataVal);
            inputElement.addClass(key);
            tr.append($('<td/>').append(inputElement).css(column.css || TD_CSS));
        }

        if (!_options.readonly) {
            closeBtn = $('<span/>').addClass('glyphicon glyphicon-remove tab-close-icon');
            closeBtn.click(function () {
                tr.remove();
            });
        }
        table.append(tr);
        tr.append($('<td/>').append(closeBtn));
        tr.find('input').first().focus();
    }

    function getDataRowInput(column, dataVal) {
        var inputElement;
        switch (column.type) {
            case INPUT_TYPE.CHECKBOX:
                inputElement = $('<input/>').prop({type:'checkbox', checked:dataVal});
                break;
            case INPUT_TYPE.READONLY:
                inputElement = $('<span/>').html(dataVal);
                break;
            case INPUT_TYPE.SELECT:
                inputElement = $('<select/>');
                populateSelect(inputElement, column.selectOptions, dataVal);
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
        return _modal.find('tr.' + TR_CLASS);
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
            input = $('#' + ID_PREFIX.INPUT + key);
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

    function getInputValue(key) {
        var el = _modal.find('#' + ID_PREFIX.INPUT + key);
        if (el.length) {
            return el.val();
        }
    }

    function setInputValue(key, value) {
        var el = _modal.find('#' + ID_PREFIX.INPUT + key);
        if (el.length) {
            el.val(value);
        }
    }

    function makeModalDraggable() {
        var realPosX, realPosY;
        var $doc = $(document);
        var docWidth = $doc.width();
        var docHeight = $doc.height();
        var content = _modal.find('.modal-content');
        var modalWidth = content.outerWidth();
        var modalHeight = content.outerHeight();
        var marginTop = _modal.find('.modal-dialog').css('margin-top');
        var shouldBeDraggable = _options.draggable && docHeight > modalHeight;
        marginTop = marginTop.substring(0, marginTop.indexOf('px'));
        marginTop = parseInt(marginTop);
        realPosX = docWidth - modalWidth;
        realPosY = docHeight - modalHeight - marginTop * 2;
        _modal.draggable({
            handle: '.modal-header',
            containment: [-realPosX, -marginTop, realPosX, realPosY]
        });
        _modal.draggable(shouldBeDraggable ? 'enable' : 'disable');
    }

    function positionDropdownMenus() {
        _modal.find('.dropdown-toggle').on('click', function () {
            var contentOffset;
            var button = $(this);
            var offset = button.offset();
            var dropDownTop = offset.top + button.outerHeight();
            var dropDownLeft = offset.left;
            var content = button.closest('.modal-content');
            if (content.length) {
                contentOffset = content.offset();
                dropDownTop -= contentOffset.top;
                dropDownLeft -= contentOffset.left;
            }

            button.parent().find('ul').css({position: 'fixed', top: dropDownTop + 'px', left: dropDownLeft + 'px'});
        });
    }

    function closeModal() {
        if (typeof _options.onClose === FUNCTION) {
            _options.onClose();
        }
        _modal.remove();
        _modal = null;
    }

    function checkAll(state, queryStr) {
        $(queryStr).filter(':visible').not('.exclude').prop('checked', state).change();
    }

    return {
        getInputValue: getInputValue,
        setInputValue: setInputValue,
        openBuilderModal: openBuilderModal,
        populateSelect: populateSelect,
        populateTextSelect: populateTextSelect,
        toggle: toggle,
        BOOTSTRAP_TYPE: BOOTSTRAP_TYPE,
        DISPLAY_TYPE: DISPLAY_TYPE,
        ID_PREFIX: ID_PREFIX,
        INPUT_LAYOUT: INPUT_LAYOUT,
        INPUT_TYPE: INPUT_TYPE,
        MODAL_SIZE: MODAL_SIZE
    };

})(jQuery);