package wsc;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class WSCMutationPipeline extends BreedingPipeline {

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
        	inds[1] = (Individual)(inds[subpopulation].clone());
        }

        if (!(inds[1] instanceof SequenceVectorIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCMutationPipeline didn't get a SequenceVectorIndividual. The offending individual is: " + inds[subpopulation]);

        WSCInitializer init = (WSCInitializer) state.initializer;

        // Perform mutation

    	SequenceVectorIndividual tree = (SequenceVectorIndividual)inds[subpopulation];

    	int indexA = init.random.nextInt(tree.genome.length);
    	int indexB = init.random.nextInt(tree.genome.length);
    	swapServices(tree.genome, indexA, indexB);
        tree.evaluated=false;

        return n;
	}

	private void swapServices(Service[] genome, int indexA, int indexB) {
		Service temp = genome[indexA];
		genome[indexA] = genome[indexB];
		genome[indexB] = temp;
	}

}
