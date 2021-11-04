package ua.aharoo.utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AESEncryptionImpl {


    public SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256,new SecureRandom());
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public void encrypt(SecretKey key,File inputFile,File outputFile)
            throws InvalidAlgorithmParameterException, InvalidKeyException,
            NoSuchPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException {

        byte[] bufferIV = {-13, -20, 75, 61, 90, 40, 100, 19, 8, -24, 31, 72, 8, 41, 12, -39};

        IvParameterSpec ivParameterSpec = new IvParameterSpec(bufferIV);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,key,ivParameterSpec);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try{
            in = new BufferedInputStream(new FileInputStream(inputFile));
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
            byte[] ibuf = new byte[1024];
            int len;
            while((len = in.read(ibuf)) != -1){
                byte[] obuf = cipher.update(ibuf,0,len);
                if(obuf != null) out.write(obuf);
            }
            byte[] obuf = cipher.doFinal();
            if (obuf != null) out.write(obuf);
        }  catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void decrypt(SecretKey key, File inputFile, File outputFile)
            throws IllegalBlockSizeException, BadPaddingException,
            NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {

        byte[] bufferIV = {-13, -20, 75, 61, 90, 40, 100, 19, 8, -24, 31, 72, 8, 41, 12, -39};

        IvParameterSpec ivParameterSpec = new IvParameterSpec(bufferIV);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(inputFile));
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
            byte[] ibuf = new byte[1024];
            int len;
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,key,ivParameterSpec);
            while((len = in.read(ibuf)) != -1){
                byte[] obuf = cipher.update(ibuf,0,len);
                if (obuf != null) out.write(obuf);
            }
            byte[] obuf = cipher.doFinal();
            if (obuf != null) out.write(obuf);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void saveToFiles(SecretKey key) throws Exception {
        String path = "config" + System.getProperty("file.separator") +
                "SkyProtectKeys" + System.getProperty("file.separator");
        File filepath = new File(path);
        if (!filepath.exists()) {
            if (!filepath.mkdirs()) {
                throw new RuntimeException("Cannot create config dirs!");
            }
        }
        File pukfile = new File(path + "secretkey");
        if (pukfile.exists()) {
            if (!pukfile.canWrite()) {
                throw new RuntimeException("File cannot be written!");
            }
        } else {
            pukfile.createNewFile();
        }
        System.out.println("Secretkey was created in " + path);
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(pukfile));
        oos.writeObject(key);
        oos.flush();
        oos.close();
    }

    public SecretKey loadSecretKey() throws Exception {
        String path = "config" + System.getProperty("file.separator") + "SkyProtectKeys" + System.getProperty("file.separator") + "secretkey";
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path));
        SecretKey secretKey = (SecretKey) ois.readObject();
        return secretKey;
    }

}
