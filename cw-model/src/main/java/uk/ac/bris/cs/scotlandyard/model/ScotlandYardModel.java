package uk.ac.bris.cs.scotlandyard.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

import com.sun.javafx.UnmodifiableArrayList;

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

	private int currentRound;

	private Colour currentPlayer;

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
		currentPlayer = mrX.colour;
		final Set<Move> moves = GetMoves(mrX.colour, mrX.location, mrX.tickets, true);
		mrX.player.makeMove(this, mrX.location, moves, (Move move) -> {
			DoMove(mrX, moves, move);
		});
		currentRound++;
		currentPlayer = firstDetective.colour;
		final Set<Move> moreMoves = GetMoves(firstDetective.colour, firstDetective.location, firstDetective.tickets, false);
		firstDetective.player.makeMove(this, firstDetective.location, moreMoves, (Move move) -> {
			DoMove(firstDetective, moreMoves, move);
		});
		for (PlayerConfiguration detective : restOfTheDetectives) {
			currentPlayer = detective.colour;
			final Set<Move> evenMoreMoves = GetMoves(detective.colour, detective.location, detective.tickets, false);
			detective.player.makeMove(this, detective.location, evenMoreMoves, (Move move) -> {
				DoMove(detective, evenMoreMoves, move);
			});
		}
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
		if (currentRound == rounds.size()) winners.add(mrX.colour);

		if (mrX.location == firstDetective.location || IsStuck(mrX)) {
			winners.add(firstDetective.colour);
			for (PlayerConfiguration detective : restOfTheDetectives) winners.add(detective.colour);
		}
		for (PlayerConfiguration detective : restOfTheDetectives) {
			if (mrX.location == detective.location) {
				winners.add(firstDetective.colour);
				for (PlayerConfiguration detectiveAgain : restOfTheDetectives) winners.add(detectiveAgain.colour);	
			}
		}
		
		boolean allStuck = IsStuck(firstDetective);
		for (PlayerConfiguration detective : restOfTheDetectives) {
			if (allStuck == false) break;
			else {
				allStuck = IsStuck(detective);
			}
		}
		if (allStuck) winners.add(mrX.colour);

		return Collections.unmodifiableSet(winners);
	}
	
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if (mrX.colour == colour) {
			if (rounds.get(currentRound)) return Optional.of(mrX.location);
			else return Optional.of(0);
		}
		else if (firstDetective.colour == colour) return Optional.of(firstDetective.location);
		else {
			for (PlayerConfiguration detective : restOfTheDetectives) {
				if (detective.colour == colour) return Optional.of(detective.location);
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

	private boolean IsStuck(PlayerConfiguration player) {
		if (player.tickets.get(Ticket.BUS) > 0) return false;
		if (player.tickets.get(Ticket.DOUBLE) > 0) return false;
		if (player.tickets.get(Ticket.SECRET) > 0) return false;
		if (player.tickets.get(Ticket.TAXI) > 0) return false;
		if (player.tickets.get(Ticket.UNDERGROUND) > 0) return false;
		return true;
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
		return moves;
	}
	
	private Set<TicketMove> GetTicketMoves(Colour colour, Integer location, Map<Ticket, Integer> tickets, Boolean isMrX) {
		Set<TicketMove> ticketMoves = new HashSet<TicketMove>();
		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(location));
		for (Edge<Integer, Transport> edge : edges) {
			if (tickets.get(Ticket.fromTransport(edge.data())) > 0) {
				if (!isMrX || ContainsDetective(edge.destination().value())) {
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

	private void DoMove(PlayerConfiguration player, Set<Move> moves, Move move) {
		if (!moves.contains(move)) throw new IllegalArgumentException("Not valid move.");
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
