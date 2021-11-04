package drivers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ua.aharoo.drivers.GoogleDriver;
import ua.aharoo.exceptions.StorageCloudException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GoogleDriverTest {

    private String bucketName = "aharoo";
    private GoogleDriver googleDriver = new GoogleDriver("cloud2");
    private String fileId = "testGoogle";
    private File file = new File("D:/Document.docx");

    @Before
    public void init() throws StorageCloudException {
        googleDriver.initSession();
    }

    @After
    public void delete() throws StorageCloudException {
     //   googleDriver.deleteContainer(bucketName,fileId.split(""));
    }

    @Test
    public void mustUploadFile() throws StorageCloudException {
        googleDriver.uploadData(bucketName,file,fileId);
    }

    @Test
    public void mustDownloadFile() throws StorageCloudException, IOException {
        File bufferFile = new File("D:/Cloud-test.txt");
        if (!bufferFile.exists()) bufferFile.createNewFile();
        googleDriver.downloadData(bucketName,fileId,bufferFile);
       // if (bufferFile.exists()) Assert.assertTrue(true);
    }

}
