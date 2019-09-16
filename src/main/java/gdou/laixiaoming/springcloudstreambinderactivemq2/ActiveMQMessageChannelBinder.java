package gdou.laixiaoming.springcloudstreambinderactivemq2;

import gdou.laixiaoming.springcloudstreambinderactivemq2.mapper.PartitionJmsHeaderMapper;
import gdou.laixiaoming.springcloudstreambinderactivemq2.mapper.StreamActivemqHeaderProperty;
import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQConsumerProperties;
import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQExtendedBindingProperties;
import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQProducerProperties;
import gdou.laixiaoming.springcloudstreambinderactivemq2.provisioning.ActiveMQQueueProvisioner;
import gdou.laixiaoming.springcloudstreambinderactivemq2.support.ActiveMQMessageProducer;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.core.env.Environment;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.util.Map;

public class ActiveMQMessageChannelBinder 
	extends AbstractMessageChannelBinder<ExtendedConsumerProperties<ActiveMQConsumerProperties>, 
			ExtendedProducerProperties<ActiveMQProducerProperties>, ActiveMQQueueProvisioner>
	implements ExtendedPropertiesBinder<MessageChannel, ActiveMQConsumerProperties, ActiveMQProducerProperties>{
	
	private ActiveMQExtendedBindingProperties extendedProperties;
	
	private static final String VIRTUAL_TOPIC_PREFIX = "VirtualTopic.";
	
	private static final String VIRTUAL_QUEUE_PREFIX = "Consumer.";
	
	private Environment env;

	private ConnectionFactory connectionFactory;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	public void setEnv(Environment env) {
		this.env = env;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public ActiveMQMessageChannelBinder(
			ActiveMQQueueProvisioner provisioningProvider) {
		super( new String[0], provisioningProvider);
	}

	public void setExtendedProperties(ActiveMQExtendedBindingProperties extendedProperties) {
		this.extendedProperties = extendedProperties;
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
			ExtendedProducerProperties<ActiveMQProducerProperties> producerProperties, MessageChannel errorChannel)
			throws Exception {
		JmsTemplate jmsTemplate = buildJmsTemplate(producerProperties.getExtension());
		JmsSendingMessageHandler handler = new JmsSendingMessageHandler(jmsTemplate);
		Destination destinationObject = null;
		PartitionJmsHeaderMapper headerMapper = new PartitionJmsHeaderMapper();
		if (!StringUtils.isEmpty(producerProperties.getExtension().getPartition())) {
			headerMapper.put(StreamActivemqHeaderProperty.PARTITION_PEOPERTY, producerProperties.getExtension().getPartition());
		}
		destinationObject = new ActiveMQTopic(getVirtualTopic(destination.getName()));
		handler.setDestination(destinationObject);
		handler.setHeaderMapper(headerMapper);
		return handler;
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
			ExtendedConsumerProperties<ActiveMQConsumerProperties> properties) throws Exception {
		String realGroup = StringUtils.isEmpty(group) ? getDefalutGroup() : group;
		Destination destinationObject = new ActiveMQQueue(getQueueForVirtualTopic(destination.getName(), realGroup));
		DefaultMessageListenerContainer messageListenerContainer = buildDefaultMessageListenerContainer(properties.getExtension(), destinationObject);
		//concurrent consumers
		if(properties.getConcurrency() > 0){
			messageListenerContainer.setConcurrency(String.valueOf(properties.getConcurrency()));
		}
		ErrorInfrastructure errorInfrastructure = registerErrorInfrastructure(destination, realGroup, properties);
		ActiveMQMessageProducer messageProducer = new ActiveMQMessageProducer(messageListenerContainer);
		//retry config
		if(properties.getMaxAttempts() > 1) {
			messageProducer.setRetryTemplate(buildRetryTemplate(properties));
			messageProducer.setRecoveryCallback(errorInfrastructure.getRecoverer());
		} else {
			//error handler
			messageProducer.setErrorMessageStrategy(errorMessageStrategy);
			messageProducer.setErrorChannel(errorInfrastructure.getErrorChannel());
		}
		//republish to dlq
		//dlq's name like ".dlq"
		if(properties.getExtension().isRepublishToDlq()){
			messageProducer.setDlqDestination(buildDlqDestination(destination.getName(), realGroup));
		}
		return messageProducer;
	}

	@Override
	public ActiveMQConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.extendedProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public ActiveMQProducerProperties getExtendedProducerProperties(String channelName) {
		return this.extendedProperties.getExtendedProducerProperties(channelName);
	}

	@Override
	public Map<String, ?> getBindings() {
		return extendedProperties.getBindings();
	}

	@Override
	public String getDefaultsPrefix() {
		return extendedProperties.getDefaultsPrefix();
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return null;
	}

	private String getVirtualTopic(String destination) {
		return VIRTUAL_TOPIC_PREFIX + destination;
	}
	
	private String getQueueForVirtualTopic(String destination, String group) {
		return VIRTUAL_QUEUE_PREFIX + group + "." + VIRTUAL_TOPIC_PREFIX + destination;
	}
	
	private String getDefalutGroup() {
		return env.getRequiredProperty("spring.application.name");
	}

	private JmsTemplate buildJmsTemplate(ActiveMQProducerProperties producerProperties){
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		if (producerProperties.isTransaction()) {
			jmsTemplate.setSessionTransacted(true);
		}
		return jmsTemplate;
	}

	private DefaultMessageListenerContainer buildDefaultMessageListenerContainer(ActiveMQConsumerProperties consumerProperties,
													 Destination destination) {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		if (!StringUtils.isEmpty(consumerProperties.getPartition())) {
			container.setMessageSelector("" + StreamActivemqHeaderProperty.PARTITION_PEOPERTY +"='"+ consumerProperties.getPartition() +"'");
		}
		container.setDestination(destination);
		container.setConnectionFactory(connectionFactory);
		container.setSessionAcknowledgeMode(JmsProperties.AcknowledgeMode.AUTO.getMode());
		if(consumerProperties.isTransaction()) {
			container.setSessionTransacted(consumerProperties.isTransaction());
		}
		return container;
	}

	private Destination buildDlqDestination(String destination, String group){
		String dlqDestination = destination + ".dlq";
		return new ActiveMQQueue(getQueueForVirtualTopic(dlqDestination, group));
	}
}
