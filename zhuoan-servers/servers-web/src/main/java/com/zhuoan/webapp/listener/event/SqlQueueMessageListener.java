package com.zhuoan.webapp.listener.event;

import com.zhuoan.biz.event.bdx.BDXGameEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDeal;
import com.zhuoan.biz.event.zjh.ZJHGameEventDeal;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.exception.EventException;
import com.zhuoan.queue.SqlModel;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * SqlQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-11 21:43
 */
@Component
public class SqlQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(SqlQueueMessageListener.class);

    @Resource
    private MaJiangBiz mjBiz;

    @Resource
    private SSSService sssService;

    private NNGameEventDeal nnGameEventDeal = new NNGameEventDeal();
    private ZJHGameEventDeal zjhGameEventDeal = new ZJHGameEventDeal();
    private BDXGameEventDeal bdxGameEventDeal = new BDXGameEventDeal();


    @Override
    public void onMessage(Message message) {
        SqlModel sqlModel;
        try {
            sqlModel = (SqlModel) ((ActiveMQObjectMessage) message).getObject();
            logger.info("[" + this.getClass().getName() + "] 接收 = [" + JSONObject.fromObject(sqlModel) + "]");
        } catch (JMSException e) {
            throw new EventException("sqlModel对象接收异常", e.getMessage());
        }
        try {
            switch (sqlModel.type) {
                case SqlModel.EXECUTEUPDATEBYSQL:// 更新
                    DBUtil.executeUpdateBySQL(sqlModel.sql, sqlModel.params);
                    break;
                case SqlModel.PUMP:// 抽水
                    //mjBiz.pump(sqlModel.getUserIds(), sqlModel.getRoomNo(), sqlModel.getGid(), sqlModel.getFee(), sqlModel.getType1());
                    break;
                case SqlModel.SAVELOGS_NN:// 牛牛战绩
                    nnGameEventDeal.savelogs(sqlModel.getRoom_nn(), sqlModel.getGamelog_nn(), sqlModel.getUglogs_nn(), sqlModel.getType_nn(), sqlModel.getArray_nn());
                    break;
                case SqlModel.SAVELOGS_ZJH:// 炸金花战绩
                    zjhGameEventDeal.savelogs(sqlModel.getRoomNo_zjh(), sqlModel.getJiesuanData_zjh(), sqlModel.getJiesuanArray_zjh());
                    break;
                case SqlModel.SAVELOGS_SSS:// 十三水战绩
                    sssService.gamelog(sqlModel.getRoom_sss(), sqlModel.getUs_sss(), sqlModel.getUid_sss(), sqlModel.isE_sss());
                    break;
                case SqlModel.SAVELOGS_BDX:// 比大小战绩
                    bdxGameEventDeal.gamelog(sqlModel.getRoom_bdx(), sqlModel.getUs_bdx());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new EventException("*********************异常sql****************"+sqlModel.type, e.getMessage());
        }
    }
}
