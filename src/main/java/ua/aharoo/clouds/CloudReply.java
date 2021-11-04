package ua.aharoo.clouds;

import ua.aharoo.core.SkyProtectDataUnit;

import java.io.File;
import java.util.LinkedList;

public class CloudReply {

    public int sequence, type, protoOp;
    public String cloudId, container, vNumber, vHash, exceptionMessage, valueFileId;
    public SkyProtectDataUnit dataUnit;
    public Object response;
    public boolean isMetadataFile;
    public byte[] value, allDataHash, hashMatching;
    public long receiveTime, initReceiveTime, startTime, metadataReceiveTime;
    public LinkedList<String> listNames;
    public File filePath;

    @Override
    public String toString() {
        return "sn:" + sequence + "#cloud:" + cloudId + "#type:" + type
                + "#dataUnitId:" + (dataUnit != null ? dataUnit.dataUnitId : "null")
                + "#op:" + protoOp + "#vn:" + vNumber + "#mdfile?" + isMetadataFile;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
                      String container, SkyProtectDataUnit dataUnit, int protoOp, boolean isMetadataFile, byte[] hashMatching) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.dataUnit = dataUnit;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.hashMatching = hashMatching;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
                      String container, SkyProtectDataUnit dataUnit, int protoOp, boolean isMetadataFile,
                      byte[] value, String vNumber, byte[] allDataHash, LinkedList<String> list_names, byte[] hashMatching, File filePath) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.dataUnit = dataUnit;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.value = value;
        this.vNumber = vNumber;
        this.allDataHash = allDataHash;
        this.listNames = list_names;
        this.hashMatching = hashMatching;
        this.filePath = filePath;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
                      String container, SkyProtectDataUnit dataUnit, int protoOp, boolean isMetadataFile,
                      File filePath, String vNumber, byte[] allDataHash, LinkedList<String> list_names, byte[] hashMatching) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.dataUnit = dataUnit;
        this.protoOp = protoOp;
        this.isMetadataFile = isMetadataFile;
        this.filePath = filePath;
        this.vNumber = vNumber;
        this.allDataHash = allDataHash;
        this.listNames = list_names;
        this.hashMatching = hashMatching;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
                      String container, SkyProtectDataUnit dataUnit, int protoOp, boolean isMetadataFile,
                      String vNumber, String vHash, byte[] allDataHash, byte[] hashMatching) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.dataUnit = dataUnit;
        this.isMetadataFile = isMetadataFile;
        this.protoOp = protoOp;
        this.vNumber = vNumber;
        this.vHash = vHash;
        this.allDataHash = allDataHash;
        this.hashMatching = hashMatching;
    }

    public CloudReply(int type, int sequence, String cloudId, Object response,
                      String container, SkyProtectDataUnit dataUnit, int protoOp, boolean isMetadataFile,
                      String vNumber, String vHash, byte[] allDataHash, byte[] hashMatching, File filePath) {
        this.type = type;
        this.sequence = sequence;
        this.cloudId = cloudId;
        this.response = response;
        this.container = container;
        this.dataUnit = dataUnit;
        this.isMetadataFile = isMetadataFile;
        this.protoOp = protoOp;
        this.vNumber = vNumber;
        this.vHash = vHash;
        this.allDataHash = allDataHash;
        this.hashMatching = hashMatching;
        this.filePath = filePath;
    }



    public void setVersionNumber(String vNumber) {
        this.vNumber = vNumber;
    }

    public void setVersionHash(String vHash) {
        this.vHash = vHash;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public void setInitReceiveTime(long initReceiveTime) {
        this.initReceiveTime = initReceiveTime;
    }

    public void setExceptionMessage(String msg) {
        this.exceptionMessage = msg;
    }

    public void setValueFileId(String valueFileId) {
        this.valueFileId = valueFileId;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    void setMetadataReceiveTime(long metadataReceiveTime) {
        this.metadataReceiveTime = metadataReceiveTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudReply that = (CloudReply) o;
        return sequence == that.sequence && cloudId.equals(that.cloudId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.sequence;
        hash = 29 * hash + (this.cloudId != null ? this.cloudId.hashCode() : 0);
        return hash;
    }

    public void invalidateResponse() {
        this.response = null;
        this.vNumber = null;
        this.vHash = null;
    }
}

