package gdou.laixiaoming.springcloudstreambinderactivemq2.mapper;

import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PartitionJmsHeaderMapper extends DefaultJmsHeaderMapper {
	
	private Map<String, Object> extendHeaders = new HashMap<>();
	
	@Override
	public void fromHeaders(MessageHeaders headers, Message jmsMessage) {
		if (null != extendHeaders) {
			for (Entry<String, Object> entry : extendHeaders.entrySet()) {
				try {
					jmsMessage.setObjectProperty(entry.getKey(), entry.getValue());
				} catch (JMSException e) {
					// ignore
				}
			}
		}
		super.fromHeaders(headers, jmsMessage);
	}
	
	public PartitionJmsHeaderMapper put(String property, Object value) {
		extendHeaders.put(property, value);
		return this;
	}
}
