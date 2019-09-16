package gdou.laixiaoming.springcloudstreambinderactivemq2.properties;

import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

public class ActiveMQBindingProperties implements BinderSpecificPropertiesProvider {

	private ActiveMQConsumerProperties consumer = new ActiveMQConsumerProperties();
	
	private ActiveMQProducerProperties producer = new ActiveMQProducerProperties();

	@Override
	public ActiveMQConsumerProperties getConsumer() {
		return consumer;
	}

	public void setConsumer(ActiveMQConsumerProperties consumer) {
		this.consumer = consumer;
	}

	@Override
	public ActiveMQProducerProperties getProducer() {
		return producer;
	}

	public void setProducer(ActiveMQProducerProperties producer) {
		this.producer = producer;
	}
	
}
