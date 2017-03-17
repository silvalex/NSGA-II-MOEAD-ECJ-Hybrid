package wsc;

import java.util.Arrays;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import ec.multiobjective.MultiObjectiveFitness;

public class WSCLocalSearchPipeline extends BreedingPipeline {

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
        	// Calculate a single score value dynamically, using weights that reflect the the importance of
        	//each objective (we are trying to push the good objective to keep on improving as much as possible).
        	double[] weights = new double[bestFitness.getNumObjectives()];

        	// Determine total to use in weight calculations
        	double total = 0.0;
        	for (double objective : bestFitness.getObjectives()) {
        		total += objective;
        	}

        	// Calculate weights
        	for (int i = 0; i < bestFitness.getNumObjectives(); i++) {
        		weights[i] = bestFitness.getObjective(i)/total;
        	}

        	// Now calculate initial best score
        	double bestScore = 0.0;
        	for (int i = 0; i < bestFitness.getNumObjectives(); i++) {
        		bestScore += (weights[i] * bestFitness.getObjective(i));
        	}

        	Service[] bestNeighbour = tree.genome;

        	double score = 0.0;
        	Service[] neighbour = null;

        	for (int i = 0; i < tree.genome.length; i++) {
        		for (int j = i + 1; j < tree.genome.length; j++) {
        			neighbour = Arrays.copyOf(tree.genome, tree.genome.length);
        			swapServices(neighbour, i, j);

        			// Calculate fitness, and update the best neighbour if necessary
        			tree.calculateSequenceFitness(init.numLayers, init.endServ, neighbour, init, state, true);

            		// Calculate the single-score value
            		score = 0.0;
            		for (int k = 0; k < ((MultiObjectiveFitness)tree.fitness).getNumObjectives(); k++) {
                		score += (weights[k] * ((MultiObjectiveFitness)tree.fitness).getObjective(k));
                	}
        			if (score < bestScore) {
        				bestFitness.setObjectives(state, ((MultiObjectiveFitness)tree.fitness).getObjectives());
        				bestNeighbour = Arrays.copyOf(neighbour, tree.genome.length);
        				bestScore = score;
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
