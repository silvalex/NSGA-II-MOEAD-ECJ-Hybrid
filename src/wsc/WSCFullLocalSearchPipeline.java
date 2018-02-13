package wsc;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class WSCFullLocalSearchPipeline extends BreedingPipeline {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscmutationpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {

		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
        	inds[start] = (Individual)(inds[start].clone());
        }

        if (!(inds[start] instanceof SequenceVectorIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCMutationPipeline didn't get a SequenceVectorIndividual. The offending individual is: " + inds[start]);

        WSCInitializer init = (WSCInitializer) state.initializer;
    	SequenceVectorIndividual bestNeighbour = (SequenceVectorIndividual)inds[start].clone();

    	double bestScore;
		if (WSCInitializer.tchebycheff)
			bestScore = init.calculateTchebycheffScore(bestNeighbour, start);
		else
			bestScore = init.calculateScore(bestNeighbour, start);

    	SequenceVectorIndividual neighbour;

    	// Randomly select a fixed index
    	int indexA = init.random.nextInt(bestNeighbour.genome.length);

    	for (int indexB = 0; indexB < bestNeighbour.genome.length; indexB++) {
    		neighbour = (SequenceVectorIndividual)inds[start].clone();
    		// Randomly select the second index to swap
	    	swapServices(neighbour.genome, indexA, indexB);

	    	double score;
			if (WSCInitializer.tchebycheff)
				score = init.calculateTchebycheffScore(neighbour, start);
			else
				score = init.calculateScore(neighbour, start);

	    	// If the neighbour has a better fitness score than the current best, set current best to be neighbour
	        if (score < bestScore) {
	        	bestScore = score;
	        	bestNeighbour = neighbour;
	        }
    	}

        inds[start] = bestNeighbour;
        inds[start].evaluated = false;

        return n;
	}

	private void swapServices(Service[] genome, int indexA, int indexB) {
		Service temp = genome[indexA];
		genome[indexA] = genome[indexB];
		genome[indexB] = temp;
	}


}
