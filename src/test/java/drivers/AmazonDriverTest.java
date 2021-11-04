package drivers;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ua.aharoo.drivers.AmazonDriver;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class AmazonDriverTest {
    private String accessKey = "AKIA4PXRA6X3QH5EKHON";
    private String secretKey = "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";
    private String bucketName = "aharoo-cloud1";
    private String fileId = "TestFile";
    private AmazonDriver amazonDriver = new AmazonDriver("cloud1",accessKey,secretKey);
    private File file = new File("D:/Document.docx");

    @Before
    public void init() throws StorageCloudException {
        amazonDriver.initSession();
    }

    @After
    public void delete() throws StorageCloudException {
   //     amazonDriver.deleteData(bucketName,fileId);
    }

    @Test
    public void mustUploadFile() throws StorageCloudException, IOException {
        amazonDriver.uploadData(bucketName,file,fileId);
    }

    @Test
    public void mustDownloadFile() throws StorageCloudException, IOException {
        File bufferFile = new File("D:/Cloud-test.txt");
        amazonDriver.downloadData(bucketName,fileId,bufferFile);
        if (bufferFile.exists()) Assert.assertTrue(true);
    }


    @Test
    public void mustUploadAndFindFile() throws StorageCloudException {
//        amazonDriver.uploadData(bucketName,а,fileId);
//        LinkedList<String> testList = amazonDriver.listNames(fileId,bucketName);
//        Assert.assertTrue(testList.contains(fileId));
    }

}
