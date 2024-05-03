
import io.atlassian.fugue.Pair;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Nancy;
import uk.ac.bris.cs.scotlandyard.ui.ai.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;

public class NancyTest {
    // minimax -> know which score should be returned
    // getMrXfilter children: comparision -> should know what it filters
    // filter det children
    // one destination one node

    @Test
    public void testGetMrXFilteredChildren(){
        HelperFunctions help = new HelperFunctions();
        Board.GameState state = help.getGameState();
        Nancy Drew = new Nancy();
        Pair<Long, TimeUnit> timeoutPair = new Pair<>((long) 400, TimeUnit.MILLISECONDS);

        Move m = Drew.pickMove((Board) state, timeoutPair);

    }


}
