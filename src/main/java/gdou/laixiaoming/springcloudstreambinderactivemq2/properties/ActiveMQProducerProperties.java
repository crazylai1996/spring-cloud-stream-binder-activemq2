package gdou.laixiaoming.springcloudstreambinderactivemq2.properties;

public class ActiveMQProducerProperties{

	private String destination;
	
	private String partition;
	
	private boolean transaction = false;

	public boolean isTransaction() {
		return transaction;
	}

	public void setTransaction(boolean transaction) {
		this.transaction = transaction;
	}

	public String getPartition() {
		return partition;
	}

	public void setPartition(String partition) {
		this.partition = partition;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
	
}
