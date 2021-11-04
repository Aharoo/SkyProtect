package drivers;

import com.github.sardine.DavResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ua.aharoo.drivers.LocalCloudDriver;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class LocalCloudDriverTest {
    private String bucketName = "testbucket134";
    private byte[] buffer = "Hello,amazon test!".getBytes();
    private String fileId = "TestFile";
    private LocalCloudDriver nextCloud = new LocalCloudDriver("cloud5");

    @Before
    public void init() throws StorageCloudException {
        nextCloud.initSession();
    }


    @After
    public void delete() throws StorageCloudException {
    //    nextCloud.deleteData(bucketName,fileId);
    }

    @Test
    public void mustUploadAndDownloadFile() throws StorageCloudException {
        nextCloud.uploadData(bucketName,buffer,fileId);
        boolean isExists = Arrays.equals(nextCloud.downloadData(bucketName, fileId), buffer);
        Assert.assertTrue(isExists);
    }

    @Test
    public void mustUploadAndFindFile() throws StorageCloudException, IOException {
        File file = new File("D:/Buffer.txt");
        if (!file.exists()) file.createNewFile();
        nextCloud.downloadData("aharoo1","aharooreplicationvalue1000",file);

    }

}
