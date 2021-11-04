package ua.aharoo.models;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table
public class FileInfo implements Serializable {

    @Id
    @SequenceGenerator(
            name = "fileinfo_sequence",
            sequenceName = "fileinfo_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "fileinfo_sequence"
    )
    private Long fileInfo_id;
    private String filename;
    private Long firstChunk;
    private Long secondChunk;
    private Long thirdChunk;
    private Long fourthChunk;
    private LocalDateTime creationDate;
    private boolean isFileEncrypted;
    private byte[] fileHash;
    private boolean isAdditionalReplicationExists;
    @ManyToOne
    @JoinColumn(name = "dataUnit_id", nullable = false)
    private DataUnit dataUnit;

    public FileInfo() {
    }

    public FileInfo(String filename, Long firstChunk, Long secondChunk,
                    Long thirdChunk, Long fourthChunk, LocalDateTime creationDate,
                    boolean isFileEncrypted, DataUnit dataUnit, byte[] fileHash,
                    boolean isAdditionalReplicationExists) {
        this.filename = filename;
        this.firstChunk = firstChunk;
        this.secondChunk = secondChunk;
        this.thirdChunk = thirdChunk;
        this.fourthChunk = fourthChunk;
        this.creationDate = creationDate;
        this.isFileEncrypted = isFileEncrypted;
        this.dataUnit = dataUnit;
        this.fileHash = fileHash;
        this.isAdditionalReplicationExists = isAdditionalReplicationExists;;
    }

    public boolean isAdditionalReplicationExists() {
        return isAdditionalReplicationExists;
    }

    public void setAdditionalReplicationExists(boolean additionalReplicationExists) {
        isAdditionalReplicationExists = additionalReplicationExists;
    }

    public byte[] getFileHash() {
        return fileHash;
    }

    public void setFileHash(byte[] fileHash) {
        this.fileHash = fileHash;
    }

    public boolean isFileEncrypted() {
        return isFileEncrypted;
    }

    public void setFileEncrypted(boolean fileEncrypted) {
        isFileEncrypted = fileEncrypted;
    }

    public FileInfo(Long fileInfo_id) {
        this.fileInfo_id = fileInfo_id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFirstChunk() {
        return firstChunk;
    }

    public void setFirstChunk(Long firstChunk) {
        this.firstChunk = firstChunk;
    }

    public Long getSecondChunk() {
        return secondChunk;
    }

    public void setSecondChunk(Long secondChunk) {
        this.secondChunk = secondChunk;
    }

    public Long getThirdChunk() {
        return thirdChunk;
    }

    public void setThirdChunk(Long thirdChunk) {
        this.thirdChunk = thirdChunk;
    }

    public Long getFourthChunk() {
        return fourthChunk;
    }

    public void setFourthChunk(Long fourthChunk) {
        this.fourthChunk = fourthChunk;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public DataUnit getDataUnit() {
        return dataUnit;
    }

    public void setDataUnit(DataUnit dataUnit) {
        this.dataUnit = dataUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return isFileEncrypted == fileInfo.isFileEncrypted && isAdditionalReplicationExists == fileInfo.isAdditionalReplicationExists && Objects.equals(filename, fileInfo.filename) && Objects.equals(firstChunk, fileInfo.firstChunk) && Objects.equals(secondChunk, fileInfo.secondChunk) && Objects.equals(thirdChunk, fileInfo.thirdChunk) && Objects.equals(fourthChunk, fileInfo.fourthChunk) && Arrays.equals(fileHash, fileInfo.fileHash) && Objects.equals(dataUnit, fileInfo.dataUnit);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(filename, firstChunk, secondChunk, thirdChunk, fourthChunk, isFileEncrypted, isAdditionalReplicationExists, dataUnit);
        result = 31 * result + Arrays.hashCode(fileHash);
        return result;
    }

    @Override
    public String toString() {
        return "FileInfo_id=" + fileInfo_id
                + ", filename=" + filename
                + ", isFileEncrypted=" + isFileEncrypted
                + ", isAdditionalReplicationExists=" + isAdditionalReplicationExists
                + ", dataUnit=" + dataUnit.getDataUnitName();
    }
}
