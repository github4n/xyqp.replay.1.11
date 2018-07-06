package com.zhuoan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;


public class HttpReqUtil {

	/**
	 * 带证书的http请求
	 * @param url 路径
	 * @param queryString 查询参数
	 * @param charset 参数编码格式
	 * @return
	 * @throws Exception 
	 */
//	public static String doGet(String url, String queryString, String charset,String path) throws Exception {
//		
//		StringBuffer resp = new StringBuffer();
//		CloseableHttpClient httpclient = KeyStoreUtil.getCilentWithKey(path);
//		try {
//			HttpPost method = new HttpPost(url); 
//			
//			//组织传递的参数
//			StringEntity param = new StringEntity(queryString, "UTF-8");
//			param.setContentType("text/xml;charset=UTF-8");
//			param.setContentEncoding("UTF-8");
//			method.addHeader("Content-Type", "text/xml"); 
//			method.setEntity(param); 
//			
//			//发起post请求
//			CloseableHttpResponse response = httpclient.execute(method);
//			try {
//				HttpEntity entity = response.getEntity();
//				if (entity != null) {
//					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent(),charset));
//					String line;
//					while ((line = bufferedReader.readLine()) != null) {
//						resp.append(line);
//					}
//				}
//				EntityUtils.consume(entity);
//			} finally {
//				response.close();
//			}
//		}finally {
//            httpclient.close();
//        }
//		return resp.toString();
//	}
	
	
	/**
	 * http请求
	 * @param url 路径
	 * @param queryString 查询参数
	 * @param charset 参数编码格式
	 * @return
	 */
	public static String doGet(String url, String queryString, String charset) {
		
		StringBuffer resp = new StringBuffer();
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);
		try {
			if (StringUtils.isNotBlank(queryString))
				// 对get请求参数做了http请求默认编码，好像没有任何问题，汉字编码后，就成为%式样的字符串
				method.setQueryString(URIUtil.encodeQuery(queryString));
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(method.getResponseBodyAsStream(),
								charset));
				String line;
				while ((line = reader.readLine()) != null) {

					resp.append(line);
				}
				reader.close();
			}
		} catch (URIException e) {
			e.printStackTrace();
			System.out.println("执行HTTP Get请求时，编码查询字符串“" + queryString
					+ "”发生异常！");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("执行HTTP Get请求" + url + "时，发生异常！");
		} finally {
			method.releaseConnection();
		}
		return resp.toString();
	}
	
	/**
	 * 执行一个HTTP POST请求，返回请求响应的HTML
	 * @throws UnsupportedEncodingException 
	 */
	public static String doPost(String url, Map<String, String> params,String queryString,
			String charset) throws UnsupportedEncodingException {
		
		StringBuffer response = new StringBuffer();
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(url);
		post.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET,"utf-8");
		
		// 设置Http Post数据
		if (params != null) {
			
			NameValuePair[] p = new NameValuePair[params.size()];
			
	        int i = 0;
	        
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	        	p[i++] = new NameValuePair(entry.getKey(), entry.getValue());
	        }
	        post.setRequestBody(p);  
		}
		else if(queryString!=null){
			RequestEntity requestEntity = new StringRequestEntity(queryString,"text/xml","UTF-8");
			post.setRequestEntity(requestEntity);
		}
		
		try {
			
			client.executeMethod(post);
			
			if (post.getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(post.getResponseBodyAsStream(),
								charset));
				String line;
				while ((line = reader.readLine()) != null) {
					
						response.append(line);
				}
				reader.close();
			}
		} catch (IOException e) {
			System.out.println("执行HTTP Post请求" + url + "时，发生异常！");
		} finally {
			post.releaseConnection();
		}
		
		
		return response.toString();
	}
}
