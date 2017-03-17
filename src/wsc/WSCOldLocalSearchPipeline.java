package wsc;

import java.util.Arrays;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;

public class WSCOldLocalSearchPipeline extends BreedingPipeline {

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
            for(int q=start;q<n+start;q++)
                inds[q] = (Individual)(inds[q].clone());
        }

        if (!(inds[start] instanceof SequenceVectorIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCMutationPipeline didn't get a SequenceVectorIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds[start]);

        WSCInitializer init = (WSCInitializer) state.initializer;

        // Perform mutation
        for(int q=start;q<n+start;q++) {
        	SequenceVectorIndividual tree = (SequenceVectorIndividual)inds[q];

        	MultiObjectiveFitness bestFitness = (MultiObjectiveFitness) tree.fitness.clone();
        	Service[] bestNeighbour = tree.genome;

        	Service[] neighbour = null;

        	// Pick a random index to swap with
        	int chosenIndex = init.random.nextInt(tree.genome.length);

        	for (int i = 0; i < tree.genome.length; i++) {
        		if (i != chosenIndex) {
        			neighbour = Arrays.copyOf(tree.genome, tree.genome.length);
        			swapServices(neighbour, i, chosenIndex);

        			// Calculate fitness, and update the best neighbour if necessary
        			tree.calculateSequenceFitness(init.numLayers, init.endServ, neighbour, init, state, true);
        			if (tree.fitness.betterThan(bestFitness)) {
        				bestFitness.setObjectives(state, ((MultiObjectiveFitness)tree.fitness).getObjectives());
        				bestNeighbour = Arrays.copyOf(neighbour, tree.genome.length);
        			}
        		}
        	}
            // Update the tree to contain the best genome found
        	tree.genome = bestNeighbour;
            tree.evaluated=false;
        }
        return n;
	}

	private void swapServices(Service[] genome, int indexA, int indexB) {
		Service temp = genome[indexA];
		genome[indexA] = genome[indexB];
		genome[indexB] = temp;
	}

}
