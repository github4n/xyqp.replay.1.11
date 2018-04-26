package com.zhuoan.webapp.controller.socketio;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.util.thread.ThreadPoolHelper;
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
     * Start server.手动启动socket 服务
     */
    @RequestMapping(value = "startServer", method = RequestMethod.GET)
    @ResponseBody
    public String startServer(final HttpServletRequest request) {
        logger.info("当前IP = [" + getIp(request) + "] 手动启动 socket服务");
        if (service.getServer() == null) {
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    service.startServer();
                }
            });
        } else {
            Configuration configuration = service.getServer().getConfiguration();
            logger.info("socket 服务已绑定 ip：" + configuration.getHostname() + " port:" + configuration.getPort());
        }
        return "<br><br><br><br><br>" +
            "<h1><div style=\"text-align: center;color: #4caf50;\">恭喜你，服务启动完成！</div></h1>";
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
        logger.info("当前IP = [" + getIp(request) + "] 手动停止 socket服务");
        if (service.getServer() != null) {
            service.stopServer();
        }
        return "SocketIO server is closed successfully!!!!!!";
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
        logger.info("当前IP = [" + getIp(request) + "] 查看 socket服务状态");
        SocketIOServer server = service.getServer();
        if (server != null) {
            int port = server.getConfiguration().getPort();
            String host = server.getConfiguration().getHostname();
            return "<br><br><br><br><br>" +
                "<h1><div style=\"text-align: center;color: #4CAF50;\">服务状态：进行中</h1></div><br><p><div style=\"text-align: center;\"> [" + host + ":" + port + "]</p>";
        }
        return "<br><br><br><br><br>" +
            "<h1><div style=\"text-align: center;color: #F44336;\">服务状态：关闭</div></h1>";
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
