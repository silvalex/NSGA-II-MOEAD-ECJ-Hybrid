package wsc;

import java.util.HashSet;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class WSCCrossoverPipeline extends BreedingPipeline {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wsccrossoverpipeline");
	}

	@Override
	public int numSources() {
		return 2;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {

		WSCInitializer init = (WSCInitializer) state.initializer;

		int n1 = sources[0].produce(min, max, start, subpopulation, inds, state, thread);
		Individual ind1 = (Individual) inds[start].clone();
		int n2 = sources[1].produce(min, max, start, subpopulation, inds, state, thread);
		Individual ind2 = (Individual) inds[start].clone();

        if (!(ind1 instanceof SequenceVectorIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCCrossoverPipeline didn't get a SequenceVectorIndividual. The offending individual is: " + ind1);

        if (!(ind2 instanceof SequenceVectorIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCCrossoverPipeline didn't get a SequenceVectorIndividual. The offending individual is: " + ind2);

        int nMin = Math.min(n1, n2);

        // Perform crossover
		SequenceVectorIndividual t1 = ((SequenceVectorIndividual)ind1);
		SequenceVectorIndividual t2 = ((SequenceVectorIndividual)ind2);

		// Select two random index numbers as the boundaries for the crossover section
		int indexA = init.random.nextInt(t1.genome.length);
		int indexB = init.random.nextInt(t1.genome.length);

		// Make sure they are different
		while (indexA == indexB)
			indexB = init.random.nextInt(t1.genome.length);

		// Determine which boundary they are
		int minBoundary = Math.min(indexA, indexB);
		int maxBoundary = Math.max(indexA, indexB);

		// Create new genomes
		Service[] newGenome1 = new Service[t1.genome.length];
		Service[] newGenome2 = new Service[t2.genome.length];

		// Swap crossover sections between candidates, keeping track of which services are in each section
		Set<Service> newSection1 = new HashSet<Service>();
		Set<Service> newSection2 = new HashSet<Service>();

		for (int index = minBoundary; index <= maxBoundary; index++) {
			// Copy section from parent 1 to genome 2
			newGenome2[index] = t1.genome[index];
			newSection2.add(t1.genome[index]);

			// Copy section from parent 2 to genome 1
			newGenome1[index] = t2.genome[index];
			newSection1.add(t2.genome[index]);
		}

		// Now fill the remainder of the new genomes, making sure not to duplicate any services
		fillNewGenome(t2, newGenome2, newSection2, minBoundary, maxBoundary);
		fillNewGenome(t1, newGenome1, newSection1, minBoundary, maxBoundary);

		// Replace the old genome with the new one
		t1.genome = newGenome1;
		t2.genome = newGenome2;

		// Select the result with the highest fitness as the result
    	double firstScore;
    	double secondScore;

    	if (WSCInitializer.tchebycheff) {
			firstScore = init.calculateTchebycheffScore(t1, start);
			secondScore = init.calculateTchebycheffScore(t2, start);
    	}
		else {
			firstScore = init.calculateScore(t1, start);
			secondScore = init.calculateScore(t2, start);
		}

    	if (firstScore < secondScore)
        	inds[start] = t1;
    	else
    		inds[start] = t2;

        inds[start].evaluated=false;
        return n1;
	}

	private void fillNewGenome(SequenceVectorIndividual parent, Service[] newGenome, Set<Service> newSection, int minBoundary, int maxBoundary) {
		int genomeIndex = getInitialIndex(minBoundary, maxBoundary);

		for (int i = 0; i < parent.genome.length; i++) {
			if (genomeIndex >= newGenome.length + 1)
				break;
			// Check that the service has not been already included, and add it
			if (!newSection.contains(parent.genome[i])) {
				newGenome[genomeIndex] = parent.genome[i];
				// Increment genome index
				genomeIndex = incrementIndex(genomeIndex, minBoundary, maxBoundary);
			}
		}
	}

	private int getInitialIndex(int minBoundary, int maxBoundary) {
		if (minBoundary == 0)
			return maxBoundary + 1;
		else
			return 0;
	}

	private int incrementIndex(int currentIndex, int minBoundary, int maxBoundary) {
		if (currentIndex + 1 >= minBoundary && currentIndex + 1 <= maxBoundary)
			return maxBoundary + 1;
		else
			return currentIndex + 1;
	}
}
