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

package com.castsoftware.demeter.results.demeter;

/** id: number; name: string; application: string; numObjects: number; */
public class DemeterGroupResult {
    public Long id;
    public String name;
    public String application;
    public Long numObjects;

    public DemeterGroupResult(Long id, String name, String application, Long numObjects) {
        this.id = id;
        this.name = name;
        this.application = application;
        this.numObjects = numObjects;
    }
}
