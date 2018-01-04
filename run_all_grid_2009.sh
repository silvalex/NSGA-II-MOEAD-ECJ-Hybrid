#!/bin/sh

NUM_RUNS=50

for i in {1..5}; do
  qsub -t 1-$NUM_RUNS:1 indirect_sequence.sh ~/workspace/wsc2009/Testset0${i} 2009-nsga-ii-moead-ecj-hybrid${i} nsga2-indirect-sequence.params;
done
