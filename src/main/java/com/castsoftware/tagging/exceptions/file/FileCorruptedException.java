/*
 *  Copyright (C) 2020  Hugo JOBY
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.castsoftware.tagging.exceptions.file;

import com.castsoftware.tagging.exceptions.TaggingException;

/**
 * The <code>FileNotFound</code> is thrown when the procedure can't read a file because of corrupted file or bad format.
 * FileNotFound
 */
public class FileCorruptedException extends TaggingException {

    private static final long serialVersionUID = 5538686331898382119L;
    private static final String MESSAGE_PREFIX = "Error, file corrupted and can't be processed by Moirai : ";
    private static final String CODE_PREFIX = "FIL_CR_";

    public FileCorruptedException(String message, String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message).concat(". Path : ").concat(path), cause, CODE_PREFIX.concat(code));
    }

    public FileCorruptedException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

    public FileCorruptedException(String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(path), cause, CODE_PREFIX.concat(code));
    }
}
