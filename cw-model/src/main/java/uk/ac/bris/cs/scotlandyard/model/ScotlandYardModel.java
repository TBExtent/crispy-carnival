package uk.ac.bris.cs.scotlandyard.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.Map;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;

	private Graph<Integer, Transport> graph;

	private PlayerConfiguration mrX;

	private PlayerConfiguration firstDetective;

	private PlayerConfiguration[] restOfTheDetectives;

	private Integer currentRound;

	private Colour currentPlayer;

	private int mrXLocation;

	private int mrXLastLocation;
	
	private int firstDetectiveLocation;
	
	private int[] restOfTheDetectivesLocations;

	private Map<Ticket, Integer> mrXTickets;

	private Map<Ticket, Integer> firstDetectiveTickets;

	private ArrayList<HashMap<Ticket, Integer>> restOfTheDetectivesTickets;

	private ArrayList<Spectator> spectators;

	private CallbackVisit callbackVisit;

	private HashSet<Colour> winningPlayers;

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
		mrXLastLocation = 0;
		mrXTickets = new HashMap<Ticket, Integer>(mrX.tickets);
		firstDetectiveLocation = firstDetective.location;
		firstDetectiveTickets = new HashMap<Ticket, Integer>(firstDetective.tickets);
		restOfTheDetectivesLocations = new int[restOfTheDetectives.length];
		restOfTheDetectivesTickets = new ArrayList<HashMap<Ticket, Integer>>();
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			restOfTheDetectivesLocations[i] = restOfTheDetectives[i].location;
			restOfTheDetectivesTickets.add(new HashMap<Ticket, Integer>(restOfTheDetectives[i].tickets));
		}

		spectators = new ArrayList<Spectator>();
		winningPlayers = new HashSet<Colour>();

		callbackVisit = new CallbackVisit();
		
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
		
		if (AllDetectivesStuck()) winningPlayers.add(mrX.colour);
	}
	
	@Override
	public void registerSpectator(Spectator spectator) {
		if (spectator == null) throw new NullPointerException("Spectator is null.");
		if (spectators.contains(spectator)) throw new IllegalArgumentException("Spectator already exists.");
		spectators.add(spectator);
	}
	
	@Override
	public void unregisterSpectator(Spectator spectator) {
		if (spectator == null) throw new NullPointerException("Spectator is null.");
		if (!spectators.remove(spectator)) throw new IllegalArgumentException("Spectator doesn't exist.");
	}
	
	@Override
	public void startRotate() {
		if (isGameOver()) throw new IllegalStateException("Game is already over.");
		final Set<Move> moves = GetMoves(mrX.colour, mrXLocation, mrXTickets, true);
		mrX.player.makeMove(this, mrXLocation, moves, (Move move) -> {
			if (move == null) throw new NullPointerException("Move is null.");
			if (!moves.contains(move)) throw new IllegalArgumentException("Not valid move.");
			callbackVisit.doCallback(move, (DoubleMove doubleMove) -> {MrXDoDoubleMove(doubleMove);},
										   (PassMove passMove)     -> {MrXDoPassMove(passMove);},
										   (TicketMove ticketMove) -> {MrXDoTicketMove(ticketMove);});
		});
	}
	
	private void MrXDoDoubleMove(DoubleMove doubleMove) {
		RemoveTicket(mrXTickets, Ticket.DOUBLE);
		mrXLocation = doubleMove.finalDestination();
		currentPlayer = firstDetective.colour;
		doubleMove = HideMrXDestDouble(doubleMove, currentRound, mrXLastLocation);
		SpectatorMoveMade(doubleMove);
		if (rounds.get(currentRound)) mrXLastLocation = doubleMove.firstMove().destination();
		currentRound++;
		RemoveTicket(mrXTickets, doubleMove.firstMove().ticket());
		SpectatorRoundStarted(currentRound);
		SpectatorMoveMade(doubleMove.firstMove());
		if (rounds.get(currentRound)) mrXLastLocation = doubleMove.secondMove().destination();
		currentRound++;
		RemoveTicket(mrXTickets, doubleMove.secondMove().ticket());
		SpectatorRoundStarted(currentRound);
		SpectatorMoveMade(doubleMove.secondMove());
		
		if (isGameOver()) SpectatorGameOver(winningPlayers);
		else FirstDetectiveMove();
	}
	
	private TicketMove HideMrXDest(TicketMove ticketMove, Integer round, Integer lastDest) {
		if (rounds.get(round)) return ticketMove;
		else return new TicketMove(ticketMove.colour(), ticketMove.ticket(), lastDest);
	}
	
	private DoubleMove HideMrXDestDouble(DoubleMove doubleMove, Integer round, Integer lastDest) {
		TicketMove firstMove = HideMrXDest(doubleMove.firstMove(), round, lastDest);
		lastDest = firstMove.destination();
		TicketMove secondMove = HideMrXDest(doubleMove.secondMove(), round + 1, lastDest);
		return new DoubleMove(doubleMove.colour(), firstMove, secondMove);
	}
	
	private void MrXDoPassMove(PassMove passMove) {
		currentRound++;
		currentPlayer = firstDetective.colour;
		SpectatorRoundStarted(currentRound);
		SpectatorMoveMade(passMove);
		
		if (isGameOver()) SpectatorGameOver(winningPlayers);
		else FirstDetectiveMove();
	}
	
	private void MrXDoTicketMove(TicketMove ticketMove) {
		RemoveTicket(mrXTickets, ticketMove.ticket());
		mrXLocation = ticketMove.destination();
		ticketMove = HideMrXDest(ticketMove, currentRound, mrXLastLocation);
		currentRound++;
		if (rounds.get(currentRound - 1)) mrXLastLocation = mrXLocation;
		currentPlayer = firstDetective.colour;
		SpectatorRoundStarted(currentRound);
		SpectatorMoveMade(ticketMove);
		
		if (isGameOver()) SpectatorGameOver(winningPlayers);
		else FirstDetectiveMove();
	}
	
	private void FirstDetectiveMove() {
		final Set<Move> moves = GetMoves(firstDetective.colour, firstDetectiveLocation, firstDetectiveTickets, false);
		firstDetective.player.makeMove(this, firstDetectiveLocation, moves, (Move move) -> {
			if (move == null) throw new NullPointerException("Move is null.");
			if (!moves.contains(move)) throw new IllegalArgumentException("Not valid move.");
			callbackVisit.doCallback(move, (DoubleMove doubleMove) -> {},
										   (PassMove passMove)     -> {FirstDetectiveDoPassMove(passMove);},
										   (TicketMove ticketMove) -> {FirstDetectiveDoTicketMove(ticketMove);});
		});
	}
	
	private void FirstDetectiveDoPassMove(PassMove passMove) {
		if (restOfTheDetectives.length == 0) {
			currentPlayer = mrX.colour;
			if (currentRound == rounds.size()) MrXWins();
			SpectatorMoveMade(passMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else SpectatorRotationComplete();
		}
		else {
			currentPlayer = restOfTheDetectives[0].colour;
			SpectatorMoveMade(passMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else RestOfTheDetectivesMove(0);
		}
	}
	
	private void FirstDetectiveDoTicketMove(TicketMove ticketMove) {
		RemoveTicket(firstDetectiveTickets, ticketMove.ticket());
		GiveMrXTicket(ticketMove.ticket());
		firstDetectiveLocation = ticketMove.destination();
		if (restOfTheDetectives.length == 0) {
			currentPlayer = mrX.colour;
			if (firstDetectiveLocation == mrXLocation) DetectivesWin();
			else if (AllDetectivesStuck()) MrXWins();
			else if (currentRound == rounds.size()) MrXWins();
			else if (MrXStuck()) DetectivesWin();
			SpectatorMoveMade(ticketMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else SpectatorRotationComplete();
		}
		else {
			currentPlayer = restOfTheDetectives[0].colour;
			if (firstDetectiveLocation == mrXLocation) DetectivesWin();
			else if (AllDetectivesStuck()) MrXWins();
			SpectatorMoveMade(ticketMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else RestOfTheDetectivesMove(0);
		}
	}
	
	private void RestOfTheDetectivesMove(Integer i) {
		final Set<Move> moves = GetMoves(restOfTheDetectives[i].colour, restOfTheDetectivesLocations[i], restOfTheDetectivesTickets.get(i), false);
		restOfTheDetectives[i].player.makeMove(this, restOfTheDetectivesLocations[i], moves, (Move move) -> {
			if (move == null) throw new NullPointerException("Move is null.");
			if (!moves.contains(move)) throw new IllegalArgumentException("Not valid move.");
			callbackVisit.doCallback(move, (DoubleMove doubleMove) -> {},
										   (PassMove passMove)     -> {RestOfTheDetectivesDoPassMove(passMove, i);},
										   (TicketMove ticketMove) -> {RestOfTheDetectivesDoTicketMove(ticketMove, i);});
		});	
	}
	
	private void RestOfTheDetectivesDoPassMove(PassMove passMove, Integer i) {
		if (restOfTheDetectives.length == i + 1) {
			currentPlayer = mrX.colour;
			if (currentRound == rounds.size()) MrXWins();
			SpectatorMoveMade(passMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else SpectatorRotationComplete();
		}
		else {
			currentPlayer = restOfTheDetectives[i + 1].colour;
			SpectatorMoveMade(passMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else RestOfTheDetectivesMove(i + 1);
		}
	}
	
	private void RestOfTheDetectivesDoTicketMove(TicketMove ticketMove, Integer i) {
		RemoveTicket(restOfTheDetectivesTickets.get(i), ticketMove.ticket());
		GiveMrXTicket(ticketMove.ticket());
		restOfTheDetectivesLocations[i] = ticketMove.destination();
		if (restOfTheDetectives.length == i + 1) {
			currentPlayer = mrX.colour;
			if (restOfTheDetectivesLocations[i] == mrXLocation) DetectivesWin();
			else if (AllDetectivesStuck()) MrXWins();
			else if (currentRound == rounds.size()) MrXWins();
			else if (MrXStuck()) DetectivesWin();
			SpectatorMoveMade(ticketMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else SpectatorRotationComplete();
		}
		else {
			currentPlayer = restOfTheDetectives[i + 1].colour;
			if (restOfTheDetectivesLocations[i] == mrXLocation) DetectivesWin();
			else if (AllDetectivesStuck()) MrXWins();
			SpectatorMoveMade(ticketMove);
			if (isGameOver()) SpectatorGameOver(winningPlayers);
			else RestOfTheDetectivesMove(i + 1);
		}
	}
	
	private void DetectivesWin() {
		winningPlayers.add(firstDetective.colour);
		for (PlayerConfiguration detective : restOfTheDetectives) winningPlayers.add(detective.colour);
	}
	
	private void MrXWins() {
		winningPlayers.add(mrX.colour);
	}
		
	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
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
		return Collections.unmodifiableSet(winningPlayers);
	}
	
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if (mrX.colour == colour) {
			return Optional.of(mrXLastLocation);
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
		if (mrX.colour == colour) return Optional.of(mrXTickets.get(ticket));
		else if (firstDetective.colour == colour) return Optional.of(firstDetectiveTickets.get(ticket));
		else {
			for (Integer i = 0; i < restOfTheDetectives.length; i++) {
				if (restOfTheDetectives[i].colour == colour) return Optional.of(restOfTheDetectivesTickets.get(i).get(ticket));
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
		boolean containsDetective = firstDetectiveLocation == position;	
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			if (containsDetective) break;
			containsDetective = restOfTheDetectivesLocations[i] == position;
		}

		return containsDetective;
	}

	private void SpectatorGameOver(Set<Colour> winners) {
		for (Spectator spectator : spectators) {
			spectator.onGameOver(this, winners);
		}
	}
	
	private void SpectatorMoveMade(Move move) {
		for (Spectator spectator : spectators) {
			spectator.onMoveMade(this, move);
		}
	}

	private void SpectatorRotationComplete() {
		for (Spectator spectator : spectators) {
			spectator.onRotationComplete(this);
		}
	}

	private void SpectatorRoundStarted(Integer round) {
		for (Spectator spectator : spectators) {
			spectator.onRoundStarted(this, round);
		}
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

	private void RemoveTicket(Map<Ticket, Integer> tickets, Ticket ticket) {
		tickets.put(ticket, tickets.get(ticket) - 1);
	}

	private void GiveMrXTicket(Ticket ticket) {
		mrXTickets.put(ticket, mrXTickets.get(ticket) + 1);
	}

	private boolean AllDetectivesStuck() {
		boolean allStuck = GetMoves(firstDetective.colour, firstDetectiveLocation, firstDetectiveTickets, false).contains(new PassMove(firstDetective.colour));
		for (Integer i = 0; i < restOfTheDetectives.length; i++) {
			if (!allStuck) return false;
			allStuck = GetMoves(restOfTheDetectives[i].colour, restOfTheDetectivesLocations[i], restOfTheDetectivesTickets.get(i), false).contains(new PassMove(restOfTheDetectives[i].colour));
		}
		return allStuck;
	}

	private boolean MrXStuck() {
		return GetMoves(mrX.colour, mrXLocation, mrXTickets, true).contains(new PassMove(mrX.colour));
	}
}
