package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static java.lang.Integer.MAX_VALUE;

public class Node {
    // ATTRIBUTES
    // holds the gamestate with the move made
    private Board.GameState gameState;
    // holds the score of the gameState
    private Integer score;
    // holds all the moves that could be made in the current game state
    private List<Move> possibleMoves;
    // holds the position of mr x in the current game state
    private Integer mrXLoc;
    //list of moves passed in (moves made to get to gamestate
    private List<Move> MoveMade = new ArrayList<>();

    // CONSTRUCTORS
    public Node(Board.GameState preMoveGamestate, List<Move> currentMove) {
        boolean mrXMove;
        // if they are detectives moves to be made
        if (currentMove.get(0).commencedBy().isDetective()){
            mrXMove = false;
            // try and advance all the moves
            for (Move m : currentMove) {
                this.MoveMade.add(m);
                try {
                    this.gameState = preMoveGamestate.advance(m);
                } catch (Exception e) {
                    // if the move cannot be made then this gamestate is not possible
                    this.score = -10;
                }
            }
        } else {
            // the list will contain different moves to the same location so pick the first
            this.MoveMade = currentMove;
            this.gameState = preMoveGamestate.advance(this.MoveMade.get(0));

            Visitor vistor = new Visitor();
            mrXMove = true;
            this.mrXLoc = currentMove.get(0).accept(vistor);
        }
        // get the possible moves-> this is filtered by nancy
        this.possibleMoves = this.gameState.getAvailableMoves().asList();
        // if there are no moves availble weight the game state very low
        if (possibleMoves.size() <= 0) {
            this.score = 0;
        } else {
            if (!mrXMove){
                // get the location of MrX
                this.mrXLoc = possibleMoves.get(0).source();
            }
            // calculate the score
            this.score = calcScore();
        }
    }
    public Node(Board.GameState preMove, Move currentMove){
        this.MoveMade = new ArrayList<>();
        MoveMade.add(currentMove);
        this.mrXLoc = getmrXLoc(currentMove);
        // make MrX's move and get the new gamestate
        this.gameState = preMove.advance(currentMove);
        //Just moved MrX so available moves will be detective moves
        this.possibleMoves = this.gameState.getAvailableMoves().asList();
        // calculate the best score for mrx's next move
        this.score = calcScore();
    }
    // GETTERS
    public Board.GameState getGameState(){
        return this.gameState;
    }

    public int getScore(){
        return this.score;
    }

    public List<Move> getPossibleMoves(){
        return this.possibleMoves;
    }

    public List<Move> getMoveMade(){
        return this.MoveMade;
    }

    // return the location of mrx from a move
    private Integer getmrXLoc(Move currentMove){
        Visitor v = new Visitor();
        return currentMove.accept(v);
    }
    // METHODS
    private Integer calcScore(){
        // get a list of detectives
        List<Piece> detectives = this.gameState.getPlayers().stream()
                .filter(x -> x.isDetective()).toList();
        // a list storing an entry for the distance to each detective
        List<Integer> detectiveScoreArray = new ArrayList<>();
        // get the distance between mrX's location and all the other nodes
        HashMap<Integer, Integer> distances = Dijkstra(this.mrXLoc, gameState.getSetup());
        // for each detective work out the raw distance between that detective and mrx
        for(Piece det:detectives){
            int detLoc = gameState.getDetectiveLocation((Piece.Detective) det).orElseThrow();
            int detDistanceToX = distances.get(detLoc);
            if (this.MoveMade.get(0).commencedBy().isMrX()) {
                // check if the detective is one move away from mrx
                if (detDistanceToX <= 1) {
                    return 0;
                }
            }
            detectiveScoreArray.add(detDistanceToX);
        }
        return detectiveScoreArray.stream().min(Comparator.comparing(Integer::valueOf)).get() + addTicketWeighting(gameState.getSetup());
    }
    public HashMap<Integer, Integer> Dijkstra(Integer source, GameSetup setup){
        // lists of unvisited nodes: Node, Distance from source
        List<Integer> unvisited = new ArrayList<>();
        Integer[] dist = new Integer[setup.graph.nodes().size()+1];
        // loop through all the nodes in the graph
        for (int node : setup.graph.nodes()){
            // set the distance for each node to infinity
            dist[node] =MAX_VALUE;
            // add the node to the list of unvisited nodes
            unvisited.add(node);
            // set the distance to the source to 0 (as it is the start)
        }
        dist[source] = 0;
        // while q is not empty
        while (!unvisited.isEmpty()){
            // get the vertex from the list of unvisited with the minimum distance
            Integer u = getShortest(dist, unvisited);
            // remove the current node
            unvisited.remove(u);
            for (int neighbour : setup.graph.adjacentNodes(u)){
                // check that the neighbour is unvisited
                if (unvisited.contains(neighbour)){
                    // get the distance to the node u, then add one for the hop to the neighbour
                    Integer alt = dist[(u)] + 1;
                    if (alt < dist[(neighbour)]){
                        dist[neighbour]= alt;
                    }
                }
            }
        }
        // assign the nodes and the distances then return
        // hashmap storing the results in the form node: distance to the node
        HashMap<Integer, Integer> distances = new HashMap<>();
        for (int node : setup.graph.nodes()){
            if(dist[node] > 3){
                dist[node] = dist[node]*2;
            } else if (dist[node] == 1) {
                dist[node] = 0;
            }
            distances.put(node, dist[node]);
        }
        return distances;
    }

    // gets the smallest distance in the array of distances and returns the index i.e. the node
    private Integer getShortest(Integer[] list, List<Integer> unvisted) {
        int currentMin = MAX_VALUE;
        int index = 1;

        for (int i=1;i<list.length;i++){
            if (list[i]<currentMin && unvisted.contains(i)){
                index =i;
            }
        }

        return index;
    }
    // calculates how good a node is by working out how connected it is
    private Integer addTicketWeighting(GameSetup setup){
        int score=0;
        boolean taxi = false;
        boolean bus = false;
        boolean underground = false;
        boolean ferry = false;
        // see how
        for (Integer x : setup.graph.adjacentNodes(mrXLoc)){
            //+1 to score for each mode of transport
            if (setup.graph.edgeValue(mrXLoc, x).get().contains(ScotlandYard.Transport.TAXI)){
                taxi = true;
            }
            if (setup.graph.edgeValue(mrXLoc, x).get().contains(ScotlandYard.Transport.BUS)){
                bus = true;
            }
            if (setup.graph.edgeValue(mrXLoc, x).get().contains(ScotlandYard.Transport.UNDERGROUND)){
                underground=true;
            }
            if (setup.graph.edgeValue(mrXLoc, x).get().contains(ScotlandYard.Transport.FERRY)){
                ferry=true;
            }
            // if all of the types have been set already break -> dont need to loop anymore
            if (taxi && bus && underground && ferry){
                break;
            }
        }
        // if the adjacent node has more than 6 possible moves after
        if (setup.graph.degree(mrXLoc) > 6){
            score +=3;
        }
        if (taxi){
            score +=1;
        }
        if (bus){
            score +=1;
        }
        if (ferry){
            score +=1;
        }
        if (underground){
            score +=1;
        }
        return score;
    }

}


