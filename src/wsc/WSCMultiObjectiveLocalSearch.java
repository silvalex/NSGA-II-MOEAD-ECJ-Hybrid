package wsc;

import ec.EvolutionState;
import ec.Exchanger;
import ec.Population;
import ec.util.Parameter;

public class WSCMultiObjectiveLocalSearch extends Exchanger {

	@Override
	public void setup(EvolutionState state, Parameter base) {
		// TODO Auto-generated method stub
	}

	@Override
	public Population preBreedingExchangePopulation(EvolutionState state) {
		// TODO Auto-generated method stub
		return state.population;
	}

	@Override
	public Population postBreedingExchangePopulation(EvolutionState state) {
		System.out.printf("Test, generation %d\n", state.generation);
		// We get the initial set of mutually non-dominated solutions, called archive.

		// The solutions are initially marked as unexplored.

		// The search then iteratively applies the following steps:

			// First, a solution S is chosen randomly among all unexplored ones.

			// Then, the neighbourhood of S is fully explored and all neighbours that are non-dominated with regards to S and any solution in the archive A are added to A.

			// Solutions in A that are dominated by the newly added solutions are removed.

			// Once the neighbourhood of S has been fully explored, S is marked as explored.

			// When all solutions have been explored, and no more non-dominated solutions can be discovered, the algorithm stops.

		// TODO Auto-generated method stub
		return state.population;
	}

	@Override
	public String runComplete(EvolutionState state) {
		// TODO Auto-generated method stub
		return null;
	}
}
