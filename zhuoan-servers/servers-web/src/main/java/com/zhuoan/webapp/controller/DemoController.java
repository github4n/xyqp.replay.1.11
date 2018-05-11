package com.zhuoan.webapp.controller;

import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.zhuoan.enumtype.PaginationEnum;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.model.condition.ZaUsersCondition;
import com.zhuoan.model.vo.ZaUsersVO;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.cache.impl.EhCacheHelper;
import com.zhuoan.user.ZaUserBiz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


/**
 * The type Test controller.
 *
 * @author weixiang.wu
 * @see DemoController#for404() DemoController#for404()
 * @see DemoController#demoObtainValueByKey() DemoController#demoObtainValueByKey()属性配置【key=value】获取value,
 * @see DemoController#demoLog() DemoController#demoLog()日志文件输出,配置详情见src/main/env/???/logback.xml文件## ???表示dev/pro  开发or生产
 * @see DemoController#queryByCondition(ZaUsersCondition, String) DemoController#queryByCondition(ZaUsersCondition, String)按条件查询并分页、业务异常抛出、返回码配置
 * @see DemoController#cacheTest() DemoController#cacheTest()缓存使用
 * @see DemoController#demoForRedisUse() DemoController#demoForRedisUse()redis使用
 */
@RequestMapping(value = "/")
@Controller
public class DemoController extends BaseController {

    private final static Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Resource
    private Environment env;

    @Resource
    private ZaUserBiz zaUserBiz;

    @Resource
    private RedisService redisService;

    @RequestMapping("demoForRedisUse")
    @ResponseBody
    public void demoForRedisUse() throws InterruptedException {
        // 插入键值对 且保存时间为400s
        redisService.insertKey("123","123232",400);

        // 插入键值对 且保存时间永久
        redisService.insertKey("long","longlonglong",null);

        // 查询key为 123的value
        Object o =redisService.queryValueByKey("123");
        logger.info("key:123 value = "+ o);

        // 倒计时3s
        redisService.expire("123",3);

        Thread.sleep(3001);
        logger.info("key:123 value = "+ redisService.queryValueByKey("123") +"  因为对指定的key设置时间3s");

        redisService.deleteByKey("long");
        logger.info("删除了key后的值为"+redisService.queryValueByKey("long"));
        redisService.insertKey("hello，i am here","方便RDM查看到我",null);
    }


        /**
         * Enter string.
         *
         * @return the string
         */
        @RequestMapping
        public String enter () {
            return "redirect:admin";
        }

        /**
         * Demo.
         *
         * @param request the request
         */
        @RequestMapping("admin")
        public void demo (HttpServletRequest request){
            logger.info("用户 " + getIp(request) + "] 访问了admin.html");
        }


        /**
         * For 404 string.
         *
         * @return the string
         */
        @RequestMapping("error")
        @ResponseBody
        public String for404 () {
            return "Hi,真不巧,网页走丢了！";
        }

        /**
         * 变量配置于:src/main/env/???/config/common.properties文件中## ???表示dev/pro  开发or生产
         */
        @RequestMapping("key")
        @ResponseBody
        public void demoObtainValueByKey () {
//            logger.info("获取到配置参数key中的value=[" + env.getProperty(EnvKeyEnum.KEY.getKey()) + "]");
        }

        /**
         * Test log.
         *
         * @return the string
         */
        @SuppressWarnings("unchecked")
        @RequestMapping("log")
        @ResponseBody
        public String demoLog () {

            logger.info("日常业务代码请打info级别的日志，info级别以下的日志不会给予显示");

            return "WEB-INF/pages下并没有log.html这个页面噢~";
        }

        /**
         * Query by condition string.
         *
         * @param zaUsersCondition the za users condition
         * @param draw             the draw
         * @return the string
         */
        @RequestMapping(value = "queryByCondition", method = RequestMethod.GET)
        @ResponseBody
        public String queryByCondition (ZaUsersCondition zaUsersCondition, String draw){
            Map<String, Object> resultMap = new HashMap<>();
            zaUsersCondition.setPageLimit(getPageLimit());
            logger.info("查数据开始");
            PageList<ZaUsersVO> zaUsersVOS = zaUserBiz.queryAllUsersByCondition(zaUsersCondition);
            /**
             * 抛出异常测试.
             */
//        if (Boolean.TRUE) {
//            throw new BizException(ResCodeEnum.OTHER.getResMessage(), ResCodeEnum.OTHER.getResCode());
//        }
            resultMap.put(PaginationEnum.DATA.getConstant(), zaUsersVOS);
            resultMap.put(PaginationEnum.DRAW.getConstant(), draw);
            resultMap.put(PaginationEnum.RECORDS_TOTAL.getConstant(), zaUsersVOS.getPaginator().getTotalCount());
            resultMap.put(PaginationEnum.RECORDS_FILTERED.getConstant(), zaUsersVOS.getPaginator().getTotalCount());
            resultMap.put(ResCodeEnum.RES_CODE.getResCode(), ResCodeEnum.SUCCESS.getResCode());
            resultMap.put(ResCodeEnum.RES_MSG.getResCode(), ResCodeEnum.SUCCESS.getResMessage());
            return objectToJson(resultMap);
        }

        /**
         * Cache test.
         *
         * @see EhCacheHelper 详细用法见
         */
        @RequestMapping("cache")
        public void cacheTest () {
            EhCacheHelper.put("helloworld", "1", "1");

            String a = (String) EhCacheHelper.get("helloworld", "1");
            String a2 = (String) EhCacheHelper.get("helloworld2", "1");


//        Cache cache = cacheManager.getCache("helloworld");
//        // create a key to map the data to
//        final String key = "greeting";
//
//        // Create a data element
//        final Element putGreeting = new Element(key, "Hello, World!");
//
//        // Put the element into the data store
//        cache.put(putGreeting);
//
//        // Retrieve the data element
//        final Element getGreeting = cache.get(key);
//
//        // Print the value
//        logger.info(String.valueOf(getGreeting.getObjectValue()));

        }


//
//    //队列消息生产者
//    @Resource
//    private ProducerService producerService;
//
//    /**
//     * Send.
//     *
//     * @param msg the msg
//     */
//    @RequestMapping(value = "/SendMessage", method = RequestMethod.POST)
//    @ResponseBody
//    public void send(String msg) {
//        logger.info(Thread.currentThread().getName()+"------------SEND TO JMS START！！！");
//        producerService.sendMessage(msg);
//        producerService.sendMessage(demoQueueDestination,msg);
//        logger.info(Thread.currentThread().getName()+"------------SEND TO JMS END！！！");
//    }


    }
