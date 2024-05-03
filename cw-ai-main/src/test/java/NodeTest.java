import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import uk.ac.bris.cs.scotlandyard.ui.ai.Node;
public class NodeTest{
    @Test
    public void DijkstraTest(){
        HelperFunctions help = new HelperFunctions();
        Board.GameState state = help.getGameState();
        List<Move> move = new ArrayList<>();
        Piece mrX = state.getPlayers().stream().filter(x -> x.isMrX()).toList().get(0);
        move.add(new Move.SingleMove(mrX, 1, ScotlandYard.Ticket.TAXI, 9));
        Node n = new Node(state, move);

        HashMap<Integer, Integer> distances = n.Dijkstra(1, state.getSetup());
        //check that the distance between itself is 0
        assertThat(distances.get(1)).isEqualTo(0);
        // actual distance is 2 with weighting is 2
        assertThat(distances.get(33)).isEqualTo(2);
        // actual distance is 5 so with wighting is 10
        assertThat(distances.get(67)).isEqualTo(10);
        // actual distance is 1 with weighting is 0
        assertThat(distances.get(58)).isEqualTo(0);
    }
    @Test
    // when it's a MrX move
    public void testScoreAdjacent(){
        HelperFunctions helper = new HelperFunctions();
        Board.GameState gameState = helper.getGameState();
        // get mrX piece
        Piece mrX = gameState.getPlayers().stream().filter(x -> x.isMrX()).toList().get(0);
        List<Move> move = new ArrayList<>();
        move.add(new Move.SingleMove(mrX, 1, ScotlandYard.Ticket.TAXI, 9));
        Node n = new Node(gameState, move);
        // one away from det so 0
        assertThat(n.getScore()).isEqualTo(0);

    }
    @Test
    public void testScoreNormal(){
        HelperFunctions helper = new HelperFunctions();
        Board.GameState gameState = helper.getGameState();
        Piece mrX = gameState.getPlayers().stream().filter(x -> x.isMrX()).toList().get(0);
        List<Move> move = new ArrayList<>();
        move.add(new Move.SingleMove(mrX, 1, ScotlandYard.Ticket.BUS, 58));
        Node n2 = new Node(gameState, move);
        assertThat(n2.getScore()).isEqualTo(7);
    }

}
