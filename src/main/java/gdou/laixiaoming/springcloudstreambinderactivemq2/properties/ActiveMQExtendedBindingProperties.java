package gdou.laixiaoming.springcloudstreambinderactivemq2.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("spring.cloud.stream.activemq")
public class ActiveMQExtendedBindingProperties implements ExtendedBindingProperties<ActiveMQConsumerProperties, ActiveMQProducerProperties>{

	private Map<String, ActiveMQBindingProperties> bindings = new HashMap<>();

	@Override
	public Map<String, ActiveMQBindingProperties> getBindings() {
		return bindings;
	}

	@Override
	public String getDefaultsPrefix() {
		return "spring.cloud.stream.activemq";
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return ActiveMQBindingProperties.class;
	}

	public void setBindings(Map<String, ActiveMQBindingProperties> bindings) {
		this.bindings = bindings;
	}

	@Override
	public ActiveMQConsumerProperties getExtendedConsumerProperties(String channelName) {
		if (bindings.containsKey(channelName) && null != bindings.get(channelName).getConsumer()) {
			return bindings.get(channelName).getConsumer();
		}else {
			return new ActiveMQConsumerProperties();
		}
	}

	@Override
	public ActiveMQProducerProperties getExtendedProducerProperties(String channelName) {
		if (bindings.containsKey(channelName) && null != bindings.get(channelName).getProducer()) {
			return bindings.get(channelName).getProducer();
		}else {
			return new ActiveMQProducerProperties();
		}
	}

}
