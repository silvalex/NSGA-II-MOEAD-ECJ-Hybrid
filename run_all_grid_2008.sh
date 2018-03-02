#!/bin/sh

NUM_RUNS=50

for i in {1..8}; do
  qsub -t 1-$NUM_RUNS:1 indirect_sequence.sh ~/workspace/wsc2008/Set0${i}MetaData 2008-nsga-ii-moead-ecj-hybrid${i} nsga2-indirect-sequence.params;
done
