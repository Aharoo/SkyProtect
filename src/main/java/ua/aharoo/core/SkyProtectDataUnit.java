package ua.aharoo.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class SkyProtectDataUnit implements Serializable {
    private static final long serialVersionUID = 4571313203675873816L;
    public long lastVersionNumber;
    public long lastVersionOfFirstChunk;
    public long lastVersionOfSecondChunk;
    public long lastVersionOfThirdChunk;
    public long lastVersionOfFourthChunk;
    public String dataUnitId;
    private HashMap<String,String> idsCache; // кэш для облачных контейнеров
    public HashMap<String,Long> cloudVersions; // кэш для версий облачных данных
    public HashMap<String,String> previousMetaData;
    private long currentHighestVersion = -1;
    public Long highestCloudVersion = null;
    private String bucketName = null;

    public SkyProtectDataUnit(String dataUnitId){
        this.dataUnitId = dataUnitId;
        this.lastVersionNumber = -1;
        this.idsCache = new HashMap<>();
        this.cloudVersions = new HashMap<>();
        this.previousMetaData = new HashMap<>();
        this.lastVersionOfFirstChunk = -1;
        this.lastVersionOfSecondChunk = -1;
        this.lastVersionOfThirdChunk = -1;
        this.lastVersionOfFourthChunk = -1;

    }

    public String getMetaDataFileName(){return dataUnitId + "metadata";}

    public String getGivenVersionValueDataFileName(String vn){return dataUnitId + "value" + vn;}

    public String getContainerName(){
        return (dataUnitId + "container").toLowerCase();
        // из-за политики AWS название контейнеров будут начинаться с маленькой буквы
    }

    public String getContainerId(String cloudId){
        if(idsCache.get(cloudId) == null)
            for (int i = 0; i < idsCache.size(); i++)
                if (idsCache.get(i) != null)
                    return idsCache.get(i);

        return idsCache.get(cloudId);
    }

    public void setContainerId(String cloudId,String cid){
        if(!idsCache.containsKey(cloudId) && cid != null){
            idsCache.put(cloudId,cid);
        } else if (cid == null){
            for (int i = 0; i < idsCache.size(); i++){
                if (idsCache.get(i) != null){
                    idsCache.put(cloudId,idsCache.get(i));
                }
            }
        }
    }

    public Long getCloudVersion(String cloudId){return cloudVersions.get(cloudId);}

    public void setCloudVersion(String cloudId,long version){
        cloudVersions.put(cloudId,version);
        if (version > currentHighestVersion) currentHighestVersion = version;
        if (highestCloudVersion == null || currentHighestVersion > highestCloudVersion) highestCloudVersion = currentHighestVersion;
    }

    public Long getMaxVersion(){
//        if (cloudVersions.size() >= n - f) return highestCloudVersion;
        return highestCloudVersion;
    }

    public void setLastVersionOfFirstChunk(long lastVersionOfFirstChunk) {
        this.lastVersionOfFirstChunk = lastVersionOfFirstChunk;
    }

    public void setLastVersionOfSecondChunk(long lastVersionOfSecondChunk) {
        this.lastVersionOfSecondChunk = lastVersionOfSecondChunk;
    }

    public void setLastVersionOfThirdChunk(long lastVersionOfThirdChunk) {
        this.lastVersionOfThirdChunk = lastVersionOfThirdChunk;
    }

    public void setLastVersionOfFourthChunk(long lastVersionOfFourthChunk) {
        this.lastVersionOfFourthChunk = lastVersionOfFourthChunk;
    }

    public String getBucketName(){return this.bucketName;}

    public void clearAllCaches(){
        idsCache.clear();
        cloudVersions.clear();
        previousMetaData.clear();
        highestCloudVersion = null;
        currentHighestVersion = -1;
    }

    public String toString(){
        return "SkyProtectRegister: " + dataUnitId + " # "
                + getContainerName() + "\n"
                + Arrays.toString(idsCache.keySet().toArray()) + "\n"
                + Arrays.toString(idsCache.values().toArray()) + "\n"
                + getMetaDataFileName()
                + "lastVN = " + lastVersionNumber;
    }

    public String getDataUnitId(){
        return this.dataUnitId;
    }
}
