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
    
    public int produce(final int subpopulation,
            final EvolutionState state,
            final int thread)
            {
            // pick size random individuals, then pick the best.
            Individual[] oldinds = state.population.subpops[0].individuals;
            int best = getRandomIndividual(0, subpopulation, state, thread);
            
            int s = getTournamentSizeToUse(state.random[thread]);
                    
            if (pickWorst)
                for (int x=1;x<s;x++)
                    {
                    int j = getRandomIndividual(x, subpopulation, state, thread);
                    if (!betterThan(oldinds[j], oldinds[best], subpopulation, state, thread))  // j is at least as bad as best
                        best = j;
                    }
            else
                for (int x=1;x<s;x++)
                    {
                    int j = getRandomIndividual(x, subpopulation, state, thread);
                    if (betterThan(oldinds[j], oldinds[best], subpopulation, state, thread))  // j is better than best
                        best = j;
                    }
                
            return best;
            }
}
