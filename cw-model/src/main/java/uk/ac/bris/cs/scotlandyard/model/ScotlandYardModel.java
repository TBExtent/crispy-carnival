package uk.ac.bris.cs.scotlandyard.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

//import com.sun.javafx.UnmodifiableArrayList;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;

	private Graph<Integer, Transport> graph;

	private PlayerConfiguration mrX;

	private PlayerConfiguration firstDetective;

	private PlayerConfiguration[] restOfTheDetectives;

	private Integer currentRound;

	private Colour currentPlayer;

	private Integer mrXLocation;
	
	private Integer firstDetectiveLocation;
	
	private Integer[] restOfTheDetectivesLocations;

	private Map<Ticket, Integer> mrXTickets;

	private Map<Ticket, Integer> firstDetectiveTickets;

	private ArrayList<HashMap<Ticket, Integer>> restOfTheDetectivesTickets;

	private Boolean roundHasNotYetFinishedYouStreakOfPiss = false;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		
		this.rounds = rounds;
		this.graph = graph;
		this.mrX = mrX;
		this.firstDetective = firstDetective;
		this.restOfTheDetectives = restOfTheDetectives;
		currentRound = 0;
		currentPlayer = mrX.colour;

		mrXLocation = mrX.location;
		mrXTickets = new HashMap<Ticket, Integer>(mrX.tickets);
		firstDetectiveLocation = firstDetective.location;
		firstDetectiveTickets = new HashMap<Ticket, Integer>(firstDetective.tickets);
		restOfTheDetectivesLocations = new Integer[restOfTheDetectives.length];
		restOfTheDetectivesTickets = new ArrayList<HashMap<Ticket, Integer>>();
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			restOfTheDetectivesLocations[i] = restOfTheDetectives[i].location;
			restOfTheDetectivesTickets.add(new HashMap<Ticket, Integer>(restOfTheDetectives[i].tickets));
		}
		
		if (rounds == null) throw new NullPointerException("Rounds is null.");
		if (graph == null) throw new NullPointerException("Graph is null.");
		
		if (mrX.colour != Colour.BLACK) throw new IllegalArgumentException("Mr X must be black.");
		if (CheckPlayerTickets(mrX)) throw new IllegalArgumentException("Mr X must have all types of ticket.");
		
		if (CheckPlayerTickets(firstDetective)) throw new IllegalArgumentException("First detective must have all types of ticket.");
		if (mrX.location == firstDetective.location) {
			throw new IllegalArgumentException("Mr X cannot have the same starting location as a detective.");
		}
		if (firstDetective.tickets.get(Ticket.DOUBLE) != 0) throw new IllegalArgumentException("First detective should not have a double ticket.");
		if (firstDetective.tickets.get(Ticket.SECRET) != 0) throw new IllegalArgumentException("First detective should not have a secret ticket.");
		
		for (PlayerConfiguration detective : restOfTheDetectives) {
			if (CheckPlayerTickets(detective)) throw new IllegalArgumentException("Rest of the detectives must have all types of ticket.");
			if (detective.tickets.get(Ticket.DOUBLE) != 0) throw new IllegalArgumentException("Detective should not have a double ticket.");
			if (detective.tickets.get(Ticket.SECRET) != 0) throw new IllegalArgumentException("Detective should not have a secret ticket.");
			if (mrX.location == detective.location) {
				throw new IllegalArgumentException("Mr X cannot have the same starting location as a detective.");
			}
			
			if (detective.location == firstDetective.location) throw new IllegalArgumentException("PISS OFF!");
			for (PlayerConfiguration detectiveAgain : restOfTheDetectives) {
				if (detective != detectiveAgain && detective.location == detectiveAgain.location) throw new IllegalArgumentException("PISS OFF!");
			}
		}
		
	}
	
	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}
	
	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}
	
	@Override
	public void startRotate() {
		if (isGameOver()) throw new IllegalStateException("Game is already over.");
		currentPlayer = mrX.colour;
		final Set<Move> moves = GetMoves(mrX.colour, mrXLocation, mrX.tickets, true);
		mrX.player.makeMove(this, mrX.location, moves, (Move move) -> {
			mrXLocation = DoMove(mrXLocation, moves, move, mrXTickets);
		});
		currentRound++;
		roundHasNotYetFinishedYouStreakOfPiss = true;
		currentPlayer = firstDetective.colour;
		final Set<Move> moreMoves = GetMoves(firstDetective.colour, firstDetectiveLocation, firstDetective.tickets, false);
		firstDetective.player.makeMove(this, firstDetective.location, moreMoves, (Move move) -> {
			firstDetectiveLocation = DoMove(firstDetectiveLocation, moreMoves, move, firstDetectiveTickets);
		});
		for (int i = 0; i < restOfTheDetectives.length; i++) {
			final Integer iPrime = i;
			PlayerConfiguration detective = restOfTheDetectives[i];
			currentPlayer = detective.colour;
			final Set<Move> evenMoreMoves = GetMoves(detective.colour, restOfTheDetectivesLocations[i], detective.tickets, false);
			detective.player.makeMove(this, restOfTheDetectivesLocations[i], evenMoreMoves, (Move move) -> {
				restOfTheDetectivesLocations[iPrime] = DoMove(restOfTheDetectivesLocations[iPrime], evenMoreMoves, move, restOfTheDetectivesTickets.get(iPrime));
			});
		}
		roundHasNotYetFinishedYouStreakOfPiss = false;
	}
	
	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}
	
	@Override
	public List<Colour> getPlayers() {
		List<Colour> players = new ArrayList<Colour>();
		
		players.add(mrX.colour);
		players.add(firstDetective.colour);
		for (PlayerConfiguration detective : restOfTheDetectives) players.add(detective.colour);

		return Collections.unmodifiableList(players);
	}
	
	@Override
	public Set<Colour> getWinningPlayers() {
		Set<Colour> winners = new HashSet<Colour>();
		if (currentRound == rounds.size()) {
			winners.add(mrX.colour);
		}
		if (mrXLocation == firstDetectiveLocation || GetMoves(mrX.colour, mrXLocation, mrXTickets, true).contains(new PassMove(mrX.colour))) {
			winners.add(firstDetective.colour);
			for (PlayerConfiguration detective : restOfTheDetectives) winners.add(detective.colour);
		}
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			if (mrXLocation == restOfTheDetectivesLocations[i]) {
				winners.add(firstDetective.colour);
				for (PlayerConfiguration detective : restOfTheDetectives) winners.add(detective.colour);
			}
		}
		
		boolean allStuck = GetMoves(firstDetective.colour, firstDetectiveLocation , firstDetectiveTickets, false).contains(new PassMove(firstDetective.colour));
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			if (allStuck == false) break;
			else {
				allStuck = GetMoves(restOfTheDetectives[i].colour, restOfTheDetectivesLocations[i], restOfTheDetectivesTickets.get(i), false).contains(new PassMove(restOfTheDetectives[i].colour));
			}
		}
		if (allStuck) {
			winners.add(mrX.colour);
		}

		return Collections.unmodifiableSet(winners);
	}
	
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if (mrX.colour == colour) {
			if (roundHasNotYetFinishedYouStreakOfPiss) {
				if (rounds.get(currentRound - 1)) return Optional.of(mrXLocation);
				else return Optional.of(0);
			}
			else {
				if (rounds.get(currentRound)) return Optional.of(mrXLocation);
				else return Optional.of(0);

			}
		}
		else if (firstDetective.colour == colour) return Optional.of(firstDetectiveLocation);
		else {
			for (Integer i = 0; i < restOfTheDetectives.length; i++) {
				if (restOfTheDetectives[i].colour == colour) return Optional.of(restOfTheDetectivesLocations[i]);
			}
		}
		return Optional.empty();
 	}
	
	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		if (mrX.colour == colour) return Optional.of(mrX.tickets.get(ticket));
		else if (firstDetective.colour == colour) return Optional.of(firstDetective.tickets.get(ticket));
		else {
			for (PlayerConfiguration detective : restOfTheDetectives) {
				if (detective.colour == colour) return Optional.of(detective.tickets.get(ticket));
			}
		}
		return Optional.empty();
	}
	
	@Override
	public boolean isGameOver() {
		return !getWinningPlayers().isEmpty();
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
		return new ImmutableGraph<Integer, Transport>(graph);
	}
	
	private boolean CheckPlayerTickets(PlayerConfiguration player) {
		return !player.tickets.containsKey(Ticket.BUS)    ||
			   !player.tickets.containsKey(Ticket.DOUBLE) ||
			   !player.tickets.containsKey(Ticket.SECRET) ||
			   !player.tickets.containsKey(Ticket.TAXI)   ||
			   !player.tickets.containsKey(Ticket.UNDERGROUND);
	}

	private Set<Move> GetMoves(Colour colour, Integer location, Map<Ticket, Integer> tickets, Boolean isMrX) {
		Set<TicketMove> ticketMoves = GetTicketMoves(colour, location, tickets, isMrX);
		Set<Move> moves = new HashSet<Move>(ticketMoves);
		if (tickets.get(Ticket.DOUBLE) > 0) {
			for (TicketMove ticketMove : ticketMoves) {
				Map<Ticket, Integer> tempTickets = new HashMap<Ticket, Integer>(tickets);
				tempTickets.put(ticketMove.ticket(), tickets.get(ticketMove.ticket()) - 1);
				tempTickets.put(Ticket.DOUBLE, tickets.get(Ticket.DOUBLE) - 1);
				Integer tempLocation = ticketMove.destination();
				Set<TicketMove> secondMoves = GetTicketMoves(colour, tempLocation, tempTickets, isMrX);
				for (TicketMove secondMove : secondMoves) {
					moves.add(new DoubleMove(colour, ticketMove, secondMove));
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
				if (!isMrX || !ContainsDetective(edge.destination().value())) {
					ticketMoves.add(new TicketMove(colour, Ticket.fromTransport(edge.data()), edge.destination().value()));
					if (Ticket.fromTransport(edge.data()) != Ticket.SECRET) {
						if (tickets.get(Ticket.SECRET) > 0) {
							ticketMoves.add(new TicketMove(colour, Ticket.SECRET, edge.destination().value()));
						}
					}
				}
			}
		}
		return ticketMoves;
	}

	private Integer DoMove(Integer location, Set<Move> moves, Move move, Map<Ticket, Integer> tickets) {
		if (!moves.contains(move)) throw new IllegalArgumentException("Not valid move.");
		MVisit mVisit = new MVisit(location);
		move.visit(mVisit);
		mVisit.useTickets(tickets);
		return mVisit.destination;
	}

	private class MVisit implements MoveVisitor {
		public Integer destination;

		private ArrayList<Ticket> usedTickets;

		public MVisit(Integer destination) {
			this.destination = destination;
			usedTickets = new ArrayList<Ticket>();
		}

		@Override
		public void visit(DoubleMove move) {
			destination = move.finalDestination();
			usedTickets.add(move.firstMove().ticket());
			usedTickets.add(move.secondMove().ticket());
		}

		@Override
		public void visit(PassMove move) {

		}

		@Override
		public void visit(TicketMove move) {
			destination = move.destination();
			usedTickets.add(move.ticket());
		}

		public void useTickets(Map<Ticket, Integer> tickets) {
			for (Ticket ticket : usedTickets) {
				tickets.put(ticket, tickets.get(ticket) - 1);
			}
		}
	}

	private boolean ContainsDetective(Integer position) {
		boolean containsDetective = firstDetective.location == position;

		for (PlayerConfiguration detective : restOfTheDetectives) {
			if (containsDetective) break;
			containsDetective = detective.location == position;
		}

		return containsDetective;
	}
}
