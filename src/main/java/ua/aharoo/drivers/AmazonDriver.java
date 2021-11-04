package ua.aharoo.drivers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.google.cloud.storage.BlobInfo;
import com.microsoft.azure.storage.StorageException;
import org.apache.commons.io.FileUtils;
import ua.aharoo.exceptions.ClientSiteException;
import ua.aharoo.exceptions.ServiceSiteException;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.*;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import static com.amazonaws.services.s3.model.Permission.Read;
import static com.amazonaws.services.s3.model.Permission.Write;

// Класс,который напрямую взаимодействует с Amazon S3 API


public class AmazonDriver implements CloudDriver{

    private String driverId;
    private String defaultBucketName = "aharoo";
    private String accessKey, secretKey;
    private String property = "amazonBucketName";
    private Regions region = null;
    private AmazonS3 amazonS3;

    public AmazonDriver(String driverId,String accessKey,String secretKey){
        this.driverId = driverId;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        try {
            getBucketName(defaultBucketName,property);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (this.driverId.equals("cloud1")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud1");
            this.region = Regions.EU_WEST_1;
        } else if (this.driverId.equals("cloud2")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud2");
            this.region = Regions.US_WEST_1;
        } else if (this.driverId.equals("cloud3")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud3");
            this.region = Regions.AP_SOUTH_1;
        } else if (this.driverId.equals("cloud4")) {
            this.defaultBucketName = this.defaultBucketName.concat("-cloud4");
            this.region = Regions.EU_WEST_2;
        }
    }

    /**
     * Соединение с Amazon S3
     */


    @Override
    public String initSession() throws StorageCloudException {
        try {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            amazonS3 = AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(region)
                    .build();
            if (!amazonS3.doesBucketExistV2(defaultBucketName)) {
                amazonS3.createBucket(defaultBucketName);
            }
        } catch (AmazonServiceException e){
            throw new StorageCloudException(StorageCloudException.INVALID_SESSION);
        }
        return "sid";
    }

    @Override
    public String initSession(boolean useModel) throws StorageCloudException {
        try {
            String mprops = "accessKey=" + this.accessKey + "\r\n" + "secretKey = " + this.secretKey;
            PropertiesCredentials b = new PropertiesCredentials(new ByteArrayInputStream(mprops.getBytes()));
            this.amazonS3 = new AmazonS3Client(b);
            this.amazonS3.setEndpoint("http://s3.amazonaws.com");

            if (!this.amazonS3.doesBucketExist(this.defaultBucketName)) {
                this.amazonS3.createBucket(this.defaultBucketName);
            }
        } catch (IOException var3) {
            System.out.println("Cannot connect with Amazon S3.");
            throw new StorageCloudException("Incorrect session properties.");
        }
        return "sid";
    }



    @Override
    public String uploadData(String bucketName, byte[] data, String fileId)  throws StorageCloudException  {
        try(ByteArrayInputStream in = new ByteArrayInputStream(data)){
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);

            if (bucketName != null) {
                if (!amazonS3.doesBucketExistV2(bucketName)) {
                    amazonS3.createBucket(bucketName);
                }
                amazonS3.putObject(new PutObjectRequest(bucketName, fileId, in, metadata));

            } else amazonS3.putObject(new PutObjectRequest(defaultBucketName,fileId,in,metadata));
            return fileId;
        } catch (AmazonServiceException e1){
            throw new StorageCloudException("AWSS3Exception::" + e1.getMessage());
        } catch (AmazonClientException e2){
            throw new ClientSiteException("AWSS3Exception::" + e2.getMessage());
        } catch (IOException e3){
            e3.printStackTrace();
            throw new StorageCloudException("AWSS3Exception::" + e3.getMessage());
        }
    }

    @Override
    public String uploadData(String bucketName, File filePath, String fileId) throws StorageCloudException {
        try{
            if (defaultBucketName != null) {
                if (!amazonS3.doesBucketExistV2(defaultBucketName)) {
                    amazonS3.createBucket(defaultBucketName);
                }
                AmazonThreadUpload thread = new AmazonThreadUpload(fileId,filePath);
                thread.start();
            }
            return fileId;
        } catch (AmazonServiceException e1){
            throw new StorageCloudException("AWSS3Exception::" + e1.getMessage());
        } catch (AmazonClientException e2){
            throw new ClientSiteException("AWSS3Exception::" + e2.getMessage());
        }
    }

    private class AmazonThreadUpload extends Thread{
        private String fileId;
        private File filePath;

        public AmazonThreadUpload(String fileId,File filePath){
            this.fileId = fileId;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            amazonS3.putObject(new PutObjectRequest(defaultBucketName,fileId,filePath));
        }
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId)  throws StorageCloudException  {
        try {
            S3Object object = null;

            if (bucketName == null) object = amazonS3.getObject(defaultBucketName,fileId);
            else object = amazonS3.getObject(bucketName,fileId);

            return IOUtils.toByteArray(object.getObjectContent());
        } catch (AmazonServiceException e1){
           // throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
        } catch (AmazonClientException e2){
            throw new ClientSiteException("AWSS3Exception::" + e2.getMessage());
        } catch (IOException e3){
            e3.printStackTrace();
            throw new StorageCloudException("AWSS3Exception::" + e3.getMessage());
        }
        return null;
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId, File filePath) throws StorageCloudException {
        S3ObjectInputStream in= null;
        try {
            S3Object object = null;

            if (bucketName == null) object = amazonS3.getObject(defaultBucketName,fileId);
            else object = amazonS3.getObject(bucketName,fileId);
            in = object.getObjectContent();
            FileUtils.copyInputStreamToFile(in,filePath);
            return fileId.getBytes();
        } catch (AmazonServiceException e1){
            // throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
        } catch (AmazonClientException e2){
            throw new ClientSiteException("AWSS3Exception::" + e2.getMessage());
        } catch (IOException e3){
            e3.printStackTrace();
            throw new StorageCloudException("AWSS3Exception::" + e3.getMessage());
        }
        return new byte[0];
    }

//    private class AmazonThreadDownload extends Thread{
//        private S3Object object;
//        private String fileId;
//        private File filePath;
//
//        public AmazonThreadDownload(S3Object object,String fileId,File filePath){
//            this.object = object;
//            this.fileId = fileId;
//            this.filePath = filePath;
//        }
//        @Override
//        public void run() {
//            S3ObjectInputStream in = object.getObjectContent();
//            amazonS3.putObject(new PutObjectRequest(defaultBucketName,fileId,filePath));
//        }
//    }

    @Override
    public boolean deleteData(String bucketName, String fileId)  throws StorageCloudException  {
        try {
            if (bucketName == null) amazonS3.deleteObject(defaultBucketName,fileId);
            else amazonS3.deleteObject(bucketName,fileId);

            return true;
        } catch (Exception ex){
            return false;
        }
    }

    @Override
    public boolean deleteContainer(String bucketName, String[] namesToDelete)  throws StorageCloudException  {
        try {
            for (String fileId : namesToDelete)
                amazonS3.deleteObject(defaultBucketName,fileId);
            return true;
        } catch (Exception ex){
            throw new StorageCloudException("AWSS3Exception::" + ex.getMessage());
        }
    }

    @Override
    public LinkedList<String> listNames(String prefix, String bucketName)  throws StorageCloudException  {
        LinkedList<String> find = new LinkedList<>();
        try {
            ObjectListing objectListing = null;
            if (bucketName == null) objectListing = amazonS3.listObjects(new ListObjectsRequest().withBucketName(defaultBucketName).withPrefix(prefix));
            else {
                objectListing = amazonS3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName).withPrefix(prefix));
            }
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
                find.add(objectSummary.getKey());
        } catch (AmazonServiceException e1) {
            throw new ServiceSiteException("AWSS3Exception::" + e1.getMessage());
        } catch (AmazonClientException e2) {
            throw new ClientSiteException("AWSS3Exception::" + e2.getMessage());
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

