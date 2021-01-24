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

package com.castsoftware.demeter.exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The <code>TaggingException</code> is the based Exception class of the Moirai procedure. It
 * implements a system of Exception code. Each child class is free to add a prefix to the message or
 * the code. If so, the prefix of the code should be of the form "X{3}_Y{2,3}_" with X the trigram
 * representing the error type and Y the specific identifier. Example for a Neo4j bad request
 * exception : X -> NEO, Y -> BR ==> prefix = NEO_BR_ The final code should be of the form :
 * NEO_BR_00000fd TaggingException
 */
public abstract class TaggingException extends Throwable {

  private static final long serialVersionUID = -8579984202435983804L;
  private static Pattern errorCodeReg =
      Pattern.compile("^[\\w\\d]{3}_[\\w\\d]{2,3}_[\\w\\d]{8,10}$", Pattern.CASE_INSENSITIVE);
  protected String code;

  public TaggingException(String message, Throwable cause, String code) {
    super(message, cause);

    Matcher m = errorCodeReg.matcher(code);
    if (!m.matches()) {
      message.concat(
          String.format(
              " ### Malformed exception code : %s. Please see the recommendations in the documentation.",
              code));
    }
    this.code = code;
  }

  public TaggingException(String message, String code) {
    super(message);

    Matcher m = errorCodeReg.matcher(code);
    if (!m.matches()) {
      message.concat(
          String.format(
              " ### Malformed exception code : %s. Please see the recommendations in the documentation.",
              code));
    }
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
