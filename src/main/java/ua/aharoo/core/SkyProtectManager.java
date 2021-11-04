package ua.aharoo.core;

import ua.aharoo.clouds.*;
import ua.aharoo.drivers.CloudDriver;
import ua.aharoo.utils.SkyProtectKeyLoader;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class SkyProtectManager implements CloudDataManager {

    public static final int MAX_CLIENTS = 1000;//(ids: 0 to 999)
    public static final int READ_PROTO = 0;
    public static final int WRITE_PROTO = 1;
    public static final int DELETE_ALL = 2;
    public static final String CRLF = "\r\n";
    public SkyProtectCloudManager[] driversManagers;
    public SkyProtectKeyLoader keyLoader;
    public SkyProtectLocalClient client;
    public ConcurrentHashMap<String, LinkedList<SkyProtectMetaData>> cloud1;
    public ConcurrentHashMap<String, LinkedList<SkyProtectMetaData>> cloud2;
    public ConcurrentHashMap<String, LinkedList<SkyProtectMetaData>> cloud3;
    public ConcurrentHashMap<String, LinkedList<SkyProtectMetaData>> cloud4;

    public SkyProtectManager(CloudDriver[] drivers, SkyProtectLocalClient client){
        this.driversManagers = new SkyProtectCloudManager[drivers.length];
        this.keyLoader = new SkyProtectKeyLoader("");
        this.client = client;

        cloud1 = new ConcurrentHashMap<>();
        cloud2 = new ConcurrentHashMap<>();
        cloud3 = new ConcurrentHashMap<>();
        cloud4 = new ConcurrentHashMap<>();

        init(drivers);
    }

    private void init(CloudDriver[] drivers){
        for (int i = 0; i < drivers.length; i++)
            driversManagers[i] = new SkyProtectCloudManager(drivers[i],this,client);
    }

    private void clearAllRequestsToProcess(){
        for (int i = 0; i < driversManagers.length;i++)
            driversManagers[i].resetRequests();;
    }

    @Override
    public void processMetaData(CloudReply metaDataReply) {
        try {
            metaDataReply.setReceiveTime(System.currentTimeMillis());
            ByteArrayInputStream biss = new ByteArrayInputStream((byte[]) metaDataReply.response);
            ObjectInputStream ins = new ObjectInputStream(biss);
            biss.close();

            LinkedList<SkyProtectMetaData> allMetaData = new LinkedList<>();

            int size = ins.readInt();
            byte[] metaDataInit = new byte[size];
            SkyProtectMetaData.readFromObjectInput(ins, metaDataInit);
            size = ins.readInt();
            byte[] allMetaDataSignature = new byte[size];
            SkyProtectMetaData.readFromObjectInput(ins, allMetaDataSignature);

            ins.close();

            biss = new ByteArrayInputStream(metaDataInit);
            ins = new ObjectInputStream(biss);

            size = ins.readInt();
            for (int i = 0; i < size; i++) {
                SkyProtectMetaData meta = new SkyProtectMetaData();
                meta.readExternal(ins);
                allMetaData.add(meta);
            }

            biss.close();
            ins.close();

            String dataReplied = null;
            SkyProtectMetaData dm = null;
            int cont = 0;

            //if is a request to delete a entire dataUnit
            if (metaDataReply.protoOp == SkyProtectManager.DELETE_ALL) {
                String[] namesToDelete = new String[allMetaData.size() + 1];
                namesToDelete[0] = metaDataReply.dataUnit.getMetaDataFileName();
                for (int i = 1; i < allMetaData.size() + 1; i++)
                    namesToDelete[i] = allMetaData.get(i - 1).getVersionFileId();

                SkyProtectCloudManager manager = getDriverManagerByDriverId(metaDataReply.cloudId);
                CloudRequest request = new CloudRequest(SkyProtectCloudManager.DEL_CONT,
                        metaDataReply.sequence, manager.cloudDriver.getSessionKey(),
                        metaDataReply.container,namesToDelete,metaDataReply.dataUnit,
                        SkyProtectManager.DELETE_ALL, false);
                manager.doRequest(request);

                return;
            }

            //if is a download matching operation
            if (metaDataReply.hashMatching != null) {
                for (int i = 0; i < allMetaData.size(); i++) {
                    if (Arrays.equals(allMetaData.get(i).getAllDataHash(), metaDataReply.hashMatching)) {
                        dm = allMetaData.get(i);
                        dataReplied = dm.getMetadata();
                        if (dataReplied.length() < 1) throw new Exception("Invalid metaData size received");
                        cont = allMetaData.size() + 1;
                    }
                }
                if (cont < allMetaData.size() + 1) throw new Exception("No matching version available");
            } else { //if is a normal download (last version download)
                dm = allMetaData.getFirst();
                dataReplied = dm.getMetadata();
                if (dataReplied.length() < 1) throw new Exception("Invalid metaData size received");
            }

            byte[] mdInfo = dataReplied.getBytes();
            byte[] signature = dm.getSignature();
            Properties props = new Properties();
            props.load(new ByteArrayInputStream(mdInfo));
            // MetaData info
            String verNumber = props.getProperty("versionNumber");
            String verHash = props.getProperty("versionHash");
            String verValueFileId = props.getProperty("versionFileId");
            long versionFound = Long.parseLong(verNumber);

            // Long writerId = versionFound % MAX_CLIENTS;
            // metaData signature check
            if (!verifyMetadataSignature(mdInfo, signature) ||
                    !verifyMetadataSignature(metaDataInit, allMetaDataSignature)) {
                System.out.println(".................");
                throw new Exception("Signature verification failed for " + metaDataReply);
            }

            //long ts = versionFound - writerId; // remove clientId from versionNumber
            metaDataReply.setVersionNumber(versionFound + ""); //version received
            metaDataReply.setVersionHash(verHash);
            metaDataReply.setValueFileId(verValueFileId);

            if (metaDataReply.protoOp == SkyProtectManager.WRITE_PROTO) {
                if (metaDataReply.cloudId.equals("cloud1")) {
                    cloud1.put(metaDataReply.container, allMetaData);
                }
                else if (metaDataReply.cloudId.equals("cloud2")) {
                    cloud2.put(metaDataReply.container, allMetaData);
                }
                else if (metaDataReply.cloudId.equals("cloud3")) {
                    cloud3.put(metaDataReply.container, allMetaData);
                }
                else if (metaDataReply.cloudId.equals("cloud4")) {
                    cloud4.put(metaDataReply.container, allMetaData);
                }

                client.dataReceived(metaDataReply);
                return;
            }
            synchronized (this) {
                if (metaDataReply.sequence == client.lastReadMetaDataSequence) {
                    if (client.lastMetaDataReplies == null) client.lastMetaDataReplies = new ArrayList<>();
                    client.lastMetaDataReplies.add(metaDataReply);
                    metaDataReply.dataUnit.setCloudVersion(metaDataReply.cloudId, versionFound);

                }
                if (metaDataReply.sequence >= 0 && canReleaseAndReturn(metaDataReply)) return;

                SkyProtectCloudManager manager = getDriverManagerByDriverId(metaDataReply.cloudId);
                CloudRequest request = new CloudRequest(SkyProtectCloudManager.GET_DATA,
                        metaDataReply.sequence, manager.cloudDriver.getSessionKey(),
                        metaDataReply.container, verValueFileId, metaDataReply.filePath, null,
                        metaDataReply.dataUnit, metaDataReply.protoOp, false,
                        versionFound + "", verHash, null,false);
                request.setStartTime(metaDataReply.startTime);
                request.setMetadataReceiveTime(metaDataReply.metadataReceiveTime);
                manager.doRequest(request);

            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR_PROCESSING_METADATA: " + metaDataReply);
            metaDataReply.invalidateResponse();
        }

    }

    private boolean canReleaseAndReturn(CloudReply mdreply) {
        /*required in the case where we are waiting for n - f metadata replies and already have the value*/
        try {
            CloudRepliesControlSet rcs = client.replies.get(mdreply.sequence);
            if(rcs != null){
//                if (mdreply.dataUnit.cloudVersions.size() >= client.N - client.F
//                        && rcs.replies.size() > client.F) {
                    for (int i = 0; i < rcs.replies.size() - 1; i++) {
                        CloudReply r = rcs.replies.get(i);
                        if (r.response != null && r.vNumber != null
                                && rcs.replies.get(i).vNumber.equals(mdreply.dataUnit.getMaxVersion() + "")
                                && rcs.replies.get(i + 1).vNumber.equals(mdreply.dataUnit.getMaxVersion() + "")) {
                            rcs.waitReplies.release();
                            return true;

                    }
                }
//                if (mdreply.dataUnit.cloudVersions.size() >= client.N - client.F
//                        && rcs.replies.size() > 0) {
                    for (int i = 0; i < rcs.replies.size(); i++) {
                        CloudReply r = rcs.replies.get(i);
                        if (r.response != null && r.vNumber != null
                                && rcs.replies.get(i).vNumber.equals(mdreply.dataUnit.getMaxVersion() + "")) {
                            rcs.waitReplies.release();
                            return true;

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean verifyMetadataSignature(byte[] metaData, byte[] signature){
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(keyLoader.loadPublicKey());
            sig.update(metaData);
            return sig.verify(signature);
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }



    public SkyProtectCloudManager getDriverManagerByDriverId(String id) {
        for (int i = 0; i < driversManagers.length; i++) {
            if (driversManagers[i].cloudDriver.getDriverId().equals(id)) {
                return driversManagers[i];
            }
        }
        return null;
    }

    @Override
    public void checkDataIntegrity(CloudReply valueDataReply) {
        try {
            String valueHash = getHexString(getHash(valueDataReply.filePath));
            valueDataReply.setReceiveTime(System.currentTimeMillis());
            if (true) client.dataReceived(valueDataReply); // TODO: Решить проблему з хэшами
            else throw new Exception("integrity verification failed..." + valueDataReply);
        } catch (Exception e) {
            e.printStackTrace();
            valueDataReply.invalidateResponse();
            valueDataReply.setExceptionMessage(e.getMessage());
        }
    }


    @Override
    public void writeNewMetaData(CloudReply reply) {
        ByteArrayOutputStream allMetaData = null;
        ObjectOutputStream out = null;
        try {
            allMetaData = new ByteArrayOutputStream();
            out = new ObjectOutputStream(allMetaData);
            String valueDataFileId = (String) reply.response;
            String mProps = "versionNumber = " + reply.vNumber + CRLF
                    + "versionHash = " + getHexString(getHash(reply.filePath)) + CRLF
                    + "allDataHash = " + reply.allDataHash + CRLF
                    + "versionFileId = " + valueDataFileId + CRLF;

            //getting the last versions metadata information
            SkyProtectMetaData newMD = new SkyProtectMetaData(mProps, getSignature(mProps.getBytes()), reply.allDataHash, valueDataFileId);
            LinkedList<SkyProtectMetaData> oldMetaData = new LinkedList<>();
            if (reply.cloudId.equals("cloud1")) {
                if (cloud1.containsKey(reply.container)) {
                    oldMetaData = new LinkedList<>(cloud1.get(reply.container));
                    cloud1.remove(reply.container);
                }
            }
            else if (reply.cloudId.equals("cloud2")) {
                if (cloud2.containsKey(reply.container)) {
                    oldMetaData = new LinkedList<>(cloud2.get(reply.container));
                    cloud2.remove(reply.container);
                }
            }
            else if (reply.cloudId.equals("cloud3")) {
                if (cloud3.containsKey(reply.container)) {
                    oldMetaData = new LinkedList<>(cloud3.get(reply.container));
                    cloud3.remove(reply.container);
                }
            }
            else if (reply.cloudId.equals("cloud4")) {
                if (cloud4.containsKey(reply.container)) {
                    oldMetaData = new LinkedList<>(cloud4.get(reply.container));
                    cloud4.remove(reply.container);
                }
            }

            oldMetaData.addFirst(newMD);
            out.writeInt(oldMetaData.size());
            for (int i = 0; i < oldMetaData.size(); i++) oldMetaData.get(i).writeExternal(out);
            out.close();
            allMetaData.close();

            byte[] metaDataInit = allMetaData.toByteArray();
            byte[] allMetaDataSignature = getSignature(metaDataInit);

            allMetaData = new ByteArrayOutputStream();
            out = new ObjectOutputStream(allMetaData);

            out.writeInt(metaDataInit.length);
            out.write(metaDataInit);
            out.writeInt(allMetaDataSignature.length);
            out.write(allMetaDataSignature);

            out.flush();
            allMetaData.flush();
            allMetaData.close();

            SkyProtectCloudManager manager = getDriverManagerByDriverId(reply.cloudId);
            CloudRequest request = new CloudRequest(SkyProtectCloudManager.NEW_DATA,
                    reply.sequence, manager.cloudDriver.getSessionKey(),
                    reply.container, reply.dataUnit.dataUnitId + "metadata",
                    allMetaData.toByteArray(), null, reply.dataUnit,
                    reply.protoOp, true, reply.hashMatching,reply.filePath);
            request.setStartTime(reply.startTime);
            manager.doRequest(request);

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] getSignature(byte[] value){
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(keyLoader.loadPrivateKey());
            sig.update(value);
            return sig.sign();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static String getHexString(byte[] raw)
            throws UnsupportedEncodingException {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }

    private byte[] getHash(File filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return client.getFileChecksum(sha256,filePath);
    }

    private byte[] getHash(byte[] value) throws NoSuchAlgorithmException, IOException {
        return MessageDigest.getInstance("SHA-256").digest(value);
    }

    private static final byte[] HEX_CHAR_TABLE = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
            (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public void doRequest(String cloudId, CloudRequest request){getDriverManagerByDriverId(cloudId).doRequest(request);}
}
