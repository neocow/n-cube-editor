var NCEBuilderOptions = (function () {
    /*
     * All methods have valid options of:
     * readonly     -   self-explanatory
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
                }
            }
        };
    }

    /*
     * additional required options:
     *  appId
     *  cubeName
     *  appSelectList
     *  populateVersionDropdownFunc
     *  populateBranchDropdownFunc
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
                            $('#' + FormBuilder.ID_PREFIX + 'version').trigger('populate');
                            $('#' + FormBuilder.ID_PREFIX + 'branch').trigger('populate');
                        }
                    }
                },
                version: {
                    type: FormBuilder.INPUT_TYPE.TEXT_SELECT,
                    label: 'New version',
                    data: appId.version,
                    listeners: {
                        change: function() {
                            $('#' + FormBuilder.ID_PREFIX + 'branch').trigger('populate');
                        },
                        populate: function() {
                            var appSelect = $('#' + FormBuilder.ID_PREFIX + 'app');
                            var app = appSelect.length ? appSelect.val() : appId.app;
                            var versions = opts.populateVersionDropdownFunc(app);
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
                            var appSelect = $('#' + FormBuilder.ID_PREFIX + 'app');
                            var versionSelect = $('#' + FormBuilder.ID_PREFIX + 'version');
                            var newAppId = appSelect.length ? {
                                app: appSelect.val(),
                                version: versionSelect.val(),
                                status: STATUS.SNAPSHOT,
                                branch: 'HEAD'
                            } : appId;
                            var branches = opts.populateBranchDropdownFunc(newAppId);
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
                refSectionStart: {
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
                            data: metaProps.referenceVersion
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
                transformSectionStart: {
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
                            data: metaProps.transformVersion
                        },
                        transCube: {
                            label: 'Cube',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformCubeName
                        },
                        transMeth: {
                            label: 'Method',
                            layout: FormBuilder.INPUT_LAYOUT.TABLE,
                            readonly: true,
                            hidden: !metaProps.transformApp,
                            data: metaProps.transformMethodName
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
                    data: axis.preferredOrder,
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

    return {
        filterData: filterData,
        metaProperties: metaProperties,
        copyCube: copyCube,
        deleteAxis: deleteAxis,
        updateAxis: updateAxis
    };
})(jQuery);