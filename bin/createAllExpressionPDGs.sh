#!/bin/sh

cd $PDG_CONSTRAINTS/test/test/constraints/expression

for class in *.java
do
  cd $ACCRUE_BYTECODE
  name=test.constraints.expression.${class%.*}
  echo "CHECKING" "$name"
  time bin/runAnalysis.sh -e $name -cp ../pdg-constraints/classes/test -n pdg -writeDotPDG -haf "cs(0)" -out ../pdg-constraints/tests
  cd $PDG_CONSTRAINTS/test/test/constraints/expression
done
