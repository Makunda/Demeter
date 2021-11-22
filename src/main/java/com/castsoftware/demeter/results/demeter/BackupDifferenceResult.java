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

public class BackupDifferenceResult {
  // Sources options
  public String sourceState;
  public String sourceLevel;
  public Long sourceObject;

  // Destination options
  public String destinationState;
  public String destinationLevel;
  public Long destinationObject;

  public BackupDifferenceResult(
      String sourceState,
      String sourceLevel,
      Long sourceObject,
      String destinationState,
      String destinationLevel,
      Long destinationObject) {
    this.sourceState = sourceState;
    this.sourceLevel = sourceLevel;
    this.sourceObject = sourceObject;
    this.destinationState = destinationState;
    this.destinationLevel = destinationLevel;
    this.destinationObject = destinationObject;
  }
}
