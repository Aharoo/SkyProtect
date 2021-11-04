package ua.aharoo.clouds;

import ua.aharoo.core.SkyProtectDataUnit;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class CloudRequest {

    public int op, seqNumber, protoOp, retries;
    public String driverId, containerId, fileId, vNumber, vHash;
    public String[] namesToDelete;
    public SkyProtectDataUnit dataUnit;
    public byte[] data, allDataHash, hashMatching;
    public Properties props;
    public boolean isMetaDataFile;
    public long startTime, metaDataReceiveTime;
    public File filePath;


    public CloudRequest(int op, int seqNumber, String driverId, String containerId, String fileId,
                        byte[] data, Properties props, SkyProtectDataUnit dataUnit, int protoOp,
                        boolean isMetaDataFile, byte[] hashMatching) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.driverId = driverId;
        this.containerId = containerId;
        this.fileId = fileId;
        this.data = data;
        this.props = props;
        this.dataUnit = dataUnit;
        this.protoOp = protoOp;
        this.isMetaDataFile = isMetaDataFile;
        this.hashMatching = hashMatching;
    }

    public CloudRequest(int op, int seqNumber, String driverId, String containerId, String fileId,
                        byte[] data, Properties props, SkyProtectDataUnit dataUnit, int protoOp,
                        boolean isMetaDataFile, byte[] hashMatching, File filePath) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.driverId = driverId;
        this.containerId = containerId;
        this.fileId = fileId;
        this.data = data;
        this.props = props;
        this.dataUnit = dataUnit;
        this.protoOp = protoOp;
        this.isMetaDataFile = isMetaDataFile;
        this.hashMatching = hashMatching;
        this.filePath = filePath;
    }

    public CloudRequest(int op, int seqNumber, String driverId, String containerId, String fileId,
                        File filePath, Properties props, SkyProtectDataUnit dataUnit, int protoOp,
                        boolean isMetaDataFile, String vNumber, String vHash, byte[] allDataHash, boolean flag) {
        this.op = op;
        this.containerId = containerId;
        this.seqNumber = seqNumber;
        this.driverId = driverId;
        this.fileId = fileId;
        this.filePath = filePath;
        this.props = props;
        this.dataUnit = dataUnit;
        this.isMetaDataFile = isMetaDataFile;
        this.protoOp = protoOp;
        this.vNumber = vNumber;
        this.vHash = vHash;
        this.allDataHash = allDataHash;

    }

    public CloudRequest(int op, int seqNumber, String driverId, String containerId, String[] namesToDelete,
                        SkyProtectDataUnit dataUnit, int protoOp, boolean isMetaDataFile) {
        this.op = op;
        this.seqNumber = seqNumber;
        this.driverId = driverId;
        this.containerId = containerId;
        this.namesToDelete = namesToDelete;
        this.dataUnit = dataUnit;
        this.isMetaDataFile = isMetaDataFile;
        this.protoOp = protoOp;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setMetadataReceiveTime(long metaDataReceiveTime) {
        this.metaDataReceiveTime = metaDataReceiveTime;
    }

    public String toString() {
        return op + ":" + seqNumber + ":" + protoOp + ":" + containerId + ":" + fileId;
    }

    public void incrementRetries() {
        retries++;
    }

    public void resetRetries() {
        retries = 0;
    }

}
