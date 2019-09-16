package gdou.laixiaoming.springcloudstreambinderactivemq2.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.MessagingException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

public class ActiveMQMessageProducer extends JmsMessageDrivenEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQMessageProducer.class);

    private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<>();

    private volatile MessageConverter messageConverter = new SimpleMessageConverter();

    private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

    private RetryTemplate retryTemplate;

    private RecoveryCallback<? extends Object> recoveryCallback;

    private Destination dlqDestination;

    void doInit(){
        RetryChannelPublishingJmsMessageListener retryListener = (RetryChannelPublishingJmsMessageListener) getListener();
        retryListener.setActiveMQMessageProducer(this);
    }

    /**
     * Construct an instance with an externally configured container.
     * @param listenerContainer the container.
     */
    public ActiveMQMessageProducer(AbstractMessageListenerContainer listenerContainer) {
        super(listenerContainer, new RetryChannelPublishingJmsMessageListener());
        doInit();
    }

    public Destination getDlqDestination() {
        return dlqDestination;
    }

    public void setDlqDestination(Destination dlqDestination) {
        this.dlqDestination = dlqDestination;
    }

    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    public void setRecoveryCallback(RecoveryCallback<? extends Object> recoveryCallback) {
        this.recoveryCallback = recoveryCallback;
    }

    private void setAttributesIfNecessary(org.springframework.messaging.Message<?> message) {
        boolean needHolder = getErrorChannel() != null && this.retryTemplate == null;
        boolean needAttributes = needHolder || this.retryTemplate != null;
        if (needHolder) {
            attributesHolder.set(ErrorMessageUtils.getAttributeAccessor(null, null));
        }
        if (needAttributes) {
            AttributeAccessor attributes = this.retryTemplate != null
                    ? RetrySynchronizationManager.getContext()
                    : attributesHolder.get();
            if (attributes != null) {
                attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
            }
        }
    }

    /**
     * MessageListener With RetryTemplate
     */
    private static class RetryChannelPublishingJmsMessageListener extends ChannelPublishingJmsMessageListener {

        private ActiveMQMessageProducer activeMQMessageProducer;

        public void setActiveMQMessageProducer(ActiveMQMessageProducer activeMQMessageProducer) {
            this.activeMQMessageProducer = activeMQMessageProducer;
        }

        @Override
        public void onMessage(Message jmsMessage, Session session) throws JMSException {
            boolean retryDisabled = activeMQMessageProducer.retryTemplate == null;
            if(retryDisabled){
                try {
                    super.onMessage(jmsMessage, session);
                }catch (RuntimeException e){
                    //send to errorChannel
                    if (activeMQMessageProducer.getErrorChannel() != null) {
                        resetBytesMessage(jmsMessage);
                        org.springframework.messaging.Message<?> message = toMessage(jmsMessage);
                        activeMQMessageProducer.setAttributesIfNecessary(message);
                        try {
                            activeMQMessageProducer.getMessagingTemplate()
                                    .send(activeMQMessageProducer.getErrorChannel(), activeMQMessageProducer.buildErrorMessage(message, e));
                        }catch (MessagingException me){
                            if(activeMQMessageProducer.getDlqDestination() == null){
                                throw me;
                            }
                            //errorChannel not exist,republish to dlq
                            MessageProducer messageProducer = session.createProducer(activeMQMessageProducer.getDlqDestination());
                            messageProducer.send(jmsMessage);
                        }
                    }
                    else {
                        throw e;
                    }
                }
            } else {
                activeMQMessageProducer.retryTemplate.execute(ctx -> {
                    LOGGER.info("retryCount:{}", ctx.getRetryCount());
                    try {
                        resetBytesMessage(jmsMessage);
                        activeMQMessageProducer.setAttributesIfNecessary(toMessage(jmsMessage));
                        super.onMessage(jmsMessage, session);
                    }catch (Exception e){
                        LOGGER.error(e.getMessage(), e);
                        throw e;
                    }
                    return null;
                }, ctx -> {
                    try {
                        activeMQMessageProducer.recoveryCallback.recover(ctx);
                    }catch (MessagingException me){
                        //errorChannel not exist,republish to dlq
                        MessageProducer messageProducer = session.createProducer(activeMQMessageProducer.getDlqDestination());
                        messageProducer.send(jmsMessage);
                    }
                    return null;
                });
            }
        }

        private Message resetBytesMessage(Message srcMessage){
            if(srcMessage instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) srcMessage;
                try {
                    bytesMessage.reset();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return bytesMessage;
            }
            return srcMessage;
        }

        private org.springframework.messaging.Message<?> toMessage(Message jmsMessage) {
            Object result = jmsMessage;
            Map<String, Object> headers = new HashMap<>();
            try {
                result = activeMQMessageProducer.messageConverter.fromMessage(jmsMessage);
                headers = activeMQMessageProducer.headerMapper.toHeaders(jmsMessage);
            } catch (Exception e){
                LOGGER.error(e.getMessage(), e);
            }
            MessageBuilderFactory messageBuilderFactory = activeMQMessageProducer.getMessageBuilderFactory();
            org.springframework.messaging.Message<?> requestMessage = (result instanceof org.springframework.messaging.Message<?>) ?
                    messageBuilderFactory.fromMessage((org.springframework.messaging.Message<?>) result).copyHeaders(headers).build() :
                    messageBuilderFactory.withPayload(result).copyHeaders(headers).build();
            //reset
            resetBytesMessage(jmsMessage);
            return requestMessage;
        }
    }
}

