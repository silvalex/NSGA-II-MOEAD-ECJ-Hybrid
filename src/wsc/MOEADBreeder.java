package wsc;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.multiobjective.nsga2.NSGA2Breeder;

public class MOEADBreeder extends NSGA2Breeder {

	private static final long serialVersionUID = 1L;

	/**
	 * A simple breeder that doesn't attempt to do any cross- population breeding.
	 * Basically it applies pipelines, one per thread, to various subchunks of a new
	 * population.
	 */
	public Population breedPopulation(EvolutionState state) {

		//Population newpop = (Population) state.population.emptyClone();
		Population newpop = state.population;

		// do regular breeding of this subpopulation
		BreedingPipeline bp = null;
		if (clonePipelineAndPopulation)
			bp = (BreedingPipeline) newpop.subpops[0].species.pipe_prototype.clone();
		else
			bp = (BreedingPipeline) newpop.subpops[0].species.pipe_prototype;

		for (int probIndex = 0; probIndex < state.population.subpops[0].individuals.length; probIndex++) {
			// Don't use min./max. parameters, and instead just pass the problem index as the subpopulation index
			bp.produce(0, 0, 0, probIndex, newpop.subpops[0].individuals, state, 0);
		}

		for (Individual i : newpop.subpops[0].individuals) {
			((WSCInitializer) state.initializer).externalPopulation.add(i);
		}

		return newpop;
	}
}
