package com.castsoftware.tagging.exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The <code>TaggingException</code> is the based Exception class of the Moirai procedure.
 * It implements a system of Exception code. Each child class is free to add a prefix to the message or the code.
 * If so, the prefix of the code should be of the form "X{3}_Y{2,3}_" with X the trigram representing the error type and Y the specific identifier.
 * Example for a Neo4j bad request exception : X -> NEO, Y -> BR ==> prefix = NEO_BR_
 * The final code should be of the form : NEO_BR_00000fd
 * TaggingException
 */
public abstract class TaggingException extends Throwable {

    private static final long serialVersionUID = -8579984202435983804L;
    private static Pattern errorCodeReg = Pattern.compile("^[\\w\\d]{3}_[\\w\\d]{2,3}_[\\w\\d]{8,10}$",
            Pattern.CASE_INSENSITIVE);
    protected String code;

    public TaggingException(String message, Throwable cause, String code) {
        super(message, cause);

        Matcher m  = errorCodeReg.matcher(code);
        if(! m.matches() ) {
            message.concat(String.format(" ### Malformed exception code : %s. Please see the recommendations in the documentation.", code));
        }
        this.code = code;
    }

    public TaggingException(String message, String code) {
        super(message);

        Matcher m  = errorCodeReg.matcher(code);
        if(! m.matches() ) {
            message.concat(String.format(" ### Malformed exception code : %s. Please see the recommendations in the documentation.", code));
        }
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
