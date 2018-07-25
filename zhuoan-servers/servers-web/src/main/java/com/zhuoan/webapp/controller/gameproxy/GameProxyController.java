package com.zhuoan.webapp.controller.gameproxy;

import com.zhuoan.util.Dto;
import com.zhuoan.util.MD5;
import com.zhuoan.webapp.controller.BaseController;
import com.zhuoan.webapp.service.GameProxyService;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:54 2018/7/25
 * @Modified By:
 **/
@Controller
public class GameProxyController extends BaseController {

    private final static Logger logger = LoggerFactory.getLogger(GameProxyController.class);

    @Resource
    private GameProxyService gameProxyService;

    @RequestMapping(value = "updateRedisCache.json", method = RequestMethod.POST)
    public void updateRedisCache(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/plain; charset=utf-8");
        String cacheType = request.getParameter("cache_type");
        String thisDate = request.getParameter("this_date");
        String platform = request.getParameter("platform");
        String roomNo = request.getParameter("room_no");
        String sign = request.getParameter("sign");
        String md5 = MD5.MD5Encode(cacheType + thisDate + platform + roomNo + "zhoan");
        JSONObject result = new JSONObject();
        if (!Dto.stringIsNULL(md5) && md5.equals(sign)) {
            gameProxyService.deleteCacheByKey(Integer.valueOf(cacheType), roomNo, platform);
            logger.info("[" + getIp(request) + "] 清除缓存数据 [cacheType:" + cacheType + ",roomNo:" + roomNo + ",platform:" + platform + "]");
            result.put("code", 1);
            result.put("msg", "操作成功");
        } else {
            result.put("code", 0);
            result.put("msg", "操作失败");
        }
        response.getWriter().print(result);
    }

}
