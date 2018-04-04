package com.zhuoan.biz.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 系统常量bean
 *
 * @version 0.1
 * @Copyright Copyright (c) 2015
 * @Company zhouan
 */
public class Dto {

    private final static Logger logger = LoggerFactory.getLogger(Dto.class);
//	public static String PLATFORM="";//平台号

    /**
     * 判断String是否为空
     *
     * @param values the values
     * @return true or false (等于空返回true   不等于看返回false)
     */
    public static boolean stringIsNULL(String values){
		
		if(values==null||"".equals(values)||"null".equals(values)||"undefined".equals(values)){
			return true;
		}
		return false;
		
	}

    /**
     * 根据长度随机生成
     * 26英文字和0~9随机生成
     *
     * @param length the length
     * @return entNum string
     */
    public static String getEntNumCode(int length){
		String val = "";  
        Random random = new Random();  
        for (int i = 0; i < length; i++) {  
            // 输出字母还是数字  
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";   
            // 字符串  
            if ("char".equalsIgnoreCase(charOrNum)) {  
                // 取得大写字母还是小写字母  
                int choice = 65;   
                val += (char) (choice + random.nextInt(26));  
            } else if ("num".equalsIgnoreCase(charOrNum)) { // 数字  
                val += String.valueOf(random.nextInt(10));  
            }  
        }  
       return val;
	}

    /**
     * 1.判断是否为空
     *
     * @param obj the obj
     * @return the boolean
     */
    public static boolean isNull(Object obj){
		if(obj!=null&&!String.valueOf(obj).equals("null")&&!String.valueOf(obj).equals("")){
			return false;
		}else{
			return true;
		}
	}

    /**
     * 1.判断JSONObject是否为空
     *
     * @param obj the obj
     * @return the boolean
     */
    public static boolean isObjNull(JSONObject obj){
		if(obj!=null&&!obj.isEmpty()&&!String.valueOf(obj).equals("null")&&!String.valueOf(obj).equals("")){
			return false;
		}else{
			return true;
		}
	}

    /**
     * 1.判断是否为空
     *
     * @param obj     the obj
     * @param keyname the keyname
     * @return the boolean
     */
    public static boolean isNull(JSONObject obj, String keyname){
		if(obj.containsKey(keyname)&&obj.get(keyname)!=null&&!obj.get(keyname).equals("")&&!obj.get(keyname).equals("null")){
			return false;
		}else{
			return true;
		}
	}

    /**
     * 编码转化
     *
     * @param values the values
     * @return String string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static String string_UTF_8(String values) throws UnsupportedEncodingException{
		if (!Dto.stringIsNULL(values)){
			values = new String(values.getBytes("iso8859-1"),"utf-8");
		}
		return values;
		
	}

    /**
     * JSONObject空key转为“”
     *
     * @param obj the obj
     * @return String json object
     * @throws UnsupportedEncodingException
     */
    public static JSONObject string_JSONObject(JSONObject obj){
		
		if(!isNull(obj)){
			Iterator keys = obj.keys();		
			while(keys.hasNext()){	
					String key = keys.next().toString();	
					String value = obj.optString(key);	
					if(Dto.stringIsNULL(value)){
						obj.element(key, "");
					}
					
			}
		}
		return obj;
		
	}

    /**
     * JSONArray空key转为“”
     *
     * @param array the array
     * @return String json array
     * @throws UnsupportedEncodingException
     */
    public static JSONArray string_JSONArray(JSONArray array){
		
		for(int i=0;i<array.size();i++){
			JSONObject obj = array.getJSONObject(i);
			if(!isNull(obj)){
				Iterator keys = obj.keys();		
				while(keys.hasNext()){	
					String key = keys.next().toString();	
					String value = obj.optString(key);	
					if(Dto.stringIsNULL(value)){
						obj.element(key, "");
					}
					
				}
			}
		}
		return array;
		
	}

    /**
     * JSONArray空key转为“”
     *
     * @param array the array
     * @param objs  the objs
     * @return String json array
     * @throws UnsupportedEncodingException
     */
    public static JSONArray string_JSONArray2(JSONArray array, Object objs){
		
		for(int i=0;i<array.size();i++){
			JSONObject obj = array.getJSONObject(i);
			if(!isNull(obj)){
				Iterator keys = obj.keys();		
				while(keys.hasNext()){
					String key = keys.next().toString();	
					String value = obj.optString(key);	
					if(Dto.stringIsNULL(value)){
						obj.element(key, objs);
					}
					
				}
			}
		}
		return array;
		
	}


    /**
     * Write log.
     *
     * @param msg the msg
     */
    public static void writeLog(String msg){
		logger.info(msg);
	}

    /**
     * Write log.
     *
     * @param msgs the msgs
     */
    public static void writeLog(String[] msgs){
        logger.info(getString(msgs));
	}

    /**
     * Write log.
     *
     * @param msgs the msgs
     */
    public static void writeLog(Object[] msgs){
        logger.info(getString(msgs));
	}

    /**
     * 多个string相加
     *
     * @param strs the strs
     * @return the string
     */
    public static String getString(String[] strs){
		StringBuffer sb=new StringBuffer();
		for(String str:strs){
			sb.append(str);
		}
		return sb.toString();
	}

    /**
     * 多个string相加
     *
     * @param strs the strs
     * @return the string
     */
    public static String getString(Object[] strs){
		StringBuffer sb=new StringBuffer();
		for(Object str:strs){
			sb.append(str+",");
		}
		return sb.toString();
	}

    /**
     * 把JSON拼成STERING
     *
     * @param arr the arr
     * @return string string
     */
    public static String getJSON(JSONArray arr){
		String e="";
		if(arr.size()!=0){
			for (int i = 0; i < arr.size(); i++) {
				e+=arr.get(i)+",";
			}
			e.subSequence(0, e.length()-1);
		}
		return e;
		
	}

    /**
     * Error.
     */
    public static void ERROR(){
		 int i=8/0;
	}

    /**
     * 加
     *
     * @param a1 the a 1
     * @param b1 the b 1
     * @return the double
     */
    public static double add(double a1, double b1) {
		BigDecimal a2 = new BigDecimal(Double.toString(a1));  
		BigDecimal b2 = new BigDecimal(Double.toString(b1));  
		return a2.add(b2).doubleValue();  
	}

    /**
     * 减
     *
     * @param a1 the a 1
     * @param b1 the b 1
     * @return the double
     */
    public static double sub(double a1, double b1) {
		BigDecimal a2 = new BigDecimal(Double.toString(a1));  
		BigDecimal b2 = new BigDecimal(Double.toString(b1));  
		return a2.subtract(b2).doubleValue();  
	}

    /**
     * 乘
     *
     * @param a1 the a 1
     * @param b1 the b 1
     * @return the double
     */
    public static double mul(double a1, double b1) {
		BigDecimal a2 = new BigDecimal(Double.toString(a1));  
		BigDecimal b2 = new BigDecimal(Double.toString(b1));  
		return a2.multiply(b2).doubleValue();  
	}


    /**
     * 乘2,
     * 保留两位小数
     *
     * @param a1 the a 1
     * @param b1 the b 1
     * @return the double
     */
    public static double mul2(double a1, double b1) {
		BigDecimal a2 = new BigDecimal(Double.toString(a1));  
		BigDecimal b2 = new BigDecimal(Double.toString(b1));  
		String b = String.format("%.2f", a2.multiply(b2).doubleValue());
		return Double.valueOf(b);  
	}


    /**
     * 除
     *
     * @param a1    the a 1
     * @param b1    the b 1
     * @param scale the scale
     * @return the double
     */
    public static double div(double a1, double b1, int scale) {
		
		if (scale < 0) {  
			throw new IllegalArgumentException("error");  
		}
		BigDecimal a2 = new BigDecimal(Double.toString(a1));  
		BigDecimal b2 = new BigDecimal(Double.toString(b1));  
		return a2.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();  
	}


    /**
     * 将int[]转换成list
     *
     * @param array the array
     * @param index // 开始位置下标
     * @return list list
     */
    public static List<Integer> arrayToList(int[] array, int index){
		
		if(index<0||index>=array.length){
			index=0;
		}
		List<Integer> list = new ArrayList<Integer>();
		for (int i=index; i<array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
}
