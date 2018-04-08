package com.zhuoan.webapp.controller.socketio;

import com.corundumstudio.socketio.Configuration;
import com.zhuoan.socketio.SocketIoManagerService;
import com.zhuoan.webapp.controller.BaseController;
import com.zhuoan.webapp.listener.socketio.ServerStartListener;
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
     *
     * @see ServerStartListener 此处默认启动socket 服务，无需手动调用
     */
    @RequestMapping(value = "startServer", method = RequestMethod.POST)
    @ResponseBody
    public String startServer(final HttpServletRequest request) {
        logger.info("当前IP = [" + getIp(request) + "] 手动启动 socket服务");
        if (service.getServer() == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    service.startServer();
                }
            }).start();
        } else {
            Configuration configuration = service.getServer().getConfiguration();
            logger.info("socket 服务已绑定 ip：" + configuration.getHostname() + " port:" + configuration.getPort());
        }
        return "SocketIO server is started successfully!!!!!!";
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
