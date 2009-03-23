/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 27, 2008
 */
package org.jsoar.kernel.learning.rl;

class rl_string_parameter
{
    int value;
    StringParameterValFunc val_func;
    StringParameterToString to_str;
    StringParameterFromString from_str;
}