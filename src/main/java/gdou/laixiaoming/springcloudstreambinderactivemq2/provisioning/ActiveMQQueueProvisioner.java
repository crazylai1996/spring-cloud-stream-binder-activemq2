package gdou.laixiaoming.springcloudstreambinderactivemq2.provisioning;

import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQConsumerProperties;
import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQProducerProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

public class ActiveMQQueueProvisioner implements 
//		ApplicationListener<ActivemqExceptionEvent>,
		ProvisioningProvider<ExtendedConsumerProperties<ActiveMQConsumerProperties>,
		ExtendedProducerProperties<ActiveMQProducerProperties>>{
	
	private static final class ActivemqProducerDestination implements ProducerDestination{

		private String destination;

		public ActivemqProducerDestination(String destination) {
			super();
			this.destination = destination;
		}

		@Override
		public String getName() {
			return this.destination;
		}

		@Override
		public String getNameForPartition(int partition) {
			return null;
		}

	}
	
	private static final class ActivemqConsumerDestination implements ConsumerDestination{

		private String destination;

		public ActivemqConsumerDestination(String destination) {
			super();
			this.destination = destination;
		}

		@Override
		public String getName() {
			return destination;
		}
		
	}

	@Override
	public ProducerDestination provisionProducerDestination(String name,
			ExtendedProducerProperties<ActiveMQProducerProperties> properties) throws ProvisioningException {
		String dest = StringUtils.isEmpty(properties.getExtension().getDestination()) ? name : properties.getExtension().getDestination();
		return new ActivemqProducerDestination(dest);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String name, String group,
			ExtendedConsumerProperties<ActiveMQConsumerProperties> properties) throws ProvisioningException {
		String dest = StringUtils.isEmpty(properties.getExtension().getDestination()) ? name: properties.getExtension().getDestination();
		return new ActivemqConsumerDestination(dest);
	}

}
