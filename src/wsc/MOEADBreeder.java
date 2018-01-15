package wsc;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.multiobjective.nsga2.NSGA2Breeder;

public class MOEADBreeder extends NSGA2Breeder {

	private static final long serialVersionUID = 1L;

	public Population breedPopulation(EvolutionState state) {

		Population population = state.population;
		Individual[] inds = state.population.subpops[0].individuals;

		// do regular breeding of this subpopulation
		BreedingPipeline bp = (BreedingPipeline) population.subpops[0].species.pipe_prototype;

		// Pass the probIndex as the starting point for each pipeline invocation
		for (int probIndex = 0; probIndex < state.population.subpops[0].individuals.length; probIndex++) {
			bp.produce(1, 1, probIndex, 0, inds, state, 0);
		}

		for (Individual i : inds) {
			((WSCInitializer) state.initializer).externalPopulation.add(i);
		}

		return population;
	}
}
