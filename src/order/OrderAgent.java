package order;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class OrderAgent extends Agent {
	private static final long serialVersionUID = 1L;

	protected void setup() {
		addBehaviour(new SimpleBehaviour(this) {
			private static final long serialVersionUID = 1L;
			private boolean finished = false;
			public String DestinationPoint = "Moscow";
			String hostAddress = "192.168.0.95:1099/JADE";
			@SuppressWarnings("deprecation")
			AID[] resources = { 
					new AID("Resource1@"+hostAddress),
					new AID("Resource2@"+hostAddress),
					new AID("Resource3@"+hostAddress),
					new AID("Resource4@"+hostAddress),
					new AID("Resource5@"+hostAddress) };

			public void action() {
				System.out.println(getLocalName() + " is active");
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setOntology("TestOntology");
				msg.setContent(DestinationPoint);
				for (int i = 0; i < resources.length; i++)
					msg.addReceiver(resources[i]);
				send(msg);
				finished = true;
			}

			public boolean done() {
				return finished;
			}
		});
	}
}