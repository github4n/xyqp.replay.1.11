package com.zhuoan.webapp.controller.gameproxy;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MD5;
import com.zhuoan.webapp.controller.BaseController;
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
    private RedisService redisService;

    @RequestMapping(value = "updateRedisCache.json", method = RequestMethod.POST)
    public void updateRedisCache(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/plain; charset=utf-8");
        String cacheType = request.getParameter("cache_type");
        String thisDate = request.getParameter("this_date");
        String platform = request.getParameter("platform");
        String roomNo = request.getParameter("room_no");
        String sign = request.getParameter("sign");
        String gameId = request.getParameter("game_id");
        String md5 = MD5.MD5Encode(cacheType + thisDate + platform + roomNo + "zhoan");
        JSONObject result = new JSONObject();
        if (!Dto.stringIsNULL(md5) && md5.equals(sign)) {
            if (cacheType.equals("match_setting")) {
                redisService.deleteByKey("match_setting_0");
                redisService.deleteByKey("match_setting_1");
            }else if (cacheType.equals("dissolveRoom")) {
                if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                    JSONObject obj = new JSONObject();
                    obj.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                    obj.put(CommonConstant.RESULT_KEY_MSG,"房间已被解散");
                    CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),String.valueOf(obj),"tipMsgPush");
                    RoomManage.gameRoomMap.remove(roomNo);
                }
            } else{
                if (cacheType.contains("gold_setting_")) {
                    redisService.deleteByKey("game_info_by_id_" + gameId);
                    redisService.deleteByKey("game_on_or_off_" + gameId);
                }
                redisService.deleteByKey(cacheType);
            }
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
