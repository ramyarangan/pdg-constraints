#!/bin/sh

cd $PDG_CONSTRAINTS/test/test/integration

for class in *.java
do
  cd $ACCRUE_BYTECODE
  name=test.integration.${class%.*}
  echo "CHECKING" "$name"
  time bin/runAnalysis.sh -e $name -cp ../pdg-constraints/classes/test -n pdg -writeDotPDG -haf "cs(0)" -out ../pdg-constraints/tests
  cd $PDG_CONSTRAINTS/test/test/integration
done
