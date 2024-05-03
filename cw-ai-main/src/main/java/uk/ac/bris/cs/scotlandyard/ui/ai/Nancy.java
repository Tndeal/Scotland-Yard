package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

public class Nancy implements Ai {
	Board.GameState gameState;
	List<Move> bestMove = new ArrayList<>();
	@Nonnull @Override public String name() { return "Nancy"; }
	// method to pick the best move for the gamestate
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		this.gameState = (Board.GameState) board;
		// variables to hold the best move and score
		int bestScore =0;
		List<Move> best = new ArrayList<>();

		// group nodes by destination
		List<Node> nodes = oneDestinationOneNode(this.gameState.getAvailableMoves().asList(), this.gameState);
		// loop through mrx's moves, and call mini max then get the move with the highest score
		for (Node n:nodes){
			int temp = minimax(n, 2, false, MIN_VALUE, MAX_VALUE, 0);
			// if the move is better write it to the best variables
			if (bestScore < temp ){
				bestScore = temp;
				best = List.copyOf(n.getMoveMade());
			}
		}
		//return the best move
		return best.get(best.size() -1);
	}

	// mini max function with alpha beta pruning
	private int minimax(Node node, int depth, boolean maximisingPlayer, int alpha, int beta, int count){
		int value = 0;
		// base case
		if (depth == 0){
			return node.getScore();
		}
		// max = working out MrX's turn
		if (maximisingPlayer){
			value = MIN_VALUE;
			// loop through all the children of the node
			for (Node n:getMrXFilteredChildren(node)){
				// recurse on the children
				int valuetemp = Math.max(value, minimax(n, depth-1, false, alpha, beta, count+1));
				if (valuetemp >= value){
					bestMove = List.copyOf(n.getMoveMade());
					value = valuetemp;
				}
				if (value > beta){
					break;
				}
				alpha = Math.max(alpha, value);
			}
			return value;
		}
		// minimise = working out MrX's turn
		else{
			value = MAX_VALUE;
			List<Node> nodes = getDetectiveFilteredChildren(node);
			if (!nodes.isEmpty()){
				for (Node n:nodes){
					int valuetemp;
					valuetemp = Math.min(value, minimax(n, depth -1, true, alpha, beta, count +1));
					if (valuetemp <= value){
						bestMove = List.copyOf(n.getMoveMade());
						value = valuetemp;
					}
					if (value < alpha){
						break;
					}
					beta = Math.min(beta, value);
				}
			}
		}
		// return the score
		return value;
	}
	private List<Node> getMrXFilteredChildren(Node node) {
		List<Node> children = new ArrayList<>();
		List<Piece> detectives = node.getGameState().getPlayers().stream().filter(x -> x.isDetective()).toList();
		Set<Integer> adjNodes = new HashSet<>();
		// filter out the moves that will get us close to detectives
		for (Piece det : detectives){
			// get the adjacent nodes to the detective
			Set<Integer> temp = node.getGameState().getSetup().graph.adjacentNodes(node.getGameState().getDetectiveLocation((Piece.Detective) det).orElseThrow());
			for (Integer i : temp){
				adjNodes.add(i);
			}
		}
		// remove the nodes that are adjacent
		List<Move> moves = node.getPossibleMoves();
		Visitor vistor = new Visitor();
		// if the destination of the move is not in the adjacenet nodes, do not make a node
		for (Move m: moves){
			//if (!adjNodes.contains(m.accept(vistor)))
			Node temp = new Node(node.getGameState(), m);
			children.add(temp);
		}
		return children;
	}
	// filter the detectives moves
	//for each detective get the best possible 2 moves
	//create all combinations - creating nodes using combinations
	private List<Node> getDetectiveFilteredChildren(Node node){
		List<Node> children = new ArrayList<>();
		List<Move> bestMoves = new ArrayList<>();

		//gets all detectives
		List<Piece> detectives = node.getGameState().getPlayers().stream().filter(x -> x.isDetective()).toList();
		HashMap<Move, Node> nodes = new HashMap<>();
		// loop through each detective and get their 2 best moves
		for (Piece det:detectives){
			// MOVE:SCORE OF MOVE
			HashMap<Move, Integer> scoredMoves = new HashMap<>();
			//get the moves for det in the current gamestate
			List<Move> possibleMoves = node.getPossibleMoves().stream().filter(x -> x.commencedBy() == det).toList();

			//for each possible move, find the score of the resulting game state
			for (Move m:possibleMoves){
				Node temp = new Node(node.getGameState(), m);
				nodes.put(m, temp);
				scoredMoves.put(m, temp.getScore());
			}
			// check that there are moves to be made
			if(!scoredMoves.isEmpty()){
				//get the best two moves and add them to a Hashmap containing the detective, score and move
				Map.Entry<Move, Integer> first = scoredMoves.entrySet().stream().max(Map.Entry.comparingByValue()).get();
				bestMoves.add(first.getKey());
				scoredMoves.remove(first.getKey());
			}
			else {
				bestMoves.add(null);
			}

			if (!scoredMoves.isEmpty()){
				Map.Entry<Move, Integer> second = scoredMoves.entrySet().stream().max(Map.Entry.comparingByValue()).get();
				bestMoves.add(second.getKey());
			}
			else {
				bestMoves.add(null);
			}
		}
		// get the combinations of the move
		Integer numDet = detectives.size();
		// the number of possible combinations if each detective has the choice of 2 moves so 2^numDet
		Integer numCombinations = (int) Math.pow(2, numDet);
		List<Move> tempMove = new ArrayList<>();
		for (int i=0; i<numCombinations; i++){
			// the layout of the possible move 0 being the first move, 1 being the second move
			String binary = Integer.toBinaryString(i);
			// make the binary number 5 bits
			if (binary.length() < numDet){
				int difference = numDet - binary.length();
				for (int k = 0; k<difference;k++){
					binary = '0' + binary;
				}
			}
			int binaryPos = 0;
			char[] binaryC = binary.toCharArray();
			// loop through the list containing the 2 moves each detective can make -> count up in twos (0, 2, 4, etc)
			for (int k=0; k<bestMoves.size(); k+=2){
				// add the correct move to the list
				if (Character.compare(binaryC[binaryPos], '0') == 0){
					if (!(bestMoves.get(k) == null)){
						tempMove.add(bestMoves.get(k));
					}
				}else{
					if (!(bestMoves.get(k+1) == null)){
						tempMove.add(bestMoves.get(k+1));
					}
				}
				binaryPos ++;
			}
		}
		// filter the moves to only have one move for each destination
		children = oneDestinationOneNode(tempMove, node.getGameState());

		// returned the filtered set
		return children;
	}

	// picks only one move that takes you to the destination
	public List<Node> oneDestinationOneNode(List<Move> moves, Board.GameState tempGamestate){
		List<Node> nodes = new ArrayList<>();



		// Hashmap : INTEGER:LIST<MOVES> : DESTINATION:MOVES
		HashMap<Integer, List<Move>> movesandnode = new HashMap<>();
		//loop through moves, add to the hashmap if the destination is not already in there otherwise just add the move to the list for that destination
		Visitor vistor = new Visitor();
		for (Move m : moves){
			int destination = m.accept(vistor);
			if (movesandnode.containsKey(destination)){
				// the move is already in so just add to the list
				List<Move> temp = movesandnode.get(destination);
				temp.add(m);
				movesandnode.replace(destination, temp);
			}else{
				// the move is not so make new
				List<Move> temp = new ArrayList<>();
				temp.add(m);
				movesandnode.put(destination, temp);
			}
		}
		// make a new node for each and return
		for (Map.Entry<Integer, List<Move>> k : movesandnode.entrySet()){
			List<Move> sameMoves = k.getValue();
			Node temp = new Node(tempGamestate, sameMoves);
			nodes.add(temp);
		}
		return nodes;
	}

}