package ua.aharoo.exceptions;

import java.io.Serializable;

public class StorageCloudException extends Exception implements Serializable {

    public static final String UNAVAILABLE_FUNC = "Not supported by driver";
    public static final String INVALID_SESSION = "Incorrect session properties";
    public static final String UNKNOWN_EXCEPTION = "Unknown exception throw";
    public static final String LOGIN_ERROR = "Not logged in. Please login.";

    public StorageCloudException(String message){super(message);}

}
