package ua.aharoo.models;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table
public class DataUnit implements Serializable {

    @Id
    @SequenceGenerator(
            name = "dataunit_sequence",
            sequenceName = "dataunit_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "dataunit_sequence"
    )
    private Long dataUnit_id;
    @Column(unique = true)
    private String dataUnitName;
    private LocalDateTime creationDate;
    private boolean onlyOnPrivateCloud;
    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "dataUnit"
    )
    private List<FileInfo> fileInfos = new ArrayList<>();

    public DataUnit(String dataUnitName, boolean onlyOnPrivateCloud) {
        this.dataUnitName = dataUnitName;
        this.onlyOnPrivateCloud = onlyOnPrivateCloud;
    }

    public DataUnit() {
    }

    public Long getDataUnit_id() {
        return dataUnit_id;
    }

    public void setDataUnit_id(Long dataUnit_id) {
        this.dataUnit_id = dataUnit_id;
    }

    public DataUnit(Long dataUnit_id) {
        this.dataUnit_id = dataUnit_id;
    }

    public String getDataUnitName() {
        return dataUnitName;
    }

    public void setDataUnitName(String dataUnitName) {
        this.dataUnitName = dataUnitName;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isOnlyOnAWS() {
        return onlyOnPrivateCloud;
    }

    public void setOnlyOnAWS(boolean onlyOnPrivateCloud) {
        this.onlyOnPrivateCloud = onlyOnPrivateCloud;
    }

    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(List<FileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataUnit dataUnit = (DataUnit) o;
        return onlyOnPrivateCloud == dataUnit.onlyOnPrivateCloud && dataUnitName.equals(dataUnit.dataUnitName) && Objects.equals(fileInfos, dataUnit.fileInfos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataUnitName, onlyOnPrivateCloud, fileInfos);
    }

    @Override
    public String toString() {
        return "DataUnit_id=" + dataUnit_id +
                ", dataUnitName='" + dataUnitName +
                ", creationDate=" + creationDate +
                ", onlyOnPrivateCloud=" + onlyOnPrivateCloud;
    }
}
