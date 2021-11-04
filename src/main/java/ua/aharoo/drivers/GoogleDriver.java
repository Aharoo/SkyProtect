package ua.aharoo.drivers;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.*;
import java.util.LinkedList;

public class GoogleDriver implements CloudDriver {

    private String driverId;
    private String projectId = "cryptic-heaven-325015";
    private String property = "googleBucketName";
    private String defaultBucketName = "aharoo1";
    private Storage storage;
    private Bucket bucket = null;
    private Logger logger = LoggerFactory.getLogger(GoogleDriver.class);

    public GoogleDriver(String driverId){
        this.driverId = driverId;
        try {
            getBucketName(defaultBucketName,property);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (this.driverId.equals("cloud1")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud1");
        } else if (this.driverId.equals("cloud2")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud2");
        } else if (this.driverId.equals("cloud3")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud3");
        } else if (this.driverId.equals("cloud4")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud4");
        }
    }

    @Override
    public String uploadData(String bucketName, byte[] data, String fileId) throws StorageCloudException {
        bucket = storage.get(defaultBucketName);
        if (bucket == null) storage.create(BucketInfo.of(defaultBucketName));
        BlobId blobId = BlobId.of(defaultBucketName, fileId);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, data);
        return fileId;
    }

    @Override
    public String uploadData(String bucketName, File filePath, String fileId) throws StorageCloudException {
        bucket = storage.get(defaultBucketName);
        if (bucket == null) storage.create(BucketInfo.of(defaultBucketName));
        BlobId blobId = BlobId.of(defaultBucketName, fileId);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        GoogleThreadUpload thread = new GoogleThreadUpload(blobInfo, filePath);
        thread.start();

        return fileId;
    }

    private class GoogleThreadUpload extends Thread{
        private BlobInfo blobInfo;
        private File filePath;

        public GoogleThreadUpload(BlobInfo blobInfo,File filePath){
            this.blobInfo = blobInfo;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            try {
                int largeBufferSize = 5 * 1024 * 1024;
                storage.createFrom(blobInfo,filePath.toPath(),largeBufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId) throws StorageCloudException {
        bucket = storage.get(defaultBucketName);
        if (bucket == null) throw new StorageCloudException("Bucket doesn't exists");
        BlobId blobId = BlobId.of(defaultBucketName,fileId);
        Blob blob = storage.get(blobId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        blob.downloadTo(out);
        byte[] buffer = out.toByteArray();
        return buffer;
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId, File filePath) throws StorageCloudException {
        try {
            bucket = storage.get(defaultBucketName);
            if (bucket == null) throw new StorageCloudException("Bucket doesn't exists");
            BlobId blobId = BlobId.of(defaultBucketName, fileId);
            Blob blob = storage.get(blobId);
            FileOutputStream out = new FileOutputStream(filePath);
            blob.downloadTo(out);
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return fileId.getBytes();
    }

//    private class GoogleThreadDownload extends Thread{
//        private Blob blob;
//        private File filePath;
//
//        public GoogleThreadDownload(Blob blob,File filePath){
//            this.blob = blob;
//            this.filePath = filePath;
//        }
//        @Override
//        public void run() {
//            try {
//                FileOutputStream out = new FileOutputStream(filePath);
//                blob.downloadTo(out);
//                out.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    @Override
    public boolean deleteData(String bucketName, String fileId) throws StorageCloudException {
        bucket = storage.get(defaultBucketName);
        if (bucket == null) throw new StorageCloudException("Bucket doesn't exists");
        BlobId blobId = BlobId.of(defaultBucketName,fileId);
        Blob blob = storage.get(blobId);
        return blob.delete();
    }

    @Override
    public boolean deleteContainer(String bucketName, String[] namesToDelete) throws StorageCloudException {
        bucket = storage.get(defaultBucketName);
        if (bucket != null) {
            for (String str : namesToDelete)
                deleteData(defaultBucketName,str);
            return true;
        }
        return false;
    }

    @Override
    public String initSession() throws StorageCloudException {
        storage = StorageOptions.getDefaultInstance().getService();
        return "sid";
    }

    @Override
    public String initSession(boolean useModel) throws StorageCloudException {
        storage = StorageOptions.getDefaultInstance().getService();
        return "sid";
    }

    // TODO: Сделать отображение всех файлов в bucket

    @Override
    public LinkedList<String> listNames(String prefix, String bucketName) throws StorageCloudException {
        storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        bucket = storage.get(bucketName);
       // LinkedList<Blob> blobList = bucket.get();
        return null;
    }
    @Override
    public String getSessionKey() {
        return "sid";
    }

    @Override
    public String getDriverId() {
        return driverId;
    }
}
