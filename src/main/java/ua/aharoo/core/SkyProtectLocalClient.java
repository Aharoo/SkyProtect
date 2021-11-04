package ua.aharoo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.aharoo.clouds.CloudRepliesControlSet;
import ua.aharoo.clouds.CloudReply;
import ua.aharoo.clouds.CloudRequest;
import ua.aharoo.clouds.SkyProtectCloudManager;
import ua.aharoo.dao.DataUnitAndFileInfoDAO;
import ua.aharoo.drivers.CloudDriver;
import ua.aharoo.drivers.LocalCloudDriver;
import ua.aharoo.exceptions.StorageCloudException;
import ua.aharoo.models.DataUnit;
import ua.aharoo.models.FileInfo;
import ua.aharoo.utils.AESEncryptionImpl;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;

public class SkyProtectLocalClient implements SkyProtectClient {

    private static Logger log = LoggerFactory.getLogger(SkyProtectLocalClient.class);
    private int sequence = -1;
    public CloudDriver[] drivers;
    public SkyProtectManager manager;
    public HashMap<Integer, CloudRepliesControlSet> replies;
    public boolean parallelRequests = false;
    private static DataUnitAndFileInfoDAO dataUnitAndFileInfoDAO;
    private List<CloudReply> lastReadReplies;
    public List<CloudReply> lastMetaDataReplies;
    private static HashMap<SkyProtectDataUnit,List<Long>> chunkNamesBuffer;
    public int lastReadMetaDataSequence = -1, lastReadRepliesMaxVerIdx = -1;
    private static AESEncryptionImpl encryption;
    private static SkyProtectDataManager dataManager;

    public SkyProtectLocalClient(boolean useTestClouds) {
        this.dataUnitAndFileInfoDAO = new DataUnitAndFileInfoDAO();
        this.encryption = new AESEncryptionImpl();
        this.dataManager = new SkyProtectDataManager();
        this.chunkNamesBuffer = new HashMap<>();
        List<String[][]> credentials = null;
        try {
            credentials = readCredentials(useTestClouds);
        } catch (FileNotFoundException e) {
            System.out.println("accounts.properties doesn't exists");
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("account.properties misconfigured");
            e.printStackTrace();
        }
        this.drivers = new CloudDriver[4];
        String type = null, driverId = null, accessKey = null, secretKey = null;
        for (int i = 0; i < credentials.size(); i++) {
            for (String[] pair : credentials.get(i)) {
                if (pair[0].equalsIgnoreCase("driver.type")) type = pair[1];
                else if (pair[0].equalsIgnoreCase("driver.id")) driverId = pair[1];
                else if (pair[0].equalsIgnoreCase("accessKey")) accessKey = pair[1];
                else if (pair[0].equalsIgnoreCase("secretKey")) secretKey = pair[1];
            }
            drivers[i] = CloudDriversFactory.getDriver(type, driverId, accessKey, secretKey);
        }
        this.manager = new SkyProtectManager(drivers, this);
        this.replies = new HashMap<>();

        if (!startDrivers()) System.out.println("Connection error!");
    }

    @Override
    public synchronized void download(SkyProtectDataUnit dataUnit, File filePath, byte[] hashMatching, boolean needToDecrypt) throws Exception {
        parallelRequests = true;
        lastMetaDataReplies = null;
        CloudRepliesControlSet rcs = null;
        List<String> versionNumbers = new ArrayList<>();
        try {
            for (int i = 0; i < drivers.length; i++) {
                int seq = getNextSequence();
                rcs = new CloudRepliesControlSet(seq);
                broadcastGetMetaData(seq, dataUnit, SkyProtectManager.READ_PROTO, hashMatching, i, filePath);
                replies.put(seq, rcs);
                int nullResponses = 0;

                lastReadMetaDataSequence = seq;
                rcs.waitReplies.acquire();
                lastReadReplies = rcs.replies;

                for (int j = 0; j < rcs.replies.size(); j++) {
                    CloudReply reply = rcs.replies.get(j);
                    if (reply.response == null || reply.type != SkyProtectCloudManager.GET_DATA ||
                            reply.vNumber == null || reply.dataUnit == null) {
                        nullResponses++;
                    } else {
                        Long maxVersionFound = reply.dataUnit.getMaxVersion();

                        if (maxVersionFound.longValue() == Long.parseLong(reply.vNumber)) {
                            lastReadRepliesMaxVerIdx = i;
                            versionNumbers.add(i, reply.vNumber);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("READ ERROR: Could not get data after processing metadata");
        } finally {
            parallelRequests = false;
            if (rcs != null) replies.remove(rcs.sequence);
            replies.clear();
            rcs.sequence = 0;
        }
        dataManager.mergeFile(filePath,versionNumbers);
        dataManager.deleteFileChunks(filePath,versionNumbers);
        if (needToDecrypt) {
            String[] token = filePath.getName().split("\\.(?=[^\\.]+$)");
            File decryptedFile = new File(filePath.getParent() + "/" + token[0] + "Decrypted." + token[1]);
            encryption.decrypt(encryption.loadSecretKey(), filePath, decryptedFile);
            filePath.delete();
        }
    }

    public synchronized void downloadReplication(SkyProtectDataUnit dataUnit, File filePath, byte[] hashMatching) throws Exception {
        parallelRequests = true;
        lastMetaDataReplies = null;
        CloudRepliesControlSet rcs = null;
        try {
            for (int i = 0; i < drivers.length; i++) {
                int seq = getNextSequence();
                rcs = new CloudRepliesControlSet(seq);
                broadcastGetMetaData(seq, dataUnit, SkyProtectManager.READ_PROTO, hashMatching, i, filePath);
                replies.put(seq, rcs);
                int nullResponses = 0;

                lastReadMetaDataSequence = seq;
                rcs.waitReplies.acquire();
                lastReadReplies = rcs.replies;

                for (int j = 0; j < rcs.replies.size(); j++) {
                    CloudReply reply = rcs.replies.get(j);
                    additionalDownload(dataUnit, reply.dataUnit.getMaxVersion(), filePath);
                    return;
                }
            }
        } catch (Exception e) {
            throw new Exception("READ ERROR: Could not get data after processing metadata");
        } finally {
            parallelRequests = false;
            if (rcs != null) replies.remove(rcs.sequence);
            replies.clear();
            rcs.sequence = 0;
        }
    }

    public void additionalDownload(SkyProtectDataUnit dataUnit,long version, File filePath) throws Exception {
        LocalCloudDriver localCloudDriver = new LocalCloudDriver("cloud5");
        localCloudDriver.initSession();
        localCloudDriver.downloadData(dataUnit.getBucketName(),dataUnit.getDataUnitId() + "replicationvalue" + version,filePath);
//        if (needToDecrypt) {
//            String[] token = filePath.getName().split("\\.(?=[^\\.]+$)");
//            File decryptedFile = new File(filePath.getParent() + "/" + token[0] + "Decrypted." + token[1]);
//            encryption.decrypt(encryption.loadSecretKey(),filePath,decryptedFile);
//        }
    }

    public void deleteContainer(SkyProtectDataUnit dataUnit) {
        CloudRepliesControlSet rcs = null;
        try {
            int seq = getNextSequence();
            rcs = new CloudRepliesControlSet(seq);
            replies.put(seq, rcs);
            broadcastGetMetaData(seq, dataUnit, SkyProtectManager.DELETE_ALL, null,null);
            rcs.waitReplies.acquire();
            lastReadReplies = rcs.replies;
        } catch (StorageCloudException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void additionalWrite(SkyProtectDataUnit dataUnit, File filePath, long version, boolean needToEncrypt) throws Exception {
        LocalCloudDriver localCloudDriver = new LocalCloudDriver("cloud5");
        localCloudDriver.initSession();
        String[] tokens = filePath.getName().split("\\.(?=[^\\.]+$)");
        if (needToEncrypt) {
            File fileEncrypted = new File(filePath.getParent() + "/" + tokens[0] + "Encrypted." + tokens[1]);
            if (!fileEncrypted.exists()) fileEncrypted.createNewFile();
            encryption.encrypt(encryption.loadSecretKey(), filePath, fileEncrypted);
            localCloudDriver.uploadData(dataUnit.getBucketName(),fileEncrypted,dataUnit.getDataUnitId() + "replicationvalue" + version);
        } else localCloudDriver.uploadData(dataUnit.getBucketName(),filePath,dataUnit.getDataUnitId() + "replicationvalue" + version);
    }

    @Override
    public synchronized byte[] write(SkyProtectDataUnit dataUnit, byte[] allDataHash, File filepath, boolean useReplication, boolean needToEncrypt) throws Exception {

        long[] timeTracker = new long[4];

        long initialTime = System.currentTimeMillis();

        CloudRepliesControlSet rcs = null, wrcs = null;

        List<String> versions = new ArrayList<>();
        try {
            int seq = getNextSequence();
            rcs = new CloudRepliesControlSet(seq);
            replies.put(seq,rcs);
            broadcastGetMetaData(seq, dataUnit, SkyProtectManager.WRITE_PROTO, null,filepath);
            rcs.waitReplies.acquire();
            lastReadReplies = rcs.replies;

            int nullCounter = 0;
            long maxVersionDataUnitFound = 0;

            timeTracker[0] = System.currentTimeMillis() - initialTime;
            initialTime = System.currentTimeMillis();

            for (int i = 0; i < rcs.replies.size(); i++) {
                CloudReply reply = rcs.replies.get(i);
                if (reply.response == null || reply.type != SkyProtectCloudManager.GET_DATA || reply.vNumber == null) {
                    nullCounter++;
                    continue;
                } else {
                    long version = Long.parseLong(reply.vNumber);
                    if (version > maxVersionDataUnitFound) maxVersionDataUnitFound = version;
                }
            }

            timeTracker[1] = System.currentTimeMillis() - initialTime;
            initialTime = System.currentTimeMillis();

            long nextVersion = maxVersionDataUnitFound + SkyProtectManager.MAX_CLIENTS - (maxVersionDataUnitFound % SkyProtectManager.MAX_CLIENTS);

            seq = getNextSequence();
            wrcs = new CloudRepliesControlSet(seq);
            replies.put(seq, wrcs);

            timeTracker[2] = System.currentTimeMillis() - initialTime;

            if (useReplication) additionalWrite(dataUnit,filepath,nextVersion,needToEncrypt);

            List<Long> chunkNames = new ArrayList<>();
            for(int i = 0; i < drivers.length; i++) {
                File file = new File(filepath + String.valueOf(i));
                broadcastWriteValueRequests(seq, dataUnit, file, nextVersion + i + "", allDataHash, i);
                chunkNames.add(nextVersion + i);
                versions.add(String.valueOf(i));
            }

            timeTracker[3] = System.currentTimeMillis() - initialTime;

            dataUnit.setLastVersionOfFirstChunk(chunkNames.get(0));
            dataUnit.setLastVersionOfSecondChunk(chunkNames.get(1));
            dataUnit.setLastVersionOfThirdChunk(chunkNames.get(2));
            dataUnit.setLastVersionOfFourthChunk(chunkNames.get(3));
            chunkNamesBuffer.put(dataUnit,chunkNames);

            wrcs.waitReplies.acquire();
            lastReadReplies = wrcs.replies;

            System.out.println("Write function time to broadcast to all clouds to get the Metadata associated with this dataUnit: " + timeTracker[0]);
            System.out.println("Write function time to process metadata replies: " + timeTracker[1]);
            System.out.println("Write function time to calculate the name of the version to be written: " + timeTracker[2]);
            System.out.println("Write function time to broadcast data to all clouds: " + timeTracker[3]);

            dataUnit.lastVersionNumber = nextVersion;
            return allDataHash;
        } catch (Exception e) {
            System.out.println("SKYPROTECT WRITE ERROR:");
            e.printStackTrace();
            throw e;
        } finally{
           // dataManager.deleteFileChunks(filepath,versions);
        }
    }

    private void broadcastWriteValueRequests(int sequence, SkyProtectDataUnit dataUnit, File filePath, String version, byte[] allDataHash, int driverId) {
        CloudRequest request = new CloudRequest(SkyProtectCloudManager.NEW_DATA, sequence,
                drivers[driverId].getSessionKey(), dataUnit.getContainerId(drivers[driverId].getDriverId()), dataUnit.getGivenVersionValueDataFileName(version),
                filePath, null, dataUnit, SkyProtectManager.WRITE_PROTO, false, version, null, allDataHash, false);
        manager.doRequest(drivers[driverId].getDriverId(), request);
    }

    private void broadcastGetMetaData(int sequence, SkyProtectDataUnit dataUnit, int protoOp, byte[] hashMatching, File filePath) throws StorageCloudException {
        for (int i = 0; i < drivers.length; i++) {
            CloudRequest request = new CloudRequest(SkyProtectCloudManager.GET_DATA, sequence,
                    drivers[i].getSessionKey(), dataUnit.getContainerName(), dataUnit.getMetaDataFileName(),
                    null, null, dataUnit, protoOp, true, hashMatching, filePath);
            if (dataUnit.getContainerId(drivers[i].getDriverId()) == null)
                dataUnit.setContainerId(drivers[i].getDriverId(), dataUnit.getContainerName());

            manager.doRequest(drivers[i].getDriverId(), request);
        }
    }

    private void broadcastGetMetaData(int sequence, SkyProtectDataUnit dataUnit, int protoOp, byte[] hashMatching, int i, File filePath) throws StorageCloudException {
        CloudRequest request = new CloudRequest(SkyProtectCloudManager.GET_DATA, sequence,
                drivers[i].getSessionKey(), dataUnit.getContainerName(), dataUnit.getMetaDataFileName(),
                null, null, dataUnit, protoOp, true, hashMatching,filePath);
        if (dataUnit.getContainerId(drivers[i].getDriverId()) == null)
            dataUnit.setContainerId(drivers[i].getDriverId(), dataUnit.getContainerName());

        manager.doRequest(drivers[i].getDriverId(), request);
    }

    private byte[] generateSHA256Hash(File file) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return getFileChecksum(sha256,file);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getFileChecksum(MessageDigest digest, File file) {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];
            int bytesCount = 0;

            while((bytesCount = in.read(buffer)) != -1)
                digest.update(buffer,0,bytesCount);

            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void dataReceived(CloudReply reply) {
        if (!replies.containsKey(reply.sequence))
            return;

        CloudRepliesControlSet rcs = replies.get(reply.sequence);

        if (rcs != null) rcs.replies.add(reply);
        else return;

        //individual test measures (only add reply)
        if (reply.sequence < 0) return;

        //processing reply
        if (reply.protoOp == SkyProtectManager.READ_PROTO && reply.response != null) {
            rcs.waitReplies.release();
        } else if (reply.protoOp == SkyProtectManager.READ_PROTO
                && reply.dataUnit != null
                && reply.dataUnit.cloudVersions != null
                && rcs.replies != null
                && rcs.replies.size() > 0
                && !reply.isMetadataFile) {
            //normal & optimized read trigger (reg without PVSS)
            Long maxVersion = reply.dataUnit.getMaxVersion();
            Long foundVersion = reply.dataUnit.getCloudVersion(reply.cloudId);
            if (maxVersion != null
                    && maxVersion.longValue() == foundVersion.longValue()) {
                rcs.waitReplies.release();
                return;
            } else {
                System.out.println(reply.cloudId + " does not have max version "
                        + maxVersion + " but has " + foundVersion);
            }
        } else if (reply.protoOp == SkyProtectManager.READ_PROTO) {
            int nonNull = 0, nulls = 0;
            for (int i = 0; i < rcs.replies.size(); i++) {
                if (rcs.replies.get(i).response != null) {
                    nonNull++;
                } else {
                    nulls++;
                }
            }
        } else if (reply.protoOp >= SkyProtectManager.WRITE_PROTO && reply.dataUnit != null) {
            rcs.waitReplies.release();
            return;
        }

        if (reply.protoOp != SkyProtectManager.WRITE_PROTO) {
            rcs.waitReplies.release();
            replies.remove(rcs.sequence);
        }
    }

    @Override
    public boolean sendingParallelRequests() {
        return parallelRequests;
    }

    private List<String[][]> readCredentials(boolean useTestClouds) throws FileNotFoundException, ParseException {
        Scanner scanner;
        if (useTestClouds) scanner = new Scanner(new File("src/main/resources/accounts_LOCALE_ONLY.properties"));
        else scanner = new Scanner(new File("src/main/resources/accounts.properties"));
        String line;
        String[] splitLine;
        LinkedList<String[][]> list = new LinkedList<>();
        int lineNum = -1;
        LinkedList<String[]> list2 = new LinkedList<>();
        boolean firstTime = true;
        while (scanner.hasNext()) {
            lineNum++;
            line = scanner.nextLine();
            if (line.startsWith("#") || line.equals("")) continue;
            else {
                splitLine = line.split("=", 2);
                if (splitLine.length != 2) {
                    scanner.close();
                    throw new ParseException("Bad formatted accounts.properties file.", lineNum);
                } else {
                    if (splitLine[0].equals("driver.type")) {
                        if (!firstTime) {
                            String[][] array = new String[list2.size()][2];
                            for (int i = 0; i < array.length; i++)
                                array[i] = list2.get(i);
                            list.add(array);
                            list2 = new LinkedList<>();
                        } else firstTime = false;
                    }
                    list2.add(splitLine);
                }
            }
        }
        String[][] array = new String[list2.size()][2];
        for (int i = 0; i < array.length; i++)
            array[i] = list2.get(i);
        list.add(array);
        scanner.close();
        return list;
    }

    private boolean startDrivers() {
        System.out.println("Starting drivers...");
        int seq = getNextSequence();
        CloudRepliesControlSet rcs = new CloudRepliesControlSet(seq);
        replies.put(seq,rcs);
        for (int i = 0; i < 4; i++)
            manager.driversManagers[i].doRequest(
                    new CloudRequest(SkyProtectCloudManager.INIT_SESS, seq, null,
                            null, null, null, new Properties(), null, -1, false, null));
        try {
            rcs.waitReplies.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int nullCounter = 0;
        for (int i = 0; i < rcs.replies.size(); i++) {
            CloudReply reply = rcs.replies.get(i);
            if (reply.response == null) {
                System.out.println("Cloud error " + reply.cloudId);
                nullCounter++;
                if (nullCounter > 0) {
                    System.out.println("ERROR: Drivers initialization was failed!");
                    return false;
                }
            }
        }
        replies.remove(rcs.sequence);
        System.out.println("All drivers were started successfully!");

        return true;
    }

    private synchronized int getNextSequence() {
        sequence++;
        return sequence;
    }

    // TODO: Добавить систему регистрации
    public static void main(String[] args) throws Exception {
        args = new String[1];
        args[0] = "1"; // 1 - использовать 4 разных облака, 0 - использовать тестовые облака
        if (Integer.valueOf(args[0]) <= 1 && Integer.valueOf(args[0]) >= 0) {
            System.out.println("Write 'help' to list all available commands");

            boolean useTestClouds = false;
            boolean useReplication = false;
            boolean needToDecrypt = false;
            if (args[0].equals("0"))  useTestClouds = true;
            SkyProtectLocalClient local = new SkyProtectLocalClient(useTestClouds);
            SkyProtectDataUnit dataUnit = null;

            boolean terminate = false;
            Scanner in = new Scanner(System.in);
            String input;
            HashMap<String, LinkedList<byte[]>> map = new HashMap<>();
            while (!terminate) {
                input = in.nextLine();

                if (input.equals("exit")) {
                    System.out.println("Thank you for using this program!");
                    System.exit(0);
                } else if(input.equals("help")){
                    System.out.println("USAGE: commands          function");
                    System.out.println("       pick 'name'     - change the container");
                    System.out.println("       write 'data'    - write a new version in the selected container");
                    System.out.println("       en_write 'data' - write a new version with additional encryption'");
                    System.out.println("       download 'path' - download the last version of the selected container");
                    System.out.println("       get_all_files   - return list of existing files in containers");
                    System.out.println("       get_all_units   - return list of existing units");
                    System.out.println("       delete_all      - delete all the files in the selected container");
                    System.out.println("       hash_download 'num' - download old versions, you need to enter filename");
                    System.out.println("       help            - shows list of available commands");
                    System.out.println("       exit            - stop the program");
                    System.out.println();
                } else if(input.equals("get_all_files")){
                    dataUnitAndFileInfoDAO.getAllFiles();
                } else if(input.equals("get_all_units")){
                    dataUnitAndFileInfoDAO.getAllDataUnits();
                }
                if (input.length() > 4 && input.startsWith("pick") && input.split(" ").length > 0) {
                    StringBuilder sb = new StringBuilder(input.substring(5));
                    dataUnit = new SkyProtectDataUnit(sb.toString());
                    if (!map.containsKey(sb.toString())) {
                        LinkedList<byte[]> hashes = new LinkedList<>();
                        map.put(sb.toString(), hashes);
                    }
                    System.out.println("DataUnit '" + sb + "' selected!");
                } else {
                    if (dataUnit != null) {
                        if (input.equals("delete_all")) {
                            try {
                                local.deleteContainer(dataUnit);
                                dataUnitAndFileInfoDAO.deleteAllFiles(dataUnit.dataUnitId);
                                dataUnitAndFileInfoDAO.deleteDataUnit(dataUnit.dataUnitId);
                                System.out.println("Deleting is finished");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if(input.startsWith("download") && input.split(" ").length > 0) {
                            StringBuilder sb = new StringBuilder(input.substring(9));
                            File file = new File(sb.toString());
                            System.out.println("Need to decrypt file?(If file is encrypted) 1 - yes, 0 - no");
                            input = in.nextLine();
                            if (input.equals("1")) needToDecrypt = true;
                            else needToDecrypt = false;
                            System.out.println("Need to download replicated file? 1 - yes, 0 - no");
                            input = in.nextLine();
                            if (input.equals("1")) useReplication = true;
                            else useReplication = false;
                            System.out.println("DOWNLOADING TO: " + sb);
                            long acMil = System.currentTimeMillis();
                            try {
                                if (useReplication) {
                                    local.downloadReplication(dataUnit,file,null);
                                    if (needToDecrypt) {
                                        String[] token = file.getName().split("\\.(?=[^\\.]+$)");
                                        File decryptedFile = new File(file.getParent() + "/" + token[0] + "Decrypted." + token[1]);
                                        encryption.decrypt(encryption.loadSecretKey(),file,decryptedFile);
                                    }
                                }
                                else local.download(dataUnit, file,null, needToDecrypt);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            long tempo = System.currentTimeMillis() - acMil;
                            System.out.println("I'm finished download -> " + tempo + " milis");
                        } else if (input.startsWith("write") && input.split(" ").length > 0) {
                            StringBuilder sb = new StringBuilder(input.substring(6));
                            System.out.println("Need to use additional replication? 1 - yes, 0 - no");
                            input = in.nextLine();
                            if(input.equals("1")) useReplication = true;
                            else useReplication = false;
                            System.out.println("WRITING: " + sb);
                            File filePath = new File(sb.toString());
                            try {
                                if (filePath.exists()) {
                                    long acMil = System.currentTimeMillis();
                                    long start = System.currentTimeMillis();
                                    log.info("Start splitting files " + start);
                                    dataManager.splitFile(filePath);
                                    log.info("Finished splitting files " + (System.currentTimeMillis() - start));
                                    byte[] allDataHash = local.generateSHA256Hash(filePath);
                                    byte[] hash = local.write(dataUnit, allDataHash, filePath, useReplication,false);

                                    LinkedList<byte[]> current = map.get(dataUnit.getDataUnitId());
                                    current.addFirst(hash);
                                    map.put(dataUnit.getDataUnitId(), current);
                                    long tempo = System.currentTimeMillis() - acMil;

                                    DataUnit dataUnit1 = new DataUnit();
                                    dataUnit1.setDataUnitName(dataUnit.getDataUnitId());
                                    if (useTestClouds) dataUnit1.setOnlyOnAWS(true);
                                    else dataUnit1.setOnlyOnAWS(false);
                                    dataUnit1.setCreationDate(LocalDateTime.now());

                                    FileInfo fileInfo = new FileInfo();
                                    fileInfo.setFileEncrypted(false);
                                    fileInfo.setFilename(filePath.getName());
                                    fileInfo.setCreationDate(LocalDateTime.now());
                                    fileInfo.setFirstChunk(chunkNamesBuffer.get(dataUnit).get(0));
                                    fileInfo.setSecondChunk(chunkNamesBuffer.get(dataUnit).get(1));
                                    fileInfo.setThirdChunk(chunkNamesBuffer.get(dataUnit).get(2));
                                    fileInfo.setFourthChunk(chunkNamesBuffer.get(dataUnit).get(3));
                                    fileInfo.setFileHash(hash);
                                    if (useReplication) fileInfo.setAdditionalReplicationExists(true);
                                    else fileInfo.setAdditionalReplicationExists(false);

                                    chunkNamesBuffer.remove(dataUnit);
                                    dataUnitAndFileInfoDAO.saveDataUnit(dataUnit1, fileInfo);
                                    //dataManager.deleteFileChunks(filePath);
                                    System.out.println("I'm finished write -> " + tempo + " milis");
                                } else throw new FileNotFoundException("File does not exists! Please try again.");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (input.startsWith("en_write") && input.split(" ").length > 0) {
                            StringBuilder sb = new StringBuilder(input.substring(9));
                            System.out.println("Need to use additional replication? 1 - yes, 0 - no");
                            input = in.nextLine();
                            if(input.equals("1")) useReplication = true;
                            else useReplication = false;
                            System.out.println("WRITING " + sb + " WITH AES ENCRYPTION:");
                            File file = new File(sb.toString());
                            try {
                                if (file.exists()) {
                                    long acMil = System.currentTimeMillis();
                                    String[] tokens = file.getName().split("\\.(?=[^\\.]+$)");
                                    File fileEncrypted = new File(file.getParent() + "/" + tokens[0] + "Encrypted." + tokens[1]);
                                    encryption.encrypt(encryption.loadSecretKey(),file,fileEncrypted);
                                    byte[] allDataHash = local.generateSHA256Hash(fileEncrypted);
                                    dataManager.splitFile(fileEncrypted);
                                    byte[] hash = local.write(dataUnit, allDataHash, fileEncrypted, useReplication,true);

                                    LinkedList<byte[]> current = map.get(dataUnit.getDataUnitId());
                                    current.addFirst(hash);
                                    map.put(dataUnit.getDataUnitId(), current);
                                    long tempo = System.currentTimeMillis() - acMil;

                                    DataUnit dataUnit1 = new DataUnit();
                                    dataUnit1.setDataUnitName(dataUnit.getDataUnitId());
                                    if (useTestClouds) dataUnit1.setOnlyOnAWS(true);
                                    else dataUnit1.setOnlyOnAWS(false);
                                    dataUnit1.setCreationDate(LocalDateTime.now());

                                    FileInfo fileInfo = new FileInfo();
                                    fileInfo.setFileEncrypted(true);
                                    fileInfo.setFilename(file.getName());
                                    fileInfo.setCreationDate(LocalDateTime.now());
                                    fileInfo.setFirstChunk(chunkNamesBuffer.get(dataUnit).get(0));
                                    fileInfo.setSecondChunk(chunkNamesBuffer.get(dataUnit).get(1));
                                    fileInfo.setThirdChunk(chunkNamesBuffer.get(dataUnit).get(2));
                                    fileInfo.setFourthChunk(chunkNamesBuffer.get(dataUnit).get(3));
                                    fileInfo.setFileHash(hash);
                                    if (useReplication) fileInfo.setAdditionalReplicationExists(true);
                                    else fileInfo.setAdditionalReplicationExists(false);

                                    chunkNamesBuffer.remove(dataUnit);
                                    dataUnitAndFileInfoDAO.saveDataUnit(dataUnit1, fileInfo);
                                    System.out.println("I'm finished write with aes encryption -> " + tempo + " milis");
                                } else throw new FileNotFoundException("File does not exists! Please try again.");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if (input.startsWith("hash_download") && input.split(" ").length > 0) {
                            StringBuilder sb = new StringBuilder(input.substring(14));
                            File file = new File(sb.toString());
                            System.out.println("Need to download replicated file? 1 - yes, 0 - no");
                            input = in.nextLine();
                            if(input.equals("1")) useReplication = true;
                            else useReplication = false;
                            System.out.println("Need to decrypt file?(If file is encrypted) 1 - yes, 0 - no");
                            input = in.nextLine();
                            if (input.equals("1")) needToDecrypt = true;
                            else needToDecrypt = false;
                            System.out.println("Enter filename you need to download:");
                            input = in.nextLine();
                            StringBuilder buffer = new StringBuilder(input);
                            FileInfo fileInfo = dataUnitAndFileInfoDAO.getFileInfo(buffer.toString());
                            byte[] hashMatching = fileInfo.getFileHash();
                            System.out.println("DOWNLOADING TO: " + sb);
                            long acMil = System.currentTimeMillis();
                            try {
                                dataUnit.clearAllCaches();
                                if (useReplication) {
                                    local.downloadReplication(dataUnit,file,hashMatching);
                                    if (needToDecrypt) {
                                        String[] token = file.getName().split("\\.(?=[^\\.]+$)");
                                        File decryptedFile = new File(file.getParent() + "/" + token[0] + "Decrypted." + token[1]);
                                        encryption.decrypt(encryption.loadSecretKey(),file,decryptedFile);
                                    }
                                }
                                else local.download(dataUnit,file,hashMatching,needToDecrypt);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            long tempo = System.currentTimeMillis() - acMil;
                            System.out.println("I'm finished download -> " + tempo + " milis");
                        } else System.out.println("Enter available commands:");
                    } else System.out.println("You need to pick a container to use.");
                }
            }
            in.close();
        } else System.out.println("Invalid arguments");
    }
}

