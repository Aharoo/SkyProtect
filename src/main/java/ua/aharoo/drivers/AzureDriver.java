package ua.aharoo.drivers;

import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.common.ParallelTransferOptions;
import com.azure.storage.common.ProgressReceiver;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import org.apache.commons.io.FileUtils;
import ua.aharoo.exceptions.ServiceSiteException;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousFileChannel;
import java.security.InvalidKeyException;
import java.util.LinkedList;

public class AzureDriver implements CloudDriver {

    private static final String STORAGE_CONNECTION_STRING = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
    private String driverId;
    private String defaultBucketName = "aharoo1";
    private String property = "azureBucketName";
    private CloudBlobClient blobClient;
    private CloudStorageAccount storageAccount;

    public AzureDriver(String driverId){
        this.driverId = driverId;
        try {
            getBucketName(defaultBucketName,property);
        } catch (FileNotFoundException e){
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
        try {
            CloudBlobContainer container = null;
            if (bucketName == null)
                container = blobClient.getContainerReference(defaultBucketName);
            else
                container = blobClient.getContainerReference(bucketName);

            container.createIfNotExists();
            CloudBlockBlob blob = container.getBlockBlobReference(fileId);
            blob.upload(new ByteArrayInputStream(data),data.length);
        } catch (URISyntaxException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (StorageException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (IOException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        }

        return fileId;
    }

    @Override
    public String uploadData(String bucketName, File filePath, String fileId) throws StorageCloudException {
        try {
            CloudBlobContainer container = null;
            if (bucketName == null)
                container = blobClient.getContainerReference(defaultBucketName);
            else
                container = blobClient.getContainerReference(bucketName);

            container.createIfNotExists();
            CloudBlockBlob blob = container.getBlockBlobReference(fileId);
            AzureThreadUpload thread = new AzureThreadUpload(blob,filePath);
            thread.start();
        } catch (URISyntaxException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (StorageException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        }

        return fileId;
    }

    private class AzureThreadUpload extends Thread{
        private CloudBlockBlob blob;
        private File filePath;

        public AzureThreadUpload(CloudBlockBlob blob,File filePath){
            this.blob = blob;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            try {
                blob.uploadFromFile(filePath.getAbsolutePath());
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public byte[] downloadData(String bucketName, String fileId) throws StorageCloudException {
        byte[] data = null;
        try {
            CloudBlobContainer container = null;
            if (bucketName == null)
                container = blobClient.getContainerReference(defaultBucketName);
            else
                container = blobClient.getContainerReference(bucketName);
            CloudBlockBlob blob = container.getBlockBlobReference(fileId);
            if (!blob.exists()) throw new StorageCloudException("Azure download error");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blob.download(out);
            data = out.toByteArray();
            return data;
        } catch (URISyntaxException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (StorageException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        }
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId, File filePath) throws StorageCloudException {
        try {
            CloudBlobContainer container = null;
            if (bucketName == null)
                container = blobClient.getContainerReference(defaultBucketName);
            else
                container = blobClient.getContainerReference(bucketName);
            CloudBlockBlob blob = container.getBlockBlobReference(fileId);
            if (!blob.exists()) throw new StorageCloudException("Azure download error");
            FileOutputStream out = new FileOutputStream(filePath);
            blob.download(out);
            return fileId.getBytes();
        } catch (URISyntaxException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (StorageException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

//    private class AzureThreadDownload extends Thread{
//        private CloudBlockBlob blob;
//        private File filePath;
//
//        public AzureThreadDownload(CloudBlockBlob blob,File filePath){
//            this.blob = blob;
//            this.filePath = filePath;
//        }
//        @Override
//        public void run() {
//            try {
//                FileOutputStream out = new FileOutputStream(filePath);
//                blob.download(out);
//            } catch (StorageException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    @Override
    public boolean deleteData(String bucketName, String fileId) throws StorageCloudException {
        try {
            CloudBlobContainer container = null;
            if (bucketName == null)
                container = blobClient.getContainerReference(defaultBucketName);
            else
                container = blobClient.getContainerReference(bucketName);
            CloudBlob blob =  container.getBlockBlobReference(fileId);
            blob.delete();
            return true;
        } catch (URISyntaxException e) {
            return false;
        } catch (StorageException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean deleteContainer(String bucketName, String[] namesToDelete) throws StorageCloudException {
        CloudBlobContainer container = null;
        try {
            if (bucketName != null) container = blobClient.getContainerReference(bucketName);
            else container = blobClient.getContainerReference(defaultBucketName);
            CloudBlockBlob blob;
            for (String str : namesToDelete){
                blob = container.getBlockBlobReference(str);
                blob.delete();
            }
            return true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (StorageException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String initSession() throws StorageCloudException {
        try {
            storageAccount = CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
            blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(defaultBucketName);
            container.createIfNotExists();
        } catch (URISyntaxException e){
            throw new StorageCloudException("AzureStorageException::" + e.getMessage());
        } catch (StorageException e){
            throw new StorageCloudException("AzureStorageException::" + e.getMessage());
        } catch (InvalidKeyException e){
            throw new StorageCloudException("AzureStorageException::" + e.getMessage());
        }
        return "sid";
    }

    @Override
    public String initSession(boolean useModel) throws StorageCloudException {
        return "sid";
    }

    @Override
    public LinkedList<?> listNames(String prefix, String bucketName) throws StorageCloudException {
        LinkedList<String> find = new LinkedList<>();
        try {
            CloudBlobContainer container = blobClient.getContainerReference(defaultBucketName);
            Iterable<ListBlobItem> listOfItems = container.listBlobs(prefix);
            for (ListBlobItem item : listOfItems){
                String[] name = item.getUri().getPath().split("/");
                find.add(name[name.length-1]);
            }
        } catch (StorageException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        } catch (URISyntaxException e){
            throw new ServiceSiteException("AzureStorageException::" + e.getMessage());
        }
        return find;
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
