package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Collection;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.PassMove;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.MoveVisitor;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Edge;

// TODO name the AI
@ManagedAI("MrYAI")
public class MyAI implements PlayerFactory {

	
	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer(colour);
	}
	
	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player {
		
		private final Random random = new Random();

		private Boolean first = true;

		private Colour colour;

		private ImmutableGraph<Integer, Transport> graph;

		public MyPlayer(Colour colour) {
			this.colour = colour;
		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			if (first) {
				first = false;
				graph = new ImmutableGraph<Integer, Transport>(view.getGraph());
				//System.out.println(Arrays.asList(calcDistances(view, 70)));
			}

			Move finalMove = new PassMove(colour);
			double score = 0;

			DestVisit destVisit = new DestVisit(location);

			for (Move move : moves) {
				move.visit(destVisit);
				int dest = destVisit.getDestination();
				double tempScore = score(view, dest);
				if (tempScore > score) {
					finalMove = move;
					score = tempScore;
				}
			}

			/*MyView thisView = new MyView(view, graph);
			Set<MyView> nextViews = thisView.getNextViews();
			Move move = new PassMove(colour);
			int score = 0;
			for (MyView nextView : nextViews) {
				int tempScore = score(nextView);
				if (score < tempScore) {
					move = nextView.getHistory().get(view.getCurrentRound()).get(colour);
				}
			}*/
			callback.accept(finalMove);
		}

		/*private int score(ScotlandYardView view) {
			int start = view.getPlayerLocation(colour).get().intValue();
			Integer[] distances = calcDistances(view, start);
			int score = 0;
			for (Colour player : view.getPlayers()) {
				if (!colour.equals(player)) score += Math.sqrt(distances[view.getPlayerLocation(player).get().intValue()]);
			}
			return score;
		}*/
		
		private double score(ScotlandYardView view, int start) {
			Integer[] distances = calcDistances(start);
			double score = 0;
			for (Colour player : view.getPlayers()) {
				if (!colour.equals(player)) score += Math.sqrt(distances[view.getPlayerLocation(player).get().intValue()]);
			}
			return score;
		}

		private Integer[] calcDistances(int start) {
			Integer[] distances = new Integer[graph.size()];
			Boolean[] setDistances = new Boolean[graph.size()];
			for (int i = 0; i < graph.size(); i++) {
				distances[i] = 127;
				setDistances[i] = false;
			}
			distances[start - 1] = 0;

			while (hasUnsetDistances(setDistances)) {
				int current = getLowestUnsetDistance(distances, setDistances);
				setDistances[current] = true;
				for (Edge<Integer, Transport> edge : graph.getEdgesFrom(graph.getNode(current + 1))) {
					if (distances[edge.destination().value() - 1] > distances[current]) distances[edge.destination().value() - 1] = distances[current] + 1;
				}
			}

			return distances;
		}

		private Boolean hasUnsetDistances(Boolean[] setDistances) {
			for (int i = 0; i < setDistances.length; i++) {
				if (!setDistances[i]) return true;
			}
			return false;
		}

		private int getLowestUnsetDistance(Integer[] distances, Boolean[] setDistances) {
			int lowest = -1;
			for (int i = 0; i < distances.length; i++) {
				if (!setDistances[i]) {
					if (lowest == -1) lowest = i;
					else if (distances[i] < distances[lowest]) lowest = i;
				}
			}
			return lowest;
		}
	}

	private static class DestVisit implements MoveVisitor {
		private int destination = 0;

		public DestVisit(int destination) {
			this.destination = destination;
		}

		@Override
		public void visit(DoubleMove doubleMove) {
			destination = doubleMove.finalDestination();
		}
	
		@Override
		public void visit(PassMove passMove) {

		}
	
		@Override
		public void visit(TicketMove ticketMove) {
			destination = ticketMove.destination();
		}

		public int getDestination() {
			return destination;
		}
	}

	private static class MyView implements ScotlandYardView {

		private List<Colour> players;
		private Set<Colour> winningPlayers;
		private Map<Colour, Integer> playerLocations;
		private Map<Colour, Map<Ticket, Integer>> playerTickets;
		private Colour currentPlayer;
		private int currentRound;
		private List<Boolean> rounds;
		private ImmutableGraph<Integer, Transport> graph;
		private List<Map<Colour, Move>> history;

		public MyView(ScotlandYardView view, ImmutableGraph<Integer, Transport> graph) {
			players = new ArrayList<Colour>(view.getPlayers());
			winningPlayers = new HashSet<Colour>(view.getWinningPlayers());
			playerLocations = new HashMap<Colour, Integer>();
			playerTickets = new HashMap<Colour, Map<Ticket, Integer>>();
			for (Colour player : players) {
				playerLocations.put(player, view.getPlayerLocation(player).get());
				playerTickets.put(player, new HashMap<Ticket, Integer>());
				for (Ticket ticket : Ticket.values()) {
					playerTickets.get(player).put(ticket, view.getPlayerTickets(player, ticket).get());
				}
			}
			currentPlayer = view.getCurrentPlayer();
			currentRound = view.getCurrentRound();
			rounds = new ArrayList<Boolean>(view.getRounds());
			history = new ArrayList<Map<Colour, Move>>();
			this.graph = graph;
		}
		
		public MyView(MyView view, Move move, List<Map<Colour, Move>> history) {
			players = new ArrayList<Colour>(view.getPlayers());
			winningPlayers = new HashSet<Colour>(view.getWinningPlayers());
			playerLocations = new HashMap<Colour, Integer>();
			playerTickets = new HashMap<Colour, Map<Ticket, Integer>>();
			for (Colour player : players) {
				playerTickets.put(player, new HashMap<Ticket, Integer>());
				for (Ticket ticket : Ticket.values()) {
					playerTickets.get(player).put(ticket, view.getPlayerTickets(player, ticket).get());
				}
			}
			currentPlayer = view.getCurrentPlayer();
			currentRound = view.getCurrentRound();
			rounds = new ArrayList<Boolean>(view.getRounds());
			graph = view.getImmutableGraph();
			this.history = history;
			if (this.history.get(currentRound) == null) this.history.set(currentRound, new HashMap<Colour, Move>());
			this.history.get(currentRound).put(move.colour(), move);
			CallbackVisit callbackVisit = new CallbackVisit();
			callbackVisit.doCallback(move, (DoubleMove doubleMove) -> {DoDoubleMove(doubleMove);}, 
										   (PassMove passMove) 	   -> {},
										   (TicketMove ticketMove) -> {DoTicketMove(ticketMove);});
		}

		private void DoDoubleMove(DoubleMove doubleMove) {
			playerLocations.put(doubleMove.colour(), doubleMove.finalDestination());
			playerTickets.get(doubleMove.colour()).put(Ticket.DOUBLE, playerTickets.get(doubleMove.colour()).get(Ticket.DOUBLE) - 1);
			playerTickets.get(doubleMove.colour()).put(doubleMove.firstMove().ticket(), playerTickets.get(doubleMove.colour()).get(doubleMove.firstMove().ticket()) - 1);
			playerTickets.get(doubleMove.colour()).put(doubleMove.secondMove().ticket(), playerTickets.get(doubleMove.colour()).get(doubleMove.secondMove().ticket()) - 1);
		}

		private void DoTicketMove(TicketMove ticketMove) {
			playerLocations.put(ticketMove.colour(), ticketMove.destination());
			playerTickets.get(ticketMove.colour()).put(ticketMove.ticket(), playerTickets.get(ticketMove.colour()).get(ticketMove.ticket()) - 1);
		}

		@Override
		public List<Colour> getPlayers() {
			return Collections.unmodifiableList(players);
		}

		@Override
		public Set<Colour> getWinningPlayers() {
			return Collections.unmodifiableSet(winningPlayers);
		}

		@Override
		public Optional<Integer> getPlayerLocation(Colour colour) {
			if (playerLocations.containsKey(colour)) return Optional.of(playerLocations.get(colour));
			else return Optional.empty();
		}

		@Override
		public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
			if (playerTickets.containsKey(colour)) return Optional.of(playerTickets.get(colour).get(ticket));
			else return Optional.empty();
		}

		@Override
		public boolean isGameOver() {
			return !winningPlayers.isEmpty();
		}

		@Override
		public Colour getCurrentPlayer() {
			return currentPlayer;
		}

		@Override
		public int getCurrentRound() {
			return currentRound;
		}

		@Override
		public List<Boolean> getRounds() {
			return Collections.unmodifiableList(rounds);
		}

		@Override
		public Graph<Integer, Transport> getGraph() {
			return graph;
		}

		public ImmutableGraph<Integer, Transport> getImmutableGraph() {
			return graph;
		}

		public Set<MyView> getNextViews() {
			Set<Move> moves = GetMoves(currentPlayer, playerLocations.get(currentPlayer), playerTickets.get(currentPlayer), currentPlayer == Colour.BLACK);
			Set<MyView> nextViews = new HashSet<MyView>();
			for (Move move : moves) nextViews.add(new MyView(this, move, history));
			return nextViews;
		}

		public List<Map<Colour, Move>> getHistory() {
			return Collections.unmodifiableList(history);
		}

		private Set<Move> GetMoves(Colour colour, Integer location, Map<Ticket, Integer> tickets, Boolean isMrX) {
			Set<TicketMove> ticketMoves = GetTicketMoves(colour, location, tickets, isMrX);
			Set<Move> moves = new HashSet<Move>(ticketMoves);
			if (tickets.get(Ticket.DOUBLE) > 0 && currentRound < rounds.size() - 1) {
				for (TicketMove ticketMove : ticketMoves) {
					Map<Ticket, Integer> tempTickets = new HashMap<Ticket, Integer>(tickets);
					tempTickets.put(ticketMove.ticket(), tickets.get(ticketMove.ticket()) - 1);
					tempTickets.put(Ticket.DOUBLE, tickets.get(Ticket.DOUBLE) - 1);
					Integer tempLocation = ticketMove.destination();
					Set<TicketMove> secondMoves = GetTicketMoves(colour, tempLocation, tempTickets, isMrX);
					for (TicketMove secondMove : secondMoves) {
						if (!ContainsDetective(secondMove.destination())) {
							moves.add(new DoubleMove(colour, ticketMove, secondMove));
						}
					}
				}
			}
			if (moves.size() == 0) moves.add(new PassMove(colour));
			return moves;
		}
		
		private Set<TicketMove> GetTicketMoves(Colour colour, Integer location, Map<Ticket, Integer> tickets, Boolean isMrX) {
			Set<TicketMove> ticketMoves = new HashSet<TicketMove>();
			Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(location));
			for (Edge<Integer, Transport> edge : edges) {
				if (tickets.get(Ticket.fromTransport(edge.data())) > 0) {
					if (!ContainsDetective(edge.destination().value())) {
						ticketMoves.add(new TicketMove(colour, Ticket.fromTransport(edge.data()), edge.destination().value()));
					}
				}
				if (Ticket.fromTransport(edge.data()) != Ticket.SECRET) {
					if (tickets.get(Ticket.SECRET) > 0) {
						if (!ContainsDetective(edge.destination().value())) {
							ticketMoves.add(new TicketMove(colour, Ticket.SECRET, edge.destination().value()));
						}
					}
				}
			}
			return ticketMoves;
		}

		private boolean ContainsDetective(int position) {
			Map<Colour, Integer> tempLocations = new HashMap<Colour, Integer>(playerLocations);
			tempLocations.remove(Colour.BLACK);
	
			return tempLocations.containsValue(position);
		}

		private class CallbackVisit implements MoveVisitor {
			private Consumer<DoubleMove> doubleCallback;
			private Consumer<PassMove> passCallback;
			private Consumer<TicketMove> ticketCallback;
	
			@Override
			public void visit(DoubleMove doubleMove) {
				doubleCallback.accept(doubleMove);
			}
		
			@Override
			public void visit(PassMove passMove) {
				passCallback.accept(passMove);
			}
		
			@Override
			public void visit(TicketMove ticketMove) {
				ticketCallback.accept(ticketMove);
			}
	
			public void doCallback(Move move, Consumer<DoubleMove> doubleCallback, Consumer<PassMove> passCallback, Consumer<TicketMove> ticketCallback) {
				this.doubleCallback = doubleCallback;
				this.passCallback = passCallback;
				this.ticketCallback = ticketCallback;
				move.visit(this);
			}
		}
	}
}
