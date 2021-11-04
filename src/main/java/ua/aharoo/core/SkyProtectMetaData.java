package ua.aharoo.core;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SkyProtectMetaData {

    private byte[] allDataHash;
    private String versionFileId;
    private byte[] signature;
    private String metaData;

    public SkyProtectMetaData() {
    }

    public SkyProtectMetaData(String metaData, byte[] signature, byte[] allDataHash, String versionFileId){

        this.metaData = metaData;
        this.signature = signature;
        this.allDataHash = allDataHash;
        this.versionFileId = versionFileId;
    }

    public byte[] getAllDataHash() {
        return allDataHash;
    }

    public String getVersionFileId() {
        return versionFileId;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getMetadata() {
        return metaData;
    }

    public void readExternal(ObjectInput in) throws IOException {
        allDataHash = new byte[in.readInt()];
        readFromObjectInput(in,allDataHash);
        versionFileId = in.readUTF();
        signature = new byte[in.readInt()];
        readFromObjectInput(in,signature);
        metaData = in.readUTF();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(allDataHash.length);
        out.write(allDataHash);
        out.writeUTF(versionFileId);
        out.writeInt(signature.length);
        out.write(signature);
        out.writeUTF(metaData);
    }


    public static void readFromObjectInput(ObjectInput in,byte[] buffer){
        try {
            for (int i = 0; i < buffer.length; i++) buffer[i] = in.readByte();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

