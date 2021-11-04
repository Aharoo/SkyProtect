package ua.aharoo.core;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SkyProtectDataManager {

    public void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) {
            bw.write(buf);
        }
    }

    public void deleteFileChunks(File filePath,List<String> versions) throws IOException {
        for (int i = 0; i < versions.size(); i++)
            FileUtils.forceDelete(new File(filePath + versions.get(i)));
    }

    public void splitFile(File filepath) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filepath, "r");
        long numSplits = 4;
        long sourceSize = raf.length();
        long bytesPerSplit = sourceSize / numSplits;
        long remainingBytes = sourceSize % numSplits;

        int maxReadBufferSize = 8 * 1024; // 8 Kb
        for (int destIx = 0; destIx < numSplits; destIx++) {
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(filepath + String.valueOf(destIx)));
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
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(filepath + String.valueOf(numSplits - 1),true));
            readWrite(raf, bw, remainingBytes);
            bw.close();
        }
        raf.close();
    }

    public void mergeFile(File filePath, List<String> versionNumbers) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        try {
            if (!filePath.exists()) filePath.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(filePath));

            for (int i = 0; i < 4; i++) {
                File filepath = new File(filePath + versionNumbers.get(i));
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
            out.flush();
            out.close();
            in.close();
            System.gc();
        }
    }
}
