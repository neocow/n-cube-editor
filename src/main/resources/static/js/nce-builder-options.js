var NCEBuilderOptions = (function () {
    /*
     * All methods have valid options of:
     * readonly     - self-explanatory
     * afterSave    - callback to fire on save
     * onClose      - callback to fire when the modal closes (fires after save if saved)
     */

    /*
     * additional required options:
     *  columnSelectList
     */
    function filterData(opts) {
        return {
            title: 'Filter Data',
            instructionsTitle: 'Instructions - Filter Data',
            instructionsText: 'Select filters to apply to cell data for ncube.',
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            canAddRemoveRows: true,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            columns: {
                isApplied: {
                    heading: 'Apply',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    default: true
                },
                column: {
                    heading: 'Column',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    selectOptions: opts.columnSelectList
                },
                comparator: {
                    heading: 'Comparator',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    selectOptions: FILTER_COMPARATOR_LIST,
                    default: FILTER_COMPARATOR_LIST[0]
                },
                expressionValue: {
                    heading: 'Comparison Value',
                    type: FormBuilder.INPUT_TYPE.TEXT
                },
                isIncludeAll: {
                    heading: 'Include Empty Cells',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    default: true
                }
            }
        };
    }

    /*
     * additional required options:
     *  name
     *  type
     */
    function metaProperties(opts) {
        return {
            title: 'Metaproperties - ' + opts.name,
            instructionsTitle: 'Instructions - Metaproperties',
            instructionsText: 'Add custom properties for this ' + opts.type + '.',
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            canAddRemoveRows: true,
            size: FormBuilder.MODAL_SIZE.LARGE,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            columns: {
                key: {
                    heading: 'Key',
                    type: FormBuilder.INPUT_TYPE.TEXT
                },
                dataType: {
                    heading: 'Type',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    default: 'string',
                    selectOptions: METAPROPERTIES.DATA_TYPE_LIST
                },
                isUrl: {
                    heading: 'Url',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX
                },
                isCached: {
                    heading: 'Cached',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX
                },
                value: {
                    heading: 'Value',
                    type: FormBuilder.INPUT_TYPE.TEXT
                },
                popout: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    css: {},
                    default: 'Pop Out',
                    listeners: {
                        click: function(e) {
                            e.preventDefault();
                            opts.onPopOut($(e.target).closest('tr').find('.value'));
                        }
                    }
                }
            }
        };
    }

    /*
     * additional required options:
     *  appId
     *  appSelectList
     *  populateVersionFunc
     */
    function copyBranch(opts) {
        var appId = opts.appId;
        return {
            title: 'Copy ' + appId.app + ' ' + appId.version + '-' + appId.status + ' ' + appId.branch,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Copy Branch',
            formInputs: {
                app: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New app',
                    selectOptions: opts.appSelectList,
                    data: appId.app,
                    listeners: {
                        change: function() {
                            FormBuilder.findElementByKey('version').trigger('populate');
                        }
                    }
                },
                version: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New version',
                    data: appId.version,
                    listeners: {
                        populate: function() {
                            var app = FormBuilder.getInputValue('app') || appId.app;
                            var versions = opts.populateVersionFunc(app, STATUS.SNAPSHOT);
                            FormBuilder.populateTextSelect($(this).parent(), versions);
                        }
                    }
                },
                branch: {
                    label: 'New branch'
                },
                copyHistory: {
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    label: 'Copy branch with history'
                }
            }
        };
    }

    /*
     * additional required options:
     *  appId
     */
    function deleteBranch(opts) {
        var appId = opts.appId;
        return {
            title: 'Delete ' + appId.app + ' ' + appId.version + '-' + appId.status + ' ' + appId.branch,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            size: FormBuilder.MODAL_SIZE.SMALL,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Delete Branch',
            formInputs: {
                surety: {
                    type: FormBuilder.INPUT_TYPE.READONLY,
                    label: 'Are you sure?'
                }
            }
        };
    }

    /*
     * additional required options:
     *  appId
     *  cubeName
     *  appSelectList
     *  populateVersionFunc
     *  populateBranchFunc
     */
    function copyCube(opts) {
        var appId = opts.appId;
        return {
            title: 'Duplicate - ' + opts.cubeName,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Duplicate',
            formInputs: {
                app: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New app',
                    selectOptions: opts.appSelectList,
                    data: appId.app,
                    listeners: {
                        change: function() {
                            FormBuilder.findElementByKey('version').trigger('populate');
                            FormBuilder.findElementByKey('branch').trigger('populate');
                        }
                    }
                },
                version: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New version',
                    data: appId.version,
                    listeners: {
                        change: function() {
                            FormBuilder.findElementByKey('branch').trigger('populate');
                        },
                        populate: function() {
                            var app = FormBuilder.getInputValue('app') || appId.app;
                            var versions = opts.populateVersionFunc(app, STATUS.SNAPSHOT);
                            FormBuilder.populateTextSelect($(this).parent(), versions);
                        }
                    }
                },
                branch: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New branch',
                    data: appId.branch,
                    listeners: {
                        populate: function() {
                            var app = FormBuilder.getInputValue('app');
                            var version = FormBuilder.getInputValue('version');
                            var newAppId = app ? {
                                app: app,
                                version: version,
                                status: STATUS.SNAPSHOT,
                                branch: 'HEAD'
                            } : appId;
                            var branches = opts.populateBranchFunc(newAppId);
                            FormBuilder.populateTextSelect($(this).parent(), branches);
                        }
                    }
                },
                cubeName: {
                    label: 'New n-cube name',
                    data: opts.cubeName
                }
            }
        };
    }

    /*
     * additional required options:
     *  appSelectList
     *  populateVersionFunc
     *  populateCubeFunc
     *  populateAxisFunc
     */
    function addAxis(opts) {
        return {
            title: 'Add Axis',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Add Axis',
            formInputs: {
                name: {
                    label: 'Axis name'
                },
                isRef: {
                    label: 'Reference Axis',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    listeners: {
                        change: function() {
                            FormBuilder.toggle('refSection');
                            FormBuilder.toggle('hasTransform');
                            FormBuilder.toggle('transformSection', false);
                        }
                    }
                },
                refSection: {
                    label: 'Reference Axis',
                    type: FormBuilder.INPUT_TYPE.SECTION,
                    hidden: true,
                    formInputs: {
                        refApp: {
                            label: 'Application',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            selectOptions: opts.appSelectList,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('refVer').trigger('populate');
                                    FormBuilder.findElementByKey('refBranch').trigger('populate');
                                    FormBuilder.findElementByKey('refCube').trigger('populate');
                                    FormBuilder.findElementByKey('refAxis').trigger('populate');
                                }
                            }
                        },
                        refVer: {
                            label: 'Version',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('refBranch').trigger('populate');
                                    FormBuilder.findElementByKey('refCube').trigger('populate');
                                    FormBuilder.findElementByKey('refAxis').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.getInputValue('refApp');
                                    var versions = app ? opts.populateVersionFunc(app) : null;
                                    FormBuilder.populateSelect($(this), versions);
                                }
                            }
                        },
                        refBranch: {
                            label: 'Branch',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('refCube').trigger('populate');
                                    FormBuilder.findElementByKey('refAxis').trigger('populate');
                                },
                                populate: function() {
                                    var branches, splitVer;
                                    var app = FormBuilder.getInputValue('refApp');
                                    var version = FormBuilder.getInputValue('refVer');
                                    if (app && version) {
                                        splitVer = version.split('-');
                                        branches = opts.populateBranchFunc(appIdFrom(app, splitVer[0], splitVer[1], 'HEAD'));
                                    }
                                    FormBuilder.populateSelect($(this), branches);
                                }
                            }
                        },
                        refCube: {
                            label: 'Cube',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('refAxis').trigger('populate');
                                },
                                populate: function() {
                                    var cubes, splitVer;
                                    var app = FormBuilder.getInputValue('refApp');
                                    var version = FormBuilder.getInputValue('refVer');
                                    var branch = FormBuilder.getInputValue('refBranch');
                                    if (app && version && branch) {
                                        splitVer = version.split('-');
                                        cubes = opts.populateCubeFunc(appIdFrom(app, splitVer[0], splitVer[1], branch));
                                    }
                                    FormBuilder.populateSelect($(this), cubes);
                                }
                            }
                        },
                        refAxis: {
                            label: 'Axis',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    var axis, splitVer;
                                    var app = FormBuilder.getInputValue('refApp');
                                    var version = FormBuilder.getInputValue('refVer');
                                    var branch = FormBuilder.getInputValue('refBranch');
                                    var cube = FormBuilder.getInputValue('refCube');
                                    if (app && version && branch && cube) {
                                        splitVer = version.split('-');
                                        axis = opts.populateAxisFunc(appIdFrom(app, splitVer[0], splitVer[1], branch), cube, $(this).val());
                                        FormBuilder.setInputValue('type', axis.type);
                                        FormBuilder.setInputValue('valueType', axis.valueType);
                                    }
                                },
                                populate: function() {
                                    var axisNames, splitVer;
                                    var app = FormBuilder.getInputValue('refApp');
                                    var version = FormBuilder.getInputValue('refVer');
                                    var branch = FormBuilder.getInputValue('refBranch');
                                    var cube = FormBuilder.getInputValue('refCube');
                                    if (app && version && branch && cube) {
                                        splitVer = version.split('-');
                                        axisNames = opts.populateAxisFunc(appIdFrom(app, splitVer[0], splitVer[1], branch), cube);
                                    }
                                    FormBuilder.populateSelect($(this), axisNames);
                                }
                            }
                        }
                    }
                },
                hasTransform: {
                    label: 'Transform',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    hidden: true,
                    listeners: {
                        change: function() {
                            FormBuilder.toggle('transformSection');
                        }
                    }
                },
                transformSection: {
                    label: 'Transform',
                    type: FormBuilder.INPUT_TYPE.SECTION,
                    hidden: true,
                    formInputs: {
                        transApp: {
                            label: 'Application',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            selectOptions: opts.appSelectList,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('transVer').trigger('populate');
                                    FormBuilder.findElementByKey('transBranch').trigger('populate');
                                    FormBuilder.findElementByKey('transCube').trigger('populate');
                                }
                            }
                        },
                        transVer: {
                            label: 'Version',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('transBranch').trigger('populate');
                                    FormBuilder.findElementByKey('transCube').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.getInputValue('transApp');
                                    var versions = app ? opts.populateVersionFunc(app) : null;
                                    FormBuilder.populateSelect($(this), versions);
                                }
                            }
                        },
                        transBranch: {
                            label: 'Branch',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                change: function() {
                                    FormBuilder.findElementByKey('transCube').trigger('populate');
                                },
                                populate: function() {
                                    var branches, splitVer;
                                    var app = FormBuilder.getInputValue('transApp');
                                    var version = FormBuilder.getInputValue('transVer');
                                    if (app && version) {
                                        splitVer = version.split('-');
                                        branches = opts.populateBranchFunc(appIdFrom(app, splitVer[0], splitVer[1], 'HEAD'))
                                    }
                                    FormBuilder.populateSelect($(this), branches);
                                }
                            }
                        },
                        transCube: {
                            label: 'Cube',
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            listeners: {
                                populate: function() {
                                    var cubes, splitVer;
                                    var app = FormBuilder.getInputValue('transApp');
                                    var version = FormBuilder.getInputValue('transVer');
                                    var branch = FormBuilder.getInputValue('transBranch');
                                    if (app && version && branch) {
                                        splitVer = version.split('-');
                                        cubes = opts.populateCubeFunc(appIdFrom(app, splitVer[0], splitVer[1], branch));
                                    }
                                    FormBuilder.populateSelect($(this), cubes);
                                }
                            }
                        }
                    }
                },
                type: {
                    label: 'Axis type (Discrete, Range, Set, Nearest, Rule)',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    selectOptions: AXIS_TYPE_LIST.TYPE,
                    data: 'DISCRETE',
                    listeners: {
                        change: function() {
                            FormBuilder.findElementByKey('valueType').trigger('populate');
                        }
                    }
                },
                valueType: {
                    label: 'Column data type (String, Date, Integer, ...)',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    data: AXIS_SUBTYPES.STRING,
                    listeners: {
                        populate: function() {
                            var axisType = FormBuilder.getInputValue('type');
                            var isRuleAxis = axisType === 'RULE';
                            var valueTypes = isRuleAxis ? AXIS_TYPE_LIST.RULE_SUBTYPE : AXIS_TYPE_LIST.GENERAL_SUBTYPE;
                            var initVal = isRuleAxis ? 'EXPRESSION' : 'STRING';
                            FormBuilder.populateSelect($(this), valueTypes, initVal);
                        }
                    }
                }
            }
        };
    }

    /*
     * additional required options:
     *  axisName
     */
    function deleteAxis(opts) {
        return {
            title: 'Delete Axis - ' + opts.axisName,
            instructionsTitle: '',
            instructionsText: 'Note: All cells will be cleared when an axis is deleted.',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Delete Axis',
            formInputs: {
                name: {
                    label: 'Axis to delete',
                    readonly: true,
                    data: opts.axisName
                }
            }
        };
    }

    /*
     * additional required options:
     *  axis
     */
    function updateAxis(opts) {
        var axis = opts.axis;
        var metaProps = axis.metaProps || {};
        return {
            title: 'Update Axis - ' + axis.name,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            formInputs: {
                name: {
                    label: 'Axis name',
                    data: axis.name
                },
                refSection: {
                    label: 'Reference Axis',
                    type: FormBuilder.INPUT_TYPE.SECTION,
                    hidden: !axis.isRef,
                    readonly: true,
                    formInputs: {
                        refApp: {
                            label: 'Application',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            data: metaProps.referenceApp
                        },
                        refVer: {
                            label: 'Version',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            data: metaProps.referenceVersion + '-' + metaProps.referenceStatus
                        },
                        refBranch: {
                            label: 'Branch',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            data: metaProps.referenceBranch
                        },
                        refCube: {
                            label: 'Cube',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            data: metaProps.referenceCubeName
                        },
                        refAxis: {
                            label: 'Axis',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            data: metaProps.referenceAxisName
                        }
                    }
                },
                transformSection: {
                    label: 'Transform',
                    type: FormBuilder.INPUT_TYPE.SECTION,
                    hidden: !metaProps.transformApp,
                    readonly: true,
                    formInputs: {
                        transApp: {
                            label: 'Application',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformApp
                        },
                        transVer: {
                            label: 'Version',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformVersion + '-' + metaProps.transformStatus
                        },
                        transBranch: {
                            label: 'Branch',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformBranch
                        },
                        transCube: {
                            label: 'Cube',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformCubeName
                        }
                    }
                },
                type: {
                    label: 'Axis type (Discrete, Range, Set, Nearest, Rule)',
                    readonly: true,
                    data: axis.type.name
                },
                valueType: {
                    label: 'Column data type (String, Date, Integer, ...)',
                    readonly: true,
                    data: axis.valueType.name
                },
                default: {
                    label: 'Has Default Column',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    hidden: axis.type.name === 'NEAREST',
                    data: axis.defaultCol,
                    default: false
                },
                sorted: {
                    label: 'Sorted',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    readonly: axis.isRef,
                    hidden: ['RULE','NEAREST'].indexOf(axis.type.name) > -1,
                    data: !axis.preferredOrder,
                    default: false
                },
                fireAll: {
                    label: 'Fire all matching (versus first matching)',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    hidden: axis.type.name !== 'RULE',
                    data: axis.fireAll,
                    default: false
                }
            }
        };
    }

    /*
     * additional required options:
     *  cubeName
     *  refClick
     */
    function outboundRefs(opts) {
        return {
            title: 'Outbound refs of - ' + opts.cubeName,
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            size: FormBuilder.MODAL_SIZE.MEDIUM,
            readonly: true,
            onClose: opts.onClose,
            closeButtonText: 'Close',
            hasFilter: true,
            css: {},
            columns: {
                refCube: {
                    type: FormBuilder.INPUT_TYPE.LINK,
                    css: {},
                    listeners: {
                        click: opts.refClick
                    }
                }
            }
        };
    }

    /*
     * additional required options:
     *  cubeName
     */
    function requiredScope(opts) {
        return {
            title: 'Scope for - ' + opts.cubeName,
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            size: FormBuilder.MODAL_SIZE.MEDIUM,
            readonly: true,
            onClose: opts.onClose,
            closeButtonText: 'Close',
            hasFilter: true,
            css: {},
            columns: {
                scopeKey: {
                    type: FormBuilder.INPUT_TYPE.READONLY,
                    css: {}
                }
            }
        };
    }

    /*
     * additional required options:
     *  leftAppId
     *  leftCube
     *  rightAppId
     *  rightCube
     *  leftDisabled
     *  appSelectList
     *  populateVersionFunc
     *  populateBranchFunc
     *  populateCubeFunc
     */
    function globalComparator(opts) {
        return {
            title: 'Compare Any Two Cubes',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Compare',
            formInputs: {
                appRow: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: { margin: '0 auto', width: '90%'},
                    data: [{
                        appLabel: 'Application',
                        leftApp: [opts.leftAppId.app],
                        rightApp: [opts.rightAppId.app]
                    }],
                    columns: {
                        appLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '20%' }
                        },
                        leftApp: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            selectOptions: opts.appSelectList,
                            readonly: opts.leftDisabled,
                            css: { width: '40%' },
                            listeners: {
                                load: function() { $(this).trigger('change'); },
                                change: function() {
                                    FormBuilder.findTableElementsByKey('leftVersion').trigger('populate');
                                    FormBuilder.findTableElementsByKey('leftBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('leftCube').trigger('populate');
                                }
                            }
                        },
                        rightApp: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            selectOptions: opts.appSelectList,
                            css: { width: '40%' },
                            listeners: {
                                load: function() { $(this).trigger('change'); },
                                change: function() {
                                    FormBuilder.findTableElementsByKey('rightVersion').trigger('populate');
                                    FormBuilder.findTableElementsByKey('rightBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('rightCube').trigger('populate');
                                }
                            }
                        }
                    }
                },
                versionRow: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: { margin: '0 auto', width: '90%'},
                    data: [{
                        versionLabel: 'Version',
                        leftVersion: [opts.leftAppId.version + '-' + opts.rightAppId.status],
                        rightVersion: [opts.rightAppId.version + '-' + opts.rightAppId.status]
                    }],
                    columns: {
                        versionLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '20%' }
                        },
                        leftVersion: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            readonly: opts.leftDisabled,
                            css: { width: '40%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('leftBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('leftCube').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('leftApp').val();
                                    var versions = opts.populateVersionFunc(app || opts.leftAppId.app);
                                    var def = app ? '' : opts.leftAppId.version + '-' + opts.leftAppId.status;
                                    FormBuilder.populateSelect($(this), versions, def);
                                }
                            }
                        },
                        rightVersion: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '40%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('rightBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('rightCube').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('rightApp').val();
                                    var versions = opts.populateVersionFunc(app || opts.rightAppId.app);
                                    var def = app ? '' : opts.rightAppId.version + '-' + opts.rightAppId.status;
                                    FormBuilder.populateSelect($(this), versions, def);
                                }
                            }
                        }
                    }
                },
                branchRow: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: { margin: '0 auto', width: '90%'},
                    data: [{
                        branchLabel: 'Branch',
                        leftBranch: [opts.leftAppId.branch],
                        rightBranch: [opts.rightAppId.branch]
                    }],
                    columns: {
                        branchLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '20%' }
                        },
                        leftBranch: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            readonly: opts.leftDisabled,
                            css: { width: '40%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('leftCube').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('leftApp').val();
                                    var version = FormBuilder.findTableElementsByKey('leftVersion').val();
                                    var verstat = version ? version.split('-') : version;
                                    var newAppId = app && verstat ? appIdFrom(app, verstat[0], verstat[1], 'HEAD') : opts.leftAppId;
                                    var branches = opts.populateBranchFunc(newAppId);
                                    FormBuilder.populateSelect($(this), branches, newAppId.branch);
                                }
                            }
                        },
                        rightBranch: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '40%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('rightCube').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('rightApp').val();
                                    var version = FormBuilder.findTableElementsByKey('rightVersion').val();
                                    var verstat = version ? version.split('-') : version;
                                    var newAppId = app && verstat ? appIdFrom(app, verstat[0], verstat[1], 'HEAD') : opts.rightAppId;
                                    var branches = opts.populateBranchFunc(newAppId);
                                    FormBuilder.populateSelect($(this), branches, newAppId.branch);
                                }
                            }
                        }
                    }
                },
                cubeRow: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: { margin: '0 auto', width: '90%'},
                    data: [{
                        cubeLabel: 'Cube',
                        leftCube: [opts.leftAppId.cube],
                        rightCube: [opts.rightAppId.cube]
                    }],
                    columns: {
                        cubeLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '20%' }
                        },
                        leftCube: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            readonly: opts.leftDisabled,
                            css: { width: '40%' },
                            listeners: {
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('leftApp').val();
                                    var version = FormBuilder.findTableElementsByKey('leftVersion').val();
                                    var branch = FormBuilder.findTableElementsByKey('leftBranch').val();
                                    var verstat = version ? version.split('-') : version;
                                    var newAppId = app && verstat && branch ? appIdFrom(app, verstat[0], verstat[1], branch) : opts.leftAppId;
                                    var cubes = opts.populateCubeFunc(newAppId);
                                    FormBuilder.populateSelect($(this), cubes, opts.leftCube);
                                }
                            }
                        },
                        rightCube: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '40%' },
                            listeners: {
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('rightApp').val();
                                    var version = FormBuilder.findTableElementsByKey('rightVersion').val();
                                    var branch = FormBuilder.findTableElementsByKey('rightBranch').val();
                                    var verstat = version ? version.split('-') : version;
                                    var newAppId = app && verstat && branch ? appIdFrom(app, verstat[0], verstat[1], branch) : opts.rightAppId;
                                    var cubes = opts.populateCubeFunc(newAppId);
                                    FormBuilder.populateSelect($(this), cubes, opts.rightCube);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    /*
     * additional required options:
     *  onCreate
     *  branchNames
     *  onBranchClick
     */
    function selectBranch(opts) {
        return {
            title: 'Select or Create Branch',
            instructionsTitle: '',
            instructionsText: 'Before a change can be made, a <b>branch</b> must be selected or created.',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            onClose: opts.onClose,
            hasFilter: true,
            formInputs: {
                name: {
                    label: 'New branch name',
                    buttonLabel: 'Create',
                    buttonClick: function() {
                        opts.onCreate(FormBuilder.getInputValue('name'));
                    }
                },
                selectBranchSection: {
                    type: FormBuilder.INPUT_TYPE.SECTION,
                    label: 'Select existing branch',
                    sectionType: FormBuilder.BOOTSTRAP_TYPE.PRIMARY,
                    formInputs: {
                        branchList: {
                            type: FormBuilder.INPUT_TYPE.TABLE,
                            css: {},
                            data: opts.branchNames,
                            columns: {
                                branchName: {
                                    type: FormBuilder.INPUT_TYPE.BUTTON,
                                    css: {},
                                    listeners: {
                                        click: function(e) {
                                            opts.onBranchClick(e.target.textContent);
                                            FormBuilder.closeBuilderModal();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    /*
     * additional required options:
     *  app
     *  version
     */
    function createSnapshotFromRelease(opts) {
        var setNextVer = function(e, versionPart) {
            var nextVersion = getNextVersion(opts.version, versionPart);
            FormBuilder.setInputValue('newVersion', nextVersion);
            e.preventDefault();
        };
        return {
            title: 'Copy ' + opts.app + ' ' + opts.version + '-' + STATUS.RELEASE,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Create',
            formInputs: {
                questionLabel: {
                    type: FormBuilder.INPUT_TYPE.READONLY,
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'What kind of version is this?'
                },
                major: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Major',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.MAJOR);
                        }
                    }
                },
                minor: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Minor',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.MINOR);
                        }
                    }
                },
                patch: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Patch',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.PATCH);
                        }
                    }
                },
                newVersion: {
                    label: 'New SNAPSHOT version',
                    readonly: true,
                    placeholder: 'Version number major.minor.patch'
                }
            }
        };
    }

    /*
     * additional required options:
     *  app
     *  version
     *  initProgress
     *  initProgressInfo
     *  optional - newVersion
     */
    function releaseVersion(opts) {
        var setNextVer = function(e, versionPart) {
            var nextVersion = getNextVersion(opts.version, versionPart);
            FormBuilder.setInputValue('newVersion', nextVersion);
            e.preventDefault();
        };
        return {
            updateProgress: function(progress, msg) {
                FormBuilder.setInputValue('progressBar', progress);
                FormBuilder.setInputValue('progressInfo', msg);
            },
            toggleReleaseButton: function(state) {
                $('.form-builder-save').toggle(state);
            },
            title: 'Release ' + opts.app + ' ' + opts.version + '-' + STATUS.SNAPSHOT,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Release',
            closeAfterSave: false,
            formInputs: {
                questionLabel: {
                    type: FormBuilder.INPUT_TYPE.READONLY,
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'What kind of version is this?'
                },
                major: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Major',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.MAJOR);
                        }
                    }
                },
                minor: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Minor',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.MINOR);
                        }
                    }
                },
                patch: {
                    type: FormBuilder.INPUT_TYPE.BUTTON,
                    buttonClass: 'btn-primary',
                    layout: FormBuilder.INPUT_LAYOUT.INLINE,
                    label: 'Patch',
                    listeners: {
                        click: function(e) {
                            setNextVer(e, VERSION.PATCH);
                        }
                    }
                },
                newVersion: {
                    label: 'New SNAPSHOT version',
                    readonly: true,
                    placeholder: 'Version number major.minor.patch',
                    data: opts.newVersion
                },
                progressBar: {
                    type: FormBuilder.INPUT_TYPE.PROGRESS,
                    progressClass: 'progress-bar-striped',
                    data: opts.initProgress
                },
                progressInfo: {
                    type: FormBuilder.INPUT_TYPE.READONLY,
                    label: opts.initProgressInfo
                }
            }
        };
    }

    /*
     * additional required options:
     *  app
     *  version
     */
    function changeSnapshotVersion(opts) {
        return {
            title: 'Change ' + opts.app + ' ' + opts.version,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            onClose: opts.onClose,
            saveButtonText: 'Change',
            formInputs: {
                newVersion: {
                    label: 'Change to',
                    placeholder: 'Version number major.minor.patch'
                }
            }
        };
    }

    /*
     * additional required options:
     *  refAxList
     *  isTransform
     *  appSelectList
     *  populateVersionFunc
     *  populateBranchFunc
     *  populateCubeFunc
     */
    function referenceAxisUpdater(opts) {
        var ret = {
            title: 'Reference Axis Batch Updater - Destination',
            instructionsTitle: 'Instructions - Update Reference Axis Destination',
            instructionsText: 'Update the reference axis destination properties of checked reference axes. Only selected fields will change, blank fields will not be changed.',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            size: FormBuilder.MODAL_SIZE.XL,
            hasFilter: true,
            hasSelectAllNone: true,
            readonly: opts.readonly,
            afterSave: opts.afterSave,
            data: {
                refAxList: opts.refAxList,
                isTransform: opts.isTransform
            },
            closeAfterSave: false,
            saveButtonText: 'Update',
            closeButtonText: 'Close',
            formInputs: {
                updateRow: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: { margin: '0 auto', width: '90%'},
                    data: [{
                        appLabel: 'App',
                        versionLabel: 'Version',
                        branchLabel: 'Branch',
                        cubeLabel: 'Cube',
                        axisLabel: 'Axis'
                    }],
                    columns: {
                        appLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '5%' }
                        },
                        updateApp: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            selectOptions: opts.appSelectList,
                            css: { width: '15%' },
                            listeners: {
                                load: function() { $(this).trigger('change'); },
                                change: function() {
                                    FormBuilder.findTableElementsByKey('updateVersion').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateCube').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateAxis').trigger('populate');
                                }
                            }
                        },
                        versionLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '5%' }
                        },
                        updateVersion: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '15%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('updateBranch').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateCube').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateAxis').trigger('populate');
                                },
                                populate: function() {
                                    var app = FormBuilder.findTableElementsByKey('updateApp').val();
                                    var versions = app ? opts.populateVersionFunc(app) : null;
                                    FormBuilder.populateSelect($(this), versions);
                                }
                            }
                        },
                        branchLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '5%' }
                        },
                        updateBranch: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '15%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('updateCube').trigger('populate');
                                    FormBuilder.findTableElementsByKey('updateAxis').trigger('populate');
                                },
                                populate: function() {
                                    var branches, splitVer;
                                    var app = FormBuilder.findTableElementsByKey('updateApp').val();
                                    var version = FormBuilder.findTableElementsByKey('updateVersion').val();
                                    if (app && version) {
                                        splitVer = version.split('-');
                                        branches = opts.populateBranchFunc(appIdFrom(app, splitVer[0], splitVer[1], 'HEAD'));
                                    }
                                    FormBuilder.populateSelect($(this), branches);
                                }
                            }
                        },
                        cubeLabel: {
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: { width: '5%' }
                        },
                        updateCube: {
                            type: FormBuilder.INPUT_TYPE.SELECT,
                            css: { width: '15%' },
                            listeners: {
                                change: function() {
                                    FormBuilder.findTableElementsByKey('updateAxis').trigger('populate');
                                },
                                populate: function() {
                                    var cubes;
                                    var app = FormBuilder.findTableElementsByKey('updateApp').val();
                                    var version = FormBuilder.findTableElementsByKey('updateVersion').val();
                                    var branch = FormBuilder.findTableElementsByKey('updateBranch').val();
                                    var verstat = version ? version.split('-') : version;
                                    if (app && verstat && branch) {
                                        cubes = opts.populateCubeFunc(appIdFrom(app, verstat[0], verstat[1], branch));
                                    }
                                    FormBuilder.populateSelect($(this), cubes);
                                }
                            }
                        }
                    }
                },
                refAxTable: {
                    type: FormBuilder.INPUT_TYPE.TABLE,
                    css: {margin: '0 auto', width: '95%', 'table-layout':'fixed'},
                    data: opts.refAxList,
                    columns: {
                        isApplied: {
                            heading: 'Apply',
                            type: FormBuilder.INPUT_TYPE.CHECKBOX,
                            css: {width: '4%', overflow:'hidden', 'text-overflow':'ellipsis'}
                        },
                        srcCubeName: {
                            heading: 'Source Cube',
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: {width: '32%', overflow:'hidden', 'text-overflow':'ellipsis', 'background-color':'white'}
                        },
                        srcAxisName: {
                            heading: 'Source Axis',
                            type: FormBuilder.INPUT_TYPE.READONLY,
                            css: {width: '10%', overflow:'hidden', 'text-overflow':'ellipsis'}
                        }
                    }
                }
            }
        };

        var table = ret.formInputs.refAxTable.columns;
        var updateRow = ret.formInputs.updateRow.columns;
        var prefix, tableHeading;
        if (opts.isTransform) {
            prefix = 'transform';
            tableHeading = 'Transform';
            ret.footerButtons = {
                breakTransform: {
                    buttonClass: 'btn-primary',
                    label: 'Break Transform',
                    action: function() {
                        FormBuilder.setDataValue('breakTransform', true);
                        FormBuilder.closeBuilderModal(true);
                    }
                }
            };
        } else {
            prefix = 'dest';
            tableHeading = 'Ref';
            updateRow.axisLabel = {
                type: FormBuilder.INPUT_TYPE.READONLY,
                css: { width: '5%', overflow:'hidden', 'text-overflow':'ellipsis' }
            };
            updateRow.updateAxis = {
                type: FormBuilder.INPUT_TYPE.SELECT,
                css: { width: '15%' },
                listeners: {
                    populate: function() {
                        var axisNames, splitVer;
                        var app = FormBuilder.findTableElementsByKey('updateApp').val();
                        var version = FormBuilder.findTableElementsByKey('updateVersion').val();
                        var branch = FormBuilder.findTableElementsByKey('updateBranch').val();
                        var cube = FormBuilder.findTableElementsByKey('updateCube').val();
                        if (app && version && branch && cube) {
                            splitVer = version.split('-');
                            axisNames = opts.populateAxisFunc(appIdFrom(app, splitVer[0], splitVer[1], branch), cube);
                        }
                        FormBuilder.populateSelect($(this), axisNames);
                    }
                }
            };
        }

        table[prefix + 'App'] = {
            heading: tableHeading + ' App',
            type: FormBuilder.INPUT_TYPE.READONLY,
            css: {width: '10%', overflow:'hidden', 'text-overflow':'ellipsis'}
        };
        table[prefix + 'Version'] = {
            heading: 'Version',
            type: FormBuilder.INPUT_TYPE.READONLY,
            css: {width: '5%', overflow:'hidden', 'text-overflow':'ellipsis'}
        };
        table[prefix + 'Branch'] = {
            heading: 'Branch',
            type: FormBuilder.INPUT_TYPE.READONLY,
            css: {width: '10%', overflow:'hidden', 'text-overflow':'ellipsis'}
        };
        table[prefix + 'CubeName'] = {
            heading: 'Cube',
            type: FormBuilder.INPUT_TYPE.READONLY,
            css: {width: '16%', overflow:'hidden', 'text-overflow':'ellipsis'}
        };
        if (!opts.isTransform) {
            table.destAxisName = {
                heading: 'Axis',
                type: FormBuilder.INPUT_TYPE.READONLY,
                css: {width: '13%', overflow:'hidden', 'text-overflow':'ellipsis'}
            };
        }

        return ret;
    }

    return {
        filterData: filterData,
        metaProperties: metaProperties,
        copyBranch: copyBranch,
        deleteBranch: deleteBranch,
        copyCube: copyCube,
        addAxis: addAxis,
        deleteAxis: deleteAxis,
        updateAxis: updateAxis,
        outboundRefs: outboundRefs,
        requiredScope: requiredScope,
        globalComparator: globalComparator,
        selectBranch: selectBranch,
        createSnapshotFromRelease: createSnapshotFromRelease,
        changeSnapshotVersion: changeSnapshotVersion,
        releaseVersion: releaseVersion,
        referenceAxisUpdater: referenceAxisUpdater
    };
})(jQuery);