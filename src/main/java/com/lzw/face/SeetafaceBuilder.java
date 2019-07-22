package com.lzw.face;

import com.lzw.face.bean.FaceIndex;
import com.seetaface2.SeetaFace2JNI;
import com.seetaface2.model.SeetaImageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;


public class SeetafaceBuilder {
    private static Logger logger = LoggerFactory.getLogger(SeetafaceBuilder.class);
    private volatile static SeetaFace2JNI seeta = null;

    /**
     * 状态枚举
     */
    public enum FacedbStatus {
        READY, LOADING, OK, INACTIV;
    }

    private volatile static FacedbStatus face_db_status = FacedbStatus.READY;

    /**
     * 构建seetaface
     * @return
     */
    public static SeetaFace2JNI build() {
        if (seeta == null) {
            synchronized (SeetafaceBuilder.class) {
                if (seeta != null) {
                    return seeta;
                }
                init();
            }
        }
        return seeta;
    }

    /**
     * 返回人脸数据库状态
     * @return
     */
    public static FacedbStatus getFaceDbStatus() {
        return face_db_status;
    }

    private static void init() {
        Properties prop = getConfig();
        String separator = System.getProperty("path.separator");
        String sysLib = System.getProperty("java.library.path");
        if (sysLib.endsWith(separator)) {
            System.setProperty("java.library.path", sysLib + prop.getProperty("libs.path", ""));
        } else {
            System.setProperty("java.library.path", sysLib + separator + prop.getProperty("libs.path", ""));
        }
        try {//使java.library.path生效
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] libs = prop.getProperty("libs", "").split(",");
        for (String lib : libs) {
            logger.debug("load library: {}", lib);
            System.loadLibrary(lib);
        }
        String bindata = prop.getProperty("bindata.dir");
        logger.debug("bindata dir: {}", bindata);
        seeta = new SeetaFace2JNI();
        seeta.initModel(bindata);
        logger.info("Seetaface init completed!!!");
        face_db_status = FacedbStatus.OK;
    }

    /**
     * 注册人脸数据到seetaface中 用于人脸搜索
     * @param key  人脸照片唯一标识
     * @param face 人脸照片
     * @return 当前人脸在人脸库的下标 大于0成功 -1表示超出限制 500张
     * @throws IOException
     */
    private static int register(String key, FaceIndex face) {
        SeetaImageData imageData = new SeetaImageData(face.getWidth(), face.getHeight(), face.getChannel());
        imageData.data = face.getImgData();
        int index = seeta.register(imageData);
        if (index < 0) {
            logger.info("Register face fail: key={}, index={}", key, index);
            return index;
        }
        logger.info("Register face success: key={}, index={}", key, index);
        return index;
    }

    private static Properties getConfig() {
        Properties properties = new Properties();
        String location = "classpath:/seetaface.properties";
        InputStream is = null;
        try  {
            is = new DefaultResourceLoader().getResource(location).getInputStream();
            properties.load(is);
            logger.debug("seetaface config: {}", properties.toString());
        } catch (IOException ex) {
            logger.error("Could not load property file:" + location, ex);
        }finally {
            try {
                if (null != is){
                    is.close();
                }
            }catch (Exception e){

            }
        }
        return properties;
    }
}
