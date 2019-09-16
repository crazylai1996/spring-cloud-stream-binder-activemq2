package gdou.laixiaoming.springcloudstreambinderactivemq2.config;

import gdou.laixiaoming.springcloudstreambinderactivemq2.ActiveMQMessageChannelBinder;
import gdou.laixiaoming.springcloudstreambinderactivemq2.properties.ActiveMQExtendedBindingProperties;
import gdou.laixiaoming.springcloudstreambinderactivemq2.provisioning.ActiveMQQueueProvisioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

@Configuration
@AutoConfigureAfter({JmsAutoConfiguration.class})
@Import({PropertyPlaceholderAutoConfiguration.class})
@EnableConfigurationProperties({ActiveMQExtendedBindingProperties.class})
public class ActiveMQMessageChannelBinderConfiguration {

	@Autowired
	private ActiveMQExtendedBindingProperties extendedProperties;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	@ConditionalOnMissingBean
	ActiveMQMessageChannelBinder activeMQMessageChannelBinder(Environment env) {
		ActiveMQMessageChannelBinder binder = new ActiveMQMessageChannelBinder(new ActiveMQQueueProvisioner());
		binder.setEnv(env);
		binder.setExtendedProperties(extendedProperties);
		binder.setConnectionFactory(connectionFactory);
		return binder;
	}
}
