package com.zhuoan.util;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ObtainIPUtil
 *
 * @author weixiang.wu
 * @date 2018-05-11 09:38
 **/
public class IpAddressUtil {

    private final static Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);

    /**
     * 获得内网IP
     *
     * @return 内网IP
     */
    public static String getInnerNetIp() {
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.error("获得内网IP异常", e);
        }
        return ip;
    }

    /**
     * 获得外网IP
     *
     * @return 外网IP
     */
    public static String getOuterIp() {
        String ip = "";
        String chinaz = "http://ip.chinaz.com";
        StringBuilder inputLine = new StringBuilder();
        String read;
        BufferedReader in = null;
        try {
            URL url = new URL(chinaz);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            while ((read = in.readLine()) != null) {
                inputLine.append(read).append("\r\n");
            }
        } catch (IOException e) {
            logger.error("获得外网IP异常", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("获取外网ip#####关闭连接异常#####", e);
                }
            }
        }
        return parse(ip, inputLine);
    }

    private static String parse(String ip, StringBuilder inputLine) {
        Pattern p = Pattern.compile("\\<dd class\\=\"fz24\">(.*?)\\<\\/dd>");
        Matcher m = p.matcher(inputLine.toString());
        if (m.find()) {
            ip = m.group(1);
        }
        return ip;
    }

    /**
     * 获取外网ip和所在地
     *
     * @return
     */
    public static String getLocalOuterNetIp() {
        InputStream in = null;
        StringBuffer buffer = new StringBuffer();
        try {
            //创建 URL
            URL url = new URL("http://ip.chinaz.com/getip.aspx");
            // 打开到这个URL的流
            in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                buffer.append(inputLine);
            }
        } catch (Exception e) {
            logger.error("获取外网ip和所在地异常", e);
        } finally {
            try {
                Objects.requireNonNull(in).close();
            } catch (Exception e) {
                logger.error("获取外网ip和所在地#####关闭连接异常#####", e);
            }
        }
        JSONObject o = JSONObject.fromObject(String.valueOf(buffer));
//        o.get("address");  所在地
        return String.valueOf(o.get("ip"));
    }
}
