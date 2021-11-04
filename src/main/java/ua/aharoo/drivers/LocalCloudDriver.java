package ua.aharoo.drivers;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.google.cloud.storage.BlobInfo;
import org.apache.commons.io.IOUtils;
import ua.aharoo.exceptions.StorageCloudException;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class LocalCloudDriver implements CloudDriver {

    private static String PATH = "http://localhost:8080/remote.php/dav/files/aharoo/";
    private String driverId;
    private String defaultBucketName = "aharoo";
    private String property = "localBucketName";
    private Sardine nextCloud;

    public LocalCloudDriver(String driverId) {
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

        if (this.driverId.equals("cloud5")) this.defaultBucketName = this.defaultBucketName.concat("-cloud5");
    }

    @Override
    public String uploadData(String bucketName, byte[] data, String fileId) throws StorageCloudException {
        try {
            if(!nextCloud.exists(PATH + defaultBucketName)) nextCloud.createDirectory(PATH + defaultBucketName);
            nextCloud.put(PATH + defaultBucketName + "/" + fileId,data);
            return fileId;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileId;
    }

    @Override
    public String uploadData(String bucketName, File filePath, String fileId) throws StorageCloudException {

        try{
            if(!nextCloud.exists(PATH + defaultBucketName)) nextCloud.createDirectory(PATH + defaultBucketName);
            LocalThreadUpload thread = new LocalThreadUpload(fileId,filePath);
            thread.start();
            return fileId;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileId;
    }

    private class LocalThreadUpload extends Thread{
        private String fileId;
        private File filePath;

        public LocalThreadUpload(String fileId,File filePath){
            this.fileId = fileId;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            try {
                InputStream in = new FileInputStream(filePath);
                nextCloud.put(PATH + defaultBucketName + "/" + fileId,in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] downloadData(String bucketName, String fileId) throws StorageCloudException {
        try {
            if(!nextCloud.exists(PATH + defaultBucketName)) nextCloud.createDirectory(PATH + defaultBucketName);
            InputStream in = nextCloud.get(PATH + defaultBucketName + "/" + fileId);
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] downloadData(String bucket, String fileId, File filePath) throws StorageCloudException {
        try {
            if (!filePath.exists()) filePath.createNewFile();
            if (!nextCloud.exists(PATH + defaultBucketName)) nextCloud.createDirectory(PATH + defaultBucketName);
            FileOutputStream out = new FileOutputStream(filePath);
            InputStream in = nextCloud.get(PATH + defaultBucketName + "/" + fileId);
            in.transferTo(out);
            out.close();
            in.close();
            return fileId.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.gc();
        }
        return new byte[0];
    }

//    private class LocalThreadDownload extends Thread{
//        private String fileId;
//        private File filePath;
//
//        public LocalThreadDownload(String fileId,File filePath){
//            this.fileId = fileId;
//            this.filePath = filePath;
//        }
//        @Override
//        public void run() {
//            try {
//                FileOutputStream out = new FileOutputStream(filePath);
//                InputStream in = nextCloud.get(PATH + defaultBucketName + "/" + fileId);
//                in.transferTo(out);
//                out.close();
//                in.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    @Override
    public boolean deleteData(String bucketName, String fileId) throws StorageCloudException {
        try {
            if(nextCloud.exists(PATH + defaultBucketName + "/" + fileId)) {
                nextCloud.delete(PATH + defaultBucketName + "/" + fileId);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteContainer(String bucketName, String[] namesToDelete) throws StorageCloudException {
        if (bucketName != null) {
            for (String fileId : namesToDelete) {
                deleteData(bucketName, fileId);
            }
            return true;
        }

        return false;
    }

    @Override
    public String initSession() throws StorageCloudException {
        nextCloud = SardineFactory.begin("aharoo","a19041999");
        return "sid";
    }

    @Override
    public String initSession(boolean useModel) throws StorageCloudException {
        nextCloud = SardineFactory.begin("aharoo","a19041999");
        return "sid";
    }

    @Override
    public LinkedList<DavResource> listNames(String prefix, String bucketName) throws StorageCloudException {
        LinkedList<DavResource> list = new LinkedList<>();
        try {
            List<DavResource> bufferList = nextCloud.list(PATH);
            for (DavResource resource : bufferList) list.add(resource);
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
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
