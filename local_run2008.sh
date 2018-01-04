
for i in {1..8}; do
	mkdir ~/grid/2008-nsga-ii-moead-ecj-hybrid$i
	for j in {1..50}; do

		result=~/grid/2008-nsga-ii-moead-ecj-hybrid$i/out$j.stat
		analysis=~/grid/2008-nsga-ii-moead-ecj-hybrid$i/eval$j.stat
		java -cp ~/workspace/Library/ecj.23.jar:./bin:. ec.Evolve -file nsga2-indirect-sequence.params -p seed.0=$j -p stat.file=\$$result -p stat.evaluations=\$$analysis -p composition-task=/am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/problem.xml -p composition-taxonomy=/am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/taxonomy.xml -p composition-services=/am/state-opera/home1/sawczualex/workspace/wsc2008/Set0${i}MetaData/services-output.xml

	done
done

echo "Done!"
