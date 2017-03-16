for i in {1..5}; do
	mkdir ~/grid/2009-indirect-sequence$i
	for j in {1..50}; do

		result=~/grid/2009-indirect-sequence$i/out$j.stat
		analysis=~/grid/2009-indirect-sequence$i/eval$j.stat
		java -cp ~/workspace/Library/ecj.23.jar:./bin:. ec.Evolve -file indirect-sequence.params -p seed.0=$j -p stat.file=\$$result -p stat.evaluations=\$$analysis -p composition-task=/am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/problem.xml -p composition-taxonomy=/am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/taxonomy.xml -p composition-services=/am/state-opera/home1/sawczualex/workspace/wsc2009/Testset0${i}/services-output.xml

	done
done

echo "Done!"
