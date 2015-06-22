package task5;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class ProjectAgent extends Agent {
	private static final long serialVersionUID = 1L;
	// The list of participated programmers
	private ArrayList<AID> programmers;
	private ArrayList<Integer> programmers_seniority;
	// minimal level of seniority, which is required for the project
	private int minSeniority;
	// Price of the project for programmer
	private int price;
	// NUmber of programmers, which is required for the project
	private int programmersNumber;

	// Put agent initializations here
	protected void setup() {
		// Create the list of programmers
		init();
		
		
		setSearchParameters();
		printSearchInfo();

		// Register the project-participating service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Project");
		sd.setName("JADE-proj-prog");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from programmer agents
		addBehaviour(new RequestsServer());
		// Add the behaviour serving purchase orders from programmer agents
		addBehaviour(new ApplyOffersServer());
	}

	private void init() {
		programmers = new ArrayList<AID>();
		programmers_seniority = new ArrayList<Integer>();
	}

	private void setSearchParameters() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			int[] intNumbers = pasrNumber(args);
			minSeniority = intNumbers[0];
			price = intNumbers[1];
			programmersNumber = intNumbers[2];
		}
	}

	private void printSearchInfo() {
		System.out.println("Hallo! Projet-agent " + getAID().getName()+" is ready.");
		System.out.println("Seniority is " + minSeniority + " years");
		System.out.println("Price is " + price);
		System.out.println("Number of participated programmers is "+ programmersNumber);
	}

	private int[] pasrNumber(Object[] args) {
		String params = (String) args[0];
		params = params.trim();
		params = params.replaceAll("  ", " ");
		String[] split = params.split(" ");
		if (split != null) {
			int[] numbers = new int[split.length];
			try {
				for (int i = 0; i < split.length; i++) {
					numbers[i] = Integer.parseInt((String) split[i]);
				}
				return numbers;
			} catch (Exception e) {
				throw new IllegalArgumentException("Must be three numbers!!!");
			}
		}
		throw new IllegalArgumentException("Must be three numbers!!!");
	}

	// Put agent clean-up operations here
	protected void takeDown() { //
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Project-agent " + getAID().getName()
				+ " terminating");
	}

	private class RequestsServer extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// PROPOSE Message received. Process it
				int seniority = Integer.parseInt(msg.getContent());
				ACLMessage reply = msg.createReply();
				// Check if seniority is OK for the project
				if (seniority >= minSeniority) {
					boolean accept_flag = true;
					if (programmers.size() == programmersNumber) {
						int worst_prog = 0;
						int worst_seniority = (Integer) programmers_seniority
								.get(0);
						for (int i = 1; i < programmers_seniority.size(); i++)
							if (((Integer) programmers_seniority.get(i)) < worst_seniority) {
								worst_seniority = (Integer) programmers_seniority
										.get(i);
								worst_prog = i;
								break;
							}
						if (worst_seniority < seniority) {
							ACLMessage refuse = new ACLMessage(
									ACLMessage.REFUSE);
							refuse.addReceiver((AID) programmers
									.get(worst_prog));
							refuse.setContent("Better programmer was found");
							myAgent.send(refuse);
							programmers.remove(worst_prog);
							programmers_seniority.remove(worst_prog);
						} else
							accept_flag = false;
					}
					if (accept_flag) {
						reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						reply.setContent(String.valueOf(price));
					} else {
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setContent("All required programmers were found");
					}

				} else {
					// The seniority is too low.
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					reply.setContent("Seniority is too low");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	} // End of inner class RequestsServer

	private class ApplyOffersServer extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				ACLMessage reply = msg.createReply();
				if (!programmers.contains(msg.getSender())) {
					programmers.add(msg.getSender());
					programmers_seniority
							.add(Integer.parseInt(msg.getContent()));
				}
				reply.setContent("");
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
				if (programmers.size() == programmersNumber) {
					String ParticipatedProgrammers = "";
					for (int i = 0; i < programmers.size(); i++)
						ParticipatedProgrammers += ", "
								+ ((AID) programmers.get(i)).getName();
					System.out.println(ParticipatedProgrammers.substring(2)
							+ " are participated for " + getAID().getName());
				}
			} else {
				block();
			}
		}
	}// End of inner class RequestsServer
}
