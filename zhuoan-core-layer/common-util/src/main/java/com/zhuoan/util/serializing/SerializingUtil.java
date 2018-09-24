package com.zhuoan.util.serializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * SerializingUtil
 * 功能简述: 序列化工具类，负责byte[]和Object之间的相互转换.
 *
 * @author weixiang.wu
 * @date 2018 -04-16 11:01
 */
public class SerializingUtil {

    private final static Logger logger = LoggerFactory.getLogger(SerializingUtil.class);

    /**
     * 功能简述: 对实体Bean进行序列化操作.
     *
     * @param source 待转换的实体
     * @return 转换之后的字节数组 byte [ ]
     * @throws Exception
     */
    public static byte[] serialize(Object source) {
        ByteArrayOutputStream byteOut = null;
        ObjectOutputStream ObjOut = null;
        try {
            byteOut = new ByteArrayOutputStream();
            ObjOut = new ObjectOutputStream(byteOut);
            ObjOut.writeObject(source);
            ObjOut.flush();
        }
        catch (IOException e) {
            logger.error(source.getClass().getName()
                + " serialized error !", e);
        }
        finally {
            try {
                if (null != ObjOut) {
                    ObjOut.close();
                }
            }
            catch (IOException e) {
                ObjOut = null;
            }
        }
        return byteOut.toByteArray();
    }

    /**
     * 功能简述: 将字节数组反序列化为实体Bean.
     *
     * @param source 需要进行反序列化的字节数组
     * @return 反序列化后的实体Bean object
     * @throws Exception
     */
    public static Object deserialize(byte[] source) {
        ObjectInputStream ObjIn = null;
        Object retVal = null;
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(source);
            ObjIn = new ObjectInputStream(byteIn);
            retVal = ObjIn.readObject();
        }
        catch (Exception e) {
            logger.error("deserialized error  !", e);
        }
        finally {
            try {
                if(null != ObjIn) {
                    ObjIn.close();
                }
            }
            catch (IOException e) {
                ObjIn = null;
            }
        }
        return retVal;
    }

    /**
     * 对象序列化为字符串
     * @param obj
     * @return
     */
    public static String objectSerializable(Object obj){
        String serStr = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            serStr = byteArrayOutputStream.toString("ISO-8859-1");
            serStr = java.net.URLEncoder.encode(serStr, "UTF-8");

            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serStr;
    }

    /**
     * 字符串反序列化为对象
     * @param serStr
     * @return
     */
    public static Object objectDeserialization(String serStr){
        Object newObj = null;
        try {
            String redStr = java.net.URLDecoder.decode(serStr, "UTF-8");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(redStr.getBytes("ISO-8859-1"));
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            newObj = objectInputStream.readObject();
            objectInputStream.close();
            byteArrayInputStream.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newObj;
    }
}
