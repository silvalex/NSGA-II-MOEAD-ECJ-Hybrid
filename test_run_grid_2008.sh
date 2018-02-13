#!/bin/sh

NUM_RUNS=50

qsub -t 1-$NUM_RUNS:1 indirect_sequence.sh ~/workspace/wsc2008/Set04MetaData 2008-nsga-ii-moead-ecj-hybrid4 nsga2-indirect-sequence.params;

