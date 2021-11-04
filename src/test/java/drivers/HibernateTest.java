package drivers;


import ua.aharoo.dao.DataUnitAndFileInfoDAO;
import ua.aharoo.utils.AESEncryptionImpl;

public class HibernateTest {

    public static byte[] addTestWord(byte[] buffer){
        String test = "encrypted";
        byte[] newBuffer = new byte[buffer.length + test.length()];
        System.arraycopy(buffer,0,newBuffer,0,buffer.length);
        System.arraycopy(test.getBytes(),0,newBuffer,buffer.length,test.length());
        return newBuffer;
    }

    public static byte[] removeTestWord(byte[] buffer){
        String test = "encrypted";
        byte[] newBuffer = new byte[buffer.length - test.length()];
        System.arraycopy(buffer,0,newBuffer,0,newBuffer.length);
        return newBuffer;
    }

    public static void main(String[] args) throws Exception {
//        AESEncryptionImpl encryption = new AESEncryptionImpl();
//        byte[] buffer = "HelloWorld".getBytes();
//        byte[] encryptedBuffer = encryption.encrypt(buffer, encryption.loadSecretKey());
//
//        byte[] encryptedBufferWithWord = addTestWord(encryptedBuffer);
//
//        String bufferToString = new String(encryptedBufferWithWord);
//        if(bufferToString.endsWith("encryted")){
//            encryptedBufferWithWord = removeTestWord(encryptedBufferWithWord);
//        }
//
//        byte[] decryptedBuffer = encryption.decrypt(encryptedBufferWithWord, encryption.loadSecretKey());
//        String decodedBuffer = new String(decryptedBuffer);
//        System.out.println(decodedBuffer);
    }
}
