package ua.aharoo.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import ua.aharoo.models.DataUnit;
import ua.aharoo.models.FileInfo;

import java.util.List;

public class DataUnitAndFileInfoDAO {

    private Configuration configuration = new Configuration().configure().addAnnotatedClass(DataUnit.class).addAnnotatedClass(FileInfo.class);
    private SessionFactory sessionFactory = configuration.buildSessionFactory();
    private Session session = null;
    private Transaction tx = null;

    public void saveDataUnit(DataUnit dataUnit,FileInfo fileInfo){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("from DataUnit where dataUnitName = :name").setParameter("name",dataUnit.getDataUnitName());
            Object queryResult = query.uniqueResult();
            if (queryResult != null){
                DataUnit existingDataUnit = (DataUnit) queryResult;
                fileInfo.setDataUnit(existingDataUnit);
            } else {
                session.save(dataUnit);
                fileInfo.setDataUnit(dataUnit);
            }
            session.save(fileInfo);
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public void deleteDataUnit(String dataUnitName){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("delete DataUnit where dataUnitName = :name");
            query.setParameter("name",dataUnitName);
            query.executeUpdate();
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public void deleteAllDataUnits(){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List<DataUnit> dataUnits = session.createQuery("from DataUnit ").list();
            for (DataUnit dataUnit : dataUnits) session.delete(dataUnit);
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public void getAllDataUnits(){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List<DataUnit> dataUnits = session.createQuery("from DataUnit ").list();
            for (DataUnit dataUnit : dataUnits) System.out.println(dataUnit);
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public void getAllFiles() {
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List<FileInfo> fileInfos = session.createQuery("from FileInfo ").list();
            for (FileInfo fileInfo : fileInfos) System.out.println(fileInfo);
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public DataUnit getDataUnit(String dataUnitName){
        DataUnit dataUnit = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("SELECT DataUnit from DataUnit where dataUnitName = :dataUnitName").setParameter("dataUnitName",dataUnitName);
            Object buffer = query.uniqueResult();
            dataUnit = (DataUnit) buffer;
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
        return dataUnit;
    }

    public FileInfo getFileInfo(String filename){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("from FileInfo where filename = :filename").setParameter("filename",filename);
            FileInfo fileInfo = (FileInfo) query.uniqueResult();
            session.flush();
            tx.commit();
            return fileInfo;
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
        return null;
    }

    public void deleteFile(FileInfo fileInfo){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("delete FileInfo where filename = :name");
            query.setParameter("name",fileInfo.getFilename());
            query.executeUpdate();
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }

    public void deleteAllFiles(String dataUnitName){
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List<DataUnit> dataUnit = session.createQuery("from DataUnit where dataUnitName = :dataUnitName").setParameter("dataUnitName",dataUnitName).list();
            List<FileInfo> fileInfos = session.createQuery("FROM FileInfo").list();
            for (FileInfo fileInfo : fileInfos)
                if (fileInfo.getDataUnit().getDataUnitName() == dataUnit.get(0).getDataUnitName()) //TODO: Обратить внимание,удаляются ли все файлы
                    session.delete(fileInfo);
            session.flush();
            tx.commit();
        } catch (Exception e){
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }
    }
}
