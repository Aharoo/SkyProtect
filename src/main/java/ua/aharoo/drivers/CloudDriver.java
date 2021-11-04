package ua.aharoo.drivers;

import ua.aharoo.exceptions.StorageCloudException;

import java.io.*;
import java.util.LinkedList;
import java.util.Properties;

public interface CloudDriver {

    // Загрузка файла на облако
    String uploadData(String bucket, byte[] data,String fileId) throws StorageCloudException;

    String uploadData(String bucketName, File filePath, String fileId) throws StorageCloudException;

    // Загрузка файла с облака
    byte[] downloadData(String bucket, String fileId) throws StorageCloudException;

    byte[] downloadData(String bucket, String fileId, File filePath) throws StorageCloudException;

    // Удаление файла с облака
    boolean deleteData(String bucket,String fileId) throws StorageCloudException;

    // Удаление всех файлов и контейнера на облаке
    boolean deleteContainer(String bucket,String[] namesToDelete) throws StorageCloudException;

    // Инициализация драйвера
    String initSession() throws StorageCloudException;

    // Инициализация драйвера (тестовые 4 облака AWS)
    String initSession(boolean useTestModel) throws StorageCloudException;

    // Вывод списка файлов
    LinkedList<?> listNames(String prefix, String bucket) throws StorageCloudException;

    // Получение сессии
    String getSessionKey();

    // Получение номера облака
    String getDriverId();

    // В случае отсутствия названия контейнера - генерация случайного названия
    default String getBucketName(String defaultBucketName,String property) throws FileNotFoundException {
        String path = "src/main/resources/bucket_name.properties";
        FileInputStream fis;
        try {
            fis = new FileInputStream(path);
            Properties props = new Properties();
            props.load(fis);
            fis.close();
            String bucketName = props.getProperty(property);
            if(bucketName.length() == 0){
                char[] randName = new char[10];
                for(int i = 0; i < 10; i++){
                    char rand = (char)(Math.random() * 26 + 'a');
                    randName[i] = rand;
                }
                defaultBucketName = defaultBucketName.concat(new String(randName));
                props.setProperty(property, defaultBucketName);
                props.store(new FileOutputStream(path),"change");
            }else{
                defaultBucketName = bucketName;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
