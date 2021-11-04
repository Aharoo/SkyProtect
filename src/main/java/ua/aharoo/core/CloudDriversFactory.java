package ua.aharoo.core;

import ua.aharoo.drivers.*;

public class CloudDriversFactory {

    public static CloudDriver getDriver(String type,String driverId,String accessKey,String secretKey){
        CloudDriver driver = null;

        if (type.equals("AMAZON-S3")) driver = new AmazonDriver(driverId,accessKey,secretKey);
        else if (type.equals("GOOGLE")) driver = new GoogleDriver(driverId);
        else if (type.equals("AZURE")) driver = new AzureDriver(driverId);
        else if (type.equals("LOCALE")) driver = new LocalCloudDriver(driverId);

        return driver;
    }

}
