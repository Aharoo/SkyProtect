package ua.aharoo.clouds;

import ua.aharoo.core.SkyProtectClient;
import ua.aharoo.core.SkyProtectLocalClient;
import ua.aharoo.drivers.AmazonDriver;
import ua.aharoo.drivers.AzureDriver;
import ua.aharoo.drivers.CloudDriver;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class SkyProtectCloudManager extends Thread{
    public static final int INIT_SESS = 0;
    public static final int DEL_CONT = 1;
    public static final int NEW_DATA = 2;
    public static final int GET_DATA = 3;
    public static final int DEL_DATA = 4;

    private static final int MAX_RETRIES = 3;
    public CloudDriver cloudDriver;
    public ArrayList<CloudReply> replies;
    public ArrayList<CloudRequest> requests;
    public CloudDataManager cloudDataManager;
    public SkyProtectClient client;
    private boolean terminate = false;

    public SkyProtectCloudManager(CloudDriver cloudDriver, CloudDataManager cloudDataManager, SkyProtectClient client){
        this.cloudDriver = cloudDriver;
        this.requests = new ArrayList<>();
        this.replies = new ArrayList<>();
        this.cloudDataManager = cloudDataManager;
        this.client = client;
        this.start();
    }

    public void run(){
        while(true){
            try {
                if(terminate && replies.isEmpty() && requests.isEmpty()) break;
                else if (!replies.isEmpty()) processReply();
                else if (!requests.isEmpty()) processRequest();
                else sleepAnAInstant();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void doRequest(CloudRequest request){
        if (!terminate) requests.add(request);
       // System.out.println("Added request to list. result=" + request);
    }

    public void addReply(CloudReply reply){
        if(!terminate) replies.add(reply);
       // System.out.println("Added reply to list. result=" + reply);
    }

    private void processRequest() throws StorageCloudException {
        CloudRequest request = null;
        CloudReply reply;
        long init = 0;
        try {
            request = requests.remove(0);
            switch (request.op) {
                case INIT_SESS:
                    terminate = false;
                    String sessionId = cloudDriver.initSession();
                    addReply(new CloudReply(request.op, request.seqNumber, cloudDriver.getDriverId(),
                            sessionId, request.containerId, request.dataUnit, request.protoOp,
                            request.isMetaDataFile, request.hashMatching));
                    break;
                case DEL_CONT:
                    boolean delContRes = cloudDriver.deleteContainer(request.driverId,request.namesToDelete);
                    addReply(new CloudReply(request.op,request.seqNumber,cloudDriver.getDriverId(),delContRes,request.containerId,request.dataUnit,request.protoOp,request.isMetaDataFile,request.hashMatching));
                    break;
                case NEW_DATA:
                    init = System.currentTimeMillis();

                    if (request.isMetaDataFile) {
                        String ssid = cloudDriver.uploadData(request.dataUnit.getBucketName(), request.data, request.fileId);
                        reply = new CloudReply(request.op, request.seqNumber,
                                cloudDriver.getDriverId(), ssid, request.containerId,
                                request.dataUnit, request.protoOp, request.isMetaDataFile,
                                request.data, request.vNumber, request.allDataHash, null, null,request.filePath);
                    } else {
                        String ssid = cloudDriver.uploadData(request.dataUnit.getBucketName(), request.filePath, request.fileId);
                        reply = new CloudReply(request.op, request.seqNumber,
                                cloudDriver.getDriverId(), ssid, request.containerId,
                                request.dataUnit, request.protoOp, request.isMetaDataFile,
                                request.filePath, request.vNumber, request.allDataHash, null, null);
                    }

                    reply.setStartTime(request.startTime);
                    reply.setInitReceiveTime(init);
                    if (request.fileId.contains("metadata") && request.isMetaDataFile){
                        reply.setReceiveTime(System.currentTimeMillis());
                        reply.setStartTime(request.startTime);
                    }

                    addReply(reply);
                    break;
                case GET_DATA:
                    init = System.currentTimeMillis();
                    byte[] data;
                    if (request.isMetaDataFile) {
                        data = cloudDriver.downloadData(request.dataUnit.getBucketName(), request.fileId);

                        reply = new CloudReply(request.op, request.seqNumber, cloudDriver.getDriverId(), data, request.containerId, request.dataUnit,
                                request.protoOp, request.isMetaDataFile, request.vNumber,
                                request.vHash, request.allDataHash, request.hashMatching, request.filePath);
                    } else {
                        File file = new File(request.filePath + request.vNumber);
                        data = cloudDriver.downloadData(request.dataUnit.getBucketName(), request.fileId, file);

                        reply = new CloudReply(request.op, request.seqNumber, cloudDriver.getDriverId(), data, request.containerId, request.dataUnit,
                                request.protoOp, request.isMetaDataFile, request.vNumber,
                                request.vHash, request.allDataHash, request.hashMatching, file);
                    }

                    reply.setInitReceiveTime(init);
                    reply.setStartTime(request.startTime);
                    if (request.isMetaDataFile) reply.setMetadataReceiveTime(System.currentTimeMillis());
                    else reply.setMetadataReceiveTime(request.metaDataReceiveTime);

                    addReply(reply);
                    break;
                case DEL_DATA:
                    boolean deleteRes = cloudDriver.deleteData(request.dataUnit.getBucketName(),request.fileId);

                    addReply(new CloudReply(request.op,request.seqNumber,cloudDriver.getDriverId(),
                            deleteRes,request.containerId, request.dataUnit,request.protoOp,
                            request.isMetaDataFile,request.hashMatching));

                    break;
                default:
                    // Operation doesn't exist
                    addReply(new CloudReply(request.op,request.seqNumber,cloudDriver.getDriverId(),null,
                            request.containerId,request.dataUnit,request.protoOp,request.isMetaDataFile,null));

                    break;
                }
            } catch (Exception e){
                e.printStackTrace();//testing purposes
                if (request.retries < MAX_RETRIES) {
                    // Retry request to cloud again
                    request.incrementRetries();
                    doRequest(request);
                    return;
                }
                reply = new CloudReply(request.op, request.seqNumber,
                        cloudDriver.getDriverId(), null, request.containerId, request.dataUnit,
                        request.protoOp, request.isMetaDataFile, request.vNumber, request.vHash, null, null);
                reply.setReceiveTime(System.currentTimeMillis());
                reply.setInitReceiveTime(init);
                reply.setExceptionMessage(e.getMessage());
                reply.setStartTime(request.startTime);
                reply.invalidateResponse();
                addReply(reply);
        }
    }

    private void processReply() {
        try {
            CloudReply reply = replies.remove(0);
            if (reply == null) return;

            if (reply.response == null) {
                client.dataReceived(reply);
                return;
            }
            if (reply.response != null) {
                if (reply.type == GET_DATA && reply.isMetadataFile) cloudDataManager.processMetaData(reply);
                else if (reply.type == GET_DATA) {
                    if (reply.vHash == null) client.dataReceived(reply);
                    else cloudDataManager.checkDataIntegrity(reply);
                } else if (reply.type == NEW_DATA && !reply.isMetadataFile && reply.filePath != null)
                    cloudDataManager.writeNewMetaData(reply);
                else if (reply.type == NEW_DATA && reply.isMetadataFile && reply.value != null) {
                    client.dataReceived(reply);
                    return;
                } else {
                    client.dataReceived(reply);
                    return;
                }
            }

            if (reply.response == null) {
                client.dataReceived(reply);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void terminate(){terminate = true;}

    public void resetRequests(){
        terminate = false;
        replies.clear();
        requests.clear();
    }

    private int counter = 0;

    private void sleepAnAInstant(){
        try {
            counter++;
            if(counter % 50 == 0) counter = 0;
            Thread.sleep(50);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

