package ua.aharoo.utils;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SkyProtectKeyLoader {
    private String path;
    private PrivateKey prk;

    public SkyProtectKeyLoader(String configHome) {
        if (configHome == null || configHome.equals("")) {
            path = "config" + System.getProperty("file.separator") + "SkyProtectKeys"
                    + System.getProperty("file.separator");
        } else {
            path = configHome + System.getProperty("file.separator") + "SkyProtectKeys"
                    + System.getProperty("file.separator");
        }
    }

    public PublicKey loadPublicKey() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path + "publickey0"));
        PublicKey puk = (PublicKey) ois.readObject();
        return puk;
    }

    public PrivateKey loadPrivateKey() throws Exception {
        if (prk == null) {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(path + "privatekey0"));
            prk = (PrivateKey) ois.readObject();
        }
        return prk;
    }

    public void generateRSAKeyPair(int id) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair kp = keyGen.generateKeyPair();
        PublicKey puk = kp.getPublic();
        PrivateKey prk = kp.getPrivate();
        saveToFiles(puk, prk);
        System.out.println("KeyPair number " + id + " created in config/keys directory.");
    }

    private void saveToFiles(PublicKey puk, PrivateKey prk) throws Exception {
        String path = "config" + System.getProperty("file.separator") +
                "SkyProtectKeys" + System.getProperty("file.separator");
        File filepath = new File(path);
        if (!filepath.exists()) {
            if (!filepath.mkdirs()) {
                throw new RuntimeException("Cannot create config dirs!");
            }
        }
        File pukfile = new File(path + "publickey");
        if (pukfile.exists()) {
            if (!pukfile.canWrite()) {
                throw new RuntimeException("File cannot be written!");
            }
        } else {
            pukfile.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(pukfile));
        oos.writeObject(puk);
        oos.flush();
        oos.close();
        File prkfile = new File(path + "privatekey");
        if (prkfile.exists()) {
            if (!prkfile.canWrite()) {
                throw new RuntimeException("File cannot be written!");
            }
        } else {
            prkfile.createNewFile();
        }
        oos = new ObjectOutputStream(
                new FileOutputStream(prkfile));
        oos.writeObject(prk);
        oos.flush();
        oos.close();
    }
}
