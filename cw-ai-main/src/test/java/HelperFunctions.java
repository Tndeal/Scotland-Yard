import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.SECRET;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;

public class HelperFunctions {
    private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;
    private static Board.GameState gameState;
    private static GameSetup gameSetup;

    // Constructor
    HelperFunctions(){
        // setup then make the setup then the game state
        setUp();
        this.gameSetup = standard24MoveSetup();
        this.gameState = makeGameState();
    }
    // getters
    public GameSetup getGameSetup(){
        return this.gameSetup;
    }
    public Board.GameState getGameState(){
        return this.gameState;
    }

    // methods + initalizers

    public static void setUp() {
        try {
            graph = readGraph(Resources.toString(Resources.getResource(
                            "graph.txt"),
                    StandardCharsets.UTF_8));

        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }
    @Nonnull public static GameSetup standard24MoveSetup() {
        return new GameSetup(graph, STANDARD24MOVES);
    }

    public static Board.GameState makeGameState(){
        Board.GameState state = new MyGameStateFactory().build(gameSetup, blackPlayer(), redPlayer(), greenPlayer());
        return state;
    }

    @Nonnull public static Player blackPlayer() {
        return new Player(MRX, defaultMrXTickets(),1);
    }
    @Nonnull public static Player redPlayer() {
        return new Player(RED, defaultDetectiveTickets(), 31);
    }
    @Nonnull public static Player greenPlayer() {
        return new Player(GREEN, defaultDetectiveTickets(),20);
    }

    @Nonnull static Move.DoubleMove x2(@Nonnull Piece colour, int source,
                                       Ticket first, int firstDestination,
                                       Ticket second, int secondDestination) {
        return new Move.DoubleMove(requireNonNull(colour),
                source, requireNonNull(first), firstDestination,
                requireNonNull(second), secondDestination);
    }
    @Nonnull static Move.SingleMove taxi(@Nonnull Piece colour, int source, int destination) {
        return new Move.SingleMove(requireNonNull(colour), source, TAXI, destination);
    }

}
