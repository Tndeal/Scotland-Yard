package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;

import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {
		//private attributes
		private GameSetup setup;
		// holds the pieces that need to have their turn
		private ImmutableSet<Piece> remaining;
		// holds MrX's log entries
		private ImmutableList<LogEntry> log;
		// holds which pieces are mrX and which are the detectives
		private Player mrX;
		private List<Player> detectives;
		// holds the list of valid moves
		private ImmutableSet<Move> moves;
		// null if no winner, list of winners if there is a winner
		private ImmutableSet<Piece> winner;

		// constructor for the gamestate
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining, //
				final ImmutableList<LogEntry> log,
				final Player mrX, //holds the Mr X player
				final List<Player> detectives) {

			// check to see if setup is null
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			this.setup = setup;

			// make sure remaining is not null
			if(remaining == null) throw new NullPointerException("Remaining Players is null!");
			this.remaining = remaining;

			// check mrX is not null and is the correct piece
			if(mrX == null) throw new NullPointerException("Mr X is Empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("Mr X is not Mr X");
			this.mrX = mrX;

			checkDetectives(detectives);

			if(log == null) throw new NullPointerException("Log is null!");
			this.log = log;
			// generate the available moves
			this.moves = getAvailableMoves();
		}

		void checkDetectives(final List<Player> detectives){
			// check that there are some detectives
			if(detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty!");
			// list to hold the detectives we know we have and there location
			List<Player> detectiveTemp = new ArrayList<>();
			List<Integer> detectiveLocation = new ArrayList<>();
			// loop through the detectives
			for (Player x : detectives){
				// check that they are all not null and actually detectives
				if (x == null) throw new NullPointerException("Null Detective!");
				if (!x.isDetective()) throw new IllegalArgumentException("More than one Mr X");
				// check they don't have any secret or double tickets
				if (x.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("Detective has secret ticket");
				if (x.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has double ticket");
				// check that the detectives are not the same and that they are not on the same node
				if (detectiveTemp.contains(x)){
					throw new IllegalArgumentException("Duplicate Detective");
				} else if (detectiveLocation.contains(x.location())){
					throw new IllegalArgumentException("Detectives on the same spot!");
				}else{
					// valid new detective so add its info to the list
					detectiveLocation.add(x.location());
					detectiveTemp.add(x);
				}
			}
			this.detectives = detectives;
		}

		// method that generates all the available moves for all the players currently in remaining
		@Override public ImmutableSet<Move> getAvailableMoves() {
			Set<SingleMove> singleMoves = new HashSet<>();
			Set<DoubleMove> doubleMoves = new HashSet<>();

			if (remaining.contains(mrX.piece())){
				// single moves for MrX
				singleMoves.addAll(makeSingleMoves(setup,detectives, mrX, mrX.location()));
				// get the double moves for MrX
				if (mrX.has(DOUBLE) && setup.moves.size()>1){
					doubleMoves.addAll(makeDoubleMoves(setup,detectives,mrX,singleMoves));
				}
			}else{
				// get the (single) moves for each of the detectives in remaining
				for (Player det : detectives ){
					if (remaining.contains(det.piece())){
						singleMoves.addAll(makeSingleMoves(setup,detectives, det, det.location()));
					}
				}
			}
			// return all the moves singleMoves + doubleMoves
			Set<Move> allMoves = new HashSet<>(singleMoves);
			allMoves.addAll(doubleMoves);

			return ImmutableSet.copyOf(allMoves);
		}

		// helper methods for available moves
		// generate all the single moves
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			// hashset to hold the valid moves
			Set<SingleMove> moves = new HashSet<>();
			// iterate through all connecting stations
			for (int destination: setup.graph.adjacentNodes(source)){
				// get the current locations of all the detectives
				List<Integer> occupiedLocations = new ArrayList<>();
				for (Player x : detectives) {
					occupiedLocations.add(x.location());
				}
				// check if the detectives are on the destination
				if (!occupiedLocations.contains(destination)) {
					// for each move make sure the player has enough tickets
					// t  can take the value of the ticket types
					// loop through all the ticket types between the current location and the suggested destination
					for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						// check that the player has the required ticket
						if (player.has(t.requiredTicket())) {
							// make the move
							moves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}
					if (player.isMrX()) {
						// check if he has a secret ticket
						if (player.hasAtLeast(SECRET, 1)) {
							// make move
							moves.add(new SingleMove(player.piece(), source, SECRET, destination));
						}
					}
				}
			}
			return moves;
		}
		// generate the double moves for MrX
		// had to make singlemoves so we could access ticket
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
			// set of double moves
			Set<DoubleMove> doubleMoves = new HashSet<>();
			// loop through each of the single moves
			for (SingleMove initial : singleMoves){
				// set of new single moves
				Set<SingleMove> genSingle = new HashSet<>();
				// need: oringal start, tickettype1, new start
				int originalStart = initial.source();
				Ticket ticket1 = initial.ticket;
				int destination1 = initial.destination;
				// generate all the moves from that new location
				// check they player has enough tickets even with move1
				// remove ticket needed for move1
				player = player.use(ticket1);
				// get all the moves from the middle destination
				genSingle.addAll(makeSingleMoves(setup, detectives, player, destination1));
				// return the ticket
				player = player.give(ticket1);

				// add all the double moves to the array
				for (SingleMove i : genSingle){
					doubleMoves.add(new DoubleMove(player.piece(), originalStart,ticket1, destination1, i.ticket, i.destination));
				}
			}
			return doubleMoves;
		}
		// implementation of the board interface
		// getters
		@Override public GameSetup getSetup() {
			return setup;
		}
		@Override  public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();
			// add all the pieces to the set
			players.add(mrX.piece());
			for (Player x : detectives){
				players.add(x.piece());
			}
			return ImmutableSet.copyOf(players);
		}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			// iterate through the list of detectives to determine which player this detective is
			for (Player x : detectives){
				if (x.piece().equals(detective)){
					// return the location of that player
					return Optional.of(x.location());
				}
			}
			return Optional.empty();
		}
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// check if it is mr x
			if (piece.isMrX()){
				return getTicketBoard(mrX);
			} // check if the piece is a detective
			else{
				for (Player x : detectives){
					if (x.piece().equals(piece)){
						return getTicketBoard(x);
					}
				}
			}
			// player not found so return empty
			return Optional.empty();
		}

		public ImmutableSet<Piece> getRemaining(){
			return remaining;
		}

		// returns the ticket board for a player
		private Optional<TicketBoard> getTicketBoard(Player player){
			// creates a new ticket board
			return Optional.of(
					new TicketBoard() {
						@Override
						public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
							return player.tickets().get(ticket);
						}
					}
			);
		}

		// returns the travel log
		@Override public ImmutableList<LogEntry> getMrXTravelLog() {  return log;  }

		// returns the wining pieces or empty if no winner
		@Override public ImmutableSet<Piece> getWinner() {
			// temporary set to hold the detectives
			Set<Piece> det = new HashSet<>();
			for (Player x : detectives){
				det.add(x.piece());
			}
			// set to hold the original values in remaining
			ImmutableSet<Piece> oldRemaining = ImmutableSet.copyOf(remaining);
			// check that none of the detectives are on MrXs location -> if so then they win
			for (Player x : detectives){
				// check if they are at the same location
				if (x.location() == mrX.location()){
					remaining = ImmutableSet.of();
					return ImmutableSet.copyOf(det);
				}
			}
			// make it detectives turn
			remaining = ImmutableSet.copyOf(det);
			// if the log is full i.e. the game has ended
			if (log.size() == setup.moves.size()){
				// mrX has won as the detectives have not cort him
				remaining = ImmutableSet.of();
				return ImmutableSet.of(mrX.piece());
			}
			// if none of the detectives can move (no available moves)
			if (getAvailableMoves().isEmpty()){
				// mr x wins
				remaining = ImmutableSet.of();
				return ImmutableSet.of(mrX.piece());
			}
			// make it MrX's turn
			remaining = ImmutableSet.of(mrX.piece());
			// check if he can move
			if (getAvailableMoves().isEmpty()){
				// can't move so detectives win
				remaining = ImmutableSet.of();
				return ImmutableSet.copyOf(det);
			}
			// there is no winner so return empty winner set
			remaining = ImmutableSet.copyOf(oldRemaining);
			return ImmutableSet.of();
		}

		public class OurVisitor implements Move.Visitor<GameState>{
			// if the move is a single move
			@Override
			public GameState visit(Move.SingleMove move) {
				// holds a copy of the detectives
				List<Player> tempDet = new ArrayList<>();
				// copy of remaining
				List<Piece> tempRemaining = new ArrayList<>();
				// copy og log
				List<LogEntry> copyOfLog = new ArrayList<>(log);
				// if mrx's move
				if (move.commencedBy().isMrX()) {
					// add the move to the log
					copyOfLog = addToLog(copyOfLog, move.ticket, move.destination);
					// remove the ticket and move mrx
					mrX = mrX.use(move.ticket);
					// move mrX
					mrX = mrX.at(move.destination);

					for (Player de : detectives) {
						// check that the detective has some tickets -> if so add them to remaining
						if ((de.has(TAXI) || de.has(BUS) || de.has(UNDERGROUND))){
							tempRemaining.add(de.piece());
						}
						// add the detective to the temporary detective list
						tempDet.add(de);
					}
				} // the detectives turn
				else{
					// find the piece that moved
					Piece mover = move.commencedBy();
					for (Player det : detectives){
						if (mover.equals(det.piece())){
							// move the detective and use its tickets and give to mrX
							det = det.at(move.destination);
							det = det.use(move.ticket);
							mrX = mrX.give(move.ticket);
						}else{
							// add all the other detectives (i.e. not moved yet + still has tickets) to the temp remaining
							if (remaining.contains(det.piece()) && (det.has(TAXI) || det.has(BUS) || det.has(UNDERGROUND))){
								tempRemaining.add(det.piece());
							}
						}
						// add the detective to the temporary detective
						tempDet.add(det);
					}
					// if no detectives in remaining i.e. still to play switch to mrx turn
					if (tempRemaining.isEmpty()){
						tempRemaining.add(mrX.piece());
					}
				}
				// return the new gamestate
				return (new MyGameState(setup, ImmutableSet.copyOf(tempRemaining), ImmutableList.copyOf(copyOfLog), mrX, tempDet));
			}

			@Override
			public GameState visit(Move.DoubleMove move) {
				// copy of remaining
				List<Piece> tempRemaining = new ArrayList<>();
				// copy og log
				List<LogEntry> copyOfLog = new ArrayList<>(log);
				// add first move to the log first move
				copyOfLog = addToLog(copyOfLog, move.ticket1, move.destination1);
				// add the second move to the log
				copyOfLog = addToLog(copyOfLog, move.ticket2, move.destination2);

				// remove the ticket
				mrX = mrX.use(move.ticket1);
				mrX = mrX.use(move.ticket2);
				mrX = mrX.use(DOUBLE);
				// move mrX
				mrX = mrX.at(move.destination1);
				mrX = mrX.at(move.destination2);
				// setup, remaining, log, mrX, detectives -> make detectives turn
				for (Player de : detectives) {
					if ((de.has(TAXI) || de.has(BUS) || de.has(UNDERGROUND))){
						tempRemaining.add(de.piece());
					}
				}
				// return the new gamestate
				return (new MyGameState(setup, ImmutableSet.copyOf(tempRemaining), ImmutableList.copyOf(copyOfLog), mrX, detectives));

			}
		}
		private List<LogEntry> addToLog(List<LogEntry> log, Ticket ticket, int destination){
			// check to see if it needs to be hidden or reaveled move-> if it needs to be revealed then setup. moves will be true
			// the number of log entries will be the number of moves made-1 so works as index
			if (setup.moves.get(log.size())) {
				log.add(LogEntry.reveal(ticket, destination));
			} else {
				log.add(LogEntry.hidden(ticket));
			}
			return log;
		}

		// implementation of the gamestate interface
		@Override public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			// create a visitor
			OurVisitor detMove = new OurVisitor();
			return move.accept(detMove);
		}
	}

	// part of the Factory game interface
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
			return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
		};


}
