package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model {
		Board.GameState gameState;
		List<Observer> observerList;

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}
		// register an observer
		@Override
		public void registerObserver(@Nonnull Observer observer) {
			// check that observer is valid
			if (observer == null) throw new NullPointerException("Observer is empty");
			if(!getObservers().isEmpty()) {
				// check to see if the observer already exists
				if (!observerList.contains(observer)) {
					observerList.add(observer);
				}else{
					throw new IllegalArgumentException("Observer is already in the list");
				}
			}else{
				// valid observer so add to a list
				observerList.add(observer);
			}
		}
		// unregister the observer
		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			// check if the observer is valid and is in the observer list
			if (observer == null) throw new NullPointerException("Observer is empty");
			if (observerList.contains(observer)){
				observerList.remove(observer);
			}else{
				// the observer is not valid
				throw new IllegalArgumentException("Observer is not in list");
			}
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observerList);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			// update the observers with the new move
			// check if game over
			if (gameState.getWinner().isEmpty()){
				// game is not over
				for (Model.Observer x: observerList){
					x.onModelChanged(getCurrentBoard(), Model.Observer.Event.MOVE_MADE);
				}
			}else{
				// game over
				for (Model.Observer x: observerList){
					x.onModelChanged(getCurrentBoard(), Model.Observer.Event.GAME_OVER);
				}
			}
		}
		// constructor
		MyModel(GameSetup setup,
				Player mrX,
				ImmutableList<Player> detectives){
			this.gameState = new MyGameStateFactory().build(setup, mrX, detectives);
			this.observerList = new ArrayList<>();
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}


}
