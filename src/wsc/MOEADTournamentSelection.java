package wsc;

import ec.*;
import ec.select.TournamentSelection;

public class MOEADTournamentSelection extends TournamentSelection {

	private static final long serialVersionUID = 1L;

	/**
	 * Produces the index of a (typically uniformly distributed) randomly chosen
	 * individual to fill the tournament. <i>number</> is the position of the
	 * individual in the tournament.
	 */
	public int getRandomIndividual(int number, int subpopulation, EvolutionState state, int thread) {
		// We are using the subpopulation parameter to encode the problem index
		int index = subpopulation;

		WSCInitializer init = (WSCInitializer) state.initializer;
		int neighbourIndex = init.random.nextInt(WSCInitializer.numNeighbours);
		int populationIndex = init.neighbourhood[index][neighbourIndex];
		return populationIndex;
	}

    /** Returns true if *first* is a better (fitter, whatever) individual than *second*. */
    public boolean betterThan(Individual first, Individual second, int subpopulation, EvolutionState state, int thread){
		// We are using the subpopulation parameter to encode the problem index
		int index = subpopulation;

    	WSCInitializer init = (WSCInitializer) state.initializer;
    	double firstScore;
    	double secondScore;

    	if (WSCInitializer.tchebycheff) {
			firstScore = init.calculateTchebycheffScore(first, index);
			secondScore = init.calculateTchebycheffScore(second, index);
    	}
		else {
			firstScore = init.calculateScore(first, index);
			secondScore = init.calculateScore(second, index);
		}

        return firstScore < secondScore;
    }
}
