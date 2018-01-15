package wsc;

import ec.*;
import ec.select.TournamentSelection;

public class MOEADTournamentSelection extends TournamentSelection {

	private static final long serialVersionUID = 1L;

	public int getRandomIndividual(int subproblem, int subpopulation, EvolutionState state, int thread) {
		int index = subproblem;

		WSCInitializer init = (WSCInitializer) state.initializer;
		int neighbourIndex = init.random.nextInt(WSCInitializer.numNeighbours);
		int populationIndex = init.neighbourhood[index][neighbourIndex];
		return populationIndex;
	}

    /** Returns true if *first* is a better (fitter, whatever) individual than *second*. */
    public boolean betterThan(int subproblem, Individual first, Individual second, int subpopulation, EvolutionState state, int thread){
		int index = subproblem;

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

    @Override
    public int produce(final int min,
            final int max,
            final int start,
            final int subpopulation,
            final Individual[] inds,
            final EvolutionState state,
            final int thread)
            {
            int n=INDS_PRODUCED;
            if (n<min) n = min;
            if (n>max) n = max;

            for(int q=0;q<n;q++)
                inds[start+q] = state.population.subpops[subpopulation].
                    individuals[produceMOEAD(start,subpopulation,state,thread)];
            return n;
            }


    public int produceMOEAD(final int start, final int subpopulation,
            final EvolutionState state,
            final int thread)
            {
            // pick size random individuals, then pick the best.
            Individual[] oldinds = state.population.subpops[subpopulation].individuals;
            int best = getRandomIndividual(start, subpopulation, state, thread);

            int s = getTournamentSizeToUse(state.random[thread]);

            if (pickWorst)
                for (int x=1;x<s;x++)
                    {
                    int j = getRandomIndividual(start, subpopulation, state, thread);
                    if (!betterThan(start, oldinds[j], oldinds[best], subpopulation, state, thread))  // j is at least as bad as best
                        best = j;
                    }
            else
                for (int x=1;x<s;x++)
                    {
                    int j = getRandomIndividual(start, subpopulation, state, thread);
                    if (betterThan(start, oldinds[j], oldinds[best], subpopulation, state, thread))  // j is better than best
                        best = j;
                    }

            return best;
            }
}
