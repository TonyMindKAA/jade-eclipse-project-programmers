package resource;

/* Код класса ResourceAgent.java */import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ResourceAgent extends Agent {
	private static final long serialVersionUID = 1L;
	protected void setup() {
		addBehaviour(new SimpleBehaviour(this) {
			private static final long serialVersionUID = 1L;
			private boolean finished = false;
			public String DestinationPoint;

			public void action() {
				System.out.println(getLocalName() + " is active");
				System.out.println(getLocalName() + " input destination point:");
				
				BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
				try {
					DestinationPoint = buff.readLine().toUpperCase();
				} catch (IOException E) {
					System.out.println("Erro-kaa-pro: "+E.getMessage());
				}
				
				
				MessageTemplate m1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				MessageTemplate m2 = MessageTemplate.MatchOntology("TestOntology");
				MessageTemplate m3 = MessageTemplate.and(m1, m2);
				ACLMessage msg = blockingReceive(m3, 120000);
				
				if (msg != null) {
					System.out.println(getLocalName() + ": message from "+ msg.getSender().getLocalName() + " was received");
					if (DestinationPoint.equals(msg.getContent().toUpperCase()))
						System.out.println(getLocalName()+ ": order was accepted");
					else
						System.out.println(getLocalName()+ ": order was rejected");
				} else
					System.out.println(getLocalName()+ ": empty message was received");
				finished = true;
			}
			public boolean done() {
				return finished;
			}
		});

	}
}