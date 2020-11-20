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

package com.castsoftware.demeter.exceptions.file;

import com.castsoftware.demeter.exceptions.TaggingException;

/**
 * The <code>FileNotFound</code> is thrown when the procedure can't access a file because it doesn't exist, or the path resolution failed.
 * FileNotFound
 */
public class FileIOException extends TaggingException {

    private static final long serialVersionUID = -622271594516405222L;
    private static final String MESSAGE_PREFIX = "Error, IO exception during file operation : ";
    private static final String CODE_PREFIX = "FIL_IO_";

    public FileIOException(String message, String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message).concat(". Path : ").concat(path), cause, CODE_PREFIX.concat(code));
    }

    public FileIOException(String path, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(path), cause, CODE_PREFIX.concat(code));
    }
}
