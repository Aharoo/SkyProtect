package ua.aharoo.clouds;

public interface CloudDataManager {

    void processMetaData(CloudReply metaDataReply);

    void checkDataIntegrity(CloudReply valueDataReply);

    void writeNewMetaData(CloudReply reply);

}
