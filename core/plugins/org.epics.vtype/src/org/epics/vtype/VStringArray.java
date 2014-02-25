/**
 * Copyright (C) 2010-14 pvmanager developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.vtype;

import java.util.List;

/**
 *
 * @author carcassi
 */
public interface VStringArray extends Array, Alarm, Time, VType {
    @Override
    List<String> getData();
}
