package drivers;

import org.apache.commons.io.IOUtils;
import ua.aharoo.core.SkyProtectDataUnit;
import ua.aharoo.core.SkyProtectManager;
import ua.aharoo.drivers.LocalCloudDriver;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Split {

    private static File filePath = new File("src/main/resources/TestFile");

    static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) {
            bw.write(buf);
        }
    }

    public static void deleteFileChunks(String filename){
        System.out.println("Deleting chunks");
        for (int i = 0; i < 4; i++){
            String[] tokens = filename.split("\\.(?=[^\\.]+$)");
            File file = new File(filePath + "/" + tokens[0] + i + "." + tokens[1]);
            file.delete();
        }
    }

    public static void splitFile(String filename) throws IOException {
        File file = new File(filePath + "/" + filename);
        if (file.exists()) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long numSplits = 4; //from user input, extract it from args
            long sourceSize = raf.length();
            long bytesPerSplit = sourceSize / numSplits;
            long remainingBytes = sourceSize % numSplits;
            String[] tokens = file.getName().split("\\.(?=[^\\.]+$)");

            int maxReadBufferSize = 512 * 1024; // 512 Kb
            for (int destIx = 0; destIx < numSplits; destIx++) {
                BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(filePath + "/" + tokens[0] + destIx + "." + tokens[1]));
                if (bytesPerSplit > maxReadBufferSize) {
                    long numReads = bytesPerSplit / maxReadBufferSize;
                    long numRemainingRead = bytesPerSplit % maxReadBufferSize;
                    for (int i = 0; i < numReads; i++) {
                        readWrite(raf, bw, maxReadBufferSize);
                    }
                    if (numRemainingRead > 0) {
                        readWrite(raf, bw, numRemainingRead);
                    }
                } else {
                    readWrite(raf, bw, bytesPerSplit);
                }
                bw.close();
            }
            if (remainingBytes > 0) {
                BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream("split." + (numSplits + 1)));
                readWrite(raf, bw, remainingBytes);
                bw.close();
            }
            raf.close();
        } else throw new IllegalStateException("File doesn't exists");
    }

    public static void mergeFile(String filename) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        File filePath = new File("src/main/resources/TestFile");
        try {
            out = new BufferedOutputStream(new FileOutputStream(filePath + "/" + filename));

            String[] tokens = filename.split("\\.(?=[^\\.]+$)");
            for (int i = 0; i < 4; i++) {
                File filepath = new File(filePath + "/" + tokens[0] + i + "." + tokens[1]);
                try {
                    if (filepath.exists()) {
                        in = new BufferedInputStream(new FileInputStream(filepath));
                        byte[] buffer = IOUtils.toByteArray(in);
                        out.write(buffer);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            out.close();
            in.close();
        }
    }


    public static void mergeFile2(String filename) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        File filePath = new File("src/main/resources/TestFile");
        try {
            out = new BufferedOutputStream(new FileOutputStream(filePath + "/" + filename));

            String[] tokens = filename.split("\\.(?=[^\\.]+$)");
            for (int i = 0; i < 4; i++) {
                File filepath = new File(filePath + "/" + tokens[0] + i + "." + tokens[1]);
                try {
                    if (filepath.exists()) {
                        in = new BufferedInputStream(new FileInputStream(filepath));
                        byte[] buffer = new byte[8 * 1024];
                        int read;
                        while ((read = in.read(buffer, 0, buffer.length)) != -1)
                            out.write(buffer, 0, read);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            out.close();
            in.close();
        }
    }


    public static void main(String[] args) throws IOException {
//            BufferedInputStream in = null;
//            ByteArrayOutputStream out = null;
//            try {
//                byte[] buffer = new byte[1024];
//                in = new BufferedInputStream(new FileInputStream(file));
//                out = new ByteArrayOutputStream();
//                int read = 0;
//                while ((read = in.read(buffer)) != buffer.length)
//                    out.write(buffer, 0, buffer.length);
//                return MessageDigest.getInstance("SHA-256").digest(buffer);
//            } catch (IOException | NoSuchAlgorithmException e){
//                e.printStackTrace();
//            } finally {
//                try {
//                    out.close();
//                    in.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            return null;
//        }
    }

}
