package ua.aharoo.core;

import ua.aharoo.clouds.CloudReply;

import java.io.File;

public interface SkyProtectClient {

    void download(SkyProtectDataUnit dataUnit,File filePath, byte[] hashMatching, boolean needToDecrypt) throws Exception;

    byte[] write(SkyProtectDataUnit dataUnit, byte[] value, File filepath, boolean useReplication, boolean needToEncrypt) throws Exception;

    void dataReceived(CloudReply reply);

    boolean sendingParallelRequests();

}
