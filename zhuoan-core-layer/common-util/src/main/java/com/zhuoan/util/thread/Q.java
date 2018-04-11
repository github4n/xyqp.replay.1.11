package com.zhuoan.util.thread;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Q
 *
 * @author weixiang.wu
 * @date 2018-04-11 22:46
 **/
public class Q {
    private static final Integer ONE = 1;

    public static void main(String[] args) {
        String path = "C:\\Users\\Administrator\\Desktop\\jiesuan611725828265551157.csv";
        String outPutPath = "C:\\Users\\Administrator\\Desktop";
        String fileName = "jiesuan";

        List<String> exportData = new LinkedList<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)),
                "GBK"));
            String lineTxt = null;
            int i = 0;
            while ((lineTxt = br.readLine()) != null) {
                if (lineTxt.contains("结算：")) {
                    String a = StringUtils.substring(lineTxt, lineTxt.indexOf("结算："), lineTxt.length());
//                    int dataSize = exportData.size();
//                    if (dataSize > 0) {
//                        for (int j = 0; j < dataSize; j++) {
////                        a 要比较exportData里面的每一个
//                            String b = exportData.get(j);
//                            String c = StringUtils.substring(b, b.indexOf("结算："), b.length());
//                            if (a.equals(c)) {
//                                i++;
//                                System.out.println("重复数据计数：" + i);
//                                continue;
//                            } else {
//                                exportData.add(lineTxt);
//                            }
//                        }
//                    } else
                        exportData.add(lineTxt);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("read errors :" + e);
        }


        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                file.mkdir();
            }
            // 定义文件名格式并创建
            csvFile = File.createTempFile(fileName, ".csv",
                new File(outPutPath));
            // UTF-8使正确读取分隔符","
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(csvFile), "GBK"), 1024);
            // 写入文件头部
//            if (map.size()>0) {
//                for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator
//                    .hasNext(); ) {
//                    java.util.Map.Entry propertyEntry = (java.util.Map.Entry) propertyIterator
//                        .next();
//                    csvFileOutputStream
//                        .write("" + (String) propertyEntry.getValue() != null ? (String) propertyEntry
//                            .getValue() : "" + "");
//                    if (propertyIterator.hasNext()) {
//                        csvFileOutputStream.write(",");
//                    }
//                }
//                csvFileOutputStream.newLine();
//            }
            // 写入文件内容
            if (exportData != null && !exportData.isEmpty()) {
                for (String data : exportData) {
                    csvFileOutputStream.append(data).append("\r");
                }
            }
            csvFileOutputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
