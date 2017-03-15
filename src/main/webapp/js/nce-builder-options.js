var NCEBuilderOptions = (function () {

    function filterData(columnSelectList, readonly, afterSave, onClose) {
        return {
            title: 'Filter Data',
            instructionsTitle: 'Instructions - Filter Data',
            instructionsText: 'Select filters to apply to cell data for ncube.',
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            readonly: readonly,
            afterSave: afterSave,
            onClose: onClose,
            columns: {
                isApplied: {
                    heading: 'Apply',
                    type: FormBuilder.INPUT_TYPE.CHECKBOX,
                    default: true
                },
                column: {
                    heading: 'Column',
                    type: FormBuilder.INPUT_TYPE.SELECT,
                    selectOptions: columnSelectList
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

    function metaProperties(name, type, readonly, afterSave, onClose) {
        return {
            title: 'Metaproperties - ' + name,
            instructionsTitle: 'Instructions - Metaproperties',
            instructionsText: 'Add custom properties for this ' + type + '.',
            displayType: FormBuilder.DISPLAY_TYPE.TABLE,
            size: FormBuilder.MODAL_SIZE.LARGE,
            readonly: readonly,
            afterSave: afterSave,
            onClose: onClose,
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

    function deleteAxis(axisName, afterSave, onClose) {
        return {
            title: 'Delete Axis - ' + axisName,
            instructionsTitle: '',
            instructionsText: 'Note: All cells will be cleared when an axis is deleted.',
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            afterSave: afterSave,
            onClose: onClose,
            saveButtonText: 'Delete Axis',
            formInputs: {
                name: {
                    label: 'Axis to delete',
                    readonly: true,
                    data: axisName
                }
            }
        };
    }

    function updateAxis(axis, readonly, afterSave, onClose) {
        var metaProps = axis.metaProps || {};
        return {
            title: 'Update Axis - ' + axis.name,
            displayType: FormBuilder.DISPLAY_TYPE.FORM,
            readonly: readonly,
            afterSave: afterSave,
            onClose: onClose,
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
        deleteAxis: deleteAxis,
        updateAxis: updateAxis
    };
})(jQuery);