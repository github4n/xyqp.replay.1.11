package com.zhuoan.webapp.config;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Queue;

/**
 * 1、兴起一条队列
 * @see ActiveConfig#baseQueueDestination()
 * <p>
 * 2、配置监听，即消费者
 * @see com.zhuoan.webapp.listener.event
 * <p>
 * 3、消费者监听当前队列
 * @see ActiveConfig#queueListenerContainer(ConnectionFactory connectionFactory)
 * <p>
 * @author weixiang.wu
 * @date 2018 -04-09 21:02
 */
@Configuration
public class ActiveConfig {

    @Resource
    private MessageListener baseQueueMessageListener;

    @Resource
    private MessageListener sSSQueueMessageListener;

    @Resource
    private MessageListener sqlQueueMessageListener;

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
     * Sss queue destination queue.
     *
     * @return the queue
     */
    @Bean
    public Queue sssQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_SSS");
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

    @Bean
    public Queue zjhQueueDestination() {
        return new ActiveMQQueue("ZA_GAMES_ZJH");
    }

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
        jmsTemplate.setReceiveTimeout(10000);
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }

    /**
     * Queue listener container default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer(ConnectionFactory connectionFactory) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(baseQueueMessageListener);
        queueListenerContainer.setDestination(baseQueueDestination());
        return queueListenerContainer;
    }

    /**
     * Queue listener container 2 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer2(ConnectionFactory connectionFactory) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(sSSQueueMessageListener);
        queueListenerContainer.setDestination(sssQueueDestination());
        return queueListenerContainer;
    }

    /**
     * Queue listener container 3 default message listener container.
     *
     * @param connectionFactory the connection factory
     * @return the default message listener container
     */
    @Bean
    public DefaultMessageListenerContainer queueListenerContainer3(ConnectionFactory connectionFactory) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(sqlQueueMessageListener);
        queueListenerContainer.setDestination(sqlQueueDestination());
        return queueListenerContainer;
    }

    @Bean
    public DefaultMessageListenerContainer queueListenerContainer4(ConnectionFactory connectionFactory) {
        DefaultMessageListenerContainer queueListenerContainer = new DefaultMessageListenerContainer();
        queueListenerContainer.setConnectionFactory(connectionFactory);
        queueListenerContainer.setMessageListener(zjhQueueMessageListener);
        queueListenerContainer.setDestination(zjhQueueDestination());
        return queueListenerContainer;
    }


}
