package com.zhuoan.webapp.config;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.MessageListener;
import javax.jms.Queue;

/**
 * 1、兴起一条队列
 *
 * @author weixiang.wu
 * @date 2018 -04-09 21:02
 * @see ActiveConfig#baseQueueDestination() ActiveConfig#baseQueueDestination()ActiveConfig#baseQueueDestination()ActiveConfig#baseQueueDestination() <p> 2、配置监听，即消费者
 * @see < com.zhuoan.webapp.listener.eventp> 3、消费者监听当前队列
 * @see ActiveConfig#queueListenerContainer(ConnectionFactory connectionFactory) ActiveConfig#queueListenerContainer(ConnectionFactory connectionFactory)ActiveConfig#queueListenerContainer(ConnectionFactory connectionFactory)ActiveConfig#queueListenerContainer(ConnectionFactory connectionFactory) <p>
 */
@Configuration
public class ActiveConfig {

    @Resource
    private MessageListener baseQueueMessageListener;

    @Resource
    private MessageListener sqlQueueMessageListener;

    @Resource
    private MessageListener bdxQueueMessageListener;

    @Resource
    private MessageListener nnQueueMessageListener;

    @Resource
    private MessageListener sssQueueMessageListener;

    @Resource
    private MessageListener zjhQueueMessageListener;


    /**
     * Base queue destination queue.1、兴起队列 ZA_GAMES_BASE
     *
     * @return the queue
     */
    @Bean
    public Queue baseQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_BASE");
    }

    /**
     * Sql queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue sqlQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SQL");
    }

    /**
     * Bdx queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue bdxQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_BDX");
    }

    /**
     * Nn queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue nnQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_NN");
    }

    /**
     * Sss queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue sssQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SSS");
    }

    /**
     * Zjh queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue zjhQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_ZJH");
    }






    //===================================================================================================================


    /**
     * Queue listener container default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, baseQueueMessageListener, baseQueueDestination());
    }

    /**
     * Queue listener container 2 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer2(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, sssQueueMessageListener, sssQueueDestination());
    }

    /**
     * Queue listener container 3 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer3(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, sqlQueueMessageListener, sqlQueueDestination());
    }

    /**
     * Queue listener container 4 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer4(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, zjhQueueMessageListener, zjhQueueDestination());
    }

    /**
     * Queue listener container 5 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer5(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, nnQueueMessageListener, nnQueueDestination());
    }

    /**
     * Queue listener container 6 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer6(ConnectionFactory connectionFactory) {
        return configListenerMQ(connectionFactory, bdxQueueMessageListener, bdxQueueDestination());
    }






    //===================================================================================================================


    /**
     * Jms template jms template.
     *
     * @param connectionFactory the connection factory
     * @return the jms template
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDefaultDestination(baseQueueDestination());
        // deliveryMode, priority, timeToLive 的开关，要生效，必须配置explicitQosEnabled为true，默认false
        jmsTemplate.setExplicitQosEnabled(true);
        // 发送模式  DeliveryMode.NON_PERSISTENT=1:非持久 ; DeliveryMode.PERSISTENT=2:持久
        jmsTemplate.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsTemplate.setReceiveTimeout(10000);
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }

    /**
     * @param connectionFactory 连接池工厂
     * @param messageListener 监听器
     * @param queue 相应队列
     * @return
     */
    private DefaultMessageListenerContainer configListenerMQ(ConnectionFactory connectionFactory, MessageListener messageListener, Queue queue) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(messageListener);
        queueListenerContainer.setDestination(queue);
        return queueListenerContainer;
    }


}
