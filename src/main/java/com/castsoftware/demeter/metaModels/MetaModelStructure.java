/*
 * Copyright (C) 2020  Hugo JOBY
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License v3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public v3
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.demeter.metaModels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MetaModelStructure {

    public String metaModelName;
    public String language;
    public boolean splitNotInTransactionsObjects;
    public boolean splitExternalObjects;
    public String[] toMergeObjectType;
    public Map<String, Long[]> customOperations;

    public MetaModelStructure() {
        this.metaModelName = "YourModelName";
        this.language = "Java";
        this.splitExternalObjects = false;
        this.splitNotInTransactionsObjects = false;
        this.toMergeObjectType = new String[]{};
        this.customOperations = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaModelStructure that = (MetaModelStructure) o;
        return splitNotInTransactionsObjects == that.splitNotInTransactionsObjects
                && splitExternalObjects == that.splitExternalObjects
                && metaModelName.equals(that.metaModelName)
                && language.equals(that.language)
                && customOperations.equals(that.customOperations)
                && Arrays.equals(toMergeObjectType, that.toMergeObjectType);
    }

    @Override
    public int hashCode() {
        int result =
                Objects.hash(
                        metaModelName,
                        language,
                        splitNotInTransactionsObjects,
                        splitExternalObjects,
                        customOperations);
        result = 31 * result + Arrays.hashCode(toMergeObjectType);
        return result;
    }
}
