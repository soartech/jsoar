/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 22, 2008
 */
package org.jsoar.kernel.tracing;

/**
 * trace.cpp:49:trace_format_type
 * 
 * @author ray
 */
public enum TraceFormatType
{
    STRING_TFT,                        /* print a string */
    PERCENT_TFT,                       /* print a percent sign */
    L_BRACKET_TFT,                     /* print a left bracket */
    R_BRACKET_TFT,                     /* print a right bracket */
    VALUES_TFT,                        /* print values of attr path or '*' */
    VALUES_RECURSIVELY_TFT,            /* ditto only print recursively */
    ATTS_AND_VALUES_TFT,               /* ditto only print attr's too */
    ATTS_AND_VALUES_RECURSIVELY_TFT,   /* combination of the two above */
    CURRENT_STATE_TFT,                 /* print current state */
    CURRENT_OPERATOR_TFT,              /* print current operator */
    DECISION_CYCLE_COUNT_TFT,          /* print # of dc's */
    ELABORATION_CYCLE_COUNT_TFT,       /* print # of ec's */
    IDENTIFIER_TFT,                    /* print identifier of object */
    IF_ALL_DEFINED_TFT,                /* print subformat if it's defined */
    LEFT_JUSTIFY_TFT,                  /* left justify the subformat */
    RIGHT_JUSTIFY_TFT,                 /* right justify the subformat */
    SUBGOAL_DEPTH_TFT,                 /* print # of subgoal depth */
    REPEAT_SUBGOAL_DEPTH_TFT,          /* repeat subformat s.d. times */
    NEWLINE_TFT,                       /* print a newline */
}
