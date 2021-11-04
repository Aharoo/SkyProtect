package drivers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ua.aharoo.drivers.AzureDriver;
import ua.aharoo.drivers.GoogleDriver;
import ua.aharoo.exceptions.StorageCloudException;

import java.util.Arrays;

public class AzureDriverTest {
    private String bucketName = "aharoo1";
    private AzureDriver azureDriver = new AzureDriver("aharoo");
    private String fileId = "testAzure";

    @Before
    public void init() throws StorageCloudException {
        azureDriver.initSession();
    }

    @After
    public void delete() throws StorageCloudException {
        azureDriver.deleteData(bucketName,fileId);
        azureDriver.deleteContainer(bucketName,fileId.split(""));
    }

    @Test
    public void mustUploadAndDownloadFile() throws StorageCloudException {
        byte[] buffer = "Hello World".getBytes();
        azureDriver.uploadData(bucketName,buffer,"testAzure");
        boolean isExists = Arrays.equals(buffer,azureDriver.downloadData(bucketName,fileId));
        Assert.assertTrue(isExists);
    }
}
