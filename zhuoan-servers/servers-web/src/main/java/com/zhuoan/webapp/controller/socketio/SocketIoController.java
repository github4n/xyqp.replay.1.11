package com.zhuoan.webapp.controller.socketio;

import com.corundumstudio.socketio.Configuration;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.webapp.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * SocketIoController
 *
 * @author weixiang.wu
 * @date 2018 -04-02 09:29
 */
@Controller
public class SocketIoController extends BaseController {

    private final static Logger logger = LoggerFactory.getLogger(SocketIoController.class);

    @Resource
    private SocketIoManagerService service;

    /**
     * Start server.手动启用SocketIO服务
     */
    @RequestMapping(value = "startServer", method = RequestMethod.GET)
    @ResponseBody
    public String startServer(final HttpServletRequest request) {
        logger.info("[" + getIp(request) + "] 手动启动 SocketIO服务");
        if (service.getServer() == null) {
            try {
                service.startServer();
            } catch (Exception e) {
                return "<br><br><br><br><br>" +
                    "<h1><div style=\"text-align: center;color: #F44336;\">服务启动失败：RMI远程调用发生异常<br>" + e + "</div></h1>";
           }
        }
        Configuration configuration = service.getServer().getConfiguration();
        int port = configuration.getPort();
        String host = configuration.getHostname();
        return "<br><br><br><br><br>" +
            "<h1><div style=\"text-align: center;color: #4CAF50;\">服务状态：进行中</h1></div><br><p><div style=\"text-align: center;\"> [" + host + ":" + port + "]</p>";
    }

    /**
     * Query status string.查看服务状态
     *
     * @param request the request
     * @return the string
     */
    @RequestMapping(value = "queryStatus", method = RequestMethod.GET)
    @ResponseBody
    public String queryStatus(HttpServletRequest request) {
        logger.info(" [" + getIp(request) + "] 查看 SocketIO服务状态");
        if (service.getServer() != null) {
            Configuration configuration = service.getServer().getConfiguration();
            int port = configuration.getPort();
            String host = configuration.getHostname();
            return "<br><br><br><br><br>" +
                "<h1><div style=\"text-align: center;color: #4CAF50;\">SocketIO服务状态：进行中</h1></div><br><p><div style=\"text-align: center;\"> [" + host + ":" + port + "]</p>";
        }
        return "<br><br><br><br><br>" +
            "<h1><div style=\"text-align: center;color: #F44336;\">SocketIO服务状态：未启动</div></h1>";
    }

    /**
     * Stop server.停止socket服务
     *
     * @param request the request
     * @throws Exception the com.zhuoan.exception
     */
    @RequestMapping(value = "stopServer", method = RequestMethod.POST)
    @ResponseBody
    public String stopServer(HttpServletRequest request) {
        logger.info(" [" + getIp(request) + "] 手动停止 SocketIO服务");
        if (service.getServer() != null) {
            service.stopServer();
        }
        return "服务状态：关闭";
    }


    /**
     * Send advert info msg.给指定的客户端推送消息
     *
     * @param request  the request
     * @param response the response
     * @throws Exception the com.zhuoan.exception
     */
//    @RequestMapping("sendAdvertInfoMsg")
//    public void sendAdvertInfoMsg(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        Map params = ReflectUtil.transToMAP(request.getParameterMap());
//        String uuid = ParamsUtil.nullDeal(params, "uuid", "");
//        if (!"".equals(uuid) && service.getServer() != null) {
//            service.sendMessageToOneClient(uuid, "advert_info", "推送的内容");
//        }
//    }
//}
}
