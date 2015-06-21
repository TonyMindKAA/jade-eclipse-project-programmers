package task5;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ProgrammerAgent extends Agent {
	
	private static final long serialVersionUID = 1L;
	// Seniority of the programmer (years)
	private Integer seniority;
	// The list of known programmer agents
	private AID[] projectAgents;
	private boolean isParticipated = false;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! Programmer-agent " + getAID().getName()
				+ " is ready.");
		// Get the seniority as start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			seniority = Integer.parseInt((String) args[0]);
			System.out.println("Seniority is " + seniority + " years");
			// Add a TickerBehaviour that schedules a request to project agents
			// every 30 seconds
			addBehaviour(new TickerBehaviour(this, 30000) {
				private static final long serialVersionUID = 1L;
				protected void onTick() {
					if (!isParticipated) {
						System.out
								.println("Searching for project (seniority is "
										+ seniority + ")");
						// Update the list of project agents
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription sd = new ServiceDescription();
						sd.setType("Project");
						template.addServices(sd);
						try {
							DFAgentDescription[] result = DFService.search(
									myAgent, template);
							System.out
									.println("Found the following project agents:");
							projectAgents = new AID[result.length];
							for (int i = 0; i < result.length; ++i) {
								projectAgents[i] = result[i].getName();
								System.out.println(projectAgents[i].getName());
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}
						myAgent.addBehaviour(new RequestPerformer());
					}
				}
			});
			addBehaviour(new GetProjectResponse());
		} else {
			// Make the agent terminate
			System.out.println("Programmer-agent " + getAID().getName()
					+ " was refused");
			doDelete();
		}
	}

	private class GetProjectResponse extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.REFUSE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Programmer-agent " + getAID().getName()
						+ " was refused");
				isParticipated = false;
			}
			block();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Programmer-agent " + getAID().getName()
				+ " terminating.");
	}

	private class RequestPerformer extends Behaviour {
		private static final long serialVersionUID = 1L;
		private AID bestProject; // The agent who provides the best offer
		private int bestOffer; // The best offered price
		private int repliesCnt = 0; // The counter of replies from project
									// agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all projects
				ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
				for (int i = 0; i < projectAgents.length; ++i) {
					cfp.addReceiver(projectAgents[i]);
				}
				cfp.setContent(seniority.toString());
				cfp.setConversationId("Prog-Proj");
				cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique
																		// value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(
						MessageTemplate.MatchConversationId("Prog-Proj"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from project agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Project satisfied with this programmer
					if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						System.out.println("Programmer-agent "
								+ getAID().getName() + " was accepted");
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestProject == null || price >= bestOffer) {
							// This is the best offer at present
							bestOffer = price;
							bestProject = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= projectAgents.length) {

						// We received all replies
						step = 2;
					}
				} else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the project that provided the best
				// offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestProject);
				order.setContent(seniority.toString());
				order.setConversationId("Prog-Proj");
				order.setReplyWith("order" + System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(
						MessageTemplate.MatchConversationId("Prog-Proj"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(getAID().getName()
								+ " successfully participated in "
								+ reply.getSender().getName());
						System.out.println("Saniority = "
								+ seniority.toString() + "; Price = "
								+ bestOffer);
						isParticipated = true;
					} else {
						System.out
								.println("Attempt failed: another programmer has already participated in "
										+ reply.getSender().getName());
					}
					step = 4;
				} else {
					block();
				}
				break;
			}
		}

		public boolean done() {

			if (step == 2 && bestProject == null) {

				System.out.println("Attempt failed: " + seniority
						+ " is too small for projects");
			}
			return ((step == 2 && bestProject == null) || step == 4);

		}
	} // End of inner class RequestPerformer
}
